package de.dhbw.memory.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import de.dhbw.memory.model.Theme;
import de.dhbw.memory.controller.GameService;
import de.dhbw.memory.model.Card;
import de.dhbw.memory.model.FlipResult;
import de.dhbw.memory.model.Game;
import de.dhbw.memory.model.Player;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
     * Positions of the two cards that were just flipped face-up but did NOT match.
     * The view paints these with a red ring (instead of the normal purple face-up
     * ring) for the ~1.5s the flip-back timer is running, giving the player a
     * clear "no match" signal. Cleared as soon as the cards flip back down.
     */
    private final Set<Integer> mismatchedPositions = new HashSet<>();

    /**
     * The card grid is a plain {@link Div} styled as a CSS Grid. We switched
     * away from FlexLayout because Flexbox + {@code aspect-ratio} children
     * has fragile sizing behaviour (the cards' computed height kept collapsing
     * to zero on wrap). CSS Grid is the right primitive here: we tell it
     * "N equal columns" via {@code grid-template-columns: repeat(N, 1fr)}
     * and each card just sets its aspect ratio.
     */
    private final Div cardGrid = new Div();

    /** Single line of text above the grid showing scores and whose turn it is. */
    private final Span statusBar = new Span();

    public GameView(GameService gameService) {
        this.gameService = gameService;

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setPadding(true);

        // Page background + font matches the WalletPulse site (dark navy + Inter).
        getStyle()
                .set("background", "#0b1326")
                .set("font-family", "'Inter', sans-serif")
                .set("color", "#dae2fd");

        // Max width keeps the grid from stretching across huge monitors.
        cardGrid.setMaxWidth("600px");
        cardGrid.setWidth("100%");
        cardGrid.getStyle()
                .set("display", "grid")
                .set("gap", "10px")
                // grid-template-columns is set per game in refreshBoard() once we
                // know the grid size (4 or 6).
                .set("box-sizing", "border-box");

        // Status bar styled as a "glass panel" to match the WP design language.
        statusBar.getStyle()
                .set("font-size", "0.95rem")
                .set("font-weight", "500")
                .set("padding", "10px 18px")
                .set("background", "rgba(23, 31, 51, 0.75)")
                .set("border", "1px solid rgba(255,255,255,0.06)")
                .set("border-radius", "8px")
                .set("color", "#dae2fd")
                .set("margin-bottom", "16px")
                .set("backdrop-filter", "blur(8px)");

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
        // Reset the column template each refresh — needed because the user can
        // start a new game with a different grid size from the end-game dialog.
        cardGrid.getStyle().set("grid-template-columns", "repeat(" + size + ", 1fr)");

        for (int i = 0; i < cards.size(); i++) {
            Card card = cards.get(i);
            // Capture i in a local variable because lambdas can only close over
            // effectively-final variables — the loop variable i changes each iteration.
            int position = i;

            Component cardEl = createCardElement(card, position, size, game);
            cardGrid.add(cardEl);
        }

        refreshStatus(game);

        if (game.isFinished()) {
            showEndDialog(game);
        }
    }

    /**
     * Creates one card element representing the card's current state.
     *
     * <p>The card is a {@link Div} (not a {@link Button}) because we need absolute
     * positioning for three layered pieces of content: WalletPulse logo in the
     * top-right corner, motif image centered, and motif name at the bottom.
     * {@code Div} supports click events via the {@code ClickNotifier} interface.</p>
     *
     * <ul>
     *   <li>Matched: face-up layout, green border tint, no click handler.</li>
     *   <li>Face-up (flipped, not yet resolved): face-up layout, blue border, no click.</li>
     *   <li>Face-down: shared card-back image fills the card, clickable
     *       (unless the flip-back timer is running).</li>
     * </ul>
     */
    private Component createCardElement(Card card, int position, int size, Game game) {
        // No explicit width: the parent CSS grid (`repeat(N, 1fr)`) gives each card
        // an equal share of the available width. We only set the aspect ratio so
        // the height derives from that width — cards scale fluidly with the viewport
        // and always look like proper playing cards (4:5, slightly taller than wide).
        Div cardEl = new Div();
        cardEl.getStyle()
                .set("aspect-ratio", "1 / 1")
                .set("position", "relative")
                .set("background", "white")
                .set("border", "1px solid rgba(255,255,255,0.08)")
                .set("border-radius", "10px")
                .set("box-shadow", "0 4px 12px rgba(0,0,0,0.35)")
                .set("overflow", "hidden")
                .set("user-select", "none")
                .set("transition", "box-shadow 0.2s, transform 0.15s");

        if (card.isMatched()) {
            // Matched (= a successful pair): bright green ring + soft outer glow.
            // We use stacked box-shadows instead of changing the border width so
            // the card's box dimensions don't shift when state changes.
            //   • 0 0 0 4px green      → solid ring hugging the card
            //   • 0 0 0 8px green/30%  → softer outer ring
            //   • 0 0 28px green/55%   → blurred glow
            cardEl.getStyle().set("box-shadow",
                    "0 0 0 4px #4edea3,"
                  + " 0 0 0 8px rgba(78,222,163,0.30),"
                  + " 0 0 28px rgba(78,222,163,0.55),"
                  + " 0 4px 12px rgba(0,0,0,0.35)");
            addFaceUpContent(cardEl, card.getMotif());

        } else if (card.isFaceUp()) {
            if (mismatchedPositions.contains(position)) {
                // Mismatch (just-flipped pair that didn't match): red ring + glow.
                // Uses WP's error colour (#ffb4ab). The transition: box-shadow on
                // the base card animates the fade from purple → red automatically.
                cardEl.getStyle().set("box-shadow",
                        "0 0 0 4px #ffb4ab,"
                      + " 0 0 0 8px rgba(255,180,171,0.30),"
                      + " 0 0 26px rgba(255,107,93,0.55),"
                      + " 0 4px 12px rgba(0,0,0,0.35)");
            } else {
                // Face-up but not yet resolved: thinner brand-purple ring.
                cardEl.getStyle().set("box-shadow",
                        "0 0 0 3px #c3c0ff,"
                      + " 0 0 18px rgba(195,192,255,0.40),"
                      + " 0 4px 12px rgba(0,0,0,0.35)");
            }
            addFaceUpContent(cardEl, card.getMotif());

        } else {
            // Face-down: card-back image fills the entire card.
            Image back = new Image("/images/back.svg", "card back");
            back.getStyle()
                    .set("position", "absolute")
                    .set("top", "0").set("left", "0")
                    .set("width", "100%").set("height", "100%")
                    .set("object-fit", "cover");
            cardEl.add(back);

            if (gameService.isWaitingForFlipBack()) {
                cardEl.getStyle().set("opacity", "0.7").set("cursor", "default");
            } else {
                cardEl.getStyle().set("cursor", "pointer");
                cardEl.addClickListener(e -> {
                    // Pass UI.getCurrent() so GameService can push the flip-back
                    // update to this specific browser tab via UI.access(). The
                    // callback also clears the "red mismatch ring" state since
                    // the cards are flipping back to face-down at that point.
                    FlipResult result = gameService.flip(position, UI.getCurrent(), () -> {
                        mismatchedPositions.clear();
                        refreshBoard();
                    });

                    if (result == FlipResult.NO_MATCH) {
                        // Tag both currently face-up cards as "mismatched" so the
                        // next refreshBoard() paints them red. The model has just
                        // flipped the second card face-up, so exactly two cards
                        // satisfy isFaceUp() here.
                        List<Card> all = gameService.getGame().getBoard().getCards();
                        for (int i = 0; i < all.size(); i++) {
                            if (all.get(i).isFaceUp()) {
                                mismatchedPositions.add(i);
                            }
                        }
                        refreshBoard();
                    } else if (result != FlipResult.INVALID) {
                        // FIRST_FLIP or MATCH: just re-render normally.
                        // INVALID means we clicked too fast (card already up, or timer running).
                        refreshBoard();
                    }
                });
            }
        }

        return cardEl;
    }

    /**
     * Fills a face-up card with the three layered pieces: WalletPulse branding
     * in the top-right, the motif image centered, and the motif name at the bottom.
     *
     * <p>All children are absolutely positioned so they overlap on top of the
     * card's white background — this mirrors the layout of the NiceHash Memory
     * Game cards we are modelling.</p>
     */
    private void addFaceUpContent(Div cardEl, String motif) {
        Theme theme = gameService.getTheme();

        // ─── Tweak point: top-right WalletPulse icon ────────────────────────
        // Change WP_ICON_WIDTH to scale the brand mark; top/right control the
        // distance from the corner.
        final String WP_ICON_WIDTH = "14%";
        Image wp = new Image("/images/wp-icon.svg", "WalletPulse");
        wp.getStyle()
                .set("position", "absolute")
                .set("top", "6%").set("right", "6%")
                .set("width", WP_ICON_WIDTH).set("height", "auto")
                .set("border-radius", "3px")
                .set("opacity", "0.9");

        // ─── Tweak point: centered motif image ──────────────────────────────
        // MOTIF_SIZE controls how big the coin / space object appears.
        // translate(-50%,-50%) keeps it true-centered both axes.
        final String MOTIF_SIZE = "52%";
        Image motifImg = new Image(
                "/images/" + theme.getFolder() + "/" + motif + ".svg", motif);
        motifImg.getStyle()
                .set("position", "absolute")
                .set("top", "50%").set("left", "50%")
                .set("transform", "translate(-50%, -50%)")
                .set("width", MOTIF_SIZE).set("height", MOTIF_SIZE)
                .set("object-fit", "contain");

        // ─── Tweak point: bottom-center display name ────────────────────────
        // Change font-size clamp() to scale the label up or down across screens.
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

        cardEl.add(wp, motifImg, name);
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
