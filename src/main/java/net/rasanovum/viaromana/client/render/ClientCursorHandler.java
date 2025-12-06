package net.rasanovum.viaromana.client.render;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.client.gui.ChartingScreen;
import net.rasanovum.viaromana.client.gui.LinkSignScreen;
import net.rasanovum.viaromana.client.gui.TeleportMapScreen;
import net.rasanovum.viaromana.util.VersionUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Optional;

public class ClientCursorHandler {

    private static long customCursorHandle = 0;
    private static boolean isCustomCursorActive = false;
    private static boolean hasInitialized = false;

    private static final double CURSOR_SCALE = 2;
    private static final ResourceLocation CURSOR = VersionUtils.getLocation("via_romana:textures/screens/cursor.png");

    private static void loadCursor(Minecraft mc) {
        if (customCursorHandle != 0 || hasInitialized) return;
        hasInitialized = true;

        try {
            Optional<Resource> resource = mc.getResourceManager().getResource(CURSOR);
            if (resource.isPresent()) {
                try (InputStream stream = resource.get().open();
                     NativeImage image = NativeImage.read(stream);
                     MemoryStack stack = MemoryStack.stackPush()) {

                    int newWidth = (int) (image.getWidth() * CURSOR_SCALE);
                    int newHeight = (int) (image.getHeight() * CURSOR_SCALE);

                    ByteBuffer textureBuffer = MemoryUtil.memAlloc(newWidth * newHeight * 4);

                    for (int y = 0; y < newHeight; y++) {
                        for (int x = 0; x < newWidth; x++) {
                            int srcX = Math.min((int) (x / CURSOR_SCALE), image.getWidth() - 1);
                            int srcY = Math.min((int) (y / CURSOR_SCALE), image.getHeight() - 1);
                            textureBuffer.putInt(image.getPixelRGBA(srcX, srcY));
                        }
                    }

                    textureBuffer.flip();

                    GLFWImage cursorImage = GLFWImage.malloc(stack);
                    cursorImage.width(newWidth);
                    cursorImage.height(newHeight);
                    cursorImage.pixels(textureBuffer);

                    customCursorHandle = GLFW.glfwCreateCursor(cursorImage, 0, 0);

                    MemoryUtil.memFree(textureBuffer);
                }
            }
        } catch (IOException e) {
            ViaRomana.LOGGER.warn("Custom Cursor allocation failed: {}", e.getMessage());
        }
    }

    public static void onClientTick() {
        Minecraft mc = Minecraft.getInstance();

        if (!hasInitialized) {
            loadCursor(mc);
            if (customCursorHandle == 0) return;
        }

        long windowHandle = mc.getWindow().getWindow();

        boolean shouldBeCustom = (mc.screen instanceof TeleportMapScreen || mc.screen instanceof LinkSignScreen || mc.screen instanceof ChartingScreen);

        if (shouldBeCustom && !isCustomCursorActive) {
            GLFW.glfwSetCursor(windowHandle, customCursorHandle);
            isCustomCursorActive = true;
        }
        else if (!shouldBeCustom && isCustomCursorActive) {
            GLFW.glfwSetCursor(windowHandle, 0);
            isCustomCursorActive = false;
        }
    }

    public static void destroy() {
        if (customCursorHandle != 0) {
            GLFW.glfwDestroyCursor(customCursorHandle);
            customCursorHandle = 0;
        }
    }
}