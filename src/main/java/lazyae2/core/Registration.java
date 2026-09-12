/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.core;

import java.util.Collections;

import javax.annotation.Nullable;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.oredict.OreDictionary;

import lazyae2.Tags;
import lazyae2.item.ItemMaterial;
import appeng.api.AEApi;
import appeng.api.features.IInscriberRegistry;
import appeng.api.features.InscriberProcessType;

/**
 * The blocks and items of every machine the config leaves switched on. A machine switched off is never
 * registered, so it leaves nothing behind in the registries.
 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class Registration {

    public static final String MATERIAL = "material";

    @Nullable
    public static ItemMaterial material;

    private Registration() {
    }

    @SubscribeEvent
    public static void registerItems(final RegistryEvent.Register<Item> event) {
        // The same registry name the old mod gave its materials, so a saved world keeps them
        material = new ItemMaterial();
        material.setRegistryName(Tags.MOD_ID, MATERIAL);
        material.setTranslationKey(Tags.MOD_ID + "." + MATERIAL);
        event.getRegistry().register(material);
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
