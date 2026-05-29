package de.dhbw.memory.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * The game board: a flat list of cards whose index represents the grid position shown in the UI.
 * Cards are created in pair order and randomised via {@link #shuffle(long)}.
 *
 * @author Markus Wenninger
 */
public class Board {

    private final int size;
    private final List<Card> cards;

    /**
     * Creates an unshuffled board for the given grid size and theme.
     * Cards are arranged as consecutive pairs: [motif0-a, motif0-b, motif1-a, motif1-b, …].
     * Call {@link #shuffle(long)} before starting a game.
     *
     * @param size  grid edge length — must be 4 or 6
     * @param theme determines which motif names are used
     */
    public Board(int size, Theme theme) {
        if (size != 4 && size != 6) {
            throw new IllegalArgumentException("Grid size must be 4 or 6, got: " + size);
        }
        this.size = size;
        List<String> motifs = theme.getMotifsFor(size);
        List<Card> list = new ArrayList<>(size * size);
        int id = 0;
        for (String motif : motifs) {
            list.add(new Card(id++, motif));
            list.add(new Card(id++, motif));
        }
        this.cards = list;
    }

    /**
     * Shuffles the cards using a seeded {@link Random}, making the result deterministic.
     * Pass the same seed in tests to get a known card order.
     *
     * @param seed any long value; the same seed always produces the same order
     */
    public void shuffle(long seed) {
        Collections.shuffle(cards, new Random(seed));
    }

    /**
     * Returns an unmodifiable view of the card list. The index in this list is the grid position
     * that the view renders.
     *
     * @return unmodifiable list of all cards in their current order
     */
    public List<Card> getCards() {
        return Collections.unmodifiableList(cards);
    }

    /**
     * Returns the card at grid position {@code position}.
     *
     * @param position 0-based index into the shuffled card list
     * @return the card at the given position
     * @throws IndexOutOfBoundsException if position is out of range
     */
    public Card getCardAt(int position) {
        return cards.get(position);
    }

    /**
     * Returns the grid edge length (4 or 6).
     *
     * @return the grid edge length used to construct this board
     */
    public int getSize() {
        return size;
    }
}
