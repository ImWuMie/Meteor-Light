/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.entity.BoatMoveEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.VehicleMoveS2CPacket;
import net.minecraft.util.math.Vec3d;

public class BoatFly extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
            .name("speed")
            .description("Horizontal speed in blocks per second.")
            .defaultValue(10)
            .min(0)
            .sliderMax(50)
            .build()
    );

    private final Setting<Double> verticalSpeed = sgGeneral.add(new DoubleSetting.Builder()
            .name("vertical-speed")
            .description("Vertical speed in blocks per second.")
            .defaultValue(6)
            .min(0)
            .sliderMax(20)
            .build()
    );

    private final Setting<Double> fallSpeed = sgGeneral.add(new DoubleSetting.Builder()
            .name("fall-speed")
            .description("How fast you fall in blocks per second.")
            .defaultValue(0.1)
            .min(0)
            .build()
    );

    private final Setting<Boolean> cancelServerPackets = sgGeneral.add(new BoolSetting.Builder()
            .name("cancel-server-packets")
            .description("Cancels incoming boat move packets.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> phase = sgGeneral.add(new BoolSetting.Builder()
        .name("phase")
        .description("boat noclip")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> vphase = sgGeneral.add(new BoolSetting.Builder()
        .name("VPhase")
        .description("boat Y noclip")
        .defaultValue(false)
        .build()
    );

    public BoatFly() {
        super(Categories.Movement, "boat-fly", "Transforms your boat into a plane.");
    }

    Vec3d startPos = null;
    float fspeed;

    @Override
    public void onActivate() {
        startPos = mc.player.getPos();
        fspeed = mc.player.getAbilities().getFlySpeed();
        if (phase.get() && vphase.get()) {
            mc.player.getAbilities().setFlySpeed(0);
        }
    }

    @Override
    public void onDeactivate() {
        if (phase.get() && vphase.get()) {
        mc.player.noClip = false;
        mc.player.getAbilities().flying = false;
        mc.player.getAbilities().setFlySpeed(fspeed);
        Vec3d ppos = mc.player.getPos();
        mc.player.updatePosition(ppos.x, ppos.y, ppos.z); // teleport after we stopped blocking movement packets
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre e) {
        Entity vehicle = mc.player.getVehicle();
        if (vehicle == null) return;
        if (startPos == null) return;
        if (phase.get()) {
            if (vphase.get()) {
                Vec3d ppos = mc.player.getPos();
                if (mc.options.jumpKey.isPressed()) {
                    mc.player.updatePosition(startPos.x, ppos.y + 0.05, startPos.z);
                }
                if (mc.options.sneakKey.isPressed()) {
                    mc.player.updatePosition(startPos.x, ppos.y - 0.05, startPos.z);
                }
            }
        }
    }

    @EventHandler
    private void onWorldRender(Render3DEvent e) {
        Entity vehicle = mc.player.getVehicle();
        if (vehicle == null) return;
        if (phase.get()) {
            mc.player.getVehicle().noClip = true;
            mc.player.getVehicle().setNoGravity(true);
            mc.player.noClip = true;

            if (vphase.get()) {
                mc.player.noClip = true;
                mc.player.getAbilities().flying = true;
                Vec3d p = mc.player.getPos();
                mc.player.updatePosition(startPos.x, p.y, startPos.z);
                mc.player.setVelocity(0, 0, 0);
                mc.player.setSwimming(false);
                mc.player.setPose(EntityPose.STANDING);
            }
        }
    }

    @EventHandler
    private void onBoatMove(BoatMoveEvent event) {
        if (event.boat.getPrimaryPassenger() != mc.player) return;

        event.boat.setYaw(mc.player.getYaw());

        // Horizontal movement
        Vec3d vel = PlayerUtils.getHorizontalVelocity(speed.get());
        double velX = vel.getX();
        double velY = 0;
        double velZ = vel.getZ();

        // Vertical movement
        if (mc.options.jumpKey.isPressed()) velY += verticalSpeed.get() / 20;
        if (mc.options.sprintKey.isPressed()) velY -= verticalSpeed.get() / 20;
        else velY -= fallSpeed.get() / 20;

        // Apply velocity
        ((IVec3d) event.boat.getVelocity()).set(velX, velY, velZ);
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (event.packet instanceof VehicleMoveS2CPacket && cancelServerPackets.get()) {
            event.cancel();
        }
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (vphase.get() && phase.get()) {
            if (event.packet instanceof PlayerMoveC2SPacket) {
                event.cancel();
            }
        }
    }
}
