package com.bidcast.selection_service.offer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Persistencia definitiva en PostgreSQL (Cold Data).
 * Maneja el ciclo de vida de las entidades y estados financieros históricos.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OfferPersistenceService {

    private final SessionOfferRepository sessionOfferRepository;

    // Activa la oferta
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SessionOffer activate(UUID offerId) {
        return sessionOfferRepository.findById(offerId)
                .map(offer -> {
                    offer.activate();
                    return sessionOfferRepository.save(offer);
                })
                .orElseThrow(() -> new RuntimeException("Offer not found to activate: " + offerId));
    }

    // Marca la oferta como fallida
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(UUID offerId) {
        sessionOfferRepository.findById(offerId).ifPresent(offer -> {
            offer.fail();
            sessionOfferRepository.save(offer);
        });
    }

    // Marca un fallo crítico
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCriticalFailure(UUID offerId) {
        sessionOfferRepository.findById(offerId).ifPresent(offer -> {
            offer.markCriticalFailure();
            sessionOfferRepository.save(offer);
        });
    }

    // Marca la oferta como agotada (sin presupuesto)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void exhaust(UUID offerId) {
        sessionOfferRepository.findById(offerId).ifPresent(offer -> {
            offer.exhaust();
            sessionOfferRepository.save(offer);
        });
    }

    // Cierra la oferta al finalizar la sesión
    @Transactional
    public void close(UUID offerId) {
        sessionOfferRepository.findById(offerId).ifPresent(offer -> {
            offer.close();
            sessionOfferRepository.save(offer);
        });
    }
}
