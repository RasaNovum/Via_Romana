package net.rasanovum.viaromana.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rasanovum.viaromana.util.VersionUtils;

/**
 * Screen that opens when right-clicking a warp block, made for modpack developers
 */
@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
public class WarpBlockScreen extends Screen {
    private static final ResourceLocation WARP_BLOCK_TEXTURE = VersionUtils.getLocation("via_romana:textures/screens/warp_block_texture.png");
    private final BlockPos blockPos;

    public WarpBlockScreen(BlockPos blockPos) {
        super(Component.translatable("gui.viaromana.warp_block.title"));
        this.blockPos = blockPos;
    }

    public BlockPos getBlockPos() {
        return this.blockPos;
    }

    @Override
    protected void init() {
        super.init();
        
        Button doneButton = Button.builder(Component.literal("Done"), (button) -> {
            this.onClose();
        })
        .bounds(this.width / 2 - 100, this.height / 4 + 120 + 24, 200, 20)
        .build();
        
        this.addRenderableWidget(doneButton);
    }

    //? if >1.21
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Skip 1.21 background rendering
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        Component title = this.getTitle();
        int titleWidth = this.font.width(title);
        guiGraphics.drawString(this.font, title, (this.width - titleWidth) / 2, 20, 0xFFFFFF);

        int textureSize = 128;
        
        int drawX = (this.width - textureSize) / 2;
        int drawY = (this.height - textureSize) / 2 - 18;

        //? if >1.21
        this.renderTransparentBackground(guiGraphics);

        guiGraphics.blit(WARP_BLOCK_TEXTURE, drawX, drawY, 0, 0, textureSize, textureSize, 128, 128);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
