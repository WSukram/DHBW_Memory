package de.dhbw.memory;

import com.vaadin.flow.component.page.AppShellConfigurator;
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
 * <p>{@link AppShellConfigurator} marks this class as the place where
 * application-wide Vaadin settings (viewport, theme, push-mode, etc.)
 * belong. We use Vaadin's default Lumo theme for now; a custom theme
 * can be added later via {@code @Theme("memory")} once we provide the
 * theme folder under {@code src/main/frontend/themes/memory/}.</p>
 *
 * @author Markus Wenninger
 */
@SpringBootApplication
public class MemoryApplication implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(MemoryApplication.class, args);
    }
}
