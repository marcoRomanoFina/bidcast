package com.bidcast.auction_service.bid;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 en esta clase se separa gestión técnica de la infraestructura Redis (Hot Data).
 Realiza las operaciones atómicas de inyección y lectura
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BidInfrastructureService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    // TTL 30 minutos de inactividad
    private static final Duration SESSION_TTL = Duration.ofMinutes(30);

    /*
        Refresca el TTL de las claves asociadas a una sesión y sus bids usando Pipelining.
        Manda todas las órdenes de expiración en un solo viaje de red.
     */
    public void extendTTL(String sessionId, List<String> bidIds) {
        if (bidIds == null || bidIds.isEmpty()) return;

        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            byte[] sessionKey = String.format("session:%s:active_bids", sessionId).getBytes();
            connection.keyCommands().expire(sessionKey, SESSION_TTL.getSeconds());

            for (String bidId : bidIds) {
                byte[] bidKey = String.format("session:%s:bid:%s", sessionId, bidId).getBytes();
                connection.keyCommands().expire(bidKey, SESSION_TTL.getSeconds());
            }
            return null;
        });
    }

    /**
      Inyecta el estado completo de un Bid en Redis con TTL automático utilizando un Hash.
      Utiliza Pipelining para minimizar RTT.
     */
    public void injectIntoRedis(SessionBid bid, long remainingCents) {
        try {
            String bidId = bid.getId().toString();
            String sessionId = bid.getSessionId();
            
            // usamos metadata asi no inyectamos toda la entity
            BidMetadata metadata = BidMetadata.fromEntity(bid);
            String metadataJson = objectMapper.writeValueAsString(metadata);

            // usamos pipeline asi vamos solo una vez a redis
            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                byte[] bidKey = String.format("session:%s:bid:%s", sessionId, bidId).getBytes();
                byte[] sessionSetKey = String.format("session:%s:active_bids", sessionId).getBytes();
                
                // 1. Inyectamos en un hash 
                connection.hashCommands().hSet(bidKey, "budget".getBytes(), String.valueOf(remainingCents).getBytes());
                connection.hashCommands().hSet(bidKey, "metadata".getBytes(), metadataJson.getBytes());
                
                // 2. Seteamos TTL al hash
                connection.keyCommands().expire(bidKey, SESSION_TTL.getSeconds());

                // 3. Inyectamos en el Set de bids activos de la sesión
                connection.setCommands().sAdd(sessionSetKey, bidId.getBytes());
                
                // 4. Refrescamos TTL del Set de la sesión
                connection.keyCommands().expire(sessionSetKey, SESSION_TTL.getSeconds());
                
                return null;
            });

            log.debug("Bid {} injected into Redis hash with a 30m TTL via pipeline.", bidId);

        } catch (Exception e) {
            log.error("Critical failure injecting bid {} into Redis: {}", bid.getId(), e.getMessage());
            throw new RuntimeException("Hot-path infrastructure error (Redis)", e);
        }
    }

    /**
     * Elimina una puja del índice activo de la sesión.
     */
    public void removeFromActiveIndex(String sessionId, String bidId) {
        String key = String.format("session:%s:active_bids", sessionId);
        redisTemplate.opsForSet().remove(key, bidId);
    }

    /**
     * Limpia todo el rastro de un bid en Redis.
     */
    public void purgeBid(String sessionId, String bidId) {
        String bidKey = String.format("session:%s:bid:%s", sessionId, bidId);
        redisTemplate.delete(bidKey);
    }

    /**
     * Borra el índice de una sesión completa.
     */
    public void purgeSessionIndex(String sessionId) {
        String key = String.format("session:%s:active_bids", sessionId);
        redisTemplate.delete(key);
    }
}
