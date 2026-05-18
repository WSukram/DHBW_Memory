package de.dhbw.memory.controller;

import de.dhbw.memory.model.Board;
import de.dhbw.memory.model.FlipResult;
import de.dhbw.memory.model.TestBoards;
import de.dhbw.memory.model.Theme;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link GameService}. These run without a Spring context or a
 * real Vaadin {@code UI} — only the synchronous state transitions are verified
 * here. The 1 500 ms timer itself is a JDK {@code ScheduledExecutorService}
 * guarantee and does not need to be re-tested.
 *
 * @author Markus Wenninger
 */
class GameServiceTest {

    private GameService service;

    private TestBoards.Pair match;
    private TestBoards.Pair mismatch;

    @BeforeEach
    void setUp() {
        service = new GameService();
    }

    @Test
    void flipBeforeStartReturnsInvalid() {
        // game is null — any flip must be a no-op
        assertEquals(FlipResult.INVALID, service.flip(0, null, () -> {}));
    }

    @Test
    void isWaitingForFlipBackStartsFalse() {
        assertFalse(service.isWaitingForFlipBack());
    }

    @Test
    void getGameIsNullBeforeStart() {
        assertNull(service.getGame());
    }

    @Test
    void startGameCreatesNonNullGame() {
        service.startGame(List.of("Alice"), 4, Theme.CRYPTO);
        assertNotNull(service.getGame());
    }

    @Test
    void startGameCreatesUnfinishedGame() {
        service.startGame(List.of("Alice"), 4, Theme.CRYPTO);
        assertFalse(service.getGame().isFinished());
    }

    @Test
    void startGameWithTwoPlayers() {
        service.startGame(List.of("Alice", "Bob"), 4, Theme.CRYPTO);
        assertEquals(2, service.getGame().getPlayers().size());
    }

    @Test
    void firstFlipReturnsFirstFlip() {
        startAndLocatePositions();
        assertEquals(FlipResult.FIRST_FLIP, service.flip(match.a(), null, () -> {}));
    }

    @Test
    void secondFlipMatchReturnsMatch() {
        startAndLocatePositions();
        service.flip(match.a(), null, () -> {});
        assertEquals(FlipResult.MATCH, service.flip(match.b(), null, () -> {}));
    }

    @Test
    void matchDoesNotSetWaitingFlag() {
        startAndLocatePositions();
        service.flip(match.a(), null, () -> {});
        service.flip(match.b(), null, () -> {});
        assertFalse(service.isWaitingForFlipBack());
    }

    @Test
    void noMatchSetsWaitingFlag() {
        startAndLocatePositions();
        service.flip(mismatch.a(), null, () -> {});
        // Pass null for UI — the scheduled task will NPE only when it fires (1500 ms later),
        // not during the flip call itself. Cancel it via shutdown() immediately after.
        service.flip(mismatch.b(), null, () -> {});
        assertTrue(service.isWaitingForFlipBack());
        service.shutdown();
    }

    @Test
    void flipBlockedWhileWaiting() {
        startAndLocatePositions();
        service.flip(mismatch.a(), null, () -> {});
        service.flip(mismatch.b(), null, () -> {});
        // While the delay timer is running, any additional flip must return INVALID.
        assertEquals(FlipResult.INVALID, service.flip(0, null, () -> {}));
        service.shutdown();
    }

    @Test
    void startGameResetsWaitingFlag() {
        startAndLocatePositions();
        service.flip(mismatch.a(), null, () -> {});
        service.flip(mismatch.b(), null, () -> {});
        assertTrue(service.isWaitingForFlipBack());
        // Starting a new game must clear the flag so the board is immediately playable.
        service.startGame(List.of("Alice"), 4, Theme.CRYPTO);
        assertFalse(service.isWaitingForFlipBack());
        service.shutdown();
    }

    /**
     * Calls {@code startGame} and locates a matching pair plus a disjoint mismatching
     * pair so the tests are independent of the random shuffle.
     */
    private void startAndLocatePositions() {
        service.startGame(List.of("Alice", "Bob"), 4, Theme.CRYPTO);
        Board board = service.getGame().getBoard();
        match = TestBoards.locateMatch(board);
        mismatch = TestBoards.locateMismatch(board, match);
    }
}
