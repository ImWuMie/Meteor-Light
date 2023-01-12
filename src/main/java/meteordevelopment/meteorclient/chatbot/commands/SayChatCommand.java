/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.chatbot.commands;

import baritone.api.BaritoneAPI;
import meteordevelopment.meteorclient.chatbot.ChatCommand;
import meteordevelopment.meteorclient.utils.player.ChatUtils;

public class SayChatCommand extends ChatCommand {
    public SayChatCommand() {
        super("say");
    }

    @Override
    public void run(String[] args,String string) {
        if (args.length == 1) {
            if (string.startsWith("#")) {
                BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute(string.replace("#",""));
            } else {
                ChatUtils.sendPlayerMsg(string);
            }
        }
    }
}
