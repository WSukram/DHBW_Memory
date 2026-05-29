package de.dhbw.memory.model;

import java.util.List;

/**
 * Shared test helpers for locating card positions on a shuffled board.
 * Keeps {@link GameTest} and {@link de.dhbw.memory.controller.GameServiceTest}
 * independent of the actual shuffle order.
 *
 * @author Markus Wenninger
 */
public final class TestBoards {

    private TestBoards() {
    }

    /** Two positions whose cards share a motif. */
    public record Pair(int a, int b) {
    }

    /** Returns the first pair of indices whose cards have the same motif. */
    public static Pair locateMatch(Board board) {
        List<Card> cards = board.getCards();
        for (int i = 0; i < cards.size(); i++) {
            for (int j = i + 1; j < cards.size(); j++) {
                if (cards.get(i).getMotif().equals(cards.get(j).getMotif())) {
                    return new Pair(i, j);
                }
            }
        }
        throw new IllegalStateException("Board has no matching pair — should never happen.");
    }

    /** Returns the first pair of indices with different motifs, skipping the excluded positions. */
    public static Pair locateMismatch(Board board, Pair exclude) {
        List<Card> cards = board.getCards();
        for (int i = 0; i < cards.size(); i++) {
            if (i == exclude.a() || i == exclude.b()) continue;
            for (int j = i + 1; j < cards.size(); j++) {
                if (j == exclude.a() || j == exclude.b()) continue;
                if (!cards.get(i).getMotif().equals(cards.get(j).getMotif())) {
                    return new Pair(i, j);
                }
            }
        }
        throw new IllegalStateException("Board has no mismatching pair — should never happen.");
    }
}
