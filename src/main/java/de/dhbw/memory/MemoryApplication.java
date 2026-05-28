package de.dhbw.memory;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Inline;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.page.TargetElement;
import com.vaadin.flow.server.AppShellSettings;
import com.vaadin.flow.shared.ui.Transport;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Map;

/**
 * Entry point of the DHBW Memory application.
 *
 * <p>Boots an embedded Tomcat server via Spring Boot and serves the
 * Vaadin-based UI at {@code http://localhost:8080}.</p>
 *
 * <p>{@link SpringBootApplication} triggers Spring Boot's auto-configuration:
 * it scans this package (and sub-packages) for components, services, and
 * Vaadin views, and wires them up automatically.</p>
 *
 * <p>{@link AppShellConfigurator#configurePage} is the application-wide hook
 * for page-level settings. We use it to (a) preload the Geist + JetBrains Mono
 * fonts (soft-stack typography), (b) wire the WP icon as a favicon, and
 * (c) inject a tiny inline script that sets
 * the {@code theme} attribute on {@code <html>} before paint — so the page
 * never flashes the wrong colour scheme on load.</p>
 *
 * @author Markus Wenninger
 */
@SpringBootApplication
@Push(transport = Transport.WEBSOCKET_XHR)
public class MemoryApplication implements AppShellConfigurator {

    /**
     * Boots the Spring Boot context (and, with it, the embedded Tomcat
     * + Vaadin runtime) so the game is reachable in the browser.
     *
     * @param args standard JVM command-line arguments, forwarded to Spring
     */
    public static void main(String[] args) {
        SpringApplication.run(MemoryApplication.class, args);
    }

    @Override
    public void configurePage(AppShellSettings settings) {
        // Soft-stack typography: Geist for UI/body + JetBrains Mono for tabular
        // numbers (timer, stats). Matches the visual language of the WalletPulse
        // sister project so both sites share a typographic feel.
        settings.addLink(
                "https://fonts.googleapis.com/css2?family=Geist:wght@400;500;600;700;800&family=JetBrains+Mono:wght@400;500;600&display=swap",
                Map.of("rel", "stylesheet"));

        // Material Symbols Outlined — same icon set used on memory.walletpulse.de
        // so the colour-theme picker (dark_mode / light_mode / desktop_windows)
        // looks identical here.
        settings.addLink(
                "https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@24,400,0,0",
                Map.of("rel", "stylesheet"));

        // WalletPulse mark as browser tab favicon (SVG so it stays crisp).
        settings.addFavIcon("icon", "/images/wp-icon.svg", "32x32");
        settings.addLink("/images/wp-icon.svg", Map.of("rel", "icon", "type", "image/svg+xml"));

        // Inline theme bootstrap — runs synchronously before <body> paints so
        // there is no flash of the wrong theme. Reads the stored preference
        // (default: 'system') and applies the resolved theme to <html>.
        // PREPEND so the script is the very first child of <head>, before any
        // stylesheet/script that could rely on the theme attribute.
        settings.addInlineWithContents(
                TargetElement.HEAD,
                Inline.Position.PREPEND,
                "(function(){"
                        + "var pref=localStorage.getItem('dhbw-memory-theme')||'system';"
                        + "var actual=pref==='system'"
                        + "?(window.matchMedia('(prefers-color-scheme: dark)').matches?'dark':'light')"
                        + ":pref;"
                        + "document.documentElement.setAttribute('theme',actual);"
                        + "})();",
                Inline.Wrapping.JAVASCRIPT);
    }
}
