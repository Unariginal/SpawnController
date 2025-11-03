package me.unariginal.spawncontroller.config;

import com.google.gson.*;
import me.unariginal.spawncontroller.SpawnController;
import me.unariginal.spawncontroller.datatypes.ListData;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.util.List;

public class WhitelistConfig {
    private static final Gson gson = new GsonBuilder()
            .disableHtmlEscaping()
            .setLenient()
            .setPrettyPrinting()
            .create();

    public static ListData whitelist = new ListData(List.of(),List.of(),List.of(),List.of(),List.of(),List.of(),List.of());

    public static void load() throws IOException {
        File whitelistFile = FabricLoader.getInstance().getConfigDir().resolve("SpawnController/whitelist.json").toFile();
        if (!whitelistFile.exists()) save();
        else whitelist = gson.fromJson(JsonParser.parseReader(new FileReader(whitelistFile)).toString(), ListData.class);
    }

    public static void save() {
        File whitelistFile = FabricLoader.getInstance().getConfigDir().resolve("SpawnController/whitelist.json").toFile();

        JsonObject root = new JsonObject();

        JsonArray species = new JsonArray();
        whitelist.species.forEach(species::add);
        root.add("species", species);

        JsonArray worlds = new JsonArray();
        whitelist.worlds.forEach(worlds::add);
        root.add("worlds", worlds);

        JsonArray biomes = new JsonArray();
        whitelist.biomes.forEach(biomes::add);
        root.add("biomes", biomes);

        JsonArray generations = new JsonArray();
        whitelist.generations.forEach(generations::add);
        root.add("generations", generations);

        JsonArray forms = new JsonArray();
        whitelist.forms.forEach(forms::add);
        root.add("forms", forms);

        JsonArray groups = new JsonArray();
        whitelist.groups.forEach(groups::add);
        root.add("groups", groups);

        JsonArray customLabels = new JsonArray();
        whitelist.customLabels.forEach(customLabels::add);
        root.add("customLabels", customLabels);

        try {
            whitelistFile.createNewFile();
            Writer writer = new FileWriter(whitelistFile);
            gson.toJson(root, writer);
            writer.close();
        } catch (IOException e) {
            SpawnController.LOGGER.error("Error while saving whitelist config!", e);
        }
    }
}
