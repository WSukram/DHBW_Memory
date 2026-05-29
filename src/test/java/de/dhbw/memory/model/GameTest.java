package de.dhbw.memory.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Game}: flip outcomes, score tracking, player switching, and end-game detection.
 *
 * @author Markus Wenninger
 */
class GameTest {

    private Board board;
    private Player alice;
    private Player bob;
    private Game game;

    private TestBoards.Pair match;
    private TestBoards.Pair mismatch;

    @BeforeEach
    void setUp() {
        board = new Board(4, Theme.CRYPTO);
        board.shuffle(0L);
        alice = new Player("Alice");
        bob = new Player("Bob");
        game = new Game(List.of(alice, bob), board);

        match = TestBoards.locateMatch(board);
        mismatch = TestBoards.locateMismatch(board, match);
    }

    @Test
    void firstFlipReturnsFirstFlip() {
        assertEquals(FlipResult.FIRST_FLIP, game.flip(match.a()));
    }

    @Test
    void firstFlipLeavesOnlyOneCardFaceUp() {
        game.flip(match.a());
        long faceUpCount = board.getCards().stream().filter(Card::isFaceUp).count();
        assertEquals(1, faceUpCount);
    }

    @Test
    void secondFlipMatchingPairReturnsMatch() {
        game.flip(match.a());
        assertEquals(FlipResult.MATCH, game.flip(match.b()));
    }

    @Test
    void matchIncreasesActivePlayerScore() {
        game.flip(match.a());
        game.flip(match.b());
        assertEquals(1, alice.getScore());
        assertEquals(0, bob.getScore());
    }

    @Test
    void matchDoesNotSwitchActivePlayer() {
        game.flip(match.a());
        game.flip(match.b());
        assertSame(alice, game.getActivePlayer());
    }

    @Test
    void matchedCardsAreMarkedMatched() {
        game.flip(match.a());
        game.flip(match.b());
        assertTrue(board.getCardAt(match.a()).isMatched());
        assertTrue(board.getCardAt(match.b()).isMatched());
    }

    @Test
    void secondFlipNonMatchingReturnsNoMatch() {
        game.flip(mismatch.a());
        assertEquals(FlipResult.NO_MATCH, game.flip(mismatch.b()));
    }

    @Test
    void noMatchDoesNotChangeScore() {
        game.flip(mismatch.a());
        game.flip(mismatch.b());
        assertEquals(0, alice.getScore());
    }

    @Test
    void noMatchActivePlayerUnchangedUntilResetMismatch() {
        game.flip(mismatch.a());
        game.flip(mismatch.b());
        assertSame(alice, game.getActivePlayer());
    }

    @Test
    void resetMismatchFlipsCardsFaceDown() {
        game.flip(mismatch.a());
        game.flip(mismatch.b());
        game.resetMismatch();
        assertFalse(board.getCardAt(mismatch.a()).isFaceUp());
        assertFalse(board.getCardAt(mismatch.b()).isFaceUp());
    }

    @Test
    void resetMismatchSwitchesPlayer() {
        game.flip(mismatch.a());
        game.flip(mismatch.b());
        game.resetMismatch();
        assertSame(bob, game.getActivePlayer());
    }

    @Test
    void totalTurnsIncreasesOnSecondFlip() {
        game.flip(match.a());
        game.flip(match.b());
        assertEquals(1, game.getTotalTurns());
    }

    @Test
    void isFinishedFalseWhileUnmatchedCardsRemain() {
        game.flip(match.a());
        game.flip(match.b());
        assertFalse(game.isFinished());
    }

    @Test
    void isFinishedTrueWhenAllCardsMatched() {
        board.getCards().forEach(Card::match);
        assertTrue(game.isFinished());
    }

    @Test
    void winnerIsHigherScoringPlayer() {
        // Manually set the scores so the test does not depend on the shuffle:
        // Alice 5 pairs, Bob 3 pairs → Alice wins.
        for (int i = 0; i < 5; i++) alice.addPoint();
        for (int i = 0; i < 3; i++) bob.addPoint();
        board.getCards().forEach(Card::match);
        assertSame(alice, game.winner());
        assertFalse(game.isTie());
    }

    @Test
    void tieWhenScoresAreEqual() {
        alice.addPoint();
        bob.addPoint();
        board.getCards().forEach(Card::match);
        assertTrue(game.isTie());
        assertNull(game.winner());
    }

    @Test
    void singlePlayerAlwaysWins() {
        Board soloBoard = new Board(4, Theme.CRYPTO);
        soloBoard.shuffle(0L);
        Player solo = new Player("Solo");
        Game soloGame = new Game(List.of(solo), soloBoard);
        soloBoard.getCards().forEach(Card::match);
        assertSame(solo, soloGame.winner());
        assertFalse(soloGame.isTie());
    }

    @Test
    void flipAlreadyMatchedCardReturnsInvalid() {
        game.flip(match.a());
        game.flip(match.b());
        assertEquals(FlipResult.INVALID, game.flip(match.a()));
    }

    @Test
    void flipAlreadyFaceUpCardReturnsInvalid() {
        game.flip(mismatch.a());
        assertEquals(FlipResult.INVALID, game.flip(mismatch.a()));
    }

    @Test
    void emptyPlayerListThrows() {
        Board emptyBoard = new Board(4, Theme.CRYPTO);
        emptyBoard.shuffle(0L);
        assertThrows(IllegalArgumentException.class, () -> new Game(List.of(), emptyBoard));
    }
}
