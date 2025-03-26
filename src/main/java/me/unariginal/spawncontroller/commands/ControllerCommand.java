package me.unariginal.spawncontroller.commands;

import com.mojang.brigadier.CommandDispatcher;
import me.lucko.fabric.api.permissions.v0.Permissions;
import me.unariginal.spawncontroller.Config;
import me.unariginal.spawncontroller.SpawnController;
import me.unariginal.spawncontroller.commands.blacklist.Blacklist;
import me.unariginal.spawncontroller.commands.whitelist.Whitelist;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.CommandManager.*;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class ControllerCommand {
    private final static SpawnController sc = SpawnController.INSTANCE;

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, RegistrationEnvironment environment) {
        dispatcher.register(
                CommandManager.literal("controller")
                        .then(
                                CommandManager.literal("reload")
                                        .requires(Permissions.require("spawncontroller.reload", 4))
                                        .executes(ctx -> {
                                            sc.reload();
                                            ctx.getSource().sendMessage(Text.literal("Config reloaded!"));
                                            return 1;
                                        })
                        )
                        .then(
                                new Whitelist()
                        )
                        .then(
                                new Blacklist()
                        )
                        .then(
                                new Bucket()
                        )
                        .then(
                                new SpawnInfo()
                        )
        );
    }
}
