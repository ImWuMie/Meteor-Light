/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.player;

import meteordevelopment.meteorclient.utils.misc.Vec3;

public class MeteorRotation {
    public double yaw;
    public double pitch;
    public MeteorRotation(double yaw,double pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public double getYaw() {
        return yaw;
    }

    public void setYaw(double yaw) {
        this.yaw = yaw;
    }

    public double getPitch() {
        return pitch;
    }

    public void setPitch(double pitch) {
        this.pitch = pitch;
    }

    public static class VecRotation {
        public Vec3 vec3;
        public MeteorRotation meteorRotation;
        public VecRotation(Vec3 vec, MeteorRotation rotation) {
            this.vec3 = vec;
            this.meteorRotation = rotation;
        }

        public MeteorRotation getRotation() {
            return meteorRotation;
        }
    }
}
