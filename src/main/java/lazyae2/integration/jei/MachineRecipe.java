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

/**
 * One machine recipe as HEI reads it: every ingredient is a slot of its own, and each slot takes one item.
 */
final class MachineRecipe implements IRecipeWrapper {

    private final List<Ingredient> inputs;
    private final ItemStack output;

    MachineRecipe(final List<Ingredient> inputs, final ItemStack output) {
        this.inputs = inputs;
        this.output = output;
    }

    @Override
    public void getIngredients(final IIngredients ingredients) {
        final List<List<ItemStack>> stacks = new ArrayList<>(this.inputs.size());
        for (final Ingredient ingredient : this.inputs) {
            stacks.add(Arrays.asList(ingredient.getMatchingStacks()));
        }
        ingredients.setInputLists(VanillaTypes.ITEM, stacks);
        ingredients.setOutput(VanillaTypes.ITEM, this.output);
    }
}
