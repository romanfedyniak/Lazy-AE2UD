/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.recipe;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;

/**
 * What the Pulse Centrifuge makes: a crystal shaken into its purified form, and the few other things the old
 * mod ground down there.
 */
public final class PurifyRecipe extends SingleItemRecipe {

    public PurifyRecipe(final Ingredient input, final ItemStack output) {
        super(input, output);
    }
}
