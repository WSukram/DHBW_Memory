package de.dhbw.memory.controller;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.spring.annotation.UIScope;
import de.dhbw.memory.model.Board;
import de.dhbw.memory.model.FlipResult;
import de.dhbw.memory.model.Game;
import de.dhbw.memory.model.Player;
import de.dhbw.memory.model.Theme;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Spring service that orchestrates one Memory game per browser tab.
 *
 * <p>{@code @UIScoped} is a Vaadin scope that ties the bean lifetime to a single
 * {@link UI} instance (= one browser tab). Each tab therefore gets its own game
 * state, independent of other open tabs.</p>
 *
 * <p>Typical call sequence from the view layer:
 * <ol>
 *   <li>{@link #startGame} — build board and create a new {@link Game}.</li>
 *   <li>{@link #flip} on every card click — returns a {@link FlipResult} immediately.</li>
 *   <li>On {@link FlipResult#NO_MATCH} the service automatically schedules the
 *       flip-back after {@value #FLIP_BACK_DELAY_MS} ms and notifies the view
 *       via the {@code afterReset} callback (run inside {@link UI#access}).</li>
 * </ol>
 *
 * @author Markus Wenninger
 */
@Service
@UIScope
public class GameService {

    /** Delay in milliseconds before mismatched cards are flipped face-down again. */
    static final long FLIP_BACK_DELAY_MS = 1500;

    /**
     * Single-threaded executor used for the flip-back delay.
     * One thread per service instance is plenty — only one delayed task can be
     * in flight at a time (cards are locked while waiting).
     */
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();

    private Game game;
    private Theme theme;
    private long startTimeMs;

    /**
     * {@code volatile} so the scheduler thread's write is immediately visible
     * to the Vaadin UI thread on the next card click.
     */
    private volatile boolean waitingForFlipBack;

    /**
     * Creates a new game with the given configuration and replaces any previous game.
     *
     * @param playerNames one or two player names (non-empty strings)
     * @param gridSize    board edge length — must be 4 or 6
     * @param theme       card theme that determines motif names
     */
    public void startGame(List<String> playerNames, int gridSize, Theme theme) {
        this.theme = theme;
        this.startTimeMs = System.currentTimeMillis();
        Board board = new Board(gridSize, theme);
        // System.currentTimeMillis() gives a different seed each game, so cards
        // are shuffled differently every time (requirement #FANF04).
        board.shuffle(System.currentTimeMillis());

        List<Player> players = playerNames.stream()
                .map(Player::new)
                .toList();

        game = new Game(players, board);
        waitingForFlipBack = false;
    }

    /**
     * Flips the card at {@code position} and returns the outcome.
     *
     * <p>On {@link FlipResult#NO_MATCH} this method schedules a background task
     * that waits {@value #FLIP_BACK_DELAY_MS} ms, calls {@link Game#resetMismatch()},
     * then runs {@code afterReset} inside {@link UI#access} so the view can
     * re-render without blocking the UI thread (requirement #FANF09).</p>
     *
     * <p>While the flip-back delay is in progress, every subsequent call returns
     * {@link FlipResult#INVALID} to prevent the player from clicking more cards.</p>
     *
     * @param position  0-based grid position of the clicked card
     * @param ui        the Vaadin {@link UI} of the current browser tab; required
     *                  for the async push-back on NO_MATCH (may be {@code null}
     *                  when no mismatch can occur, e.g. in unit tests)
     * @param afterReset callback executed inside {@code ui.access()} after the
     *                   flip-back delay; the view should re-render the board here
     * @return the result of this flip attempt
     */
    public FlipResult flip(int position, UI ui, Runnable afterReset) {
        if (game == null || waitingForFlipBack) {
            return FlipResult.INVALID;
        }

        FlipResult result = game.flip(position);

        if (result == FlipResult.NO_MATCH) {
            waitingForFlipBack = true;
            scheduler.schedule(() -> {
                game.resetMismatch();
                waitingForFlipBack = false;
                // UI.access() acquires the Vaadin session lock and pushes the
                // runnable onto the UI thread — safe to call from any thread.
                // afterReset::run adapts Runnable to Vaadin's Command functional interface.
                ui.access(afterReset::run);
            }, FLIP_BACK_DELAY_MS, TimeUnit.MILLISECONDS);
        }

        return result;
    }

    /**
     * Returns the current {@link Game}, or {@code null} if {@link #startGame} has
     * not been called yet.
     */
    public Game getGame() {
        return game;
    }

    /**
     * Returns the {@link Theme} chosen for the current game, or {@code null} before
     * {@link #startGame} has been called.
     */
    public Theme getTheme() {
        return theme;
    }

    /**
     * Returns the number of whole seconds that have passed since {@link #startGame}
     * was called. Used by the status bar timer.
     */
    public long getElapsedSeconds() {
        return (System.currentTimeMillis() - startTimeMs) / 1000;
    }

    /**
     * Returns {@code true} while the flip-back delay after a mismatch is in
     * progress. The view should disable card buttons in this state.
     */
    public boolean isWaitingForFlipBack() {
        return waitingForFlipBack;
    }

    /**
     * Shuts down the background scheduler when the Vaadin UI (browser tab) is
     * closed. {@code @PreDestroy} is called by Spring when the scoped bean is
     * destroyed, preventing thread leaks.
     */
    @PreDestroy
    public void shutdown() {
        scheduler.shutdownNow();
    }
}
