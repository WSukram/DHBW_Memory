package de.dhbw.memory.model;

import java.util.List;

/**
 * Available card themes. Each theme knows its static-resource folder and the full list of
 * 18 motif names (enough for a 6×6 grid; the first 8 are used for a 4×4 grid).
 *
 * @author Markus Wenninger
 */
public enum Theme {

    // First 8 motifs are used for the 4×4 grid; all 18 for 6×6.
    CRYPTO("crypto", List.of(
            // 4×4 core — most recognisable coins
            "btc", "eth", "sol", "bnb", "xrp", "ada", "dot", "doge",
            // 6×6 extras
            "link", "avax", "matic", "ltc", "trx", "uni", "near", "pepe", "sui", "fet"
    )),

    SPACE("space", List.of(
            // 4×4 core — iconic mix of objects and exploration, not just planets
            "rocket", "astronaut", "moon", "saturn", "blackhole", "galaxy", "sun", "mars",
            // 6×6 extras
            "earth", "jupiter", "mercury", "venus", "uranus", "neptune", "comet", "nebula", "satellite", "meteor"
    ));

    private final String folder;
    private final List<String> motifs;

    Theme(String folder, List<String> motifs) {
        this.folder = folder;
        this.motifs = motifs;
    }

    /**
     * Returns the static-resource sub-folder name for this theme (e.g. {@code "crypto"}).
     * Images live at {@code /static/themes/<folder>/<motif>.png}.
     */
    public String getFolder() {
        return folder;
    }

    /**
     * Returns the first {@code (gridSize * gridSize) / 2} motif names for this theme.
     * For a 4×4 grid that is 8 motifs; for a 6×6 grid it is 18.
     *
     * @param gridSize either 4 or 6
     * @return immutable sub-list of motif names
     * @throws IllegalArgumentException if gridSize is not 4 or 6
     */
    public List<String> getMotifsFor(int gridSize) {
        if (gridSize != 4 && gridSize != 6) {
            throw new IllegalArgumentException("Grid size must be 4 or 6, got: " + gridSize);
        }
        int pairs = (gridSize * gridSize) / 2;
        return motifs.subList(0, pairs);
    }
}
