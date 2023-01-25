package meteordevelopment.meteorclient.systems.websocket.packets.client;

import meteordevelopment.meteorclient.systems.websocket.Packet;

public class KeepAlivePacket extends Packet {
    public KeepAlivePacket() {
        super("keepalive", "null");
    }
}
