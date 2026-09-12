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

import javax.annotation.Nullable;

import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeWrapper;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;

/**
 * One machine recipe as HEI reads it: every ingredient is a slot of its own, and each slot takes one item.
 */
final class MachineRecipe implements IRecipeWrapper {

    private static final int INFO_COLOR = 0x808080;

    private final List<Ingredient> inputs;
    private final ItemStack output;
    /** What this recipe costs, for the one machine that prices each of them separately. */
    @Nullable
    private final String info;

    MachineRecipe(final List<Ingredient> inputs, final ItemStack output) {
        this(inputs, output, null);
    }

    MachineRecipe(final List<Ingredient> inputs, final ItemStack output, @Nullable final String info) {
        this.inputs = inputs;
        this.output = output;
        this.info = info;
    }

    @Override
    public void drawInfo(final Minecraft minecraft, final int recipeWidth, final int recipeHeight, final int mouseX,
            final int mouseY) {
        if (this.info != null) {
            minecraft.fontRenderer.drawString(this.info,
                    (recipeWidth - minecraft.fontRenderer.getStringWidth(this.info)) / 2,
                    recipeHeight - minecraft.fontRenderer.FONT_HEIGHT, INFO_COLOR);
        }
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
