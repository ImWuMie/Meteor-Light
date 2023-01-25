package meteordevelopment.meteorclient.systems.websocket.packets;

import meteordevelopment.meteorclient.systems.websocket.Packet;
import meteordevelopment.meteorclient.utils.player.ChatUtils;

public class CommandPacket extends Packet {
    public CommandPacket(String command, boolean botCmd) {
        super("command_" + (botCmd ? "a" : "b"), command);
    }

    @Override
    public void apply() {
        boolean botCmd = name.substring("command_".length()).equals("a");
        ChatUtils.sendPlayerMsg(action);
        super.apply();
    }
}
