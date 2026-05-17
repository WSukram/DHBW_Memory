package de.dhbw.memory.model;

/**
 * Represents a player participating in the game, identified by name and tracked by score.
 *
 * @author Markus Wenninger
 */
public class Player {

    private final String name;
    private int score;

    /**
     * Creates a player with zero score.
     *
     * @param name display name shown in the UI
     */
    public Player(String name) {
        this.name = name;
        this.score = 0;
    }

    /** Returns the player's display name. */
    public String getName() {
        return name;
    }

    /** Returns the number of pairs this player has matched. */
    public int getScore() {
        return score;
    }

    /** Increments the score by one (called when this player matches a pair). */
    public void addPoint() {
        score++;
    }
}
