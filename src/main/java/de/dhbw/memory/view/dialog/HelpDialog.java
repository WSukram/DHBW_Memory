package de.dhbw.memory.view.dialog;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * "How to play" overlay. Wording adapts to single- vs multi-player mode so
 * the rules read naturally regardless of the game type.
 *
 * <p>Usage: {@code new HelpDialog(singlePlayer).open();} — the dialog is
 * self-contained, no external state needed.</p>
 *
 * @author Markus Wenninger
 */
public class HelpDialog extends Dialog {

    /**
     * @param singlePlayer true if the current game has exactly one player; switches
     *                     the rules text away from "next player" / "winner" phrasing
     */
    public HelpDialog(boolean singlePlayer) {
        VerticalLayout content = new VerticalLayout();
        content.setAlignItems(VerticalLayout.Alignment.START);
        content.setWidth("320px");

        content.add(new H3("How to play"));
        content.add(new Paragraph(
                "Flip two cards per turn. If they match, you score a point and play again."));

        if (singlePlayer) {
            content.add(new Paragraph(
                    "If they don't match, they flip back. Try to find all pairs in as few moves as possible."));
        } else {
            content.add(new Paragraph(
                    "If they don't match, they flip back and the next player takes their turn."));
            content.add(new Paragraph("The player with the most pairs at the end wins."));
        }

        Paragraph keys = new Paragraph("Keyboard: Arrow keys to move · Space or Enter to flip");
        keys.addClassName("help-keys");
        content.add(keys);

        Button close = new Button("Got it", e -> close());
        close.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        content.add(close);

        add(content);
    }
}
