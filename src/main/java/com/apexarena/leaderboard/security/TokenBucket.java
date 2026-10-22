package com.apexarena.leaderboard.security;

/**
 * Classic token bucket: holds up to {@code capacity} tokens, refills continuously at
 * {@code refillPerSecond}. Each request takes one token; no token means "too many requests".
 * Allows short bursts (up to capacity) while capping the long-run rate.
 */
public class TokenBucket {

    private final double capacity;
    private final double refillPerNano;
    private double tokens;
    private long lastRefillNanos;

    public TokenBucket(int capacity, double refillPerSecond, long nowNanos) {
        this.capacity = capacity;
        this.refillPerNano = refillPerSecond / 1_000_000_000.0;
        this.tokens = capacity;
        this.lastRefillNanos = nowNanos;
    }

    /** @return 0 if a token was taken, otherwise how many seconds until one is available */
    public synchronized long tryConsume(long nowNanos) {
        refill(nowNanos);
        if (tokens >= 1) {
            tokens -= 1;
            return 0;
        }
        return (long) Math.ceil((1 - tokens) / refillPerNano / 1_000_000_000.0);
    }

    /** True when the bucket is full again, i.e. the client has been idle; safe to forget it. */
    public synchronized boolean isFull(long nowNanos) {
        refill(nowNanos);
        return tokens >= capacity;
    }

    private void refill(long nowNanos) {
        long elapsed = nowNanos - lastRefillNanos;
        if (elapsed > 0) {
            tokens = Math.min(capacity, tokens + elapsed * refillPerNano);
            lastRefillNanos = nowNanos;
        }
    }
}
