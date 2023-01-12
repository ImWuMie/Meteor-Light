/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.network;

public class OnlinePlayers {
    private static long lastPingTime;

    public static void update() {
        // deleted by wumie
        /*
        long time = System.currentTimeMillis();

        if (time - lastPingTime > 5 * 60 * 1000) {
            MeteorExecutor.execute(() -> Http.post("https://meteorclient.com/api/online/ping").send());

            lastPingTime = time;
        }*/
    }

    public static void leave() {
        // deleted by wumie
        /*
        MeteorExecutor.execute(() -> Http.post("https://meteorclient.com/api/online/leave").send());
         */
    }
}
