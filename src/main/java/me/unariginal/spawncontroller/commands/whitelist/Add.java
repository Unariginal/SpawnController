package me.unariginal.spawncontroller.commands.whitelist;

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

                                                sc.getSpeciesWhitelist().forEach(whitelistSpecies -> {
                                                    if (whitelistSpecies.equals(species)) {
                                                        pass.set(false);
                                                    }
                                                });

                                                if (pass.get()) {
                                                    builder.suggest(species.getName().toLowerCase());
                                                }
                                            });

                                            return builder.buildFuture();
                                        })
                                        .executes(this::whitelistSpecies)
                        )
        )
                .then(
                        CommandManager.literal("biome")
                                .then(
                                        CommandManager.argument("biome", StringArgumentType.string())
                                                .requires(Permissions.require("spawncontroller.biome", 4))
                                                .suggests((context, builder)-> {
                                                    for (Biome biome : sc.getRegisteredBiomes()) {
                                                        if (!sc.getBiomeWhitelist().contains(biome)) {
                                                            sc.server.getOverworld().getRegistryManager().get(RegistryKeys.BIOME).getEntry(biome).getKey().ifPresent(key -> {
                                                                builder.suggest("\"" + key.getValue().toString() + "\"");
                                                            });
                                                        }
                                                    }
                                                    return builder.buildFuture();
                                                })
                                                .executes(this::whitelistBiome)
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
                                                .executes(this::whitelistWorld)
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
                                                .executes(ctx -> whitelistLabel(ctx, "generation"))
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
                                                .executes(ctx -> whitelistLabel(ctx, "form"))
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
                                                .executes(ctx -> whitelistLabel(ctx, "group"))
                                )
                )
                .then(
                        CommandManager.literal("customlabel")
                                .then(
                                        CommandManager.argument("label", StringArgumentType.string())
                                                .requires(Permissions.require("spawncontroller.customlabel", 4))
                                                .executes(ctx -> whitelistLabel(ctx, "label"))
                                )
                );
    }

    public int whitelistSpecies(CommandContext<ServerCommandSource> ctx) {
        Species species = SpeciesArgumentType.Companion.getPokemon(ctx, "species");
        if (sc.whitelistAdd(species)) {
            ctx.getSource().sendMessage(Text.literal(species.getName().toLowerCase() + " has been whitelisted!"));
            sc.config.updateWhitelist();
            return 1;
        } else {
            ctx.getSource().sendMessage(Text.literal(species.getName().toLowerCase() + " is already whitelisted!"));
            return 0;
        }
    }

    public int whitelistBiome(CommandContext<ServerCommandSource> ctx) {
        String biomeString = StringArgumentType.getString(ctx, "biome");

        for (Biome biome : sc.getRegisteredBiomes()) {
            AtomicReference<RegistryKey<Biome>> key = new AtomicReference<>();
            sc.server.getOverworld().getRegistryManager().get(RegistryKeys.BIOME).getEntry(biome).getKey().ifPresent(key::set);
            if ((key.get().getValue().toString()).equalsIgnoreCase(biomeString)) {
                if (sc.whitelistAdd(biome)) {
                    ctx.getSource().sendMessage(Text.literal(biomeString + " has been whitelisted!"));
                    sc.config.updateWhitelist();
                    return 1;
                } else {
                    ctx.getSource().sendMessage(Text.literal(biomeString + " is already whitelisted!"));
                    return 0;
                }
            }
        }

        ctx.getSource().sendMessage(Text.literal("Could not find that biome!"));
        return 0;
    }

    public int whitelistWorld(CommandContext<ServerCommandSource> ctx) {
        String worldString = StringArgumentType.getString(ctx, "world");
        for (ServerWorld world : sc.server.getWorlds()) {
            if ((world.getRegistryKey().getValue().toString()).equalsIgnoreCase(worldString)) {
                if (sc.whitelistAdd(world)) {
                    ctx.getSource().sendMessage(Text.literal(worldString + " has been whitelisted!"));
                    sc.config.updateWhitelist();
                    return 1;
                } else {
                    ctx.getSource().sendMessage(Text.literal(worldString + " is already whitelisted!"));
                    return 0;
                }
            }
        }

        ctx.getSource().sendMessage(Text.literal("Could not find that world!"));
        return 0;
    }

    public int whitelistLabel(CommandContext<ServerCommandSource> ctx, String type) {
        String labelString = StringArgumentType.getString(ctx, type);
        if (sc.whitelistAdd(labelString, type)) {
            ctx.getSource().sendMessage(Text.literal(type + " " + labelString + " has been whitelisted!"));
            sc.config.updateWhitelist();
            return 1;
        } else {
            ctx.getSource().sendMessage(Text.literal(type + " " + labelString + " is already whitelisted!"));
            return 0;
        }
    }
}