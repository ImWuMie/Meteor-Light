package meteordevelopment.meteorclient.systems.websocket.server;

import meteordevelopment.meteorclient.systems.commands.commands.Check.GsonUtils;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.Socket;
import meteordevelopment.meteorclient.systems.websocket.NetworkHandle;
import meteordevelopment.meteorclient.systems.websocket.Packet;
import meteordevelopment.meteorclient.systems.websocket.SocketLaunch;
import meteordevelopment.meteorclient.systems.websocket.packets.CommandPacket;
import meteordevelopment.meteorclient.systems.websocket.packets.SayPacket;
import meteordevelopment.meteorclient.systems.websocket.results.MessageResult;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class SocketServer extends WebSocketServer {
    private int connections = 0;
    public List<String> ids = new ArrayList<>();

    public SocketServer(int port) {
        super(new InetSocketAddress(port));
    }

    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        connections++;
        SocketLaunch.info("一个客户端连接到此,当前连接数: " + connections);
    }

    @Override
    public void onClose(WebSocket webSocket, int i, String s, boolean b) {
        connections--;
        SocketLaunch.info("一个客户端断开连接,当前连接数: " + connections);
    }

    @Override
    public void onMessage(WebSocket webSocket, String s) {
        GMessage info = GsonUtils.jsonToBean(s,GMessage.class);

        if (info.getRaw_message() == null) return;

        if (info.getRaw_message().startsWith("packet")) {
            String[] args = info.getRaw_message().split(" ");
            switch (args[1].toLowerCase()) {
                case "chat" -> {
                    String message = info.getRaw_message().substring("packet chat ".length());
                    SayPacket packet = new SayPacket(message);
                    NetworkHandle.applyPacket(packet);
                    MessageResult result = new MessageResult(new MessageResult.Params(info.group_id,"已发送"+message));
                    broadcast(result.toJSON());
                }
                case "cmd" -> {
                    String command = info.getRaw_message().substring("packet cmd ".length());
                    CommandPacket packet = new CommandPacket(command,false);
                    NetworkHandle.applyPacket(packet);
                    MessageResult result = new MessageResult(new MessageResult.Params(info.group_id,"已发送"+command));
                    broadcast(result.toJSON());
                }
                case "custom" -> {
                    String packet = info.raw_message.substring("packet custom ".length());
                    Packet p = NetworkHandle.readPacket(packet);
                    if (p.name.isEmpty()) break;
                    NetworkHandle.applyPacket(p);
                    MessageResult result = new MessageResult(new MessageResult.Params(info.group_id,"已发送"+packet));
                    broadcast(result.toJSON());
                }
            }
            return;
        }

        if (info.getPost_type().equalsIgnoreCase("message")) {
            String name = info.sender.getCard().isEmpty() ? info.sender.getNickname() : info.sender.card;
            SocketLaunch.info("[QQ] ("+info.group_id+") "+name+" -> "+info.getRaw_message());
            return;
        }

        if (Modules.get().get(Socket.class).debug.get())  SocketLaunch.info("[Server] ReceivePacket: "+s);
        Packet packet = NetworkHandle.readPacket(s);
        if (packet.name.isEmpty()) return;
        NetworkHandle.applyPacket(packet);
    }

    @Override
    public void onError(WebSocket webSocket, Exception e) {

    }

    @Override
    public void onStart() {

    }

    public void sendDebug(String s) {
        SocketLaunch.info("SendPacket: "+s);
        broadcast(s);
    }
}
