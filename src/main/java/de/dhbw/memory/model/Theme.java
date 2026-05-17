package de.dhbw.memory.model;

import java.util.List;

/**
 * Available card themes. Each theme knows its static-resource folder and the full list of
 * 18 motif names (enough for a 6×6 grid; the first 8 are used for a 4×4 grid).
 *
 * @author Markus Wenninger
 */
public enum Theme {

    CRYPTO("crypto", List.of(
            "btc", "eth", "bnb", "sol", "ada", "dot", "matic", "link",
            "avax", "ltc", "xrp", "doge", "shib", "uni", "atom", "xlm", "algo", "fil"
    )),

    SPACE("space", List.of(
            "sun", "moon", "mars", "saturn", "jupiter", "earth", "mercury", "venus",
            "uranus", "neptune", "comet", "meteor", "galaxy", "nebula", "blackhole", "rocket", "astronaut", "satellite"
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
