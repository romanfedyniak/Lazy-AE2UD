/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.recipe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraftforge.oredict.OreIngredient;

import lazyae2.item.ItemMaterial;
import appeng.api.AEApi;
import appeng.api.definitions.IMaterials;

/**
 * Every recipe this mod's own machines run. The lists stay open: CraftTweaker and GroovyScript edit them.
 */
public final class LazyRecipes {

    private static final List<AggregatorRecipe> AGGREGATOR = new ArrayList<>();

    private LazyRecipes() {
    }

    public static List<AggregatorRecipe> aggregator() {
        return AGGREGATOR;
    }

    @Nullable
    public static AggregatorRecipe findAggregator(final List<ItemStack> slots) {
        for (final AggregatorRecipe recipe : AGGREGATOR) {
            if (recipe.matches(slots)) {
                return recipe;
            }
        }
        return null;
    }

    public static void registerDefaults() {
        final IMaterials materials = AEApi.instance().definitions().materials();

        // Steelmaking, the half that needs a machine
        add(Arrays.asList(ore("dustCoal"), ore("dustFluix"), ore("ingotIron")),
                ItemMaterial.Type.FLUIX_STEEL.newStack(1));
        add(Arrays.asList(ore("dustCoal"), ore("dustFluix"), ore("itemSilicon")),
                ItemMaterial.Type.STEEL_PROCESS_DUST.newStack(1));

        // The in-world recipes the machine exists to replace
        materials.fluixCrystal().maybeStack(2).ifPresent(fluix ->
                materials.certusQuartzCrystalCharged().maybeStack(1).ifPresent(charged ->
                        add(Arrays.asList(ore("gemQuartz"), ore("dustRedstone"), exact(charged)), fluix)));
        materials.skyDust().maybeStack(1).ifPresent(skyDust ->
                add(Arrays.asList(ore("gemDiamond"), exact(skyDust), ore("dustEnderPearl")),
                        ItemMaterial.Type.SPACE_GEM.newStack(1)));
        materials.skyDust().maybeStack(1).ifPresent(skyDust ->
                materials.matterBall().maybeStack(1).ifPresent(matterBall ->
                        add(Arrays.asList(exact(skyDust), exact(matterBall),
                                exact(ItemMaterial.Type.STEEL_PROCESS_DUST.newStack(1))),
                                ItemMaterial.Type.SPEC_CORE.newStack(1))));
    }

    private static void add(final List<Ingredient> inputs, final ItemStack output) {
        if (!output.isEmpty()) {
            AGGREGATOR.add(new AggregatorRecipe(inputs, output));
        }
    }

    private static Ingredient ore(final String name) {
        return new OreIngredient(name);
    }

    /**
     * One item and nothing else, damage included - the old mod matched its own materials this way.
     */
    private static Ingredient exact(final ItemStack stack) {
        return stack.isEmpty() ? Ingredient.EMPTY : Ingredient.fromStacks(stack);
    }
}
