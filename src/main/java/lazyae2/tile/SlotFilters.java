/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.tile;

import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import appeng.util.inv.filter.IAEItemFilter;

/**
 * What automation may do with a machine's slots: put things in the ones a recipe is read from, and take them
 * out of the ones the machine fills. Never the other way around.
 */
public final class SlotFilters {

    public static final IAEItemFilter INSERT_ONLY = new Directed(true);
    public static final IAEItemFilter EXTRACT_ONLY = new Directed(false);

    private SlotFilters() {
    }

    private static final class Directed implements IAEItemFilter {

        private final boolean insert;

        Directed(final boolean insert) {
            this.insert = insert;
        }

        @Override
        public boolean allowInsert(final IItemHandler inv, final int slot, final ItemStack stack) {
            return this.insert;
        }

        @Override
        public boolean allowExtract(final IItemHandler inv, final int slot, final int amount) {
            return !this.insert;
        }
    }
}
