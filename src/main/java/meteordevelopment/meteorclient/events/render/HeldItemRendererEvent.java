/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.events.render;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3f;

public class HeldItemRendererEvent {
    private static final HeldItemRendererEvent INSTANCE = new HeldItemRendererEvent();

    public Hand hand;
    public MatrixStack matrix;
    public Arm arm;
    public float tickDelta;
    public float prevEquipProgressMainHand;
    public float equipProgressMainHand;
    public float swingProgress;

    public static HeldItemRendererEvent get(Hand hand, MatrixStack matrices, Arm arm, float tickDelta, float prevEquipProgressMainHand, float equipProgressMainHand,float swingProgress) {
        INSTANCE.hand = hand;
        INSTANCE.matrix = matrices;
        INSTANCE.arm = arm;
        INSTANCE.tickDelta = tickDelta;
        INSTANCE.prevEquipProgressMainHand = prevEquipProgressMainHand;
        INSTANCE.equipProgressMainHand = equipProgressMainHand;
        INSTANCE.swingProgress = swingProgress;
        return INSTANCE;
    }

    public void applyEquipOffset(MatrixStack matrices, Arm arm, float equipProgress) {
        int i = arm == Arm.RIGHT ? 1 : -1;
        matrices.translate((double)((float)i * 0.56F), (double)(-0.52F + equipProgress * -0.6F), -0.7200000286102295D);
    }

    public void applySwingOffset(MatrixStack matrices, Arm arm, float swingProgress) {
        int i = arm == Arm.RIGHT ? 1 : -1;
        float f = MathHelper.sin(swingProgress * swingProgress * 3.1415927F);
        matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion((float)i * (45.0F + f * -20.0F)));
        float g = MathHelper.sin(MathHelper.sqrt(swingProgress) * 3.1415927F);
        matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion((float)i * g * -20.0F));
        matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(g * -80.0F));
        matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion((float)i * -45.0F));
    }

    public void transformFirstPersonItem(MatrixStack matrices, Arm arm,float equipProgress,float swingProgress) {
        applyEquipOffset(matrices,arm,equipProgress);
        applySwingOffset(matrices,arm,swingProgress);
    }
}
