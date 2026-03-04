package com.billy.customers.common;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Limita la cantidad de peticiones por segundo (ventana fija de 1 s)
 * para mantener métricas coherentes y evitar saturación.
 */
public final class RateLimiter {

    private final int maxRequestsPerSecond;
    private final AtomicLong currentSecond = new AtomicLong(0);
    private final AtomicInteger count = new AtomicInteger(0);

    public RateLimiter(int maxRequestsPerSecond) {
        this.maxRequestsPerSecond = maxRequestsPerSecond;
    }

    /**
     * Devuelve true si la petición está permitida, false si se superó el límite (devolver 429).
     */
    public boolean allowRequest() {
        long now = System.currentTimeMillis() / 1000;
        if (currentSecond.get() != now) {
            currentSecond.set(now);
            count.set(0);
        }
        return count.incrementAndGet() <= maxRequestsPerSecond;
    }
}
