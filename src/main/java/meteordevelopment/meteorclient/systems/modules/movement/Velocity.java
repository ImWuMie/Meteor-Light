/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.entity.player.InteractBlockEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.EntityVelocityUpdateS2CPacketAccessor;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.util.math.Vec3d;

public class Velocity extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("Mode")
        .description("Velocity Mode")
        .defaultValue(Mode.Vanilla)
        .build()
    );

    public final Setting<Boolean> knockback = sgGeneral.add(new BoolSetting.Builder()
        .name("knockback")
        .description("Modifies the amount of knockback you take from attacks.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Double> knockbackHorizontal = sgGeneral.add(new DoubleSetting.Builder()
        .name("knockback-horizontal")
        .description("How much horizontal knockback you will take.")
        .defaultValue(0)
        .sliderMax(1)
        .visible(knockback::get)
        .build()
    );

    public final Setting<Double> knockbackVertical = sgGeneral.add(new DoubleSetting.Builder()
        .name("knockback-vertical")
        .description("How much vertical knockback you will take.")
        .defaultValue(0)
        .sliderMax(1)
        .visible(knockback::get)
        .build()
    );

    public final Setting<Boolean> explosions = sgGeneral.add(new BoolSetting.Builder()
        .name("explosions")
        .description("Modifies your knockback from explosions.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Double> explosionsHorizontal = sgGeneral.add(new DoubleSetting.Builder()
        .name("explosions-horizontal")
        .description("How much velocity you will take from explosions horizontally.")
        .defaultValue(0)
        .sliderMax(1)
        .visible(explosions::get)
        .build()
    );

    public final Setting<Double> explosionsVertical = sgGeneral.add(new DoubleSetting.Builder()
        .name("explosions-vertical")
        .description("How much velocity you will take from explosions vertically.")
        .defaultValue(0)
        .sliderMax(1)
        .visible(explosions::get)
        .build()
    );

    public final Setting<Boolean> liquids = sgGeneral.add(new BoolSetting.Builder()
        .name("liquids")
        .description("Modifies the amount you are pushed by flowing liquids.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Double> liquidsHorizontal = sgGeneral.add(new DoubleSetting.Builder()
        .name("liquids-horizontal")
        .description("How much velocity you will take from liquids horizontally.")
        .defaultValue(0)
        .sliderMax(1)
        .visible(liquids::get)
        .build()
    );

    public final Setting<Double> liquidsVertical = sgGeneral.add(new DoubleSetting.Builder()
        .name("liquids-vertical")
        .description("How much velocity you will take from liquids vertically.")
        .defaultValue(0)
        .sliderMax(1)
        .visible(liquids::get)
        .build()
    );

    public final Setting<Boolean> entityPush = sgGeneral.add(new BoolSetting.Builder()
        .name("entity-push")
        .description("Modifies the amount you are pushed by entities.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Double> entityPushAmount = sgGeneral.add(new DoubleSetting.Builder()
        .name("entity-push-amount")
        .description("How much you will be pushed.")
        .defaultValue(0)
        .sliderMax(1)
        .visible(entityPush::get)
        .build()
    );

    public final Setting<Boolean> blocks = sgGeneral.add(new BoolSetting.Builder()
        .name("blocks")
        .description("Prevents you from being pushed out of blocks.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Boolean> sinking = sgGeneral.add(new BoolSetting.Builder()
        .name("sinking")
        .description("Prevents you from sinking in liquids.")
        .defaultValue(false)
        .build()
    );

    private float yaw = 0;
    private float pitch = 0;
    private Vec3d position = Vec3d.ZERO;
    private boolean update = false;

    @Override
    public void onActivate() {
        if (mc.player != null){
            yaw = mc.player.getYaw();
            pitch = mc.player.getPitch();
            position = mc.player.getPos();
        }
        super.onActivate();
    }

    public Velocity() {
        super(Categories.Movement, "velocity", "Prevents you from being moved by external forces.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (position == Vec3d.ZERO && yaw == 0 && pitch == 0) {
            if (mc.player != null){
                yaw = mc.player.getYaw();
                pitch = mc.player.getPitch();
                position = mc.player.getPos();
            }
        }
        if (mc.player != null && mode.get().equals(Mode.BypassTest) && update) {
            mc.player.setVelocity(0, 0, 0);
            mc.player.setPos(position.x, position.y, position.z);
        }
        if (!sinking.get()) return;
        if (mc.options.jumpKey.isPressed() || mc.options.sneakKey.isPressed()) return;

        if ((mc.player.isTouchingWater() || mc.player.isInLava()) && mc.player.getVelocity().y < 0) {
            ((IVec3d) mc.player.getVelocity()).setY(0);
        }
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (knockback.get() && event.packet instanceof EntityVelocityUpdateS2CPacket packet
            && ((EntityVelocityUpdateS2CPacket) event.packet).getId() == mc.player.getId()) {
            if (mode.get().equals(Mode.Vanilla)) {
                double velX = (packet.getVelocityX() / 8000d - mc.player.getVelocity().x) * knockbackHorizontal.get();
                double velY = (packet.getVelocityY() / 8000d - mc.player.getVelocity().y) * knockbackVertical.get();
                double velZ = (packet.getVelocityZ() / 8000d - mc.player.getVelocity().z) * knockbackHorizontal.get();
                ((EntityVelocityUpdateS2CPacketAccessor) packet).setX((int) (velX * 8000 + mc.player.getVelocity().x * 8000));
                ((EntityVelocityUpdateS2CPacketAccessor) packet).setY((int) (velY * 8000 + mc.player.getVelocity().y * 8000));
                ((EntityVelocityUpdateS2CPacketAccessor) packet).setZ((int) (velZ * 8000 + mc.player.getVelocity().z * 8000));
            }
            switch (mode.get()) {
                case Matrix -> {
                    double velX = (packet.getVelocityX() / 8000d - mc.player.getVelocity().x);
                    double velZ = (packet.getVelocityZ() / 8000d - mc.player.getVelocity().z);
                    if (mc.player.isOnGround()) {
                        ((EntityVelocityUpdateS2CPacketAccessor) packet).setZ((int) ((int) velX * 0.9));
                        ((EntityVelocityUpdateS2CPacketAccessor) packet).setZ((int) ((int) velZ * 0.9));
                    }
                }
            }
        }

        if (event.packet instanceof EntityVelocityUpdateS2CPacket p) {
            switch (mode.get()) {
                case Cancel -> {
                    event.setCancelled(true);
                }
                case BypassTest -> {
                    update = true;
                }
            }
        }
    }

    @EventHandler
    private void onMovePacket(PacketEvent.Sent event) {
        if (mode.get().equals(Mode.BypassTest)) {
            if (event.packet instanceof PlayerMoveC2SPacket playerMove) {
                setFreezeLook(event, playerMove);
            }
        }
        update = false;
    }
    @EventHandler
    private void onMovePacket2(PacketEvent.Send event) {
        if (mode.get().equals(Mode.BypassTest)) {
            if (event.packet instanceof PlayerMoveC2SPacket playerMove) {
                setFreezeLook(event, playerMove);
            }
        }
        update = false;
    }

    public double getHorizontal(Setting<Double> setting) {
        return isActive() ? setting.get() : 1;
    }

    public double getVertical(Setting<Double> setting) {
        return isActive() ? setting.get() : 1;
    }

    private boolean rotate = false;

    private void setFreezeLook(PacketEvent event, PlayerMoveC2SPacket playerMove)
    {
        if  (!update && !mode.get().equals(Mode.BypassTest)) return;

        if (playerMove.changesLook() && !rotate) {
            event.setCancelled(true);
        }
        else if (mc.player != null && playerMove.changesLook()) {
            event.setCancelled(true);
            mc.player.setYaw(yaw);
            mc.player.setPitch(pitch);
        }
        if (mc.player != null && playerMove.changesPosition()) {
            mc.player.setVelocity(0, 0, 0);
            mc.player.setPos(position.x, position.y, position.z);
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void InteractBlockEvent(InteractBlockEvent event)
    {
        if  (!update && !mode.get().equals(Mode.BypassTest)) return;
        if (mc.player != null && mc.getNetworkHandler() != null) {
            PlayerMoveC2SPacket.LookAndOnGround r = new PlayerMoveC2SPacket.LookAndOnGround(mc.player.getYaw(), mc.player.getPitch(), mc.player.isOnGround());
            rotate = true;
            mc.getNetworkHandler().sendPacket(r);
            rotate = false;
        }
    }

    public enum Mode {
        Vanilla,
        Cancel,
        Matrix,
        BypassTest
    }
}
