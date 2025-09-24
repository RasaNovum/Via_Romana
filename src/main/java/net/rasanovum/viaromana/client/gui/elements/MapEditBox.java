package net.rasanovum.viaromana.client.gui.elements;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.rasanovum.viaromana.client.gui.GuiConstants;

/**
 * Custom EditBox for the LinkSignScreen that removes drop shadows, adds a bottom border to imitate a signage field on a form.
 */
public class MapEditBox extends EditBox {
    private final Font font;
    private int frame = 0;

    public MapEditBox(Font font, int x, int y, int width, int height, Component message) {
        super(font, x, y, width, height, message);
        this.font = font;
        this.setBordered(false);
    }
    
    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.frame++;
        if (!this.isVisible()) {
            return;
        }

        Font font = this.font != null ? this.font : net.minecraft.client.Minecraft.getInstance().font;
        if (font == null) {
            return;
        }

        String value = this.getValue();
        int textX = this.getX() + 4;
        int textY = this.getY() + (this.height - 8) / 2;
        int textColor = this.isFocused() ? GuiConstants.TEXT_COLOR_PRIMARY : GuiConstants.TEXT_COLOR_SECONDARY; // Custom colors
        int cursorPos = this.getCursorPosition();
        String selectedText = this.getHighlighted();
        boolean hasSelection = !selectedText.isEmpty();
        
        int maxVisibleWidth = this.getInnerWidth();
        String visibleText = value;
        int scrollOffset = 0;
        
        int textWidth = font.width(value);
        if (textWidth > maxVisibleWidth) {
            String textToCursor = value.substring(0, Math.min(cursorPos, value.length()));
            int cursorX = font.width(textToCursor);
            
            if (cursorX > maxVisibleWidth) {
                scrollOffset = Math.max(0, cursorPos - (maxVisibleWidth / font.width("W")));
                scrollOffset = Math.min(scrollOffset, value.length());
                visibleText = value.substring(scrollOffset);
                visibleText = font.plainSubstrByWidth(visibleText, maxVisibleWidth);
            } else {
                visibleText = font.plainSubstrByWidth(value, maxVisibleWidth);
            }
        }
        
        if (hasSelection && this.isFocused()) {
            int selectionStart = Math.max(0, textX);
            int selectionWidth = Math.min(font.width(selectedText), maxVisibleWidth);
            int highlightY = textY - 1;
            
            guiGraphics.fill(selectionStart, highlightY, selectionStart + selectionWidth, highlightY + 10, 0x800055FF);
        }
        
        guiGraphics.drawString(font, visibleText, textX, textY, textColor, false);
        
        if (this.isFocused() && this.frame / 6 % 2 == 0 && !hasSelection) {
            int relativeCursorPos = cursorPos - scrollOffset;
            if (relativeCursorPos >= 0 && relativeCursorPos <= visibleText.length()) {
                int cursorX = textX;
                if (relativeCursorPos < visibleText.length()) {
                    String textBeforeCursor = visibleText.substring(0, relativeCursorPos);
                    cursorX = textX + font.width(textBeforeCursor);
                } else if (relativeCursorPos == visibleText.length()) {
                    cursorX = textX + font.width(visibleText);
                }
                
                if (cursorPos < value.length()) {
                    guiGraphics.fill(RenderType.guiOverlay(), cursorX, textY - 1, cursorX + 1, textY + 9, 0xFF000000 | GuiConstants.TEXT_COLOR_PRIMARY);
                } else {
                    guiGraphics.drawString(font, "_", cursorX, textY, textColor, false);
                }
            }
        }
        
        guiGraphics.fill(this.getX(), this.getY() + this.height - 1, this.getX() + this.width, this.getY() + this.height, 0xFF000000 | GuiConstants.BORDER_COLOR_PRIMARY);
    }
}
