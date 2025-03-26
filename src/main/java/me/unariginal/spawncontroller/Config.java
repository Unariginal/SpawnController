package me.unariginal.spawncontroller;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.spawning.BestSpawner;
import com.cobblemon.mod.common.api.spawning.SpawnBucket;
import com.cobblemon.mod.common.pokemon.Species;
import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import java.io.*;
import java.nio.file.Path;
import java.util.List;

public class Config {
    public Config() {
        try {
            checkFiles();
        } catch (IOException e) {
            e.printStackTrace();
        }

        loadBlacklist();
        loadWhitelist();
        loadSpawnBuckets();
    }

    private void checkFiles() throws IOException {
        Path rootFolder = FabricLoader.getInstance().getConfigDir().resolve("SpawnController");
        File rootFile = rootFolder.toFile();
        if (!rootFile.exists()) {
            try {
                rootFile.mkdirs();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        String[] files = {
                "blacklist.json",
                "whitelist.json",
                "spawnbuckets.json",
        };

        for (String fileName : files) {
            File file = FabricLoader.getInstance().getConfigDir().resolve("SpawnController/" + fileName).toFile();
            if (file.createNewFile()) {
                InputStream in = SpawnController.class.getResourceAsStream("/spawncontroller_config/" + fileName);
                OutputStream out = new FileOutputStream(file);

                assert in != null;

                byte[] buffer = new byte[1024];
                int length;
                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }

                in.close();
                out.close();
            }
        }
    }

    private JsonObject getRoot(File file) {
        JsonElement root = null;
        try {
            root = JsonParser.parseReader(new FileReader(file));
        } catch (Exception e) {
            e.printStackTrace();
        }

        assert root != null;
        return root.getAsJsonObject();
    }

    private void loadSpawnBuckets() {
        File spawnBucketsFile = FabricLoader.getInstance().getConfigDir().resolve("SpawnController/spawnbuckets.json").toFile();

        JsonObject root = getRoot(spawnBucketsFile);

        float common = root.get("common").getAsFloat();
        float uncommon = root.get("uncommon").getAsFloat();
        float rare = root.get("rare").getAsFloat();
        float ultraRare = root.get("ultra-rare").getAsFloat();

        BestSpawner.INSTANCE.getConfig().getBuckets().clear();
        BestSpawner.INSTANCE.getConfig().getBuckets().addAll(List.of(
                new SpawnBucket("common", common),
                new SpawnBucket("uncommon", uncommon),
                new SpawnBucket("rare", rare),
                new SpawnBucket("ultra-rare", ultraRare)
        ));
    }

    private void loadBlacklist() {
        File blacklistFile = FabricLoader.getInstance().getConfigDir().resolve("SpawnController/blacklist.json").toFile();

        JsonObject rootObject = getRoot(blacklistFile);

        JsonArray speciesBlacklistObject = rootObject.get("species").getAsJsonArray();
        for (JsonElement element : speciesBlacklistObject) {
            String speciesString = element.getAsString();
            Species species = PokemonSpecies.INSTANCE.getByName(speciesString.toLowerCase());
            if (species != null) {
                SpawnController.INSTANCE.blacklistAdd(species);
            }
        }

        JsonArray worldBlacklistObject = rootObject.get("worlds").getAsJsonArray();
        for (JsonElement element : worldBlacklistObject) {
            String worldString = element.getAsString();
            for (ServerWorld world : SpawnController.INSTANCE.server.getWorlds()) {
                if ((world.getRegistryKey().getValue().getNamespace() + ":" + world.getRegistryKey().getValue().getPath()).equalsIgnoreCase(worldString)) {
                    SpawnController.INSTANCE.blacklistAdd(world);
                }
            }
        }

        JsonArray biomeBlacklistObject = rootObject.get("biomes").getAsJsonArray();
        for (JsonElement element : biomeBlacklistObject) {
            String biomeString = element.getAsString();
            SpawnController.INSTANCE.getRegisteredBiomes().forEach(biome -> SpawnController.INSTANCE.server.getOverworld().getRegistryManager().get(RegistryKeys.BIOME).getEntry(biome).getKey().ifPresent(key -> {
                if ((key.getValue().getNamespace() + ":" + key.getValue().getPath()).equalsIgnoreCase(biomeString)) {
                    SpawnController.INSTANCE.blacklistAdd(biome);
                }
            }));
        }

        JsonArray generationBlacklistObject = rootObject.get("generations").getAsJsonArray();
        for (JsonElement element : generationBlacklistObject) {
            String label = element.getAsString();
            SpawnController.INSTANCE.blacklistAdd(label, "generation");
        }

        JsonArray formBlacklistObject = rootObject.get("forms").getAsJsonArray();
        for (JsonElement element : formBlacklistObject) {
            String label = element.getAsString();
            SpawnController.INSTANCE.blacklistAdd(label, "form");
        }

        JsonArray groupBlacklistObject = rootObject.get("groups").getAsJsonArray();
        for (JsonElement element : groupBlacklistObject) {
            String label = element.getAsString();
            SpawnController.INSTANCE.blacklistAdd(label, "group");
        }

        JsonArray customLabelsBlacklistObject = rootObject.get("customLabels").getAsJsonArray();
        for (JsonElement element : customLabelsBlacklistObject) {
            String label = element.getAsString();
            SpawnController.INSTANCE.blacklistAdd(label, "label");
        }
    }

    private void loadWhitelist() {
        File whitelistFile = FabricLoader.getInstance().getConfigDir().resolve("SpawnController/whitelist.json").toFile();

        JsonObject rootObject = getRoot(whitelistFile);

        JsonArray speciesWhitelistObject = rootObject.get("species").getAsJsonArray();
        for (JsonElement element : speciesWhitelistObject) {
            String speciesString = element.getAsString();
            Species species = PokemonSpecies.INSTANCE.getByName(speciesString.toLowerCase());
            if (species != null) {
                SpawnController.INSTANCE.whitelistAdd(species);
            }
        }

        JsonArray worldWhitelistObject = rootObject.get("worlds").getAsJsonArray();
        for (JsonElement element : worldWhitelistObject) {
            String worldString = element.getAsString();
            for (ServerWorld world : SpawnController.INSTANCE.server.getWorlds()) {
                if ((world.getRegistryKey().getValue().getNamespace() + ":" + world.getRegistryKey().getValue().getPath()).equalsIgnoreCase(worldString)) {
                    SpawnController.INSTANCE.whitelistAdd(world);
                }
            }
        }

        JsonArray biomeWhitelistObject = rootObject.get("biomes").getAsJsonArray();
        for (JsonElement element : biomeWhitelistObject) {
            String biomeString = element.getAsString();
            SpawnController.INSTANCE.getRegisteredBiomes().forEach(biome -> SpawnController.INSTANCE.server.getOverworld().getRegistryManager().get(RegistryKeys.BIOME).getEntry(biome).getKey().ifPresent(key -> {
                if ((key.getValue().getNamespace() + ":" + key.getValue().getPath()).equalsIgnoreCase(biomeString)) {
                    SpawnController.INSTANCE.whitelistAdd(biome);
                }
            }));
        }

        JsonArray generationWhitelistObject = rootObject.get("generations").getAsJsonArray();
        for (JsonElement element : generationWhitelistObject) {
            String label = element.getAsString();
            SpawnController.INSTANCE.whitelistAdd(label, "generation");
        }

        JsonArray formWhitelistObject = rootObject.get("forms").getAsJsonArray();
        for (JsonElement element : formWhitelistObject) {
            String label = element.getAsString();
            SpawnController.INSTANCE.whitelistAdd(label, "form");
        }

        JsonArray groupWhitelistObject = rootObject.get("groups").getAsJsonArray();
        for (JsonElement element : groupWhitelistObject) {
            String label = element.getAsString();
            SpawnController.INSTANCE.whitelistAdd(label, "group");
        }

        JsonArray customLabelsWhitelistObject = rootObject.get("customLabels").getAsJsonArray();
        for (JsonElement element : customLabelsWhitelistObject) {
            String label = element.getAsString();
            SpawnController.INSTANCE.whitelistAdd(label, "label");
        }
    }

    public void updateSpawnBuckets() {
        try {
            File spawnBucketsFile = FabricLoader.getInstance().getConfigDir().resolve("SpawnController/spawnbuckets.json").toFile();
            spawnBucketsFile.createNewFile();

            JsonObject root = new JsonObject();
            for (SpawnBucket bucket : BestSpawner.INSTANCE.getConfig().getBuckets()) {
               root.addProperty(bucket.getName(), bucket.getWeight());
            }

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Writer writer = new FileWriter(spawnBucketsFile);
            gson.toJson(root, writer);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updateBlacklist() {
        try {
            File blacklistFile = FabricLoader.getInstance().getConfigDir().resolve("SpawnController/blacklist.json").toFile();
            blacklistFile.createNewFile();

            JsonObject root = new JsonObject();
            JsonArray speciesList = new JsonArray();
            SpawnController.INSTANCE.getSpeciesBlacklist().forEach(species -> speciesList.add(species.getName()));
            root.add("species", speciesList);

            JsonArray worldsList = new JsonArray();
            SpawnController.INSTANCE.getWorldBlacklist().forEach(world -> worldsList.add(world.getRegistryKey().getValue().getNamespace() + ":" + world.getRegistryKey().getValue().getPath()));
            root.add("worlds", worldsList);

            JsonArray biomesList = new JsonArray();
            SpawnController.INSTANCE.getBiomeBlacklist().forEach(biome -> SpawnController.INSTANCE.server.getOverworld().getRegistryManager().get(RegistryKeys.BIOME).getEntry(biome).getKey().ifPresent(key -> biomesList.add(key.getValue().getNamespace() + ":" + key.getValue().getPath())));
            root.add("biomes", biomesList);

            JsonArray generationList = new JsonArray();
            SpawnController.INSTANCE.getGenerationBlacklist().forEach(generationList::add);
            root.add("generations", generationList);

            JsonArray formList = new JsonArray();
            SpawnController.INSTANCE.getFormBlacklist().forEach(formList::add);
            root.add("forms", formList);

            JsonArray groupList = new JsonArray();
            SpawnController.INSTANCE.getGroupBlacklist().forEach(groupList::add);
            root.add("groups", groupList);

            JsonArray customLabelList = new JsonArray();
            SpawnController.INSTANCE.getCustomLabelBlacklist().forEach(customLabelList::add);
            root.add("customLabels", customLabelList);

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Writer writer = new FileWriter(blacklistFile);
            gson.toJson(root, writer);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updateWhitelist() {
        try {
            File whitelistFile = FabricLoader.getInstance().getConfigDir().resolve("SpawnController/whitelist.json").toFile();
            whitelistFile.createNewFile();

            JsonObject root = new JsonObject();
            JsonArray speciesList = new JsonArray();
            SpawnController.INSTANCE.getSpeciesWhitelist().forEach(species -> speciesList.add(species.getName()));
            root.add("species", speciesList);

            JsonArray worldsList = new JsonArray();
            SpawnController.INSTANCE.getWorldWhitelist().forEach(world -> worldsList.add(world.getRegistryKey().getValue().getNamespace() + ":" + world.getRegistryKey().getValue().getPath()));
            root.add("worlds", worldsList);

            JsonArray biomesList = new JsonArray();
            SpawnController.INSTANCE.getBiomeWhitelist().forEach(biome -> SpawnController.INSTANCE.server.getOverworld().getRegistryManager().get(RegistryKeys.BIOME).getEntry(biome).getKey().ifPresent(key -> biomesList.add(key.getValue().getNamespace() + ":" + key.getValue().getPath())));
            root.add("biomes", biomesList);

            JsonArray generationList = new JsonArray();
            SpawnController.INSTANCE.getGenerationWhitelist().forEach(generationList::add);
            root.add("generations", generationList);

            JsonArray formList = new JsonArray();
            SpawnController.INSTANCE.getFormWhitelist().forEach(formList::add);
            root.add("forms", formList);

            JsonArray groupList = new JsonArray();
            SpawnController.INSTANCE.getGroupWhitelist().forEach(groupList::add);
            root.add("groups", groupList);

            JsonArray customLabelList = new JsonArray();
            SpawnController.INSTANCE.getCustomLabelWhitelist().forEach(customLabelList::add);
            root.add("customLabels", customLabelList);

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Writer writer = new FileWriter(whitelistFile);
            gson.toJson(root, writer);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
