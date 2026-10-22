package com.apexarena.leaderboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;

import com.apexarena.leaderboard.cache.LeaderboardCache;
import com.apexarena.leaderboard.cache.LeaderboardCache.CachedRank;
import com.apexarena.leaderboard.cache.ScoreSubmittedEvent;
import com.apexarena.leaderboard.config.GameProperties;
import com.apexarena.leaderboard.domain.Board;
import com.apexarena.leaderboard.domain.Difficulty;
import com.apexarena.leaderboard.domain.GameMode;
import com.apexarena.leaderboard.domain.Score;
import com.apexarena.leaderboard.dto.LeaderboardResponse;
import com.apexarena.leaderboard.dto.ScoreResponse;
import com.apexarena.leaderboard.dto.SubmitScoreRequest;
import com.apexarena.leaderboard.exception.DuplicateFightException;
import com.apexarena.leaderboard.exception.InvalidScoreException;
import com.apexarena.leaderboard.exception.PlayerNotFoundException;
import com.apexarena.leaderboard.repository.PlayerBest;
import com.apexarena.leaderboard.repository.ScoreRepository;

@ExtendWith(MockitoExtension.class)
class LeaderboardServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-22T10:00:00Z");

    @Mock ScoreRepository repository;
    @Mock LeaderboardCache cache;
    @Mock ApplicationEventPublisher events;

    private LeaderboardService service;

    @BeforeEach
    void setUp() {
        GameProperties props = new GameProperties(List.of("zapret", "golmoss"), 99);
        service = new LeaderboardService(repository, new ScoreCalculator(props), new ScoreValidator(3), props,
                Clock.fixed(NOW, ZoneOffset.UTC), cache, events);
    }

    private static SubmitScoreRequest fight(String player, String monster, boolean won, int hp, double secs) {
        return new SubmitScoreRequest(player, monster, Difficulty.HARD, GameMode.QUICK, won, hp, secs, "fight-1");
    }

    @Test
    void submitComputesPointsSavesAndPublishesEvent() {
        when(repository.save(any())).thenAnswer(inv -> ((Score) inv.getArgument(0)).withId(42L));
        when(repository.bestPoints(Board.overall(), "aish")).thenReturn(Optional.of(537));
        when(repository.countPlayersAbove(Board.overall(), 537)).thenReturn(1L);

        ScoreResponse res = service.submit(fight("Aish", "Zapret", true, 80, 30));

        ArgumentCaptor<Score> saved = ArgumentCaptor.forClass(Score.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().playerName()).isEqualTo("aish");
        assertThat(saved.getValue().monster()).isEqualTo("zapret");
        assertThat(saved.getValue().points()).isEqualTo(537);
        assertThat(saved.getValue().submittedAt()).isEqualTo(NOW);
        verify(events).publishEvent(new ScoreSubmittedEvent(saved.getValue().withId(42L)));
        assertThat(res.id()).isEqualTo(42L);
        assertThat(res.overallRank()).isEqualTo(2);
    }

    @Test
    void rejectsUnknownMonster() {
        assertThatThrownBy(() -> service.submit(fight("aish", "godzilla", true, 50, 30)))
                .isInstanceOf(InvalidScoreException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void rejectsImpossibleWin() {
        assertThatThrownBy(() -> service.submit(fight("aish", "zapret", true, 0, 30)))
                .isInstanceOf(InvalidScoreException.class).hasMessageContaining("0 HP");
        assertThatThrownBy(() -> service.submit(fight("aish", "zapret", true, 50, 1)))
                .isInstanceOf(InvalidScoreException.class).hasMessageContaining("seconds");
    }

    @Test
    void rejectsReplayedFightId() {
        when(repository.existsByFightId("fight-1")).thenReturn(true);
        assertThatThrownBy(() -> service.submit(fight("aish", "zapret", true, 50, 30)))
                .isInstanceOf(DuplicateFightException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void uniqueIndexViolationAlsoBecomesDuplicate() {
        when(repository.save(any())).thenThrow(new DataIntegrityViolationException("uk_scores_fight_id"));
        assertThatThrownBy(() -> service.submit(fight("aish", "zapret", true, 50, 30)))
                .isInstanceOf(DuplicateFightException.class);
        verify(events, never()).publishEvent(any());
    }

    @Test
    void topUsesCacheWhenAvailableAndRanksTiesTogether() {
        when(cache.top(Board.overall(), 4)).thenReturn(Optional.of(List.of(
                new PlayerBest("zed", 900), new PlayerBest("amy", 900), new PlayerBest("bob", 500),
                new PlayerBest("cat", 100))));

        LeaderboardResponse res = service.top(Board.overall(), 4);

        assertThat(res.entries()).extracting("rank", "playerName")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(1, "amy"),
                        org.assertj.core.groups.Tuple.tuple(1, "zed"),
                        org.assertj.core.groups.Tuple.tuple(3, "bob"),
                        org.assertj.core.groups.Tuple.tuple(4, "cat"));
        verify(repository, never()).topPlayers(any(), anyInt());
    }

    @Test
    void topFallsBackToDatabaseWhenCacheUnavailable() {
        when(cache.top(any(), anyInt())).thenReturn(Optional.empty());
        when(repository.topPlayers(Board.of(Difficulty.BOSS), 10)).thenReturn(List.of(new PlayerBest("aish", 936)));

        LeaderboardResponse res = service.top(Board.of(Difficulty.BOSS), 10);

        assertThat(res.board()).isEqualTo("difficulty:boss");
        assertThat(res.entries()).hasSize(1);
    }

    @Test
    void rankFromCache() {
        when(cache.rank(Board.overall(), "aish")).thenReturn(Optional.of(new CachedRank(936, 0, 5)));
        var r = service.rank("AISH", Board.overall());
        assertThat(r.rank()).isEqualTo(1);
        assertThat(r.totalPlayers()).isEqualTo(5);
        verify(repository, never()).bestPoints(any(), any());
    }

    @Test
    void rankNotFoundFromCacheAndDatabase() {
        when(cache.rank(any(), any())).thenReturn(Optional.of(new CachedRank(null, 0, 5)));
        assertThatThrownBy(() -> service.rank("ghost", Board.overall())).isInstanceOf(PlayerNotFoundException.class);

        when(cache.rank(any(), any())).thenReturn(Optional.empty());
        when(repository.bestPoints(any(), any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.rank("ghost", Board.overall())).isInstanceOf(PlayerNotFoundException.class);
    }
}
