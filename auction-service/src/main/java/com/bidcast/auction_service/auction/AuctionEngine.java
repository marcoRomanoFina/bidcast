package com.bidcast.auction_service.auction;

import com.bidcast.auction_service.bid.BidInfrastructureService;
import com.bidcast.auction_service.bid.BidMetadata;
import com.bidcast.auction_service.bid.BidRehydrationService;
import com.bidcast.auction_service.bid.RestoredBid;
import com.bidcast.auction_service.core.exception.AuctionExecutionException;
import com.bidcast.auction_service.core.exception.NoAdFoundException;
import com.bidcast.auction_service.core.exception.SessionConcurrencyException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/*
 Bueno aca vamos con el "corazon" del proyecto, donde se hace la subasta y se eligue el ganador
 para mostrar siguiente en alguna session.
 (versión inicial, faltan muchos detalles, ej - "slots" de 15 segs x anuncio, base price del publisher, etc)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuctionEngine {

    private final StringRedisTemplate redisTemplate;
    private final RedissonClient redissonClient;
    private final ReceiptTokenService receiptTokenService;
    private final BidRehydrationService rehydrationService;
    private final BidInfrastructureService infrastructureService;
    private final AuctionPersistenceService auctionPersistenceService;
    private final ObjectMapper objectMapper;

    
    /**
     * Evalúa qué anuncio mostrar en una sesión.
     * Implementa un Lock Distribuido para evitar procesamiento concurrente innecesario.
     */
    public WinningAd evaluateNext(String sessionId) {
        long startTime = System.currentTimeMillis();
        String lockKey = "lock:auction:" + sessionId;
        RLock lock = redissonClient.getLock(lockKey);
        boolean acquired = false;

        // 1. Intento de adquisición 
        try {
            acquired = lock.tryLock(500, 5000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            // Restauramos el flag de interrupción y lanzamos error específico
            Thread.currentThread().interrupt();
            throw new AuctionExecutionException(sessionId, 
                "Thread interrupted: auction could not be completed", "ERR_THREAD_INTERRUPTED", e);
        }

        // 2. Si no lo logramos, rebotamos (Fail-Fast)
        if (!acquired) {
            log.warn("Concurrent auction detected for session {}. Rejecting request.", sessionId);
            throw new SessionConcurrencyException(sessionId, "An auction is already in progress for this session.");
        }

        // 3. Una vez que TENEMOS el lock, entramos al try-finally de ejecución
        try {
            // Buscamos todos los bids activos de la session
            List<String> activeBidIds = resolveActiveBidIds(sessionId);
            if (activeBidIds.isEmpty()) {
                throw new NoAdFoundException(sessionId);
            }

            // Extendemos el TTL para mantener la sesión caliente
            infrastructureService.extendTTL(sessionId, activeBidIds);

            // Vamos a buscar a redis la info de los bids
            List<BidSnapshot> snapshots = fetchBidSnapshots(sessionId, activeBidIds);

            // Seleccionamos el ganador
            WinningAd winner = selectWinningBid(snapshots)
                    .map(w -> finalizeAuction(sessionId, w, startTime))
                    .orElseThrow(() -> new NoAdFoundException(sessionId));

            
            auctionPersistenceService.persistAuctionSuccess(winner);

            return winner;

        } catch (NoAdFoundException | SessionConcurrencyException e) {
            throw e;
        } catch (Exception e) {
            log.error("Critical auction error for session {}: {}", sessionId, e.getMessage());
            throw new AuctionExecutionException(sessionId, 
                "Critical failure in auction engine", "ERR_AUCTION_SESSION_FAILURE", e);
        } finally {
            // Solo liberamos si somos los dueños (garantizado por la lógica del tryLock exitoso arriba)
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
    
    // metodo privado para buscar los active bids de la session pasada por argumento
    private List<String> resolveActiveBidIds(String sessionId) {
        // primero buscamos los active bids de redis
        String sessionSetKey = String.format("session:%s:active_bids", sessionId);
        Optional<Set<String>> members = Optional.ofNullable(redisTemplate.opsForSet().members(sessionSetKey));
        
        // si no estan, buscamos en la DB, para ello usamos el bidRehydrationService
        // y hacemos un lock distribuido asi solo lo hacemos una vez
        if (members.isEmpty() || members.get().isEmpty()) {
            String lockKey = "lock:rehydrate:" + sessionId;
            RLock lock = redissonClient.getLock(lockKey);
            
            try {
                // Lock distribuido manual con Redis (SETNX) para evitar Thundering Herd
                if (lock.tryLock(1, 3, TimeUnit.SECONDS)) {
                    try {
                        // Double-check: Verificar si otro nodo ya rehidrató mientras esperábamos el lock
                        members = Optional.ofNullable(redisTemplate.opsForSet().members(sessionSetKey));

                        if (members.isEmpty() || members.get().isEmpty()) {
                            log.warn("Session {} has no Redis index. Rehydrating...", sessionId);
                            rehydrationService.rehydrateSession(sessionId);
                            //buscamos en redis los datos actualizados
                            members = Optional.ofNullable(redisTemplate.opsForSet().members(sessionSetKey));
                        }
                    } finally {
                        //soltamos el lock 
                        if (lock.isHeldByCurrentThread()) {
                            lock.unlock();
                        }
                    }
                } else {

                    // Espera corta y reintento si otro nodo está rehidratando (mini polling)
                    log.info("Session {} is being rehydrated by another node. Waiting...", sessionId);
                    for (int i = 0; i < 3; i++) {
                        members = Optional.ofNullable(redisTemplate.opsForSet().members(sessionSetKey));
                        if (!members.isEmpty() && !members.get().isEmpty()) break;
                        Thread.sleep(100);
}
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Interrupted while waiting for lock on session {}", sessionId);
            }
        }
        // devolvemos el resultado final despues de actualizar
        return (!members.isEmpty()) ? new ArrayList<>(members.get()) : Collections.emptyList();
    }


    // metodo privado para buscar la metadata y budget de cada bidId, devolviendolo en un BidSnapshot liguero
    private List<BidSnapshot> fetchBidSnapshots(String sessionId, List<String> bidIds) {
        if (bidIds.isEmpty()) return Collections.emptyList();

        // Recuperamos metadata y budget de todos los bids en un único RTT usando Pipelining
        List<Object> results = redisTemplate.executePipelined(new SessionCallback<Object>() {
            @Override
            @SuppressWarnings("unchecked")
            public <K, V> Object execute(RedisOperations<K, V> operations) {
                var ops = (RedisOperations<String, String>) operations;
                for (String bidId : bidIds) {
                    String bidKey = String.format("session:%s:bid:%s", sessionId, bidId);
                    ops.opsForHash().multiGet(bidKey, List.of("metadata", "budget"));
                }
                return null;
            }
        });

        return IntStream.range(0, bidIds.size())
                .mapToObj(i -> parseRawPipelineResult(bidIds.get(i), sessionId, results.get(i)))
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<BidSnapshot> parseRawPipelineResult(String bidId, String sessionId, Object rawResult) {
        // 1. validamos los resultamos sean de la forma que queremos y completos, sino reparamos
        if (!(rawResult instanceof List<?> fields) || fields.size() < 2 || fields.get(0) == null || fields.get(1) == null) {
            log.warn("Incomplete/null Redis data for bid {}. Starting self-healing...", bidId);
            return attemptRepair(bidId, sessionId);
        }

        // 2. Si pasamos la validacion, parseamos a un bidSnapshot
        try {
            String metaJson = fields.get(0).toString();
            long budget = Long.parseLong(fields.get(1).toString());
            BidMetadata metadata = objectMapper.readValue(metaJson, BidMetadata.class);
            return Optional.of(new BidSnapshot(metadata, budget));
        } catch (Exception e) {
            log.error("Critical error deserializing bid {} in session {}: {}", bidId, sessionId, e.getMessage());
            // Si el JSON estaba roto, también disparamos la reparación
            return attemptRepair(bidId, sessionId);
        }
    }
    // metodo que intenta "reparar" los datos faltantes
    private Optional<BidSnapshot> attemptRepair(String bidId, String sessionId) {
        try {
            UUID bidUuid = UUID.fromString(bidId);
            RestoredBid restored = rehydrationService.rehydrateFullBid(bidUuid);
            return Optional.ofNullable(new BidSnapshot(restored.metadata(), restored.balanceCents()));
        } catch (Exception e) {
            log.error("Self-healing failed for bid {}: {}", bidId, e.getMessage());
            return Optional.empty();
        }
    }

    // metodo privado para seleccionar el ganador de una subasta, (first price) y validamos que tenga budget restante
    private Optional<BidMetadata> selectWinningBid(List<BidSnapshot> snapshots) {
        return snapshots.stream()
                .filter(s -> s.balanceCents() >= s.metadata().advertiserBidPrice().multiply(new BigDecimal("100")).longValue())
                .map(BidSnapshot::metadata)
                .max(Comparator.comparing(BidMetadata::advertiserBidPrice));
    }

    // metodo privado para generar el receipt con el HMAC para despues vuelva como PoP y enviarlo en un DTO
    private WinningAd finalizeAuction(String sessionId, BidMetadata winner, long startTime) {
        String receiptId = receiptTokenService.generateReceiptId(
                sessionId, winner.id(), winner.advertiserId(), winner.advertiserBidPrice());

        return WinningAd.builder()
                .auctionId(UUID.randomUUID())
                .bidId(winner.id())
                .mediaUrl(winner.mediaUrl())
                .advertiserId(winner.advertiserId())
                .campaignId(winner.campaignId())
                .playReceiptId(receiptId)
                .build();
    }

    // version liguera con los datos necesarios para guardarlos
    private record BidSnapshot(BidMetadata metadata, long balanceCents) {}
}
