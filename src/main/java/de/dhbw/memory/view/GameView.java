package de.dhbw.memory.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
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

import java.util.List;

/**
 * The main game screen: shows the card grid, status bar, and an end-of-game overlay.
 *
 * <p>{@link BeforeEnterObserver} gives us a hook that runs before Vaadin renders
 * the view. We use it to redirect to the start screen if no game has been started
 * yet (e.g. when the user navigates directly to {@code /game} via the URL bar).</p>
 *
 * @author Markus Wenninger
 */
@Route("game")
@PageTitle("DHBW Memory")
public class GameView extends VerticalLayout implements BeforeEnterObserver {

    private final GameService gameService;

    /**
     * FlexLayout is the Vaadin equivalent of a CSS Flexbox container.
     * With FlexWrap.WRAP, child elements (card buttons) automatically flow
     * onto the next row when the container width is exceeded.
     */
    private final FlexLayout cardGrid = new FlexLayout();

    /** Single line of text above the grid showing scores and whose turn it is. */
    private final Span statusBar = new Span();

    public GameView(GameService gameService) {
        this.gameService = gameService;

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setPadding(true);

        cardGrid.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        // Max width keeps the grid from stretching across huge monitors.
        cardGrid.setMaxWidth("600px");
        cardGrid.setWidth("100%");
        cardGrid.getStyle().set("gap", "8px");

        statusBar.getStyle()
                .set("font-size", "1.1rem")
                .set("margin-bottom", "16px");

        add(statusBar, cardGrid);
    }

    /**
     * Called by Vaadin before the view is rendered.
     * Redirects to the start screen if no game is active.
     */
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (gameService.getGame() == null) {
            // forwardTo silently swaps the target view — the URL stays as /game
            // which is fine; the user ends up at the start screen either way.
            event.forwardTo(StartView.class);
            return;
        }
        refreshBoard();
    }

    /**
     * Rebuilds the card grid and status bar from the current game state.
     *
     * <p>This method is called both from the UI thread (on click) and from the
     * background thread via {@code UI.access()} after the flip-back delay.
     * Because Vaadin's {@code UI.access()} already acquires the session lock,
     * DOM updates inside this method are always safe.</p>
     */
    private void refreshBoard() {
        Game game = gameService.getGame();
        List<Card> cards = game.getBoard().getCards();
        int size = game.getBoard().getSize();

        cardGrid.removeAll();

        for (int i = 0; i < cards.size(); i++) {
            Card card = cards.get(i);
            // Capture i in a local variable because lambdas can only close over
            // effectively-final variables — the loop variable i changes each iteration.
            int position = i;

            Button btn = createCardButton(card, position, size, game);
            cardGrid.add(btn);
        }

        refreshStatus(game);

        if (game.isFinished()) {
            showEndDialog(game);
        }
    }

    /**
     * Creates one card button representing the card's current state.
     *
     * <ul>
     *   <li>Matched: motif label, success colour, disabled (no more clicks).</li>
     *   <li>Face-up (flipped, not yet resolved): motif label, disabled.</li>
     *   <li>Face-down: "?" label, clickable (unless flip-back is in progress).</li>
     * </ul>
     */
    private Button createCardButton(Card card, int position, int size, Game game) {
        // Each card occupies an equal share of the grid width.
        // "calc()" is CSS — Vaadin passes inline styles straight to the browser.
        String cardWidth = "calc((100% - " + (size - 1) * 8 + "px) / " + size + ")";

        Button btn = new Button();
        btn.setWidth(cardWidth);
        btn.setHeight("90px");
        btn.getStyle()
                .set("font-size", "0.8rem")
                .set("font-weight", "bold")
                .set("transition", "background-color 0.2s");

        if (card.isMatched()) {
            btn.setText(card.getMotif().toUpperCase());
            btn.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
            btn.setEnabled(false);

        } else if (card.isFaceUp()) {
            btn.setText(card.getMotif().toUpperCase());
            btn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            // Disabled while we are waiting for the second card or flip-back.
            btn.setEnabled(false);

        } else {
            btn.setText("?");
            // Disable all face-down cards while the flip-back timer is running.
            btn.setEnabled(!gameService.isWaitingForFlipBack());

            btn.addClickListener(e -> {
                // Pass UI.getCurrent() so GameService can push the flip-back
                // update to this specific browser tab via UI.access().
                FlipResult result = gameService.flip(position, UI.getCurrent(), this::refreshBoard);

                // INVALID means we clicked too fast (card already up, or timer running).
                // In all other cases we refresh the board to show the new state.
                if (result != FlipResult.INVALID) {
                    refreshBoard();
                }
            });
        }

        return btn;
    }

    /**
     * Updates the status bar text to show each player's score, whose turn it is,
     * and how many turns have been played in total (requirement #FANF11).
     */
    private void refreshStatus(Game game) {
        StringBuilder sb = new StringBuilder();

        for (Player player : game.getPlayers()) {
            // Mark the active player with an arrow so it's obvious whose turn it is.
            if (player == game.getActivePlayer() && !game.isFinished()) {
                sb.append("▶ ");
            }
            sb.append(player.getName())
              .append(": ")
              .append(player.getScore())
              .append(" pairs   ");
        }

        sb.append("| Turns: ").append(game.getTotalTurns());
        statusBar.setText(sb.toString());
    }

    /**
     * Shows a modal dialog when the game is finished (requirement #FANF13).
     *
     * <p>A Vaadin {@link Dialog} is a modal overlay — the rest of the UI is
     * blocked until the dialog is closed. We open it programmatically here.</p>
     */
    private void showEndDialog(Game game) {
        Dialog dialog = new Dialog();
        dialog.setCloseOnOutsideClick(false);

        VerticalLayout content = new VerticalLayout();
        content.setAlignItems(Alignment.CENTER);

        String headline;
        if (game.isTie()) {
            headline = "It's a tie!";
        } else if (game.winner() != null) {
            headline = game.getPlayers().size() == 1
                    ? "You finished!"
                    : game.winner().getName() + " wins!";
        } else {
            headline = "Game over!";
        }

        content.add(new H2(headline));

        // Show final scores for all players.
        for (Player player : game.getPlayers()) {
            content.add(new Paragraph(player.getName() + ": " + player.getScore() + " pairs"));
        }
        content.add(new Paragraph("Total turns: " + game.getTotalTurns()));

        Button playAgain = new Button("Play Again", e -> {
            dialog.close();
            UI.getCurrent().navigate(StartView.class);
        });
        playAgain.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        content.add(playAgain);

        dialog.add(content);
        dialog.open();
    }
}
