package me.unariginal.spawncontroller.commands.blacklist;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.ServerCommandSource;

public class Blacklist extends LiteralArgumentBuilder<ServerCommandSource> {
    public Blacklist() {
        super("blacklist");

        then(
                new Add()
        ).then(
                new Remove()
        );
    }
}
