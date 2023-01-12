/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.player.OldHitting;
import meteordevelopment.meteorclient.systems.modules.render.Chams;
import meteordevelopment.meteorclient.systems.modules.render.HandView;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.UseAction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(PlayerEntityRenderer.class)
public class PlayerEntityRendererMixin {
    @Inject(method = "setModelPose", at = @At(value = "HEAD"))
    private void render(AbstractClientPlayerEntity player, CallbackInfo ci) {
    }

    @ModifyArgs(method = "renderArm", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelPart;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;II)V", ordinal = 0))
    private void modifyRenderLayer(Args args, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, AbstractClientPlayerEntity player, ModelPart arm, ModelPart sleeve) {
        Chams chams = Modules.get().get(Chams.class);

        if (chams.isActive() && chams.hand.get()) {
            Identifier texture = chams.handTexture.get() ? player.getSkinTexture() : Chams.BLANK;
            args.set(1, vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(texture)));
        }
    }

    @Redirect(method = "renderArm", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelPart;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;II)V", ordinal = 0))
    private void redirectRenderMain(ModelPart modelPart, MatrixStack matrices, VertexConsumer vertices, int light, int overlay) {
        Chams chams = Modules.get().get(Chams.class);

        if (chams.isActive() && chams.hand.get()) {
            Color color = chams.handColor.get();
            modelPart.render(matrices, vertices, light, overlay, color.r / 255f, color.g / 255f, color.b / 255f, color.a / 255f);
        } else {
            modelPart.render(matrices, vertices, light, overlay);
        }
    }

    @Redirect(method = "renderArm", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelPart;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;II)V", ordinal = 1))
    private void redirectRenderSleeve(ModelPart modelPart, MatrixStack matrices, VertexConsumer vertices, int light, int overlay) {
        Chams chams = Modules.get().get(Chams.class);

        if (Modules.get().isActive(HandView.class)) return;

        if (chams.isActive() && chams.hand.get()) {
            Color color = chams.handColor.get();
            modelPart.render(matrices, vertices, light, overlay, color.r / 255f, color.g / 255f, color.b / 255f, color.a / 255f);
        } else {
            modelPart.render(matrices, vertices, light, overlay);
        }
    }

    /**
     * @author wumie
     * @reason {@link meteordevelopment.meteorclient.systems.modules.player.OldHitting} old hitting
     */
    @Overwrite
    public static BipedEntityModel.ArmPose getArmPose(AbstractClientPlayerEntity player, Hand hand) {
        ItemStack itemStack = player.getStackInHand(hand);
        OldHitting oldHitting = Modules.get().get(OldHitting.class);
        if (itemStack.isEmpty()) {
            return BipedEntityModel.ArmPose.EMPTY;
        } else {
            if (player.getActiveHand() == hand && player.getItemUseTimeLeft() > 0) {
                UseAction useAction = itemStack.getUseAction();
                if (useAction == UseAction.BLOCK) {
                    return BipedEntityModel.ArmPose.BLOCK;
                }

                if (useAction == UseAction.BOW) {
                    return BipedEntityModel.ArmPose.BOW_AND_ARROW;
                }

                if (useAction == UseAction.SPEAR) {
                    return BipedEntityModel.ArmPose.THROW_SPEAR;
                }

                if (useAction == UseAction.CROSSBOW && hand == player.getActiveHand()) {
                    return BipedEntityModel.ArmPose.CROSSBOW_CHARGE;
                }

                if (useAction == UseAction.SPYGLASS) {
                    return BipedEntityModel.ArmPose.SPYGLASS;
                }

                if (useAction == UseAction.TOOT_HORN) {
                    return BipedEntityModel.ArmPose.TOOT_HORN;
                }
            } else if (!player.handSwinging && itemStack.isOf(Items.CROSSBOW) && CrossbowItem.isCharged(itemStack)) {
                return BipedEntityModel.ArmPose.CROSSBOW_HOLD;
            } else if (oldHitting.isOldHit(itemStack.getItem(), player)) {
                return BipedEntityModel.ArmPose.BLOCK;
            }

            return (oldHitting.isOldHit(itemStack.getItem(), player)) ? BipedEntityModel.ArmPose.BLOCK : BipedEntityModel.ArmPose.ITEM;
        }
    }
}
