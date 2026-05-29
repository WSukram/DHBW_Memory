package de.dhbw.memory.model;

/**
 * A single memory card with a unique id, a motif shared with its pair, and mutable state.
 *
 * @author Markus Wenninger
 */
public class Card {

    private final int id;
    private final String motif;
    private boolean faceUp;
    private boolean matched;

    /**
     * Creates a face-down, unmatched card.
     *
     * @param id     unique index on the board (0 … size²−1)
     * @param motif  image name shared with exactly one other card; equality determines a match
     */
    public Card(int id, String motif) {
        this.id = id;
        this.motif = motif;
        this.faceUp = false;
        this.matched = false;
    }

    /**
     * Returns the unique card id.
     *
     * @return 0-based index assigned at board creation
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the motif name used to detect pairs.
     *
     * @return motif id shared with exactly one other card on the board
     */
    public String getMotif() {
        return motif;
    }

    /**
     * Returns true when the card is showing its face.
     *
     * @return {@code true} if the card is currently face-up
     */
    public boolean isFaceUp() {
        return faceUp;
    }

    /**
     * Returns true when this card has been permanently matched.
     *
     * @return {@code true} if the card has been matched and stays face-up
     */
    public boolean isMatched() {
        return matched;
    }

    /** Toggles the face-up state (face-down ↔ face-up). */
    public void flip() {
        faceUp = !faceUp;
    }

    /**
     * Marks the card as permanently matched and keeps it face-up. Idempotent.
     */
    public void match() {
        this.matched = true;
        this.faceUp = true;
    }
}
