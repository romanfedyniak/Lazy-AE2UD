/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.item;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import lazyae2.core.LazyAE2Tab;
import lazyae2.part.PartLevelMaintainerTerminal;
import appeng.api.AEApi;
import appeng.api.parts.IPartItem;

/**
 * The terminal as it is carried and placed: a part, so it goes on a cable the way every other terminal does.
 */
public final class ItemPartTerminal extends Item implements IPartItem<PartLevelMaintainerTerminal> {

    public ItemPartTerminal() {
        this.setCreativeTab(LazyAE2Tab.INSTANCE);
    }

    @Nullable
    @Override
    public PartLevelMaintainerTerminal createPartFromItemStack(final ItemStack is) {
        return new PartLevelMaintainerTerminal(is);
    }

    @Override
    public EnumActionResult onItemUse(final EntityPlayer player, final World world, final BlockPos pos,
            final EnumHand hand, final EnumFacing side, final float hitX, final float hitY, final float hitZ) {
        return AEApi.instance().partHelper().placeBus(player.getHeldItem(hand), pos, side, player, hand, world);
    }
}
