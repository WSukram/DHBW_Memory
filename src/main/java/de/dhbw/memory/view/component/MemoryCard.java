package de.dhbw.memory.view.component;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import de.dhbw.memory.model.Card;
import de.dhbw.memory.model.Theme;

import java.util.function.IntConsumer;

/**
 * A single Memory card as a Vaadin component.
 *
 * <p>Encapsulates the three-level DOM structure required for the CSS 3D flip:
 * an outer wrapper ({@link Div} = this component), a transformed inner div,
 * and the two faces (back + front). State changes (flipped, matched,
 * mismatched, keyboard focused, …) are expressed as CSS classes on the
 * wrapper or its inner div — the actual visual styling lives in
 * {@code styles.css}, so this class is purely structural.</p>
 *
 * <p>Why a class instead of inline helpers? Two reasons: (1) keeps
 * {@code GameView} focused on board lifecycle rather than card mechanics,
 * (2) any future change to card markup (e.g. swapping the WP icon for a
 * theme-specific badge) happens in exactly one place.</p>
 *
 * @author Markus Wenninger
 */
public class MemoryCard extends Div {

    private final int position;
    private final Theme theme;
    private final Div inner;

    /**
     * Builds a card at the given grid position.
     *
     * @param position 0-based grid index, used for the ARIA label and click callback
     * @param theme    current game theme (controls motif folder + display name)
     * @param motif    motif id for this card (e.g. {@code "btc"})
     * @param onClick  invoked with {@link #position} when the user clicks the card
     */
    public MemoryCard(int position, Theme theme, String motif, IntConsumer onClick) {
        this.position = position;
        this.theme = theme;

        addClassName("card-wrapper");
        getElement().setAttribute("role", "button");

        inner = new Div();
        inner.addClassName("card-inner");

        Div back = new Div();
        back.addClassName("card-face");
        back.addClassName("card-back");
        Image backImg = new Image("/images/back.svg", "card back");
        backImg.addClassName("card-back-img");
        back.add(backImg);

        Div front = new Div();
        front.addClassName("card-face");
        front.addClassName("card-front");
        buildFront(front, motif);

        inner.add(back, front);
        add(inner);

        addClickListener(e -> onClick.accept(position));
    }

    /** Adds the staggered entrance animation; called once per card on initial build. */
    public void playEntranceAnimation() {
        addClassName("card-enter");
        getStyle().set("--delay", (position * 30) + "ms");
    }

    /**
     * Reflects the current card state on the DOM.
     *
     * <p>All visual states are mutually exclusive class names — exactly one of
     * {@code state-recently-matched / state-mismatched / state-face-up /
     * state-waiting / card-face-down} is applied to the wrapper, so CSS rules
     * stay simple and there is no "stale class" risk.</p>
     *
     * @param card                 the model card (face-up / matched flags)
     * @param recentlyMatched      true while the green flash should be drawn
     * @param mismatched           true while the red flash should be drawn
     * @param waitingForFlipBack   true during the 1.5s mismatch hold (dim the card)
     * @param keyboardFocused      true if this card has the keyboard focus ring
     */
    public void applyState(Card card,
                           boolean recentlyMatched,
                           boolean mismatched,
                           boolean waitingForFlipBack,
                           boolean keyboardFocused) {

        if (card.isFaceUp() || card.isMatched()) {
            inner.addClassName("flipped");
        } else {
            inner.removeClassName("flipped");
        }

        // Reset all mutable state classes, then add exactly one.
        removeClassName("state-recently-matched");
        removeClassName("state-mismatched");
        removeClassName("state-face-up");
        removeClassName("state-waiting");
        removeClassName("card-face-down");

        if (card.isMatched()) {
            if (recentlyMatched) addClassName("state-recently-matched");
        } else if (card.isFaceUp()) {
            addClassName(mismatched ? "state-mismatched" : "state-face-up");
        } else if (waitingForFlipBack) {
            addClassName("state-waiting");
        } else {
            addClassName("card-face-down");
        }

        if (keyboardFocused && !card.isMatched()) {
            addClassName("keyboard-focused");
        } else {
            removeClassName("keyboard-focused");
        }

        getElement().setAttribute("aria-label", ariaLabel(card));
    }

    private String ariaLabel(Card card) {
        String motifName = theme.getDisplayName(card.getMotif());
        if (card.isMatched())  return "Card " + (position + 1) + ", matched, " + motifName;
        if (card.isFaceUp())   return "Card " + (position + 1) + ", face up, " + motifName;
        return "Card " + (position + 1) + ", face down";
    }

    private void buildFront(Div face, String motif) {
        Image wp = new Image("/images/wp-icon.svg", "WalletPulse");
        wp.addClassName("card-face-wp");

        Image motifImg = new Image("/images/" + theme.getFolder() + "/" + motif + ".svg", motif);
        motifImg.addClassName("card-face-motif");

        Span name = new Span(theme.getDisplayName(motif));
        name.addClassName("card-face-name");

        face.add(wp, motifImg, name);
    }
}
