package meteordevelopment.meteorclient.systems.websocket.packets;

import meteordevelopment.meteorclient.systems.websocket.Packet;
import meteordevelopment.meteorclient.utils.player.ChatUtils;

public class SayPacket extends Packet {
    public SayPacket(String message) {
        super("say", message);
    }

    @Override
    public void apply() {
        ChatUtils.sendPlayerMsg(action);
        super.apply();
    }
}
