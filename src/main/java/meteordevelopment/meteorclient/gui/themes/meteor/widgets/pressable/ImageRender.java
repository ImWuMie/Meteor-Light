/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.themes.meteor.widgets.pressable;

import meteordevelopment.meteorclient.gui.themes.meteor.widgets.RenderImage;
import meteordevelopment.meteorclient.utils.misc.MeteorIdentifier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;

public class ImageRender extends RenderImage {
    public ImageRender(int x, int y, int imageWidth, int imageHeight, MeteorIdentifier image,boolean blur) {
        super(x, y, imageWidth, imageHeight, image,blur);
    }

    @Override
    public void appendNarrations(NarrationMessageBuilder builder) {

    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double deltaX, double deltaY) {
        super.onDrag(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    protected void onFocusedChanged(boolean newFocused) {
        super.onFocusedChanged(newFocused);
    }

    @Override
    public void onRelease(double mouseX, double mouseY) {
        super.onRelease(mouseX, mouseY);
    }


    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            return false;
    }

    public void onClick(double mouseX, double mouseY) {
        super.onClick(mouseX,mouseY);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return super.charTyped(chr, modifiers);
    }
}
