/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.integration.jei;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeWrapper;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;

import lazyae2.recipe.TriItemRecipe;

/**
 * One machine recipe as HEI reads it: every ingredient is a slot of its own, and each slot takes one item.
 */
final class AggregatorRecipeWrapper implements IRecipeWrapper {

    private final TriItemRecipe recipe;

    AggregatorRecipeWrapper(final TriItemRecipe recipe) {
        this.recipe = recipe;
    }

    int inputCount() {
        return this.recipe.getInputs().size();
    }

    @Override
    public void getIngredients(final IIngredients ingredients) {
        final List<List<ItemStack>> inputs = new ArrayList<>(this.recipe.getInputs().size());
        for (final Ingredient ingredient : this.recipe.getInputs()) {
            inputs.add(Arrays.asList(ingredient.getMatchingStacks()));
        }
        ingredients.setInputLists(VanillaTypes.ITEM, inputs);
        ingredients.setOutput(VanillaTypes.ITEM, this.recipe.getOutput());
    }
}
