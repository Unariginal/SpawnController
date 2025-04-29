package me.unariginal.spawncontroller.commands.modifypool;

import com.cobblemon.mod.common.api.spawning.MoonPhaseRange;
import com.cobblemon.mod.common.api.spawning.TimeRange;
import com.cobblemon.mod.common.api.spawning.condition.SpawningCondition;
import com.cobblemon.mod.common.api.spawning.condition.SubmergedSpawningCondition;
import com.cobblemon.mod.common.api.spawning.detail.SpawnDetail;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.unariginal.spawncontroller.Utils.SpawnerUtils;
import me.unariginal.spawncontroller.commands.suggestions.BooleanSuggestion;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.injection.struct.InjectorGroupInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Condition extends LiteralArgumentBuilder<ServerCommandSource> {
    public Condition(String literal) {
        super(literal);
        then(
                CommandManager.literal("isRaining")
                        .then(
                                CommandManager.argument("boolean", StringArgumentType.string())
                                        .suggests(new BooleanSuggestion())
                                        .executes(ctx -> {
                                            String boolString = StringArgumentType.getString(ctx, "boolean");
                                            Boolean bool = getBoolOrNull(boolString);
                                            SpawnDetail detail = SpawnerUtils.getSpawnDetail(ctx);
                                            if (detail == null) {
                                                return 0;
                                            }
                                            if (literal.equalsIgnoreCase("condition")) {
                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getConditions());
                                                for (SpawningCondition<?> condition : conditions) {
                                                    condition.setRaining(bool);
                                                }
                                                detail.setConditions(conditions);
                                            } else if (literal.equalsIgnoreCase("anticondition")) {
                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getAnticonditions());
                                                for (SpawningCondition<?> condition : conditions) {
                                                    condition.setRaining(bool);
                                                }
                                                detail.setAnticonditions(conditions);
                                            }
                                            SpawnerUtils.updateSpawnPool(ctx, detail);
                                            return 1;
                                        })
                        )
        );
        then(
                CommandManager.literal("isThundering")
                        .then(
                                CommandManager.argument("boolean", StringArgumentType.string())
                                        .suggests(new BooleanSuggestion())
                                        .executes(ctx -> {
                                            String boolString = StringArgumentType.getString(ctx, "boolean");
                                            Boolean bool = getBoolOrNull(boolString);
                                            SpawnDetail detail = SpawnerUtils.getSpawnDetail(ctx);
                                            if (detail == null) {
                                                return 0;
                                            }
                                            if (literal.equalsIgnoreCase("condition")) {
                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getConditions());
                                                for (SpawningCondition<?> condition : conditions) {
                                                    condition.setThundering(bool);
                                                }
                                                detail.setConditions(conditions);
                                            } else if (literal.equalsIgnoreCase("anticondition")) {
                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getAnticonditions());
                                                for (SpawningCondition<?> condition : conditions) {
                                                    condition.setThundering(bool);
                                                }
                                                detail.setAnticonditions(conditions);
                                            }
                                            SpawnerUtils.updateSpawnPool(ctx, detail);
                                            return 1;
                                        })
                        )
        );
        then(
                CommandManager.literal("isSlimeChunk")
                        .then(
                                CommandManager.argument("boolean", StringArgumentType.string())
                                        .suggests(new BooleanSuggestion())
                                        .executes(ctx -> {
                                            String boolString = StringArgumentType.getString(ctx, "boolean");
                                            Boolean bool = getBoolOrNull(boolString);
                                            SpawnDetail detail = SpawnerUtils.getSpawnDetail(ctx);
                                            if (detail == null) {
                                                return 0;
                                            }
                                            if (literal.equalsIgnoreCase("condition")) {
                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getConditions());
                                                for (SpawningCondition<?> condition : conditions) {
                                                    condition.setSlimeChunk(bool);
                                                }
                                                detail.setConditions(conditions);
                                            } else if (literal.equalsIgnoreCase("anticondition")) {
                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getAnticonditions());
                                                for (SpawningCondition<?> condition : conditions) {
                                                    condition.setSlimeChunk(bool);
                                                }
                                                detail.setAnticonditions(conditions);
                                            }
                                            SpawnerUtils.updateSpawnPool(ctx, detail);
                                            return 1;
                                        })
                        )
        );
        then(
                CommandManager.literal("canSeeSky")
                        .then(
                                CommandManager.argument("boolean", StringArgumentType.string())
                                        .suggests(new BooleanSuggestion())
                                        .executes(ctx -> {
                                            String boolString = StringArgumentType.getString(ctx, "boolean");
                                            Boolean bool = getBoolOrNull(boolString);
                                            SpawnDetail detail = SpawnerUtils.getSpawnDetail(ctx);
                                            if (detail == null) {
                                                return 0;
                                            }
                                            if (literal.equalsIgnoreCase("condition")) {
                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getConditions());
                                                for (SpawningCondition<?> condition : conditions) {
                                                    condition.setCanSeeSky(bool);
                                                }
                                                detail.setConditions(conditions);
                                            } else if (literal.equalsIgnoreCase("anticondition")) {
                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getAnticonditions());
                                                for (SpawningCondition<?> condition : conditions) {
                                                    condition.setCanSeeSky(bool);
                                                }
                                                detail.setAnticonditions(conditions);
                                            }
                                            SpawnerUtils.updateSpawnPool(ctx, detail);
                                            return 1;
                                        })
                        )
        );
        then(
                CommandManager.literal("fluidIsSource")
                        .then(
                                CommandManager.argument("boolean", StringArgumentType.string())
                                        .suggests(new BooleanSuggestion())
                                        .executes(ctx -> {
                                            String boolString = StringArgumentType.getString(ctx, "boolean");
                                            Boolean bool = getBoolOrNull(boolString);
                                            SpawnDetail detail = SpawnerUtils.getSpawnDetail(ctx);
                                            if (detail == null) {
                                                return 0;
                                            }
                                            if (literal.equalsIgnoreCase("condition")) {
                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getConditions());
                                                for (SpawningCondition<?> condition : conditions) {
                                                    if (condition instanceof SubmergedSpawningCondition submergedSpawningCondition) {
                                                        submergedSpawningCondition.setFluidIsSource(bool);
                                                    }
                                                }
                                                detail.setConditions(conditions);
                                            } else if (literal.equalsIgnoreCase("anticondition")) {
                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getAnticonditions());
                                                for (SpawningCondition<?> condition : conditions) {
                                                    if (condition instanceof SubmergedSpawningCondition submergedSpawningCondition) {
                                                        submergedSpawningCondition.setFluidIsSource(bool);
                                                    }
                                                }
                                                detail.setAnticonditions(conditions);
                                            }
                                            SpawnerUtils.updateSpawnPool(ctx, detail);
                                            return 1;
                                        })
                        )
        );
        then(
                CommandManager.literal("timeRange")
                        .then(
                                CommandManager.argument("time", StringArgumentType.string())
                                        .suggests((commandContext, suggestionsBuilder) -> {
                                            for (Map.Entry<String, TimeRange> entry : TimeRange.Companion.getTimeRanges().entrySet()) {
                                                suggestionsBuilder.suggest("\"" + entry.getKey() + "\"");
                                            }
                                            suggestionsBuilder.suggest("null");
                                            return suggestionsBuilder.buildFuture();
                                        })
                                        .executes(ctx -> {
                                            String timeString = StringArgumentType.getString(ctx, "time");
                                            TimeRange range = null;
                                            if (!timeString.equals("null")) {
                                                range = TimeRange.Companion.getTimeRanges().get(timeString);
                                            }
                                            SpawnDetail detail = SpawnerUtils.getSpawnDetail(ctx);
                                            if (detail == null) {
                                                return 0;
                                            }
                                            if (literal.equalsIgnoreCase("condition")) {
                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getConditions());
                                                for (SpawningCondition<?> condition : conditions) {
                                                    condition.setTimeRange(range);
                                                }
                                                detail.setConditions(conditions);
                                            } else if (literal.equalsIgnoreCase("anticondition")) {
                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getAnticonditions());
                                                for (SpawningCondition<?> condition : conditions) {
                                                    condition.setTimeRange(range);
                                                }
                                                detail.setAnticonditions(conditions);
                                            }
                                            SpawnerUtils.updateSpawnPool(ctx, detail);
                                            return 1;
                                        })
                        )
        );
        then(
                CommandManager.literal("moonPhase")
                        .then(
                                CommandManager.argument("phase", StringArgumentType.string())
                                        .suggests((commandContext, suggestionsBuilder) -> {
                                            for (Map.Entry<String, MoonPhaseRange> entry : MoonPhaseRange.Companion.getMoonPhaseRanges().entrySet()) {
                                                suggestionsBuilder.suggest(entry.getKey());
                                            }
                                            suggestionsBuilder.suggest("null");
                                            return suggestionsBuilder.buildFuture();
                                        })
                                        .executes(ctx -> {
                                            String moonPhase = StringArgumentType.getString(ctx, "phase");
                                            MoonPhaseRange range = null;
                                            if (!moonPhase.equals("null")) {
                                                range = MoonPhaseRange.Companion.getMoonPhaseRanges().get(moonPhase);
                                            }
                                            SpawnDetail detail = SpawnerUtils.getSpawnDetail(ctx);
                                            if (detail == null) {
                                                return 0;
                                            }
                                            if (literal.equalsIgnoreCase("condition")) {
                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getConditions());
                                                for (SpawningCondition<?> condition : conditions) {
                                                    condition.setMoonPhase(range);
                                                }
                                                detail.setConditions(conditions);
                                            } else if (literal.equalsIgnoreCase("anticondition")) {
                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getAnticonditions());
                                                for (SpawningCondition<?> condition : conditions) {
                                                    condition.setMoonPhase(range);
                                                }
                                                detail.setAnticonditions(conditions);
                                            }
                                            SpawnerUtils.updateSpawnPool(ctx, detail);
                                            return 1;
                                        })
                        )
        );
        then(
                CommandManager.literal("minLight")
                        .then(
                                CommandManager.argument("level", IntegerArgumentType.integer(0, 15))
                                        .executes(ctx -> {
                                            int level = IntegerArgumentType.getInteger(ctx, "level");
                                            SpawnDetail detail = SpawnerUtils.getSpawnDetail(ctx);
                                            if (detail == null) {
                                                return 0;
                                            }
                                            if (literal.equalsIgnoreCase("condition")) {
                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getConditions());
                                                for (SpawningCondition<?> condition : conditions) {
                                                    condition.setMinLight(level);
                                                }
                                                detail.setConditions(conditions);
                                            } else if (literal.equalsIgnoreCase("anticondition")) {
                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getAnticonditions());
                                                for (SpawningCondition<?> condition : conditions) {
                                                    condition.setMinLight(level);
                                                }
                                                detail.setAnticonditions(conditions);
                                            }
                                            SpawnerUtils.updateSpawnPool(ctx, detail);
                                            return 1;
                                        })
                        )
                        .then(
                                CommandManager.literal("null")
                                        .executes(ctx -> {
                                            SpawnDetail detail = SpawnerUtils.getSpawnDetail(ctx);
                                            if (detail == null) {
                                                return 0;
                                            }
                                            if (literal.equalsIgnoreCase("condition")) {
                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getConditions());
                                                for (SpawningCondition<?> condition : conditions) {
                                                    condition.setMinLight(null);
                                                }
                                                detail.setConditions(conditions);
                                            } else if (literal.equalsIgnoreCase("anticondition")) {
                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getAnticonditions());
                                                for (SpawningCondition<?> condition : conditions) {
                                                    condition.setMinLight(null);
                                                }
                                                detail.setAnticonditions(conditions);
                                            }
                                            SpawnerUtils.updateSpawnPool(ctx, detail);
                                            return 1;
                                        })
                        )
        );
        then(
                CommandManager.literal("maxLight")
                        .then(
                                CommandManager.argument("level", IntegerArgumentType.integer(0, 15))
                                        .executes(ctx -> {
                                            int level = IntegerArgumentType.getInteger(ctx, "level");
                                            SpawnDetail detail = SpawnerUtils.getSpawnDetail(ctx);
                                            if (detail == null) {
                                                return 0;
                                            }
                                            if (literal.equalsIgnoreCase("condition")) {
                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getConditions());
                                                for (SpawningCondition<?> condition : conditions) {
                                                    condition.setMaxLight(level);
                                                }
                                                detail.setConditions(conditions);
                                            } else if (literal.equalsIgnoreCase("anticondition")) {
                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getAnticonditions());
                                                for (SpawningCondition<?> condition : conditions) {
                                                    condition.setMaxLight(level);
                                                }
                                                detail.setAnticonditions(conditions);
                                            }
                                            SpawnerUtils.updateSpawnPool(ctx, detail);
                                            return 1;
                                        })
                        )
                        .then(
                                CommandManager.literal("null")
                                        .executes(ctx -> {
                                            SpawnDetail detail = SpawnerUtils.getSpawnDetail(ctx);
                                            if (detail == null) {
                                                return 0;
                                            }
                                            if (literal.equalsIgnoreCase("condition")) {
                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getConditions());
                                                for (SpawningCondition<?> condition : conditions) {
                                                    condition.setMaxLight(null);
                                                }
                                                detail.setConditions(conditions);
                                            } else if (literal.equalsIgnoreCase("anticondition")) {
                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getAnticonditions());
                                                for (SpawningCondition<?> condition : conditions) {
                                                    condition.setMaxLight(null);
                                                }
                                                detail.setAnticonditions(conditions);
                                            }
                                            SpawnerUtils.updateSpawnPool(ctx, detail);
                                            return 1;
                                        })
                        )
        );
        then(
                CommandManager.literal("minSkyLight")
                        .then(
                                CommandManager.argument("level", IntegerArgumentType.integer(0, 15))
                                        .executes(ctx -> {
                                            int level = IntegerArgumentType.getInteger(ctx, "level");
                                            SpawnDetail detail = SpawnerUtils.getSpawnDetail(ctx);
                                            if (detail == null) {
                                                return 0;
                                            }
                                            if (literal.equalsIgnoreCase("condition")) {
                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getConditions());
                                                for (SpawningCondition<?> condition : conditions) {
                                                    condition.setMinSkyLight(level);
                                                }
                                                detail.setConditions(conditions);
                                            } else if (literal.equalsIgnoreCase("anticondition")) {
                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getAnticonditions());
                                                for (SpawningCondition<?> condition : conditions) {
                                                    condition.setMinSkyLight(level);
                                                }
                                                detail.setAnticonditions(conditions);
                                            }
                                            SpawnerUtils.updateSpawnPool(ctx, detail);
                                            return 1;
                                        })
                        )
                        .then(
                                CommandManager.literal("null")
                                        .executes(ctx -> {
                                            SpawnDetail detail = SpawnerUtils.getSpawnDetail(ctx);
                                            if (detail == null) {
                                                return 0;
                                            }
                                            if (literal.equalsIgnoreCase("condition")) {
                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getConditions());
                                                for (SpawningCondition<?> condition : conditions) {
                                                    condition.setMinSkyLight(null);
                                                }
                                                detail.setConditions(conditions);
                                            } else if (literal.equalsIgnoreCase("anticondition")) {
                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getAnticonditions());
                                                for (SpawningCondition<?> condition : conditions) {
                                                    condition.setMinSkyLight(null);
                                                }
                                                detail.setAnticonditions(conditions);
                                            }
                                            SpawnerUtils.updateSpawnPool(ctx, detail);
                                            return 1;
                                        })
                        )
        );
        then(
                CommandManager.literal("maxSkyLight")
                        .then(
                                CommandManager.argument("level", IntegerArgumentType.integer(0, 15))
                                        .executes(ctx -> {
                                            int level = IntegerArgumentType.getInteger(ctx, "level");
                                            SpawnDetail detail = SpawnerUtils.getSpawnDetail(ctx);
                                            if (detail == null) {
                                                return 0;
                                            }
                                            if (literal.equalsIgnoreCase("condition")) {
                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getConditions());
                                                for (SpawningCondition<?> condition : conditions) {
                                                    condition.setMaxSkyLight(level);
                                                }
                                                detail.setConditions(conditions);
                                            } else if (literal.equalsIgnoreCase("anticondition")) {
                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getAnticonditions());
                                                for (SpawningCondition<?> condition : conditions) {
                                                    condition.setMaxSkyLight(level);
                                                }
                                                detail.setAnticonditions(conditions);
                                            }
                                            SpawnerUtils.updateSpawnPool(ctx, detail);
                                            return 1;
                                        })
                        )
                        .then(
                                CommandManager.literal("null")
                                        .executes(ctx -> {
                                            SpawnDetail detail = SpawnerUtils.getSpawnDetail(ctx);
                                            if (detail == null) {
                                                return 0;
                                            }
                                            if (literal.equalsIgnoreCase("condition")) {
                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getConditions());
                                                for (SpawningCondition<?> condition : conditions) {
                                                    condition.setMaxSkyLight(null);
                                                }
                                                detail.setConditions(conditions);
                                            } else if (literal.equalsIgnoreCase("anticondition")) {
                                                List<SpawningCondition<?>> conditions = new ArrayList<>(detail.getAnticonditions());
                                                for (SpawningCondition<?> condition : conditions) {
                                                    condition.setMaxSkyLight(null);
                                                }
                                                detail.setAnticonditions(conditions);
                                            }
                                            SpawnerUtils.updateSpawnPool(ctx, detail);
                                            return 1;
                                        })
                        )
        );
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
}
