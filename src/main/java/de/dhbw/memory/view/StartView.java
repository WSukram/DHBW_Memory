package de.dhbw.memory.view;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import de.dhbw.memory.controller.GameService;
import de.dhbw.memory.model.Theme;
import de.dhbw.memory.view.component.SegmentedControl;

import java.util.ArrayList;
import java.util.List;

/**
 * Start screen where players configure the game before playing.
 *
 * <p>Mapped to the root URL {@code /} via {@link Route @Route("")}. Visual
 * styling lives in {@code styles.css} ({@code .app-body, .surface-card,
 * .setup-card, .segmented, .chips, .field-group, .brand-title, .btn-gradient});
 * this class is only concerned with form composition and the start-game wiring.
 * Setting choices use the project's own {@link SegmentedControl} instead of
 * Vaadin's stock {@code RadioButtonGroup} to match the WalletPulse look.</p>
 *
 * @author Markus Wenninger
 */
@Route("")
@PageTitle("DHBW Memory – Start")
public class StartView extends VerticalLayout {

    private final SegmentedControl<String> colorTheme = new SegmentedControl<>();

    /**
     * Spring injects {@link GameService} (it is a {@code @Service} bean).
     * Constructor injection makes the dependency explicit and testable.
     */
    public StartView(GameService gameService) {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        setPadding(true);
        addClassName("app-body");

        UI.getCurrent().getPage().addStyleSheet("/styles.css");
        // game.js exposes window.dhbwMemory.{setTheme, initSegmented}.
        UI.getCurrent().getPage().addJavaScript("/game.js");

        // --- Header: WP icon + brand title side-by-side ---
        Image brandIcon = new Image("/images/wp-icon.svg", "WalletPulse");
        brandIcon.addClassName("brand-icon");

        H1 title = new H1("DHBW Memory");
        title.addClassName("brand-title");

        HorizontalLayout header = new HorizontalLayout(brandIcon, title);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setSpacing(true);

        H3 subtitle = new H3("Game Setup");
        subtitle.addClassName("setup-subtitle");

        // --- Player count ("1 Player" / "2 Players") ---
        SegmentedControl<Integer> playerCount = new SegmentedControl<>();
        playerCount.setItems(1, 2);
        playerCount.setRenderer(n -> new Span(n + (n == 1 ? " Player" : " Players")));
        playerCount.setValue(1);

        // --- Colour theme (icon-only segmented: ☀ / 🌙 / 🖥) ---
        // Initial server-side value is "system"; a small executeJs call below
        // reconciles it with localStorage on attach so the visible selection
        // matches the applied theme.
        colorTheme.withVariant("icon-only");
        colorTheme.setItems("light", "dark", "system");
        colorTheme.setRenderer(StartView::themeIcon);
        colorTheme.setValue("system");
        colorTheme.addValueChangeListener(StartView::applyTheme);

        Div playersField = fieldGroup("Players", playerCount);
        Div themeField = fieldGroup("Appearance", colorTheme);

        HorizontalLayout topRow = new HorizontalLayout(playersField, themeField);
        topRow.setWidthFull();
        topRow.setSpacing(true);
        topRow.getStyle().set("flex-wrap", "wrap");
        topRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        topRow.setAlignItems(FlexComponent.Alignment.END);

        // --- Names side-by-side. The second field group stays laid out but
        //     becomes visibility:hidden in 1-player mode so the form does not
        //     reflow vertically when the player count toggles. ---
        TextField name1 = new TextField();
        name1.setPlaceholder("Player 1");
        name1.setValue("Player 1");

        TextField name2 = new TextField();
        name2.setPlaceholder("Player 2");
        name2.setValue("Player 2");

        Div name1Field = fieldGroup("Player 1 name", name1);
        Div name2Field = fieldGroup("Player 2 name", name2);
        name1Field.getStyle().set("flex", "1");
        name2Field.getStyle().set("flex", "1").set("visibility", "hidden");
        name1.setWidthFull();
        name2.setWidthFull();

        playerCount.addValueChangeListener(v ->
                name2Field.getStyle().set("visibility", v == 2 ? "visible" : "hidden"));

        HorizontalLayout names = new HorizontalLayout(name1Field, name2Field);
        names.setWidthFull();
        names.setSpacing(true);
        names.getStyle().set("flex-wrap", "wrap");

        // --- Grid size ("4 × 4" / "6 × 6") ---
        SegmentedControl<Integer> gridSize = new SegmentedControl<>();
        gridSize.setItems(4, 6);
        gridSize.setRenderer(n -> new Span(n + " × " + n));
        gridSize.setValue(4);

        // --- Theme picker rendered as the larger ".chips" variant: bigger
        //     card-style options with the motif preview icon + label. ---
        SegmentedControl<Theme> theme = new SegmentedControl<Theme>().withVariant("chips");
        theme.setItems(Theme.values());
        theme.setRenderer(t -> {
            String motif = t.getMotifsFor(4).get(0);
            Image icon = new Image("/images/" + t.getFolder() + "/" + motif + ".svg", t.name());
            icon.addClassName("chip-icon");

            String label = t.name().charAt(0) + t.name().substring(1).toLowerCase();
            Span text = new Span(label);

            Div row = new Div(icon, text);
            row.addClassName("chip-row");
            return row;
        });
        theme.setValue(Theme.CRYPTO);

        Div gridField = fieldGroup("Grid size", gridSize);
        Div themePickerField = fieldGroup("Theme", theme);
        themePickerField.getStyle().set("flex", "1").set("min-width", "220px");

        HorizontalLayout settings = new HorizontalLayout(gridField, themePickerField);
        settings.setWidthFull();
        settings.setSpacing(true);
        settings.getStyle().set("flex-wrap", "wrap");

        // --- Start button (gradient styling lives in .btn-gradient) ---
        Button startBtn = new Button("Start Game", e -> {
            List<String> playerNames = new ArrayList<>();
            String p1 = name1.getValue().isBlank() ? "Player 1" : name1.getValue().trim();
            playerNames.add(p1);
            if (playerCount.getValue() == 2) {
                String p2 = name2.getValue().isBlank() ? "Player 2" : name2.getValue().trim();
                playerNames.add(p2);
            }
            gameService.startGame(playerNames, gridSize.getValue(), theme.getValue());
            UI.getCurrent().navigate(GameView.class);
        });
        startBtn.addThemeVariants(ButtonVariant.LUMO_LARGE);
        startBtn.addClassName("btn-gradient");

        // --- Solid (non-glass) surface card groups all controls under the
        //     title — matches WalletPulse's surface-container pattern. ---
        VerticalLayout card = new VerticalLayout(topRow, names, settings, startBtn);
        card.setSpacing(true);
        card.setMaxWidth("640px");
        card.setWidthFull();
        card.addClassNames("surface-card", "setup-card");

        add(header, subtitle, card);

        // Reconcile the colour-theme selection with localStorage, and wire
        // arrow-key navigation across each segmented control on this page.
        getElement().executeJs(
                "var pref = localStorage.getItem('dhbw-memory-theme') || 'system';"
                        + "this.$server.onThemeSync(pref);"
                        + "window.dhbwMemory && window.dhbwMemory.initSegmented "
                        + "&& window.dhbwMemory.initSegmented();");
    }

    /** Called from the client after page load to mirror localStorage into the radio. */
    @ClientCallable
    public void onThemeSync(String pref) {
        // setValue is silent (does not fire the change listener), so this
        // does not loop back into applyTheme.
        if (pref != null && !pref.equals(colorTheme.getValue())) {
            colorTheme.setValue(pref);
        }
    }

    /** Wraps a control with a small uppercase label above it. */
    private static Div fieldGroup(String labelText, Component control) {
        Span label = new Span(labelText);
        label.addClassName("field-label");

        Div group = new Div(label, control);
        group.addClassName("field-group");
        return group;
    }

    /** Renders one colour-theme option as a Material Symbols icon. */
    private static Div themeIcon(String value) {
        String iconName = switch (value) {
            case "light"  -> "light_mode";
            case "dark"   -> "dark_mode";
            default       -> "desktop_windows";
        };

        Span icon = new Span(iconName);
        icon.addClassName("material-symbols-outlined");

        Div row = new Div(icon);
        row.addClassName("icon-cell");
        return row;
    }

    /** Calls into {@code window.dhbwMemory.setTheme} so the choice is persisted + applied. */
    private static void applyTheme(String mode) {
        UI.getCurrent().getElement().executeJs(
                "window.dhbwMemory && window.dhbwMemory.setTheme($0);", mode);
    }
}
