package de.dhbw.memory.model;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Core game logic: tracks players, the active player, flipped cards, and turn count.
 * The board must already be shuffled before being passed to this constructor.
 *
 * <p>Typical turn flow:
 * <ol>
 *   <li>Player clicks a card → {@link #flip(int)} returns {@link FlipResult#FIRST_FLIP}.</li>
 *   <li>Player clicks a second card → {@link #flip(int)} returns {@link FlipResult#MATCH} or
 *       {@link FlipResult#NO_MATCH}.</li>
 *   <li>On {@code MATCH}: score already updated, same player continues.</li>
 *   <li>On {@code NO_MATCH}: controller waits ~1 500 ms, then calls {@link #resetMismatch()}
 *       to hide the cards and switch the active player.</li>
 * </ol>
 *
 * @author Markus Wenninger
 */
public class Game {

    private final List<Player> players;
    private final Board board;
    private int activePlayerIdx;
    private Card firstFlipped;
    private Card secondFlipped;
    private int totalTurns;

    /**
     * Creates a new game.
     *
     * @param players non-empty list of players; order determines turn order
     * @param board   already-shuffled board
     * @throws IllegalArgumentException if {@code players} is empty
     */
    public Game(List<Player> players, Board board) {
        if (players == null || players.isEmpty()) {
            throw new IllegalArgumentException("At least one player is required.");
        }
        this.players = players;
        this.board = board;
        this.activePlayerIdx = 0;
        this.totalTurns = 0;
    }

    /**
     * Flips the card at {@code position} and returns what happened.
     *
     * @param position 0-based grid position of the card the player clicked
     * @return the result of this flip — see {@link FlipResult}
     */
    public FlipResult flip(int position) {
        Card card = board.getCardAt(position);

        if (card.isMatched() || card.isFaceUp()) {
            return FlipResult.INVALID;
        }

        card.flip();

        if (firstFlipped == null) {
            firstFlipped = card;
            return FlipResult.FIRST_FLIP;
        }

        secondFlipped = card;
        totalTurns++;
        players.get(activePlayerIdx).addTurn();

        if (firstFlipped.getMotif().equals(secondFlipped.getMotif())) {
            resolveMatch();
            return FlipResult.MATCH;
        }

        return FlipResult.NO_MATCH;
    }

    /**
     * Marks both cards of a matching pair and awards the active player a point.
     * Active player keeps the turn (bonus-turn rule).
     */
    private void resolveMatch() {
        firstFlipped.match();
        secondFlipped.match();
        players.get(activePlayerIdx).addPoint();
        firstFlipped = null;
        secondFlipped = null;
    }

    /**
     * Hides both mismatched cards and switches to the next player.
     * Called by the controller after the flip-back delay (~1 500 ms).
     */
    public void resetMismatch() {
        if (firstFlipped != null) {
            firstFlipped.flip();
        }
        if (secondFlipped != null) {
            secondFlipped.flip();
        }
        firstFlipped = null;
        secondFlipped = null;
        activePlayerIdx = (activePlayerIdx + 1) % players.size();
    }

    /** Returns true when every card on the board has been matched. */
    public boolean isFinished() {
        return board.getCards().stream().allMatch(Card::isMatched);
    }

    /**
     * Returns the winning player, or {@code null} if the game is not finished or if it is a tie.
     * For a single-player game this always returns the only player once the game is finished.
     */
    public Player winner() {
        if (!isFinished()) {
            return null;
        }
        if (players.size() == 1) {
            return players.get(0);
        }
        Player best = players.stream().max(Comparator.comparingInt(Player::getScore)).orElseThrow();
        long topCount = players.stream().filter(p -> p.getScore() == best.getScore()).count();
        return topCount == 1 ? best : null;
    }

    /**
     * Returns true when the game is finished and two or more players share the highest score.
     */
    public boolean isTie() {
        if (!isFinished() || players.size() == 1) {
            return false;
        }
        int maxScore = players.stream().mapToInt(Player::getScore).max().orElse(0);
        return players.stream().filter(p -> p.getScore() == maxScore).count() > 1;
    }

    /** Returns the player whose turn it currently is. */
    public Player getActivePlayer() {
        return players.get(activePlayerIdx);
    }

    /** Returns the total number of completed turns (each turn = two cards flipped). */
    public int getTotalTurns() {
        return totalTurns;
    }

    /** Returns an unmodifiable view of the player list. */
    public List<Player> getPlayers() {
        return Collections.unmodifiableList(players);
    }

    /** Returns the board. */
    public Board getBoard() {
        return board;
    }
}
