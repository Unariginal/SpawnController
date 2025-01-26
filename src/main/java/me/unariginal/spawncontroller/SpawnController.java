package me.unariginal.spawncontroller;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.command.argument.SpeciesArgumentType;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import kotlin.Unit;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class SpawnController implements ModInitializer {
    private final ArrayList<Pokemon> blacklist = new ArrayList<>();
    public static SpawnController instance;
    public Config config;

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            final LiteralCommandNode<ServerCommandSource> node = dispatcher.register(
                    CommandManager.literal("spawncontroller")
                            .then(
                                    CommandManager.literal("disable")
                                            .then(
                                                    CommandManager.argument("species", SpeciesArgumentType.Companion.species())
                                                            .requires(Permissions.require("spawncontroller.disable", 4))
                                                            .suggests((context, builder) -> {
                                                                PokemonSpecies.INSTANCE.getSpecies().forEach(species -> {
                                                                    Pokemon temp = new Pokemon();
                                                                    temp.setSpecies(species);
                                                                    AtomicBoolean pass = new AtomicBoolean(true);
                                                                    blacklist.forEach(pokemon -> {
                                                                        if (pokemon.getSpecies().equals(species)) {
                                                                            pass.set(false);
                                                                        }
                                                                    });
                                                                    if (pass.get()) {
                                                                        builder.suggest(species.getName().toLowerCase());
                                                                    }
                                                                });
                                                                return builder.buildFuture();
                                                            })
                                                            .executes(this::disableSpawn)
                                            )

                            )
                            .then(
                                    CommandManager.literal("enable")
                                            .then(
                                                    CommandManager.argument("species", SpeciesArgumentType.Companion.species())
                                                            .requires(Permissions.require("spawncontroller.enable", 4))
                                                            .suggests((context, builder) -> {
                                                                blacklist.forEach(pokemon -> builder.suggest(pokemon.getSpecies().getName().toLowerCase()));
                                                                return builder.buildFuture();
                                                            })
                                                            .executes(this::enableSpawn)
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
            config = new Config();
            CobblemonEvents.POKEMON_ENTITY_SPAWN.subscribe(Priority.NORMAL, event -> {
                for (Pokemon pokemon : blacklist) {
                    if (event.getEntity().getPokemon().getSpecies().equals(pokemon.getSpecies())) {
                        event.cancel();
                    }
                }
                return Unit.INSTANCE;
            });
        });
    }

    public int enableSpawn(CommandContext<ServerCommandSource> ctx) {
        Pokemon temp = new Pokemon();
        temp.setSpecies(SpeciesArgumentType.Companion.getPokemon(ctx, "species"));
        ArrayList<Pokemon> toKeep = new ArrayList<>();
        for (Pokemon pokemon : blacklist) {
            if (!pokemon.getSpecies().equals(temp.getSpecies())) {
                toKeep.add(pokemon);
            }
        }

        if (toKeep.size() == blacklist.size()) {
            ctx.getSource().sendMessage(Text.literal("Spawn is already enabled for " + temp.getSpecies().getName().toLowerCase() + "!"));
            return 0;
        }

        blacklist.clear();
        blacklist.addAll(toKeep);
        config.updateBlacklist();
        ctx.getSource().sendMessage(Text.literal("Enabled spawns for " + temp.getSpecies().getName().toLowerCase() + "!"));
        return 1;
    }

    public int disableSpawn(CommandContext<ServerCommandSource> ctx) {
        Pokemon temp = new Pokemon();
        temp.setSpecies(SpeciesArgumentType.Companion.getPokemon(ctx, "species"));
        if (addToBlacklist(temp) == 1) {
            ctx.getSource().sendMessage(Text.literal("Disabled spawns for " + temp.getSpecies().getName().toLowerCase() + "!"));
            config.updateBlacklist();
            return 1;
        } else {
            ctx.getSource().sendMessage(Text.literal("Spawn is already disabled for " + temp.getSpecies().getName().toLowerCase() + "!"));
            return 0;
        }
    }

    public int addToBlacklist(Pokemon pokemon) {
        for (Pokemon existingPokemon : blacklist) {
            if (existingPokemon.getSpecies().equals(pokemon.getSpecies())) {
                return 0;
            }
        }
        blacklist.add(pokemon);
        return 1;
    }

    public ArrayList<Pokemon> getBlacklist() {
        return blacklist;
    }
}
