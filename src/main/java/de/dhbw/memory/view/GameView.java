package de.dhbw.memory.view;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import de.dhbw.memory.controller.GameService;
import de.dhbw.memory.model.Card;
import de.dhbw.memory.model.FlipResult;
import de.dhbw.memory.model.Game;
import de.dhbw.memory.model.Player;
import de.dhbw.memory.model.Theme;
import de.dhbw.memory.view.component.MemoryCard;
import de.dhbw.memory.view.dialog.EndGameDialog;
import de.dhbw.memory.view.dialog.HelpDialog;
import de.dhbw.memory.view.dialog.QuitConfirmDialog;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * The main game screen: card grid, status bar (player info + timer + buttons),
 * and end-of-game overlay.
 *
 * <p>Visual styling lives in {@code styles.css} (utility classes like
 * {@code .glass-surface}, {@code .status-bar}, {@code .card-grid}), card markup
 * lives in {@link MemoryCard}, dialogs live under {@link de.dhbw.memory.view.dialog},
 * and the timer + keyboard helpers live in {@code /static/game.js}. This class
 * therefore stays focused on board lifecycle and click → service wiring.</p>
 *
 * @author Markus Wenninger
 */
@Route("game")
@PageTitle("DHBW Memory")
public class GameView extends VerticalLayout implements BeforeEnterObserver {

    private final GameService gameService;

    private final Set<Integer> mismatchedPositions = new HashSet<>();
    private final Set<Integer> recentlyMatchedPositions = new HashSet<>();
    private final ScheduledExecutorService matchClearScheduler =
            Executors.newSingleThreadScheduledExecutor();
    private final Map<Integer, MemoryCard> cards = new HashMap<>();

    private final Div cardGrid = new Div();
    // statusContent is rebuilt on each refresh; timerSpan + buttons are stable
    // siblings so the JS setInterval can update #game-timer without interference.
    private final Div statusBar = new Div();
    private final Div statusContent = new Div();
    private final Span timerSpan = new Span("0:00");

    private boolean animateEntrance = false;
    private int keyboardFocus = 0;

    /**
     * Spring injects {@link GameService} (it is a {@code @Service} bean).
     * Constructor injection makes the dependency explicit and testable.
     */
    public GameView(GameService gameService) {
        this.gameService = gameService;

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setPadding(true);
        addClassName("app-body");

        UI.getCurrent().getPage().addStyleSheet("/styles.css");
        UI.getCurrent().getPage().addJavaScript("/game.js");

        cardGrid.addClassName("card-grid");

        statusContent.addClassName("status-content");
        timerSpan.setId("game-timer");

        Button quitBtn = iconButton("arrow_back", "End current game and return to setup", this::showQuitConfirm);
        Button helpBtn = iconButton("help", "Show help", this::showHelpDialog);

        statusBar.addClassNames("status-bar", "glass-surface");
        statusBar.add(statusContent, timerSpan, quitBtn, helpBtn);
        add(statusBar, cardGrid);

        addDetachListener(e -> matchClearScheduler.shutdownNow());
    }

    /**
     * Vaadin lifecycle hook fired before this view is rendered. If no game is in progress
     * (e.g. the user typed {@code /game} directly), it redirects to {@link StartView};
     * otherwise it resets transient highlight state and renders the board.
     */
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (gameService.getGame() == null) {
            event.forwardTo(StartView.class);
            return;
        }
        cards.clear();
        recentlyMatchedPositions.clear();
        mismatchedPositions.clear();
        keyboardFocus = 0;
        animateEntrance = true;
        refreshBoard();
    }

    /** Rebuilds the card grid on first call, then only updates state on subsequent calls. */
    private void refreshBoard() {
        Game game = gameService.getGame();
        List<Card> modelCards = game.getBoard().getCards();

        if (cards.isEmpty()) {
            buildBoard(modelCards, game.getBoard().getSize());
        } else {
            updateCards(modelCards);
        }

        refreshStatus(game);

        if (game.isFinished()) {
            showEndDialog(game);
        }
    }

    /**
     * Creates one {@link MemoryCard} per board position, then hands off to
     * {@link #updateCards(List)} for the initial state pass and registers the
     * client-side timer + keyboard listener.
     */
    private void buildBoard(List<Card> modelCards, int size) {
        cardGrid.removeAll();
        // data-size drives the CSS-side grid template + max-width per size.
        cardGrid.getElement().setAttribute("data-size", String.valueOf(size));

        boolean animate = animateEntrance;
        animateEntrance = false;

        Theme theme = gameService.getTheme();
        for (int i = 0; i < modelCards.size(); i++) {
            MemoryCard card = new MemoryCard(i, theme,
                    modelCards.get(i).getMotif(), this::handleCardClick);
            if (animate) card.playEntranceAnimation();
            cardGrid.add(card);
            cards.put(i, card);
        }
        updateCards(modelCards);

        getElement().executeJs("window.dhbwMemory.startTimer($0);",
                (double) gameService.getElapsedSeconds());
        getElement().executeJs("window.dhbwMemory.initKeyboard(this.$server, $0);", size);
    }

    /** Idempotent state sync: every card recomputes its visual state from the model. */
    private void updateCards(List<Card> modelCards) {
        boolean waiting = gameService.isWaitingForFlipBack();
        for (int i = 0; i < modelCards.size(); i++) {
            cards.get(i).applyState(
                    modelCards.get(i),
                    recentlyMatchedPositions.contains(i),
                    mismatchedPositions.contains(i),
                    waiting,
                    i == keyboardFocus
            );
        }
    }

    /**
     * Handles a single card flip (mouse or keyboard) and orchestrates the
     * mismatch-red / match-green ring colouring + end-of-game check.
     */
    private void handleCardClick(int position) {
        UI ui = UI.getCurrent();

        // Snapshot matched set before the flip so we can find newly-matched
        // positions afterwards and only highlight those (green flash).
        List<Card> before = gameService.getGame().getBoard().getCards();
        Set<Integer> matchedBefore = new HashSet<>();
        for (int i = 0; i < before.size(); i++) {
            if (before.get(i).isMatched()) matchedBefore.add(i);
        }

        FlipResult result = gameService.flip(position, ui, () -> {
            mismatchedPositions.clear();
            refreshBoard();
        });

        if (result == FlipResult.NO_MATCH) {
            List<Card> all = gameService.getGame().getBoard().getCards();
            for (int i = 0; i < all.size(); i++) {
                if (all.get(i).isFaceUp()) mismatchedPositions.add(i);
            }
            refreshBoard();
        } else if (result == FlipResult.MATCH) {
            List<Card> after = gameService.getGame().getBoard().getCards();
            Set<Integer> newlyMatched = new HashSet<>();
            for (int i = 0; i < after.size(); i++) {
                if (after.get(i).isMatched() && !matchedBefore.contains(i)) {
                    newlyMatched.add(i);
                }
            }
            recentlyMatchedPositions.addAll(newlyMatched);
            refreshBoard();

            matchClearScheduler.schedule(() -> ui.access(() -> {
                recentlyMatchedPositions.removeAll(newlyMatched);
                updateCards(gameService.getGame().getBoard().getCards());
            }), 1500, TimeUnit.MILLISECONDS);
        } else if (result != FlipResult.INVALID) {
            refreshBoard();
        }
    }

    /** Called from game.js on Space/Enter — flips the keyboard-focused card. */
    @ClientCallable
    public void handleKeyboardFlip(int position) {
        keyboardFocus = position;
        handleCardClick(position);
    }

    /** Called from game.js on arrow keys — moves the focus ring without flipping. */
    @ClientCallable
    public void setKeyboardFocus(int position) {
        keyboardFocus = position;
        updateCards(gameService.getGame().getBoard().getCards());
    }

    /** Repaints the player/score/moves portion of the status bar; leaves timer + buttons untouched. */
    private void refreshStatus(Game game) {
        statusContent.removeAll();

        for (Player player : game.getPlayers()) {
            boolean isActive = player == game.getActivePlayer() && !game.isFinished();

            if (isActive) {
                Span arrow = new Span("▶");
                arrow.addClassName("status-arrow");
                statusContent.add(arrow);
            }

            Span playerSpan = new Span(player.getName() + ": " + Labels.pairsLabel(player.getScore()));
            playerSpan.addClassName(isActive ? "player-active" : "player-inactive");
            statusContent.add(playerSpan);
        }

        Span moves = new Span("Moves: " + game.getTotalTurns());
        moves.addClassName("status-moves");
        statusContent.add(moves);
    }

    /** Overlay shown when every pair has been matched; also fires the confetti. */
    private void showEndDialog(Game game) {
        UI.getCurrent().getPage().executeJs(
                "window.dhbwMemory.stopTimer();"
                        + "window.dhbwMemory.showConfetti();");
        new EndGameDialog(
                game,
                gameService.getElapsedSeconds(),
                this::restartWithSameSettings,
                () -> UI.getCurrent().navigate(StartView.class)
        ).open();
    }

    /** Opens the help dialog with wording adapted to single- vs multi-player. */
    private void showHelpDialog() {
        Game game = gameService.getGame();
        boolean solo = game != null && game.getPlayers().size() == 1;
        new HelpDialog(solo).open();
    }

    /** Confirms a mid-game quit, then navigates back to the start view if accepted. */
    private void showQuitConfirm() {
        new QuitConfirmDialog(() -> UI.getCurrent().navigate(StartView.class)).open();
    }

    /**
     * Rebuilds the current board in place with identical player names, grid
     * size, and theme — used by the "Same Settings" button on the end dialog.
     */
    private void restartWithSameSettings() {
        Game current = gameService.getGame();
        List<String> names = current.getPlayers().stream().map(Player::getName).toList();
        int size = current.getBoard().getSize();
        Theme currentTheme = gameService.getTheme();

        gameService.startGame(names, size, currentTheme);

        cards.clear();
        recentlyMatchedPositions.clear();
        mismatchedPositions.clear();
        keyboardFocus = 0;
        animateEntrance = true;
        refreshBoard();
    }

    /**
     * Factory for the small Material-Symbols icon buttons in the status bar
     * (arrow_back, help). The icon name is a Material Symbols ligature.
     */
    private Button iconButton(String iconName, String ariaLabel, Runnable onClick) {
        Span icon = new Span(iconName);
        icon.addClassName("material-symbols-outlined");

        Button b = new Button(icon);
        b.addClassName("icon-btn");
        b.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        b.getElement().setAttribute("aria-label", ariaLabel);
        b.addClickListener(e -> {
            // Blur so subsequent Space/Enter doesn't re-trigger this button.
            b.getElement().executeJs("this.blur();");
            onClick.run();
        });
        return b;
    }
}
