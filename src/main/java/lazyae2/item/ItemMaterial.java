/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.item;

import java.util.Locale;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

import lazyae2.Tags;
import lazyae2.core.LazyAE2Config;
import lazyae2.core.LazyAE2Tab;
import lazyae2.core.Registration;

/**
 * Every material the mod has, as one item with a subtype each. The order is the one the old mod used: a
 * subtype's place in it is what a saved world holds.
 */
public final class ItemMaterial extends Item {

    public enum Type {
        FLUIX_STEEL,
        STEEL_PROCESS_DUST,
        STEEL_PROCESS_INGOT,
        COAL_DUST,
        MACHINE_CORE,
        SPACE_GEM,
        PARALLEL_PROCESSOR,
        SPEC_CORE,
        SPEC_CORE_2,
        SPEC_CORE_4,
        SPEC_CORE_8,
        SPEC_CORE_16,
        SPEC_CORE_32,
        SPEC_CORE_64,
        SPEC_PROCESSOR;

        private static final Type[] VALUES = values();

        public String getName() {
            return this.name().toLowerCase(Locale.ROOT);
        }

        public ItemStack newStack(final int count) {
            return Registration.material == null ? ItemStack.EMPTY : new ItemStack(Registration.material, count, this.ordinal());
        }

        public static Type fromMeta(final int meta) {
            return meta >= 0 && meta < VALUES.length ? VALUES[meta] : FLUIX_STEEL;
        }

        public static Type[] all() {
            return VALUES;
        }
    }

    public ItemMaterial() {
        this.setHasSubtypes(true);
        this.setCreativeTab(LazyAE2Tab.INSTANCE);
    }

    @Override
    public String getTranslationKey(final ItemStack stack) {
        return "item." + Tags.MOD_ID + ".material." + Type.fromMeta(stack.getMetadata()).getName();
    }

    @Override
    public void getSubItems(final CreativeTabs tab, final NonNullList<ItemStack> items) {
        if (this.isInCreativeTab(tab)) {
            for (final Type type : Type.all()) {
                // A stack of ours already in a world still works; it is only no longer offered
                if (type != Type.COAL_DUST || LazyAE2Config.instance().isCoalDustEnabled()) {
                    items.add(new ItemStack(this, 1, type.ordinal()));
                }
            }
        }
    }
}
