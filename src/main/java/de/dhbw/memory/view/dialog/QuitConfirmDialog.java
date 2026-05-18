package de.dhbw.memory.view.dialog;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;

/**
 * Confirmation overlay shown when the user clicks the back-arrow mid-game.
 *
 * <p>The dialog opts into the {@code theme="dhbw"} overlay treatment defined in
 * {@code styles.css} (solid surface-container shell + blurred backdrop) and
 * structures its content as the shared {@code .dhbw-dialog} layout: icon badge,
 * gradient/title, supporting paragraph, and a {@code .dialog-actions} button
 * row. The confirm action is delegated via a {@link Runnable} so this dialog
 * does not have to know about routing.</p>
 *
 * @author Markus Wenninger
 */
public class QuitConfirmDialog extends Dialog {

    /**
     * @param onConfirm executed after the user confirms — typically navigates back
     *                  to the start view
     */
    public QuitConfirmDialog(Runnable onConfirm) {
        // theme="dhbw" propagates onto the overlay element so the ::part(...)
        // rules in styles.css take over from Lumo's default dialog look.
        getElement().getThemeList().add("dhbw");
        setWidth("400px");

        Span iconSym = new Span("logout");
        iconSym.addClassName("material-symbols-outlined");
        Div iconBadge = new Div(iconSym);
        iconBadge.addClassNames("dialog-icon", "dialog-icon-warning");

        H2 title = new H2("End current game?");
        title.addClassName("dialog-title");

        Paragraph body = new Paragraph("Your progress will be lost.");
        body.addClassName("dialog-body");

        Button cancel = new Button("Cancel", e -> close());
        cancel.addClassName("btn-ghost");

        Button confirm = new Button("End Game", e -> {
            close();
            onConfirm.run();
        });
        confirm.addClassName("btn-danger");

        Div actions = new Div(cancel, confirm);
        actions.addClassName("dialog-actions");

        Div root = new Div(iconBadge, title, body, actions);
        root.addClassName("dhbw-dialog");
        add(root);
    }
}
