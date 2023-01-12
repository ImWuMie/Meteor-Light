/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2022 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

public class Phase extends Module {
    public int delay = 0;
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> mode = sgGeneral.add(new BoolSetting.Builder()
        .name("mod")
        .description("t = vanilla  f = ground")
        .defaultValue(false)
        .build()
    );

    public Phase() {
        super(Categories.Movement,"Phase","go through walls like a ghost");
    }

    @EventHandler
    public void onTick(TickEvent.Pre e) {
        assert mc.player != null;
        assert mc.world != null;

        if (!mode.get() && delay == 0) {
            if (mc.options.sneakKey.isPressed() && mc.player.verticalCollision) {
                for (int i = mc.player.getBlockY() - 1; i > 0; i--) {
                    BlockState bs1 = mc.world.getBlockState(mc.player.getBlockPos().subtract(new Vec3i(0, mc.player.getBlockY() - i, 0)));
                    BlockState bs2 = mc.world.getBlockState(mc.player.getBlockPos().subtract(new Vec3i(0, mc.player.getBlockY() - i - 1, 0)));
                    if (!bs1.getMaterial().blocksMovement() && !bs2.getMaterial().blocksMovement() && bs1.getBlock() != Blocks.LAVA && bs2.getBlock() != Blocks.LAVA) {
                        mc.player.updatePosition(mc.player.getX(), i, mc.player.getZ());
                        break;
                    }
                }
                delay = 20;
            }
        }

        if (mode.get() && mc.player.horizontalCollision && mc.options.sneakKey.isPressed()) {
            Vec3i v31 = mc.player.getMovementDirection().getVector();
            Vec3d v3 = new Vec3d(v31.getX(), 0, v31.getZ());
            for (double o = 2; o < 100; o++) {
                Vec3d coff = v3.multiply(o);
                BlockPos cpos = mc.player.getBlockPos().add(new Vec3i(coff.x, coff.y, coff.z));
                BlockState bs1 = mc.world.getBlockState(cpos);
                BlockState bs2 = mc.world.getBlockState(cpos.up());
                if (!bs1.getMaterial().blocksMovement() && !bs2.getMaterial().blocksMovement() && bs1.getBlock() != Blocks.LAVA && bs2.getBlock() != Blocks.LAVA) {
                    mc.player.updatePosition(cpos.getX() + 0.5, cpos.getY(), cpos.getZ() + 0.5);
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), false));
                    break;
                }
            }
        }

        if (delay > 0) {
            delay--;
        }
    }

    enum Mode {
        Vanilla,
        Ground
    }
}
