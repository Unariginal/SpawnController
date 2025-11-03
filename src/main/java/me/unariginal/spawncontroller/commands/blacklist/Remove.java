package me.unariginal.spawncontroller.commands.blacklist;

import com.cobblemon.mod.common.command.argument.SpeciesArgumentType;
import com.cobblemon.mod.common.pokemon.Species;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import me.lucko.fabric.api.permissions.v0.Permissions;
import me.unariginal.spawncontroller.SpawnController;
import me.unariginal.spawncontroller.config.BlacklistConfig;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class Remove extends LiteralArgumentBuilder<ServerCommandSource> {
    private final SpawnController sc = SpawnController.INSTANCE;

    protected Remove() {
        super("remove");
        requires(Permissions.require("spawncontroller.blacklist.remove", 4));
        then(
                CommandManager.literal("species")
                        .then(
                                CommandManager.argument("species", SpeciesArgumentType.Companion.species())
                                        .requires(Permissions.require("spawncontroller.blacklist.remove.species", 4))
                                        .suggests((context, builder) -> {
                                            BlacklistConfig.blacklist.species.forEach(builder::suggest);
                                            return builder.buildFuture();
                                        })
                                        .executes(this::enableSpecies)
                        )
        )
        .then(
                CommandManager.literal("biome")
                        .then(
                                CommandManager.argument("biome", StringArgumentType.string())
                                        .requires(Permissions.require("spawncontroller.blacklist.remove.biome", 4))
                                        .suggests((context, builder)-> {
                                            BlacklistConfig.blacklist.biomes.forEach(builder::suggest);
                                            return builder.buildFuture();
                                        })
                                        .executes(this::enableBiome)
                        )
        )
        .then(
                CommandManager.literal("world")
                        .then(
                                CommandManager.argument("world", StringArgumentType.string())
                                        .requires(Permissions.require("spawncontroller.blacklist.remove.world", 4))
                                        .suggests(((context, builder) -> {
                                            BlacklistConfig.blacklist.worlds.forEach(builder::suggest);
                                            return builder.buildFuture();
                                        }))
                                        .executes(this::enableWorld)
                        )
        )
        .then(
                CommandManager.literal("generation")
                        .then(
                                CommandManager.argument("generation", StringArgumentType.string())
                                        .requires(Permissions.require("spawncontroller.blacklist.remove.generation", 4))
                                        .suggests((ctx, builder) -> {
                                            BlacklistConfig.blacklist.generations.forEach(builder::suggest);
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> enableLabel(ctx, "generation"))
                        )
        )
        .then(
                CommandManager.literal("form")
                        .then(
                                CommandManager.argument("form", StringArgumentType.string())
                                        .requires(Permissions.require("spawncontroller.blacklist.remove.form", 4))
                                        .suggests((ctx, builder) -> {
                                            BlacklistConfig.blacklist.forms.forEach(builder::suggest);
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> enableLabel(ctx, "form"))
                        )
        )
        .then(
                CommandManager.literal("group")
                        .then(
                                CommandManager.argument("group", StringArgumentType.string())
                                        .requires(Permissions.require("spawncontroller.blacklist.remove.group", 4))
                                        .suggests((ctx, builder) -> {
                                            BlacklistConfig.blacklist.groups.forEach(builder::suggest);
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> enableLabel(ctx, "group"))
                        )
        )
        .then(
                CommandManager.literal("customlabel")
                        .then(
                                CommandManager.argument("label", StringArgumentType.string())
                                        .requires(Permissions.require("spawncontroller.blacklist.remove.customlabel", 4))
                                        .suggests((ctx, builder) -> {
                                            BlacklistConfig.blacklist.customLabels.forEach(builder::suggest);
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> enableLabel(ctx, "label"))
                        )
        );
    }

    public int enableSpecies(CommandContext<ServerCommandSource> ctx) {
        Species species = SpeciesArgumentType.Companion.getPokemon(ctx, "species");
        ArrayList<String> toKeep = new ArrayList<>();
        for (String speciesIndex : BlacklistConfig.blacklist.species) {
            if (!speciesIndex.equalsIgnoreCase(species.showdownId())) {
                toKeep.add(speciesIndex);
            }
        }

        if (toKeep.size() == BlacklistConfig.blacklist.species.size()) {
            ctx.getSource().sendMessage(Text.literal("Spawn is already enabled for species: " + species.showdownId().toLowerCase() + "!"));
            return 0;
        }

        BlacklistConfig.blacklist.species.clear();

        for (String label : toKeep) {
            sc.blacklistAddSpecies(label);
        }
        BlacklistConfig.save();
        ctx.getSource().sendMessage(Text.literal("Enabled spawns for species: " + species.showdownId().toLowerCase() + "!"));
        return 1;
    }

    public int enableBiome(CommandContext<ServerCommandSource> ctx) {
        String biomeString = StringArgumentType.getString(ctx, "biome");
        ArrayList<String> toKeep = new ArrayList<>();
        for (String biome : BlacklistConfig.blacklist.biomes) {
            if (!biome.equalsIgnoreCase(biomeString)) {
                toKeep.add(biome);
            }
        }

        if (toKeep.size() == BlacklistConfig.blacklist.biomes.size()) {
            ctx.getSource().sendMessage(Text.literal("Spawn is already enabled for biome: " + biomeString + "!"));
            return 0;
        }

        BlacklistConfig.blacklist.biomes.clear();

        for (String label : toKeep) {
            sc.blacklistAddBiome(label);
        }
        BlacklistConfig.save();
        ctx.getSource().sendMessage(Text.literal("Enabled spawns for biome: " + biomeString + "!"));
        return 1;
    }

    public int enableWorld(CommandContext<ServerCommandSource> ctx) {
        String worldString = StringArgumentType.getString(ctx, "world");
        List<String> toKeep = new ArrayList<>();
        for (String world : BlacklistConfig.blacklist.worlds) {
            if (!world.equalsIgnoreCase(worldString)) {
                toKeep.add(world);
            }
        }

        if (toKeep.size() == BlacklistConfig.blacklist.worlds.size()) {
            ctx.getSource().sendMessage(Text.literal("Spawn is already enabled for world: " + worldString + "!"));
            return 0;
        }

        BlacklistConfig.blacklist.worlds.clear();

        for (String label : toKeep) {
            sc.blacklistAddWorld(label);
        }

        BlacklistConfig.save();
        ctx.getSource().sendMessage(Text.literal("Enabled spawns for world: " + worldString + "!"));
        return 1;
    }

    public int enableLabel(CommandContext<ServerCommandSource> ctx, String type) {
        String labelString = StringArgumentType.getString(ctx, type);
        ArrayList<String> toKeep = new ArrayList<>();
        ArrayList<String> loopLabels = new ArrayList<>();
        if (type.equalsIgnoreCase("generation")) {
            loopLabels.addAll(BlacklistConfig.blacklist.generations);
            BlacklistConfig.blacklist.generations.clear();
        } else if (type.equalsIgnoreCase("form")) {
            loopLabels.addAll(BlacklistConfig.blacklist.forms);
            BlacklistConfig.blacklist.forms.clear();
        } else if (type.equalsIgnoreCase("group")) {
            loopLabels.addAll(BlacklistConfig.blacklist.groups);
            BlacklistConfig.blacklist.groups.clear();
        } else if (type.equalsIgnoreCase("label")) {
            loopLabels.addAll(BlacklistConfig.blacklist.customLabels);
            BlacklistConfig.blacklist.customLabels.clear();
        }

        for (String label : loopLabels) {
            if (!label.equalsIgnoreCase(labelString)) {
                toKeep.add(label);
            }
        }

        if (toKeep.size() == loopLabels.size()) {
            ctx.getSource().sendMessage(Text.literal("Spawn is already enabled for " + type + ": " + labelString + "!"));
            return 0;
        }

        for (String label : toKeep) {
            sc.blacklistAdd(label, type);
        }

        BlacklistConfig.save();
        ctx.getSource().sendMessage(Text.literal("Enabled spawns for " + type + ": " + labelString + "!"));
        return 1;
    }
}
