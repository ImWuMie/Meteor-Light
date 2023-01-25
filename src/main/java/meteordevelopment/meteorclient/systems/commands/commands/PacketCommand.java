package meteordevelopment.meteorclient.systems.commands.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.systems.commands.Command;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.Socket;
import meteordevelopment.meteorclient.systems.websocket.SocketLaunch;
import meteordevelopment.meteorclient.systems.websocket.results.MessageResult;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.command.CommandSource;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class PacketCommand extends Command {
    private String group_id = "";
    private String spiltStr = ":";

    public PacketCommand() {
        super("packet", "packet", "packet");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        Socket socket = Modules.get().get(Socket.class);
        if (!socket.isActive()) socket.toggle();

        builder.then(literal("custom").then(argument("packet", StringArgumentType.string()).executes(context -> {
            String packet = StringArgumentType.getString(context,"packet");
            switch (socket.mode.get()) {
                case Server -> {
                    SocketLaunch.mainServer.sendDebug(packet);
                }
                case Client -> {
                    SocketLaunch.mainClient.sendDebug(packet);
                }
            }
            return SINGLE_SUCCESS;
        })));

        builder.then(literal("qqgroup").then(argument("id", StringArgumentType.string()).executes(context -> {
            this.group_id = StringArgumentType.getString(context,"id");
            info("设置发送的QQ群为"+group_id);
            return SINGLE_SUCCESS;
        })));

        builder.then(literal("qq").then(argument("message", StringArgumentType.string()).executes(context -> {
            String message = StringArgumentType.getString(context,"message");
            MessageResult result = new MessageResult(new MessageResult.Params(group_id,message));

            switch (socket.mode.get()) {
                case Server -> {
                    SocketLaunch.mainServer.sendDebug(result.toJSON());
                }
                case Client -> {
                    SocketLaunch.mainClient.sendDebug(result.toJSON());
                }
            }
            return SINGLE_SUCCESS;
        })));

        builder.then(literal("stop").executes(context -> {
            if (socket.isActive()) socket.toggle();
            info("已关闭Socket");
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("forward").executes(context -> {
            MeteorClient.EVENT_BUS.subscribe(this);
            info("已开启游戏消息转发");
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("cancel").executes(context -> {
            MeteorClient.EVENT_BUS.unsubscribe(this);
            info("已关闭游戏消息转发");
            return SINGLE_SUCCESS;
        }));
    }

    @EventHandler
    private void onChat(ReceiveMessageEvent e) {
        Socket socket = Modules.get().get(Socket.class);
        spiltStr = socket.spiltStr.get();
        String message = e.getMessage().getString();
        if (spiltStr.isEmpty()) return;
        if (!message.contains(spiltStr)) return;
        String sender = message.split(spiltStr)[0];
        boolean hasEmptyChar = message.split(spiltStr)[1].startsWith(" ");
     //   String receive = hasEmptyChar ? message.replace(sender + spiltStr + " ", "") : message.replace(sender + spiltStr, "");

        if (socket.isAnnoyPlayer(sender)) {
            MessageResult result = new MessageResult(new MessageResult.Params(group_id, message));
            switch (socket.mode.get()) {
                case Server -> {
                    SocketLaunch.mainServer.sendDebug(result.toJSON());
                }
                case Client -> {
                    SocketLaunch.mainClient.sendDebug(result.toJSON());
                }
            }
        }
    }
}
