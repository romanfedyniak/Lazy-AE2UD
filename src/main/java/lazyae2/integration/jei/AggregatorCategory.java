/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.integration.jei;

import mezz.jei.api.IGuiHelper;

import net.minecraft.item.ItemStack;

import lazyae2.Tags;
import lazyae2.recipe.TriItemRecipe;

final class AggregatorCategory extends MachineCategory {

    static final String UID = Tags.MOD_ID + ".aggregator";

    AggregatorCategory(final IGuiHelper helper, final ItemStack machine) {
        super(helper, "aggregator", "tile.threng.machine.aggregator.name", TriItemRecipe.SLOTS, machine);
    }
}
