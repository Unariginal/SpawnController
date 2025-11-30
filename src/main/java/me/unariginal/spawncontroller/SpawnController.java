package me.unariginal.spawncontroller;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.pokemon.labels.CobblemonPokemonLabels;
import com.cobblemon.mod.common.api.spawning.*;
import com.cobblemon.mod.common.api.spawning.condition.*;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import kotlin.Unit;
import me.unariginal.spawncontroller.commands.ControllerCommand;
import me.unariginal.spawncontroller.config.BlacklistConfig;
import me.unariginal.spawncontroller.config.SpawnBucketsConfig;
import me.unariginal.spawncontroller.config.WhitelistConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.biome.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class SpawnController implements ModInitializer {
    public static final String MOD_ID = "spawncontroller";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private final List<Biome> registeredBiomes = new ArrayList<>();

    private final List<String> generations = List.of(
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
    );

    private final List<String> forms = List.of(
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
    );

    private final List<String> groups = List.of(
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
    );

    public static SpawnController INSTANCE;
    public MinecraftServer server;

    @Override
    public void onInitialize() {
        LOGGER.info("[SpawnController] Loading mod..");
        INSTANCE = this;

        CommandRegistrationCallback.EVENT.register(ControllerCommand::register);

        ServerLifecycleEvents.SERVER_STARTED.register((server) -> {
            INSTANCE = this;
            this.server = server;

            for (ServerWorld world : server.getWorlds()) {
                registeredBiomes.addAll(world.getRegistryManager().get(RegistryKeys.BIOME).stream().toList());
            }

            reload();

            LOGGER.info("[SpawnController] Loaded!");

            CobblemonEvents.POKEMON_ENTITY_SPAWN.subscribe(Priority.NORMAL, event -> {
                PokemonEntity pokemonEntity = event.getEntity();
                Pokemon pokemon = pokemonEntity.getPokemon();

                ServerWorld world = event.getSpawnablePosition().getWorld();
                Biome biome = event.getSpawnablePosition().getBiome();

                if (event.getSpawnablePosition().getSpawner().getName().startsWith("poke_snack_")) return Unit.INSTANCE;

                for (String species : BlacklistConfig.blacklist.species) {
                    if (pokemon.getSpecies().showdownId().equalsIgnoreCase(species)) {
                        event.cancel();
                    }
                }

                for (String worldID : BlacklistConfig.blacklist.worlds) {
                    if (world.getRegistryKey().getRegistry().toString().equalsIgnoreCase(worldID)) {
                        event.cancel();
                    }
                }

                for (String biomeID : BlacklistConfig.blacklist.biomes) {
                    AtomicReference<RegistryKey<Biome>> key = new AtomicReference<>();
                    server.getOverworld().getRegistryManager().get(RegistryKeys.BIOME).getEntry(biome).getKey().ifPresent(key::set);
                    if ((key.get().getValue().toString()).equalsIgnoreCase(biomeID)) {
                        event.cancel();
                    }
                }

                for (String label : BlacklistConfig.blacklist.generations) {
                    if (pokemon.getForm().getLabels().contains(label)) {
                        event.cancel();
                    }
                }

                for (String label : BlacklistConfig.blacklist.forms) {
                    if (pokemon.getForm().getLabels().contains(label)) {
                        event.cancel();
                    }
                }

                for (String label : BlacklistConfig.blacklist.groups) {
                    if (pokemon.getForm().getLabels().contains(label)) {
                        event.cancel();
                    }
                }

                for (String label : BlacklistConfig.blacklist.customLabels) {
                    if (pokemon.getForm().getLabels().contains(label)) {
                        event.cancel();
                    }
                }

                return Unit.INSTANCE;
            });
        });
    }

    public void reload() {
        try {
            WhitelistConfig.load();
            BlacklistConfig.load();
            SpawnBucketsConfig.load();
        } catch (IOException e) {
            LOGGER.error("[SpawnController] Failed to load config!");
        }
    }

    public boolean blacklistAddSpecies(String species) {
        for (String existingSpecies : BlacklistConfig.blacklist.species) {
            if (existingSpecies.equalsIgnoreCase(species)) {
                return false;
            }
        }
        BlacklistConfig.blacklist.species.add(species);
        return true;
    }

    public boolean whitelistAddSpecies(String species) {
        for (String existingSpecies : WhitelistConfig.whitelist.species) {
            if (existingSpecies.equalsIgnoreCase(species)) {
                return false;
            }
        }
        WhitelistConfig.whitelist.species.add(species);
        return true;
    }

    public boolean blacklistAddWorld(String worldID) {
        for (String existingWorld : BlacklistConfig.blacklist.worlds) {
            if (existingWorld.equalsIgnoreCase(worldID)) {
                return false;
            }
        }
        BlacklistConfig.blacklist.worlds.add(worldID);
        return true;
    }

    public boolean whitelistAddWorld(String worldID) {
        for (String existingWorld : WhitelistConfig.whitelist.worlds) {
            if (existingWorld.equalsIgnoreCase(worldID)) {
                return false;
            }
        }
        WhitelistConfig.whitelist.worlds.add(worldID);
        return true;
    }

    public boolean blacklistAddBiome(String biome) {
        for (String existingBiome : BlacklistConfig.blacklist.biomes) {
            if (existingBiome.equalsIgnoreCase(biome)) {
                return false;
            }
        }
        BlacklistConfig.blacklist.biomes.add(biome);
        return true;
    }

    public boolean whitelistAddBiome(String biome) {
        for (String existingBiome : WhitelistConfig.whitelist.biomes) {
            if (existingBiome.equalsIgnoreCase(biome)) {
                return false;
            }
        }
        WhitelistConfig.whitelist.biomes.add(biome);
        return true;
    }

    public boolean blacklistAdd(String label, String type) {
        ArrayList<String> loopLabels = new ArrayList<>();
        if (type.equalsIgnoreCase("generation")) {
            loopLabels.addAll(BlacklistConfig.blacklist.generations);
        } else if (type.equalsIgnoreCase("form")) {
            loopLabels.addAll(BlacklistConfig.blacklist.forms);
        } else if (type.equalsIgnoreCase("group")) {
            loopLabels.addAll(BlacklistConfig.blacklist.groups);
        } else if (type.equalsIgnoreCase("label")) {
            loopLabels.addAll(BlacklistConfig.blacklist.customLabels);
        }

        for (String existingLabel : loopLabels) {
            if (existingLabel.equalsIgnoreCase(label)) {
                return false;
            }
        }

        if (type.equalsIgnoreCase("generation")) {
            BlacklistConfig.blacklist.generations.add(label);
        } else if (type.equalsIgnoreCase("form")) {
            BlacklistConfig.blacklist.forms.add(label);
        } else if (type.equalsIgnoreCase("group")) {
            BlacklistConfig.blacklist.groups.add(label);
        } else if (type.equalsIgnoreCase("label")) {
            BlacklistConfig.blacklist.customLabels.add(label);
        }

        return true;
    }

    public boolean whitelistAdd(String label, String type) {
        ArrayList<String> loopLabels = new ArrayList<>();
        if (type.equalsIgnoreCase("generation")) {
            loopLabels.addAll(WhitelistConfig.whitelist.generations);
        } else if (type.equalsIgnoreCase("form")) {
            loopLabels.addAll(WhitelistConfig.whitelist.forms);
        } else if (type.equalsIgnoreCase("group")) {
            loopLabels.addAll(WhitelistConfig.whitelist.groups);
        } else if (type.equalsIgnoreCase("label")) {
            loopLabels.addAll(WhitelistConfig.whitelist.customLabels);
        }

        for (String existingLabel : loopLabels) {
            if (existingLabel.equalsIgnoreCase(label)) {
                return false;
            }
        }

        if (type.equalsIgnoreCase("generation")) {
            WhitelistConfig.whitelist.generations.add(label);
        } else if (type.equalsIgnoreCase("form")) {
            WhitelistConfig.whitelist.forms.add(label);
        } else if (type.equalsIgnoreCase("group")) {
            WhitelistConfig.whitelist.groups.add(label);
        } else if (type.equalsIgnoreCase("label")) {
            WhitelistConfig.whitelist.customLabels.add(label);
        }

        return true;
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

    public List<Biome> getRegisteredBiomes() {
        return registeredBiomes;
    }
}
