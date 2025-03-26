package me.unariginal.spawncontroller.commands.whitelist;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.ServerCommandSource;

public class Whitelist extends LiteralArgumentBuilder<ServerCommandSource> {
    public Whitelist() {
        super("whitelist");

        then(
                new Add()
        )
        .then(
                new Remove()
        );
    }
}
