package de.dhbw.memory.view.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableFunction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Replacement for {@link com.vaadin.flow.component.radiobutton.RadioButtonGroup}
 * that renders as a pill-row "segmented control" instead of stacked radio dots.
 *
 * <p>The container is a {@code <div role="radiogroup">} holding one
 * {@code <button role="radio">} per item. {@code NativeButton} is used (rather
 * than the Lumo-styled {@code Button}) so the look comes entirely from
 * {@code styles.css} — the {@code .segmented} pill variant and the {@code .chips}
 * card variant share the same DOM structure. Arrow-key navigation is wired by
 * {@code /static/game.js#initSegmented} after attach.</p>
 *
 * <p>{@link #setValue(Object)} is silent (programmatic) by design; listeners
 * registered via {@link #addValueChangeListener(SerializableConsumer)} only fire
 * for user-initiated clicks so callers can call {@code setValue} from a sync
 * path without re-entering their own handler.</p>
 *
 * @param <T> the item type held by the control
 * @author Markus Wenninger
 */
public class SegmentedControl<T> extends Div {

    private final List<T> items = new ArrayList<>();
    private final Map<T, NativeButton> segments = new LinkedHashMap<>();
    private final List<SerializableConsumer<T>> listeners = new ArrayList<>();
    private SerializableFunction<T, Component> renderer = item -> new Span(String.valueOf(item));
    private T value;

    /** Creates an empty segmented control with the default pill-row appearance. */
    public SegmentedControl() {
        addClassName("segmented");
        getElement().setAttribute("role", "radiogroup");
    }

    /**
     * Adds a CSS variant modifier (e.g. {@code "icon-only"}, {@code "chips"}).
     * Chainable so it composes cleanly at construction time.
     */
    public SegmentedControl<T> withVariant(String cssClass) {
        addClassName(cssClass);
        return this;
    }

    /** Replaces the choices and rebuilds the DOM. */
    @SafeVarargs
    public final void setItems(T... newItems) {
        items.clear();
        items.addAll(Arrays.asList(newItems));
        rebuild();
    }

    /** Overrides the per-item renderer; defaults to {@code String.valueOf(item)}. */
    public void setRenderer(SerializableFunction<T, Component> renderer) {
        this.renderer = renderer;
        rebuild();
    }

    /** Programmatic selection — does NOT fire value-change listeners. */
    public void setValue(T newValue) {
        if (Objects.equals(value, newValue)) return;
        value = newValue;
        updateActive();
    }

    /** Returns the currently selected item, or {@code null} if none. */
    public T getValue() {
        return value;
    }

    /** Subscribes to user-initiated selection changes. */
    public void addValueChangeListener(SerializableConsumer<T> listener) {
        listeners.add(listener);
    }

    private void rebuild() {
        removeAll();
        segments.clear();
        for (T item : items) {
            NativeButton seg = new NativeButton();
            seg.addClassName("seg");
            // type=button so the element never accidentally submits a parent form.
            seg.getElement().setAttribute("type", "button");
            seg.getElement().setAttribute("role", "radio");
            seg.getElement().setAttribute("aria-checked", "false");
            seg.add(renderer.apply(item));
            seg.addClickListener(e -> setValueFromClient(item));
            segments.put(item, seg);
            add(seg);
        }
        updateActive();
    }

    private void setValueFromClient(T item) {
        if (Objects.equals(value, item)) return;
        value = item;
        updateActive();
        listeners.forEach(l -> l.accept(item));
    }

    private void updateActive() {
        segments.forEach((key, btn) -> {
            boolean active = Objects.equals(key, value);
            btn.getElement().getClassList().set("active", active);
            btn.getElement().setAttribute("aria-checked", String.valueOf(active));
        });
    }
}
