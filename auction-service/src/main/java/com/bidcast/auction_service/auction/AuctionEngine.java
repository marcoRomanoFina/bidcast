package com.bidcast.auction_service.auction;

import com.bidcast.auction_service.bid.BidMetadata;
import com.bidcast.auction_service.bid.BidRehydrationService;
import com.bidcast.auction_service.bid.RestoredBid;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.IntStream;

/**
 * Motor central de subastas (RTB).
 * Implementación puramente funcional libre de NULLs.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuctionEngine {

    private final StringRedisTemplate redisTemplate;
    private final ReceiptTokenService receiptTokenService;
    private final BidRehydrationService rehydrationService;
    private final ObjectMapper objectMapper;

    public AuctionResult evaluateNext(String sessionId) {
        long startTime = System.currentTimeMillis();
        String sessionSetKey = String.format("session:%s:active_bids", sessionId);

        List<String> bidIdList = Optional.ofNullable(redisTemplate.opsForSet().members(sessionSetKey))
                .filter(ids -> !ids.isEmpty())
                .map(ArrayList::new)
                .orElseGet(() -> {
                    log.warn("Sesión {} sin datos en Redis. Iniciando sanación masiva...", sessionId);
                    rehydrationService.rehydrateSession(sessionId);
                    return Optional.ofNullable(redisTemplate.opsForSet().members(sessionSetKey))
                            .map(ArrayList::new)
                            .orElse(new ArrayList<>());
                });

        if (bidIdList.isEmpty()) {
            return new NoAdFound();
        }

        return runAuctionProcess(sessionId, bidIdList, startTime);
    }

    private AuctionResult runAuctionProcess(String sessionId, List<String> bidIdList, long startTime) {
        List<String> metadataKeys = bidIdList.stream()
                .map(id -> String.format("session:%s:bid:%s:metadata", sessionId, id))
                .toList();
        List<String> budgetKeys = bidIdList.stream()
                .map(id -> String.format("session:%s:bid:%s:budget", sessionId, id))
                .toList();

        List<String> metadatasRaw = Optional.ofNullable(redisTemplate.opsForValue().multiGet(metadataKeys))
                .orElse(Collections.emptyList());
        List<String> budgetsRaw = Optional.ofNullable(redisTemplate.opsForValue().multiGet(budgetKeys))
                .orElse(Collections.emptyList());

        return IntStream.range(0, bidIdList.size())
                .mapToObj(i -> tryParseAndRepair(bidIdList.get(i), metadatasRaw, budgetsRaw, i))
                .flatMap(Optional::stream)
                .filter(this::hasEnoughBudget)
                .max(Comparator.comparing(bb -> bb.metadata().advertiserBidPrice()))
                .map(winner -> finalizeAuction(sessionId, winner, startTime))
                .orElse(new NoAdFound());
    }

    private Optional<BidWithBalance> tryParseAndRepair(String bidId, List<String> metadatas, List<String> budgets, int index) {
        Optional<String> jsonOpt = metadatas.size() > index ? Optional.ofNullable(metadatas.get(index)) : Optional.empty();
        Optional<String> budgetOpt = budgets.size() > index ? Optional.ofNullable(budgets.get(index)) : Optional.empty();

        if (jsonOpt.isEmpty() || budgetOpt.isEmpty()) {
            log.warn("Bid {} incompleto en Redis. Reparando...", bidId);
            RestoredBid restored = rehydrationService.rehydrateFullBid(UUID.fromString(bidId));
            return Optional.of(new BidWithBalance(restored.metadata(), restored.balanceCents()));
        }

        try {
            BidMetadata metadata = objectMapper.readValue(jsonOpt.get(), BidMetadata.class);
            long balance = Long.parseLong(budgetOpt.get());
            return Optional.of(new BidWithBalance(metadata, balance));
        } catch (Exception e) {
            log.error("Fallo irrecuperable al procesar bid {}: {}", bidId, e.getMessage());
            return Optional.empty();
        }
    }

    private AuctionResult finalizeAuction(String sessionId, BidWithBalance winner, long startTime) {
        long executionTime = System.currentTimeMillis() - startTime;
        log.info("Subasta finalizada en {} ms. Ganador: {}", executionTime, winner.metadata().id());
        return createWinningAd(sessionId, winner.metadata());
    }

    private boolean hasEnoughBudget(BidWithBalance bb) {
        long requiredCents = bb.metadata().advertiserBidPrice().multiply(new BigDecimal("100")).longValue();
        return bb.balance() >= requiredCents;
    }

    private WinningAd createWinningAd(String sessionId, BidMetadata winner) {
        String playReceiptId = receiptTokenService.generateReceiptId(
                sessionId, 
                winner.id(), 
                winner.advertiserId(), 
                winner.advertiserBidPrice()
        );

        return WinningAd.builder()
                .bidId(winner.id())
                .mediaUrl(winner.mediaUrl())
                .advertiserId(winner.advertiserId())
                .campaignId(winner.campaignId())
                .playReceiptId(playReceiptId)
                .build();
    }

    private record BidWithBalance(BidMetadata metadata, long balance) {}
}
