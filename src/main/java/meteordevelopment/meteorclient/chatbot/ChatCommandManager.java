/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.chatbot;

import meteordevelopment.meteorclient.chatbot.commands.SayChatCommand;

import java.util.ArrayList;

public class ChatCommandManager {
    private static ArrayList<ChatCommand> commands = new ArrayList<>();

    public static void init() {
        add(new SayChatCommand());
    }

    public static void add(ChatCommand command) {
        commands.add(command);
    }

    public static void run(String receive,String[] args) {
        for (ChatCommand c : commands) {
            if (args[0].equals(c.getName())) {
                String str = receive.replace(c.getName() + " ", "");
                String[] out = str.split(" ");
                c.run(out,str);
            }
        }
    }
}
