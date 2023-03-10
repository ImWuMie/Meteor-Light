/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.commands.commands;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import de.gerrygames.viarewind.utils.ChatUtil;
import joptsimple.internal.Strings;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.commands.Command;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.world.TickRate;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.command.CommandSource;
import net.minecraft.network.packet.c2s.play.RequestCommandCompletionsC2SPacket;
import net.minecraft.network.packet.s2c.play.CommandSuggestionsS2CPacket;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class ServerCommand extends Command {
    private static final List<String> ANTICHEAT_LIST = Arrays.asList("nocheatplus", "negativity", "warden", "horizon", "illegalstack", "coreprotect", "exploitsx", "vulcan", "abc", "spartan", "kauri", "anticheatreloaded", "witherac", "godseye", "matrix", "wraith", "grimac","verus");

    private static final String pluginsCheck1 = "ver ";
    private static final String pluginsCheck2 = "help ";
    private static final String pluginsCheck3 = "about ";
    private static final String pluginsCheck4 = "/bukkit:pl ";
    private static final String pluginsCheck5 = "/:abcdefghijklmnopqrstuvwxyz0123456789-";
    private int ticks = 0;
    private List<String> plugins = new ArrayList<>();

    private int pluginsCheckMode = 5;
    private boolean ignoreWarning = false;
    private boolean checksuffix = false;

    public ServerCommand() {
        super("server", "Prints server information");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            basicInfo();
            return SINGLE_SUCCESS;
        });

        builder.then(literal("pluginscheck").then(argument("checkmode", IntegerArgumentType.integer(1, 5)).executes(context -> {
            this.pluginsCheckMode = IntegerArgumentType.getInteger(context, "checkmode");
            String mode = "ABC-Check";
            if (pluginsCheckMode == 1) mode = "V-Check";
            if (pluginsCheckMode == 2) mode = "H-Check";
            if (pluginsCheckMode == 3) mode = "A-Check";
            if (pluginsCheckMode == 4) mode = "SP-Check";
            if (pluginsCheckMode == 4) mode = "ABC-Check";
            info("Plugins check mode set to: "+mode);
            return SINGLE_SUCCESS;
        })));

        builder.then(literal("pluginscheck").then(argument("checksuffix", BoolArgumentType.bool()).executes(context -> {
            this.checksuffix = BoolArgumentType.getBool(context, "checksuffix");
            info("Plugins Check plus state: "+checksuffix);
            return SINGLE_SUCCESS;
        })));

        builder.then(literal("info").executes(ctx -> {
            basicInfo();
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("plugins").executes(ctx -> {
            ticks = 0;
            plugins.clear();
            MeteorClient.EVENT_BUS.subscribe(this);
            info("Please wait around 5 seconds...");
            ignoreWarning = false;
            switch (pluginsCheckMode) {
                case 1 -> {
                    (new Thread(() -> {
                        Random random = new Random();
                        if (checksuffix) {
                            pluginsCheck5.chars().forEach(i -> {
                                mc.player.networkHandler.sendPacket(new RequestCommandCompletionsC2SPacket(random.nextInt(200),pluginsCheck1 + Character.toString(i)));
                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            });} else {
                            mc.player.networkHandler.sendPacket(new RequestCommandCompletionsC2SPacket(random.nextInt(200), pluginsCheck1));
                        }
                    })).start();
                }
                case 2 -> {
                    (new Thread(() -> {
                        Random random = new Random();
                        if (checksuffix) {
                            pluginsCheck5.chars().forEach(i -> {
                                mc.player.networkHandler.sendPacket(new RequestCommandCompletionsC2SPacket(random.nextInt(200),pluginsCheck2 + Character.toString(i)));
                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            });} else {
                            mc.player.networkHandler.sendPacket(new RequestCommandCompletionsC2SPacket(random.nextInt(200), pluginsCheck2));
                        }
                    })).start();
                }
                case 3 -> {
                    (new Thread(() -> {
                        Random random = new Random();
                        if (checksuffix) {
                        pluginsCheck5.chars().forEach(i -> {
                            mc.player.networkHandler.sendPacket(new RequestCommandCompletionsC2SPacket(random.nextInt(200),pluginsCheck3 + Character.toString(i)));
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        });} else {
                            mc.player.networkHandler.sendPacket(new RequestCommandCompletionsC2SPacket(random.nextInt(200), pluginsCheck3));
                        }
                    })).start();
                }
                case 4 -> {
                    ignoreWarning = true;
                    ChatUtils.sendPlayerMsg(pluginsCheck4);
                }
                default -> {
                    (new Thread(() -> {
                        Random random = new Random();
                        pluginsCheck5.chars().forEach(i -> {
                            mc.player.networkHandler.sendPacket(new RequestCommandCompletionsC2SPacket(random.nextInt(200), Character.toString(i)));
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        });
                    })).start();
                }
            }
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("tps").executes(ctx -> {
            float tps = TickRate.INSTANCE.getTickRate();
            Formatting color;
            if (tps > 17.0f) color = Formatting.GREEN;
            else if (tps > 12.0f) color = Formatting.YELLOW;
            else color = Formatting.RED;
            info("Current TPS: %s%.2f(default).", color, tps);
            return SINGLE_SUCCESS;
        }));
    }

    private void basicInfo() {
        if (mc.isIntegratedServerRunning()) {
            IntegratedServer server = mc.getServer();

            info("Singleplayer");
            if (server != null) info("Version: %s", server.getVersion());

            return;
        }

        ServerInfo server = mc.getCurrentServerEntry();

        if (server == null) {
            info("Couldn't obtain any server information.");
            return;
        }

        String ipv4 = "";
        try {
            ipv4 = InetAddress.getByName(server.address).getHostAddress();
        } catch (UnknownHostException ignored) {}

        MutableText ipText;

        if (ipv4.isEmpty()) {
            ipText = Text.literal(Formatting.GRAY + server.address);
            ipText.setStyle(ipText.getStyle()
                .withClickEvent(new ClickEvent(
                    ClickEvent.Action.COPY_TO_CLIPBOARD,
                    server.address
                ))
                .withHoverEvent(new HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    Text.literal("Copy to clipboard")
                ))
            );
        }
        else {
            ipText = Text.literal(Formatting.GRAY + server.address);
            ipText.setStyle(ipText.getStyle()
                .withClickEvent(new ClickEvent(
                    ClickEvent.Action.COPY_TO_CLIPBOARD,
                    server.address
                ))
                .withHoverEvent(new HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    Text.literal("Copy to clipboard")
                ))
            );
            MutableText ipv4Text = Text.literal(String.format("%s (%s)", Formatting.GRAY, ipv4));
            ipv4Text.setStyle(ipText.getStyle()
                .withClickEvent(new ClickEvent(
                    ClickEvent.Action.COPY_TO_CLIPBOARD,
                    ipv4
                ))
                .withHoverEvent(new HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    Text.literal("Copy to clipboard")
                ))
            );
            ipText.append(ipv4Text);
        }
        info(
            Text.literal(String.format("%sIP: ", Formatting.GRAY))
            .append(ipText)
        );

        info("Port: %d", ServerAddress.parse(server.address).getPort());

        info("Type: %s", mc.player.getServerBrand() != null ? mc.player.getServerBrand() : "unknown");

        info("Motd: %s", server.label != null ? server.label.getString() : "unknown");

        info("Version: %s", server.version.getString());

        info("Protocol version: %d", server.protocolVersion);

        info("Difficulty: %s (Local: %.2f)", mc.world.getDifficulty().getTranslatableName().getString(), mc.world.getLocalDifficulty(mc.player.getBlockPos()).getLocalDifficulty());

        info("Day: %d", mc.world.getTimeOfDay() / 24000L);

        info("Permission level: %s", formatPerms());
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        ticks++;

        if (ticks >= 100) {
            Collections.sort(plugins);

            for (int i = 0; i < plugins.size(); i++) {
                plugins.set(i, formatName(plugins.get(i)));
            }
            if (!ignoreWarning) {
                if (plugins.isEmpty()) {
                    error("No plugins found.");
                } else {
                    info("Plugins (%d): %s ", plugins.size(), Strings.join(plugins.toArray(new String[0]), ", "));
                }
            }

            ticks = 0;
            plugins.clear();
            MeteorClient.EVENT_BUS.unsubscribe(this);
        }
    }

    @EventHandler
    private void onReadPacket(PacketEvent.Receive event) {
        try {
            if (event.packet instanceof CommandSuggestionsS2CPacket packet) {
                Suggestions matches = packet.getSuggestions();

                if (matches == null) {
                    if (!ignoreWarning) error("Invalid Packet.");
                    return;
                }
                switch (pluginsCheckMode) {
                    case 1, 2, 3 -> {
                        for (Suggestion suggestion : matches.getList()) {
                            String command = suggestion.getText();
                            String pluginName = command.replace("/", "");

                            if (!plugins.contains(pluginName)) {
                                plugins.add(pluginName.toLowerCase());
                            }
                        }
                    }
                    case 4 -> {
                        plugins.clear();
                    }
                    default -> {
                        for (Suggestion suggestion : matches.getList()) {
                            String[] command = suggestion.getText().split(":");
                            if (command.length > 1) {
                                String pluginName = command[0].replace("/", "");

                                if (!plugins.contains(pluginName)) {
                                    plugins.add(pluginName.toLowerCase());
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            error("An error occurred while trying to find plugins");
        }
    }

    private String formatName(String name) {
        if (ANTICHEAT_LIST.contains(name)) {
            return String.format("%s%s(default)", Formatting.RED, name);
        }
        else if (name.contains("exploit") || name.contains("cheat") || name.contains("illegal")) {
            return String.format("%s%s(default)", Formatting.RED, name);
        }

        return String.format("(highlight)%s(default)", name);
    }

    public String formatPerms() {
		int p = 5;
		while (!mc.player.hasPermissionLevel(p) && p > 0) p--;

		return switch (p) {
			case 0 -> "0 (No Perms)";
			case 1 -> "1 (No Perms)";
			case 2 -> "2 (Player Command Access)";
			case 3 -> "3 (Server Command Access)";
			case 4 -> "4 (Operator)";
			default -> p + " (Unknown)";
		};
	}
}
