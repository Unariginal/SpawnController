package me.unariginal.spawncontroller.commands;

import com.cobblemon.mod.common.api.spawning.BestSpawner;
import com.cobblemon.mod.common.api.spawning.SpawnBucket;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.lucko.fabric.api.permissions.v0.Permissions;
import me.unariginal.spawncontroller.SpawnController;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class Bucket extends LiteralArgumentBuilder<ServerCommandSource> {
    private final SpawnController sc = SpawnController.INSTANCE;

    protected Bucket() {
        super("bucket");

        requires(Permissions.require("spawncontroller.bucket", 4));

        executes(ctx -> {
            for (SpawnBucket bucket : BestSpawner.INSTANCE.getConfig().getBuckets()) {
                ctx.getSource().sendMessage(Text.literal(bucket.getName() + ": " + bucket.getWeight()));
            }
            return 1;
        });

        then(
                CommandManager.literal("common")
                        .then(
                                CommandManager.argument("weight", FloatArgumentType.floatArg(0.000F))
                                        .executes(ctx -> {
                                            List<SpawnBucket> buckets = new ArrayList<>(BestSpawner.INSTANCE.getConfig().getBuckets());
                                            buckets.set(0, new SpawnBucket("common", FloatArgumentType.getFloat(ctx, "weight")));
                                            BestSpawner.INSTANCE.getConfig().getBuckets().clear();
                                            BestSpawner.INSTANCE.getConfig().getBuckets().addAll(buckets);
                                            sc.config.updateSpawnBuckets();
                                            ctx.getSource().sendMessage(Text.literal("Updated common bucket."));
                                            return 1;
                                        })
                        )
        )
        .then(
                CommandManager.literal("uncommon")
                        .then(
                                CommandManager.argument("weight", FloatArgumentType.floatArg(0.000F))
                                        .executes(ctx -> {
                                            List<SpawnBucket> buckets = new ArrayList<>(BestSpawner.INSTANCE.getConfig().getBuckets());
                                            buckets.set(1, new SpawnBucket("uncommon", FloatArgumentType.getFloat(ctx, "weight")));
                                            BestSpawner.INSTANCE.getConfig().getBuckets().clear();
                                            BestSpawner.INSTANCE.getConfig().getBuckets().addAll(buckets);
                                            sc.config.updateSpawnBuckets();
                                            ctx.getSource().sendMessage(Text.literal("Updated uncommon bucket."));
                                            return 1;
                                        })
                        )
        )
        .then(
                CommandManager.literal("rare")
                        .then(
                                CommandManager.argument("weight", FloatArgumentType.floatArg(0.000F))
                                        .executes(ctx -> {
                                            List<SpawnBucket> buckets = new ArrayList<>(BestSpawner.INSTANCE.getConfig().getBuckets());
                                            buckets.set(2, new SpawnBucket("rare", FloatArgumentType.getFloat(ctx, "weight")));
                                            BestSpawner.INSTANCE.getConfig().getBuckets().clear();
                                            BestSpawner.INSTANCE.getConfig().getBuckets().addAll(buckets);
                                            sc.config.updateSpawnBuckets();
                                            ctx.getSource().sendMessage(Text.literal("Updated rare bucket."));
                                            return 1;
                                        })
                        )
        )
        .then(
                CommandManager.literal("ultra-rare")
                        .then(
                                CommandManager.argument("weight", FloatArgumentType.floatArg(0.000F))
                                        .executes(ctx -> {
                                            List<SpawnBucket> buckets = new ArrayList<>(BestSpawner.INSTANCE.getConfig().getBuckets());
                                            buckets.set(3, new SpawnBucket("ultra-rare", FloatArgumentType.getFloat(ctx, "weight")));
                                            BestSpawner.INSTANCE.getConfig().getBuckets().clear();
                                            BestSpawner.INSTANCE.getConfig().getBuckets().addAll(buckets);
                                            sc.config.updateSpawnBuckets();
                                            ctx.getSource().sendMessage(Text.literal("Updated ultra-rare bucket."));
                                            return 1;
                                        })
                        )
        )
        .then(
                CommandManager.literal("reset")
                        .executes(ctx -> {
                            BestSpawner.INSTANCE.getConfig().getBuckets().clear();
                            BestSpawner.INSTANCE.getConfig().getBuckets().addAll(List.of(
                                    new SpawnBucket("common", 93.8F),
                                    new SpawnBucket("uncommon", 5F),
                                    new SpawnBucket("rare", 1.0F),
                                    new SpawnBucket("ultra-rare", 0.2F)
                            ));
                            sc.config.updateSpawnBuckets();
                            ctx.getSource().sendMessage(Text.literal("Buckets reset."));
                            return 1;
                        })
        );
    }
}
