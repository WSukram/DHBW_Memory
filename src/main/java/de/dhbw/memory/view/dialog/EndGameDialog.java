package de.dhbw.memory.view.dialog;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.dhbw.memory.model.Game;
import de.dhbw.memory.model.Player;

/**
 * End-of-game overlay: winner / tie headline, per-player stats, elapsed time,
 * and two restart options. Cannot be dismissed by clicking outside so the
 * user actively chooses how to continue.
 *
 * @author Markus Wenninger
 */
public class EndGameDialog extends Dialog {

    /**
     * @param game            the finished game (used for winner detection + stats)
     * @param elapsedSeconds  total play time
     * @param onSameSettings  invoked when the user clicks "Same Settings" — should
     *                        rebuild the board in place with identical config
     * @param onNewSetup      invoked when the user clicks "New Setup" — typically
     *                        navigates back to {@code StartView}
     */
    public EndGameDialog(Game game,
                         long elapsedSeconds,
                         Runnable onSameSettings,
                         Runnable onNewSetup) {
        setCloseOnOutsideClick(false);

        VerticalLayout content = new VerticalLayout();
        content.setAlignItems(VerticalLayout.Alignment.CENTER);

        content.add(new H2(headline(game)));

        boolean solo = game.getPlayers().size() == 1;
        for (Player player : game.getPlayers()) {
            int accuracy = player.getTurns() > 0
                    ? (player.getScore() * 100 / player.getTurns())
                    : 0;
            String pairs = pairsLabel(player.getScore());
            String label = solo
                    ? pairs + "  ·  " + accuracy + "% accuracy"
                    : player.getName() + ": " + pairs + "  ·  " + accuracy + "% accuracy";
            content.add(new Paragraph(label));
        }

        content.add(new Paragraph("Time: " + formatTime(elapsedSeconds)
                + "  ·  " + movesLabel(game.getTotalTurns())));

        Button sameSettings = new Button("Same Settings", e -> {
            close();
            onSameSettings.run();
        });
        sameSettings.addThemeVariants(ButtonVariant.LUMO_LARGE);

        Button newSetup = new Button("New Setup", e -> {
            close();
            onNewSetup.run();
        });
        newSetup.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);

        content.add(new HorizontalLayout(sameSettings, newSetup));
        add(content);
    }

    private static String headline(Game game) {
        if (game.isTie()) return "It's a tie!";
        if (game.winner() != null) {
            return game.getPlayers().size() == 1
                    ? "You finished!"
                    : game.winner().getName() + " wins!";
        }
        return "Game over!";
    }

    private static String formatTime(long elapsedSeconds) {
        long m = elapsedSeconds / 60;
        long s = elapsedSeconds % 60;
        return m + ":" + (s < 10 ? "0" : "") + s;
    }

    private static String pairsLabel(int n) {
        return n == 1 ? "1 pair" : n + " pairs";
    }

    private static String movesLabel(int n) {
        return n == 1 ? "1 move" : n + " moves";
    }
}
