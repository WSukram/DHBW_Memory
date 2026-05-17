package de.dhbw.memory;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.AppShellSettings;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the DHBW Memory application.
 *
 * <p>Boots an embedded Tomcat server via Spring Boot and serves the
 * Vaadin-based UI at {@code http://localhost:8080}.</p>
 *
 * <p>The {@link SpringBootApplication} annotation triggers Spring Boot's
 * auto-configuration: it scans this package (and sub-packages) for
 * components, services, and Vaadin views, and wires them up automatically.</p>
 *
 * <p>{@link AppShellConfigurator#configurePage} is the application-wide hook
 * for page-level settings. We use it to (a) enable Lumo's dark theme variant
 * globally — so every Vaadin component (buttons, dialogs, inputs) picks up
 * dark colours automatically, matching the WalletPulse site — and (b) preload
 * the Inter font from Google Fonts so our custom CSS can use it.</p>
 *
 * @author Markus Wenninger
 */
@SpringBootApplication
@Push
@Theme(variant = Lumo.DARK)
public class MemoryApplication implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(MemoryApplication.class, args);
    }

    /**
     * Preloads the Inter font from Google Fonts so our inline CSS can use it
     * without each page paying the latency of a render-blocking lookup.
     * (Lumo's dark variant is enabled globally via the class-level
     * {@code @Theme} annotation above.)
     */
    @Override
    public void configurePage(AppShellSettings settings) {
        settings.addLink(
                "https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap",
                java.util.Map.of("rel", "stylesheet"));
    }
}
