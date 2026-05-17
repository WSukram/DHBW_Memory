package de.dhbw.memory.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import de.dhbw.memory.controller.GameService;
import de.dhbw.memory.model.Theme;

import java.util.ArrayList;
import java.util.List;

/**
 * Start screen where players configure the game before playing.
 *
 * <p>Mapped to the root URL {@code /} via {@link Route @Route("")}. Extending
 * {@link VerticalLayout} means the view itself is a vertical container —
 * everything we {@code add()} stacks from top to bottom.</p>
 *
 * @author Markus Wenninger
 */
@Route("")
@PageTitle("DHBW Memory – Start")
public class StartView extends VerticalLayout {

    /**
     * Spring injects {@link GameService} here automatically because it is a
     * {@code @Service} bean. Constructor injection (receiving it as a parameter)
     * is the recommended style — it makes the dependency explicit and testable.
     */
    public StartView(GameService gameService) {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        // Match the WalletPulse site: deep navy background + Inter font.
        getStyle()
                .set("background", "#0b1326")
                .set("font-family", "'Inter', sans-serif")
                .set("color", "#dae2fd");

        // --- Player count ---
        // RadioButtonGroup<Integer> means each option is an Integer value.
        // Vaadin renders it as a set of radio buttons; we pick 1 or 2 players.
        RadioButtonGroup<Integer> playerCount = new RadioButtonGroup<>();
        playerCount.setLabel("Number of players");
        playerCount.setItems(1, 2);
        playerCount.setValue(1);

        TextField name1 = new TextField("Player 1 name");
        name1.setValue("Player 1");

        TextField name2 = new TextField("Player 2 name");
        name2.setValue("Player 2");
        // Hidden by default — only shown when 2-player mode is selected.
        name2.setVisible(false);

        // Value-change listener: fires every time the radio selection changes.
        // e.getValue() returns the newly selected Integer (1 or 2).
        playerCount.addValueChangeListener(e -> name2.setVisible(e.getValue() == 2));

        // --- Grid size ---
        RadioButtonGroup<Integer> gridSize = new RadioButtonGroup<>();
        gridSize.setLabel("Grid size");
        gridSize.setItems(4, 6);
        gridSize.setValue(4);
        // ItemLabelGenerator controls the display text for each option.
        gridSize.setItemLabelGenerator(s -> s + " × " + s);

        // --- Theme ---
        // Select<Theme> is a dropdown. Theme.values() returns all enum constants.
        Select<Theme> theme = new Select<>();
        theme.setLabel("Theme");
        theme.setItems(Theme.values());
        theme.setValue(Theme.CRYPTO);
        // Show "Crypto" / "Space" instead of the raw enum name.
        theme.setItemLabelGenerator(t -> {
            String name = t.name();
            return name.charAt(0) + name.substring(1).toLowerCase();
        });

        // --- Start button ---
        Button startBtn = new Button("Start Game", e -> {
            List<String> names = new ArrayList<>();
            String p1 = name1.getValue().isBlank() ? "Player 1" : name1.getValue().trim();
            names.add(p1);
            if (playerCount.getValue() == 2) {
                String p2 = name2.getValue().isBlank() ? "Player 2" : name2.getValue().trim();
                names.add(p2);
            }

            // Tell the service to create a new Game with the chosen settings.
            gameService.startGame(names, gridSize.getValue(), theme.getValue());

            // Navigate to the game view — Vaadin replaces the current page content
            // without a full browser reload (single-page app behaviour).
            UI.getCurrent().navigate(GameView.class);
        });
        startBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);

        H1 title = new H1("DHBW Memory");
        // WalletPulse brand gradient (purple → cyan) clipped to the text.
        // Both vendor-prefixed and standard background-clip are set for browser support.
        title.getStyle()
                .set("background", "linear-gradient(90deg, #863bff, #47bfff)")
                .set("-webkit-background-clip", "text")
                .set("background-clip", "text")
                .set("color", "transparent")
                .set("font-weight", "800")
                .set("letter-spacing", "-0.02em")
                .set("margin-bottom", "0");

        H3 subtitle = new H3("Game Setup");
        subtitle.getStyle().set("color", "#c7c4d8").set("font-weight", "500");

        add(title, subtitle, playerCount, name1, name2, gridSize, theme, startBtn);
    }
}
