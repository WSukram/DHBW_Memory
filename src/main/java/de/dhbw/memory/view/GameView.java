package de.dhbw.memory.view;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * The main game screen: card grid, status bar, and end-of-game overlay.
 *
 * @author Markus Wenninger
 */
@Route("game")
@PageTitle("DHBW Memory")
public class GameView extends VerticalLayout implements BeforeEnterObserver {

    private final GameService gameService;

    private final Set<Integer> mismatchedPositions = new HashSet<>();
    private final Set<Integer> recentlyMatchedPositions = new HashSet<>();
    private final ScheduledExecutorService matchClearScheduler = Executors.newSingleThreadScheduledExecutor();
    private final Map<Integer, Div> cardWrappers = new HashMap<>();
    private final Map<Integer, Div> cardInners = new HashMap<>();

    private final Div cardGrid = new Div();

    // Status bar is split: statusContent is cleared on each refresh,
    // timerSpan and helpBtn are stable children that survive removeAll().
    private final Div statusBar = new Div();
    private final Div statusContent = new Div();
    private final Span timerSpan = new Span("0:00");

    private boolean animateEntrance = false;

    /** Grid position currently highlighted by keyboard navigation. */
    private int keyboardFocus = 0;

    public GameView(GameService gameService) {
        this.gameService = gameService;

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setPadding(true);

        UI.getCurrent().getPage().addStyleSheet("/styles.css");

        getStyle()
                .set("background", "#0b1326")
                .set("font-family", "'Inter', sans-serif")
                .set("color", "#dae2fd");

        cardGrid.setMaxWidth("600px");
        cardGrid.setWidth("100%");
        cardGrid.getStyle()
                .set("display", "grid")
                .set("gap", "10px")
                .set("box-sizing", "border-box");

        // statusContent holds the per-refresh player info; cleared on each refresh.
        statusContent.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("gap", "12px")
                .set("flex", "1");

        // timerSpan never leaves the DOM so the JS setInterval can update it freely.
        timerSpan.setId("game-timer");
        timerSpan.getStyle()
                .set("color", "#a0b0d0")
                .set("font-size", "0.85rem")
                .set("font-variant-numeric", "tabular-nums")
                .set("margin-left", "8px");

        Button helpBtn = new Button("?", e -> showHelpDialog());
        helpBtn.getStyle()
                .set("min-width", "28px").set("width", "28px").set("height", "28px")
                .set("padding", "0").set("border-radius", "50%")
                .set("font-size", "0.85rem").set("font-weight", "700")
                .set("cursor", "pointer");
        helpBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        statusBar.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("gap", "8px")
                .set("width", "100%")
                .set("max-width", "600px")
                .set("font-size", "0.9rem")
                .set("font-weight", "500")
                .set("padding", "10px 18px")
                .set("background", "rgba(23, 31, 51, 0.75)")
                .set("border", "1px solid rgba(255,255,255,0.06)")
                .set("border-radius", "8px")
                .set("color", "#dae2fd")
                .set("margin-bottom", "16px")
                .set("backdrop-filter", "blur(8px)")
                .set("box-sizing", "border-box");

        statusBar.add(statusContent, timerSpan, helpBtn);
        add(statusBar, cardGrid);

        addDetachListener(e -> matchClearScheduler.shutdownNow());
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (gameService.getGame() == null) {
            event.forwardTo(StartView.class);
            return;
        }
        cardWrappers.clear();
        cardInners.clear();
        recentlyMatchedPositions.clear();
        mismatchedPositions.clear();
        keyboardFocus = 0;
        animateEntrance = true;
        refreshBoard();
    }

    private void refreshBoard() {
        Game game = gameService.getGame();
        List<Card> cards = game.getBoard().getCards();

        if (cardWrappers.isEmpty()) {
            buildBoard(cards, game.getBoard().getSize());
        } else {
            updateBoard(cards);
        }

        refreshStatus(game);

        if (game.isFinished()) {
            showEndDialog(game);
        }
    }

    /**
     * Creates all card DOM elements once and injects the JS timer + keyboard listener.
     * After this call, only updateBoard() is used to reflect state changes.
     */
    private void buildBoard(List<Card> cards, int size) {
        cardGrid.removeAll();
        cardGrid.getStyle().set("grid-template-columns", "repeat(" + size + ", 1fr)");

        boolean animate = animateEntrance;
        animateEntrance = false;

        for (int i = 0; i < cards.size(); i++) {
            int position = i;
            String motif = cards.get(i).getMotif();

            Div wrapper = new Div();
            wrapper.addClassName("card-wrapper");
            if (animate) {
                wrapper.addClassName("card-enter");
                wrapper.getStyle().set("--delay", (position * 30) + "ms");
            }

            Div inner = new Div();
            inner.addClassName("card-inner");

            Div backFace = new Div();
            backFace.addClassName("card-face");
            backFace.addClassName("card-back");
            Image backImg = new Image("/images/back.svg", "card back");
            backImg.getStyle().set("width", "100%").set("height", "100%").set("object-fit", "cover");
            backFace.add(backImg);

            Div frontFace = new Div();
            frontFace.addClassName("card-face");
            frontFace.addClassName("card-front");
            addFaceUpContent(frontFace, motif);

            inner.add(backFace, frontFace);
            wrapper.add(inner);
            wrapper.addClickListener(e -> handleCardClick(position));

            cardGrid.add(wrapper);
            cardWrappers.put(position, wrapper);
            cardInners.put(position, inner);
        }

        updateBoard(cards);
        startClientTimer();
        injectKeyboardListener(size);
    }

    /**
     * Updates CSS classes and styles on existing card elements without touching the DOM structure.
     * Adding/removing "flipped" on card-inner triggers the CSS rotateY transition.
     */
    private void updateBoard(List<Card> cards) {
        for (int i = 0; i < cards.size(); i++) {
            Card card = cards.get(i);
            Div wrapper = cardWrappers.get(i);
            Div inner = cardInners.get(i);

            if (card.isFaceUp() || card.isMatched()) {
                inner.addClassName("flipped");
            } else {
                inner.removeClassName("flipped");
            }

            if (card.isMatched()) {
                wrapper.removeClassName("card-face-down");
                wrapper.removeClassName("keyboard-focused");
                String matchShadow = recentlyMatchedPositions.contains(i)
                        ? "0 0 0 4px #4edea3,"
                          + " 0 0 0 8px rgba(78,222,163,0.30),"
                          + " 0 0 28px rgba(78,222,163,0.55),"
                          + " 0 4px 12px rgba(0,0,0,0.35)"
                        : "0 4px 12px rgba(0,0,0,0.35)";
                wrapper.getStyle()
                        .set("box-shadow", matchShadow)
                        .set("cursor", "default")
                        .set("opacity", "1");

            } else if (card.isFaceUp()) {
                wrapper.removeClassName("card-face-down");
                String shadow = mismatchedPositions.contains(i)
                        ? "0 0 0 4px #ffb4ab,"
                          + " 0 0 0 8px rgba(255,180,171,0.30),"
                          + " 0 0 26px rgba(255,107,93,0.55),"
                          + " 0 4px 12px rgba(0,0,0,0.35)"
                        : "0 0 0 3px #c3c0ff,"
                          + " 0 0 18px rgba(195,192,255,0.40),"
                          + " 0 4px 12px rgba(0,0,0,0.35)";
                wrapper.getStyle()
                        .set("box-shadow", shadow)
                        .set("cursor", "default")
                        .set("opacity", "1");

            } else if (gameService.isWaitingForFlipBack()) {
                wrapper.removeClassName("card-face-down");
                wrapper.getStyle()
                        .set("box-shadow", "0 4px 12px rgba(0,0,0,0.35)")
                        .set("cursor", "default")
                        .set("opacity", "0.7");

            } else {
                wrapper.addClassName("card-face-down");
                wrapper.getStyle()
                        .set("box-shadow", "0 4px 12px rgba(0,0,0,0.35)")
                        .set("cursor", "pointer")
                        .set("opacity", "1");
            }

            // Keyboard focus ring — only on face-down cards.
            if (i == keyboardFocus && !card.isMatched()) {
                wrapper.addClassName("keyboard-focused");
            } else {
                wrapper.removeClassName("keyboard-focused");
            }
        }
    }

    private void handleCardClick(int position) {
        UI ui = UI.getCurrent();

        // Snapshot which positions are already matched before the flip so we can
        // diff afterward and find only the two cards that just matched.
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
                updateBoard(gameService.getGame().getBoard().getCards());
            }), 1500, TimeUnit.MILLISECONDS);
        } else if (result != FlipResult.INVALID) {
            refreshBoard();
        }
    }

    /**
     * Called from the client when the user presses Space or Enter on the focused card.
     * Runs on the Vaadin UI thread via the @ClientCallable mechanism.
     */
    @ClientCallable
    public void handleKeyboardFlip(int position) {
        keyboardFocus = position;
        handleCardClick(position);
    }

    /**
     * Called from the client on arrow key presses. Updates the focus highlight
     * without triggering a full board refresh.
     */
    @ClientCallable
    public void setKeyboardFocus(int position) {
        keyboardFocus = position;
        cardWrappers.forEach((pos, wrapper) -> {
            Card card = gameService.getGame().getBoard().getCardAt(pos);
            if (pos == position && !card.isMatched()) {
                wrapper.addClassName("keyboard-focused");
            } else {
                wrapper.removeClassName("keyboard-focused");
            }
        });
    }

    /** Starts the client-side interval that increments the timer span every 500 ms. */
    private void startClientTimer() {
        double elapsed = gameService.getElapsedSeconds();
        getElement().executeJs(
            "var el = document.getElementById('game-timer');" +
            "var start = Date.now() - $0 * 1000;" +
            "if (window._gameTimer) clearInterval(window._gameTimer);" +
            "window._gameTimer = setInterval(function() {" +
            "  if (!el) el = document.getElementById('game-timer');" +
            "  if (!el) return;" +
            "  var s = Math.floor((Date.now() - start) / 1000);" +
            "  var m = Math.floor(s / 60); var ss = s % 60;" +
            "  el.textContent = m + ':' + (ss < 10 ? '0' : '') + ss;" +
            "}, 500);",
            elapsed
        );
    }

    /**
     * Injects a keydown listener for arrow-key focus movement and space/enter flip.
     * Uses this.$server to call back into Java without a full page interaction.
     */
    private void injectKeyboardListener(int size) {
        getElement().executeJs(
            "var size = $0; var total = size * size; var focus = 0;" +
            "if (window._keyListener) document.removeEventListener('keydown', window._keyListener);" +
            "var self = this;" +
            "window._keyListener = function(e) {" +
            "  var moved = false;" +
            "  if (e.key === 'ArrowRight') { focus = (focus + 1) % total; moved = true; }" +
            "  else if (e.key === 'ArrowLeft') { focus = (focus - 1 + total) % total; moved = true; }" +
            "  else if (e.key === 'ArrowDown') { focus = Math.min(focus + size, total - 1); moved = true; }" +
            "  else if (e.key === 'ArrowUp') { focus = Math.max(focus - size, 0); moved = true; }" +
            "  else if (e.key === ' ' || e.key === 'Enter') {" +
            "    self.$server.handleKeyboardFlip(focus); e.preventDefault(); return;" +
            "  }" +
            "  if (moved) { e.preventDefault(); self.$server.setKeyboardFocus(focus); }" +
            "};" +
            "document.addEventListener('keydown', window._keyListener);",
            size
        );
    }

    private void addFaceUpContent(Div face, String motif) {
        Theme theme = gameService.getTheme();

        Image wp = new Image("/images/wp-icon.svg", "WalletPulse");
        wp.getStyle()
                .set("position", "absolute")
                .set("top", "6%").set("right", "6%")
                .set("width", "14%").set("height", "auto")
                .set("border-radius", "3px")
                .set("opacity", "0.9");

        Image motifImg = new Image("/images/" + theme.getFolder() + "/" + motif + ".svg", motif);
        motifImg.getStyle()
                .set("position", "absolute")
                .set("top", "50%").set("left", "50%")
                .set("transform", "translate(-50%, -50%)")
                .set("width", "52%").set("height", "52%")
                .set("object-fit", "contain");

        Span name = new Span(theme.getDisplayName(motif));
        name.getStyle()
                .set("position", "absolute")
                .set("bottom", "8%").set("left", "0").set("right", "0")
                .set("text-align", "center")
                .set("font-family", "'Inter', sans-serif")
                .set("font-size", "clamp(9px, 1.4vw, 13px)")
                .set("font-weight", "600")
                .set("letter-spacing", "0.2px")
                .set("color", "#1c1b1f")
                .set("padding", "0 4%")
                .set("white-space", "nowrap")
                .set("overflow", "hidden")
                .set("text-overflow", "ellipsis");

        face.add(wp, motifImg, name);
    }

    private void refreshStatus(Game game) {
        statusContent.removeAll();

        for (Player player : game.getPlayers()) {
            boolean isActive = player == game.getActivePlayer() && !game.isFinished();

            if (isActive) {
                Span arrow = new Span("▶");
                arrow.getStyle().set("color", "#863bff").set("font-size", "1.1rem").set("line-height", "1");
                statusContent.add(arrow);
            }

            Span playerSpan = new Span(player.getName() + ": " + player.getScore() + " pairs");
            if (isActive) {
                playerSpan.addClassName("player-active");
            } else {
                playerSpan.getStyle().set("color", "#8090b0");
            }
            statusContent.add(playerSpan);
        }

        Span moves = new Span("Moves: " + game.getTotalTurns());
        moves.getStyle()
                .set("margin-left", "auto")
                .set("color", "#6070a0")
                .set("font-size", "0.85rem");
        statusContent.add(moves);
    }

    private void showEndDialog(Game game) {
        // Stop the live timer.
        UI.getCurrent().getPage().executeJs("if (window._gameTimer) clearInterval(window._gameTimer);");

        long elapsed = gameService.getElapsedSeconds();
        long minutes = elapsed / 60;
        long seconds = elapsed % 60;
        String timeStr = minutes + ":" + (seconds < 10 ? "0" : "") + seconds;

        Dialog dialog = new Dialog();
        dialog.setCloseOnOutsideClick(false);

        VerticalLayout content = new VerticalLayout();
        content.setAlignItems(Alignment.CENTER);

        String headline;
        if (game.isTie()) {
            headline = "It's a tie!";
        } else if (game.winner() != null) {
            headline = game.getPlayers().size() == 1 ? "You finished!" : game.winner().getName() + " wins!";
        } else {
            headline = "Game over!";
        }

        content.add(new H2(headline));

        for (Player player : game.getPlayers()) {
            int accuracy = player.getTurns() > 0
                    ? (player.getScore() * 100 / player.getTurns())
                    : 0;
            String label = game.getPlayers().size() == 1
                    ? player.getScore() + " pairs  ·  " + accuracy + "% accuracy"
                    : player.getName() + ": " + player.getScore() + " pairs  ·  " + accuracy + "% accuracy";
            content.add(new Paragraph(label));
        }

        content.add(new Paragraph("Time: " + timeStr + "  ·  Moves: " + game.getTotalTurns()));

        Button playAgain = new Button("Play Again", e -> {
            dialog.close();
            UI.getCurrent().navigate(StartView.class);
        });
        playAgain.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        content.add(playAgain);

        dialog.add(content);
        dialog.open();
    }

    private void showHelpDialog() {
        Dialog dialog = new Dialog();

        VerticalLayout content = new VerticalLayout();
        content.setAlignItems(Alignment.START);
        content.setWidth("320px");

        content.add(new H3("How to play"));
        content.add(new Paragraph("Flip two cards per turn. If they match, you score a point and play again."));
        content.add(new Paragraph("If they don't match, they flip back and the next player takes their turn."));
        content.add(new Paragraph("The player with the most pairs at the end wins."));

        Paragraph keys = new Paragraph("Keyboard: Arrow keys to move · Space or Enter to flip");
        keys.getStyle().set("color", "#8090b0").set("font-size", "0.85rem");
        content.add(keys);

        Button close = new Button("Got it", e -> dialog.close());
        close.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        content.add(close);

        dialog.add(content);
        dialog.open();
    }
}
