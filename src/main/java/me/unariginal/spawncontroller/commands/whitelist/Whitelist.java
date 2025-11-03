package me.unariginal.spawncontroller.commands.whitelist;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.command.ServerCommandSource;

public class Whitelist extends LiteralArgumentBuilder<ServerCommandSource> {
    public Whitelist() {
        super("whitelist");
        requires(Permissions.require("spawncontroller.whitelist", 4));
        then(
                new Add()
        )
        .then(
                new Remove()
        );
    }
}
