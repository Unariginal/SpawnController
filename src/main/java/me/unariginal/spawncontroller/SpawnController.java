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
import com.cobblemon.mod.common.api.spawning.multiplier.WeightMultiplier;
import com.cobblemon.mod.common.command.argument.SpawnBucketArgumentType;
import com.cobblemon.mod.common.command.argument.SpeciesArgumentType;
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

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    CommandManager.literal("spawncontroller")
                            .then(
                                    CommandManager.literal("spawn-info")
                                            .requires(Permissions.require("spawncontroller.spawninfo", 4))
                                            .then(
                                                    CommandManager.literal("pokemon")
                                                            .then(
                                                                    CommandManager.argument("pokemon", SpeciesArgumentType.Companion.species())
                                                                            .suggests((context, builder) -> {
                                                                                PokemonSpecies.INSTANCE.getSpecies().forEach(species -> builder.suggest(species.getName().toLowerCase()));
                                                                                return builder.buildFuture();
                                                                            })
                                                                            .executes(ctx -> spawnInfo(ctx, ""))
                                                            )
                                            )
                                            .then(
                                                    CommandManager.literal("detail")
                                                            .then(
                                                                    CommandManager.argument("detail", StringArgumentType.string())
                                                                            .executes(ctx -> spawnInfo(ctx, StringArgumentType.getString(ctx, "detail")))
                                                            )
                                            )
                            )
                            .then(
                                    CommandManager.literal("modifypool")
                                            .requires(Permissions.require("spawncontroller.modifypool", 4))
                                            .then(
                                                    CommandManager.argument("pokemon", SpeciesArgumentType.Companion.species())
                                                            .suggests((context, builder) -> {
                                                                PokemonSpecies.INSTANCE.getSpecies().forEach(species -> builder.suggest(species.getName().toLowerCase()));
                                                                return builder.buildFuture();
                                                            })
                                                            .then(
                                                                    CommandManager.argument("id", StringArgumentType.string())
                                                                            .then(
                                                                                    CommandManager.literal("bucket")
                                                                                            .then(
                                                                                                    CommandManager.argument("bucket", SpawnBucketArgumentType.Companion.spawnBucket())
                                                                                                            .executes(ctx -> {
                                                                                                                SpawnBucket bucket = SpawnBucketArgumentType.Companion.getSpawnBucket(ctx, "bucket");
                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                detail.setBucket(bucket);
                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                return 1;
                                                                                                            })
                                                                                            )
                                                                            )
                                                                            .then(
                                                                                    CommandManager.literal("weight")
                                                                                            .then(
                                                                                                    CommandManager.argument("weight", FloatArgumentType.floatArg(0.001F))
                                                                                                            .executes(ctx -> {
                                                                                                                float weight = FloatArgumentType.getFloat(ctx, "weight");
                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                detail.setWeight(weight);
                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                return 1;
                                                                                                            })
                                                                                            )
                                                                            )
                                                                            .then(
                                                                                    CommandManager.literal("levelRange")
                                                                                            .then(
                                                                                                    CommandManager.argument("min", IntegerArgumentType.integer(1))
                                                                                                            .then(
                                                                                                                    CommandManager.argument("max", IntegerArgumentType.integer(1, 100))
                                                                                                                            .executes(ctx -> {
                                                                                                                                int min = IntegerArgumentType.getInteger(ctx, "min");
                                                                                                                                int max = IntegerArgumentType.getInteger(ctx, "max");
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                if (detail instanceof PokemonSpawnDetail) {
                                                                                                                                    ((PokemonSpawnDetail) detail).setLevelRange(new IntRange(min, max));
                                                                                                                                }
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                            )
                                                                            )
                                                                            .then(
                                                                                    CommandManager.literal("condition")
                                                                                            .then(
                                                                                                    CommandManager.literal("isRaining")
                                                                                                            .then(
                                                                                                                    CommandManager.argument("boolean", StringArgumentType.string())
                                                                                                                            .suggests(this::suggestBoolean)
                                                                                                                            .executes(ctx -> {
                                                                                                                                String bool = StringArgumentType.getString(ctx, "boolean");
                                                                                                                                Boolean status = getBoolOrNull(bool);

                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getConditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    condition.setRaining(status);
                                                                                                                                }
                                                                                                                                detail.setConditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                            )
                                                                                            .then(
                                                                                                    CommandManager.literal("isThundering")
                                                                                                            .then(
                                                                                                                    CommandManager.argument("boolean", StringArgumentType.string())
                                                                                                                            .suggests(this::suggestBoolean)
                                                                                                                            .executes(ctx -> {
                                                                                                                                String bool = StringArgumentType.getString(ctx, "boolean");
                                                                                                                                Boolean status = getBoolOrNull(bool);

                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getConditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    condition.setThundering(status);
                                                                                                                                }
                                                                                                                                detail.setConditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                            )
                                                                                            .then(
                                                                                                    CommandManager.literal("isSlimeChunk")
                                                                                                            .then(
                                                                                                                    CommandManager.argument("boolean", StringArgumentType.string())
                                                                                                                            .suggests(this::suggestBoolean)
                                                                                                                            .executes(ctx -> {
                                                                                                                                String bool = StringArgumentType.getString(ctx, "boolean");
                                                                                                                                Boolean status = getBoolOrNull(bool);

                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getConditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    condition.setSlimeChunk(status);
                                                                                                                                }
                                                                                                                                detail.setConditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                            )
                                                                                            .then(
                                                                                                    CommandManager.literal("canSeeSky")
                                                                                                            .then(
                                                                                                                    CommandManager.argument("boolean", StringArgumentType.string())
                                                                                                                            .suggests(this::suggestBoolean)
                                                                                                                            .executes(ctx -> {
                                                                                                                                String bool = StringArgumentType.getString(ctx, "boolean");
                                                                                                                                Boolean status = getBoolOrNull(bool);

                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getConditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    condition.setCanSeeSky(status);
                                                                                                                                }
                                                                                                                                detail.setConditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                            )
                                                                                            .then(
                                                                                                    CommandManager.literal("fluidIsSource")
                                                                                                            .then(
                                                                                                                    CommandManager.argument("boolean", StringArgumentType.string())
                                                                                                                            .suggests(this::suggestBoolean)
                                                                                                                            .executes(ctx -> {
                                                                                                                                String bool = StringArgumentType.getString(ctx, "boolean");
                                                                                                                                Boolean status = getBoolOrNull(bool);

                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getConditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    if (condition instanceof SubmergedSpawningCondition) {
                                                                                                                                        ((SubmergedSpawningCondition) condition).setFluidIsSource(status);
                                                                                                                                    }
                                                                                                                                }
                                                                                                                                detail.setConditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                            )
                                                                                            .then(
                                                                                                    CommandManager.literal("timeRange")
                                                                                                            .then(
                                                                                                                    CommandManager.argument("time", StringArgumentType.string())
                                                                                                                            .suggests((ctx, builder) -> {
                                                                                                                                for (Map.Entry<String, TimeRange> entry : TimeRange.Companion.getTimeRanges().entrySet()) {
                                                                                                                                    builder.suggest(entry.getKey());
                                                                                                                                }
                                                                                                                                builder.suggest("null");
                                                                                                                                return builder.buildFuture();
                                                                                                                            })
                                                                                                                            .executes(ctx -> {
                                                                                                                                String timeRange = StringArgumentType.getString(ctx, "time");
                                                                                                                                TimeRange range = null;
                                                                                                                                if (!timeRange.equalsIgnoreCase("null")) {
                                                                                                                                    range = TimeRange.Companion.getTimeRanges().get(timeRange);
                                                                                                                                }
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getConditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    condition.setTimeRange(range);
                                                                                                                                }
                                                                                                                                detail.setConditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                            )
                                                                                            .then(
                                                                                                    CommandManager.literal("moonPhase")
                                                                                                            .then(
                                                                                                                    CommandManager.argument("phase", StringArgumentType.string())
                                                                                                                            .suggests((ctx, builder) -> {
                                                                                                                                for (Map.Entry<String, MoonPhaseRange> entry : MoonPhaseRange.Companion.getMoonPhaseRanges().entrySet()) {
                                                                                                                                    builder.suggest(entry.getKey());
                                                                                                                                }
                                                                                                                                builder.suggest("null");
                                                                                                                                return builder.buildFuture();
                                                                                                                            })
                                                                                                                            .executes(ctx -> {
                                                                                                                                String moonPhase = StringArgumentType.getString(ctx, "phase");
                                                                                                                                MoonPhaseRange range = null;
                                                                                                                                if (!moonPhase.equals("null")) {
                                                                                                                                    range = MoonPhaseRange.Companion.getMoonPhaseRanges().get(moonPhase);
                                                                                                                                }
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getConditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    condition.setMoonPhase(range);
                                                                                                                                }
                                                                                                                                detail.setConditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                            )
                                                                                            .then(
                                                                                                    CommandManager.literal("minLight")
                                                                                                            .then(
                                                                                                                    CommandManager.argument("level", IntegerArgumentType.integer(0, 15))
                                                                                                                            .executes(ctx -> {
                                                                                                                                int level = IntegerArgumentType.getInteger(ctx, "level");
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getConditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    condition.setMinLight(level);
                                                                                                                                }
                                                                                                                                detail.setConditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                                            .then(
                                                                                                                    CommandManager.literal("null")
                                                                                                                            .executes(ctx -> {
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getConditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    condition.setMinLight(null);
                                                                                                                                }
                                                                                                                                detail.setConditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                            )
                                                                                            .then(
                                                                                                    CommandManager.literal("maxLight")
                                                                                                            .then(
                                                                                                                    CommandManager.argument("level", IntegerArgumentType.integer(0, 15))
                                                                                                                            .executes(ctx -> {
                                                                                                                                int level = IntegerArgumentType.getInteger(ctx, "level");
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getConditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    condition.setMaxLight(level);
                                                                                                                                }
                                                                                                                                detail.setConditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                                            .then(
                                                                                                                    CommandManager.literal("null")
                                                                                                                            .executes(ctx -> {
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getConditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    condition.setMaxLight(null);
                                                                                                                                }
                                                                                                                                detail.setConditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                            )
                                                                                            .then(
                                                                                                    CommandManager.literal("minSkyLight")
                                                                                                            .then(
                                                                                                                    CommandManager.argument("level", IntegerArgumentType.integer(0, 15))
                                                                                                                            .executes(ctx -> {
                                                                                                                                int level = IntegerArgumentType.getInteger(ctx, "level");
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getConditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    condition.setMinSkyLight(level);
                                                                                                                                }
                                                                                                                                detail.setConditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                                            .then(
                                                                                                                    CommandManager.literal("null")
                                                                                                                            .executes(ctx -> {
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getConditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    condition.setMinSkyLight(null);
                                                                                                                                }
                                                                                                                                detail.setConditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                            )
                                                                                            .then(
                                                                                                    CommandManager.literal("maxSkyLight")
                                                                                                            .then(
                                                                                                                    CommandManager.argument("level", IntegerArgumentType.integer(0, 15))
                                                                                                                            .executes(ctx -> {
                                                                                                                                int level = IntegerArgumentType.getInteger(ctx, "level");
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getConditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    condition.setMaxSkyLight(level);
                                                                                                                                }
                                                                                                                                detail.setConditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                                            .then(
                                                                                                                    CommandManager.literal("null")
                                                                                                                            .executes(ctx -> {
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getConditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    condition.setMaxSkyLight(null);
                                                                                                                                }
                                                                                                                                detail.setConditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                            )
                                                                                            .then(
                                                                                                    CommandManager.literal("minHeight")
                                                                                                            .then(
                                                                                                                    CommandManager.argument("value", IntegerArgumentType.integer())
                                                                                                                            .executes(ctx -> {
                                                                                                                                int value = IntegerArgumentType.getInteger(ctx, "value");
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getConditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    if (condition instanceof AreaTypeSpawningCondition<?>) {
                                                                                                                                        ((AreaTypeSpawningCondition<?>) condition).setMinHeight(value);
                                                                                                                                    }
                                                                                                                                }
                                                                                                                                detail.setConditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                                            .then(
                                                                                                                    CommandManager.literal("null")
                                                                                                                            .executes(ctx -> {
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getConditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    if (condition instanceof AreaTypeSpawningCondition<?>) {
                                                                                                                                        ((AreaTypeSpawningCondition<?>) condition).setMinHeight(null);
                                                                                                                                    }
                                                                                                                                }
                                                                                                                                detail.setConditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                            )
                                                                                            .then(
                                                                                                    CommandManager.literal("maxHeight")
                                                                                                            .then(
                                                                                                                    CommandManager.argument("value", IntegerArgumentType.integer())
                                                                                                                            .executes(ctx -> {
                                                                                                                                int value = IntegerArgumentType.getInteger(ctx, "value");
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getConditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    if (condition instanceof AreaTypeSpawningCondition<?>) {
                                                                                                                                        ((AreaTypeSpawningCondition<?>) condition).setMaxHeight(value);
                                                                                                                                    }
                                                                                                                                }
                                                                                                                                detail.setConditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                                            .then(
                                                                                                                    CommandManager.literal("null")
                                                                                                                            .executes(ctx -> {
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getConditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    if (condition instanceof AreaTypeSpawningCondition<?>) {
                                                                                                                                        ((AreaTypeSpawningCondition<?>) condition).setMaxHeight(null);
                                                                                                                                    }
                                                                                                                                }
                                                                                                                                detail.setConditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                            )
                                                                                            .then(
                                                                                                    CommandManager.literal("minLureLevel")
                                                                                                            .then(
                                                                                                                    CommandManager.argument("value", IntegerArgumentType.integer())
                                                                                                                            .executes(ctx -> {
                                                                                                                                int value = IntegerArgumentType.getInteger(ctx, "value");
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getConditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    if (condition instanceof FishingSpawningCondition) {
                                                                                                                                        ((FishingSpawningCondition) condition).setMinLureLevel(value);
                                                                                                                                    }
                                                                                                                                }
                                                                                                                                detail.setConditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                                            .then(
                                                                                                                    CommandManager.literal("null")
                                                                                                                            .executes(ctx -> {
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getConditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    if (condition instanceof FishingSpawningCondition) {
                                                                                                                                        ((FishingSpawningCondition) condition).setMinLureLevel(null);
                                                                                                                                    }
                                                                                                                                }
                                                                                                                                detail.setConditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                            )
                                                                                            .then(
                                                                                                    CommandManager.literal("maxLureLevel")
                                                                                                            .then(
                                                                                                                    CommandManager.argument("value", IntegerArgumentType.integer())
                                                                                                                            .executes(ctx -> {
                                                                                                                                int value = IntegerArgumentType.getInteger(ctx, "value");
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getConditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    if (condition instanceof FishingSpawningCondition) {
                                                                                                                                        ((FishingSpawningCondition) condition).setMaxLureLevel(value);
                                                                                                                                    }
                                                                                                                                }
                                                                                                                                detail.setConditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                                            .then(
                                                                                                                    CommandManager.literal("null")
                                                                                                                            .executes(ctx -> {
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getConditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    if (condition instanceof FishingSpawningCondition) {
                                                                                                                                        ((FishingSpawningCondition) condition).setMaxLureLevel(null);
                                                                                                                                    }
                                                                                                                                }
                                                                                                                                detail.setConditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                            )
                                                                                            .then(
                                                                                                    CommandManager.literal("minDepthSurface")
                                                                                                            .then(
                                                                                                                    CommandManager.argument("value", IntegerArgumentType.integer())
                                                                                                                            .executes(ctx -> {
                                                                                                                                int value = IntegerArgumentType.getInteger(ctx, "value");
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getConditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    if (condition instanceof SurfaceSpawningCondition) {
                                                                                                                                        ((SurfaceSpawningCondition) condition).setMinDepth(value);
                                                                                                                                    }
                                                                                                                                }
                                                                                                                                detail.setConditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                                            .then(
                                                                                                                    CommandManager.literal("null")
                                                                                                                            .executes(ctx -> {
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getConditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    if (condition instanceof SurfaceSpawningCondition) {
                                                                                                                                        ((SurfaceSpawningCondition) condition).setMinDepth(null);
                                                                                                                                    }
                                                                                                                                }
                                                                                                                                detail.setConditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                            )
                                                                                            .then(
                                                                                                    CommandManager.literal("maxDepthSurface")
                                                                                                            .then(
                                                                                                                    CommandManager.argument("value", IntegerArgumentType.integer())
                                                                                                                            .executes(ctx -> {
                                                                                                                                int value = IntegerArgumentType.getInteger(ctx, "value");
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getConditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    if (condition instanceof SurfaceSpawningCondition) {
                                                                                                                                        ((SurfaceSpawningCondition) condition).setMaxDepth(value);
                                                                                                                                    }
                                                                                                                                }
                                                                                                                                detail.setConditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                                            .then(
                                                                                                                    CommandManager.literal("null")
                                                                                                                            .executes(ctx -> {
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getConditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    if (condition instanceof SurfaceSpawningCondition) {
                                                                                                                                        ((SurfaceSpawningCondition) condition).setMaxDepth(null);
                                                                                                                                    }
                                                                                                                                }
                                                                                                                                detail.setConditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                            )
                                                                                            .then(
                                                                                                    CommandManager.literal("minDepthSubmerged")
                                                                                                            .then(
                                                                                                                    CommandManager.argument("value", IntegerArgumentType.integer())
                                                                                                                            .executes(ctx -> {
                                                                                                                                int value = IntegerArgumentType.getInteger(ctx, "value");
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getConditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    if (condition instanceof SubmergedSpawningCondition) {
                                                                                                                                        ((SubmergedSpawningCondition) condition).setMinDepth(value);
                                                                                                                                    }
                                                                                                                                }
                                                                                                                                detail.setConditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                                            .then(
                                                                                                                    CommandManager.literal("null")
                                                                                                                            .executes(ctx -> {
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getConditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    if (condition instanceof SubmergedSpawningCondition) {
                                                                                                                                        ((SubmergedSpawningCondition) condition).setMinDepth(null);
                                                                                                                                    }
                                                                                                                                }
                                                                                                                                detail.setConditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                            )
                                                                                            .then(
                                                                                                    CommandManager.literal("maxDepthSubmerged")
                                                                                                            .then(
                                                                                                                    CommandManager.argument("value", IntegerArgumentType.integer())
                                                                                                                            .executes(ctx -> {
                                                                                                                                int value = IntegerArgumentType.getInteger(ctx, "value");
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getConditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    if (condition instanceof SubmergedSpawningCondition) {
                                                                                                                                        ((SubmergedSpawningCondition) condition).setMaxDepth(value);
                                                                                                                                    }
                                                                                                                                }
                                                                                                                                detail.setConditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                                            .then(
                                                                                                                    CommandManager.literal("null")
                                                                                                                            .executes(ctx -> {
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getConditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    if (condition instanceof SubmergedSpawningCondition) {
                                                                                                                                        ((SubmergedSpawningCondition) condition).setMaxDepth(null);
                                                                                                                                    }
                                                                                                                                }
                                                                                                                                detail.setConditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                            )
                                                                                            .then(
                                                                                                    CommandManager.literal("minX")
                                                                                                            .then(
                                                                                                                    CommandManager.argument("value", FloatArgumentType.floatArg())
                                                                                                                            .executes(ctx -> {
                                                                                                                                float value = FloatArgumentType.getFloat(ctx, "value");
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getConditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    condition.setMinX(value);
                                                                                                                                }
                                                                                                                                detail.setConditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                                            .then(
                                                                                                                    CommandManager.literal("null")
                                                                                                                            .executes(ctx -> {
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getConditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    condition.setMinX(null);
                                                                                                                                }
                                                                                                                                detail.setConditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                            )
                                                                                            .then(
                                                                                                    CommandManager.literal("maxX")
                                                                                                            .then(
                                                                                                                    CommandManager.argument("value", FloatArgumentType.floatArg())
                                                                                                                            .executes(ctx -> {
                                                                                                                                float value = FloatArgumentType.getFloat(ctx, "value");
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getConditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    condition.setMaxX(value);
                                                                                                                                }
                                                                                                                                detail.setConditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                                            .then(
                                                                                                                    CommandManager.literal("null")
                                                                                                                            .executes(ctx -> {
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getConditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    condition.setMaxX(null);
                                                                                                                                }
                                                                                                                                detail.setConditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                            )
                                                                                            .then(
                                                                                                    CommandManager.literal("minY")
                                                                                                            .then(
                                                                                                                    CommandManager.argument("value", FloatArgumentType.floatArg())
                                                                                                                            .executes(ctx -> {
                                                                                                                                float value = FloatArgumentType.getFloat(ctx, "value");
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getConditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    condition.setMinY(value);
                                                                                                                                }
                                                                                                                                detail.setConditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                                            .then(
                                                                                                                    CommandManager.literal("null")
                                                                                                                            .executes(ctx -> {
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getConditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    condition.setMinY(null);
                                                                                                                                }
                                                                                                                                detail.setConditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                            )
                                                                                            .then(
                                                                                                    CommandManager.literal("maxY")
                                                                                                            .then(
                                                                                                                    CommandManager.argument("value", FloatArgumentType.floatArg())
                                                                                                                            .executes(ctx -> {
                                                                                                                                float value = FloatArgumentType.getFloat(ctx, "value");
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getConditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    condition.setMaxY(value);
                                                                                                                                }
                                                                                                                                detail.setConditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                                            .then(
                                                                                                                    CommandManager.literal("null")
                                                                                                                            .executes(ctx -> {
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getConditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    condition.setMaxY(null);
                                                                                                                                }
                                                                                                                                detail.setConditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                            )
                                                                                            .then(
                                                                                                    CommandManager.literal("minZ")
                                                                                                            .then(
                                                                                                                    CommandManager.argument("value", FloatArgumentType.floatArg())
                                                                                                                            .executes(ctx -> {
                                                                                                                                float value = FloatArgumentType.getFloat(ctx, "value");
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getConditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    condition.setMinZ(value);
                                                                                                                                }
                                                                                                                                detail.setConditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                                            .then(
                                                                                                                    CommandManager.literal("null")
                                                                                                                            .executes(ctx -> {
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getConditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    condition.setMinZ(null);
                                                                                                                                }
                                                                                                                                detail.setConditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                            )
                                                                                            .then(
                                                                                                    CommandManager.literal("maxZ")
                                                                                                            .then(
                                                                                                                    CommandManager.argument("value", FloatArgumentType.floatArg())
                                                                                                                            .executes(ctx -> {
                                                                                                                                float value = FloatArgumentType.getFloat(ctx, "value");
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getConditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    condition.setMaxZ(value);
                                                                                                                                }
                                                                                                                                detail.setConditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                                            .then(
                                                                                                                    CommandManager.literal("null")
                                                                                                                            .executes(ctx -> {
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getConditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    condition.setMaxZ(null);
                                                                                                                                }
                                                                                                                                detail.setConditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                            )
                                                                            )
                                                                            .then(
                                                                                    CommandManager.literal("anticondition")
                                                                                            .then(
                                                                                                    CommandManager.literal("isRaining")
                                                                                                            .then(
                                                                                                                    CommandManager.argument("boolean", StringArgumentType.string())
                                                                                                                            .suggests(this::suggestBoolean)
                                                                                                                            .executes(ctx -> {
                                                                                                                                String bool = StringArgumentType.getString(ctx, "boolean");
                                                                                                                                Boolean status = getBoolOrNull(bool);

                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getAnticonditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    condition.setRaining(status);
                                                                                                                                }
                                                                                                                                detail.setAnticonditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                            )
                                                                                            .then(
                                                                                                    CommandManager.literal("isThundering")
                                                                                                            .then(
                                                                                                                    CommandManager.argument("boolean", StringArgumentType.string())
                                                                                                                            .suggests(this::suggestBoolean)
                                                                                                                            .executes(ctx -> {
                                                                                                                                String bool = StringArgumentType.getString(ctx, "boolean");
                                                                                                                                Boolean status = getBoolOrNull(bool);

                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getAnticonditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    condition.setThundering(status);
                                                                                                                                }
                                                                                                                                detail.setAnticonditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                            )
                                                                                            .then(
                                                                                                    CommandManager.literal("isSlimeChunk")
                                                                                                            .then(
                                                                                                                    CommandManager.argument("boolean", StringArgumentType.string())
                                                                                                                            .suggests(this::suggestBoolean)
                                                                                                                            .executes(ctx -> {
                                                                                                                                String bool = StringArgumentType.getString(ctx, "boolean");
                                                                                                                                Boolean status = getBoolOrNull(bool);

                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getAnticonditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    condition.setSlimeChunk(status);
                                                                                                                                }
                                                                                                                                detail.setAnticonditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                            )
                                                                                            .then(
                                                                                                    CommandManager.literal("canSeeSky")
                                                                                                            .then(
                                                                                                                    CommandManager.argument("boolean", StringArgumentType.string())
                                                                                                                            .suggests(this::suggestBoolean)
                                                                                                                            .executes(ctx -> {
                                                                                                                                String bool = StringArgumentType.getString(ctx, "boolean");
                                                                                                                                Boolean status = getBoolOrNull(bool);

                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getAnticonditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    condition.setCanSeeSky(status);
                                                                                                                                }
                                                                                                                                detail.setAnticonditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                            )
                                                                                            .then(
                                                                                                    CommandManager.literal("fluidIsSource")
                                                                                                            .then(
                                                                                                                    CommandManager.argument("boolean", StringArgumentType.string())
                                                                                                                            .suggests(this::suggestBoolean)
                                                                                                                            .executes(ctx -> {
                                                                                                                                String bool = StringArgumentType.getString(ctx, "boolean");
                                                                                                                                Boolean status = getBoolOrNull(bool);

                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getAnticonditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    if (condition instanceof SubmergedSpawningCondition) {
                                                                                                                                        ((SubmergedSpawningCondition) condition).setFluidIsSource(status);
                                                                                                                                    }
                                                                                                                                }
                                                                                                                                detail.setAnticonditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                            )
                                                                                            .then(
                                                                                                    CommandManager.literal("timeRange")
                                                                                                            .then(
                                                                                                                    CommandManager.argument("time", StringArgumentType.string())
                                                                                                                            .suggests((ctx, builder) -> {
                                                                                                                                for (Map.Entry<String, TimeRange> entry : TimeRange.Companion.getTimeRanges().entrySet()) {
                                                                                                                                    builder.suggest(entry.getKey());
                                                                                                                                }
                                                                                                                                builder.suggest("null");
                                                                                                                                return builder.buildFuture();
                                                                                                                            })
                                                                                                                            .executes(ctx -> {
                                                                                                                                String timeRange = StringArgumentType.getString(ctx, "time");
                                                                                                                                TimeRange range = null;
                                                                                                                                if (!timeRange.equalsIgnoreCase("null")) {
                                                                                                                                    range = TimeRange.Companion.getTimeRanges().get(timeRange);
                                                                                                                                }
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getAnticonditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    condition.setTimeRange(range);
                                                                                                                                }
                                                                                                                                detail.setAnticonditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                            )
                                                                                            .then(
                                                                                                    CommandManager.literal("moonPhase")
                                                                                                            .then(
                                                                                                                    CommandManager.argument("phase", StringArgumentType.string())
                                                                                                                            .suggests((ctx, builder) -> {
                                                                                                                                for (Map.Entry<String, MoonPhaseRange> entry : MoonPhaseRange.Companion.getMoonPhaseRanges().entrySet()) {
                                                                                                                                    builder.suggest(entry.getKey());
                                                                                                                                }
                                                                                                                                builder.suggest("null");
                                                                                                                                return builder.buildFuture();
                                                                                                                            })
                                                                                                                            .executes(ctx -> {
                                                                                                                                String moonPhase = StringArgumentType.getString(ctx, "phase");
                                                                                                                                MoonPhaseRange range = null;
                                                                                                                                if (!moonPhase.equals("null")) {
                                                                                                                                    range = MoonPhaseRange.Companion.getMoonPhaseRanges().get(moonPhase);
                                                                                                                                }
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getAnticonditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    condition.setMoonPhase(range);
                                                                                                                                }
                                                                                                                                detail.setAnticonditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                            )
                                                                                            .then(
                                                                                                    CommandManager.literal("minLight")
                                                                                                            .then(
                                                                                                                    CommandManager.argument("level", IntegerArgumentType.integer(0, 15))
                                                                                                                            .executes(ctx -> {
                                                                                                                                int level = IntegerArgumentType.getInteger(ctx, "level");
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getAnticonditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    condition.setMinLight(level);
                                                                                                                                }
                                                                                                                                detail.setAnticonditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                                            .then(
                                                                                                                    CommandManager.literal("null")
                                                                                                                            .executes(ctx -> {
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getAnticonditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    condition.setMinLight(null);
                                                                                                                                }
                                                                                                                                detail.setAnticonditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                            )
                                                                                            .then(
                                                                                                    CommandManager.literal("maxLight")
                                                                                                            .then(
                                                                                                                    CommandManager.argument("level", IntegerArgumentType.integer(0, 15))
                                                                                                                            .executes(ctx -> {
                                                                                                                                int level = IntegerArgumentType.getInteger(ctx, "level");
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getAnticonditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    condition.setMaxLight(level);
                                                                                                                                }
                                                                                                                                detail.setAnticonditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                                            .then(
                                                                                                                    CommandManager.literal("null")
                                                                                                                            .executes(ctx -> {
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getAnticonditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    condition.setMaxLight(null);
                                                                                                                                }
                                                                                                                                detail.setAnticonditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                            )
                                                                                            .then(
                                                                                                    CommandManager.literal("minSkyLight")
                                                                                                            .then(
                                                                                                                    CommandManager.argument("level", IntegerArgumentType.integer(0, 15))
                                                                                                                            .executes(ctx -> {
                                                                                                                                int level = IntegerArgumentType.getInteger(ctx, "level");
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getAnticonditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    condition.setMinSkyLight(level);
                                                                                                                                }
                                                                                                                                detail.setAnticonditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                                            .then(
                                                                                                                    CommandManager.literal("null")
                                                                                                                            .executes(ctx -> {
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getAnticonditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    condition.setMinSkyLight(null);
                                                                                                                                }
                                                                                                                                detail.setAnticonditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                            )
                                                                                            .then(
                                                                                                    CommandManager.literal("maxSkyLight")
                                                                                                            .then(
                                                                                                                    CommandManager.argument("level", IntegerArgumentType.integer(0, 15))
                                                                                                                            .executes(ctx -> {
                                                                                                                                int level = IntegerArgumentType.getInteger(ctx, "level");
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getAnticonditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    condition.setMaxSkyLight(level);
                                                                                                                                }
                                                                                                                                detail.setAnticonditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                                            .then(
                                                                                                                    CommandManager.literal("null")
                                                                                                                            .executes(ctx -> {
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getAnticonditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    condition.setMaxSkyLight(null);
                                                                                                                                }
                                                                                                                                detail.setAnticonditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                            )
                                                                                            .then(
                                                                                                    CommandManager.literal("minHeight")
                                                                                                            .then(
                                                                                                                    CommandManager.argument("value", IntegerArgumentType.integer())
                                                                                                                            .executes(ctx -> {
                                                                                                                                int value = IntegerArgumentType.getInteger(ctx, "value");
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getAnticonditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    if (condition instanceof AreaTypeSpawningCondition<?>) {
                                                                                                                                        ((AreaTypeSpawningCondition<?>) condition).setMinHeight(value);
                                                                                                                                    }
                                                                                                                                }
                                                                                                                                detail.setAnticonditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                                            .then(
                                                                                                                    CommandManager.literal("null")
                                                                                                                            .executes(ctx -> {
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getAnticonditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    if (condition instanceof AreaTypeSpawningCondition<?>) {
                                                                                                                                        ((AreaTypeSpawningCondition<?>) condition).setMinHeight(null);
                                                                                                                                    }
                                                                                                                                }
                                                                                                                                detail.setAnticonditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                            )
                                                                                            .then(
                                                                                                    CommandManager.literal("maxHeight")
                                                                                                            .then(
                                                                                                                    CommandManager.argument("value", IntegerArgumentType.integer())
                                                                                                                            .executes(ctx -> {
                                                                                                                                int value = IntegerArgumentType.getInteger(ctx, "value");
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getAnticonditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    if (condition instanceof AreaTypeSpawningCondition<?>) {
                                                                                                                                        ((AreaTypeSpawningCondition<?>) condition).setMaxHeight(value);
                                                                                                                                    }
                                                                                                                                }
                                                                                                                                detail.setAnticonditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                                            .then(
                                                                                                                    CommandManager.literal("null")
                                                                                                                            .executes(ctx -> {
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getAnticonditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    if (condition instanceof AreaTypeSpawningCondition<?>) {
                                                                                                                                        ((AreaTypeSpawningCondition<?>) condition).setMaxHeight(null);
                                                                                                                                    }
                                                                                                                                }
                                                                                                                                detail.setAnticonditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                            )
                                                                                            .then(
                                                                                                    CommandManager.literal("minLureLevel")
                                                                                                            .then(
                                                                                                                    CommandManager.argument("value", IntegerArgumentType.integer())
                                                                                                                            .executes(ctx -> {
                                                                                                                                int value = IntegerArgumentType.getInteger(ctx, "value");
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getAnticonditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    if (condition instanceof FishingSpawningCondition) {
                                                                                                                                        ((FishingSpawningCondition) condition).setMinLureLevel(value);
                                                                                                                                    }
                                                                                                                                }
                                                                                                                                detail.setAnticonditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                                            .then(
                                                                                                                    CommandManager.literal("null")
                                                                                                                            .executes(ctx -> {
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getAnticonditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    if (condition instanceof FishingSpawningCondition) {
                                                                                                                                        ((FishingSpawningCondition) condition).setMinLureLevel(null);
                                                                                                                                    }
                                                                                                                                }
                                                                                                                                detail.setAnticonditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                            )
                                                                                            .then(
                                                                                                    CommandManager.literal("maxLureLevel")
                                                                                                            .then(
                                                                                                                    CommandManager.argument("value", IntegerArgumentType.integer())
                                                                                                                            .executes(ctx -> {
                                                                                                                                int value = IntegerArgumentType.getInteger(ctx, "value");
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getAnticonditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    if (condition instanceof FishingSpawningCondition) {
                                                                                                                                        ((FishingSpawningCondition) condition).setMaxLureLevel(value);
                                                                                                                                    }
                                                                                                                                }
                                                                                                                                detail.setAnticonditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                                            .then(
                                                                                                                    CommandManager.literal("null")
                                                                                                                            .executes(ctx -> {
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getAnticonditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    if (condition instanceof FishingSpawningCondition) {
                                                                                                                                        ((FishingSpawningCondition) condition).setMaxLureLevel(null);
                                                                                                                                    }
                                                                                                                                }
                                                                                                                                detail.setAnticonditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                            )
                                                                                            .then(
                                                                                                    CommandManager.literal("minDepthSurface")
                                                                                                            .then(
                                                                                                                    CommandManager.argument("value", IntegerArgumentType.integer())
                                                                                                                            .executes(ctx -> {
                                                                                                                                int value = IntegerArgumentType.getInteger(ctx, "value");
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getAnticonditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    if (condition instanceof SurfaceSpawningCondition) {
                                                                                                                                        ((SurfaceSpawningCondition) condition).setMinDepth(value);
                                                                                                                                    }
                                                                                                                                }
                                                                                                                                detail.setAnticonditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                                            .then(
                                                                                                                    CommandManager.literal("null")
                                                                                                                            .executes(ctx -> {
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getAnticonditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    if (condition instanceof SurfaceSpawningCondition) {
                                                                                                                                        ((SurfaceSpawningCondition) condition).setMinDepth(null);
                                                                                                                                    }
                                                                                                                                }
                                                                                                                                detail.setAnticonditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                            )
                                                                                            .then(
                                                                                                    CommandManager.literal("maxDepthSurface")
                                                                                                            .then(
                                                                                                                    CommandManager.argument("value", IntegerArgumentType.integer())
                                                                                                                            .executes(ctx -> {
                                                                                                                                int value = IntegerArgumentType.getInteger(ctx, "value");
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getAnticonditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    if (condition instanceof SurfaceSpawningCondition) {
                                                                                                                                        ((SurfaceSpawningCondition) condition).setMaxDepth(value);
                                                                                                                                    }
                                                                                                                                }
                                                                                                                                detail.setAnticonditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                                            .then(
                                                                                                                    CommandManager.literal("null")
                                                                                                                            .executes(ctx -> {
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getAnticonditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    if (condition instanceof SurfaceSpawningCondition) {
                                                                                                                                        ((SurfaceSpawningCondition) condition).setMaxDepth(null);
                                                                                                                                    }
                                                                                                                                }
                                                                                                                                detail.setAnticonditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                            )
                                                                                            .then(
                                                                                                    CommandManager.literal("minDepthSubmerged")
                                                                                                            .then(
                                                                                                                    CommandManager.argument("value", IntegerArgumentType.integer())
                                                                                                                            .executes(ctx -> {
                                                                                                                                int value = IntegerArgumentType.getInteger(ctx, "value");
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getAnticonditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    if (condition instanceof SubmergedSpawningCondition) {
                                                                                                                                        ((SubmergedSpawningCondition) condition).setMinDepth(value);
                                                                                                                                    }
                                                                                                                                }
                                                                                                                                detail.setAnticonditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                                            .then(
                                                                                                                    CommandManager.literal("null")
                                                                                                                            .executes(ctx -> {
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getAnticonditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    if (condition instanceof SubmergedSpawningCondition) {
                                                                                                                                        ((SubmergedSpawningCondition) condition).setMinDepth(null);
                                                                                                                                    }
                                                                                                                                }
                                                                                                                                detail.setAnticonditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                            )
                                                                                            .then(
                                                                                                    CommandManager.literal("maxDepthSubmerged")
                                                                                                            .then(
                                                                                                                    CommandManager.argument("value", IntegerArgumentType.integer())
                                                                                                                            .executes(ctx -> {
                                                                                                                                int value = IntegerArgumentType.getInteger(ctx, "value");
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getAnticonditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    if (condition instanceof SubmergedSpawningCondition) {
                                                                                                                                        ((SubmergedSpawningCondition) condition).setMaxDepth(value);
                                                                                                                                    }
                                                                                                                                }
                                                                                                                                detail.setAnticonditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                                            .then(
                                                                                                                    CommandManager.literal("null")
                                                                                                                            .executes(ctx -> {
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getAnticonditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    if (condition instanceof SubmergedSpawningCondition) {
                                                                                                                                        ((SubmergedSpawningCondition) condition).setMaxDepth(null);
                                                                                                                                    }
                                                                                                                                }
                                                                                                                                detail.setAnticonditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                            )
                                                                                            .then(
                                                                                                    CommandManager.literal("minX")
                                                                                                            .then(
                                                                                                                    CommandManager.argument("value", FloatArgumentType.floatArg())
                                                                                                                            .executes(ctx -> {
                                                                                                                                float value = FloatArgumentType.getFloat(ctx, "value");
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getAnticonditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    condition.setMinX(value);
                                                                                                                                }
                                                                                                                                detail.setAnticonditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                                            .then(
                                                                                                                    CommandManager.literal("null")
                                                                                                                            .executes(ctx -> {
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getAnticonditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    condition.setMinX(null);
                                                                                                                                }
                                                                                                                                detail.setAnticonditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                            )
                                                                                            .then(
                                                                                                    CommandManager.literal("maxX")
                                                                                                            .then(
                                                                                                                    CommandManager.argument("value", FloatArgumentType.floatArg())
                                                                                                                            .executes(ctx -> {
                                                                                                                                float value = FloatArgumentType.getFloat(ctx, "value");
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getAnticonditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    condition.setMaxX(value);
                                                                                                                                }
                                                                                                                                detail.setAnticonditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                                            .then(
                                                                                                                    CommandManager.literal("null")
                                                                                                                            .executes(ctx -> {
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getAnticonditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    condition.setMaxX(null);
                                                                                                                                }
                                                                                                                                detail.setAnticonditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                            )
                                                                                            .then(
                                                                                                    CommandManager.literal("minY")
                                                                                                            .then(
                                                                                                                    CommandManager.argument("value", FloatArgumentType.floatArg())
                                                                                                                            .executes(ctx -> {
                                                                                                                                float value = FloatArgumentType.getFloat(ctx, "value");
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getAnticonditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    condition.setMinY(value);
                                                                                                                                }
                                                                                                                                detail.setAnticonditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                                            .then(
                                                                                                                    CommandManager.literal("null")
                                                                                                                            .executes(ctx -> {
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getAnticonditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    condition.setMinY(null);
                                                                                                                                }
                                                                                                                                detail.setAnticonditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                            )
                                                                                            .then(
                                                                                                    CommandManager.literal("maxY")
                                                                                                            .then(
                                                                                                                    CommandManager.argument("value", FloatArgumentType.floatArg())
                                                                                                                            .executes(ctx -> {
                                                                                                                                float value = FloatArgumentType.getFloat(ctx, "value");
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getAnticonditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    condition.setMaxY(value);
                                                                                                                                }
                                                                                                                                detail.setAnticonditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                                            .then(
                                                                                                                    CommandManager.literal("null")
                                                                                                                            .executes(ctx -> {
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getAnticonditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    condition.setMaxY(null);
                                                                                                                                }
                                                                                                                                detail.setAnticonditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                            )
                                                                                            .then(
                                                                                                    CommandManager.literal("minZ")
                                                                                                            .then(
                                                                                                                    CommandManager.argument("value", FloatArgumentType.floatArg())
                                                                                                                            .executes(ctx -> {
                                                                                                                                float value = FloatArgumentType.getFloat(ctx, "value");
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getAnticonditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    condition.setMinZ(value);
                                                                                                                                }
                                                                                                                                detail.setAnticonditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                                            .then(
                                                                                                                    CommandManager.literal("null")
                                                                                                                            .executes(ctx -> {
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getAnticonditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    condition.setMinZ(null);
                                                                                                                                }
                                                                                                                                detail.setAnticonditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                            )
                                                                                            .then(
                                                                                                    CommandManager.literal("maxZ")
                                                                                                            .then(
                                                                                                                    CommandManager.argument("value", FloatArgumentType.floatArg())
                                                                                                                            .executes(ctx -> {
                                                                                                                                float value = FloatArgumentType.getFloat(ctx, "value");
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getAnticonditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    condition.setMaxZ(value);
                                                                                                                                }
                                                                                                                                detail.setAnticonditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                                            .then(
                                                                                                                    CommandManager.literal("null")
                                                                                                                            .executes(ctx -> {
                                                                                                                                SpawnDetail detail = getSpawnDetail(ctx);
                                                                                                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getAnticonditions());
                                                                                                                                for (SpawningCondition<?> condition : conditions) {
                                                                                                                                    condition.setMaxZ(null);
                                                                                                                                }
                                                                                                                                detail.setAnticonditions(conditions);
                                                                                                                                updateSpawnPool(ctx, detail);
                                                                                                                                return 1;
                                                                                                                            })
                                                                                                            )
                                                                                            )
                                                                            )
                                                            )
                                            )
                            )
            );
        });

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

    private static @Nullable Boolean getBoolOrNull(String bool) {
        Boolean boolOrNull;

        if (!bool.equals("null")) {
            boolOrNull = bool.equalsIgnoreCase("true");
        } else {
            boolOrNull = null;
        }
        return boolOrNull;
    }

    public CompletableFuture<Suggestions> suggestBoolean(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        builder.suggest("true");
        builder.suggest("false");
        builder.suggest("null");
        return builder.buildFuture();
    }

    public void updateSpawnPool(CommandContext<ServerCommandSource> ctx, SpawnDetail detail) {
        Species species = SpeciesArgumentType.Companion.getPokemon(ctx, "pokemon");
        String id = StringArgumentType.getString(ctx, "id");
        List<SpawnDetail> spawnDetails = CobblemonSpawnPools.WORLD_SPAWN_POOL.getDetails();
        int index = 0;
        for (SpawnDetail spawnDetail : spawnDetails) {
            if (spawnDetail instanceof PokemonSpawnDetail pokemonSpawnDetail) {
                if (pokemonSpawnDetail.getPokemon().getSpecies() != null && pokemonSpawnDetail.getPokemon().getSpecies().equalsIgnoreCase(species.getName())) {
                    if (pokemonSpawnDetail.getId().equalsIgnoreCase(id)) {
                        break;
                    }
                }
            }
            index++;
        }
        CobblemonSpawnPools.WORLD_SPAWN_POOL.getDetails().set(index, detail);
        CobblemonSpawnPools.WORLD_SPAWN_POOL.precalculate();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            CobblemonSpawnPools.WORLD_SPAWN_POOL.sync(player);
        }
    }
    
    public SpawnDetail getSpawnDetail(CommandContext<ServerCommandSource> ctx) {
        Species species = SpeciesArgumentType.Companion.getPokemon(ctx, "pokemon");
        String id = StringArgumentType.getString(ctx, "id");
        List<SpawnDetail> spawnDetails = CobblemonSpawnPools.WORLD_SPAWN_POOL.getDetails();
        for (SpawnDetail spawnDetail : spawnDetails) {
            if (spawnDetail instanceof PokemonSpawnDetail pokemonSpawnDetail) {
                if (pokemonSpawnDetail.getPokemon().getSpecies() != null && pokemonSpawnDetail.getPokemon().getSpecies().equalsIgnoreCase(species.getName())) {
                    if (pokemonSpawnDetail.getId().equalsIgnoreCase(id)) {
                        return spawnDetail;
                    }
                }
            }
        }
        return null;
    }

    public int spawnInfo(CommandContext<ServerCommandSource> ctx, String detailID) {
        List<SpawnDetail> spawnDetails = CobblemonSpawnPools.WORLD_SPAWN_POOL.getDetails();

        for (SpawnDetail spawnDetail : spawnDetails) {
            if (detailID.isBlank()) {
                Species species = SpeciesArgumentType.Companion.getPokemon(ctx, "pokemon");
                if (spawnDetail.getId().substring(0, spawnDetail.getId().indexOf("-")).equalsIgnoreCase(species.getName())) {
                    ctx.getSource().sendMessage(Text.literal("Spawn Detail ID: " + spawnDetail.getId()).styled(style -> style
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Click to view spawn details!")))
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/spawncontroller spawn-info detail " + spawnDetail.getId()))
                    ));
                }
            }
            else {
                if (spawnDetail.getId().equalsIgnoreCase(detailID)) {
                    ctx.getSource().sendMessage(Text.literal("Spawn Detail ID: " + spawnDetail.getId()));

                    ctx.getSource().sendMessage(Text.literal("Labels:"));
                    for (String label : spawnDetail.getLabels()) {
                        ctx.getSource().sendMessage(Text.literal(" - " + label));
                    }

                    if (spawnDetail instanceof PokemonSpawnDetail pokemonDetail) {
                        if (pokemonDetail.getLevelRange() != null) {
                            ctx.getSource().sendMessage(Text.literal("Level Range: " + pokemonDetail.getLevelRange().getStart() + " - " + pokemonDetail.getLevelRange().getEndInclusive()));
                        }
                    }

                    ctx.getSource().sendMessage(Text.literal("Context: " + spawnDetail.getContext().getName()));
                    ctx.getSource().sendMessage(Text.literal("Context Weight: " + spawnDetail.getContext().getWeight()));
                    ctx.getSource().sendMessage(Text.literal("Weight: " + spawnDetail.getWeight()));

                    if (!spawnDetail.getWeightMultipliers().isEmpty()) {
                        for (WeightMultiplier weight : spawnDetail.getWeightMultipliers()) {
                            ctx.getSource().sendMessage(Text.literal("Weight Multiplier: " + weight.getMultiplier()));

                            if (!weight.getConditions().isEmpty()) {
                                ctx.getSource().sendMessage(Text.literal("Weight Multiplier Conditions:"));
                                for (SpawningCondition<?> condition : weight.getConditions()) {
                                    showConditions(ctx.getSource(), condition);
                                }
                            }

                            if (!weight.getAnticonditions().isEmpty()) {
                                ctx.getSource().sendMessage(Text.literal("Weight Multiplier Anti-Conditions:"));
                                for (SpawningCondition<?> condition : weight.getAnticonditions()) {
                                    showConditions(ctx.getSource(), condition);
                                }
                            }
                        }
                    }

                    ctx.getSource().sendMessage(Text.literal("Bucket: " + spawnDetail.getBucket().getName()));

                    if (spawnDetail.getCompositeCondition() != null) {
                        if (!spawnDetail.getCompositeCondition().getConditions().isEmpty()) {
                            ctx.getSource().sendMessage(Text.literal("Composite Conditions:"));
                            for (SpawningCondition<?> condition : spawnDetail.getCompositeCondition().getConditions()) {
                                showConditions(ctx.getSource(), condition);
                            }
                        }

                        if (!spawnDetail.getCompositeCondition().getAnticonditions().isEmpty()) {
                            ctx.getSource().sendMessage(Text.literal("Composite Anti-Conditions:"));
                            for (SpawningCondition<?> condition : spawnDetail.getCompositeCondition().getAnticonditions()) {
                                showConditions(ctx.getSource(), condition);
                            }
                        }
                    }

                    if (!spawnDetail.getConditions().isEmpty()) {
                        ctx.getSource().sendMessage(Text.literal("Conditions:"));
                        for (SpawningCondition<?> condition : spawnDetail.getConditions()) {
                            showConditions(ctx.getSource(), condition);
                        }
                    }

                    if (!spawnDetail.getAnticonditions().isEmpty()) {
                        ctx.getSource().sendMessage(Text.literal("Anti-Conditions:"));
                        for (SpawningCondition<?> condition : spawnDetail.getAnticonditions()) {
                            showConditions(ctx.getSource(), condition);
                        }
                    }
                }
            }
        }

        return 1;
    }
    
    public void showConditions(ServerCommandSource source, SpawningCondition<?> condition) {
        if (condition.getDimensions() != null) {
            source.sendMessage(Text.literal(" - Dimensions:"));
            for (Identifier dimension : condition.getDimensions()) {
                source.sendMessage(Text.literal(" -- " + dimension.getNamespace() + ":" + dimension.getPath()));
            }
        }

        if (condition.getBiomes() != null) {
            source.sendMessage(Text.literal(" - Biomes:"));
            for (RegistryLikeCondition<Biome> biome : condition.getBiomes()) {
                for (Biome biomeKey : server.getRegistryManager().get(RegistryKeys.BIOME).stream().toList()) {
                    if (biome.fits(biomeKey, server.getRegistryManager().get(RegistryKeys.BIOME))) {
                        source.sendMessage(Text.literal(" -- " + server.getRegistryManager().get(RegistryKeys.BIOME).getEntry(biomeKey).getIdAsString()));
                    }
                }
            }
        }

        if (condition.getMoonPhase() != null) {
            source.sendMessage(Text.literal(" - Moon Phase: " + condition.getMoonPhase()));
        }

        if (condition.getCanSeeSky() != null) {
            source.sendMessage(Text.literal(" - Can See Sky: " + condition.getCanSeeSky()));
        }

        if (condition.getMinX() != null) {
            source.sendMessage(Text.literal(" - Min X: " + condition.getMinX()));
        }

        if (condition.getMaxX() != null) {
            source.sendMessage(Text.literal(" - Max X: " + condition.getMaxX()));
        }

        if (condition.getMinY() != null) {
            source.sendMessage(Text.literal(" - Min Y: " + condition.getMinY()));
        }

        if (condition.getMaxY() != null) {
            source.sendMessage(Text.literal(" - Max Y: " + condition.getMaxY()));
        }

        if (condition.getMinZ() != null) {
            source.sendMessage(Text.literal(" - Min Z: " + condition.getMinZ()));
        }

        if (condition.getMaxZ() != null) {
            source.sendMessage(Text.literal(" - Max Z: " + condition.getMaxZ()));
        }

        if (condition.getMinLight() != null) {
            source.sendMessage(Text.literal(" - Min Light: " + condition.getMinLight()));
        }

        if (condition.getMaxLight() != null) {
            source.sendMessage(Text.literal(" - Max Light: " + condition.getMaxLight()));
        }

        if (condition.isRaining() != null) {
            source.sendMessage(Text.literal(" - Raining: " + condition.isRaining()));
        }

        if (condition.isThundering() != null) {
            source.sendMessage(Text.literal(" - Thundering: " + condition.isThundering()));
        }

        if (condition.getTimeRange() != null) {
            source.sendMessage(Text.literal(" - Time Range: " + condition.getTimeRange().getRanges().getFirst().getStart() + " - " + condition.getTimeRange().getRanges().getFirst().getEndInclusive()));
        }

        if (condition.getStructures() != null) {
            source.sendMessage(Text.literal(" - Structures:"));
            for (Either<Identifier, TagKey<Structure>> structure : condition.getStructures()) {
                if (structure.left() != null && structure.left().isPresent()) {
                    source.sendMessage(Text.literal(" -- " + structure.left().orElseThrow().getNamespace() + ":" + structure.left().orElseThrow().getPath()));
                } else if (structure.right() != null && structure.right().isPresent()) {
                    source.sendMessage(Text.literal(" -- " + structure.right().orElseThrow().id().getNamespace() + ":" + structure.right().orElseThrow().id().getPath()));
                }
            }
        }

        if (condition.isSlimeChunk() != null) {
            source.sendMessage(Text.literal(" - Slime Chunk: " + condition.isSlimeChunk()));
        }

        if (condition instanceof AreaTypeSpawningCondition<?> areaCondition) {
            if (areaCondition.getMinHeight() != null) {
                source.sendMessage(Text.literal(" - Min Height: " + areaCondition.getMinHeight()));
            }
            if (areaCondition.getMaxHeight() != null) {
                source.sendMessage(Text.literal(" - Max Height: " + areaCondition.getMaxHeight()));
            }
        }

        if (condition instanceof GroundedTypeSpawningCondition<?> groundedCondition) {
            if (groundedCondition.getNeededBaseBlocks() != null) {
                source.sendMessage(Text.literal(" - Needed Grounded Base Blocks:"));
                source.sendMessage(Text.literal(" -- Currently Too Long To Display"));
//                for (RegistryLikeCondition<Block> block : groundedCondition.getNeededBaseBlocks()) {
//                    for (Block key : mcServer.getRegistryManager().get(RegistryKeys.BLOCK).stream().toList()) {
//                        if (block.fits(key, mcServer.getRegistryManager().get(RegistryKeys.BLOCK))) {
//                            source.sendMessage(Text.literal(" -- " + mcServer.getRegistryManager().get(RegistryKeys.BLOCK).getEntry(key).getIdAsString()));
//                        }
//                    }
//                }
            }
        }

        if (condition instanceof SubmergedTypeSpawningCondition<?> submergedCondition) {
            if (submergedCondition.getMinDepth() != null) {
                source.sendMessage(Text.literal(" - Min Submerged Depth: " + submergedCondition.getMinDepth()));
            }

            if (submergedCondition.getMaxDepth() != null) {
                source.sendMessage(Text.literal(" - Max Submerged Depth: " + submergedCondition.getMaxDepth()));
            }

            if (submergedCondition.getFluidIsSource() != null) {
                source.sendMessage(Text.literal(" - Fluid Is Source: " + submergedCondition.getFluidIsSource()));
            }

            if (submergedCondition.getFluid() != null) {
                for (Fluid key : server.getRegistryManager().get(RegistryKeys.FLUID).stream().toList()) {
                    if (submergedCondition.getFluid().fits(key, server.getRegistryManager().get(RegistryKeys.FLUID))) {
                        source.sendMessage(Text.literal(" - Submerged Fluid: " + server.getRegistryManager().get(RegistryKeys.FLUID).getEntry(key).getIdAsString()));
                    }
                }
            }
        }

        if (condition instanceof SurfaceTypeSpawningCondition<?> surfaceCondition) {
            if (surfaceCondition.getMinDepth() != null) {
                source.sendMessage(Text.literal(" - Min Surface Depth: " + surfaceCondition.getMinDepth()));
            }

            if (surfaceCondition.getMaxDepth() != null) {
                source.sendMessage(Text.literal(" - Max Surface Depth: " + surfaceCondition.getMaxDepth()));
            }

            if (surfaceCondition.getFluid() != null) {
                for (Fluid key : server.getRegistryManager().get(RegistryKeys.FLUID).stream().toList()) {
                    if (surfaceCondition.getFluid().fits(key, server.getRegistryManager().get(RegistryKeys.FLUID))) {
                        source.sendMessage(Text.literal(" - Submerged Fluid: " + server.getRegistryManager().get(RegistryKeys.FLUID).getEntry(key).getIdAsString()));
                    }
                }
            }
        }

        if (condition instanceof SeafloorTypeSpawningCondition<?> seafloorCondition) {
            if (seafloorCondition.getNeededBaseBlocks() != null) {
                source.sendMessage(Text.literal(" - Needed Sea Floor Base Blocks:"));
                source.sendMessage(Text.literal(" -- Currently Too Long To Display"));
//                for (RegistryLikeCondition<Block> block : seafloorCondition.getNeededBaseBlocks()) {
//                    for (Block key : mcServer.getRegistryManager().get(RegistryKeys.BLOCK).stream().toList()) {
//                        if (block.fits(key, mcServer.getRegistryManager().get(RegistryKeys.BLOCK))) {
//                            source.sendMessage(Text.literal(" -- " + mcServer.getRegistryManager().get(RegistryKeys.BLOCK).getEntry(key).getIdAsString()));
//                        }
//                    }
//                }
            }
        }

        if (condition instanceof FishingSpawningCondition fishingCondition) {
            if (fishingCondition.getRod() != null) {
                for (Item key : server.getRegistryManager().get(RegistryKeys.ITEM).stream().toList()) {
                    if (fishingCondition.getRod().fits(key, server.getRegistryManager().get(RegistryKeys.ITEM))) {
                        source.sendMessage(Text.literal(" - Rod: " + server.getRegistryManager().get(RegistryKeys.ITEM).getEntry(key).getIdAsString()));
                    }
                }
            }

            if (fishingCondition.getNeededNearbyBlocks() != null) {
                source.sendMessage(Text.literal(" - Fishing Needed Nearby Blocks:"));
                source.sendMessage(Text.literal(" -- Currently Too Long To Display"));
//                for (RegistryLikeCondition<Block> block : fishingCondition.getNeededNearbyBlocks()) {
//                    for (Block key : mcServer.getRegistryManager().get(RegistryKeys.BLOCK).stream().toList()) {
//                        if (block.fits(key, mcServer.getRegistryManager().get(RegistryKeys.BLOCK))) {
//                            source.sendMessage(Text.literal(" -- " + mcServer.getRegistryManager().get(RegistryKeys.BLOCK).getEntry(key).getIdAsString()));
//                        }
//                    }
//                }
            }

            if (fishingCondition.getMinLureLevel() != null) {
                source.sendMessage(Text.literal(" - Min Lure Level: " + fishingCondition.getMinLureLevel()));
            }

            if (fishingCondition.getMaxLureLevel() != null) {
                source.sendMessage(Text.literal(" - Max Lure Level: " + fishingCondition.getMaxLureLevel()));
            }

            if (fishingCondition.getBait() != null) {
                source.sendMessage(Text.literal(" - Bait: " + fishingCondition.getBait().getNamespace() + ":" + fishingCondition.getBait().getPath()));
            }

            if (fishingCondition.getRodType() != null) {
                source.sendMessage(Text.literal(" - Rod Type: " + fishingCondition.getRodType().getNamespace() + ":" + fishingCondition.getRodType().getPath()));
            }
        }
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
