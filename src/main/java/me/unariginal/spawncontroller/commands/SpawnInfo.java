package me.unariginal.spawncontroller.commands;

import com.cobblemon.mod.common.CobblemonItems;
import com.cobblemon.mod.common.api.conditional.RegistryLikeCondition;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.spawning.CobblemonSpawnPools;
import com.cobblemon.mod.common.api.spawning.condition.*;
import com.cobblemon.mod.common.api.spawning.detail.PokemonSpawnDetail;
import com.cobblemon.mod.common.api.spawning.detail.SpawnDetail;
import com.cobblemon.mod.common.command.argument.SpeciesArgumentType;
import com.cobblemon.mod.common.pokemon.Species;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.datafixers.util.Either;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import me.lucko.fabric.api.permissions.v0.Permissions;
import me.unariginal.spawncontroller.SpawnController;
import me.unariginal.spawncontroller.datatypes.CompiledCondition;
import net.minecraft.block.Block;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.structure.Structure;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SpawnInfo extends LiteralArgumentBuilder<ServerCommandSource> {
    private final SpawnController sc = SpawnController.INSTANCE;

    protected SpawnInfo() {
        super("spawn-info");

        requires(Permissions.require("spawncontroller.spawninfo", 4));

        then(
                CommandManager.argument("pokemon", SpeciesArgumentType.Companion.species())
                        .suggests((context, builder) -> {
                            PokemonSpecies.INSTANCE.getSpecies().forEach(species -> builder.suggest(species.getName().toLowerCase()));
                            return builder.buildFuture();
                        })
                        .executes(this::spawnDetails)
        );
    }

    public ScreenHandlerType<GenericContainerScreenHandler> getGuiSize(int size) {
        int gui_size = (int) Math.ceil((double) (size) / 9) + 1;
        return switch (gui_size) {
            case 2 -> ScreenHandlerType.GENERIC_9X2;
            case 3 -> ScreenHandlerType.GENERIC_9X3;
            case 4 -> ScreenHandlerType.GENERIC_9X4;
            case 5 -> ScreenHandlerType.GENERIC_9X5;
            default -> ScreenHandlerType.GENERIC_9X6;
        };
    }

    public int spawnDetails(CommandContext<ServerCommandSource> ctx) {
        try {
            if (!ctx.getSource().isExecutedByPlayer()) {
                return 0;
            }

            ServerPlayerEntity player = ctx.getSource().getPlayer();
            if (player == null) {
                return 0;
            }

            Species species = SpeciesArgumentType.Companion.getPokemon(ctx, "pokemon");
            List<SpawnDetail> worldSpawnPoolDetails = CobblemonSpawnPools.WORLD_SPAWN_POOL.getDetails();
            List<SpawnDetail> spawnDetails = new ArrayList<>();

            for (SpawnDetail spawnDetail : worldSpawnPoolDetails) {
                if (spawnDetail.getId().toLowerCase().contains(species.getName().toLowerCase())) {
                    spawnDetails.add(spawnDetail);
                }
            }

            SimpleGui detail_selection = new SimpleGui(getGuiSize(spawnDetails.size()), player, false);
            detail_selection.setTitle(species.getTranslatedName().append(" Spawn Details"));

            int slot = 0;
            for (SpawnDetail spawnDetail : spawnDetails) {
                GuiElementBuilder builder = new GuiElementBuilder();
                builder.setItem(CobblemonItems.WEAKNESS_POLICY);
                builder.setName(spawnDetail.getName().styled(style -> style.withItalic(false)));
                builder.setLore(List.of(
                        Text.literal("ID: " + spawnDetail.getId()).styled(style -> style.withItalic(false)),
                        Text.literal("Context: " + spawnDetail.getContext().getName()).styled(style -> style.withItalic(false)),
                        Text.literal("Weight: " + spawnDetail.getWeight()).styled(style -> style.withItalic(false)),
                        Text.literal("Bucket: " + spawnDetail.getBucket().getName()).styled(style -> style.withItalic(false)),
                        Text.empty(),
                        Text.literal("Click for more details!").styled(style -> style.withColor(Formatting.GREEN))
                ));

                builder.setCallback(clickType -> {
                    detailSections(player, spawnDetail, detail_selection);
                });

                detail_selection.setSlot(slot, builder.build());

                slot++;
                if (slot >= detail_selection.getSize()) {
                    break;
                }
            }

            detail_selection.open();
        } catch (Exception e) {
            SpawnController.LOGGER.error("Error: {}", e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                SpawnController.LOGGER.error("  {}", element.toString());
            }
        }

        return 1;
    }

    public void detailSections(ServerPlayerEntity player, SpawnDetail detail, SimpleGui back_gui) {
        SimpleGui gui = new SimpleGui(ScreenHandlerType.GENERIC_9X3, player, false);
        gui.setTitle(Text.literal(detail.getId() + " Detail Information"));

        int slot = 0;
        if (!detail.getLabels().isEmpty()) {
            List<Text> label_lore = new ArrayList<>();
            for (String label : detail.getLabels()) {
                label_lore.add(Text.literal(" - " + label));
            }

            gui.setSlot(slot++, new GuiElementBuilder(Items.PAPER)
                    .setName(Text.literal("Labels"))
                    .setLore(label_lore)
                    .build());
        }

        if (detail instanceof PokemonSpawnDetail pokemonDetail) {
            if (pokemonDetail.getLevelRange() != null) {
                gui.setSlot(slot++, new GuiElementBuilder(CobblemonItems.RARE_CANDY)
                        .setName(Text.literal("Level Range: " + pokemonDetail.getLevelRange().getStart() + " - " + pokemonDetail.getLevelRange().getEndInclusive()))
                        .build());
            }
        }

        gui.setSlot(slot++, new GuiElementBuilder(Items.BUCKET)
                .setName(Text.literal("Bucket: " + detail.getBucket().getName()))
                .build());

        gui.setSlot(slot++, new GuiElementBuilder(Items.PAPER)
                .setName(Text.literal("Weight: " + detail.getWeight()))
                .build());

        gui.setSlot(slot++, new GuiElementBuilder(Items.PAPER)
                .setName(Text.literal("Context: " + detail.getContext().getName()))
                .build());

        gui.setSlot(slot++, new GuiElementBuilder(Items.PAPER)
                .setName(Text.literal("Context Weight: " + detail.getContext().getWeight()))
                .build());

        if (!detail.getWeightMultipliers().isEmpty()) {
            gui.setSlot(slot++, new GuiElementBuilder(Items.FEATHER)
                    .setName(Text.literal("Weight Multiplier"))
                    .setLore(List.of(Text.literal("Click for more details!")))
                    .build());
        }

        if (detail.getCompositeCondition() != null) {
            gui.setSlot(slot++, new GuiElementBuilder(Items.FEATHER)
                    .setName(Text.literal("Composite Conditions"))
                    .setLore(List.of(Text.literal("Click for more details!")))
                    .setCallback(clickType -> {
                        conditionsMenu(player, detail.getCompositeCondition().getConditions(), detail.getId(), "Composite Conditions", gui);
                    })
                    .build());


            gui.setSlot(slot++, new GuiElementBuilder(Items.FEATHER)
                    .setName(Text.literal("Composite Anti-Conditions"))
                    .setLore(List.of(Text.literal("Click for more details!")))
                    .setCallback(clickType -> {
                        conditionsMenu(player, detail.getCompositeCondition().getAnticonditions(), detail.getId(), "Composite Anti-Conditions", gui);
                    })
                    .build());
        }

        if (!detail.getConditions().isEmpty()) {
            gui.setSlot(slot++, new GuiElementBuilder(Items.FEATHER)
                    .setName(Text.literal("Conditions"))
                    .setLore(List.of(Text.literal("Click for more details!")))
                    .setCallback(clickType -> {
                        conditionsMenu(player, detail.getConditions(), detail.getId(), "Conditions", gui);
                    })
                    .build());
        }

        if (!detail.getAnticonditions().isEmpty()) {
            gui.setSlot(slot, new GuiElementBuilder(Items.FEATHER)
                    .setName(Text.literal("Anti-Conditions"))
                    .setLore(List.of(Text.literal("Click for more details!")))
                    .setCallback(clickType -> {
                        conditionsMenu(player, detail.getAnticonditions(), detail.getId(), "Anti-Conditions", gui);
                    })
                    .build());
        }

        gui.setSlot(gui.getSize() - 5, new GuiElementBuilder(Items.BARRIER)
                .setName(Text.literal("Back"))
                .setCallback(clickType -> back_gui.open()));

        gui.open();
    }

    public void conditionsMenu(ServerPlayerEntity player, List<SpawningCondition<?>> conditions, String id, String title, SimpleGui back_gui) {
        SimpleGui gui = new SimpleGui(ScreenHandlerType.GENERIC_9X4, player, false);
        gui.setTitle(Text.literal(id + " " + title));

        CompiledCondition compiledCondition = new CompiledCondition();

        for (SpawningCondition<?> condition : conditions) {
            if (condition.getDimensions() != null) {
                compiledCondition.dimensions.addAll(condition.getDimensions());
            }

            if (condition.getBiomes() != null) {
                if (!condition.getBiomes().isEmpty()) {
                    for (RegistryLikeCondition<Biome> biome : condition.getBiomes()) {
                        for (Biome biomeKey : sc.server.getRegistryManager().get(RegistryKeys.BIOME).stream().toList()) {
                            if (biome.fits(biomeKey, sc.server.getRegistryManager().get(RegistryKeys.BIOME))) {
                                compiledCondition.biomes.add(sc.server.getRegistryManager().get(RegistryKeys.BIOME).getEntry(biomeKey));
                            }
                        }
                    }
                }
            }

            if (condition.getMoonPhase() != null) {
                compiledCondition.moonPhaseRange = condition.getMoonPhase();
            }

            if (condition.getCanSeeSky() != null) {
                compiledCondition.canSeeSky = condition.getCanSeeSky();
            }

            if (condition.getMinX() != null) {
                compiledCondition.minX = Text.literal("Min X: " + condition.getMinX());
            }
            if (condition.getMaxX() != null) {
                compiledCondition.maxX = Text.literal("Max X: " + condition.getMaxX());
            }

            if (condition.getMinY() != null) {
                compiledCondition.minY = Text.literal("Min Y: " + condition.getMinY());
            }
            if (condition.getMaxY() != null) {
                compiledCondition.maxY = Text.literal("Max Y: " + condition.getMaxY());
            }

            if (condition.getMinZ() != null) {
                compiledCondition.minZ = Text.literal("Min Z: " + condition.getMinZ());
            }
            if (condition.getMaxZ() != null) {
                compiledCondition.maxZ = Text.literal("Max Z: " + condition.getMaxZ());
            }

            if (condition.getMinLight() != null) {
                compiledCondition.minLight = Text.literal("Min Light: " + condition.getMinLight());
            }
            if (condition.getMaxLight() != null) {
                compiledCondition.maxLight = Text.literal("Max Light: " + condition.getMaxLight());
            }

            if (condition.isRaining() != null) {
                compiledCondition.raining = condition.isRaining();
            }
            if (condition.isThundering() != null) {
                compiledCondition.thundering = condition.isThundering();
            }

            if (condition.getTimeRange() != null) {
                compiledCondition.timeRangeStart = condition.getTimeRange().getRanges().getFirst().getStart();
                compiledCondition.timeRangeEnd = condition.getTimeRange().getRanges().getFirst().getEndInclusive();
            }

            if (condition.getStructures() != null) {
                if (!condition.getStructures().isEmpty()) {
                    for (Either<Identifier, TagKey<Structure>> structure : condition.getStructures()) {
                        if (structure.left() != null && structure.left().isPresent()) {
                            compiledCondition.structures.add(structure.left().orElseThrow());
                        } else if (structure.right() != null && structure.right().isPresent()) {
                            compiledCondition.structures.add(structure.right().orElseThrow().id());
                        }
                    }
                }
            }

            if (condition.isSlimeChunk() != null) {
                compiledCondition.slimeChunk = condition.isSlimeChunk();
            }

            if (condition instanceof AreaTypeSpawningCondition<?> areaCondition) {
                if (areaCondition.getMinHeight() != null) {
                    compiledCondition.area_minHeight = areaCondition.getMinHeight();
                }
                if (areaCondition.getMaxHeight() != null) {
                    compiledCondition.area_maxHeight = areaCondition.getMaxHeight();
                }
            }

            if (condition instanceof GroundedTypeSpawningCondition<?> groundedCondition) {
                if (groundedCondition.getNeededBaseBlocks() != null) {
                    for (RegistryLikeCondition<net.minecraft.block.Block> block : groundedCondition.getNeededBaseBlocks()) {
                        for (Block key : sc.server.getRegistryManager().get(RegistryKeys.BLOCK).stream().toList()) {
                            if (block.fits(key, sc.server.getRegistryManager().get(RegistryKeys.BLOCK))) {
                                compiledCondition.grounded_baseBlocks.add(sc.server.getRegistryManager().get(RegistryKeys.BLOCK).getEntry(key));
                            }
                        }
                    }
                }
            }

            if (condition instanceof SubmergedTypeSpawningCondition<?> submergedCondition) {
                if (submergedCondition.getMinDepth() != null) {
                    compiledCondition.sub_minDepth = submergedCondition.getMinDepth();
                }

                if (submergedCondition.getMaxDepth() != null) {
                    compiledCondition.sub_maxDepth = submergedCondition.getMaxDepth();
                }

                if (submergedCondition.getFluidIsSource() != null) {
                    compiledCondition.sub_fluidIsSource = submergedCondition.getFluidIsSource();
                }

                if (submergedCondition.getFluid() != null) {
                    for (Fluid key : sc.server.getRegistryManager().get(RegistryKeys.FLUID).stream().toList()) {
                        if (submergedCondition.getFluid().fits(key, sc.server.getRegistryManager().get(RegistryKeys.FLUID))) {
                            compiledCondition.sub_fluid = sc.server.getRegistryManager().get(RegistryKeys.FLUID).getEntry(key);
                            break;
                        }
                    }
                }
            }

            if (condition instanceof SurfaceTypeSpawningCondition<?> surfaceCondition) {
                if (surfaceCondition.getMinDepth() != null) {
                    compiledCondition.surface_minDepth = surfaceCondition.getMinDepth();
                }

                if (surfaceCondition.getMaxDepth() != null) {
                    compiledCondition.surface_maxDepth = surfaceCondition.getMaxDepth();
                }

                if (surfaceCondition.getFluid() != null) {
                    for (Fluid key : sc.server.getRegistryManager().get(RegistryKeys.FLUID).stream().toList()) {
                        if (surfaceCondition.getFluid().fits(key, sc.server.getRegistryManager().get(RegistryKeys.FLUID))) {
                            compiledCondition.surface_fluid = sc.server.getRegistryManager().get(RegistryKeys.FLUID).getEntry(key);
                            break;
                        }
                    }
                }
            }

            if (condition instanceof SeafloorTypeSpawningCondition<?> seafloorCondition) {
                if (seafloorCondition.getNeededBaseBlocks() != null) {
                    for (RegistryLikeCondition<net.minecraft.block.Block> block : seafloorCondition.getNeededBaseBlocks()) {
                        for (Block key : sc.server.getRegistryManager().get(RegistryKeys.BLOCK).stream().toList()) {
                            if (block.fits(key, sc.server.getRegistryManager().get(RegistryKeys.BLOCK))) {
                                compiledCondition.seafloor_baseBlocks.add(sc.server.getRegistryManager().get(RegistryKeys.BLOCK).getEntry(key));
                            }
                        }
                    }
                }
            }

            if (condition instanceof FishingSpawningCondition fishingCondition) {
                if (fishingCondition.getRod() != null) {
                    for (Item key : sc.server.getRegistryManager().get(RegistryKeys.ITEM).stream().toList()) {
                        if (fishingCondition.getRod().fits(key, sc.server.getRegistryManager().get(RegistryKeys.ITEM))) {
                            compiledCondition.fishing_rod = sc.server.getRegistryManager().get(RegistryKeys.ITEM).getEntry(key);
                        }
                    }
                }

                if (fishingCondition.getNeededNearbyBlocks() != null) {
                    for (RegistryLikeCondition<net.minecraft.block.Block> block : fishingCondition.getNeededNearbyBlocks()) {
                        for (Block key : sc.server.getRegistryManager().get(RegistryKeys.BLOCK).stream().toList()) {
                            if (block.fits(key, sc.server.getRegistryManager().get(RegistryKeys.BLOCK))) {
                                compiledCondition.fishing_nearbyBlocks.add(sc.server.getRegistryManager().get(RegistryKeys.BLOCK).getEntry(key));
                            }
                        }
                    }
                }

                if (fishingCondition.getMinLureLevel() != null) {
                    compiledCondition.fishing_minLureLevel = fishingCondition.getMinLureLevel();
                }

                if (fishingCondition.getMaxLureLevel() != null) {
                    compiledCondition.fishing_maxLureLevel = fishingCondition.getMaxLureLevel();
                }

                if (fishingCondition.getBait() != null) {
                    compiledCondition.fishing_bait = fishingCondition.getBait();
                }

                if (fishingCondition.getRodType() != null) {
                    compiledCondition.fishing_rodType = fishingCondition.getRodType();
                }
            }
        }

        int slot = 0;
        if (!compiledCondition.dimensions.isEmpty()) {
            List<Text> dimension_lore = new ArrayList<>();
            for (Identifier key : compiledCondition.dimensions) {
                dimension_lore.add(Text.literal("- " + key.toString()));
            }

            gui.setSlot(slot++, new GuiElementBuilder(Items.END_PORTAL_FRAME)
                    .setName(Text.literal("Dimensions"))
                    .setLore(dimension_lore)
                    .build());
        }

        if (!compiledCondition.biomes.isEmpty()) {
            gui.setSlot(slot++, new GuiElementBuilder(Items.OAK_SAPLING)
                    .setName(Text.literal("Biomes"))
                    .setLore(List.of(Text.literal("Click for more details!")))
                    .setCallback(clickType -> biomesMenu(player, compiledCondition.biomes, id, gui))
                    .build());
        }

        if (compiledCondition.moonPhaseRange != null) {
            gui.setSlot(slot++, new GuiElementBuilder(Items.END_STONE)
                    .setName(Text.literal("Moon Phase: " + compiledCondition.moonPhaseRange))
                    .build());
        }

        if (compiledCondition.canSeeSky != null) {
            gui.setSlot(slot++, new GuiElementBuilder(Items.DAYLIGHT_DETECTOR)
                    .setName(Text.literal("Can See Sky: " + compiledCondition.canSeeSky))
                    .build());
        }

        if (compiledCondition.minX != null || compiledCondition.maxX != null) {
            if (compiledCondition.minX == null) {
                compiledCondition.minX = Text.literal("Min X: null");
            }
            if (compiledCondition.maxX == null) {
                compiledCondition.maxX = Text.literal("Max X: null");
            }
            gui.setSlot(slot++, new GuiElementBuilder(Items.RED_WOOL)
                    .setName(Text.empty().append(compiledCondition.minX).append(" | ").append(compiledCondition.maxX))
                    .build());
        }

        if (compiledCondition.minY != null || compiledCondition.maxY != null) {
            if (compiledCondition.minY == null) {
                compiledCondition.minY = Text.literal("Min Y: null");
            }
            if (compiledCondition.maxY == null) {
                compiledCondition.maxY = Text.literal("Max Y: null");
            }
            gui.setSlot(slot++, new GuiElementBuilder(Items.LIME_WOOL)
                    .setName(Text.empty().append(compiledCondition.minY).append(" | ").append(compiledCondition.maxY))
                    .build());
        }

        if (compiledCondition.minZ != null || compiledCondition.maxZ != null) {
            if (compiledCondition.minZ == null) {
                compiledCondition.minZ = Text.literal("Max Y: null");
            }
            if (compiledCondition.maxZ == null) {
                compiledCondition.maxZ = Text.literal("Max Z: null");
            }

            gui.setSlot(slot++, new GuiElementBuilder(Items.LIGHT_BLUE_WOOL)
                    .setName(Text.empty().append(compiledCondition.minZ).append(" | ").append(compiledCondition.maxZ))
                    .build());
        }

        if (compiledCondition.minLight != null || compiledCondition.maxLight != null) {
            if (compiledCondition.minLight == null) {
                compiledCondition.minLight = Text.literal("Max Light: null");
            }
            if (compiledCondition.maxLight == null) {
                compiledCondition.maxLight = Text.literal("Max Light: null");
            }

            gui.setSlot(slot++, new GuiElementBuilder(Items.LIGHT)
                    .setName(Text.empty().append(compiledCondition.minLight).append(" | ").append(compiledCondition.maxLight))
                    .build());
        }

        if (compiledCondition.raining != null) {
            gui.setSlot(slot++, new GuiElementBuilder(Items.ICE)
                    .setName(Text.literal("Raining: " + compiledCondition.raining))
                    .build());
        }

        if (compiledCondition.thundering != null) {
            gui.setSlot(slot++, new GuiElementBuilder(Items.LIGHTNING_ROD)
                    .setName(Text.literal("Thundering: " + compiledCondition.thundering))
                    .build());
        }

        if (compiledCondition.timeRangeStart != null || compiledCondition.timeRangeEnd != null) {
            gui.setSlot(slot++, new GuiElementBuilder(Items.CLOCK)
                    .setName(Text.literal("Time Range: ").append(String.valueOf(compiledCondition.timeRangeStart)).append(" - ").append(String.valueOf(compiledCondition.timeRangeEnd)))
                    .build());
        }

        if (!compiledCondition.structures.isEmpty()) {
            gui.setSlot(slot++, new GuiElementBuilder(Items.MOSSY_STONE_BRICKS)
                    .setName(Text.literal("Structures"))
                    .setLore(List.of(Text.literal("Click for more details!")))
                    .setCallback(clickType -> {
                        structuresMenu(player, compiledCondition.structures, id, gui);
                    })
                    .build());
        }

        if (compiledCondition.slimeChunk != null) {
            gui.setSlot(slot++, new GuiElementBuilder(Items.SLIME_BLOCK)
                    .setName(Text.literal("Slime Chunk: " + compiledCondition.slimeChunk))
                    .build());
        }

        if (compiledCondition.area_minHeight != null || compiledCondition.area_maxHeight != null) {
            gui.setSlot(slot++, new GuiElementBuilder(Items.BEDROCK)
                    .setName(Text.empty().append("Min Height: " + compiledCondition.area_minHeight).append(" | Max Height: " + compiledCondition.area_maxHeight))
                    .build());
        }

        if (!compiledCondition.grounded_baseBlocks.isEmpty()) {
            gui.setSlot(slot++, new GuiElementBuilder(Items.STONE)
                    .setName(Text.literal("(Grounded) Base Blocks"))
                    .setLore(List.of(Text.literal("Click for more details!")))
                    .setCallback(clickType -> {
                        blocksMenu(player, compiledCondition.grounded_baseBlocks, id, gui);
                    })
                    .build());
        }

        if (compiledCondition.sub_minDepth != null || compiledCondition.sub_maxDepth != null) {
            gui.setSlot(slot++, new GuiElementBuilder(Items.PRISMARINE)
                    .setName(Text.literal("(Submerged) Min Depth: " + compiledCondition.sub_minDepth).append(" | Max Depth: " + compiledCondition.sub_maxDepth))
                    .build());
        }

        if (compiledCondition.sub_fluidIsSource != null) {
            gui.setSlot(slot++, new GuiElementBuilder(Items.HEART_OF_THE_SEA)
                    .setName(Text.literal("(Submerged) Fluid Is Source: " + compiledCondition.sub_fluidIsSource))
                    .build());
        }

        if (compiledCondition.sub_fluid != null) {
            gui.setSlot(slot++, new GuiElementBuilder(Items.WATER_BUCKET)
                    .setName(Text.literal("(Submerged) Fluid: " + compiledCondition.sub_fluid.getIdAsString()))
                    .build());
        }

        if (compiledCondition.surface_minDepth != null || compiledCondition.surface_maxDepth != null) {
            gui.setSlot(slot++, new GuiElementBuilder(Items.POINTED_DRIPSTONE)
                    .setName(Text.literal("(Surface) Min Depth: " + compiledCondition.surface_minDepth).append(" | Max Depth: " + compiledCondition.surface_maxDepth))
                    .build());
        }

        if (compiledCondition.surface_fluid != null) {
            gui.setSlot(slot++, new GuiElementBuilder(Items.WATER_BUCKET)
                    .setName(Text.literal("(Surface) Fluid: " + compiledCondition.surface_fluid.getIdAsString()))
                    .build());
        }

        if (!compiledCondition.seafloor_baseBlocks.isEmpty()) {
            gui.setSlot(slot++, new GuiElementBuilder(Items.STONE)
                    .setName(Text.literal("(Seafloor) Base Blocks"))
                    .setLore(List.of(Text.literal("Click for more details!")))
                    .setCallback(clickType -> {
                        blocksMenu(player, compiledCondition.seafloor_baseBlocks, id, gui);
                    })
                    .build());
        }

        if (compiledCondition.fishing_rod != null) {
            gui.setSlot(slot++, new GuiElementBuilder(Items.FISHING_ROD)
                    .setName(Text.literal("(Fishing) Rod: " + compiledCondition.fishing_rod.getIdAsString()))
                    .build());
        }

        if (!compiledCondition.fishing_nearbyBlocks.isEmpty()) {
            gui.setSlot(slot++, new GuiElementBuilder(Items.STONE)
                    .setName(Text.literal("(Fishing) Nearby Blocks"))
                    .setLore(List.of(Text.literal("Click for more details!")))
                    .setCallback(clickType -> {
                        blocksMenu(player, compiledCondition.fishing_nearbyBlocks, id, gui);
                    })
                    .build());
        }

        if (compiledCondition.fishing_minLureLevel != null || compiledCondition.fishing_maxLureLevel != null) {
            gui.setSlot(slot++, new GuiElementBuilder(Items.ENCHANTED_BOOK)
                    .setName(Text.literal("(Fishing) Min Lure Level: " + compiledCondition.fishing_minLureLevel).append(" | Max Lure Level: " + compiledCondition.fishing_maxLureLevel))
                    .build());
        }

        if (compiledCondition.fishing_bait != null) {
            gui.setSlot(slot++, new GuiElementBuilder(Items.COD)
                    .setName(Text.literal("(Fishing) Bait: " + compiledCondition.fishing_bait.toString()))
                    .build());
        }

        if (compiledCondition.fishing_rodType != null) {
            gui.setSlot(slot++, new GuiElementBuilder(Items.FISHING_ROD)
                    .setName(Text.literal("(Fishing) Rod Type: " + compiledCondition.fishing_rodType.toString()))
                    .build());
        }

        gui.setSlot(gui.getSize() - 5, new GuiElementBuilder(Items.BARRIER)
                .setName(Text.literal("Back"))
                .setCallback(clickType -> back_gui.open()));

        gui.open();
    }

    public void biomesMenu(ServerPlayerEntity player, Set<RegistryEntry<Biome>> biomes, String id, SimpleGui back_gui) {
        List<SimpleGui> gui_pages = new ArrayList<>();
        List<Set<RegistryEntry<Biome>>> pages = new ArrayList<>();
        pages.add(new HashSet<>());
        gui_pages.add(new SimpleGui(getGuiSize(biomes.size()), player, false));

        int slot = 0;
        int page = 0;
        for (RegistryEntry<Biome> biome : biomes) {
            pages.get(page).add(biome);
            slot++;
            if (slot % 45 == 0) {
                page++;
                pages.add(new HashSet<>());
                gui_pages.add(new SimpleGui(getGuiSize(page), player, false));
            }
        }

        page = 0;
        for (Set<RegistryEntry<Biome>> page_biomes : pages) {
            SimpleGui gui_page = new SimpleGui(getGuiSize(page_biomes.size()), player, false);
            gui_page.setTitle(Text.literal(id + " Biome Conditions"));

            slot = 0;
            for (RegistryEntry<Biome> biome : page_biomes) {
                Text biome_name = Text.literal(biome.getIdAsString());

                gui_page.setSlot(slot++, new GuiElementBuilder(Items.DIRT)
                        .setName(biome_name)
                        .build());
            }

            int finalPage = page;
            if (finalPage > 0 && gui_pages.get(finalPage - 1) != null) {
                gui_page.setSlot(gui_page.getSize() - 9, new GuiElementBuilder(Items.ARROW)
                        .setName(Text.literal("Previous"))
                        .setCallback(clickType -> gui_pages.get(finalPage - 1).open())
                        .build());
            }


            if (finalPage + 1 < gui_pages.size() && gui_pages.get(finalPage + 1) != null) {
                gui_page.setSlot(gui_page.getSize() - 1, new GuiElementBuilder(Items.ARROW)
                        .setName(Text.literal("Next"))
                        .setCallback(clickType -> gui_pages.get(finalPage + 1).open())
                        .build());
            }

            gui_page.setSlot(gui_page.getSize() - 5, new GuiElementBuilder(Items.BARRIER)
                    .setName(Text.literal("Back"))
                    .setCallback(clickType -> back_gui.open()));

            gui_pages.set(page, gui_page);
            page++;
        }

        gui_pages.getFirst().open();
    }

    public void blocksMenu(ServerPlayerEntity player, Set<RegistryEntry<Block>> blocks, String id, SimpleGui back_gui) {
        List<SimpleGui> gui_pages = new ArrayList<>();
        List<Set<RegistryEntry<Block>>> pages = new ArrayList<>();
        pages.add(new HashSet<>());
        gui_pages.add(new SimpleGui(getGuiSize(blocks.size()), player, false));

        int slot = 0;
        int page = 0;
        for (RegistryEntry<Block> block : blocks) {
            pages.get(page).add(block);
            slot++;
            if (slot % 45 == 0) {
                page++;
                pages.add(new HashSet<>());
                gui_pages.add(new SimpleGui(getGuiSize(page), player, false));
            }
        }

        page = 0;
        for (Set<RegistryEntry<Block>> page_blocks : pages) {
            SimpleGui gui_page = new SimpleGui(getGuiSize(page_blocks.size()), player, false);
            gui_page.setTitle(Text.literal(id + " Block Conditions"));

            slot = 0;
            for (RegistryEntry<Block> block : page_blocks) {
                Text block_name = Text.literal(block.getIdAsString());

                gui_page.setSlot(slot++, new GuiElementBuilder(block.value().asItem())
                        .setName(block_name)
                        .build());
            }

            int finalPage = page;
            if (finalPage > 0 && gui_pages.get(finalPage - 1) != null) {
                gui_page.setSlot(gui_page.getSize() - 9, new GuiElementBuilder(Items.ARROW)
                        .setName(Text.literal("Previous"))
                        .setCallback(clickType -> gui_pages.get(finalPage - 1).open())
                        .build());
            }


            if (finalPage + 1 < gui_pages.size() && gui_pages.get(finalPage + 1) != null) {
                gui_page.setSlot(gui_page.getSize() - 1, new GuiElementBuilder(Items.ARROW)
                        .setName(Text.literal("Next"))
                        .setCallback(clickType -> gui_pages.get(finalPage + 1).open())
                        .build());
            }

            gui_page.setSlot(gui_page.getSize() - 5, new GuiElementBuilder(Items.BARRIER)
                    .setName(Text.literal("Back"))
                    .setCallback(clickType -> back_gui.open()));

            gui_pages.set(page, gui_page);
            page++;
        }

        gui_pages.getFirst().open();
    }

    public void structuresMenu(ServerPlayerEntity player, List<Identifier> structures, String id, SimpleGui back_gui) {
        List<SimpleGui> gui_pages = new ArrayList<>();
        List<List<Identifier>> pages = new ArrayList<>();
        pages.add(new ArrayList<>());
        gui_pages.add(new SimpleGui(getGuiSize(structures.size()), player, false));

        int slot = 0;
        int page = 0;
        for (Identifier structure : structures) {
            pages.get(page).add(structure);
            slot++;
            if (slot % 45 == 0) {
                page++;
                pages.add(new ArrayList<>());
                gui_pages.add(new SimpleGui(getGuiSize(page), player, false));
            }
        }

        page = 0;
        for (List<Identifier> page_structures : pages) {
            SimpleGui gui_page = new SimpleGui(getGuiSize(page_structures.size()), player, false);
            gui_page.setTitle(Text.literal(id + " Block Conditions"));

            slot = 0;
            for (Identifier structure : page_structures) {
                Text structure_name = Text.literal(structure.toString());

                gui_page.setSlot(slot++, new GuiElementBuilder(Items.STONE_BRICKS)
                        .setName(structure_name)
                        .build());
            }

            int finalPage = page;
            if (finalPage > 0 && gui_pages.get(finalPage - 1) != null) {
                gui_page.setSlot(gui_page.getSize() - 9, new GuiElementBuilder(Items.ARROW)
                        .setName(Text.literal("Previous"))
                        .setCallback(clickType -> gui_pages.get(finalPage - 1).open())
                        .build());
            }


            if (finalPage + 1 < gui_pages.size() && gui_pages.get(finalPage + 1) != null) {
                gui_page.setSlot(gui_page.getSize() - 1, new GuiElementBuilder(Items.ARROW)
                        .setName(Text.literal("Next"))
                        .setCallback(clickType -> gui_pages.get(finalPage + 1).open())
                        .build());
            }

            gui_page.setSlot(gui_page.getSize() - 5, new GuiElementBuilder(Items.BARRIER)
                    .setName(Text.literal("Back"))
                    .setCallback(clickType -> back_gui.open()));

            gui_pages.set(page, gui_page);
            page++;
        }

        gui_pages.getFirst().open();
    }
}
