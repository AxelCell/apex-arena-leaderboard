package com.apexarena.leaderboard.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class HmacSignerTest {
    private final HmacSigner signer = new HmacSigner("test-secret");
    private final byte[] body = "{\"a\":1}".getBytes(StandardCharsets.UTF_8);

    private static final String PYTHON_VECTOR = "8cb2c3355fca388e9ac2caec004f4d5d7045d74937ab5faad61dc11682247a9f";

    @Test
    void matchesThePythonGameClient() {
        assertThat(signer.sign("1700000000", body)).isEqualTo(PYTHON_VECTOR);
    }

    @Test
    void verifiesCorrectSignatureCaseInsensitively() {
        assertThat(signer.verify("1700000000", body, PYTHON_VECTOR.toUpperCase())).isTrue();
    }

    @Test
    void rejectsTamperedBodyTimestampOrSecret() {
        byte[] tampered = "{\"a\":2}".getBytes(StandardCharsets.UTF_8);
        assertThat(signer.verify("1700000000", tampered, PYTHON_VECTOR)).isFalse();
        assertThat(signer.verify("1700000001", body, PYTHON_VECTOR)).isFalse();
        assertThat(new HmacSigner("other").verify("1700000000", body, PYTHON_VECTOR)).isFalse();
        assertThat(signer.verify("1700000000", body, null)).isFalse();
    }

    @Test
    void refusesEmptySecret() {
        assertThatThrownBy(() -> new HmacSigner(" ")).isInstanceOf(IllegalArgumentException.class);
    }
}
