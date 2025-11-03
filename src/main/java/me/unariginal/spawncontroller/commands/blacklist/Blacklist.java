package me.unariginal.spawncontroller.commands.blacklist;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.command.ServerCommandSource;

public class Blacklist extends LiteralArgumentBuilder<ServerCommandSource> {
    public Blacklist() {
        super("blacklist");
        requires(Permissions.require("spawncontroller.blacklist", 4));
        then(
                new Add()
        ).then(
                new Remove()
        );
    }
}
