package me.unariginal.spawncontroller.commands.whitelist;

import com.cobblemon.mod.common.command.argument.SpeciesArgumentType;
import com.cobblemon.mod.common.pokemon.Species;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import me.lucko.fabric.api.permissions.v0.Permissions;
import me.unariginal.spawncontroller.SpawnController;
import me.unariginal.spawncontroller.config.WhitelistConfig;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.ArrayList;

public class Remove extends LiteralArgumentBuilder<ServerCommandSource> {
    private final SpawnController sc = SpawnController.INSTANCE;

    protected Remove() {
        super("remove");
        requires(Permissions.require("spawncontroller.whitelist.remove", 4));
        then(
                CommandManager.literal("species")
                        .then(
                                CommandManager.argument("species", SpeciesArgumentType.Companion.species())
                                        .requires(Permissions.require("spawncontroller.whitelist.remove.species", 4))
                                        .suggests((context, builder) -> {
                                            WhitelistConfig.whitelist.species.forEach(builder::suggest);
                                            return builder.buildFuture();
                                        })
                                        .executes(this::unWhitelistSpecies)
                        )
        )
        .then(
                CommandManager.literal("biome")
                        .then(
                                CommandManager.argument("biome", StringArgumentType.string())
                                        .requires(Permissions.require("spawncontroller.whitelist.remove.biome", 4))
                                        .suggests((context, builder)-> {
                                            WhitelistConfig.whitelist.biomes.forEach(builder::suggest);
                                            return builder.buildFuture();
                                        })
                                        .executes(this::unWhitelistBiome)
                        )
        )
        .then(
                CommandManager.literal("world")
                        .then(
                                CommandManager.argument("world", StringArgumentType.string())
                                        .requires(Permissions.require("spawncontroller.whitelist.remove.world", 4))
                                        .suggests(((context, builder) -> {
                                            WhitelistConfig.whitelist.worlds.forEach(builder::suggest);
                                            return builder.buildFuture();
                                        }))
                                        .executes(this::unWhitelistWorld)
                        )
        )
        .then(
                CommandManager.literal("generation")
                        .then(
                                CommandManager.argument("generation", StringArgumentType.string())
                                        .requires(Permissions.require("spawncontroller.whitelist.remove.generation", 4))
                                        .suggests((ctx, builder) -> {
                                            WhitelistConfig.whitelist.generations.forEach(builder::suggest);
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> unWhitelistLabel(ctx, "generation"))
                        )
        )
        .then(
                CommandManager.literal("form")
                        .then(
                                CommandManager.argument("form", StringArgumentType.string())
                                        .requires(Permissions.require("spawncontroller.whitelist.remove.form", 4))
                                        .suggests((ctx, builder) -> {
                                            WhitelistConfig.whitelist.forms.forEach(builder::suggest);
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> unWhitelistLabel(ctx, "form"))
                        )
        )
        .then(
                CommandManager.literal("group")
                        .then(
                                CommandManager.argument("group", StringArgumentType.string())
                                        .requires(Permissions.require("spawncontroller.whitelist.remove.group", 4))
                                        .suggests((ctx, builder) -> {
                                            WhitelistConfig.whitelist.groups.forEach(builder::suggest);
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> unWhitelistLabel(ctx, "group"))
                        )
        )
        .then(
                CommandManager.literal("customlabel")
                        .then(
                                CommandManager.argument("label", StringArgumentType.string())
                                        .requires(Permissions.require("spawncontroller.whitelist.remove.customlabel", 4))
                                        .suggests((ctx, builder) -> {
                                            WhitelistConfig.whitelist.customLabels.forEach(builder::suggest);
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> unWhitelistLabel(ctx, "label"))
                        )
        );
    }

    public int unWhitelistSpecies(CommandContext<ServerCommandSource> ctx) {
        Species species = SpeciesArgumentType.Companion.getPokemon(ctx, "species");
        ArrayList<String> toKeep = new ArrayList<>();
        for (String speciesIndex : WhitelistConfig.whitelist.species) {
            if (!speciesIndex.equalsIgnoreCase(species.showdownId())) {
                toKeep.add(speciesIndex);
            }
        }

        if (toKeep.size() == WhitelistConfig.whitelist.species.size()) {
            ctx.getSource().sendMessage(Text.literal(species.showdownId().toLowerCase() + " is not whitelisted!"));
            return 0;
        }

        WhitelistConfig.whitelist.species.removeIf(item -> !toKeep.contains(item));

        WhitelistConfig.save();
        ctx.getSource().sendMessage(Text.literal(species.showdownId().toLowerCase() + " has been removed from the whitelist!"));
        return 1;
    }

    public int unWhitelistBiome(CommandContext<ServerCommandSource> ctx) {
        String biomeString = StringArgumentType.getString(ctx, "biome");
        ArrayList<String> toKeep = new ArrayList<>();
        for (String biome : WhitelistConfig.whitelist.biomes) {
            if (!biome.equalsIgnoreCase(biomeString)) {
                toKeep.add(biome);
            }
        }

        if (toKeep.size() == WhitelistConfig.whitelist.biomes.size()) {
            ctx.getSource().sendMessage(Text.literal(biomeString + " is not whitelisted!"));
            return 0;
        }

        WhitelistConfig.whitelist.biomes.removeIf(item -> !toKeep.contains(item));

        WhitelistConfig.save();
        ctx.getSource().sendMessage(Text.literal(biomeString + " has been removed from the whitelist!"));
        return 1;
    }

    public int unWhitelistWorld(CommandContext<ServerCommandSource> ctx) {
        String worldString = StringArgumentType.getString(ctx, "world");
        ArrayList<String> toKeep = new ArrayList<>();
        for (String world : WhitelistConfig.whitelist.worlds) {
            if (!world.equalsIgnoreCase(worldString)) {
                toKeep.add(world);
            }
        }

        if (toKeep.size() == WhitelistConfig.whitelist.worlds.size()) {
            ctx.getSource().sendMessage(Text.literal(worldString + " is not whitelisted!"));
            return 0;
        }

        WhitelistConfig.whitelist.worlds.removeIf(item -> !toKeep.contains(item));

        WhitelistConfig.save();
        ctx.getSource().sendMessage(Text.literal(worldString + " has been removed from the whitelist!"));
        return 1;
    }

    public int unWhitelistLabel(CommandContext<ServerCommandSource> ctx, String type) {
        String labelString = StringArgumentType.getString(ctx, type);
        ArrayList<String> toKeep = new ArrayList<>();
        ArrayList<String> loopLabels = new ArrayList<>();
        if (type.equalsIgnoreCase("generation")) {
            loopLabels.addAll(WhitelistConfig.whitelist.generations);
            WhitelistConfig.whitelist.generations.clear();
        } else if (type.equalsIgnoreCase("form")) {
            loopLabels.addAll(WhitelistConfig.whitelist.forms);
            WhitelistConfig.whitelist.forms.clear();
        } else if (type.equalsIgnoreCase("group")) {
            loopLabels.addAll(WhitelistConfig.whitelist.groups);
            WhitelistConfig.whitelist.groups.clear();
        } else if (type.equalsIgnoreCase("label")) {
            loopLabels.addAll(WhitelistConfig.whitelist.customLabels);
            WhitelistConfig.whitelist.customLabels.clear();
        }

        for (String label : loopLabels) {
            if (!label.equalsIgnoreCase(labelString)) {
                toKeep.add(label);
            }
        }

        if (toKeep.size() == loopLabels.size()) {
            ctx.getSource().sendMessage(Text.literal(type + " " + labelString + " is not whitelisted!"));
            return 0;
        }

        for (String label : toKeep) {
            sc.whitelistAdd(label, type);
        }

        WhitelistConfig.save();
        ctx.getSource().sendMessage(Text.literal(type + " " + labelString + " has been removed from the whitelist!"));
        return 1;
    }
}
