package com.apexarena.leaderboard.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * HMAC-SHA256 over {@code "<timestamp>.<raw body>"}, hex encoded.
 * Same scheme the game client uses (see game-client/leaderboard_client.py).
 */
public final class HmacSigner {

    private static final String ALGORITHM = "HmacSHA256";
    private final SecretKeySpec key;

    public HmacSigner(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("HMAC secret must not be empty");
        }
        this.key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM);
    }

    public String sign(String timestamp, byte[] body) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);   // Mac is not thread-safe: one per call
            mac.init(key);
            mac.update(timestamp.getBytes(StandardCharsets.UTF_8));
            mac.update((byte) '.');
            mac.update(body);
            return HexFormat.of().formatHex(mac.doFinal());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Constant-time comparison so an attacker cannot learn the signature byte by byte from timing. */
    public boolean verify(String timestamp, byte[] body, String signatureHex) {
        if (signatureHex == null) {
            return false;
        }
        byte[] expected = sign(timestamp, body).getBytes(StandardCharsets.US_ASCII);
        byte[] actual = signatureHex.trim().toLowerCase().getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(expected, actual);
    }
}
