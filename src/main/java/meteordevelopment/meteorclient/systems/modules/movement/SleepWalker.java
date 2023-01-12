/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.world.UpdateEvent;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.EntityPose;

public class SleepWalker extends Module {
    public SleepWalker() {
        super(Categories.Movement,"SleepWalker","sleep walk");
    }

    @EventHandler
    public void onUpdate(UpdateEvent event) {
        if (mc.player.isSleeping()) {
        mc.player.setPose(EntityPose.STANDING);
        mc.player.clearSleepingPosition();
        }
    }
}
