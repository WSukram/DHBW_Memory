package de.dhbw.memory.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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
    void setMatchedTrueSetsMatchedAndFaceUp() {
        Card card = new Card(0, "btc");
        card.setMatched(true);
        assertTrue(card.isMatched());
        assertTrue(card.isFaceUp());
    }

    @Test
    void setMatchedFalseDoesNotForceCardFaceUp() {
        Card card = new Card(0, "btc");
        card.setMatched(false);
        assertFalse(card.isMatched());
        assertFalse(card.isFaceUp());
    }

    @Test
    void idAndMotifAreReturnedCorrectly() {
        Card card = new Card(7, "eth");
        assertEquals(7, card.getId());
        assertEquals("eth", card.getMotif());
    }
}
