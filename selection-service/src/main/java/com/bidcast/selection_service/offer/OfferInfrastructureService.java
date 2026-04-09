package com.bidcast.selection_service.offer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 en esta clase se separa gestión técnica de la infraestructura Redis (Hot Data).
 Realiza las operaciones atómicas de inyección y lectura
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OfferInfrastructureService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    // TTL 30 minutos de inactividad
    private static final Duration SESSION_TTL = Duration.ofMinutes(30);
    private static final String ACTIVE_OFFERS_KEY = "session:%s:active_offers";
    private static final String OFFER_KEY = "session:%s:offer:%s";
    private static final String DEVICE_CREATIVE_COOLDOWN_KEY = "session:%s:device:%s:creative:%s:cooldown";

    /*
        Refresca el TTL de las claves asociadas a una sesión y sus offers usando Pipelining.
        Manda todas las órdenes de expiración en un solo viaje de red.
     */
    public void extendTTL(String sessionId, List<String> offerIds) {
        if (offerIds == null || offerIds.isEmpty()) return;

        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            byte[] sessionKey = activeOffersKey(sessionId).getBytes();
            connection.keyCommands().expire(sessionKey, SESSION_TTL.getSeconds());

            for (String offerId : offerIds) {
                byte[] offerKey = offerKey(sessionId, offerId).getBytes();
                connection.keyCommands().expire(offerKey, SESSION_TTL.getSeconds());
            }
            return null;
        });
    }

    /**
      Inyecta el estado completo de una offer en Redis con TTL automatico utilizando un Hash.
      Utiliza Pipelining para minimizar RTT.
     */
    public void injectIntoRedis(SessionOffer offer, long remainingCents) {
        try {
            String offerId = offer.getId().toString();
            String sessionId = offer.getSessionId();
            
            // usamos metadata asi no inyectamos toda la entity
            OfferMetadata metadata = OfferMetadata.fromEntity(offer);
            String metadataJson = objectMapper.writeValueAsString(metadata);

            // usamos pipeline asi vamos solo una vez a redis
            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                byte[] offerKey = offerKey(sessionId, offerId).getBytes();
                byte[] sessionSetKey = activeOffersKey(sessionId).getBytes();
                
                // 1. Inyectamos en un hash
                connection.hashCommands().hSet(offerKey, "budget".getBytes(), String.valueOf(remainingCents).getBytes());
                connection.hashCommands().hSet(offerKey, "metadata".getBytes(), metadataJson.getBytes());
                
                // 2. Seteamos TTL al hash
                connection.keyCommands().expire(offerKey, SESSION_TTL.getSeconds());

                // 3. Inyectamos en el set de offers activas de la sesión
                connection.setCommands().sAdd(sessionSetKey, offerId.getBytes());
                
                // 4. Refrescamos TTL del Set de la sesión
                connection.keyCommands().expire(sessionSetKey, SESSION_TTL.getSeconds());
                
                return null;
            });

            log.debug("Offer {} injected into Redis hash with a 30m TTL via pipeline.", offerId);

        } catch (Exception e) {
            log.error("Critical failure injecting offer {} into Redis: {}", offer.getId(), e.getMessage());
            throw new RuntimeException("Hot-path infrastructure error (Redis)", e);
        }
    }

    /**
     * Elimina una offer del índice activo de la sesión.
     */
    public void removeFromActiveIndex(String sessionId, String offerId) {
        String key = activeOffersKey(sessionId);
        redisTemplate.opsForSet().remove(key, offerId);
    }

    /**
     * Limpia todo el rastro de una offer en Redis.
     */
    public void purgeOffer(String sessionId, String offerId) {
        String offerKey = offerKey(sessionId, offerId);
        redisTemplate.delete(offerKey);
    }

    /**
     * Borra el índice de una sesión completa.
     */
    public void purgeSessionIndex(String sessionId) {
        String key = activeOffersKey(sessionId);
        redisTemplate.delete(key);
    }

    public void blockCreativeForDevice(String sessionId, String deviceId, String creativeId, Duration cooldown) {
        if (cooldown == null || cooldown.isZero() || cooldown.isNegative()) {
            return;
        }
        redisTemplate.opsForValue().set(deviceCreativeCooldownKey(sessionId, deviceId, creativeId), "1", cooldown);
    }

    public boolean isCreativeBlockedForDevice(String sessionId, String deviceId, String creativeId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(deviceCreativeCooldownKey(sessionId, deviceId, creativeId)));
    }

    public Set<String> filterBlockedCreativeIds(String sessionId, String deviceId, List<String> creativeIds) {
        if (creativeIds == null || creativeIds.isEmpty()) {
            return Set.of();
        }
        return creativeIds.stream()
                .filter(creativeId -> isCreativeBlockedForDevice(sessionId, deviceId, creativeId))
                .collect(Collectors.toSet());
    }

    public Optional<Long> getBudgetCents(String sessionId, String offerId) {
        Object rawBudget = redisTemplate.opsForHash().get(offerKey(sessionId, offerId), "budget");
        if (rawBudget == null) {
            return Optional.empty();
        }
        return Optional.of(Long.parseLong(rawBudget.toString()));
    }

    public Optional<Long> decrementBudgetCents(String sessionId, String offerId, long amountCents) {
        return Optional.ofNullable(
                redisTemplate.opsForHash().increment(offerKey(sessionId, offerId), "budget", -amountCents)
        );
    }

    public Optional<Long> incrementBudgetCents(String sessionId, String offerId, long amountCents) {
        return Optional.ofNullable(
                redisTemplate.opsForHash().increment(offerKey(sessionId, offerId), "budget", amountCents)
        );
    }

    private String activeOffersKey(String sessionId) {
        return String.format(ACTIVE_OFFERS_KEY, sessionId);
    }

    private String offerKey(String sessionId, String offerId) {
        return String.format(OFFER_KEY, sessionId, offerId);
    }

    private String deviceCreativeCooldownKey(String sessionId, String deviceId, String creativeId) {
        return String.format(DEVICE_CREATIVE_COOLDOWN_KEY, sessionId, deviceId, creativeId);
    }
}
