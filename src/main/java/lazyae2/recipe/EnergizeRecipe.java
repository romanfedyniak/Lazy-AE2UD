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
 * What the Crystal Energizer makes: a crystal held in a field until it is charged. Each recipe says how much
 * energy that takes, rather than the machine charging a flat price like the others.
 */
public final class EnergizeRecipe extends SingleItemRecipe {

    private final int energy;

    public EnergizeRecipe(final Ingredient input, final int energy, final ItemStack output) {
        super(input, output);
        this.energy = energy;
    }

    /**
     * The whole cost of one charge, in FE, spread over the ticks the machine takes.
     */
    public int getEnergy() {
        return this.energy;
    }
}
