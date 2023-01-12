/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.player;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.player.SendMovementPacketsEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.modules.combat.KillAura;
import meteordevelopment.meteorclient.utils.PreInit;
import meteordevelopment.meteorclient.utils.entity.Target;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.misc.Vec3;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Rotations {
    private static final Pool<Rotation> rotationPool = new Pool<>(Rotation::new);
    private static final List<Rotation> rotations = new ArrayList<>();
    public static float serverYaw;
    public static float serverPitch;
    public static int rotationTimer;
    private static float preYaw, prePitch;
    private static int i = 0;
    private static MeteorRotation serverRotation;

    private static Rotation lastRotation;
    private static int lastRotationTimer;
    private static boolean sentLastRotation;
    public static boolean rotating = false;

    @PreInit
    public static void init() {
        MeteorClient.EVENT_BUS.subscribe(Rotations.class);
    }

    public static void rotate(double yaw, double pitch, int priority, boolean clientSide, Runnable callback) {
        mc.player.renderPitch = (float) pitch;
        mc.player.renderYaw = (float) yaw;
        Rotation rotation = rotationPool.get();
        rotation.set(yaw, pitch, priority, clientSide, callback);

        int i = 0;
        for (; i < rotations.size(); i++) {
            if (priority > rotations.get(i).priority) break;
        }

        rotations.add(i, rotation);
    }

    public static void rotate(double yaw, double pitch, int priority, Runnable callback) {
        rotate(yaw, pitch, priority, false, callback);
    }

    public static void rotate(double yaw, double pitch, Runnable callback) {
        rotate(yaw, pitch, 0, callback);
    }

    public static void rotation(Entity entity, SmoothMode smode, boolean th, double minTurnSpeed, double maxTurnSpeed,MeteorRotation sR) {
        serverRotation = sR;
        if (sR == null) return;
        if (serverRotation == null) return;
        Box boundingBox = entity.getBoundingBox();
        MeteorRotation directRotation = calculateCenter(boundingBox).getRotation();

        double diffAngle = getRotationDifference(serverRotation,directRotation);
        if (diffAngle < 0) diffAngle = -diffAngle;
        if (diffAngle > 180.0) diffAngle = 180.0;

        double calculateSpeed = 180.0;
        if (smode.equals(SmoothMode.Line)) {
            calculateSpeed = (diffAngle / 180) * maxTurnSpeed + (1 - diffAngle / 180) * minTurnSpeed;
        }
        if (smode.equals(SmoothMode.Quad)) {
            calculateSpeed = Math.pow(diffAngle / 180.0,2.0) * maxTurnSpeed + (1 - Math.pow(diffAngle / 180.0,2.0)) * minTurnSpeed;
        }
        if (smode.equals(SmoothMode.Sine)) {
            calculateSpeed = (-Math.cos(diffAngle / 180 * Math.PI) * 0.5 + 0.5) * maxTurnSpeed + (Math.cos(diffAngle / 180 * Math.PI) * 0.5 + 0.5) * minTurnSpeed;
        }
        if (smode.equals(SmoothMode.QuadSine)) {
            calculateSpeed = Math.pow(-Math.cos(diffAngle / 180 * Math.PI) * 0.5 + 0.5,2.0) * maxTurnSpeed + (1 - Math.pow(-Math.cos(diffAngle / 180 * Math.PI) * 0.5 + 0.5,2.0)) * minTurnSpeed;
        }

        MeteorRotation rotation = limitAngleChange(serverRotation,directRotation,calculateSpeed);
        rotate(rotation.yaw,rotation.pitch);
    }

    private static final Vec3 vec3d1 = new Vec3();
    public static void rotation(Entity target, double delta,double speed,KillAura.RotationTarget rm) {
        vec3d1.set(target, delta);

            if (rm.equals(KillAura.RotationTarget.Head)) vec3d1.add(0, target.getEyeHeight(target.getPose()), 0);
            if (rm.equals(KillAura.RotationTarget.Body)) vec3d1.add(0, target.getEyeHeight(target.getPose()) / 2, 0);

        double deltaX = vec3d1.x - mc.player.getX();
        double deltaZ = vec3d1.z - mc.player.getZ();
        double deltaY = vec3d1.y - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));

        // Yaw
        double angle = Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90;
        double deltaAngle;
        double toRotate;

            deltaAngle = MathHelper.wrapDegrees(angle - mc.player.getYaw());
            toRotate = speed * (deltaAngle >= 0 ? 1 : -1) * delta;
            if ((toRotate >= 0 && toRotate > deltaAngle) || (toRotate < 0 && toRotate < deltaAngle)) toRotate = deltaAngle;
        rotate(mc.player.getYaw() + (float) toRotate,mc.player.getPitch());
        // Pitch
        double idk = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        angle = -Math.toDegrees(Math.atan2(deltaY, idk));

            deltaAngle = MathHelper.wrapDegrees(angle - mc.player.getPitch());
            toRotate = speed * (deltaAngle >= 0 ? 1 : -1) * delta;
            if ((toRotate >= 0 && toRotate > deltaAngle) || (toRotate < 0 && toRotate < deltaAngle)) toRotate = deltaAngle;
        rotate(mc.player.getYaw(),mc.player.getPitch() + (float) toRotate);
    }

    public static void rotate(double yaw, double pitch) {
        rotate(yaw, pitch, 0, null);
    }

    public static MeteorRotation limitAngleChange(final MeteorRotation currentRotation, final MeteorRotation targetRotation, final double turnSpeed) {
        final float yawDifference = getAngleDifference(targetRotation.getYaw(), currentRotation.getYaw());
        final float pitchDifference = getAngleDifference(targetRotation.getPitch(), currentRotation.getPitch());

        return new MeteorRotation(
            currentRotation.getYaw() + (yawDifference > turnSpeed ? turnSpeed : Math.max(yawDifference, -turnSpeed)),
            currentRotation.getPitch() + (pitchDifference > turnSpeed ? turnSpeed : Math.max(pitchDifference, -turnSpeed)
            ));
    }

    public static double getRotationDifference(final MeteorRotation rotation) {
        return serverRotation == null ? 0D : getRotationDifference(rotation, serverRotation);
    }

    public static float getAngleDifference(final double a, final double b) {
        return (float) (((((a - b) % 360F) + 540F) % 360F) - 180F);
    }

    public static double getRotationDifference(final MeteorRotation a, final MeteorRotation b) {
        return Math.hypot(getAngleDifference(a.yaw, b.yaw), a.pitch - b.pitch);
    }

    public static MeteorRotation.VecRotation calculateCenter(Box bb) {
        MeteorRotation.VecRotation vecRotation = null;

        double xMin = 0.0D;
        double yMin = 0.0D;
        double zMin = 0.0D;
        double xMax = 0.0D;
        double yMax = 0.0D;
        double zMax = 0.0D;
        double xDist = 0.0D;
        double yDist = 0.0D;
        double zDist = 0.0D;

        xMin = 0.15D; xMax = 0.85D; xDist = 0.1D;
        yMin = 0.15D; yMax = 1.00D; yDist = 0.1D;
        zMin = 0.15D; zMax = 0.85D; zDist = 0.1D;

        Vec3 curVec3 = null;


        xMin = 0.45D; xMax = 0.55D; xDist = 0.0125D;
        yMin = 0.10D; yMax = 0.90D; yDist = 0.1D;
        zMin = 0.45D; zMax = 0.55D; zDist = 0.0125D;

        for(double xSearch = xMin; xSearch < xMax; xSearch += xDist) {
            for (double ySearch = yMin; ySearch < yMax; ySearch += yDist) {
                for (double zSearch = zMin; zSearch < zMax; zSearch += zDist) {
                    final Vec3 vec3 = new Vec3(bb.minX + (bb.maxX - bb.minX) * xSearch, bb.minY + (bb.maxY - bb.minY) * ySearch, bb.minZ + (bb.maxZ - bb.minZ) * zSearch);
                    final MeteorRotation rotation = toRotation(vec3);
                    final MeteorRotation.VecRotation currentVec = new MeteorRotation.VecRotation(vec3, rotation);
                        if (vecRotation == null || (getRotationDifference(currentVec.getRotation()) < getRotationDifference(vecRotation.getRotation()))) {
                            vecRotation = currentVec;
                            curVec3 = vec3;
                        }
                    }
                }
            }
        return vecRotation;
    }

    public static MeteorRotation toRotation(final Vec3 vec) {
        final Vec3 eyesPos = new Vec3(mc.player.getX(), mc.player.getBoundingBox().minY +
            mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ());

        final double diffX = vec.x - eyesPos.x;
        final double diffY = vec.y - eyesPos.y;
        final double diffZ = vec.z - eyesPos.z;

        return new MeteorRotation(MathHelper.wrapDegrees(
            (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90F
        ), MathHelper.wrapDegrees(
            (float) (-Math.toDegrees(Math.atan2(diffY, Math.sqrt(diffX * diffX + diffZ * diffZ))))
        ));
    }

    private static void resetLastRotation() {
        if (lastRotation != null) {
            rotationPool.free(lastRotation);

            lastRotation = null;
            lastRotationTimer = 0;
        }
    }

    @EventHandler
    private static void onSendMovementPacketsPre(SendMovementPacketsEvent.Pre event) {
        if (mc.cameraEntity != mc.player) return;
        sentLastRotation = false;

        if (!rotations.isEmpty()) {
            rotating = true;
            resetLastRotation();

            Rotation rotation = rotations.get(i);
            setupMovementPacketRotation(rotation);

            if (rotations.size() > 1) rotationPool.free(rotation);

            i++;
        } else if (lastRotation != null) {
            if (lastRotationTimer >= Config.get().rotationHoldTicks.get()) {
                resetLastRotation();
                rotating = false;
            } else {
                setupMovementPacketRotation(lastRotation);
                sentLastRotation = true;

                lastRotationTimer++;
            }
        }
    }

    private static void setupMovementPacketRotation(Rotation rotation) {
        setClientRotation(rotation);
        setCamRotation(rotation.yaw, rotation.pitch);
    }

    private static void setClientRotation(Rotation rotation) {
        preYaw = mc.player.getYaw();
        prePitch = mc.player.getPitch();

        mc.player.setYaw((float) rotation.yaw);
        mc.player.setPitch((float) rotation.pitch);
    }

    @EventHandler
    private static void onSendMovementPacketsPost(SendMovementPacketsEvent.Post event) {
        if (!rotations.isEmpty()) {
            if (mc.cameraEntity == mc.player) {
                rotations.get(i - 1).runCallback();

                if (rotations.size() == 1) lastRotation = rotations.get(i - 1);

                resetPreRotation();
            }

            for (; i < rotations.size(); i++) {
                Rotation rotation = rotations.get(i);

                setCamRotation(rotation.yaw, rotation.pitch);
                if (rotation.clientSide) setClientRotation(rotation);
                rotation.sendPacket();
                if (rotation.clientSide) resetPreRotation();

                if (i == rotations.size() - 1) lastRotation = rotation;
                else rotationPool.free(rotation);
            }

            rotations.clear();
            i = 0;
        } else if (sentLastRotation) {
            resetPreRotation();
        }
    }

    private static void resetPreRotation() {
        mc.player.setYaw(preYaw);
        mc.player.setPitch(prePitch);
    }

    @EventHandler
    private static void onTick(TickEvent.Pre event) {
        rotationTimer++;
    }

    public static double getYaw(Entity entity) {
        return mc.player.getYaw() + MathHelper.wrapDegrees((float) Math.toDegrees(Math.atan2(entity.getZ() - mc.player.getZ(), entity.getX() - mc.player.getX())) - 90f - mc.player.getYaw());
    }

    public static double getYaw(Vec3d pos) {
        return mc.player.getYaw() + MathHelper.wrapDegrees((float) Math.toDegrees(Math.atan2(pos.getZ() - mc.player.getZ(), pos.getX() - mc.player.getX())) - 90f - mc.player.getYaw());
    }

    public static double getPitch(Vec3d pos) {
        double diffX = pos.getX() - mc.player.getX();
        double diffY = pos.getY() - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));
        double diffZ = pos.getZ() - mc.player.getZ();

        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

        return mc.player.getPitch() + MathHelper.wrapDegrees((float) -Math.toDegrees(Math.atan2(diffY, diffXZ)) - mc.player.getPitch());
    }

    public static double getPitch(Entity entity, Target target) {
        double y;
        if (target == Target.Head) y = entity.getEyeY();
        else if (target == Target.Body) y = entity.getY() + entity.getHeight() / 2;
        else y = entity.getY();

        double diffX = entity.getX() - mc.player.getX();
        double diffY = y - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));
        double diffZ = entity.getZ() - mc.player.getZ();

        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

        return mc.player.getPitch() + MathHelper.wrapDegrees((float) -Math.toDegrees(Math.atan2(diffY, diffXZ)) - mc.player.getPitch());
    }

    public static double getPitch(Entity entity) {
        return getPitch(entity, Target.Body);
    }

    public static double getYaw(BlockPos pos) {
        return mc.player.getYaw() + MathHelper.wrapDegrees((float) Math.toDegrees(Math.atan2(pos.getZ() + 0.5 - mc.player.getZ(), pos.getX() + 0.5 - mc.player.getX())) - 90f - mc.player.getYaw());
    }

    public static double getPitch(BlockPos pos) {
        double diffX = pos.getX() + 0.5 - mc.player.getX();
        double diffY = pos.getY() + 0.5 - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));
        double diffZ = pos.getZ() + 0.5 - mc.player.getZ();

        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

        return mc.player.getPitch() + MathHelper.wrapDegrees((float) -Math.toDegrees(Math.atan2(diffY, diffXZ)) - mc.player.getPitch());
    }

    public static void setCamRotation(double yaw, double pitch) {
        serverYaw = (float) yaw;
        serverPitch = (float) pitch;
        rotationTimer = 0;
    }

    public static void setYawAngle(float yawAngle) {
        mc.player.setYaw(yawAngle);
        mc.player.headYaw = yawAngle;
        mc.player.bodyYaw = yawAngle;
    }

    public static void setPlayerRotation(float yaw,float pitch,Runnable runnable) {
        setYawAngle(yaw);
        setPitchAngle(pitch);
        if (runnable != null) runnable.run();
    }

    public static void setPitchAngle(float pitchAngle) {
        mc.player.setPitch(pitchAngle);
    }

    private static class Rotation {
        public double yaw, pitch;
        public int priority;
        public boolean clientSide;
        public Runnable callback;

        public void set(double yaw, double pitch, int priority, boolean clientSide, Runnable callback) {
            this.yaw = yaw;
            this.pitch = pitch;
            this.priority = priority;
            this.clientSide = clientSide;
            this.callback = callback;
        }

        public void sendPacket() {
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround((float) yaw, (float) pitch, mc.player.isOnGround()));
            runCallback();
        }

        public void runCallback() {
            if (callback != null) callback.run();
        }
    }
}
