package com.bidcast.selection_service.offer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Esta clase encapsula toda la infraestructura caliente de Redis para offers.
 *
 * La idea es separar:
 * - estado persistente y auditable en PostgreSQL
 * - estado operativo y efímero en Redis
 *
 * Acá viven las primitivas de bajo nivel que usa el hot path:
 * - indexar offers activas por session
 * - guardar metadata compacta de cada offer
 * - mantener budget caliente en centavos
 * - bloquear creatives por device durante el cooldown local
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OfferInfrastructureService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    // Si una session deja de usarse, su hot state expira solo
    private static final Duration SESSION_TTL = Duration.ofMinutes(60);
    private static final Duration SESSION_CONTEXT_TTL = Duration.ofHours(12);
    private static final String ACTIVE_SESSIONS_KEY = "selection:active_sessions";
    private static final String ACTIVE_OFFERS_KEY = "session:%s:active_offers";
    private static final String OFFER_KEY = "session:%s:offer:%s";
    private static final String SESSION_CONTEXT_KEY = "session:%s:context";
    private static final String DEVICE_CREATIVE_COOLDOWN_KEY = "session:%s:device:%s:creative:%s:cooldown";

    /*
        Refresca el TTL de todas las claves calientes relacionadas a una session.

        Se usa cuando hay actividad real (selection o PoP) para extender la vida
        del hot state sin tener que reescribirlo completo.
     */
    public void extendTTL(String sessionId, List<String> offerIds) {
        if (offerIds == null || offerIds.isEmpty()) return;

        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            byte[] activeSessionsKey = ACTIVE_SESSIONS_KEY.getBytes();
            byte[] sessionKey = activeOffersKey(sessionId).getBytes();
            byte[] sessionContextKey = sessionContextKey(sessionId).getBytes();
            connection.keyCommands().expire(activeSessionsKey, SESSION_CONTEXT_TTL.getSeconds());
            connection.keyCommands().expire(sessionKey, SESSION_TTL.getSeconds());
            connection.keyCommands().expire(sessionContextKey, SESSION_CONTEXT_TTL.getSeconds());

            for (String offerId : offerIds) {
                byte[] offerKey = offerKey(sessionId, offerId).getBytes();
                connection.keyCommands().expire(offerKey, SESSION_TTL.getSeconds());
            }
            return null;
        });
    }

    /**
     * Registra metadata base de la session para que selection tenga contexto del venue
     * aun antes de que existan offers activas.
     */
    public void initializeSessionContext(String sessionId, String venueId, String ownerId, String basePricePerSlot) {
        redisTemplate.opsForHash().putAll(sessionContextKey(sessionId), Map.of(
                "venueId", venueId,
                "ownerId", ownerId,
                "basePricePerSlot", basePricePerSlot
        ));
        redisTemplate.opsForSet().add(ACTIVE_SESSIONS_KEY, sessionId);
        redisTemplate.expire(sessionContextKey(sessionId), SESSION_CONTEXT_TTL);
        redisTemplate.expire(ACTIVE_SESSIONS_KEY, SESSION_CONTEXT_TTL);
    }

    public void purgeSessionContext(String sessionId) {
        redisTemplate.delete(sessionContextKey(sessionId));
        redisTemplate.opsForSet().remove(ACTIVE_SESSIONS_KEY, sessionId);
    }

    /**
     * Inyecta una offer completa en Redis.
     *
     * Guarda dos piezas:
     * - budget: saldo operativo en centavos
     * - metadata: snapshot JSON compacto de la offer
     *
     * Además registra el offerId en el set de offers activas de la session.
     */
    public void injectIntoRedis(SessionOffer offer, long remainingCents) {
        try {
            String offerId = offer.getId().toString();
            String sessionId = offer.getSessionId();
            
            // Se serializa metadata compacta para no depender del modelo JPA completo en Redis.
            OfferMetadata metadata = OfferMetadata.fromEntity(offer);
            String metadataJson = objectMapper.writeValueAsString(metadata);

            // Se usa pipeline para reducir viajes de red y mantener la operación compacta.
            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                byte[] offerKey = offerKey(sessionId, offerId).getBytes();
                byte[] sessionSetKey = activeOffersKey(sessionId).getBytes();
                
                // 1. Persistimos budget y metadata en el hash principal de la offer.
                connection.hashCommands().hSet(offerKey, "budget".getBytes(), String.valueOf(remainingCents).getBytes());
                connection.hashCommands().hSet(offerKey, "metadata".getBytes(), metadataJson.getBytes());
                
                // 2. Aseguramos expiración automática.
                connection.keyCommands().expire(offerKey, SESSION_TTL.getSeconds());

                // 3. Registramos la offer como activa dentro de la session.
                connection.setCommands().sAdd(sessionSetKey, offerId.getBytes());
                
                // 4. Refrescamos también el índice de la session.
                connection.keyCommands().expire(sessionSetKey, SESSION_TTL.getSeconds());
                
                return null;
            });

            log.debug("Offer {} injected into Redis hash with a 60m TTL via pipeline.", offerId);

        } catch (Exception e) {
            log.error("Critical failure injecting offer {} into Redis: {}", offer.getId(), e.getMessage());
            throw new RuntimeException("Hot-path infrastructure error (Redis)", e);
        }
    }

    /**
     * Saca una offer del set de activas sin borrar necesariamente todo su hash.
     *
     * Esto se usa, por ejemplo, cuando una offer queda EXHAUSTED y ya no debe competir.
     */
    public void removeFromActiveIndex(String sessionId, String offerId) {
        String key = activeOffersKey(sessionId);
        redisTemplate.opsForSet().remove(key, offerId);
    }

    /**
     * Borra la clave principal de una offer del hot state.
     */
    public void purgeOffer(String sessionId, String offerId) {
        String offerKey = offerKey(sessionId, offerId);
        redisTemplate.delete(offerKey);
    }

    /**
     * Borra el índice de offers activas de una session completa.
     *
     * Se usa al final del settlement/cierre para limpiar el hot path.
     */
    public void purgeSessionIndex(String sessionId) {
        String key = activeOffersKey(sessionId);
        redisTemplate.delete(key);
    }

    /**
     * Bloquea un creative para un device puntual durante su cooldown local.
     *
     * Este bloqueo nace al devolver la selección, no al llegar el PoP, para evitar
     * que dos refills casi consecutivos reciclen el mismo creative en el mismo device.
     */
    public void blockCreativeForDevice(String sessionId, String deviceId, String creativeId, Duration cooldown) {
        if (cooldown == null || cooldown.isZero() || cooldown.isNegative()) {
            return;
        }
        redisTemplate.opsForValue().set(deviceCreativeCooldownKey(sessionId, deviceId, creativeId), "1", cooldown);
    }

    /**
     * Chequeo puntual de bloqueo local por device + creative.
     */
    public boolean isCreativeBlockedForDevice(String sessionId, String deviceId, String creativeId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(deviceCreativeCooldownKey(sessionId, deviceId, creativeId)));
    }

    /**
     * Dada una lista de creatives candidatos, devuelve cuáles están hoy bloqueados
     * localmente para ese device.
     */
    public Set<String> filterBlockedCreativeIds(String sessionId, String deviceId, List<String> creativeIds) {
        if (creativeIds == null || creativeIds.isEmpty()) {
            return Set.of();
        }

        List<Object> results = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (String creativeId : creativeIds) {
                byte[] key = deviceCreativeCooldownKey(sessionId, deviceId, creativeId).getBytes();
                connection.keyCommands().exists(key);
            }
            return null;
        });

        if (results == null || results.isEmpty()) {
            return Set.of();
        }

        Set<String> blocked = new java.util.HashSet<>();
        for (int i = 0; i < creativeIds.size() && i < results.size(); i++) {
            Object raw = results.get(i);
            if (Boolean.TRUE.equals(raw) || (raw instanceof Number number && number.longValue() > 0)) {
                blocked.add(creativeIds.get(i));
            }
        }
        return blocked;
    }

    /**
     * Lee el budget operativo desde Redis.
     *
     * Si no existe, el caller decide si rehidrata o si falla.
     */
    public Optional<Long> getBudgetCents(String sessionId, String offerId) {
        Object rawBudget = redisTemplate.opsForHash().get(offerKey(sessionId, offerId), "budget");
        if (rawBudget == null) {
            return Optional.empty();
        }
        return Optional.of(Long.parseLong(rawBudget.toString()));
    }

    /**
     * Descuenta presupuesto en el hot state.
     *
     * Devuelve el saldo resultante si la key existe.
     */
    public Optional<Long> decrementBudgetCents(String sessionId, String offerId, long amountCents) {
        return Optional.ofNullable(
                redisTemplate.opsForHash().increment(offerKey(sessionId, offerId), "budget", -amountCents)
        );
    }

    /**
     * Compensa un descuento previo cuando una selección no puede concretarse.
     */
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

    private String sessionContextKey(String sessionId) {
        return String.format(SESSION_CONTEXT_KEY, sessionId);
    }
}
