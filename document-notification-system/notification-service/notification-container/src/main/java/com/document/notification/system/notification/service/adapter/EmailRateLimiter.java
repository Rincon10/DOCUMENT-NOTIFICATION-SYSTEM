package com.document.notification.system.notification.service.adapter;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Semaphore;

/**
 * Token Bucket rate limiter for controlling email sending throughput.
 * <p>
 * Uses a Semaphore as the token bucket: each permit represents a token.
 * A scheduled refill adds tokens back at a fixed rate, ensuring the system
 * never exceeds the configured emails-per-second.
 * <p>
 * The acquire method blocks (never fails) until a token is available,
 * so emails queue gracefully under load instead of being rejected.
 *
 * @author Ivan Camilo Rincon Saavedra
 * @version 1.0
 */
@Slf4j
public class EmailRateLimiter {

    private final Semaphore tokens;
    private final int maxTokens;
    private final long refillIntervalMs;

    private volatile boolean running = true;

    public EmailRateLimiter(int maxTokens, long refillIntervalMs) {
        this.maxTokens = maxTokens;
        this.refillIntervalMs = refillIntervalMs;
        this.tokens = new Semaphore(maxTokens);
        startRefillDaemon();
    }

    /**
     * Blocks until a token is available. Never rejects — just waits.
     */
    public void acquire() throws InterruptedException {
        tokens.acquire();
    }

    /**
     * Stops the refill daemon thread on shutdown.
     */
    public void shutdown() {
        running = false;
    }

    private void startRefillDaemon() {
        Thread refillThread = new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(refillIntervalMs);
                    int currentAvailable = tokens.availablePermits();
                    int tokensToAdd = maxTokens - currentAvailable;
                    if (tokensToAdd > 0) {
                        tokens.release(tokensToAdd);
                        log.debug("Refilled {} email tokens, available: {}", tokensToAdd, tokens.availablePermits());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "email-rate-limiter-refill");
        refillThread.setDaemon(true);
        refillThread.start();
    }
}
