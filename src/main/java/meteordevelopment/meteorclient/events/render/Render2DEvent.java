/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.events.render;

import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.client.util.math.MatrixStack;

public class Render2DEvent {
    private static final Render2DEvent INSTANCE = new Render2DEvent();

    public int screenWidth, screenHeight;
    public double frameTime;
    public float tickDelta;
    public MatrixStack matrices;

    public static Render2DEvent get(MatrixStack matrices, int screenWidth, int screenHeight, float tickDelta) {
        INSTANCE.matrices = matrices;
        INSTANCE.screenWidth = screenWidth;
        INSTANCE.screenHeight = screenHeight;
        INSTANCE.frameTime = Utils.frameTime;
        INSTANCE.tickDelta = tickDelta;
        return INSTANCE;
    }
}
