package com.apexarena.leaderboard.security;

import java.io.IOException;
import java.time.Clock;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Verifies that a score submission was signed with the shared secret:
 * <pre>
 *   X-Timestamp: 1761130000                      (unix seconds)
 *   X-Signature: hex(HMAC-SHA256(secret, "1761130000." + rawBody))
 * </pre>
 * The timestamp is inside the signature, so it cannot be changed, and requests older than
 * {@code max-skew} are rejected. Together with the unique fightId this stops replays.
 */
@Component
@Order(2)
public class SignatureFilter extends OncePerRequestFilter {

    static final String TIMESTAMP_HEADER = "X-Timestamp";
    static final String SIGNATURE_HEADER = "X-Signature";
    private static final int MAX_BODY_BYTES = 8 * 1024;

    private final boolean enabled;
    private final HmacSigner signer;
    private final long maxSkewSeconds;
    private final Clock clock;

    public SignatureFilter(@Value("${leaderboard.hmac.enabled:true}") boolean enabled,
                           @Value("${leaderboard.hmac.secret}") String secret,
                           @Value("${leaderboard.hmac.max-skew-seconds:300}") long maxSkewSeconds,
                           Clock clock) {
        this.enabled = enabled;
        this.signer = new HmacSigner(secret);
        this.maxSkewSeconds = maxSkewSeconds;
        this.clock = clock;
    }

    static boolean isScoreSubmission(HttpServletRequest req) {
        return "POST".equals(req.getMethod()) && "/api/scores".equals(req.getRequestURI());
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        return !enabled || !isScoreSubmission(req);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        byte[] body = req.getInputStream().readNBytes(MAX_BODY_BYTES + 1);
        if (body.length > MAX_BODY_BYTES) {
            Problems.write(res, 413, "Payload too large", "Body exceeds " + MAX_BODY_BYTES + " bytes",
                    req.getRequestURI());
            return;
        }
        String ts = req.getHeader(TIMESTAMP_HEADER);
        String sig = req.getHeader(SIGNATURE_HEADER);
        if (ts == null || sig == null) {
            unauthorized(res, req, "Missing " + TIMESTAMP_HEADER + " or " + SIGNATURE_HEADER + " header");
            return;
        }
        long sent;
        try {
            sent = Long.parseLong(ts.trim());
        } catch (NumberFormatException e) {
            unauthorized(res, req, "Bad " + TIMESTAMP_HEADER);
            return;
        }
        if (Math.abs(clock.instant().getEpochSecond() - sent) > maxSkewSeconds) {
            unauthorized(res, req, "Request timestamp too old or too far in the future");
            return;
        }
        if (!signer.verify(ts.trim(), body, sig)) {
            unauthorized(res, req, "Invalid signature");
            return;
        }
        chain.doFilter(new CachedBodyRequest(req, body), res);
    }

    private static void unauthorized(HttpServletResponse res, HttpServletRequest req, String detail)
            throws IOException {
        Problems.write(res, 401, "Unauthorized", detail, req.getRequestURI());
    }
}
