/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import lazyae2.tile.TileLevelMaintainer;
import appeng.api.stacks.AEKey;
import appeng.container.implementations.ContainerSetAmount;
import appeng.core.sync.GuiBridge;
import appeng.core.sync.packets.PacketSwitchGuis;
import appeng.helpers.IAmountTarget;

/**
 * How much one row of the maintainer keeps, typed into AE2's own number screen.
 * <p>
 * The window shows what the row watches rather than the machine, so the amount is typed in that thing's own
 * units - buckets for a fluid - exactly as a level emitter's threshold is.
 */
final class MaintainerAmount implements IAmountTarget {

    private final TileLevelMaintainer machine;
    private final int row;
    private final GuiBridge origin;

    MaintainerAmount(final TileLevelMaintainer machine, final int row, final GuiBridge origin) {
        this.machine = machine;
        this.row = row;
        this.origin = origin;
    }

    @Override
    public ItemStack getIcon() {
        final AEKey what = this.machine.keyOf(this.row);
        return what == null ? ItemStack.EMPTY : what.wrapForDisplayOrFilter();
    }

    @Override
    public long getAmount() {
        return this.machine.getTarget(this.row);
    }

    @Override
    public long getMinAmount() {
        return 1;
    }

    @Override
    public long getMaxAmount() {
        return Long.MAX_VALUE;
    }

    @Override
    public void apply(final EntityPlayer player, final ContainerSetAmount from, final long amount) {
        this.machine.setTarget(this.row, amount);
        PacketSwitchGuis.reopen(player, from, this.origin);
    }
}
