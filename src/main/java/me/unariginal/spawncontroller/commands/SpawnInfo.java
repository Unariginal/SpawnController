package me.unariginal.spawncontroller.commands;

import com.cobblemon.mod.common.CobblemonItems;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.spawning.CobblemonSpawnPools;
import com.cobblemon.mod.common.api.spawning.condition.SpawningCondition;
import com.cobblemon.mod.common.api.spawning.detail.PokemonSpawnDetail;
import com.cobblemon.mod.common.api.spawning.detail.SpawnDetail;
import com.cobblemon.mod.common.command.argument.SpeciesArgumentType;
import com.cobblemon.mod.common.pokemon.Species;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class SpawnInfo extends LiteralArgumentBuilder<ServerCommandSource> {
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

    public int spawnDetails(CommandContext<ServerCommandSource> ctx) {
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
            if (spawnDetail.getId().substring(0, spawnDetail.getId().indexOf("-")).equalsIgnoreCase(species.getName())) {
                ctx.getSource().sendMessage(Text.literal("Spawn Detail ID: " + spawnDetail.getId()));
                spawnDetails.add(spawnDetail);
            }
        }

        ScreenHandlerType<GenericContainerScreenHandler> screenHandlerType = ScreenHandlerType.GENERIC_9X1;
        int size = (int) Math.ceil((double) spawnDetails.size() / 9);
        screenHandlerType = switch (size) {
            case 2 -> ScreenHandlerType.GENERIC_9X2;
            case 3 -> ScreenHandlerType.GENERIC_9X3;
            case 4 -> ScreenHandlerType.GENERIC_9X4;
            case 5 -> ScreenHandlerType.GENERIC_9X5;
            case 6 -> ScreenHandlerType.GENERIC_9X6;
            default -> screenHandlerType;
        };

        SimpleGui detail_selection = new SimpleGui(screenHandlerType, player, false);
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
                detailSections(player, spawnDetail);
            });

            detail_selection.setSlot(slot, builder.build());

            slot++;
            if (slot / 9 >= size) {
                break;
            }
        }

        detail_selection.open();

        return 1;
    }

    public void detailSections(ServerPlayerEntity player, SpawnDetail detail) {
        SimpleGui gui = new SimpleGui(ScreenHandlerType.GENERIC_9X2, player, false);
        gui.setTitle(Text.literal(detail.getId() + " Detail Information"));

        List<Text> label_lore = new ArrayList<>();
        for (String label : detail.getLabels()) {
            label_lore.add(Text.literal(" - " + label));
        }

        gui.setSlot(1, new GuiElementBuilder(Items.PAPER)
                .setName(Text.literal("Labels"))
                .setLore(label_lore)
                .build());

        if (detail instanceof PokemonSpawnDetail pokemonDetail) {
            if (pokemonDetail.getLevelRange() != null) {
                gui.setSlot(2, new GuiElementBuilder(CobblemonItems.RARE_CANDY)
                        .setName(Text.literal("Level Range: " + pokemonDetail.getLevelRange().getStart() + " - " + pokemonDetail.getLevelRange().getEndInclusive()))
                        .build());
            }
        }

        gui.setSlot(3, new GuiElementBuilder(Items.PAPER)
                .setName(Text.literal("Weight: " + detail.getWeight()))
                .build());

        gui.setSlot(4, new GuiElementBuilder(Items.PAPER)
                .setName(Text.literal("Context: " + detail.getContext().getName()))
                .build());

        gui.setSlot(5, new GuiElementBuilder(Items.PAPER)
                .setName(Text.literal("Context Weight: " + detail.getContext().getWeight()))
                .build());

        gui.setSlot(6, new GuiElementBuilder(Items.FEATHER)
                .setName(Text.literal("Weight Multiplier"))
                .setLore(List.of(Text.literal("Click for more details!")))
                .build());

        gui.setSlot(7, new GuiElementBuilder(Items.BUCKET)
                .setName(Text.literal("Bucket: " + detail.getBucket().getName()))
                .build());

        if (detail.getCompositeCondition() != null) {
            gui.setSlot(11, new GuiElementBuilder(Items.FEATHER)
                    .setName(Text.literal("Composite Conditions"))
                    .setLore(List.of(Text.literal("Click for more details!")))
                    .setCallback(clickType -> {
                        conditionsMenu(player, detail.getCompositeCondition().getConditions(), detail.getId() + " Composite Conditions");
                    })
                    .build());


            gui.setSlot(12, new GuiElementBuilder(Items.FEATHER)
                    .setName(Text.literal("Composite Anti-Conditions"))
                    .setLore(List.of(Text.literal("Click for more details!")))
                    .setCallback(clickType -> {
                        conditionsMenu(player, detail.getCompositeCondition().getAnticonditions(), detail.getId() + " Composite Anti-Conditions");
                    })
                    .build());
        }

        gui.setSlot(14, new GuiElementBuilder(Items.FEATHER)
                .setName(Text.literal("Conditions"))
                .setLore(List.of(Text.literal("Click for more details!")))
                .setCallback(clickType -> {
                    conditionsMenu(player, detail.getConditions(), detail.getId() + " Conditions");
                })
                .build());

        gui.setSlot(15, new GuiElementBuilder(Items.FEATHER)
                .setName(Text.literal("Anti-Conditions"))
                .setLore(List.of(Text.literal("Click for more details!")))
                .setCallback(clickType -> {
                    conditionsMenu(player, detail.getAnticonditions(), detail.getId() + " Anti-Conditions");
                })
                .build());

        gui.open();
    }

    public void conditionsMenu(ServerPlayerEntity player, List<SpawningCondition<?>> conditions, String title) {
        SimpleGui gui = new SimpleGui(ScreenHandlerType.GENERIC_9X3, player, false);
        gui.setTitle(Text.literal(title));

        for (SpawningCondition<?> condition : conditions) {
            int slot = 0;

            List<Text> dimension_lore = new ArrayList<>();
            if (condition.getDimensions() != null) {
                for (Identifier dimension : condition.getDimensions()) {
                    dimension_lore.add(Text.literal(" - " + dimension.toString()));
                }

                gui.setSlot(slot, new GuiElementBuilder(Items.END_PORTAL_FRAME)
                        .setName(Text.literal("Dimensions"))
                        .setLore(dimension_lore)
                        .build());
                slot++;
            }

            if (condition.getBiomes() != null) {
                gui.setSlot(slot, new GuiElementBuilder(Items.OAK_SAPLING)
                        .setName(Text.literal("Biomes"))
                        .setLore(List.of(Text.literal("Click for more details!")))
                        .build());
                slot++;
            }

            if (condition.getMoonPhase() != null) {
                gui.setSlot(slot, new GuiElementBuilder(Items.END_STONE)
                        .setName(Text.literal("Moon Phase: " + condition.getMoonPhase()))
                        .build());
                slot++;
            }

            if (condition.getCanSeeSky() != null) {
                gui.setSlot(slot, new GuiElementBuilder(Items.DAYLIGHT_DETECTOR)
                        .setName(Text.literal("Can See Sky: " + condition.getCanSeeSky()))
                        .build());
                slot++;
            }

            Text name = Text.empty();
            if (condition.getMinX() != null) {
                name = Text.literal("Min X: " + condition.getMinX());
            }
            if (condition.getMaxX() != null) {
                name = Text.empty().append(name).append("Max X: " + condition.getMaxX());
            }

            if (!name.equals(Text.empty())) {
                gui.setSlot(slot, new GuiElementBuilder(Items.RED_WOOL)
                        .setName(name)
                        .build());
                slot++;
            }

            name = Text.empty();
            if (condition.getMinY() != null) {
                name = Text.literal("Min Y: " + condition.getMinY());
            }
            if (condition.getMaxY() != null) {
                name = Text.empty().append(name).append("Max Y: " + condition.getMaxY());
            }

            if (!name.equals(Text.empty())) {
                gui.setSlot(slot, new GuiElementBuilder(Items.LIME_WOOL)
                        .setName(name)
                        .build());
                slot++;
            }

            name = Text.empty();
            if (condition.getMinZ() != null) {
                name = Text.literal("Min Z: " + condition.getMinZ());
            }
            if (condition.getMaxZ() != null) {
                name = Text.empty().append(name).append("Max Z: " + condition.getMaxZ());
            }

            if (!name.equals(Text.empty())) {
                gui.setSlot(slot, new GuiElementBuilder(Items.LIGHT_BLUE_WOOL)
                        .setName(name)
                        .build());
                slot++;
            }
        }
        gui.open();
    }
}
