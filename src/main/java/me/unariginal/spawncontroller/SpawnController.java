package me.unariginal.spawncontroller;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.pokeball.catching.modifiers.LabelModifier;
import com.cobblemon.mod.common.api.pokedex.Dexes;
import com.cobblemon.mod.common.api.pokedex.PokedexManager;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.pokemon.labels.CobblemonPokemonLabels;
import com.cobblemon.mod.common.api.spawning.CobblemonSpawnPools;
import com.cobblemon.mod.common.command.argument.SpeciesArgumentType;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
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
    private final ArrayList<String> labelBlacklist = new ArrayList<>();

    private final List<Biome> registeredBiomes = new ArrayList<>();
    private final List<String> generations = new ArrayList<>(List.of(
            CobblemonPokemonLabels.GENERATION_1,
            CobblemonPokemonLabels.GENERATION_2,
            CobblemonPokemonLabels.GENERATION_3,
            CobblemonPokemonLabels.GENERATION_4,
            CobblemonPokemonLabels.GENERATION_5,
            CobblemonPokemonLabels.GENERATION_6,
            CobblemonPokemonLabels.GENERATION_7,
            CobblemonPokemonLabels.GENERATION_7B,
            CobblemonPokemonLabels.GENERATION_8,
            CobblemonPokemonLabels.GENERATION_8A,
            CobblemonPokemonLabels.GENERATION_9
    ));
    private final List<String> forms = new ArrayList<>(List.of(
            CobblemonPokemonLabels.ALOLAN_FORM,
            CobblemonPokemonLabels.GALARIAN_FORM,
            CobblemonPokemonLabels.HISUIAN_FORM,
            CobblemonPokemonLabels.HOENNIAN_FORM,
            CobblemonPokemonLabels.JOHTONIAN_FORM,
            CobblemonPokemonLabels.KALOSIAN_FORM,
            CobblemonPokemonLabels.KANTONIAN_FORM,
            CobblemonPokemonLabels.PALDEAN_FORM,
            CobblemonPokemonLabels.SINNOHAN_FORM,
            CobblemonPokemonLabels.UNOVAN_FORM,
            CobblemonPokemonLabels.MEGA,
            CobblemonPokemonLabels.GMAX,
            CobblemonPokemonLabels.TOTEM,
            CobblemonPokemonLabels.REGIONAL,
            CobblemonPokemonLabels.PRIMAL
    ));
    private final List<String> groups = new ArrayList<>(List.of(
            CobblemonPokemonLabels.BABY,
            CobblemonPokemonLabels.FOSSIL,
            CobblemonPokemonLabels.LEGENDARY,
            CobblemonPokemonLabels.MYTHICAL,
            CobblemonPokemonLabels.PARADOX,
            CobblemonPokemonLabels.ULTRA_BEAST,
            CobblemonPokemonLabels.POWERHOUSE,
            CobblemonPokemonLabels.RESTRICTED,
            CobblemonPokemonLabels.CUSTOM,
            CobblemonPokemonLabels.CUSTOMIZED_OFFICIAL
    ));

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
                                                                            .requires(Permissions.require("spawncontroller.species", 4))
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
                                                                            .requires(Permissions.require("spawncontroller.biome", 4))
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
                                                                            .requires(Permissions.require("spawncontroller.world", 4))
                                                                            .suggests(((context, builder) -> {
                                                                                mcServer.getWorlds().forEach(world -> builder.suggest("\"" + world.getRegistryKey().getValue().getNamespace() + ":" + world.getRegistryKey().getValue().getPath() + "\""));
                                                                                return builder.buildFuture();
                                                                            }))
                                                                            .executes(this::disableWorld)
                                                            )
                                            )
                                            .then(
                                                    CommandManager.literal("generation")
                                                            .then(
                                                                    CommandManager.argument("label", StringArgumentType.string())
                                                                            .requires(Permissions.require("spawncontroller.generation", 4))
                                                                            .suggests((ctx, builder) -> {
                                                                                for (String generation : generations) {
                                                                                    builder.suggest(generation);
                                                                                }
                                                                                return builder.buildFuture();
                                                                            })
                                                                            .executes(this::disableLabel)
                                                            )
                                            )
                                            .then(
                                                    CommandManager.literal("form")
                                                            .then(
                                                                    CommandManager.argument("label", StringArgumentType.string())
                                                                            .requires(Permissions.require("spawncontroller.form", 4))
                                                                            .suggests((ctx, builder) -> {
                                                                                for (String form : forms) {
                                                                                    builder.suggest(form);
                                                                                }
                                                                                return builder.buildFuture();
                                                                            })
                                                                            .executes(this::disableLabel)
                                                            )
                                            )
                                            .then(
                                                    CommandManager.literal("group")
                                                            .then(
                                                                    CommandManager.argument("label", StringArgumentType.string())
                                                                            .requires(Permissions.require("spawncontroller.group", 4))
                                                                            .suggests((ctx, builder) -> {
                                                                                for (String group : groups) {
                                                                                    builder.suggest(group);
                                                                                }
                                                                                return builder.buildFuture();
                                                                            })
                                                                            .executes(this::disableLabel)
                                                            )
                                            )
                                            .then(
                                                    CommandManager.literal("customlabel")
                                                        .then(
                                                                CommandManager.argument("label", StringArgumentType.string())
                                                                        .requires(Permissions.require("spawncontroller.customlabel", 4))
                                                                        .executes(this::disableLabel)
                                                        )
                                            )
                            )
                            .then(
                                    CommandManager.literal("enable")
                                            .then(
                                                    CommandManager.literal("species")
                                                            .then(
                                                                    CommandManager.argument("species", SpeciesArgumentType.Companion.species())
                                                                            .requires(Permissions.require("spawncontroller.species", 4))
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
                                                                            .requires(Permissions.require("spawncontroller.biome", 4))
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
                                                                            .requires(Permissions.require("spawncontroller.world", 4))
                                                                            .suggests(((context, builder) -> {
                                                                                worldBlacklist.forEach(world -> builder.suggest("\"" + world.getRegistryKey().getValue().getNamespace() + ":" + world.getRegistryKey().getValue().getPath() + "\""));
                                                                                return builder.buildFuture();
                                                                            }))
                                                                            .executes(this::enableWorld)
                                                            )
                                            )
                                            .then(
                                                    CommandManager.literal("generation")
                                                            .then(
                                                                    CommandManager.argument("label", StringArgumentType.string())
                                                                            .requires(Permissions.require("spawncontroller.generation", 4))
                                                                            .suggests((ctx, builder) -> {
                                                                                for (String generation : generations) {
                                                                                    if (labelBlacklist.contains(generation)) {
                                                                                        builder.suggest(generation);
                                                                                    }
                                                                                }
                                                                                return builder.buildFuture();
                                                                            })
                                                                            .executes(this::enableLabel)
                                                            )
                                            )
                                            .then(
                                                    CommandManager.literal("form")
                                                            .then(
                                                                    CommandManager.argument("label", StringArgumentType.string())
                                                                            .requires(Permissions.require("spawncontroller.form", 4))
                                                                            .suggests((ctx, builder) -> {
                                                                                for (String form : forms) {
                                                                                    if (labelBlacklist.contains(form)) {
                                                                                        builder.suggest(form);
                                                                                    }
                                                                                }
                                                                                return builder.buildFuture();
                                                                            })
                                                                            .executes(this::enableLabel)
                                                            )
                                            )
                                            .then(
                                                    CommandManager.literal("group")
                                                            .then(
                                                                    CommandManager.argument("label", StringArgumentType.string())
                                                                            .requires(Permissions.require("spawncontroller.group", 4))
                                                                            .suggests((ctx, builder) -> {
                                                                                for (String group : groups) {
                                                                                    if (labelBlacklist.contains(group)) {
                                                                                        builder.suggest(group);
                                                                                    }
                                                                                }
                                                                                return builder.buildFuture();
                                                                            })
                                                                            .executes(this::enableLabel)
                                                            )
                                            )
                                            .then(
                                                    CommandManager.literal("customlabel")
                                                    .then(
                                                            CommandManager.argument("label", StringArgumentType.string())
                                                                    .requires(Permissions.require("spawncontroller.customlabel", 4))
                                                                    .executes(this::enableLabel)
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
                PokemonEntity pokemonEntity = event.getEntity();
                Pokemon pokemon = pokemonEntity.getPokemon();
                ServerWorld world = event.getCtx().getWorld();
                Biome biome = event.getCtx().getBiome();

                for (Species species : speciesBlacklist) {
                    if (pokemon.getSpecies().equals(species)) {
                        event.cancel();
                    }
                }

                for (ServerWorld blWorld : worldBlacklist) {
                    if (world.equals(blWorld)) {
                        event.cancel();
                    }
                }

                for (Biome blBiome : biomeBlacklist) {
                    if (biome.equals(blBiome)) {
                        event.cancel();
                    }
                }

                for (String label : labelBlacklist) {
                    if (pokemon.getSpecies().getLabels().contains(label)) {
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

    public int enableLabel(CommandContext<ServerCommandSource> ctx) {
        String labelString = StringArgumentType.getString(ctx, "label");
        ArrayList<String> toKeep = new ArrayList<>();
        for (String label : labelBlacklist) {
            if (!label.equalsIgnoreCase(labelString)) {
                toKeep.add(label);
            }
        }
        if (toKeep.size() == labelBlacklist.size()) {
            ctx.getSource().sendMessage(Text.literal("Spawn is already enabled for label: " + labelString + "!"));
            return 0;
        }
        labelBlacklist.clear();
        labelBlacklist.addAll(toKeep);
        config.updateBlacklist();
        ctx.getSource().sendMessage(Text.literal("Enabled spawns for label: " + labelString + "!"));
        return 1;
    }

    public int disableSpecies(CommandContext<ServerCommandSource> ctx) {
        Species species = SpeciesArgumentType.Companion.getPokemon(ctx, "species");
        if (addToBlacklist(species)) {
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
                if (addToBlacklist(biome)) {
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
                if (addToBlacklist(world)) {
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

    public int disableLabel(CommandContext<ServerCommandSource> ctx) {
        String labelString = StringArgumentType.getString(ctx, "label");
        if (addToBlacklist(labelString)) {
            ctx.getSource().sendMessage(Text.literal("Disabled spawns for label: " + labelString + "!"));
            config.updateBlacklist();
            return 1;
        } else {
            ctx.getSource().sendMessage(Text.literal("Spawn is already disabled for label: " + labelString + "!"));
            return 0;
        }
    }

    public boolean addToBlacklist(Species species) {
        for (Species existingSpecies : speciesBlacklist) {
            if (existingSpecies.equals(species)) {
                return false;
            }
        }
        speciesBlacklist.add(species);
        return true;
    }

    public boolean addToBlacklist(ServerWorld world) {
        for (ServerWorld existingWorld : worldBlacklist) {
            if (existingWorld.equals(world)) {
                return false;
            }
        }
        worldBlacklist.add(world);
        return true;
    }

    public boolean addToBlacklist(Biome biome) {
        for (Biome existingBiome : biomeBlacklist) {
            if (existingBiome.equals(biome)) {
                return false;
            }
        }
        biomeBlacklist.add(biome);
        return true;
    }

    public boolean addToBlacklist(String label) {
        for (String existingLabel : labelBlacklist) {
            if (existingLabel.equalsIgnoreCase(label)) {
                return false;
            }
        }
        labelBlacklist.add(label);
        return true;
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

    public ArrayList<String> getLabelBlacklist() {
        return labelBlacklist;
    }

    public List<String> getGenerations() {
        return generations;
    }

    public List<String> getForms() {
        return forms;
    }

    public List<String> getGroups() {
        return groups;
    }
}
