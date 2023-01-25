package meteordevelopment.meteorclient.systems.websocket.client;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.Socket;
import meteordevelopment.meteorclient.systems.websocket.NetworkHandle;
import meteordevelopment.meteorclient.systems.websocket.Packet;
import meteordevelopment.meteorclient.systems.websocket.SocketLaunch;
import meteordevelopment.meteorclient.systems.websocket.packets.client.JoinSocketPacket;
import meteordevelopment.meteorclient.systems.websocket.packets.client.LeftSocketPacket;
import net.minecraft.client.MinecraftClient;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

public class SocketClient extends WebSocketClient {
    public SocketClient(URI serverUri) {
        super(serverUri);
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        send(new JoinSocketPacket("[C] " + MinecraftClient.getInstance().player.getName()));
        SocketLaunch.info("成功连接服务器!");
    }

    @Override
    public void onMessage(String s) {
        if (Modules.get().get(Socket.class).debug.get()) SocketLaunch.info("[Client] ReceivePacket: "+s);
        Packet packet = NetworkHandle.readPacket(s);
        if (packet.name.isEmpty()) return;
        NetworkHandle.applyPacket(packet);
    }

    @Override
    public void onClose(int i, String s, boolean b) {
        send(new LeftSocketPacket("[C] " + MinecraftClient.getInstance().player.getName()));
        SocketLaunch.info("[Client] Stopping Socket Connect!");
    }

    @Override
    public void onError(Exception e) {

    }

    public void sendDebug(String s) {
        SocketLaunch.info("SendPacket: " + s);
        send(s);
    }

    public void send(Packet packet) {
        send(packet.toString());
    }
}
