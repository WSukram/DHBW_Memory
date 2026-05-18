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
    void languagesFolderIsLanguages() {
        assertEquals("languages", Theme.LANGUAGES.getFolder());
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
    void languagesHasEighteenDistinctMotifs() {
        List<String> motifs = Theme.LANGUAGES.getMotifsFor(6);
        assertEquals(18, new HashSet<>(motifs).size(), "all 18 language motifs must be unique");
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
