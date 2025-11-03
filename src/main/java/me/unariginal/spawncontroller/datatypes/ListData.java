package me.unariginal.spawncontroller.datatypes;

import java.util.List;

public class ListData {
    public List<String> species;
    public List<String> worlds;
    public List<String> biomes;
    public List<String> generations;
    public List<String> forms;
    public List<String> groups;
    public List<String> customLabels;

    public ListData(List<String> species, List<String> worlds, List<String> biomes, List<String> generations, List<String> forms, List<String> groups, List<String> customLabels) {
        this.species = species;
        this.worlds = worlds;
        this.biomes = biomes;
        this.generations = generations;
        this.forms = forms;
        this.groups = groups;
        this.customLabels = customLabels;
    }
}
