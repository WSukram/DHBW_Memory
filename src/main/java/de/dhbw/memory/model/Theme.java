package de.dhbw.memory.model;

import java.util.List;
import java.util.Map;

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

    LANGUAGES("languages", List.of(
            // 4×4 core — the eight most ubiquitous languages a student encounters
            "java", "python", "javascript", "c", "html5", "sql", "typescript", "assembly",
            // 6×6 extras
            "rust", "go", "swift", "kotlin", "ruby", "csharp", "php", "bash", "cpp", "css3"
    )),

    SPACE("space", List.of(
            // 4×4 core
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
     * Images live at {@code src/main/resources/static/images/<folder>/<motif>.svg}
     * (served at {@code /images/<folder>/<motif>.svg}).
     *
     * @return the folder name used to construct image URLs
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

    /**
     * Returns the human-readable display name for a motif id (e.g. {@code "btc" → "Bitcoin"}).
     * Used by the view to render a friendly label on each card. If the motif is unknown,
     * the id is returned as-is so we never crash on a missing entry.
     *
     * @param motif the motif id from {@link #getMotifsFor(int)}
     * @return the display name suitable for showing to the user
     */
    public String getDisplayName(String motif) {
        Map<String, String> table = switch (this) {
            case CRYPTO -> CRYPTO_DISPLAY_NAMES;
            case LANGUAGES -> LANGUAGE_DISPLAY_NAMES;
            case SPACE -> SPACE_DISPLAY_NAMES;
        };
        return table.getOrDefault(motif, motif);
    }

    private static final Map<String, String> CRYPTO_DISPLAY_NAMES = Map.ofEntries(
            Map.entry("btc",   "Bitcoin"),
            Map.entry("eth",   "Ethereum"),
            Map.entry("sol",   "Solana"),
            Map.entry("bnb",   "BNB"),
            Map.entry("xrp",   "XRP"),
            Map.entry("ada",   "Cardano"),
            Map.entry("dot",   "Polkadot"),
            Map.entry("doge",  "Dogecoin"),
            Map.entry("link",  "Chainlink"),
            Map.entry("avax",  "Avalanche"),
            Map.entry("matic", "Polygon"),
            Map.entry("ltc",   "Litecoin"),
            Map.entry("trx",   "TRON"),
            Map.entry("uni",   "Uniswap"),
            Map.entry("near",  "NEAR"),
            Map.entry("pepe",  "Pepe"),
            Map.entry("sui",   "Sui"),
            Map.entry("fet",   "Fetch.ai")
    );

    private static final Map<String, String> LANGUAGE_DISPLAY_NAMES = Map.ofEntries(
            Map.entry("java",       "Java"),
            Map.entry("python",     "Python"),
            Map.entry("javascript", "JavaScript"),
            Map.entry("c",          "C"),
            Map.entry("html5",      "HTML5"),
            Map.entry("sql",        "SQL"),
            Map.entry("typescript", "TypeScript"),
            Map.entry("assembly",   "Assembly"),
            Map.entry("rust",       "Rust"),
            Map.entry("go",         "Go"),
            Map.entry("swift",      "Swift"),
            Map.entry("kotlin",     "Kotlin"),
            Map.entry("ruby",       "Ruby"),
            Map.entry("csharp",     "C#"),
            Map.entry("php",        "PHP"),
            Map.entry("bash",       "Bash"),
            Map.entry("cpp",        "C++"),
            Map.entry("css3",       "CSS3")
    );

    private static final Map<String, String> SPACE_DISPLAY_NAMES = Map.ofEntries(
            Map.entry("rocket",    "Rocket"),
            Map.entry("astronaut", "Astronaut"),
            Map.entry("moon",      "Moon"),
            Map.entry("saturn",    "Saturn"),
            Map.entry("blackhole", "Black Hole"),
            Map.entry("galaxy",    "Galaxy"),
            Map.entry("sun",       "Sun"),
            Map.entry("mars",      "Mars"),
            Map.entry("earth",     "Earth"),
            Map.entry("jupiter",   "Jupiter"),
            Map.entry("mercury",   "Mercury"),
            Map.entry("venus",     "Venus"),
            Map.entry("uranus",    "Uranus"),
            Map.entry("neptune",   "Neptune"),
            Map.entry("comet",     "Comet"),
            Map.entry("nebula",    "Nebula"),
            Map.entry("satellite", "Satellite"),
            Map.entry("meteor",    "Meteor")
    );
}
