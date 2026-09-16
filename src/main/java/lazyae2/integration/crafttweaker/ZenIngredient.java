/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.integration.crafttweaker;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;

import crafttweaker.api.item.IIngredient;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import crafttweaker.mc1120.item.MCItemStack;

/**
 * A script's ingredient as a recipe holds it, matching by asking the script's ingredient itself, so a condition
 * or NBT the script put on it still counts.
 */
final class ZenIngredient extends Ingredient {

    private final IIngredient ingredient;

    private ZenIngredient(final IIngredient ingredient, final ItemStack[] shown) {
        super(shown);
        this.ingredient = ingredient;
    }

    static Ingredient of(final IIngredient ingredient) {
        final List<ItemStack> shown = new ArrayList<>();
        for (final IItemStack item : ingredient.getItems()) {
            if (item instanceof MCItemStack) {
                shown.add(CraftTweakerMC.getItemStack(item));
            }
        }
        if (shown.isEmpty()) {
            throw new IllegalArgumentException("Bad CraftTweaker item ingredient: " + ingredient);
        }
        return new ZenIngredient(ingredient, shown.toArray(new ItemStack[0]));
    }

    @Override
    public boolean apply(@Nullable final ItemStack stack) {
        return stack != null && !stack.isEmpty() && this.ingredient.matches(CraftTweakerMC.getIItemStack(stack));
    }
}
