package com.document.notification.system.notification.service.adapter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Token Bucket rate limiter for controlling email sending throughput.
 * <p>
 * Each permit in the semaphore represents one email that may be sent.
 * A scheduled task refills the bucket at a fixed interval so the system
 * never exceeds the configured emails-per-second.
 * <p>
 * {@link #acquire()} blocks (never rejects) until a token is available,
 * so emails queue gracefully under load.
 * <p>
 * Implements {@link DisposableBean} for clean shutdown with Spring lifecycle.
 *
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 */
@Slf4j
public class EmailRateLimiter implements DisposableBean {

    private final Semaphore tokens;
    private final int maxTokens;
    private final ScheduledExecutorService scheduler;

    public EmailRateLimiter(int maxTokens, long refillIntervalMs) {
        this.maxTokens = maxTokens;
        this.tokens = new Semaphore(maxTokens);
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "email-rate-limiter");
            t.setDaemon(true);
            return t;
        });
        this.scheduler.scheduleAtFixedRate(this::refill, refillIntervalMs, refillIntervalMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Blocks until a token is available. Never rejects — just waits.
     */
    public void acquire() throws InterruptedException {
        tokens.acquire();
    }

    @Override
    public void destroy() {
        scheduler.shutdownNow();
        log.info("Email rate limiter shut down");
    }

    private void refill() {
        int currentAvailable = tokens.availablePermits();
        int tokensToAdd = maxTokens - currentAvailable;
        if (tokensToAdd > 0) {
            tokens.release(tokensToAdd);
            log.debug("Refilled {} email tokens, available: {}", tokensToAdd, tokens.availablePermits());
        }
    }
}
