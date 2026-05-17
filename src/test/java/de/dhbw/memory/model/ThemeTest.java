package de.dhbw.memory.model;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ThemeTest {

    @Test
    void cryptoFolderIsCrypto() {
        assertEquals("crypto", Theme.CRYPTO.getFolder());
    }

    @Test
    void spaceFolderIsSpace() {
        assertEquals("space", Theme.SPACE.getFolder());
    }

    @Test
    void getMotifsForFourReturnsEightMotifs() {
        List<String> motifs = Theme.CRYPTO.getMotifsFor(4);
        assertEquals(8, motifs.size());
    }

    @Test
    void getMotifsForSixReturnsEighteenMotifs() {
        List<String> motifs = Theme.CRYPTO.getMotifsFor(6);
        assertEquals(18, motifs.size());
    }

    @Test
    void spaceHasEighteenDistinctMotifs() {
        List<String> motifs = Theme.SPACE.getMotifsFor(6);
        assertEquals(18, new HashSet<>(motifs).size(), "all 18 space motifs must be unique");
    }

    @Test
    void cryptoHasEighteenDistinctMotifs() {
        List<String> motifs = Theme.CRYPTO.getMotifsFor(6);
        assertEquals(18, new HashSet<>(motifs).size(), "all 18 crypto motifs must be unique");
    }

    @Test
    void invalidGridSizeThrows() {
        assertThrows(IllegalArgumentException.class, () -> Theme.CRYPTO.getMotifsFor(5));
    }
}
