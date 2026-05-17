package de.dhbw.memory.view.dialog;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * Confirmation overlay shown when the user clicks the back-arrow mid-game.
 * Cancel just closes; the confirm action is delegated via a {@link Runnable}
 * so this dialog doesn't have to know about routing.
 *
 * @author Markus Wenninger
 */
public class QuitConfirmDialog extends Dialog {

    /**
     * @param onConfirm executed after the user confirms — typically navigates back
     *                  to the start view
     */
    public QuitConfirmDialog(Runnable onConfirm) {
        VerticalLayout content = new VerticalLayout();
        content.setAlignItems(VerticalLayout.Alignment.CENTER);
        content.setWidth("320px");

        content.add(new H3("End current game?"));
        content.add(new Paragraph("Your progress will be lost."));

        Button cancel = new Button("Cancel", e -> close());
        Button confirm = new Button("End Game", e -> {
            close();
            onConfirm.run();
        });
        confirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);

        content.add(new HorizontalLayout(cancel, confirm));
        add(content);
    }
}
