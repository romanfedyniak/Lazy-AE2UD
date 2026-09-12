/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.recipe;

import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;

/**
 * What the Fluix Aggregator makes: the in-world crafting of three things dropped in a puddle, done in a
 * machine instead.
 */
public final class AggregatorRecipe extends TriItemRecipe {

    public AggregatorRecipe(final List<Ingredient> inputs, final ItemStack output) {
        super(inputs, output);
    }
}
