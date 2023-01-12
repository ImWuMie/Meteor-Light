/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.jellogui;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

public abstract class GuiFront extends WidgetScreen {
    public final RenderUtils gameRender = RenderUtils.instance;
    public final MinecraftClient mc = MinecraftClient.getInstance();
    public Text defaultTitle = Text.of("METEOR FOR JELLO");

    public GuiFront(GuiTheme theme,String title) {
        super(theme,title);
    }
}
