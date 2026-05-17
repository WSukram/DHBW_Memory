package de.dhbw.memory.controller;

import de.dhbw.memory.model.Board;
import de.dhbw.memory.model.Card;
import de.dhbw.memory.model.FlipResult;
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

    /** Positions of a matching pair in the seed-0 shuffle of a 4×4 CRYPTO board. */
    private int matchPos1;
    private int matchPos2;
    /** Positions of two cards with different motifs. */
    private int mismatchPos1;
    private int mismatchPos2;

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
        assertEquals(FlipResult.FIRST_FLIP, service.flip(matchPos1, null, () -> {}));
    }

    @Test
    void secondFlipMatchReturnsMatch() {
        startAndLocatePositions();
        service.flip(matchPos1, null, () -> {});
        assertEquals(FlipResult.MATCH, service.flip(matchPos2, null, () -> {}));
    }

    @Test
    void matchDoesNotSetWaitingFlag() {
        startAndLocatePositions();
        service.flip(matchPos1, null, () -> {});
        service.flip(matchPos2, null, () -> {});
        assertFalse(service.isWaitingForFlipBack());
    }

    @Test
    void noMatchSetsWaitingFlag() {
        startAndLocatePositions();
        service.flip(mismatchPos1, null, () -> {});
        // Pass null for UI — the scheduled task will NPE only when it fires (1500 ms later),
        // not during the flip call itself. We cancel that by calling shutdown immediately after.
        service.flip(mismatchPos2, null, () -> {});
        assertTrue(service.isWaitingForFlipBack());
        service.shutdown(); // cancel the scheduled task so it doesn't fire during cleanup
    }

    @Test
    void flipBlockedWhileWaiting() {
        startAndLocatePositions();
        service.flip(mismatchPos1, null, () -> {});
        service.flip(mismatchPos2, null, () -> {});
        // While the delay timer is running, any additional flip must return INVALID.
        assertEquals(FlipResult.INVALID, service.flip(0, null, () -> {}));
        service.shutdown();
    }

    @Test
    void startGameResetsWaitingFlag() {
        startAndLocatePositions();
        service.flip(mismatchPos1, null, () -> {});
        service.flip(mismatchPos2, null, () -> {});
        assertTrue(service.isWaitingForFlipBack());
        // Starting a new game must clear the flag so the board is immediately playable.
        service.startGame(List.of("Alice"), 4, Theme.CRYPTO);
        assertFalse(service.isWaitingForFlipBack());
        service.shutdown();
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    /**
     * Calls {@code startGame} with a fixed seed board and locates a matching pair
     * and a mismatching pair so tests are independent of the shuffle result.
     */
    private void startAndLocatePositions() {
        // startGame uses System.currentTimeMillis() for the seed, so we instead
        // build the board manually and replace the game via the service's own method.
        // Easier: just call startGame (random seed) and locate pairs dynamically.
        service.startGame(List.of("Alice", "Bob"), 4, Theme.CRYPTO);

        List<Card> cards = service.getGame().getBoard().getCards();
        matchPos1 = -1; matchPos2 = -1;
        mismatchPos1 = -1; mismatchPos2 = -1;

        for (int i = 0; i < cards.size() && matchPos2 == -1; i++) {
            for (int j = i + 1; j < cards.size() && matchPos2 == -1; j++) {
                if (cards.get(i).getMotif().equals(cards.get(j).getMotif())) {
                    matchPos1 = i; matchPos2 = j;
                }
            }
        }
        for (int i = 0; i < cards.size() && mismatchPos2 == -1; i++) {
            for (int j = i + 1; j < cards.size() && mismatchPos2 == -1; j++) {
                if (!cards.get(i).getMotif().equals(cards.get(j).getMotif())
                        && i != matchPos1 && i != matchPos2
                        && j != matchPos1 && j != matchPos2) {
                    mismatchPos1 = i; mismatchPos2 = j;
                }
            }
        }
    }
}
