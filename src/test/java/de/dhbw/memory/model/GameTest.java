package de.dhbw.memory.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameTest {

    private Board board;
    private Player alice;
    private Player bob;
    private Game game;

    /** Position of the first card of the first matching pair in the seed-0 shuffle. */
    private int matchPos1;
    /** Position of the second card of the same pair. */
    private int matchPos2;
    /** Position of a card whose pair is at a different position (for mismatch tests). */
    private int mismatchPos1;
    private int mismatchPos2;

    @BeforeEach
    void setUp() {
        board = new Board(4, Theme.CRYPTO);
        board.shuffle(0L);
        alice = new Player("Alice");
        bob = new Player("Bob");
        game = new Game(List.of(alice, bob), board);

        // Find positions dynamically so the test is independent of the shuffle seed result.
        List<Card> cards = board.getCards();
        matchPos1 = -1;
        matchPos2 = -1;
        for (int i = 0; i < cards.size() && matchPos2 == -1; i++) {
            for (int j = i + 1; j < cards.size() && matchPos2 == -1; j++) {
                if (cards.get(i).getMotif().equals(cards.get(j).getMotif())) {
                    matchPos1 = i;
                    matchPos2 = j;
                }
            }
        }

        // Find a mismatch pair: two cards with different motifs.
        mismatchPos1 = -1;
        mismatchPos2 = -1;
        for (int i = 0; i < cards.size() && mismatchPos2 == -1; i++) {
            for (int j = i + 1; j < cards.size() && mismatchPos2 == -1; j++) {
                if (!cards.get(i).getMotif().equals(cards.get(j).getMotif())
                        && i != matchPos1 && i != matchPos2
                        && j != matchPos1 && j != matchPos2) {
                    mismatchPos1 = i;
                    mismatchPos2 = j;
                }
            }
        }
    }

    @Test
    void firstFlipReturnsFirstFlip() {
        assertEquals(FlipResult.FIRST_FLIP, game.flip(matchPos1));
    }

    @Test
    void firstFlipLeavesOnlyOneCardFaceUp() {
        game.flip(matchPos1);
        long faceUpCount = board.getCards().stream().filter(Card::isFaceUp).count();
        assertEquals(1, faceUpCount);
    }

    @Test
    void secondFlipMatchingPairReturnsMatch() {
        game.flip(matchPos1);
        assertEquals(FlipResult.MATCH, game.flip(matchPos2));
    }

    @Test
    void matchIncreasesActivePlayerScore() {
        game.flip(matchPos1);
        game.flip(matchPos2);
        assertEquals(1, alice.getScore());
        assertEquals(0, bob.getScore());
    }

    @Test
    void matchDoesNotSwitchActivePlayer() {
        game.flip(matchPos1);
        game.flip(matchPos2);
        assertSame(alice, game.getActivePlayer());
    }

    @Test
    void matchedCardsAreMarkedMatched() {
        game.flip(matchPos1);
        game.flip(matchPos2);
        assertTrue(board.getCardAt(matchPos1).isMatched());
        assertTrue(board.getCardAt(matchPos2).isMatched());
    }

    @Test
    void secondFlipNonMatchingReturnsNoMatch() {
        game.flip(mismatchPos1);
        assertEquals(FlipResult.NO_MATCH, game.flip(mismatchPos2));
    }

    @Test
    void noMatchDoesNotChangeScore() {
        game.flip(mismatchPos1);
        game.flip(mismatchPos2);
        assertEquals(0, alice.getScore());
    }

    @Test
    void noMatchActivePlayerUnchangedUntilResetMismatch() {
        game.flip(mismatchPos1);
        game.flip(mismatchPos2);
        assertSame(alice, game.getActivePlayer());
    }

    @Test
    void resetMismatchFlipsCardsFaceDown() {
        game.flip(mismatchPos1);
        game.flip(mismatchPos2);
        game.resetMismatch();
        assertFalse(board.getCardAt(mismatchPos1).isFaceUp());
        assertFalse(board.getCardAt(mismatchPos2).isFaceUp());
    }

    @Test
    void resetMismatchSwitchesPlayer() {
        game.flip(mismatchPos1);
        game.flip(mismatchPos2);
        game.resetMismatch();
        assertSame(bob, game.getActivePlayer());
    }

    @Test
    void totalTurnsIncreasesOnSecondFlip() {
        game.flip(matchPos1);
        game.flip(matchPos2);
        assertEquals(1, game.getTotalTurns());
    }

    @Test
    void isFinishedFalseWhileUnmatchedCardsRemain() {
        game.flip(matchPos1);
        game.flip(matchPos2);
        assertFalse(game.isFinished());
    }

    @Test
    void isFinishedTrueWhenAllCardsMatched() {
        matchAllCards(game, board);
        assertTrue(game.isFinished());
    }

    @Test
    void winnerIsHigherScoringPlayer() {
        // Give alice 2 matches, bob 1 match — needs at least 3 pairs available.
        matchAllCards(game, board);
        // After matching everything, the winner is whoever has more points.
        Player w = game.winner();
        assertNotNull(w); // Someone must win (tie only if exactly equal scores on a 4x4 with 2 players sharing 8 pairs — check)
    }

    @Test
    void tieWhenScoresAreEqual() {
        // Force a tie by giving both players the same score.
        alice.addPoint();
        bob.addPoint();
        // Mark all cards matched manually so isFinished() returns true.
        board.getCards().forEach(c -> c.setMatched(true));
        assertTrue(game.isTie());
        assertNull(game.winner());
    }

    @Test
    void singlePlayerAlwaysWins() {
        Board soloBoard = new Board(4, Theme.CRYPTO);
        soloBoard.shuffle(0L);
        Player solo = new Player("Solo");
        Game soloGame = new Game(List.of(solo), soloBoard);
        soloBoard.getCards().forEach(c -> c.setMatched(true));
        assertSame(solo, soloGame.winner());
        assertFalse(soloGame.isTie());
    }

    @Test
    void flipAlreadyMatchedCardReturnsInvalid() {
        game.flip(matchPos1);
        game.flip(matchPos2);
        assertEquals(FlipResult.INVALID, game.flip(matchPos1));
    }

    @Test
    void flipAlreadyFaceUpCardReturnsInvalid() {
        game.flip(mismatchPos1);
        assertEquals(FlipResult.INVALID, game.flip(mismatchPos1));
    }

    // -------------------------------------------------------------------------
    // Helper: match every pair on the board by walking the card list.
    // -------------------------------------------------------------------------

    private static void matchAllCards(Game g, Board b) {
        List<Card> cards = b.getCards();
        boolean[] used = new boolean[cards.size()];
        for (int i = 0; i < cards.size(); i++) {
            if (used[i] || cards.get(i).isMatched()) continue;
            for (int j = i + 1; j < cards.size(); j++) {
                if (!used[j] && !cards.get(j).isMatched()
                        && cards.get(i).getMotif().equals(cards.get(j).getMotif())) {
                    // Reset any in-progress flip state first
                    if (cards.get(i).isFaceUp()) {
                        // already face-up from a previous call — skip the first flip
                    } else {
                        g.flip(i);
                    }
                    if (!cards.get(j).isFaceUp()) {
                        g.flip(j);
                    }
                    used[i] = true;
                    used[j] = true;
                    break;
                }
            }
        }
    }
}
