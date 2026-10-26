/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.item;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import lazyae2.Tags;
import lazyae2.block.BlockMachine;
import appeng.me.helpers.IGridProxyable;

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

    /** The block's network node belongs to whoever placed it, as AE2's own blocks do; the Security Station asks. */
    @Override
    public boolean placeBlockAt(final ItemStack stack, final EntityPlayer player, final World world, final BlockPos pos,
            final EnumFacing side, final float hitX, final float hitY, final float hitZ, final IBlockState newState) {
        if (!super.placeBlockAt(stack, player, world, pos, side, hitX, hitY, hitZ, newState)) {
            return false;
        }

        final TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof IGridProxyable) {
            ((IGridProxyable) tile).getProxy().setOwner(player);
        }
        return true;
    }
}
