package de.dhbw.memory.model;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BoardTest {

    @Test
    void fourByFourBoardHasSixteenCards() {
        Board board = new Board(4, Theme.CRYPTO);
        assertEquals(16, board.getCards().size());
    }

    @Test
    void sixBySixBoardHasThirtySixCards() {
        Board board = new Board(6, Theme.CRYPTO);
        assertEquals(36, board.getCards().size());
    }

    @Test
    void shuffleIsDeterministicWithSameSeed() {
        Board a = new Board(4, Theme.CRYPTO);
        Board b = new Board(4, Theme.CRYPTO);
        a.shuffle(42L);
        b.shuffle(42L);
        List<Card> cardsA = a.getCards();
        List<Card> cardsB = b.getCards();
        for (int i = 0; i < cardsA.size(); i++) {
            assertEquals(cardsA.get(i).getId(), cardsB.get(i).getId());
        }
    }

    @Test
    void shuffleDiffersWithDifferentSeeds() {
        Board a = new Board(4, Theme.CRYPTO);
        Board b = new Board(4, Theme.CRYPTO);
        a.shuffle(0L);
        b.shuffle(1L);
        boolean anyDiff = false;
        List<Card> cardsA = a.getCards();
        List<Card> cardsB = b.getCards();
        for (int i = 0; i < cardsA.size(); i++) {
            if (cardsA.get(i).getId() != cardsB.get(i).getId()) {
                anyDiff = true;
                break;
            }
        }
        assertTrue(anyDiff, "different seeds should produce different card orders");
    }

    @Test
    void eachMotifAppearsTwice() {
        Board board = new Board(4, Theme.CRYPTO);
        Map<String, Integer> counts = new HashMap<>();
        for (Card card : board.getCards()) {
            counts.merge(card.getMotif(), 1, Integer::sum);
        }
        counts.values().forEach(count -> assertEquals(2, count, "every motif must appear exactly twice"));
    }

    @Test
    void getCardAtReturnsCorrectCard() {
        Board board = new Board(4, Theme.CRYPTO);
        board.shuffle(0L);
        Card expected = board.getCards().get(3);
        assertSame(expected, board.getCardAt(3));
    }

    @Test
    void invalidGridSizeThrows() {
        assertThrows(IllegalArgumentException.class, () -> new Board(5, Theme.CRYPTO));
    }
}
