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

final class CentrifugeCategory extends MachineCategory {

    static final String UID = Tags.MOD_ID + ".centrifuge";

    CentrifugeCategory(final IGuiHelper helper, final ItemStack machine) {
        super(helper, "centrifuge", "tile.threng.machine.centrifuge.name", 1, machine);
    }
}
