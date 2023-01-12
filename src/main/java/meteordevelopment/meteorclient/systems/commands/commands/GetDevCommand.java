/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.systems.commands.Command;
import net.minecraft.command.CommandSource;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class GetDevCommand extends Command {
    public GetDevCommand() {
        super("devs", "Meteor Team & wumie", "getdev");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("get").executes(context -> {
            info("Devs:");
            info("Meteor Team & wumie");
            info("private Client :)");
            return SINGLE_SUCCESS;
        }));
    }
}
