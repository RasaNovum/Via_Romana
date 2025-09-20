package net.rasanovum.viaromana.client.gui.elements;

import net.rasanovum.viaromana.path.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Manages a group of MapIconButtons to ensure only one can be selected at a time. Arranges icons in a grid layout.
 */
public class MapIconButtonGroup {
    private final List<MapIconButton> buttons = new ArrayList<>();
    private MapIconButton selectedButton = null;
    private final Consumer<Node.Icon> onSelectionChange;

    public MapIconButtonGroup(Consumer<Node.Icon> onSelectionChange) {
        this.onSelectionChange = onSelectionChange;
    }

    /**
     * Create and add all icon buttons in a grid layout
     * @param font The font to use for rendering
     * @param startX The starting X position for the grid
     * @param startY The starting Y position for the grid
     * @param columns Number of columns in the grid
     * @param spacing Spacing between buttons
     * @return List of created buttons to add to the screen
     */
    public List<MapIconButton> createIconButtons(net.minecraft.client.gui.Font font, int startX, int startY, int columns, int spacing) {
        Node.Icon[] icons = Node.Icon.values();
        
        for (int i = 0; i < icons.length; i++) {
            int row = i / columns;
            int col = i % columns;
            
            int x = startX + col * (MapIconButton.CIRCLE_SIZE + spacing);
            int y = startY + row * (MapIconButton.CIRCLE_SIZE + spacing);
            
            MapIconButton button = new MapIconButton(font, x, y, icons[i], this::selectByIcon);
            this.buttons.add(button);
        }
        
        return new ArrayList<>(this.buttons);
    }

    public void addButton(MapIconButton button) {
        this.buttons.add(button);
    }

    public void selectButton(MapIconButton button) {
        if (!this.buttons.contains(button)) {
            return;
        }

        for (MapIconButton btn : this.buttons) {
            btn.setSelected(false);
        }

        button.setSelected(true);
        this.selectedButton = button;

        if (this.onSelectionChange != null) {
            this.onSelectionChange.accept(button.getIcon());
        }
    }

    public void selectByIcon(Node.Icon icon) {
        for (MapIconButton button : this.buttons) {
            if (button.getIcon() == icon) {
                selectButton(button);
                break;
            }
        }
    }

    public Node.Icon getSelectedIcon() {
        return this.selectedButton != null ? this.selectedButton.getIcon() : null;
    }

    public MapIconButton getSelectedButton() {
        return this.selectedButton;
    }
}
