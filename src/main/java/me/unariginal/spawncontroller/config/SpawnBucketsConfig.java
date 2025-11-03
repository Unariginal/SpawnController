package me.unariginal.spawncontroller.config;

import com.cobblemon.mod.common.api.spawning.BestSpawner;
import com.cobblemon.mod.common.api.spawning.SpawnBucket;
import com.google.gson.*;
import me.unariginal.spawncontroller.SpawnController;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;

public class SpawnBucketsConfig {
    private static final Gson gson = new GsonBuilder()
            .disableHtmlEscaping()
            .setLenient()
            .setPrettyPrinting()
            .create();

    public static void load() throws IOException {
        File rootFolder = FabricLoader.getInstance().getConfigDir().resolve("SpawnController").toFile();
        if (!rootFolder.exists()) rootFolder.mkdirs();
        File spawnBucketsFile = FabricLoader.getInstance().getConfigDir().resolve("SpawnController/spawn_buckets.json").toFile();

        if (!spawnBucketsFile.exists()) save();
        else {
            JsonObject root = JsonParser.parseReader(new FileReader(spawnBucketsFile)).getAsJsonObject();
            BestSpawner.INSTANCE.getConfig().getBuckets().clear();
            for (String bucketName : root.keySet()) {
                float bucketWeight = root.get(bucketName).getAsFloat();
                BestSpawner.INSTANCE.getConfig().getBuckets().add(
                        new SpawnBucket(bucketName, bucketWeight)
                );
            }
        }
    }

    public static void save() {
        File spawnBucketsFile = FabricLoader.getInstance().getConfigDir().resolve("SpawnController/spawn_buckets.json").toFile();

        JsonObject root = new JsonObject();
        BestSpawner.INSTANCE.getConfig().getBuckets().forEach(bucket -> {
            root.addProperty(bucket.getName(), bucket.getWeight());
        });

        try {
            spawnBucketsFile.createNewFile();
            Writer writer = new FileWriter(spawnBucketsFile);
            gson.toJson(root, writer);
            writer.close();
        } catch (IOException e) {
            SpawnController.LOGGER.error("Error while saving spawn buckets config!", e);
        }
    }
}
