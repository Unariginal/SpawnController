package me.unariginal.spawncontroller.commands.modifypool;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.spawning.CobblemonSpawnPools;
import com.cobblemon.mod.common.api.spawning.SpawnBucket;
import com.cobblemon.mod.common.api.spawning.detail.PokemonSpawnDetail;
import com.cobblemon.mod.common.api.spawning.detail.SpawnDetail;
import com.cobblemon.mod.common.command.argument.SpawnBucketArgumentType;
import com.cobblemon.mod.common.command.argument.SpeciesArgumentType;
import com.cobblemon.mod.common.pokemon.Species;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import kotlin.ranges.IntRange;
import me.lucko.fabric.api.permissions.v0.Permissions;
import me.unariginal.spawncontroller.Utils.SpawnerUtils;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import java.util.List;

public class ModifyPool extends LiteralArgumentBuilder<ServerCommandSource> {

    public ModifyPool() {
        super("modify-pool");
        requires(Permissions.require("spawncontroller.modifypool", 4));
        then(
                CommandManager.argument("pokemon", SpeciesArgumentType.Companion.species())
                        .suggests((commandContext, suggestionsBuilder) -> {
                            PokemonSpecies.INSTANCE.getSpecies().forEach(species -> suggestionsBuilder.suggest(species.showdownId().toLowerCase()));
                            return suggestionsBuilder.buildFuture();
                        })
                        .then(
                                CommandManager.argument("spawn-detail", StringArgumentType.string())
                                        .suggests((commandContext, suggestionsBuilder) -> {
                                            Species species = SpeciesArgumentType.Companion.getPokemon(commandContext, "pokemon");
                                            List<SpawnDetail> worldSpawnPoolDetails = CobblemonSpawnPools.WORLD_SPAWN_POOL.getDetails();

                                            for (SpawnDetail spawnDetail : worldSpawnPoolDetails) {
                                                if (!spawnDetail.getId().isEmpty() && spawnDetail.getId().contains("-")) {
                                                    if (spawnDetail.getId().toLowerCase().substring(0, spawnDetail.getId().toLowerCase().indexOf("-")).equalsIgnoreCase(species.showdownId().toLowerCase())) {
                                                        suggestionsBuilder.suggest("\"" + spawnDetail.getId().toLowerCase() + "\"");
                                                    }
                                                }
                                            }
                                            return suggestionsBuilder.buildFuture();
                                        })
                                        .then(
                                                CommandManager.literal("bucket")
                                                        .then(
                                                                CommandManager.argument("bucket", SpawnBucketArgumentType.Companion.spawnBucket())
                                                                        .executes(ctx -> {
                                                                            SpawnBucket bucket = SpawnBucketArgumentType.Companion.getSpawnBucket(ctx, "bucket");
                                                                            SpawnDetail detail = SpawnerUtils.getSpawnDetail(ctx);
                                                                            if (detail == null) {
                                                                                return 0;
                                                                            }
                                                                            detail.setBucket(bucket);
                                                                            SpawnerUtils.updateSpawnPool(ctx, detail);
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
                                                                            SpawnDetail detail = SpawnerUtils.getSpawnDetail(ctx);
                                                                            if (detail == null) {
                                                                                return 0;
                                                                            }
                                                                            detail.setWeight(weight);
                                                                            SpawnerUtils.updateSpawnPool(ctx, detail);
                                                                            return 1;
                                                                        })
                                                        )
                                        )
                                        .then(
                                                CommandManager.literal("levelRange")
                                                        .then(
                                                                CommandManager.argument("minimum", IntegerArgumentType.integer(1))
                                                                        .then(
                                                                                CommandManager.argument("maximum", IntegerArgumentType.integer(1, Cobblemon.config.getMaxPokemonLevel()))
                                                                                        .executes(ctx -> {
                                                                                            int minimum = IntegerArgumentType.getInteger(ctx, "minimum");
                                                                                            int maximum = IntegerArgumentType.getInteger(ctx, "maximum");
                                                                                            SpawnDetail detail = SpawnerUtils.getSpawnDetail(ctx);
                                                                                            if (detail instanceof PokemonSpawnDetail pokemonSpawnDetail) {
                                                                                                pokemonSpawnDetail.setLevelRange(new IntRange(minimum, maximum));
                                                                                            }
                                                                                            SpawnerUtils.updateSpawnPool(ctx, detail);
                                                                                            return 1;
                                                                                        })
                                                                        )
                                                        )
                                        )
                                        .then(
                                                new Condition("condition")
                                        )
                                        .then(
                                                new Condition("anticondition")
                                        )
                        )
        );
    }
}
