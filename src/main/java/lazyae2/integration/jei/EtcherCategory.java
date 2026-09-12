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
import lazyae2.recipe.EtchRecipe;

final class EtcherCategory extends MachineCategory {

    static final String UID = Tags.MOD_ID + ".etcher";

    EtcherCategory(final IGuiHelper helper, final ItemStack machine) {
        super(helper, "etcher", "tile.threng.machine.etcher.name", EtchRecipe.SLOTS, machine);
    }
}
