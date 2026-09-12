/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.core;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import lazyae2.Tags;
import lazyae2.item.ItemMaterial;

/**
 * The mod's own creative tab, as the old mod had.
 */
public final class LazyAE2Tab extends CreativeTabs {

    public static final LazyAE2Tab INSTANCE = new LazyAE2Tab();

    private LazyAE2Tab() {
        super(Tags.MOD_ID);
    }

    @Override
    public ItemStack createIcon() {
        final ItemStack icon = ItemMaterial.Type.FLUIX_STEEL.newStack(1);
        return icon.isEmpty() ? new ItemStack(Items.IRON_INGOT) : icon;
    }
}
