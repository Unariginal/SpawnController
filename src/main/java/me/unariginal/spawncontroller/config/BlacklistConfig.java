package me.unariginal.spawncontroller.config;

import com.google.gson.*;
import me.unariginal.spawncontroller.SpawnController;
import me.unariginal.spawncontroller.datatypes.ListData;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.util.List;

public class BlacklistConfig {
    private static final Gson gson = new GsonBuilder()
            .disableHtmlEscaping()
            .setLenient()
            .setPrettyPrinting()
            .create();

    public static ListData blacklist = new ListData(List.of(),List.of(),List.of(),List.of(),List.of(),List.of(),List.of());

    public static void load() throws IOException {
        File blacklistFile = FabricLoader.getInstance().getConfigDir().resolve("SpawnController/blacklist.json").toFile();
        File rootFolder = FabricLoader.getInstance().getConfigDir().resolve("SpawnController").toFile();
        if (!rootFolder.exists()) rootFolder.mkdirs();
        if (!blacklistFile.exists()) save();
        else blacklist = gson.fromJson(JsonParser.parseReader(new FileReader(blacklistFile)).toString(), ListData.class);
    }

    public static void save() {
        File blacklistFile = FabricLoader.getInstance().getConfigDir().resolve("SpawnController/blacklist.json").toFile();

        JsonObject root = new JsonObject();

        JsonArray species = new JsonArray();
        blacklist.species.forEach(species::add);
        root.add("species", species);

        JsonArray worlds = new JsonArray();
        blacklist.worlds.forEach(worlds::add);
        root.add("worlds", worlds);

        JsonArray biomes = new JsonArray();
        blacklist.biomes.forEach(biomes::add);
        root.add("biomes", biomes);

        JsonArray generations = new JsonArray();
        blacklist.generations.forEach(generations::add);
        root.add("generations", generations);

        JsonArray forms = new JsonArray();
        blacklist.forms.forEach(forms::add);
        root.add("forms", forms);

        JsonArray groups = new JsonArray();
        blacklist.groups.forEach(groups::add);
        root.add("groups", groups);

        JsonArray customLabels = new JsonArray();
        blacklist.customLabels.forEach(customLabels::add);
        root.add("customLabels", customLabels);

        try {
            blacklistFile.createNewFile();
            Writer writer = new FileWriter(blacklistFile);
            gson.toJson(root, writer);
            writer.close();
        } catch (IOException e) {
            SpawnController.LOGGER.error("Error while saving blacklist config!", e);
        }
    }
}
