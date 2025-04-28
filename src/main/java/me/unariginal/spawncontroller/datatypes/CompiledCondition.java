package me.unariginal.spawncontroller.datatypes;

import com.cobblemon.mod.common.api.spawning.MoonPhaseRange;
import net.minecraft.block.Block;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CompiledCondition {
    public List<Identifier> dimensions = new ArrayList<>();
    public Set<RegistryEntry<Biome>> biomes = new HashSet<>();
    public MoonPhaseRange moonPhaseRange = null;
    public Boolean canSeeSky = null;
    public Text minX = null;
    public Text minY = null;
    public Text minZ = null;
    public Text maxX = null;
    public Text maxY = null;
    public Text maxZ = null;
    public Text minLight = null;
    public Text maxLight = null;
    public Boolean raining = null;
    public Boolean thundering = null;
    public Integer timeRangeStart = null;
    public Integer timeRangeEnd = null;
    public List<Identifier> structures = new ArrayList<>();
    public Boolean slimeChunk = null;
    public Integer area_minHeight = null;
    public Integer area_maxHeight = null;
    public Set<RegistryEntry<Block>> grounded_baseBlocks = new HashSet<>();
    public Integer sub_minDepth = null;
    public Integer sub_maxDepth = null;
    public Boolean sub_fluidIsSource = null;
    public RegistryEntry<Fluid> sub_fluid = null;
    public Integer surface_minDepth = null;
    public Integer surface_maxDepth = null;
    public RegistryEntry<Fluid> surface_fluid = null;
    public Set<RegistryEntry<Block>> seafloor_baseBlocks = new HashSet<>();
    public RegistryEntry<Item> fishing_rod = null;
    public Set<RegistryEntry<Block>> fishing_nearbyBlocks = new HashSet<>();
    public Integer fishing_minLureLevel = null;
    public Integer fishing_maxLureLevel = null;
    public Identifier fishing_bait = null;
    public Identifier fishing_rodType = null;
}
