package me.unariginal.spawncontroller;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;

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
        JsonArray blacklistObject = rootObject.get("blacklist").getAsJsonArray();
        for (JsonElement element : blacklistObject) {
            String speciesString = element.getAsString();
            Pokemon temp = new Pokemon();
            Species species = PokemonSpecies.INSTANCE.getByName(speciesString.toLowerCase());
            if (species != null) {
                temp.setSpecies(species);
            }
            SpawnController.instance.addToBlacklist(temp);
        }
    }

    public void updateBlacklist() {
        try {
            File blacklistFile = FabricLoader.getInstance().getConfigDir().resolve("SpawnController/blacklist.json").toFile();
            if (!blacklistFile.exists()) {
                blacklistFile.createNewFile();
            }
            JsonObject root = new JsonObject();
            JsonArray blacklist = new JsonArray();
            SpawnController.instance.getBlacklist().forEach(pokemon -> {
                blacklist.add(pokemon.getSpecies().getName());
            });
            root.add("blacklist", blacklist);

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Writer writer = new FileWriter(blacklistFile);
            gson.toJson(root, writer);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
