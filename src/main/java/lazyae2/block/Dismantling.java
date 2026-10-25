/*
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.block;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import appeng.util.Platform;

/**
 * A sneak-click with a wrench takes a block apart as AE2UD's own blocks are: the block and what it held go into the
 * player's inventory, whatever does not fit at their feet. It used to drop everything on the ground.
 */
final class Dismantling {

    private static final ThreadLocal<EntityPlayer> PLAYER = new ThreadLocal<>();

    private Dismantling() {
    }

    static void dismantle(final Block block, final World world, final BlockPos pos, final IBlockState state,
            final EntityPlayer player) {
        final NonNullList<ItemStack> drops = NonNullList.create();
        block.getDrops(drops, world, pos, state, 0);
        PLAYER.set(player);
        try {
            world.setBlockToAir(pos);
        } finally {
            PLAYER.remove();
        }
        Platform.givePlayerDrops(player, drops);
    }

    /**
     * What a broken block held: to the dismantling player, or on the ground when it was broken any other way.
     */
    static void drop(final World world, final BlockPos pos, final List<ItemStack> drops) {
        final EntityPlayer player = PLAYER.get();
        if (player != null) {
            Platform.givePlayerDrops(player, drops);
            return;
        }
        for (final ItemStack drop : drops) {
            Block.spawnAsEntity(world, pos, drop);
        }
    }
}
