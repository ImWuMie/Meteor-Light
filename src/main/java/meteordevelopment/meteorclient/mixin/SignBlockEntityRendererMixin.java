/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import net.minecraft.block.*;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.SignBlockEntityRenderer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.SignType;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

@Mixin(SignBlockEntityRenderer.class)
public abstract class SignBlockEntityRendererMixin {
    @Shadow
    @Final
    private TextRenderer textRenderer;

    @Shadow
    @Final
    private static int RENDER_DISTANCE;

    @Shadow
    @Final
    private Map<SignType, SignBlockEntityRenderer.SignModel> typeToModel;

    private static boolean shouldRender(SignBlockEntity sign, int signColor) {
        if (signColor == DyeColor.BLACK.getSignColor()) {
            return true;
        } else {
            MinecraftClient minecraftClient = MinecraftClient.getInstance();
            ClientPlayerEntity clientPlayerEntity = minecraftClient.player;
            if (clientPlayerEntity != null && minecraftClient.options.getPerspective().isFirstPerson() && clientPlayerEntity.isUsingSpyglass()) {
                return true;
            } else {
                Entity entity = minecraftClient.getCameraEntity();
                return entity != null && entity.squaredDistanceTo(Vec3d.ofCenter(sign.getPos())) < (double)RENDER_DISTANCE;
            }
        }
    }

    private static SignType getSignType(Block block) {
        SignType signType;
        if (block instanceof AbstractSignBlock) {
            signType = ((AbstractSignBlock)block).getSignType();
        } else {
            signType = SignType.OAK;
        }

        return signType;
    }

    /**
     * @author wumie
     * @reason none
     */
    @Overwrite
    public void render(SignBlockEntity signBlockEntity, float f, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, int j) {
        BlockState blockState = signBlockEntity.getCachedState();
        matrixStack.push();
        float g = 0.6666667F;
        SignType signType = getSignType(blockState.getBlock());
        SignBlockEntityRenderer.SignModel signModel = this.typeToModel.get(signType);
        float h;
        if (blockState.getBlock() instanceof SignBlock) {
            matrixStack.translate(0.5D, 0.5D, 0.5D);
            h = -((float)((Integer)blockState.get(SignBlock.ROTATION) * 360) / 16.0F);
            matrixStack.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(h));
            signModel.stick.visible = true;
        } else {
            matrixStack.translate(0.5D, 0.5D, 0.5D);
            h = -((Direction)blockState.get(WallSignBlock.FACING)).asRotation();
            matrixStack.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(h));
            matrixStack.translate(0.0D, -0.3125D, -0.4375D);
            signModel.stick.visible = false;
        }

        matrixStack.push();
        matrixStack.scale(0.6666667F, -0.6666667F, -0.6666667F);
        SpriteIdentifier spriteIdentifier = TexturedRenderLayers.getSignTextureId(signType);
        Objects.requireNonNull(signModel);
        VertexConsumer vertexConsumer = spriteIdentifier.getVertexConsumer(vertexConsumerProvider, signModel::getLayer);
        signModel.root.render(matrixStack, vertexConsumer, i, j);
        matrixStack.pop();
        float k = 0.010416667F;
        matrixStack.translate(0.0D, 0.3333333432674408D, 0.046666666865348816D);
        matrixStack.scale(0.010416667F, -0.010416667F, 0.010416667F);
        int l = getColor(signBlockEntity);
        OrderedText[] orderedTexts = signBlockEntity.updateSign(MinecraftClient.getInstance().shouldFilterText(), (text) -> {
            List<OrderedText> list = this.textRenderer.wrapLines(text, 90);
            return list.isEmpty() ? OrderedText.EMPTY : (OrderedText)list.get(0);
        });
        int n;
        boolean bl;
        int o;
        if (signBlockEntity.isGlowingText() && !Modules.get().get(NoRender.class).noSignTextColor()) {
            n = signBlockEntity.getTextColor().getSignColor();
            bl = shouldRender(signBlockEntity, n);
            o = 15728880;
        } else {
            n = l;
            bl = false;
            o = i;
        }

        for(int p = 0; p < 4; ++p) {
            OrderedText orderedText = orderedTexts[p];
            float q = (float)(-this.textRenderer.getWidth(orderedText) / 2);
            if (bl && !Modules.get().get(NoRender.class).noSignTextColor()) {
                this.textRenderer.drawWithOutline(orderedText, q, (float)(p * 10 - 20), n, l, matrixStack.peek().getPositionMatrix(), vertexConsumerProvider, o);
            } else {
                this.textRenderer.draw(orderedText, q, (float)(p * 10 - 20), n, false, matrixStack.peek().getPositionMatrix(), vertexConsumerProvider, false, 0, o);
            }
        }

        matrixStack.pop();
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/entity/SignBlockEntity;updateSign(ZLjava/util/function/Function;)[Lnet/minecraft/text/OrderedText;"))
    private OrderedText[] updateSignProxy(SignBlockEntity sign, boolean filterText, Function<Text, OrderedText> textOrderingFunction) {
        if (Modules.get().get(NoRender.class).noSignText()) return null;
        return sign.updateSign(filterText, textOrderingFunction);
    }

    @ModifyConstant(method = "render", constant = @Constant(intValue = 4))
    private int loopTextLengthProxy(int i) {
        if (Modules.get().get(NoRender.class).noSignText()) return 0;
        return i;
    }

    /**
     * @author wumie
     * @reason none
     */
    @Overwrite
    private static int getColor(SignBlockEntity sign) {
        int i = sign.getTextColor().getSignColor();
        double d = 0.4D;
        int j = (int)((double) NativeImage.getRed(i) * 0.4D);
        int k = (int)((double)NativeImage.getGreen(i) * 0.4D);
        int l = (int)((double)NativeImage.getBlue(i) * 0.4D);
        if (Modules.get().get(NoRender.class).noSignTextColor()) {
            return DyeColor.BLACK.getSignColor();
        } else {
            return i == DyeColor.BLACK.getSignColor() && sign.isGlowingText() ? -988212 : NativeImage.packColor(0, l, k, j);
        }
    }

}
