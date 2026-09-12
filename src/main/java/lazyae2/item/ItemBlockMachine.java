/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.item;

import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

import lazyae2.Tags;
import lazyae2.block.BlockMachine;

/**
 * The machines as items: one item with a subtype each, like the block it places.
 */
public final class ItemBlockMachine extends ItemBlock {

    public ItemBlockMachine(final BlockMachine block) {
        super(block);
        this.setHasSubtypes(true);
    }

    @Override
    public int getMetadata(final int damage) {
        return damage;
    }

    @Override
    public String getTranslationKey(final ItemStack stack) {
        return "tile." + Tags.MOD_ID + ".machine." + BlockMachine.Type.of(stack.getMetadata()).getName();
    }
}
