package de.dhbw.memory.view.dialog;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;

/**
 * "How to play" overlay. Wording adapts to single- vs multi-player mode so
 * the rules read naturally regardless of the game type.
 *
 * <p>Shares the {@code theme="dhbw"} overlay treatment and {@code .dhbw-dialog}
 * structure with {@link QuitConfirmDialog} and {@link EndGameDialog}. Each rule
 * is a {@code .dialog-rule} row with a leading Material Symbols icon; the
 * keyboard hints render as {@code .kbd} chips so they read as actual keys
 * rather than punctuation.</p>
 *
 * @author Markus Wenninger
 */
public class HelpDialog extends Dialog {

    /**
     * @param singlePlayer true if the current game has exactly one player; switches
     *                     the rules text away from "next player" / "winner" phrasing
     */
    public HelpDialog(boolean singlePlayer) {
        getElement().getThemeList().add("dhbw");
        setWidth("440px");

        Span iconSym = new Span("help_outline");
        iconSym.addClassName("material-symbols-outlined");
        Div iconBadge = new Div(iconSym);
        iconBadge.addClassName("dialog-icon");

        H2 title = new H2("How to play");
        title.addClassName("dialog-title");

        Div rules = new Div();
        rules.addClassName("dialog-rules");
        rules.add(rule("touch_app",
                "Flip two cards per turn. If they match, you score a point and play again."));
        if (singlePlayer) {
            rules.add(rule("autorenew",
                    "If they don't match, they flip back. Try to find all pairs in as few moves as possible."));
        } else {
            rules.add(rule("autorenew",
                    "If they don't match, they flip back and the next player takes their turn."));
            rules.add(rule("emoji_events",
                    "The player with the most pairs at the end wins."));
        }
        rules.add(keyboardRule());

        Button close = new Button("Got it", e -> close());
        close.addClassName("btn-gradient");

        Div actions = new Div(close);
        actions.addClassName("dialog-actions");

        Div root = new Div(iconBadge, title, rules, actions);
        root.addClassName("dhbw-dialog");
        add(root);
    }

    /** One icon + body-text row in the rules list. */
    private static Div rule(String iconName, String text) {
        Span sym = new Span(iconName);
        sym.addClassName("material-symbols-outlined");
        Span body = new Span(text);
        Div row = new Div(sym, body);
        row.addClassName("dialog-rule");
        return row;
    }

    /** Final rules row: prose with inline {@code .kbd} chips for each key. */
    private static Div keyboardRule() {
        Span sym = new Span("keyboard");
        sym.addClassName("material-symbols-outlined");

        Span body = new Span();
        body.add(new Span("Move with "));
        body.add(kbd("↑"));
        body.add(new Span(" "));
        body.add(kbd("↓"));
        body.add(new Span(" "));
        body.add(kbd("←"));
        body.add(new Span(" "));
        body.add(kbd("→"));
        body.add(new Span(", flip with "));
        body.add(kbd("Space"));
        body.add(new Span(" or "));
        body.add(kbd("Enter"));
        body.add(new Span("."));

        Div row = new Div(sym, body);
        row.addClassName("dialog-rule");
        return row;
    }

    private static Span kbd(String text) {
        Span k = new Span(text);
        k.addClassName("kbd");
        return k;
    }
}
