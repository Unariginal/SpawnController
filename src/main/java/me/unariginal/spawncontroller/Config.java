package me.unariginal.spawncontroller;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.pokemon.Species;
import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;

import java.io.*;
import java.nio.file.Path;

public class Config {
    public Config() {
        try {
            checkFiles();
        } catch (IOException e) {
            e.printStackTrace();
        }
        loadBlacklist();
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

        File blacklistFile = FabricLoader.getInstance().getConfigDir().resolve("SpawnController/blacklist.json").toFile();
        if (!blacklistFile.exists()) {
            blacklistFile.createNewFile();

            InputStream in = SpawnController.class.getResourceAsStream("/spawncontroller_config/blacklist.json");
            OutputStream out = new FileOutputStream(blacklistFile);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }

            in.close();
            out.close();
        }
    }

    private void loadBlacklist() {
        File blacklistFile = FabricLoader.getInstance().getConfigDir().resolve("SpawnController/blacklist.json").toFile();

        JsonElement root = null;
        try {
            root = JsonParser.parseReader(new FileReader(blacklistFile));
        } catch (Exception e) {
            e.printStackTrace();
        }

        assert root != null;
        JsonObject rootObject = root.getAsJsonObject();

        JsonArray speciesBlacklistObject = rootObject.get("species").getAsJsonArray();
        for (JsonElement element : speciesBlacklistObject) {
            String speciesString = element.getAsString();
            Species species = PokemonSpecies.INSTANCE.getByName(speciesString.toLowerCase());
            if (species != null) {
                SpawnController.instance.addToBlacklist(species);
            }
        }

        JsonArray worldBlacklistObject = rootObject.get("worlds").getAsJsonArray();
        for (JsonElement element : worldBlacklistObject) {
            String worldString = element.getAsString();
            for (ServerWorld world : SpawnController.instance.mcServer.getWorlds()) {
                if ((world.getRegistryKey().getValue().getNamespace() + ":" + world.getRegistryKey().getValue().getPath()).equalsIgnoreCase(worldString)) {
                    SpawnController.instance.addToBlacklist(world);
                }
            }
        }

        JsonArray biomeBlacklistObject = rootObject.get("biomes").getAsJsonArray();
        for (JsonElement element : biomeBlacklistObject) {
            String biomeString = element.getAsString();
            SpawnController.instance.getBiomeList().forEach(biome -> SpawnController.instance.mcServer.getOverworld().getRegistryManager().get(RegistryKeys.BIOME).getEntry(biome).getKey().ifPresent(key -> {
                if ((key.getValue().getNamespace() + ":" + key.getValue().getPath()).equalsIgnoreCase(biomeString)) {
                    SpawnController.instance.addToBlacklist(biome);
                }
            }));
        }
    }

    public void updateBlacklist() {
        try {
            File blacklistFile = FabricLoader.getInstance().getConfigDir().resolve("SpawnController/blacklist.json").toFile();
            if (!blacklistFile.exists()) {
                blacklistFile.createNewFile();
            }
            JsonObject root = new JsonObject();
            JsonArray speciesList = new JsonArray();
            SpawnController.instance.getSpeciesBlacklist().forEach(species -> {
                speciesList.add(species.getName());
            });
            root.add("species", speciesList);

            JsonArray worldsList = new JsonArray();
            SpawnController.instance.getWorldBlacklist().forEach(world -> {
                worldsList.add(world.getRegistryKey().getValue().getNamespace() + ":" + world.getRegistryKey().getValue().getPath());
            });
            root.add("worlds", worldsList);

            JsonArray biomesList = new JsonArray();
            SpawnController.instance.getBiomeBlacklist().forEach(biome -> {
                SpawnController.instance.mcServer.getOverworld().getRegistryManager().get(RegistryKeys.BIOME).getEntry(biome).getKey().ifPresent(key -> biomesList.add(key.getValue().getNamespace() + ":" + key.getValue().getPath()));
            });
            root.add("biomes", biomesList);

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Writer writer = new FileWriter(blacklistFile);
            gson.toJson(root, writer);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
