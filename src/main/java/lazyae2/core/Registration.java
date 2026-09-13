/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.core;

import java.util.Collections;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;

import lazyae2.Tags;
import lazyae2.block.BlockMachine;
import lazyae2.item.ItemBlockMachine;
import lazyae2.item.ItemMaterial;
import lazyae2.item.ItemPartTerminal;
import lazyae2.part.PartLevelMaintainerTerminal;
import lazyae2.tile.TileAggregator;
import lazyae2.tile.TileCentrifuge;
import lazyae2.tile.TileEnergizer;
import lazyae2.tile.TileEtcher;
import lazyae2.tile.TileLevelMaintainer;
import lazyae2.tile.TilePau;
import appeng.api.AEApi;
import appeng.api.features.IInscriberRegistry;
import appeng.api.features.IWirelessTerminalModeRegistry;
import appeng.api.features.InscriberProcessType;
import appeng.api.upgrades.CardTrait;
import appeng.api.upgrades.CardTraits;
import appeng.api.upgrades.IUpgradeRegistry;

/**
 * The blocks and items of every machine the config leaves switched on. A machine switched off is never
 * registered, so it leaves nothing behind in the registries.
 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class Registration {

    public static final String MATERIAL = "material";
    public static final String MACHINE = "machine";
    public static final String TERMINAL = "level_maintainer_terminal";

    @Nullable
    public static ItemMaterial material;
    @Nullable
    public static BlockMachine machine;
    @Nullable
    public static ItemPartTerminal terminal;

    private Registration() {
    }

    @SubscribeEvent
    public static void registerBlocks(final RegistryEvent.Register<Block> event) {
        machine = new BlockMachine();
        machine.setRegistryName(Tags.MOD_ID, MACHINE);
        machine.setTranslationKey(Tags.MOD_ID + "." + MACHINE);
        event.getRegistry().register(machine);

        // The names the old mod's library gave its tiles, so a machine it placed loads into this one
        GameRegistry.registerTileEntity(TileAggregator.class, new ResourceLocation(Tags.MOD_ID, "TileAggregator"));
        GameRegistry.registerTileEntity(TileCentrifuge.class, new ResourceLocation(Tags.MOD_ID, "TileCentrifuge"));
        GameRegistry.registerTileEntity(TileEtcher.class, new ResourceLocation(Tags.MOD_ID, "TileEtcher"));
        GameRegistry.registerTileEntity(TileEnergizer.class, new ResourceLocation(Tags.MOD_ID, "TileEnergizer"));
        GameRegistry.registerTileEntity(TilePau.class, new ResourceLocation(Tags.MOD_ID, "TileFastCraftingBus"));
        GameRegistry.registerTileEntity(TileLevelMaintainer.class,
                new ResourceLocation(Tags.MOD_ID, "TileLevelMaintainer"));
    }

    @SubscribeEvent
    public static void registerItems(final RegistryEvent.Register<Item> event) {
        // The same registry name the old mod gave its materials, so a saved world keeps them
        material = new ItemMaterial();
        material.setRegistryName(Tags.MOD_ID, MATERIAL);
        material.setTranslationKey(Tags.MOD_ID + "." + MATERIAL);
        event.getRegistry().register(material);

        if (machine != null) {
            event.getRegistry().register(new ItemBlockMachine(machine).setRegistryName(machine.getRegistryName()));
        }

        if (LazyAE2Config.instance().isLevelMaintainerTerminalEnabled()) {
            terminal = new ItemPartTerminal();
            terminal.setRegistryName(Tags.MOD_ID, TERMINAL);
            terminal.setTranslationKey(Tags.MOD_ID + ".part." + TERMINAL);
            event.getRegistry().register(terminal);
        }
    }

    /**
     * What the terminal looks like on a cable, and the face the wireless terminal can be switched to. Both
     * are asked for while the mod is still starting: a model registered later is never baked, and a mode
     * registered later has nothing to unlock it.
     */
    public static void registerTerminal() {
        if (!LazyAE2Config.instance().isLevelMaintainerTerminalEnabled()) {
            return;
        }

        AEApi.instance().registries().partModels().registerModels(PartLevelMaintainerTerminal.MODEL_OFF,
                PartLevelMaintainerTerminal.MODEL_ON);

        final IWirelessTerminalModeRegistry modes = AEApi.instance().registries().wirelessTerminalModes();
        modes.register(new LevelMaintainerTerminalMode());
    }

    /**
     * Which cards fit which machine. Nothing goes in a machine until it is named here.
     */
    public static void registerUpgrades() {
        if (machine == null) {
            return;
        }
        final IUpgradeRegistry upgrades = AEApi.instance().registries().upgrades();
        final LazyAE2Config config = LazyAE2Config.instance();

        for (final BlockMachine.Type type : BlockMachine.Type.all()) {
            if (!type.isEnabled()) {
                continue;
            }
            final ItemStack host = new ItemStack(machine, 1, type.getMeta());
            if (type == BlockMachine.Type.PAU) {
                // It has no work to speed up; its cards buy rows of patterns, as an interface's do
                support(upgrades, CardTraits.PATTERN_EXPANSION, host, config.getPatternCards(type.getConfigKey()),
                        config.getPatternPoints(type.getConfigKey()));
            } else {
                support(upgrades, CardTraits.SPEED, host, config.getSpeedCards(type.getConfigKey()),
                        config.getSpeedPoints(type.getConfigKey()));
            }
        }
    }

    private static void support(final IUpgradeRegistry upgrades, final CardTrait trait, final ItemStack host,
            final int cards, final int points) {
        if (cards > 0) {
            upgrades.addTraitSupport(trait, host, cards);
        }
        if (points > 0) {
            upgrades.setTraitLimit(trait, host, points);
        }
    }

    public static void registerOres() {
        OreDictionary.registerOre("ingotFluixSteel", ItemMaterial.Type.FLUIX_STEEL.newStack(1));

        // AE2's grindstone builds its own recipe out of this: it pairs coal with whatever dustCoal names
        if (LazyAE2Config.instance().isCoalDustEnabled()) {
            OreDictionary.registerOre("dustCoal", ItemMaterial.Type.COAL_DUST.newStack(1));
        }
    }

    /**
     * Steelmaking: iron is plated in the Inscriber, and the plated ingot smelts into fluix steel. Everything
     * else a material is made in belongs to the machine that makes it.
     */
    public static void registerRecipes() {
        final IInscriberRegistry inscriber = AEApi.instance().registries().inscriber();
        AEApi.instance().definitions().materials().skyDust().maybeStack(1).ifPresent(skyDust ->
                inscriber.addRecipe(inscriber.builder()
                        .withProcessType(InscriberProcessType.PRESS)
                        .withInputs(Collections.singletonList(new ItemStack(Items.IRON_INGOT)))
                        .withTopOptional(Collections.singletonList(ItemMaterial.Type.STEEL_PROCESS_DUST.newStack(1)))
                        .withBottomOptional(Collections.singletonList(skyDust))
                        .withOutput(ItemMaterial.Type.STEEL_PROCESS_INGOT.newStack(1))
                        .build()));

        FurnaceRecipes.instance().addSmeltingRecipe(ItemMaterial.Type.STEEL_PROCESS_INGOT.newStack(1),
                ItemMaterial.Type.FLUIX_STEEL.newStack(1), 0F);
    }
}
