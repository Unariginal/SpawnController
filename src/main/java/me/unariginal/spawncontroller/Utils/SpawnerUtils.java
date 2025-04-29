package me.unariginal.spawncontroller.Utils;

import com.cobblemon.mod.common.api.spawning.CobblemonSpawnPools;
import com.cobblemon.mod.common.api.spawning.detail.PokemonSpawnDetail;
import com.cobblemon.mod.common.api.spawning.detail.SpawnDetail;
import com.cobblemon.mod.common.command.argument.SpeciesArgumentType;
import com.cobblemon.mod.common.pokemon.Species;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.unariginal.spawncontroller.SpawnController;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

public class SpawnerUtils {
    public static SpawnDetail getSpawnDetail(CommandContext<ServerCommandSource> ctx) {
        Species species = SpeciesArgumentType.Companion.getPokemon(ctx, "pokemon");
        String id = StringArgumentType.getString(ctx, "spawn-detail");
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

    public static void updateSpawnPool(CommandContext<ServerCommandSource> ctx, SpawnDetail detail) {
        Species species = SpeciesArgumentType.Companion.getPokemon(ctx, "pokemon");
        String id = StringArgumentType.getString(ctx, "spawn-detail");
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
        for (ServerPlayerEntity player : SpawnController.INSTANCE.server.getPlayerManager().getPlayerList()) {
            CobblemonSpawnPools.WORLD_SPAWN_POOL.sync(player);
        }
    }
}
