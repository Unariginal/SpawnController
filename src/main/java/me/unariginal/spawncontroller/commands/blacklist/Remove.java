package me.unariginal.spawncontroller.commands.blacklist;

import com.cobblemon.mod.common.command.argument.SpeciesArgumentType;
import com.cobblemon.mod.common.pokemon.Species;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import me.lucko.fabric.api.permissions.v0.Permissions;
import me.unariginal.spawncontroller.SpawnController;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.world.biome.Biome;

import java.util.ArrayList;

public class Remove extends LiteralArgumentBuilder<ServerCommandSource> {
    private final SpawnController sc = SpawnController.INSTANCE;

    protected Remove() {
        super("remove");

        then(
                CommandManager.literal("species")
                        .then(
                                CommandManager.argument("species", SpeciesArgumentType.Companion.species())
                                        .requires(Permissions.require("spawncontroller.species", 4))
                                        .suggests((context, builder) -> {
                                            sc.getSpeciesBlacklist().forEach(species -> builder.suggest(species.showdownId().toLowerCase()));
                                            return builder.buildFuture();
                                        })
                                        .executes(this::enableSpecies)
                        )
        )
        .then(
                CommandManager.literal("biome")
                        .then(
                                CommandManager.argument("biome", StringArgumentType.string())
                                        .requires(Permissions.require("spawncontroller.biome", 4))
                                        .suggests((context, builder)-> {
                                            for (Biome biome : sc.getBiomeBlacklist()) {
                                                sc.server.getOverworld().getRegistryManager().get(RegistryKeys.BIOME).getEntry(biome).getKey().ifPresent(key ->{
                                                    builder.suggest("\"" + key.getValue().toString() + "\"");
                                                });
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(this::enableBiome)
                        )
        )
        .then(
                CommandManager.literal("world")
                        .then(
                                CommandManager.argument("world", StringArgumentType.string())
                                        .requires(Permissions.require("spawncontroller.world", 4))
                                        .suggests(((context, builder) -> {
                                            sc.getWorldBlacklist().forEach(world -> builder.suggest("\"" + world.getRegistryKey().getValue().toString() + "\""));
                                            return builder.buildFuture();
                                        }))
                                        .executes(this::enableWorld)
                        )
        )
        .then(
                CommandManager.literal("generation")
                        .then(
                                CommandManager.argument("generation", StringArgumentType.string())
                                        .requires(Permissions.require("spawncontroller.generation", 4))
                                        .suggests((ctx, builder) -> {
                                            for (String generation : sc.getGenerationBlacklist()) {
                                                builder.suggest(generation);
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> enableLabel(ctx, "generation"))
                        )
        )
        .then(
                CommandManager.literal("form")
                        .then(
                                CommandManager.argument("form", StringArgumentType.string())
                                        .requires(Permissions.require("spawncontroller.form", 4))
                                        .suggests((ctx, builder) -> {
                                            for (String form : sc.getFormBlacklist()) {
                                                builder.suggest(form);
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> enableLabel(ctx, "form"))
                        )
        )
        .then(
                CommandManager.literal("group")
                        .then(
                                CommandManager.argument("group", StringArgumentType.string())
                                        .requires(Permissions.require("spawncontroller.group", 4))
                                        .suggests((ctx, builder) -> {
                                            for (String group : sc.getGroupBlacklist()) {
                                                builder.suggest(group);
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> enableLabel(ctx, "group"))
                        )
        )
        .then(
                CommandManager.literal("customlabel")
                        .then(
                                CommandManager.argument("label", StringArgumentType.string())
                                        .requires(Permissions.require("spawncontroller.customlabel", 4))
                                        .suggests((ctx, builder) -> {
                                            for (String label : sc.getCustomLabelBlacklist()) {
                                                builder.suggest(label);
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> enableLabel(ctx, "label"))
                        )
        );
    }

    public int enableSpecies(CommandContext<ServerCommandSource> ctx) {
        Species species = SpeciesArgumentType.Companion.getPokemon(ctx, "species");
        ArrayList<Species> toKeep = new ArrayList<>();
        for (Species speciesIndex : sc.getSpeciesBlacklist()) {
            if (!speciesIndex.equals(species)) {
                toKeep.add(speciesIndex);
            }
        }

        if (toKeep.size() == sc.getSpeciesBlacklist().size()) {
            ctx.getSource().sendMessage(Text.literal("Spawn is already enabled for species: " + species.showdownId().toLowerCase() + "!"));
            return 0;
        }

        sc.clearSpeciesBlacklist();

        for (Species label : toKeep) {
            sc.blacklistAdd(label);
        }
        sc.config.updateBlacklist();
        ctx.getSource().sendMessage(Text.literal("Enabled spawns for species: " + species.showdownId().toLowerCase() + "!"));
        return 1;
    }

    public int enableBiome(CommandContext<ServerCommandSource> ctx) {
        String biomeString = StringArgumentType.getString(ctx, "biome");
        ArrayList<Biome> toKeep = new ArrayList<>();
        for (Biome biome : sc.getBiomeBlacklist()) {
            sc.server.getOverworld().getRegistryManager().get(RegistryKeys.BIOME).getEntry(biome).getKey().ifPresent(key ->{
                if (!((key.getValue().toString()).equalsIgnoreCase(biomeString))) {
                    toKeep.add(biome);
                }
            });
        }

        if (toKeep.size() == sc.getBiomeBlacklist().size()) {
            ctx.getSource().sendMessage(Text.literal("Spawn is already enabled for biome: " + biomeString + "!"));
            return 0;
        }

        sc.clearBiomeBlacklist();

        for (Biome label : toKeep) {
            sc.blacklistAdd(label);
        }
        sc.config.updateBlacklist();
        ctx.getSource().sendMessage(Text.literal("Enabled spawns for biome: " + biomeString + "!"));
        return 1;
    }

    public int enableWorld(CommandContext<ServerCommandSource> ctx) {
        String worldString = StringArgumentType.getString(ctx, "world");
        ArrayList<ServerWorld> toKeep = new ArrayList<>();
        for (ServerWorld world : sc.getWorldBlacklist()) {
            if (!(world.getRegistryKey().getValue().toString()).equalsIgnoreCase(worldString)) {
                toKeep.add(world);
            }
        }

        if (toKeep.size() == sc.getWorldBlacklist().size()) {
            ctx.getSource().sendMessage(Text.literal("Spawn is already enabled for world: " + worldString + "!"));
            return 0;
        }

        sc.clearWorldBlacklist();

        for (ServerWorld label : toKeep) {
            sc.blacklistAdd(label);
        }

        sc.config.updateBlacklist();
        ctx.getSource().sendMessage(Text.literal("Enabled spawns for world: " + worldString + "!"));
        return 1;
    }

    public int enableLabel(CommandContext<ServerCommandSource> ctx, String type) {
        String labelString = StringArgumentType.getString(ctx, type);
        ArrayList<String> toKeep = new ArrayList<>();
        ArrayList<String> loopLabels = new ArrayList<>();
        if (type.equalsIgnoreCase("generation")) {
            loopLabels.addAll(sc.getGenerationBlacklist());
            sc.clearGenerationBlacklist();
        } else if (type.equalsIgnoreCase("form")) {
            loopLabels.addAll(sc.getFormBlacklist());
            sc.clearFormBlacklist();
        } else if (type.equalsIgnoreCase("group")) {
            loopLabels.addAll(sc.getGroupBlacklist());
            sc.clearGroupBlacklist();
        } else if (type.equalsIgnoreCase("label")) {
            loopLabels.addAll(sc.getCustomLabelBlacklist());
            sc.clearCustomLabelBlacklist();
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

        sc.config.updateBlacklist();
        ctx.getSource().sendMessage(Text.literal("Enabled spawns for " + type + ": " + labelString + "!"));
        return 1;
    }
}
