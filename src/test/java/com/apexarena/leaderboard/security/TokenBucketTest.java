package com.apexarena.leaderboard.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TokenBucketTest {

    private static final long SECOND = 1_000_000_000L;

    @Test
    void allowsBurstUpToCapacityThenRejects() {
        TokenBucket b = new TokenBucket(3, 1.0, 0);
        assertThat(b.tryConsume(0)).isZero();
        assertThat(b.tryConsume(0)).isZero();
        assertThat(b.tryConsume(0)).isZero();
        assertThat(b.tryConsume(0)).isEqualTo(1);   // 1 second until the next token
    }

    @Test
    void refillsOverTime() {
        TokenBucket b = new TokenBucket(2, 0.5, 0);  // one token every 2 seconds
        b.tryConsume(0);
        b.tryConsume(0);
        assertThat(b.tryConsume(SECOND)).isPositive();
        assertThat(b.tryConsume(2 * SECOND)).isZero();
    }

    @Test
    void neverExceedsCapacityAfterLongIdle() {
        TokenBucket b = new TokenBucket(2, 1.0, 0);
        long later = 3600 * SECOND;
        assertThat(b.isFull(later)).isTrue();
        assertThat(b.tryConsume(later)).isZero();
        assertThat(b.tryConsume(later)).isZero();
        assertThat(b.tryConsume(later)).isPositive();
    }
}
