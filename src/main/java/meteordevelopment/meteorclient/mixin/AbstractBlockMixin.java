/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.AmbientOcclusionEvent;
import meteordevelopment.meteorclient.events.world.CollisionShapeEvent;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.NoBlockTrace;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractBlock.class)
public class AbstractBlockMixin {
    @Inject(method = "getAmbientOcclusionLightLevel", at = @At("HEAD"), cancellable = true)
    private void onGetAmbientOcclusionLightLevel(BlockState state, BlockView world, BlockPos pos, CallbackInfoReturnable<Float> info) {
        AmbientOcclusionEvent event = MeteorClient.EVENT_BUS.post(AmbientOcclusionEvent.get());

        if (event.lightLevel != -1) info.setReturnValue(event.lightLevel);
    }

    @Inject(method = "getOutlineShape", at = @At("HEAD"), cancellable = true)
    private void onGetOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context, CallbackInfoReturnable<VoxelShape> info) {
        if (state != null && Modules.get() != null && Modules.get().isActive(NoBlockTrace.class) && Modules.get().get(NoBlockTrace.class).shouldCancelOutline(state.getBlock())) {
            info.setReturnValue(VoxelShapes.empty());
        }
    }

    @Inject(method = "getCollisionShape", at = @At("HEAD"), cancellable = true)
    private void onGetCollisionShape(BlockState state, BlockView view, BlockPos pos, ShapeContext context, CallbackInfoReturnable<VoxelShape> info) {
        if (!(state.getFluidState().isEmpty())) {
            CollisionShapeEvent event = MeteorClient.EVENT_BUS.post(CollisionShapeEvent.get(state.getFluidState().getBlockState(), pos, CollisionShapeEvent.CollisionType.FLUID));

            if (event.shape != null) info.setReturnValue(event.shape);
        } else {
            CollisionShapeEvent event = MeteorClient.EVENT_BUS.post(CollisionShapeEvent.get(state, pos, CollisionShapeEvent.CollisionType.BLOCK));

            if (event.shape != null) info.setReturnValue(event.shape);
        }
    }
}
