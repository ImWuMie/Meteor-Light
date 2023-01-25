package meteordevelopment.meteorclient.systems.websocket.packets.client;

import meteordevelopment.meteorclient.systems.websocket.Packet;
import meteordevelopment.meteorclient.systems.websocket.SocketLaunch;
import meteordevelopment.meteorclient.systems.websocket.server.SocketServer;

public class LeftSocketPacket extends Packet {
    public LeftSocketPacket(String id) {
        super("left", id);
    }

    @Override
    public void apply() {
        SocketServer server = SocketLaunch.mainServer;
        SocketLaunch.info("ID为: " + action + " 的断开连接.");
        server.ids.removeIf(s -> s.equals(action));
        super.apply();
    }
}
