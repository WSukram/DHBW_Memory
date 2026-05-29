package de.dhbw.memory.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Card}: initial state, flip behaviour, and match marking.
 *
 * @author Markus Wenninger
 */
class CardTest {

    @Test
    void newCardIsFaceDown() {
        Card card = new Card(0, "btc");
        assertFalse(card.isFaceUp());
    }

    @Test
    void newCardIsNotMatched() {
        Card card = new Card(0, "btc");
        assertFalse(card.isMatched());
    }

    @Test
    void flipMakesCardFaceUp() {
        Card card = new Card(0, "btc");
        card.flip();
        assertTrue(card.isFaceUp());
    }

    @Test
    void doubleFlipReturnsFaceDown() {
        Card card = new Card(0, "btc");
        card.flip();
        card.flip();
        assertFalse(card.isFaceUp());
    }

    @Test
    void matchMarksMatchedAndFaceUp() {
        Card card = new Card(0, "btc");
        card.match();
        assertTrue(card.isMatched());
        assertTrue(card.isFaceUp());
    }

    @Test
    void matchIsIdempotent() {
        Card card = new Card(0, "btc");
        card.match();
        card.match();
        assertTrue(card.isMatched());
        assertTrue(card.isFaceUp());
    }

    @Test
    void idAndMotifAreReturnedCorrectly() {
        Card card = new Card(7, "eth");
        assertEquals(7, card.getId());
        assertEquals("eth", card.getMotif());
    }
}
