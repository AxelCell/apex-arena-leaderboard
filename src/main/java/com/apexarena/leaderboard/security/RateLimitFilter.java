package com.apexarena.leaderboard.security;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Limits score submissions per client IP with a token bucket. A real fight takes well over
 * ten seconds, so a human never comes close to the default limit (burst 10, 20 per minute).
 *
 * <p>Runs first (before HMAC) so a flood is rejected without spending CPU on crypto.
 * In-memory, so the limit is per app instance; with several instances this would move
 * to Redis (INCR + EXPIRE per window). Behind a proxy, use the X-Forwarded-For client IP.
 */
@Component
@Order(1)
public class RateLimitFilter extends OncePerRequestFilter {

    private final boolean enabled;
    private final int capacity;
    private final double refillPerSecond;
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(@Value("${leaderboard.rate-limit.enabled:true}") boolean enabled,
                           @Value("${leaderboard.rate-limit.capacity:10}") int capacity,
                           @Value("${leaderboard.rate-limit.per-minute:20}") int perMinute) {
        this.enabled = enabled;
        this.capacity = capacity;
        this.refillPerSecond = perMinute / 60.0;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        return !enabled || !SignatureFilter.isScoreSubmission(req);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        long now = System.nanoTime();
        TokenBucket bucket = buckets.computeIfAbsent(req.getRemoteAddr(),
                ip -> new TokenBucket(capacity, refillPerSecond, now));
        long waitSeconds = bucket.tryConsume(now);
        if (waitSeconds > 0) {
            res.setHeader("Retry-After", Long.toString(waitSeconds));
            Problems.write(res, 429, "Too many requests",
                    "Score submission rate limit exceeded, retry in " + waitSeconds + "s", req.getRequestURI());
            return;
        }
        chain.doFilter(req, res);
    }

    /** Forget clients whose bucket has refilled, so the map does not grow forever. */
    @Scheduled(fixedDelayString = "${leaderboard.rate-limit.cleanup-interval:5m}")
    void evictIdle() {
        long now = System.nanoTime();
        buckets.entrySet().removeIf(e -> e.getValue().isFull(now));
    }
}
