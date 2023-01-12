/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.PlayerMoveC2SPacketAccessor;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import meteordevelopment.meteorclient.utils.player.PathFinder;
import meteordevelopment.meteorclient.utils.time.TickTimer;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.AirBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.item.Items;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Flight extends Module {
    private int boostState = 1;
    private double moveSpeed = 0.0;
    private double lastDistance = 0.0;
    private TickTimer timer = new TickTimer();

    public enum Mode {
        Abilities,
        Velocity,
        Soul,
        CubeXOld,
        CubeX
    }

    public enum AntiKickMode {
        Normal,
        Packet,
        None
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgAntiKick = settings.createGroup("Anti Kick"); //Pog

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .description("The mode for Flight.")
            .defaultValue(Mode.Abilities)
            .build()
    );

    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
            .name("speed")
            .description("Your speed when flying.")
            .defaultValue(0.1)
            .visible(() -> !mode.get().equals(Mode.CubeX))
            .min(0.0)
            .build()
    );

    private final Setting<Double> cubexSpeed = sgGeneral.add(new DoubleSetting.Builder()
            .name("cubex-speed")
            .description("Your speed when flying.")
            .defaultValue(1)
            .visible(() -> mode.get().equals(Mode.CubeXOld))
            .range(0, 10)
            .sliderRange(0, 10)
            .build()
    );

    private final Setting<Boolean> elytraDetection = sgGeneral.add(new BoolSetting.Builder()
            .name("elytra-detection")
            .visible(() -> mode.get().equals(Mode.CubeXOld))
            .description("Detect the player has elytra.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> verticalSpeedMatch = sgGeneral.add(new BoolSetting.Builder()
            .name("vertical-speed-match")
            .description("Matches your vertical speed to your horizontal speed, otherwise uses vanilla ratio.")
            .defaultValue(false)
            .build()
    );

    // Anti Kick

    private final Setting<AntiKickMode> antiKickMode = sgAntiKick.add(new EnumSetting.Builder<AntiKickMode>()
            .name("mode")
            .description("The mode for anti kick.")
            .defaultValue(AntiKickMode.Packet)
            .build()
    );

    private final Setting<Integer> delay = sgAntiKick.add(new IntSetting.Builder()
            .name("delay")
            .description("The amount of delay, in ticks, between toggles in normal mode.")
            .defaultValue(80)
            .range(1, 5000)
            .sliderMax(200)
            .visible(() -> antiKickMode.get() == AntiKickMode.Normal)
            .build()
    );

    private final Setting<Integer> offTime = sgAntiKick.add(new IntSetting.Builder()
            .name("off-time")
            .description("The amount of delay, in ticks, that Flight is toggled off for in normal mode.")
            .defaultValue(5)
            .range(1, 20)
            .visible(() -> antiKickMode.get() == AntiKickMode.Normal)
            .build()
    );

    private final Setting<Boolean> soulRenderOriginal = sgGeneral.add(new BoolSetting.Builder()
            .name("render-original")
            .description("Renders your player model at the original position.")
            .defaultValue(true)
            .build()
    );

    public Flight() {
        super(Categories.Movement, "flight", "FLYYYY! No Fall is recommended with this module.");
    }

    private int delayLeft = delay.get();
    private int offLeft = offTime.get();

    private final List<PlayerMoveC2SPacket> soulpackets = new ArrayList<>();
    private FakePlayerEntity soulmodel;
    private int soultimer = 0;
    private boolean issoul = false;

    @Override
    public void onActivate() {
        if (!mc.player.isSpectator()) {
            switch (mode.get()) {
                case Abilities -> {
                    mc.player.getAbilities().flying = true;
                    if (mc.player.getAbilities().creativeMode) return;
                    mc.player.getAbilities().allowFlying = true;
                }
                case Soul -> {
                    if (mc.player.isOnGround()) {
                        mc.player.jump();
                    }
                    mc.player.getAbilities().flying = true;
                    if (mc.player.getAbilities().creativeMode) return;
                    mc.player.getAbilities().allowFlying = true;
                    if (soulRenderOriginal.get()) {
                        soulmodel = new FakePlayerEntity(mc.player, mc.player.getGameProfile().getName(), 20, true);
                        soulmodel.doNotPush = true;
                        soulmodel.hideWhenInsideCamera = true;
                        soulmodel.spawn();
                        issoul = true;
                    }
                }
            }
        }
    }

    @Override
    public void onDeactivate() {
        if (!mc.player.isSpectator()) {
            switch (mode.get()) {
                case Abilities -> {
                    mc.player.getAbilities().flying = false;
                    mc.player.getAbilities().setFlySpeed(0.05f);
                    if (mc.player.getAbilities().creativeMode) return;
                    mc.player.getAbilities().allowFlying = false;
                }
                case Soul -> {
                    mc.player.getAbilities().flying = false;
                    mc.player.getAbilities().setFlySpeed(0.05f);
                    if (mc.player.getAbilities().creativeMode) return;
                    mc.player.getAbilities().allowFlying = false;
                    synchronized (soulpackets) {
                        soulpackets.forEach(mc.player.networkHandler::sendPacket);
                        soulpackets.clear();
                    }

                    if (soulmodel != null) {
                        soulmodel.despawn();
                        soulmodel = null;
                    }

                    soultimer = 0;
                    issoul = false;
                }
            }
        }
    }

    private boolean flip;
    private float lastYaw;

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        float currentYaw = mc.player.getYaw();
        if (mc.player.fallDistance >= 3f && currentYaw == lastYaw && mc.player.getVelocity().length() < 0.003d) {
            mc.player.setYaw(currentYaw + (flip ? 1 : -1));
            flip = !flip;
        }
        lastYaw = currentYaw;
    }

    @EventHandler
    private void onPostTick(TickEvent.Post event) {
        if (mode.get().equals(Mode.Soul)) {
            soultimer++;
        }
        if (antiKickMode.get() == AntiKickMode.Normal && delayLeft > 0) delayLeft--;

        else if (antiKickMode.get() == AntiKickMode.Normal && delayLeft <= 0 && offLeft > 0) {
            offLeft--;

            if (mode.get() == Mode.Abilities) {
                mc.player.getAbilities().flying = false;
                mc.player.getAbilities().setFlySpeed(0.05f);
                if (mc.player.getAbilities().creativeMode) return;
                mc.player.getAbilities().allowFlying = false;
            }

            return;
        } else if (antiKickMode.get() == AntiKickMode.Normal && delayLeft <= 0 && offLeft <= 0) {
            delayLeft = delay.get();
            offLeft = offTime.get();
        }

        if (mc.player.getYaw() != lastYaw) mc.player.setYaw(lastYaw);

        switch (mode.get()) {
            case Velocity -> {
                mc.player.getAbilities().flying = false;
                mc.player.airStrafingSpeed = speed.get().floatValue() * (mc.player.isSprinting() ? 15f : 10f);

                mc.player.setVelocity(0, 0, 0);
                Vec3d initialVelocity = mc.player.getVelocity();

                if (mc.options.jumpKey.isPressed())
                    mc.player.setVelocity(initialVelocity.add(0, speed.get() * (verticalSpeedMatch.get() ? 10f : 5f), 0));
                if (mc.options.sneakKey.isPressed())
                    mc.player.setVelocity(initialVelocity.subtract(0, speed.get() * (verticalSpeedMatch.get() ? 10f : 5f), 0));
            }
            case CubeX -> {
                BlockPos pos = new BlockPos(mc.player.getBlockX(),mc.player.getBlockY()-1,mc.player.getBlockZ());
                BlockState blockState = PathFinder.getBlockState(pos);
                if (blockState.isAir()) {
                    mc.getNetworkHandler().onBlockUpdate(new BlockUpdateS2CPacket(pos, Blocks.STONE.getDefaultState()));
                }
            }
            case CubeXOld -> {
                float speed = (cubexSpeed.get().floatValue() * (mc.player.isSprinting() ? 1.5f : 1f));
                if (elytraDetection.get()) {
                    boolean skip = false;
                    boolean hasElytra = mc.player.getInventory().armor.get(3).getItem().equals(Items.ELYTRA);
                    boolean hasElytra1 = mc.player.getInventory().armor.get(2).getItem().equals(Items.ELYTRA);
                    boolean hasElytra2 = mc.player.getInventory().armor.get(1).getItem().equals(Items.ELYTRA);
                    boolean hasElytra3 = mc.player.getInventory().armor.get(0).getItem().equals(Items.ELYTRA);
                    if (hasElytra || hasElytra1 || hasElytra2 || hasElytra3) skip = true;

                    if (!skip) {
                        error("No elytra found");
                        if (isActive()) {
                            toggle();
                        }
                        break;
                    }
                }

                if (mc.player.isFallFlying()) {
                    KeyBinding[] keys = new KeyBinding[] {
                            mc.options.forwardKey,mc.options.backKey,mc.options.rightKey,mc.options.leftKey
                    };

                    for (KeyBinding key : keys) {
                        key.setPressed(false);
                    }
                }

                mc.player.getAbilities().flying = false;
                boolean forwardKey = mc.options.forwardKey.isPressed();
                boolean backKey = mc.options.backKey.isPressed();
                boolean rightKey = mc.options.rightKey.isPressed();
                boolean leftKey = mc.options.leftKey.isPressed();

                if (forwardKey && rightKey) {
                    speed = (float) (speed / new Random().nextDouble(1.5,2));
                }
                if (forwardKey && leftKey) {
                    speed = (float) (speed / new Random().nextDouble(1.5,2));
                }
                if (backKey && rightKey) {
                    speed = (float) (speed / new Random().nextDouble(1.5,2));
                }
                if (backKey && leftKey) {
                    speed = (float) (speed / new Random().nextDouble(1.5,2));
                }
                mc.player.airStrafingSpeed = (speed / 10);
                mc.player.setVelocity(0, 0, 0);
                Vec3d velocity = mc.player.getVelocity();
                if (mc.options.jumpKey.isPressed()) mc.player.setVelocity(velocity.add(0, (speed / 10), 0));
                if (mc.options.sneakKey.isPressed()) mc.player.setVelocity(velocity.subtract(0, (speed / 10), 0));
            }
            case Abilities -> {
                if (mc.player.isSpectator()) break;
                mc.player.getAbilities().setFlySpeed(speed.get().floatValue());
                mc.player.getAbilities().flying = true;
                if (mc.player.getAbilities().creativeMode) break;
                mc.player.getAbilities().allowFlying = true;
            }
            case Soul -> {
                if (issoul) {
                    if (mc.player.isSpectator()) break;
                    mc.player.getAbilities().setFlySpeed(speed.get().floatValue());
                    mc.player.getAbilities().flying = true;
                    if (mc.player.getAbilities().creativeMode) break;
                    mc.player.getAbilities().allowFlying = true;
                }
            }
        }
    }

    private long lastModifiedTime = 0;
    private double lastY = Double.MAX_VALUE;

    /**
     * @see ServerPlayNetworkHandler#onPlayerMove(PlayerMoveC2SPacket)
     */
    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (!(event.packet instanceof PlayerMoveC2SPacket) || antiKickMode.get() != AntiKickMode.Packet) return;

        PlayerMoveC2SPacket packet = (PlayerMoveC2SPacket) event.packet;
        long currentTime = System.currentTimeMillis();
        double currentY = packet.getY(Double.MAX_VALUE);
        if (currentY != Double.MAX_VALUE) {
            // maximum time we can be "floating" is 80 ticks, so 4 seconds max
            if (currentTime - lastModifiedTime > 1000
                    && lastY != Double.MAX_VALUE
                    && mc.world.getBlockState(mc.player.getBlockPos().down()).isAir()) {
                // actual check is for >= -0.03125D, but we have to do a bit more than that
                // probably due to compression or some shit IDK
                ((PlayerMoveC2SPacketAccessor) packet).setY(lastY - 0.03130D);
                lastModifiedTime = currentTime;
            } else {
                lastY = currentY;
            }
        }
    }

    @EventHandler
    private void onSendPacket1(PacketEvent.Send event) {
        switch (mode.get()) {
            case Soul -> {
                if (!(event.packet instanceof PlayerMoveC2SPacket p)) break;
                event.cancel();

                PlayerMoveC2SPacket prev = soulpackets.size() == 0 ? null : soulpackets.get(soulpackets.size() - 1);

                if (prev != null &&
                        p.isOnGround() == prev.isOnGround() &&
                        p.getYaw(-1) == prev.getYaw(-1) &&
                        p.getPitch(-1) == prev.getPitch(-1) &&
                        p.getX(-1) == prev.getX(-1) &&
                        p.getY(-1) == prev.getY(-1) &&
                        p.getZ(-1) == prev.getZ(-1)
                ) break;

                synchronized (soulpackets) {
                    soulpackets.add(p);
                }
            }
        }

    }
}
