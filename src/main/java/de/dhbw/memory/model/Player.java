package de.dhbw.memory.model;

/**
 * Represents a player participating in the game, identified by name and tracked by score.
 *
 * @author Markus Wenninger
 */
public class Player {

    private final String name;
    private int score;
    private int turns;

    /**
     * Creates a player with zero score and zero turns.
     *
     * @param name display name shown in the UI
     */
    public Player(String name) {
        this.name = name;
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

    /** Increments the turn counter by one (called after each completed turn). */
    public void addTurn() {
        turns++;
    }

    /** Returns how many complete turns this player has taken. */
    public int getTurns() {
        return turns;
    }
}
