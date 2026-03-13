package com.bidcast.auction_service.bid;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;



/**
 * RESPONSABILIDAD: Gestión técnica de la infraestructura Redis (Hot Data).
 * Realiza las operaciones atómicas de inyección y lectura en milisegundos.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BidInfrastructureService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Inyecta el estado completo de un Bid en Redis para que el motor pueda operar.
     */
    public void injectIntoRedis(SessionBid bid, long remainingCents) {
        try {
            String bidId = bid.getId().toString();
            String sessionId = bid.getSessionId();

            // 1. Saldo Atómico
            String budgetKey = String.format("session:%s:bid:%s:budget", sessionId, bidId);
            redisTemplate.opsForValue().set(budgetKey, String.valueOf(remainingCents));

            // 2. Metadatos (DTO Limpio)
            String metadataKey = String.format("session:%s:bid:%s:metadata", sessionId, bidId);
            BidMetadata metadata = BidMetadata.fromEntity(bid);
            redisTemplate.opsForValue().set(metadataKey, objectMapper.writeValueAsString(metadata));

            // 3. Índice de la Sesión
            String sessionSetKey = String.format("session:%s:active_bids", sessionId);
            redisTemplate.opsForSet().add(sessionSetKey, bidId);

        } catch (Exception e) {
            log.error("Fallo crítico al inyectar bid {} en Redis: {}", bid.getId(), e.getMessage());
            throw new RuntimeException("Error de infraestructura caliente (Redis)", e);
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
        String budgetKey = String.format("session:%s:bid:%s:budget", sessionId, bidId);
        String metadataKey = String.format("session:%s:bid:%s:metadata", sessionId, bidId);
        redisTemplate.delete(java.util.List.of(budgetKey, metadataKey));
    }

    /**
     * Borra el índice de una sesión completa.
     */
    public void purgeSessionIndex(String sessionId) {
        String key = String.format("session:%s:active_bids", sessionId);
        redisTemplate.delete(key);
    }
}
