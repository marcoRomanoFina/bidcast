package com.bidcast.selection_service.selection;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.stereotype.Service;

import com.bidcast.selection_service.core.exception.SelectionInfrastructureUnavailableException;
import com.bidcast.selection_service.core.exception.SessionSelectionBusyException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
// Encapsula el lock distribuido por session.
// Evita que dos workers seleccionen al mismo tiempo para la misma session y se pisen
// en punteros, budget o decisiones de ranking.
public class SelectionLockService {

    private static final long SELECTION_LOCK_WAIT_MILLIS = 0L;
    private static final long SELECTION_LOCK_LEASE_MILLIS = 2_000L;

    private final RedissonClient redissonClient;

    /**
     * Ejecuta una acción bajo exclusión mutua por session.
     */
    public <T> T withSessionLock(String sessionId, Supplier<T> action) {
        try {
            RLock lock = redissonClient.getLock(selectionLockKey(sessionId));
            boolean acquired = acquireSessionLock(lock, sessionId);

            try {
                return action.get();
            } finally {
                if (acquired && lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        } catch (SelectionInfrastructureUnavailableException | SessionSelectionBusyException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            if (isRedisUnavailable(ex)) {
                throw new SelectionInfrastructureUnavailableException("Redis");
            }
            throw ex;
        }
    }

    // Intenta tomar el lock de la session de manera no bloqueante.
    private boolean acquireSessionLock(RLock lock, String sessionId) {
        try {
            boolean acquired = lock.tryLock(
                    SELECTION_LOCK_WAIT_MILLIS,
                    SELECTION_LOCK_LEASE_MILLIS,
                    TimeUnit.MILLISECONDS
            );
            if (!acquired) {
                throw new SessionSelectionBusyException(sessionId);
            }
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SessionSelectionBusyException(sessionId);
        }
    }

    // Convención única para la key del lock distribuido.
    private String selectionLockKey(String sessionId) {
        return "lock:session:" + sessionId + ":selection";
    }

    // Unifica detección de fallos Redis/Redisson para mapearlos a error de dominio.
    private boolean isRedisUnavailable(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof RedisConnectionFailureException
                    || current instanceof RedisSystemException
                    || current instanceof RedisException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
