package de.dhbw.memory.view.dialog;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import de.dhbw.memory.model.Game;
import de.dhbw.memory.model.Player;
import de.dhbw.memory.view.Labels;

/**
 * End-of-game overlay: winner / tie headline, per-player stats, elapsed time,
 * and two restart options.
 *
 * <p>Cannot be dismissed by clicking outside (the user actively chooses how to
 * continue). Shares the {@code theme="dhbw"} overlay treatment with the other
 * dialogs and adds a {@code .stats-panel} block for per-player rows plus a
 * {@code .stats-footer} for time + total moves. The headline uses the brand
 * gradient via {@code .dialog-title-gradient} to echo the start screen.</p>
 *
 * @author Markus Wenninger
 */
public class EndGameDialog extends Dialog {

    /**
     * Builds the end-of-game overlay for the given finished {@link Game}: headline
     * (winner / tie / single-player completion), per-player stats panel, elapsed
     * time and total moves, and the two restart buttons.
     *
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
        getElement().getThemeList().add("dhbw");
        setWidth("440px");

        Span iconSym = new Span("emoji_events");
        iconSym.addClassName("material-symbols-outlined");
        Div iconBadge = new Div(iconSym);
        iconBadge.addClassNames("dialog-icon", "dialog-icon-trophy");

        H2 title = new H2(headline(game));
        title.addClassNames("dialog-title", "dialog-title-gradient");

        Div stats = new Div();
        stats.addClassName("stats-panel");

        Player winner = game.winner();
        boolean multi = game.getPlayers().size() > 1;
        for (Player player : game.getPlayers()) {
            stats.add(statsRow(player, winner, multi));
        }

        Div footer = new Div();
        footer.addClassName("stats-footer");
        footer.add(footerItem("schedule", formatTime(elapsedSeconds)));
        footer.add(footerItem("touch_app", movesLabel(game.getTotalTurns())));

        Button sameSettings = new Button("Same Settings", e -> {
            close();
            onSameSettings.run();
        });
        sameSettings.addClassName("btn-ghost");

        Button newSetup = new Button("New Setup", e -> {
            close();
            onNewSetup.run();
        });
        newSetup.addClassName("btn-gradient");

        Div actions = new Div(sameSettings, newSetup);
        actions.addClassName("dialog-actions");

        Div root = new Div(iconBadge, title, stats, footer, actions);
        root.addClassName("dhbw-dialog");
        add(root);
    }

    private static Div statsRow(Player player, Player winner, boolean multi) {
        int accuracy = player.getTurns() > 0
                ? (player.getScore() * 100 / player.getTurns())
                : 0;

        Span name = new Span(player.getName());
        name.addClassName("stats-name");
        Div left = new Div(name);
        if (multi && winner == player) {
            Span pill = new Span("Winner");
            pill.addClassName("winner-pill");
            left.add(pill);
        }

        Span meta = new Span(Labels.pairsLabel(player.getScore()) + " · " + accuracy + "% accuracy");
        meta.addClassName("stats-meta");
        Div right = new Div(meta);

        Div row = new Div(left, right);
        row.addClassName("stats-row");
        return row;
    }

    private static Div footerItem(String iconName, String text) {
        Span sym = new Span(iconName);
        sym.addClassName("material-symbols-outlined");
        Span body = new Span(text);
        Div item = new Div(sym, body);
        item.addClassName("stats-footer-item");
        return item;
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

    private static String movesLabel(int n) {
        return n == 1 ? "1 move" : n + " moves";
    }
}
