/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.combat;

import baritone.api.BaritoneAPI;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.player.OldHitting;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.Target;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.*;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.ZombifiedPiglinEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

import java.util.ArrayList;
import java.util.List;

public class KillAura extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgTargeting = settings.createGroup("Targeting");
    private final SettingGroup sgDelay = settings.createGroup("Delay");
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final SettingGroup sgRotation = settings.createGroup("Rotation");
    private final SettingGroup sgBlock = settings.createGroup("AutoBlock");

    // General

    private final Setting<Weapon> weapon = sgGeneral.add(new EnumSetting.Builder<Weapon>()
        .name("weapon")
        .description("Only attacks an entity when a specified item is in your hand.")
        .defaultValue(Weapon.Both)
        .build()
    );

    private final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-switch")
        .description("Switches to your selected weapon when attacking the target.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> onlyOnClick = sgGeneral.add(new BoolSetting.Builder()
        .name("only-on-click")
        .description("Only attacks when hold left click.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> onlyWhenLook = sgGeneral.add(new BoolSetting.Builder()
        .name("only-when-look")
        .description("Only attacks when you are looking at the entity.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> randomTeleport = sgGeneral.add(new BoolSetting.Builder()
        .name("random-teleport")
        .description("Randomly teleport around the target.")
        .defaultValue(false)
        .visible(() -> !onlyWhenLook.get())
        .build()
    );


    private final Setting<RandomTPMode> randomTeleportMode = sgGeneral.add(new EnumSetting.Builder<RandomTPMode>()
        .name("random-teleport-mode")
        .defaultValue(RandomTPMode.Normal)
        .build()
    );

    private final Setting<RotationMode> rotation = sgGeneral.add(new EnumSetting.Builder<RotationMode>()
        .name("rotate")
        .description("Determines when you should rotate towards the target.")
        .defaultValue(RotationMode.Always)
        .build()
    );

    private final Setting<SmoothMode> smoothMode = sgGeneral.add(new EnumSetting.Builder<SmoothMode>()
        .name("smooth-mode")
        .defaultValue(SmoothMode.Line)
        .visible(() -> rotation.get().equals(RotationMode.Smooth))
        .build()
    );

    private final Setting<RotationTarget> rotationT = sgGeneral.add(new EnumSetting.Builder<RotationTarget>()
        .name("rotation-target")
        .defaultValue(RotationTarget.Body)
        .visible(() -> rotation.get().equals(RotationMode.Aim))
        .build()
    );

    private final Setting<Double> hitChance = sgGeneral.add(new DoubleSetting.Builder()
        .name("hit-chance")
        .description("The probability of your hits landing.")
        .defaultValue(100)
        .range(1, 100)
        .sliderRange(1, 100)
        .build()
    );

    private final Setting<Boolean> pauseOnCombat = sgGeneral.add(new BoolSetting.Builder()
        .name("pause-on-combat")
        .description("Freezes Baritone temporarily until you are finished attacking the entity.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> noRightClick = sgGeneral.add(new BoolSetting.Builder()
        .name("pause-on-use")
        .description("Does not attack if using an item.")
        .defaultValue(true)
        .build()
    );

    // Targeting

    private final Setting<Boolean> ignorePassive = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-passive")
        .description("Will only attack sometimes passive mobs if they are targeting you.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> ignoreTamed = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-tamed")
        .description("Will avoid attacking mobs you tamed.")
        .defaultValue(false)
        .build()
    );


    private final Setting<Object2BooleanMap<EntityType<?>>> entities = sgTargeting.add(new EntityTypeListSetting.Builder()
        .name("entities")
        .description("Entities to attack.")
        .onlyAttackable()
        .build()
    );

    private final Setting<Double> range = sgTargeting.add(new DoubleSetting.Builder()
        .name("range")
        .description("The maximum range the entity can be to attack it.")
        .defaultValue(4.5)
        .min(0)
        .sliderMax(6)
        .build()
    );

    private final Setting<Double> wallsRange = sgTargeting.add(new DoubleSetting.Builder()
        .name("walls-range")
        .description("The maximum range the entity can be attacked through walls.")
        .defaultValue(3.5)
        .min(0)
        .sliderMax(6)
        .build()
    );

    private final Setting<SortPriority> priority = sgTargeting.add(new EnumSetting.Builder<SortPriority>()
        .name("priority")
        .description("How to filter targets within range.")
        .defaultValue(SortPriority.LowestHealth)
        .build()
    );

    private final Setting<Integer> maxTargets = sgTargeting.add(new IntSetting.Builder()
        .name("max-targets")
        .description("How many entities to target at once.")
        .defaultValue(1)
        .min(1)
        .sliderRange(1, 5)
        .build()
    );

    private final Setting<Boolean> babies = sgTargeting.add(new BoolSetting.Builder()
        .name("babies")
        .description("Whether or not to attack baby variants of the entity.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> nametagged = sgTargeting.add(new BoolSetting.Builder()
        .name("nametagged")
        .description("Whether or not to attack mobs with a name tag.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> ignoreShield = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-shield")
        .description("Attacks only if the blow is not blocked by a shield.")
        .defaultValue(true)
        .build()
    );

    // Delay

    private final Setting<Boolean> smartDelay = sgDelay.add(new BoolSetting.Builder()
        .name("smart-delay")
        .description("Uses the vanilla cooldown to attack entities.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> hitDelay = sgDelay.add(new IntSetting.Builder()
        .name("hit-delay")
        .description("How fast you hit the entity in ticks.")
        .defaultValue(0)
        .min(0)
        .sliderMax(60)
        .visible(() -> !smartDelay.get())
        .build()
    );

    private final Setting<Boolean> randomDelayEnabled = sgDelay.add(new BoolSetting.Builder()
        .name("random-delay-enabled")
        .description("Adds a random delay between hits to attempt to bypass anti-cheats.")
        .defaultValue(false)
        .visible(() -> !smartDelay.get())
        .build()
    );

    private final Setting<Integer> randomDelayMax = sgDelay.add(new IntSetting.Builder()
        .name("random-delay-max")
        .description("The maximum value for random delay.")
        .defaultValue(4)
        .min(0)
        .sliderMax(20)
        .visible(() -> randomDelayEnabled.get() && !smartDelay.get())
        .build()
    );

    private final Setting<Integer> switchDelay = sgDelay.add(new IntSetting.Builder()
        .name("switch-delay")
        .description("How many ticks to wait before hitting an entity after switching hotbar slots.")
        .defaultValue(0)
        .min(0)
        .build()
    );

    // RENDER
    private final Setting<Boolean> esp = sgRender.add(new BoolSetting.Builder()
        .name("ESP")
        .defaultValue(false)
        .build()
    );

    private final Setting<ESPMode> espmode = sgRender.add(new EnumSetting.Builder<ESPMode>()
        .name("esp-mode")
        .defaultValue(ESPMode.Box)
        .visible(esp::get)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The color of the sides of the blocks being rendered.")
        .defaultValue(new SettingColor(204, 0, 0, 10))
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The color of the lines of the blocks being rendered.")
        .defaultValue(new SettingColor(204, 0, 0, 255))
        .build()
    );

    // Rotation
    private final Setting<Double> minTurnSpeed = sgRotation.add(new DoubleSetting.Builder()
        .name("min-turn-speed")
        .defaultValue(180.0)
        .min(0)
        .sliderMax(180.0)
        .build()
    );

    private final Setting<Double> maxTurnSpeed = sgRotation.add(new DoubleSetting.Builder()
        .name("max-turn-speed")
        .defaultValue(180.0)
        .sliderMax(180.0)
        .build()
    );

    private final Setting<Double> aimSpeed = sgRotation.add(new DoubleSetting.Builder()
        .name("aim-speed")
        .defaultValue(5.0)
        .min(0)
        .visible(() -> rotation.get().equals(RotationMode.Aim))
        .build()
    );

    // BLock
    private final Setting<Boolean> autoblock = sgBlock.add(new BoolSetting.Builder()
        .name("auto-block")
        .defaultValue(false)
        .visible(() -> Modules.get().get(OldHitting.class).isActive())
        .build()
    );

    private final Setting<BlockMode> autoblockMode = sgBlock.add(new EnumSetting.Builder<BlockMode>()
        .name("auto-block-mode")
        .defaultValue(BlockMode.NCP)
        .visible(autoblock::get)
        .build()
    );



    private final List<Entity> targets = new ArrayList<>();
    private int hitDelayTimer, switchTimer;
    private boolean wasPathing, isBlocking;

    public KillAura() {
        super(Categories.Combat, "kill-aura", "Attacks specified entities around you.");
    }

    public boolean getBlocking() {
        return isBlocking;
    }

    private boolean hasSword() {
        if (mc.player.getMainHandStack().getItem() == null) {
            return false;
        } else return mc.player.getMainHandStack().getItem() instanceof SwordItem;
    }

    @Override
    public void onDeactivate() {
        hitDelayTimer = 0;
        targets.clear();
    }

    public List<PlayerEntity> getTargets() {
        List<PlayerEntity> idk = new ArrayList<>();
        for (Entity player : targets)
            if (player instanceof PlayerEntity t) {
                idk.add(t);
                if (t.isDead()) {
                    idk.remove(t);
                }
            }
        return idk;
    }

    @EventHandler
    private void onRender3D(Render3DEvent e) {
        this.tickDelta = e.tickDelta;
        if (esp.get()) {
            if (espmode.get().equals(ESPMode.Box)) {
                for (int i = 0; i < (this.targets.size() > maxTargets.get() ? maxTargets.get() : this.targets.size()); ++i) {
                    e.renderer.box(targets.get(i).getBoundingBox(), sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                }
            }
            if (espmode.get().equals(ESPMode.FakeSigma)) {
                for (int ii = 0; ii < (this.targets.size() > maxTargets.get() ? maxTargets.get() : this.targets.size()); ++ii) {
                    e.renderer.drawFakeSigma(e.matrices,targets.get(ii),sideColor.get(),lineColor.get(),shapeMode.get(),0);
                    }
                }
            if (espmode.get().equals(ESPMode.Smooth)) {
                for (int ii = 0; ii < (this.targets.size() > maxTargets.get() ? maxTargets.get() : this.targets.size()); ++ii) {
                    e.renderer.up2Dbox(targets.get(ii).getBoundingBox(),sideColor.get(),lineColor.get(),shapeMode.get(),(targets.get(ii).getBoundingBox().maxY - targets.get(ii).getBoundingBox().minY)/2,0);
                }
            }
            if (espmode.get().equals(ESPMode.Test)) {
                for (int ii = 0; ii < (this.targets.size() > maxTargets.get() ? maxTargets.get() : this.targets.size()); ++ii) {
                    e.renderer.Circle(targets.get(ii),lineColor.get(),shapeMode.get());
                }
            }
            }
        }

        private double tickDelta = 0.0;
    private Entity dirTarget;

    @EventHandler
    private void onTick(Render3DEvent event) {
        if (!mc.player.isAlive() || PlayerUtils.getGameMode() == GameMode.SPECTATOR) return;

        TargetUtils.getList(targets, this::entityCheck, priority.get(), maxTargets.get());

        if (targets.isEmpty()) {
            if (wasPathing) {
                BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("resume");
                wasPathing = false;
            }
            return;
        }

        if (pauseOnCombat.get() && BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing() && !wasPathing) {
            BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("pause");
            wasPathing = true;
        }

        Entity primary = targets.get(0);
        this.dirTarget = primary;
        if (rotation.get() == RotationMode.Always) rotate(primary, null);
        if (rotation.get() == RotationMode.Smooth) Rotations.rotation(primary,smoothMode.get(),true,minTurnSpeed.get(),maxTurnSpeed.get(),new MeteorRotation(mc.player.getYaw(),mc.player.getPitch()));
        if (tickDelta != 0.0) {
            if (rotation.get() == RotationMode.Aim)
                Rotations.rotation(primary, tickDelta, aimSpeed.get(), rotationT.get());
        }
        if (onlyOnClick.get() && !mc.options.attackKey.isPressed()) return;

        if (onlyWhenLook.get()) {
            primary = mc.targetedEntity;

            if (primary == null) return;
            if (!entityCheck(primary)) return;

            targets.clear();
            targets.add(primary);
        }

        if (autoSwitch.get()) {
            FindItemResult weaponResult = InvUtils.findInHotbar(itemStack -> {
                Item item = itemStack.getItem();

                return switch (weapon.get()) {
                    case Axe -> item instanceof AxeItem;
                    case Sword -> item instanceof SwordItem;
                    case Both -> item instanceof AxeItem || item instanceof SwordItem;
                    default -> true;
                };
            });

            InvUtils.swap(weaponResult.slot(), false);
        }

        isBlocking = mc.player.isBlocking();

        if (!itemInHand()) return;

        if (delayCheck()) {
            if (hasSword() && mc.player.isBlocking()) {
                unblock();
            }
            targets.forEach(this::attack);
        }

        if (!mc.player.isBlocking() && this.hasSword() && autoblock.get()) {
            block();
        }

        if (randomTeleport.get() && !onlyWhenLook.get()) {
            if (randomTeleportMode.get().equals(RandomTPMode.Fast)) mc.player.setPosition(primary.getX() + randomOffset(), primary.getY(), primary.getZ() + randomOffset());
        }
    }

    @EventHandler
    private void onTickPre(TickEvent.Pre event) {
        if (randomTeleport.get() && !onlyWhenLook.get()) {
            if (dirTarget == null) return;
            if (randomTeleportMode.get().equals(RandomTPMode.Normal)) mc.player.setPosition(dirTarget.getX() + randomOffset(), dirTarget.getY(), dirTarget.getZ() + randomOffset());
        }
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (event.packet instanceof UpdateSelectedSlotC2SPacket) {
            switchTimer = switchDelay.get();
        }
    }

    private void block() {
        if (autoblock.get() && !isBlocking && !mc.options.useKey.isPressed()) {
            if (autoblockMode.get().equals(BlockMode.NCP)) {
                sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND,new BlockHitResult(new Vec3d(-1,-1,-1), Direction.DOWN,BlockPos.ORIGIN,false),0));
                isBlocking = true;
            }
        }
    }

    private void unblock() {
        if (autoblock.get() && isBlocking) {
            if (autoblockMode.get().equals(BlockMode.NCP)) {
                mc.player.clearActiveItem();
                sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.RELEASE_USE_ITEM,BlockPos.ORIGIN,Direction.DOWN));
                isBlocking = false;
            }
        }
    }

    private double randomOffset() {
        return Math.random() * 4 - 2;
    }

    private boolean entityCheck(Entity entity) {
        if (entity.equals(mc.player) || entity.equals(mc.cameraEntity)) return false;
        if ((entity instanceof LivingEntity && ((LivingEntity) entity).isDead()) || !entity.isAlive()) return false;
        if (noRightClick.get() && (mc.interactionManager.isBreakingBlock() || mc.player.isUsingItem())) return false;
        if (PlayerUtils.distanceTo(entity) > range.get()) return false;
        if (!entities.get().getBoolean(entity.getType())) return false;
        if (!nametagged.get() && entity.hasCustomName()) return false;
        if (!PlayerUtils.canSeeEntity(entity) && PlayerUtils.distanceTo(entity) > wallsRange.get()) return false;
        if (ignoreTamed.get()) {
            if (entity instanceof Tameable tameable
                && tameable.getOwnerUuid() != null
                && tameable.getOwnerUuid().equals(mc.player.getUuid())
            ) return false;
        }
        if (ignorePassive.get()) {
            if (entity instanceof EndermanEntity enderman && !enderman.isAngryAt(mc.player)) return false;
            if (entity instanceof ZombifiedPiglinEntity piglin && !piglin.isAngryAt(mc.player)) return false;
            if (entity instanceof WolfEntity wolf && !wolf.isAttacking()) return false;
        }
        if (entity instanceof PlayerEntity player) {
            if (player.isCreative()) return false;
            if (!Friends.get().shouldAttack(player)) return false;
            if (ignoreShield.get() && player.blockedByShield(DamageSource.player(mc.player))) return false;
            if (Modules.get().get(MeteorAntiBot.class).isBot(player)) return false;
        }
        return !(entity instanceof AnimalEntity animal) || babies.get() || !animal.isBaby();
    }

    private boolean delayCheck() {
        if (switchTimer > 0) {
            switchTimer--;
            return false;
        }

        if (smartDelay.get()) return mc.player.getAttackCooldownProgress(0.5f) >= 1;

        if (hitDelayTimer > 0) {
            hitDelayTimer--;
            return false;
        } else {
            hitDelayTimer = hitDelay.get();
            if (randomDelayEnabled.get()) hitDelayTimer += Math.round(Math.random() * randomDelayMax.get());
            return true;
        }
    }

    private void attack(Entity target) {
        if (Math.random() > hitChance.get() / 100) return;

        if (rotation.get() == RotationMode.OnHit) rotate(target, () -> hitEntity(target));
        else hitEntity(target);
    }

    private void hitEntity(Entity target) {
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private void rotate(Entity target, Runnable callback) {
        Rotations.rotate(Rotations.getYaw(target), Rotations.getPitch(target, Target.Body), callback);
    }

    private boolean itemInHand() {
        return switch (weapon.get()) {
            case Axe -> mc.player.getMainHandStack().getItem() instanceof AxeItem;
            case Sword -> mc.player.getMainHandStack().getItem() instanceof SwordItem;
            case Both -> mc.player.getMainHandStack().getItem() instanceof AxeItem || mc.player.getMainHandStack().getItem() instanceof SwordItem;
            default -> true;
        };
    }

    public Entity getTarget() {
        if (!targets.isEmpty()) return targets.get(0);
        return null;
    }

    @Override
    public String getInfoString() {
        if (!targets.isEmpty()) EntityUtils.getName(getTarget());
        return null;
    }

    public enum Weapon {
        Sword,
        Axe,
        Both,
        Any
    }

    public enum RotationMode {
        Always,
        OnHit,
        Smooth,
        Aim,
        None
    }

    public enum RandomTPMode {
        Normal,
        Fast
    }

    public enum ESPMode {
        Box,
        Smooth,
        FakeSigma,
        Test
    }

    public enum RotationTarget {
        Body,
        Head,
        Feet
    }

    public enum BlockMode {
        NCP
    }
}
