package me.unariginal.spawncontroller.commands.blacklist;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.command.argument.SpeciesArgumentType;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import me.lucko.fabric.api.permissions.v0.Permissions;
import me.unariginal.spawncontroller.SpawnController;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.world.biome.Biome;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class Add extends LiteralArgumentBuilder<ServerCommandSource> {
    private final SpawnController sc = SpawnController.INSTANCE;

    protected Add() {
        super("add");

        then(
                CommandManager.literal("species")
                        .then(
                                CommandManager.argument("species", SpeciesArgumentType.Companion.species())
                                        .requires(Permissions.require("spawncontroller.species", 4))
                                        .suggests((context, builder) -> {
                                            PokemonSpecies.INSTANCE.getSpecies().forEach(species -> {
                                                AtomicBoolean pass = new AtomicBoolean(true);

                                                sc.getSpeciesBlacklist().forEach(blacklistSpecies -> {
                                                    if (blacklistSpecies.equals(species)) {
                                                        pass.set(false);
                                                    }
                                                });

                                                if (pass.get()) {
                                                    builder.suggest(species.getName().toLowerCase());
                                                }
                                            });

                                            return builder.buildFuture();
                                        })
                                        .executes(this::disableSpecies)
                        )
        )
        .then(
                CommandManager.literal("biome")
                        .then(
                                CommandManager.argument("biome", StringArgumentType.string())
                                        .requires(Permissions.require("spawncontroller.biome", 4))
                                        .suggests((context, builder)-> {
                                            for (Biome biome : sc.getRegisteredBiomes()) {
                                                if (!sc.getBiomeBlacklist().contains(biome)) {
                                                    sc.server.getOverworld().getRegistryManager().get(RegistryKeys.BIOME).getEntry(biome).getKey().ifPresent(key -> {
                                                        builder.suggest("\"" + key.getValue().toString() + "\"");
                                                    });
                                                }
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(this::disableBiome)
                        )
        )
        .then(
                CommandManager.literal("world")
                        .then(
                                CommandManager.argument("world", StringArgumentType.string())
                                        .requires(Permissions.require("spawncontroller.world", 4))
                                        .suggests(((context, builder) -> {
                                            sc.server.getWorlds().forEach(world -> builder.suggest("\"" + world.getRegistryKey().getValue().toString() + "\""));
                                            return builder.buildFuture();
                                        }))
                                        .executes(this::disableWorld)
                        )
        )
        .then(
                CommandManager.literal("generation")
                        .then(
                                CommandManager.argument("generation", StringArgumentType.string())
                                        .requires(Permissions.require("spawncontroller.generation", 4))
                                        .suggests((ctx, builder) -> {
                                            for (String generation : sc.getGenerations()) {
                                                builder.suggest(generation);
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> disableLabel(ctx, "generation"))
                        )
        )
        .then(
                CommandManager.literal("form")
                        .then(
                                CommandManager.argument("form", StringArgumentType.string())
                                        .requires(Permissions.require("spawncontroller.form", 4))
                                        .suggests((ctx, builder) -> {
                                            for (String form : sc.getForms()) {
                                                builder.suggest(form);
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> disableLabel(ctx, "form"))
                        )
        )
        .then(
                CommandManager.literal("group")
                        .then(
                                CommandManager.argument("group", StringArgumentType.string())
                                        .requires(Permissions.require("spawncontroller.group", 4))
                                        .suggests((ctx, builder) -> {
                                            for (String group : sc.getGroups()) {
                                                builder.suggest(group);
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> disableLabel(ctx, "group"))
                        )
        )
        .then(
                CommandManager.literal("customlabel")
                        .then(
                                CommandManager.argument("label", StringArgumentType.string())
                                        .requires(Permissions.require("spawncontroller.customlabel", 4))
                                        .executes(ctx -> disableLabel(ctx, "label"))
                        )
        );
    }

    public int disableSpecies(CommandContext<ServerCommandSource> ctx) {
        Species species = SpeciesArgumentType.Companion.getPokemon(ctx, "species");
        if (sc.blacklistAdd(species)) {
            ctx.getSource().sendMessage(Text.literal("Disabled spawns for species: " + species.getName().toLowerCase() + "!"));
            sc.config.updateBlacklist();
            return 1;
        } else {
            ctx.getSource().sendMessage(Text.literal("Spawn is already disabled for species: " + species.getName().toLowerCase() + "!"));
            return 0;
        }
    }

    public int disableBiome(CommandContext<ServerCommandSource> ctx) {
        String biomeString = StringArgumentType.getString(ctx, "biome");

        for (Biome biome : sc.getRegisteredBiomes()) {
            AtomicReference<RegistryKey<Biome>> key = new AtomicReference<>();
            sc.server.getOverworld().getRegistryManager().get(RegistryKeys.BIOME).getEntry(biome).getKey().ifPresent(key::set);
            if ((key.get().getValue().toString()).equalsIgnoreCase(biomeString)) {
                if (sc.blacklistAdd(biome)) {
                    ctx.getSource().sendMessage(Text.literal("Disabled spawns in biome " + biomeString + "!"));
                    sc.config.updateBlacklist();
                    return 1;
                } else {
                    ctx.getSource().sendMessage(Text.literal("Spawn is already disabled in biome " + biomeString + "!"));
                    return 0;
                }
            }
        }

        ctx.getSource().sendMessage(Text.literal("Could not find that biome!"));
        return 0;
    }

    public int disableWorld(CommandContext<ServerCommandSource> ctx) {
        String worldString = StringArgumentType.getString(ctx, "world");
        for (ServerWorld world : sc.server.getWorlds()) {
            if ((world.getRegistryKey().getValue().toString()).equalsIgnoreCase(worldString)) {
                if (sc.blacklistAdd(world)) {
                    ctx.getSource().sendMessage(Text.literal("Disabled spawns in world " + worldString + "!"));
                    sc.config.updateBlacklist();
                    return 1;
                } else {
                    ctx.getSource().sendMessage(Text.literal("Spawn is already disabled in world " + worldString + "!"));
                    return 0;
                }
            }
        }

        ctx.getSource().sendMessage(Text.literal("Could not find that world!"));
        return 0;
    }

    public int disableLabel(CommandContext<ServerCommandSource> ctx, String type) {
        String labelString = StringArgumentType.getString(ctx, type);
        if (sc.blacklistAdd(labelString, type)) {
            ctx.getSource().sendMessage(Text.literal("Disabled spawns for " + type + ": " + labelString + "!"));
            sc.config.updateBlacklist();
            return 1;
        } else {
            ctx.getSource().sendMessage(Text.literal("Spawn is already disabled for " + type + ": " + labelString + "!"));
            return 0;
        }
    }
}
