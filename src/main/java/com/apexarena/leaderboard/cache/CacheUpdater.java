package com.apexarena.leaderboard.cache;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Updates the cache only AFTER the database transaction commits. If the insert rolled back,
 * the cache must not show a score that does not exist.
 */
@Component
public class CacheUpdater {

    private final LeaderboardCache cache;

    public CacheUpdater(LeaderboardCache cache) {
        this.cache = cache;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onScoreSubmitted(ScoreSubmittedEvent event) {
        cache.record(event.score());
    }
}
