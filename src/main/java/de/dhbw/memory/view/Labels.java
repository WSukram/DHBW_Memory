package de.dhbw.memory.view;

/**
 * Tiny formatter helpers shared across the view layer.
 *
 * <p>Lives in {@code view} rather than {@code model} so the model package
 * stays free of any user-facing string formatting (German/English wording,
 * pluralisation, …) and remains swappable behind another UI.</p>
 *
 * @author Markus Wenninger
 */
public final class Labels {

    private Labels() {
    }

    /**
     * Returns a grammatically correct "N pair" / "N pairs" label.
     *
     * @param score number of matched pairs
     * @return e.g. {@code "1 pair"} or {@code "3 pairs"}
     */
    public static String pairsLabel(int score) {
        return score == 1 ? "1 pair" : score + " pairs";
    }
}
