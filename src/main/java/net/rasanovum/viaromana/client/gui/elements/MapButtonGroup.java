package net.rasanovum.viaromana.client.gui.elements;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages a group of MapCycleButtons to ensure only one can be selected at a time
 */
public class MapButtonGroup<T> {
    private final List<MapCycleButton<T>> buttons = new ArrayList<>();
    private MapCycleButton<T> selectedButton = null;
    private final java.util.function.Consumer<T> onSelectionChange;

    public MapButtonGroup(java.util.function.Consumer<T> onSelectionChange) {
        this.onSelectionChange = onSelectionChange;
    }

    public void addButton(MapCycleButton<T> button) {
        this.buttons.add(button);
        
        button.setOnPress(() -> {
            selectButton(button);
        });
    }

    public void selectButton(MapCycleButton<T> button) {
        if (!this.buttons.contains(button)) {
            return;
        }

        for (MapCycleButton<T> btn : this.buttons) {
            btn.setSelected(false);
        }

        button.setSelected(true);
        this.selectedButton = button;

        if (this.onSelectionChange != null) {
            this.onSelectionChange.accept(button.getValue());
        }
    }

    public void selectByValue(T value) {
        for (MapCycleButton<T> button : this.buttons) {
            if (button.getValue().equals(value)) {
                selectButton(button);
                break;
            }
        }
    }

    public T getSelectedValue() {
        return this.selectedButton != null ? this.selectedButton.getValue() : null;
    }

    public MapCycleButton<T> getSelectedButton() {
        return this.selectedButton;
    }
}
