package de.dhbw.memory.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlayerTest {

    @Test
    void newPlayerScoreIsZero() {
        Player player = new Player("Alice");
        assertEquals(0, player.getScore());
    }

    @Test
    void newPlayerTurnsIsZero() {
        Player player = new Player("Alice");
        assertEquals(0, player.getTurns());
    }

    @Test
    void addPointIncrementsScore() {
        Player player = new Player("Alice");
        player.addPoint();
        assertEquals(1, player.getScore());
    }

    @Test
    void addPointCanBeCalledMultipleTimes() {
        Player player = new Player("Alice");
        player.addPoint();
        player.addPoint();
        player.addPoint();
        assertEquals(3, player.getScore());
    }

    @Test
    void addTurnIncrementsTurns() {
        Player player = new Player("Alice");
        player.addTurn();
        assertEquals(1, player.getTurns());
    }

    @Test
    void addTurnAndAddPointAreIndependent() {
        Player player = new Player("Alice");
        player.addTurn();
        player.addTurn();
        player.addPoint();
        assertEquals(2, player.getTurns());
        assertEquals(1, player.getScore());
    }

    @Test
    void nameIsReturnedCorrectly() {
        Player player = new Player("Bob");
        assertEquals("Bob", player.getName());
    }
}
