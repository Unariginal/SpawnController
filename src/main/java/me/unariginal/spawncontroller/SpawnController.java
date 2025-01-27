package me.unariginal.spawncontroller;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.command.argument.SpeciesArgumentType;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import kotlin.Unit;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.world.biome.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class SpawnController implements ModInitializer {
    public static final String MOD_ID = "spawncontroller";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private final ArrayList<Species> speciesBlacklist = new ArrayList<>();
    private final ArrayList<ServerWorld> worldBlacklist = new ArrayList<>();
    private final ArrayList<Biome> biomeBlacklist = new ArrayList<>();

    private List<Biome> registeredBiomes = new ArrayList<>();

    public static SpawnController instance;
    public Config config;
    public MinecraftServer mcServer;

    @Override
    public void onInitialize() {
        LOGGER.info("[SpawnController] Loading mod..");
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    CommandManager.literal("spawncontroller")
                            .then(
                                    CommandManager.literal("disable")
                                            .then(
                                                    CommandManager.literal("species")
                                                            .then(
                                                                    CommandManager.argument("species", SpeciesArgumentType.Companion.species())
                                                                            .requires(Permissions.require("spawncontroller.disable.species", 4))
                                                                            .suggests((context, builder) -> {
                                                                                PokemonSpecies.INSTANCE.getSpecies().forEach(species -> {
                                                                                    Pokemon temp = new Pokemon();
                                                                                    temp.setSpecies(species);
                                                                                    AtomicBoolean pass = new AtomicBoolean(true);
                                                                                    speciesBlacklist.forEach(blacklistSpecies -> {
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
                                                                            .requires(Permissions.require("spawncontroller.disable.biome", 4))
                                                                            .suggests((context, builder)-> {
                                                                                for (Biome biome : registeredBiomes) {
                                                                                    if (!biomeBlacklist.contains(biome)) {
                                                                                        mcServer.getOverworld().getRegistryManager().get(RegistryKeys.BIOME).getEntry(biome).getKey().ifPresent(key -> {
                                                                                            builder.suggest("\"" + key.getValue().getNamespace() + ":" + key.getValue().getPath() + "\"");
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
                                                                            .requires(Permissions.require("spawncontroller.disable.world", 4))
                                                                            .suggests(((context, builder) -> {
                                                                                mcServer.getWorlds().forEach(world -> builder.suggest("\"" + world.getRegistryKey().getValue().getNamespace() + ":" + world.getRegistryKey().getValue().getPath() + "\""));
                                                                                return builder.buildFuture();
                                                                            }))
                                                                            .executes(this::disableWorld)
                                                            )
                                            )
                            )
                            .then(
                                    CommandManager.literal("enable")
                                            .then(
                                                    CommandManager.literal("species")
                                                            .then(
                                                                    CommandManager.argument("species", SpeciesArgumentType.Companion.species())
                                                                            .requires(Permissions.require("spawncontroller.enable.species", 4))
                                                                            .suggests((context, builder) -> {
                                                                                speciesBlacklist.forEach(species -> builder.suggest(species.getName().toLowerCase()));
                                                                                return builder.buildFuture();
                                                                            })
                                                                            .executes(this::enableSpecies)
                                                            )
                                            )
                                            .then(
                                                    CommandManager.literal("biome")
                                                            .then(
                                                                    CommandManager.argument("biome", StringArgumentType.string())
                                                                            .requires(Permissions.require("spawncontroller.enable.biome", 4))
                                                                            .suggests((context, builder)-> {
                                                                                for (Biome biome : biomeBlacklist) {
                                                                                    mcServer.getOverworld().getRegistryManager().get(RegistryKeys.BIOME).getEntry(biome).getKey().ifPresent(key ->{
                                                                                        builder.suggest("\"" + key.getValue().getNamespace() + ":" + key.getValue().getPath() + "\"");
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
                                                                            .requires(Permissions.require("spawncontroller.enable.world", 4))
                                                                            .suggests(((context, builder) -> {
                                                                                worldBlacklist.forEach(world -> builder.suggest("\"" + world.getRegistryKey().getValue().getNamespace() + ":" + world.getRegistryKey().getValue().getPath() + "\""));
                                                                                return builder.buildFuture();
                                                                            }))
                                                                            .executes(this::enableWorld)
                                                            )
                                            )
                            )
                            .then(
                                    CommandManager.literal("reload")
                                            .requires(Permissions.require("spawncontroller.reload", 4))
                                            .executes(ctx -> {
                                                config = new Config();
                                                ctx.getSource().sendMessage(Text.literal("Config reloaded!"));
                                                return 1;
                                            })
                            )
            );
        });

        ServerLifecycleEvents.SERVER_STARTED.register((server) -> {
            instance = this;
            mcServer = server;

            for (ServerWorld world : SpawnController.instance.mcServer.getWorlds()) {
                registeredBiomes.addAll(world.getRegistryManager().get(RegistryKeys.BIOME).stream().toList());
            }

            config = new Config();

            LOGGER.info("[SpawnController] Loaded!");

            CobblemonEvents.POKEMON_ENTITY_SPAWN.subscribe(Priority.NORMAL, event -> {
                for (Species species : speciesBlacklist) {
                    if (event.getEntity().getPokemon().getSpecies().equals(species)) {
                        event.cancel();
                    }
                }

                for (ServerWorld world : worldBlacklist) {
                    if (event.getCtx().getWorld().equals(world)) {
                        event.cancel();
                    }
                }

                for (Biome biome : biomeBlacklist) {
                    if (event.getCtx().getBiome().equals(biome)) {
                        event.cancel();
                    }
                }

                return Unit.INSTANCE;
            });
        });
    }

    public int enableSpecies(CommandContext<ServerCommandSource> ctx) {
        Species species = SpeciesArgumentType.Companion.getPokemon(ctx, "species");
        ArrayList<Species> toKeep = new ArrayList<>();
        for (Species speciesIndex : speciesBlacklist) {
            if (!speciesIndex.equals(species)) {
                toKeep.add(speciesIndex);
            }
        }

        if (toKeep.size() == speciesBlacklist.size()) {
            ctx.getSource().sendMessage(Text.literal("Spawn is already enabled for species: " + species.getName().toLowerCase() + "!"));
            return 0;
        }

        speciesBlacklist.clear();
        speciesBlacklist.addAll(toKeep);
        config.updateBlacklist();
        ctx.getSource().sendMessage(Text.literal("Enabled spawns for species: " + species.getName().toLowerCase() + "!"));
        return 1;
    }

    public int enableBiome(CommandContext<ServerCommandSource> ctx) {
        String biomeString = StringArgumentType.getString(ctx, "biome");
        ArrayList<Biome> toKeep = new ArrayList<>();
        for (Biome biome : biomeBlacklist) {
            mcServer.getOverworld().getRegistryManager().get(RegistryKeys.BIOME).getEntry(biome).getKey().ifPresent(key ->{
                if (!((key.getValue().getNamespace() + ":" + key.getValue().getPath()).equalsIgnoreCase(biomeString))) {
                    toKeep.add(biome);
                }
            });
        }

        if (toKeep.size() == biomeBlacklist.size()) {
            ctx.getSource().sendMessage(Text.literal("Spawn is already enabled for biome: " + biomeString + "!"));
            return 0;
        }

        biomeBlacklist.clear();
        biomeBlacklist.addAll(toKeep);
        config.updateBlacklist();
        ctx.getSource().sendMessage(Text.literal("Enabled spawns for biome: " + biomeString + "!"));
        return 1;
    }

    public int enableWorld(CommandContext<ServerCommandSource> ctx) {
        String worldString = StringArgumentType.getString(ctx, "world");
        ArrayList<ServerWorld> toKeep = new ArrayList<>();
        for (ServerWorld world : worldBlacklist) {
            if (!(world.getRegistryKey().getValue().getNamespace() + ":" + world.getRegistryKey().getValue().getPath()).equalsIgnoreCase(worldString)) {
                toKeep.add(world);
            }
        }
        if (toKeep.size() == worldBlacklist.size()) {
            ctx.getSource().sendMessage(Text.literal("Spawn is already enabled for world: " + worldString + "!"));
            return 0;
        }
        worldBlacklist.clear();
        worldBlacklist.addAll(toKeep);
        config.updateBlacklist();
        ctx.getSource().sendMessage(Text.literal("Enabled spawns for world: " + worldString + "!"));
        return 1;
    }

    public int disableSpecies(CommandContext<ServerCommandSource> ctx) {
        Species species = SpeciesArgumentType.Companion.getPokemon(ctx, "species");
        if (addToBlacklist(species) == 1) {
            ctx.getSource().sendMessage(Text.literal("Disabled spawns for species: " + species.getName().toLowerCase() + "!"));
            config.updateBlacklist();
            return 1;
        } else {
            ctx.getSource().sendMessage(Text.literal("Spawn is already disabled for species: " + species.getName().toLowerCase() + "!"));
            return 0;
        }
    }

    public int disableBiome(CommandContext<ServerCommandSource> ctx) {
        String biomeString = StringArgumentType.getString(ctx, "biome");

        for (Biome biome : getBiomeList()) {
            AtomicReference<RegistryKey<Biome>> key = new AtomicReference<>();
            mcServer.getOverworld().getRegistryManager().get(RegistryKeys.BIOME).getEntry(biome).getKey().ifPresent(key::set);
            if ((key.get().getValue().getNamespace() + ":" + key.get().getValue().getPath()).equalsIgnoreCase(biomeString)) {
                if (addToBlacklist(biome) == 1) {
                    ctx.getSource().sendMessage(Text.literal("Disabled spawns in biome " + biomeString + "!"));
                    config.updateBlacklist();
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
        for (ServerWorld world : mcServer.getWorlds()) {
            if ((world.getRegistryKey().getValue().getNamespace() + ":" + world.getRegistryKey().getValue().getPath()).equalsIgnoreCase(worldString)) {
                if (addToBlacklist(world) == 1) {
                    ctx.getSource().sendMessage(Text.literal("Disabled spawns in world " + worldString + "!"));
                    config.updateBlacklist();
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

    public int addToBlacklist(Species species) {
        for (Species existingSpecies : speciesBlacklist) {
            if (existingSpecies.equals(species)) {
                return 0;
            }
        }
        speciesBlacklist.add(species);
        return 1;
    }

    public int addToBlacklist(ServerWorld world) {
        for (ServerWorld existingWorld : worldBlacklist) {
            if (existingWorld.equals(world)) {
                return 0;
            }
        }
        worldBlacklist.add(world);
        return 1;
    }

    public int addToBlacklist(Biome biome) {
        for (Biome existingBiome : biomeBlacklist) {
            if (existingBiome.equals(biome)) {
                return 0;
            }
        }
        biomeBlacklist.add(biome);
        return 1;
    }

    public ArrayList<Species> getSpeciesBlacklist() {
        return speciesBlacklist;
    }

    public ArrayList<ServerWorld> getWorldBlacklist() {
        return worldBlacklist;
    }

    public ArrayList<Biome> getBiomeBlacklist() {
        return biomeBlacklist;
    }

    public List<Biome> getBiomeList() {
        return registeredBiomes;
    }
}
