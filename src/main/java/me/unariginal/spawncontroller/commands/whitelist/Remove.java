package me.unariginal.spawncontroller.commands.whitelist;

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
                                            sc.getSpeciesWhitelist().forEach(species -> builder.suggest(species.showdownId().toLowerCase()));
                                            return builder.buildFuture();
                                        })
                                        .executes(this::unWhitelistSpecies)
                        )
        )
        .then(
                CommandManager.literal("biome")
                        .then(
                                CommandManager.argument("biome", StringArgumentType.string())
                                        .requires(Permissions.require("spawncontroller.biome", 4))
                                        .suggests((context, builder)-> {
                                            for (Biome biome : sc.getBiomeWhitelist()) {
                                                sc.server.getOverworld().getRegistryManager().get(RegistryKeys.BIOME).getEntry(biome).getKey().ifPresent(key ->{
                                                    builder.suggest("\"" + key.getValue().toString() + "\"");
                                                });
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(this::unWhitelistBiome)
                        )
        )
        .then(
                CommandManager.literal("world")
                        .then(
                                CommandManager.argument("world", StringArgumentType.string())
                                        .requires(Permissions.require("spawncontroller.world", 4))
                                        .suggests(((context, builder) -> {
                                            sc.getWorldWhitelist().forEach(world -> builder.suggest("\"" + world.getRegistryKey().getValue().toString() + "\""));
                                            return builder.buildFuture();
                                        }))
                                        .executes(this::unWhitelistWorld)
                        )
        )
        .then(
                CommandManager.literal("generation")
                        .then(
                                CommandManager.argument("generation", StringArgumentType.string())
                                        .requires(Permissions.require("spawncontroller.generation", 4))
                                        .suggests((ctx, builder) -> {
                                            for (String generation : sc.getGenerationWhitelist()) {
                                                builder.suggest(generation);
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> unWhitelistLabel(ctx, "generation"))
                        )
        )
        .then(
                CommandManager.literal("form")
                        .then(
                                CommandManager.argument("form", StringArgumentType.string())
                                        .requires(Permissions.require("spawncontroller.form", 4))
                                        .suggests((ctx, builder) -> {
                                            for (String form : sc.getFormWhitelist()) {
                                                builder.suggest(form);
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> unWhitelistLabel(ctx, "form"))
                        )
        )
        .then(
                CommandManager.literal("group")
                        .then(
                                CommandManager.argument("group", StringArgumentType.string())
                                        .requires(Permissions.require("spawncontroller.group", 4))
                                        .suggests((ctx, builder) -> {
                                            for (String group : sc.getGroupWhitelist()) {
                                                builder.suggest(group);
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> unWhitelistLabel(ctx, "group"))
                        )
        )
        .then(
                CommandManager.literal("customlabel")
                        .then(
                                CommandManager.argument("label", StringArgumentType.string())
                                        .requires(Permissions.require("spawncontroller.customlabel", 4))
                                        .suggests((ctx, builder) -> {
                                            for (String label : sc.getCustomLabelWhitelist()) {
                                                builder.suggest(label);
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> unWhitelistLabel(ctx, "label"))
                        )
        );
    }

    public int unWhitelistSpecies(CommandContext<ServerCommandSource> ctx) {
        Species species = SpeciesArgumentType.Companion.getPokemon(ctx, "species");
        ArrayList<Species> toKeep = new ArrayList<>();
        for (Species speciesIndex : sc.getSpeciesWhitelist()) {
            if (!speciesIndex.equals(species)) {
                toKeep.add(speciesIndex);
            }
        }

        if (toKeep.size() == sc.getSpeciesWhitelist().size()) {
            ctx.getSource().sendMessage(Text.literal(species.showdownId().toLowerCase() + " is not whitelisted!"));
            return 0;
        }

        sc.clearSpeciesWhitelist();

        for (Species label : toKeep) {
            sc.whitelistAdd(label);
        }
        sc.config.updateWhitelist();
        ctx.getSource().sendMessage(Text.literal(species.showdownId().toLowerCase() + " has been removed from the whitelist!"));
        return 1;
    }

    public int unWhitelistBiome(CommandContext<ServerCommandSource> ctx) {
        String biomeString = StringArgumentType.getString(ctx, "biome");
        ArrayList<Biome> toKeep = new ArrayList<>();
        for (Biome biome : sc.getBiomeWhitelist()) {
            sc.server.getOverworld().getRegistryManager().get(RegistryKeys.BIOME).getEntry(biome).getKey().ifPresent(key ->{
                if (!((key.getValue().toString()).equalsIgnoreCase(biomeString))) {
                    toKeep.add(biome);
                }
            });
        }

        if (toKeep.size() == sc.getBiomeWhitelist().size()) {
            ctx.getSource().sendMessage(Text.literal(biomeString + " is not whitelisted!"));
            return 0;
        }

        sc.clearBiomeWhitelist();

        for (Biome label : toKeep) {
            sc.whitelistAdd(label);
        }
        sc.config.updateWhitelist();
        ctx.getSource().sendMessage(Text.literal(biomeString + " has been removed from the whitelist!"));
        return 1;
    }

    public int unWhitelistWorld(CommandContext<ServerCommandSource> ctx) {
        String worldString = StringArgumentType.getString(ctx, "world");
        ArrayList<ServerWorld> toKeep = new ArrayList<>();
        for (ServerWorld world : sc.getWorldWhitelist()) {
            if (!(world.getRegistryKey().getValue().toString()).equalsIgnoreCase(worldString)) {
                toKeep.add(world);
            }
        }

        if (toKeep.size() == sc.getWorldWhitelist().size()) {
            ctx.getSource().sendMessage(Text.literal(worldString + " is not whitelisted!"));
            return 0;
        }

        sc.clearWorldWhitelist();

        for (ServerWorld label : toKeep) {
            sc.whitelistAdd(label);
        }

        sc.config.updateWhitelist();
        ctx.getSource().sendMessage(Text.literal(worldString + " has been removed from the whitelist!"));
        return 1;
    }

    public int unWhitelistLabel(CommandContext<ServerCommandSource> ctx, String type) {
        String labelString = StringArgumentType.getString(ctx, type);
        ArrayList<String> toKeep = new ArrayList<>();
        ArrayList<String> loopLabels = new ArrayList<>();
        if (type.equalsIgnoreCase("generation")) {
            loopLabels.addAll(sc.getGenerationWhitelist());
            sc.clearGenerationWhitelist();
        } else if (type.equalsIgnoreCase("form")) {
            loopLabels.addAll(sc.getFormWhitelist());
            sc.clearFormWhitelist();
        } else if (type.equalsIgnoreCase("group")) {
            loopLabels.addAll(sc.getGroupWhitelist());
            sc.clearGroupWhitelist();
        } else if (type.equalsIgnoreCase("label")) {
            loopLabels.addAll(sc.getCustomLabelWhitelist());
            sc.clearCustomLabelWhitelist();
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

        sc.config.updateWhitelist();
        ctx.getSource().sendMessage(Text.literal(type + " " + labelString + " has been removed from the whitelist!"));
        return 1;
    }
}
