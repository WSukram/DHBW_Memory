package de.dhbw.memory.view;

import com.vaadin.flow.component.ClientCallable;
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
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.radiobutton.RadioGroupVariant;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import de.dhbw.memory.controller.GameService;
import de.dhbw.memory.model.Theme;

import java.util.ArrayList;
import java.util.List;

/**
 * Start screen where players configure the game before playing.
 *
 * <p>Mapped to the root URL {@code /} via {@link Route @Route("")}. Visual
 * styling lives in {@code styles.css} ({@code .app-body, .glass-surface,
 * .setup-card, .brand-title, .btn-gradient}); this class is only concerned
 * with form composition and the start-game wiring.</p>
 *
 * @author Markus Wenninger
 */
@Route("")
@PageTitle("DHBW Memory – Start")
public class StartView extends VerticalLayout {

    private final RadioButtonGroup<String> colorTheme = new RadioButtonGroup<>();

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
        // game.js exposes window.dhbwMemory.setTheme — needed by the theme switcher.
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

        // --- Player count (1 or 2) ---
        RadioButtonGroup<Integer> playerCount = new RadioButtonGroup<>();
        playerCount.setLabel("Number of players");
        playerCount.setItems(1, 2);
        playerCount.setValue(1);

        // --- Colour theme (Light / Dark / System) — same radio-button pattern
        //     as the other settings. Initial server-side value is "system"; a
        //     small executeJs call below reconciles it with localStorage on
        //     attach so the visible selection matches the applied theme. ---
        colorTheme.setLabel("Colour theme");
        colorTheme.setItems("light", "dark", "system");
        colorTheme.setValue("system");
        colorTheme.setRenderer(new ComponentRenderer<>(StartView::themeRadioRow));
        colorTheme.addValueChangeListener(e -> {
            if (e.isFromClient() && e.getValue() != null) {
                applyTheme(e.getValue());
            }
        });

        HorizontalLayout topRow = new HorizontalLayout(playerCount, colorTheme);
        topRow.setWidthFull();
        topRow.setSpacing(true);
        topRow.getStyle().set("flex-wrap", "wrap");
        topRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        topRow.setAlignItems(FlexComponent.Alignment.END);

        // --- Names side-by-side. name2 is always laid out, just hidden when 1-player.
        //     visibility:hidden (vs setVisible/display:none) keeps the slot reserved
        //     so toggling players doesn't shift the form vertically. ---
        TextField name1 = new TextField("Player 1 name");
        name1.setValue("Player 1");
        name1.getStyle().set("flex", "1");

        TextField name2 = new TextField("Player 2 name");
        name2.setValue("Player 2");
        name2.getStyle().set("flex", "1").set("visibility", "hidden");

        playerCount.addValueChangeListener(e ->
                name2.getStyle().set("visibility", e.getValue() == 2 ? "visible" : "hidden"));

        HorizontalLayout names = new HorizontalLayout(name1, name2);
        names.setWidthFull();
        names.setSpacing(true);
        names.getStyle().set("flex-wrap", "wrap");

        // --- Grid size (4 or 6) — stacked vertically to mirror the theme picker. ---
        RadioButtonGroup<Integer> gridSize = new RadioButtonGroup<>();
        gridSize.setLabel("Grid size");
        gridSize.setItems(4, 6);
        gridSize.setValue(4);
        gridSize.setItemLabelGenerator(s -> s + " × " + s);
        gridSize.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);

        // --- Theme as a RadioButtonGroup so the icon stays visible at all times.
        //     A Select hides the renderer once the popup closes, so an icon-only
        //     preview vanishes after selection — radio buttons sidestep that. ---
        RadioButtonGroup<Theme> theme = new RadioButtonGroup<>();
        theme.setLabel("Theme");
        theme.setItems(Theme.values());
        theme.setValue(Theme.CRYPTO);
        theme.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);
        theme.setRenderer(new ComponentRenderer<>(t -> {
            String motif = t.getMotifsFor(4).get(0);
            Image icon = new Image("/images/" + t.getFolder() + "/" + motif + ".svg", t.name());
            // display:block + fixed square + object-fit:contain keep the SVG dead-centre
            // regardless of whitespace in its viewBox.
            icon.getStyle()
                    .set("width", "24px").set("height", "24px")
                    .set("object-fit", "contain")
                    .set("display", "block")
                    .set("flex-shrink", "0");

            String label = t.name().charAt(0) + t.name().substring(1).toLowerCase();
            Span text = new Span(label);
            text.getStyle().set("line-height", "1");

            // Plain Div + display:flex bypasses HorizontalLayout's Lumo padding,
            // which was nudging the icon off-centre relative to the text baseline.
            Div row = new Div(icon, text);
            row.getStyle()
                    .set("display", "inline-flex")
                    .set("align-items", "center")
                    .set("gap", "10px")
                    .set("padding", "0")
                    .set("margin", "0");
            return row;
        }));

        // --- Grid size + theme side-by-side, wrapping below the breakpoint ---
        HorizontalLayout settings = new HorizontalLayout(gridSize, theme);
        settings.setWidthFull();
        settings.setSpacing(true);
        settings.getStyle().set("flex-wrap", "wrap");
        gridSize.getStyle().set("flex", "1").set("min-width", "140px");
        theme.getStyle().set("flex", "1").set("min-width", "180px");

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

        // --- Glass card container groups all controls under the title. ---
        VerticalLayout card = new VerticalLayout(topRow, names, settings, startBtn);
        card.setSpacing(true);
        card.setMaxWidth("640px");
        card.setWidthFull();
        card.addClassNames("glass-surface", "setup-card");

        add(header, subtitle, card);

        // Reconcile the radio's visual state with localStorage after the
        // element is attached. Without this, a returning user who chose
        // "dark" last session would see the page rendered dark but the radio
        // still pointing at "system".
        getElement().executeJs(
                "var pref = localStorage.getItem('dhbw-memory-theme') || 'system';"
                        + "this.$server.onThemeSync(pref);");
    }

    /** Called from the client after page load to mirror localStorage into the radio. */
    @ClientCallable
    public void onThemeSync(String pref) {
        if (pref != null && !pref.equals(colorTheme.getValue())) {
            // The change listener guards on isFromClient() so setting the
            // value here does not loop back into applyTheme().
            colorTheme.setValue(pref);
        }
    }

    /** Renders one colour-theme option: a Material Symbols icon + capitalised label. */
    private static Div themeRadioRow(String value) {
        String iconName = switch (value) {
            case "light"  -> "light_mode";
            case "dark"   -> "dark_mode";
            default       -> "desktop_windows";
        };
        String label = switch (value) {
            case "light"  -> "Light";
            case "dark"   -> "Dark";
            default       -> "System";
        };

        Span icon = new Span(iconName);
        icon.addClassName("material-symbols-outlined");

        Span s = new Span(label);
        s.getStyle().set("line-height", "1");

        Div row = new Div(icon, s);
        row.getStyle()
                .set("display", "inline-flex")
                .set("align-items", "center")
                .set("gap", "8px");
        return row;
    }

    /** Calls into {@code window.dhbwMemory.setTheme} so the choice is persisted + applied. */
    private static void applyTheme(String mode) {
        UI.getCurrent().getElement().executeJs(
                "window.dhbwMemory && window.dhbwMemory.setTheme($0);", mode);
    }
}
