/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.chatbot;

import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.network.Packet;

public abstract class ChatCommand {
    private String name;
    public MinecraftClient mc = MinecraftClient.getInstance();

    public ChatCommand(String name) {
        this.name = name;
    }

    public abstract void run(String[] args,String receive);

    public void sendChatMessage(String message) {
        ChatUtils.sendPlayerMsg(message);
    }

    public void sendPacket(Packet packet) {
        mc.getNetworkHandler().sendPacket(packet);
    }

    public void attackEntity(Entity entity) {
        assert mc.interactionManager != null;
        mc.interactionManager.attackEntity(mc.player,entity);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
