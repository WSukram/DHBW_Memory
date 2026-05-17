package de.dhbw.memory.view;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * Placeholder landing page used to confirm that Spring Boot and Vaadin start
 * correctly. Will be replaced by the real start menu in Phase 4.
 *
 * <p>{@link Route @Route("")} maps this view to the root URL ({@code /}).
 * Extending {@link VerticalLayout} means this class <em>is</em> a vertical
 * Vaadin layout — anything we {@code add()} in the constructor becomes a
 * child component shown in the browser.</p>
 *
 * @author Markus Wenninger
 */
@Route("")
@PageTitle("DHBW Memory")
public class HelloView extends VerticalLayout {

    public HelloView() {
        add(new H1("DHBW Memory"));
        add(new Paragraph("Memory is starting… (Phase 1 skeleton)"));

        // Fill the browser viewport and center the content for a clean first impression.
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
    }
}
