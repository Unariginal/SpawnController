package me.unariginal.spawncontroller;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.conditional.RegistryLikeCondition;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.pokemon.labels.CobblemonPokemonLabels;
import com.cobblemon.mod.common.api.spawning.*;
import com.cobblemon.mod.common.api.spawning.condition.*;
import com.cobblemon.mod.common.api.spawning.detail.PokemonSpawnDetail;
import com.cobblemon.mod.common.api.spawning.detail.SpawnDetail;
import com.cobblemon.mod.common.api.spawning.detail.SpawnPool;
import com.cobblemon.mod.common.api.spawning.multiplier.WeightMultiplier;
import com.cobblemon.mod.common.command.argument.SpawnBucketArgumentType;
import com.cobblemon.mod.common.command.argument.SpeciesArgumentType;
import com.cobblemon.mod.common.data.CobblemonDataProvider;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.datafixers.util.Either;
import kotlin.Unit;
import kotlin.ranges.IntRange;
import me.lucko.fabric.api.permissions.v0.Permissions;
import me.unariginal.spawncontroller.commands.ControllerCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.structure.Structure;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class SpawnController implements ModInitializer {
    public static final String MOD_ID = "spawncontroller";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private final List<Species> speciesBlacklist = new ArrayList<>();
    private final List<ServerWorld> worldBlacklist = new ArrayList<>();
    private final List<Biome> biomeBlacklist = new ArrayList<>();
    private final List<String> generationBlacklist = new ArrayList<>();
    private final List<String> formBlacklist = new ArrayList<>();
    private final List<String> groupBlacklist = new ArrayList<>();
    private final List<String> customLabelBlacklist = new ArrayList<>();

    private final List<Species> speciesWhitelist = new ArrayList<>();
    private final List<ServerWorld> worldWhitelist = new ArrayList<>();
    private final List<Biome> biomeWhitelist = new ArrayList<>();
    private final List<String> generationWhitelist = new ArrayList<>();
    private final List<String> formWhitelist = new ArrayList<>();
    private final List<String> groupWhitelist = new ArrayList<>();
    private final List<String> customLabelWhitelist = new ArrayList<>();

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
    public Config config;
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

                for (String label : generationBlacklist) {
                    if (pokemon.getForm().getLabels().contains(label)) {
                        event.cancel();
                    }
                }

                for (String label : formBlacklist) {
                    if (pokemon.getForm().getLabels().contains(label)) {
                        event.cancel();
                    }
                }

                for (String label : groupBlacklist) {
                    if (pokemon.getForm().getLabels().contains(label)) {
                        event.cancel();
                    }
                }

                return Unit.INSTANCE;
            });
        });
    }

    public void reload() {
        this.config = new Config();
    }

    public boolean blacklistAdd(Species species) {
        for (Species existingSpecies : speciesBlacklist) {
            if (existingSpecies.equals(species)) {
                return false;
            }
        }
        speciesBlacklist.add(species);
        return true;
    }

    public boolean whitelistAdd(Species species) {
        for (Species existingSpecies : speciesWhitelist) {
            if (existingSpecies.equals(species)) {
                return false;
            }
        }
        speciesWhitelist.add(species);
        return true;
    }

    public boolean blacklistAdd(ServerWorld world) {
        for (ServerWorld existingWorld : worldBlacklist) {
            if (existingWorld.equals(world)) {
                return false;
            }
        }
        worldBlacklist.add(world);
        return true;
    }

    public boolean whitelistAdd(ServerWorld world) {
        for (ServerWorld existingWorld : worldWhitelist) {
            if (existingWorld.equals(world)) {
                return false;
            }
        }
        worldWhitelist.add(world);
        return true;
    }

    public boolean blacklistAdd(Biome biome) {
        for (Biome existingBiome : biomeBlacklist) {
            if (existingBiome.equals(biome)) {
                return false;
            }
        }
        biomeBlacklist.add(biome);
        return true;
    }

    public boolean whitelistAdd(Biome biome) {
        for (Biome existingBiome : biomeWhitelist) {
            if (existingBiome.equals(biome)) {
                return false;
            }
        }
        biomeWhitelist.add(biome);
        return true;
    }

    public boolean blacklistAdd(String label, String type) {
        ArrayList<String> loopLabels = new ArrayList<>();
        if (type.equalsIgnoreCase("generation")) {
            loopLabels.addAll(generationBlacklist);
        } else if (type.equalsIgnoreCase("form")) {
            loopLabels.addAll(formBlacklist);
        } else if (type.equalsIgnoreCase("group")) {
            loopLabels.addAll(groupBlacklist);
        } else if (type.equalsIgnoreCase("label")) {
            loopLabels.addAll(customLabelBlacklist);
        }

        for (String existingLabel : loopLabels) {
            if (existingLabel.equalsIgnoreCase(label)) {
                return false;
            }
        }

        if (type.equalsIgnoreCase("generation")) {
            generationBlacklist.add(label);
        } else if (type.equalsIgnoreCase("form")) {
            formBlacklist.add(label);
        } else if (type.equalsIgnoreCase("group")) {
            groupBlacklist.add(label);
        } else if (type.equalsIgnoreCase("label")) {
            customLabelBlacklist.add(label);
        }

        return true;
    }

    public boolean whitelistAdd(String label, String type) {
        ArrayList<String> loopLabels = new ArrayList<>();
        if (type.equalsIgnoreCase("generation")) {
            loopLabels.addAll(generationWhitelist);
        } else if (type.equalsIgnoreCase("form")) {
            loopLabels.addAll(formWhitelist);
        } else if (type.equalsIgnoreCase("group")) {
            loopLabels.addAll(groupWhitelist);
        } else if (type.equalsIgnoreCase("label")) {
            loopLabels.addAll(customLabelWhitelist);
        }

        for (String existingLabel : loopLabels) {
            if (existingLabel.equalsIgnoreCase(label)) {
                return false;
            }
        }

        if (type.equalsIgnoreCase("generation")) {
            generationWhitelist.add(label);
        } else if (type.equalsIgnoreCase("form")) {
            formWhitelist.add(label);
        } else if (type.equalsIgnoreCase("group")) {
            groupWhitelist.add(label);
        } else if (type.equalsIgnoreCase("label")) {
            customLabelWhitelist.add(label);
        }

        return true;
    }

    public List<Species> getSpeciesBlacklist() {
        return speciesBlacklist;
    }

    public void clearSpeciesBlacklist() {
        speciesBlacklist.clear();
    }

    public List<Species> getSpeciesWhitelist() {
        return speciesWhitelist;
    }

    public void clearSpeciesWhitelist() {
        speciesWhitelist.clear();
    }

    public List<ServerWorld> getWorldBlacklist() {
        return worldBlacklist;
    }

    public void clearWorldBlacklist() {
        worldBlacklist.clear();
    }

    public List<ServerWorld> getWorldWhitelist() {
        return worldWhitelist;
    }

    public void clearWorldWhitelist() {
        worldWhitelist.clear();
    }

    public List<Biome> getBiomeBlacklist() {
        return biomeBlacklist;
    }

    public void clearBiomeBlacklist() {
        biomeBlacklist.clear();
    }

    public List<Biome> getBiomeWhitelist() {
        return biomeWhitelist;
    }

    public void clearBiomeWhitelist() {
        biomeWhitelist.clear();
    }

    public List<String> getGenerationBlacklist() {
        return generationBlacklist;
    }

    public void clearGenerationBlacklist() {
        generationBlacklist.clear();
    }

    public List<String> getGenerationWhitelist() {
        return generationWhitelist;
    }

    public void clearGenerationWhitelist() {
        generationWhitelist.clear();
    }

    public List<String> getFormBlacklist() {
        return formBlacklist;
    }

    public void clearFormBlacklist() {
        formBlacklist.clear();
    }

    public List<String> getFormWhitelist() {
        return formWhitelist;
    }

    public void clearFormWhitelist() {
        formWhitelist.clear();
    }

    public List<String> getGroupBlacklist() {
        return groupBlacklist;
    }

    public void clearGroupBlacklist() {
        groupBlacklist.clear();
    }

    public List<String> getGroupWhitelist() {
        return groupWhitelist;
    }

    public void clearGroupWhitelist() {
        groupWhitelist.clear();
    }

    public List<String> getCustomLabelBlacklist() {
        return customLabelBlacklist;
    }

    public void clearCustomLabelBlacklist() {
        customLabelBlacklist.clear();
    }

    public List<String> getCustomLabelWhitelist() {
        return customLabelWhitelist;
    }

    public void clearCustomLabelWhitelist() {
        customLabelWhitelist.clear();
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
