package de.dhbw.memory.model;

/**
 * Outcome returned by {@link Game#flip(int)} so the controller knows how to react.
 *
 * @author Markus Wenninger
 */
public enum FlipResult {

    /** First card of this turn was revealed; waiting for the second flip. */
    FIRST_FLIP,

    /** Both cards revealed and motifs match; active player scored and keeps the turn. */
    MATCH,

    /**
     * Both cards revealed but motifs differ; controller must schedule a flip-back delay
     * (≈1 500 ms) and then call {@link Game#resetMismatch()} to hide the cards and switch player.
     */
    NO_MATCH,

    /** Card is already matched or already face-up; the click has no effect. */
    INVALID
}
