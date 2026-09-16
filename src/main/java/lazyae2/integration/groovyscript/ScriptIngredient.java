/*
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.integration.groovyscript;

import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;

import com.cleanroommc.groovyscript.api.IIngredient;

/**
 * A script's ingredient as a recipe holds it. It matches by asking the script's ingredient itself, since
 * {@link IIngredient#toMcIngredient()} forgets NBT and whatever else that ingredient checks.
 */
final class ScriptIngredient extends Ingredient {

    private final IIngredient ingredient;

    private ScriptIngredient(final IIngredient ingredient) {
        super(ingredient.getMatchingStacks());
        this.ingredient = ingredient;
    }

    static Ingredient of(final IIngredient ingredient) {
        // An empty slot, as the etcher's presses are when a script leaves them out
        return ingredient == IIngredient.EMPTY ? Ingredient.EMPTY : new ScriptIngredient(ingredient);
    }

    @Override
    public boolean apply(@Nullable final ItemStack stack) {
        return stack != null && this.ingredient.test(stack);
    }

    /** Whether a recipe's ingredient takes any item the script's ingredient names. */
    static boolean anyMatches(final IIngredient ingredient, final Ingredient recipeInput) {
        for (final ItemStack stack : ingredient.getMatchingStacks()) {
            if (recipeInput.apply(stack)) {
                return true;
            }
        }
        return false;
    }
}
