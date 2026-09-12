/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.container;

import net.minecraft.entity.player.InventoryPlayer;

import lazyae2.recipe.TriItemRecipe;
import lazyae2.tile.TileAggregator;
import lazyae2.tile.TileProcessor;
import appeng.container.slot.AppEngSlot;
import appeng.container.slot.SlotOutput;

public final class ContainerAggregator extends ContainerProcessor {

    public static final int INPUT_LEFT = 28;
    public static final int SLOT_TOP = 35;
    public static final int OUTPUT_LEFT = 128;

    public ContainerAggregator(final InventoryPlayer ip, final TileAggregator machine) {
        super(ip, machine);
    }

    @Override
    protected void setupSlots(final TileProcessor machine) {
        final TileAggregator aggregator = (TileAggregator) machine;
        for (int slot = 0; slot < TriItemRecipe.SLOTS; slot++) {
            this.addSlotToContainer(new AppEngSlot(aggregator.getInputSlots(), slot, INPUT_LEFT + slot * 20, SLOT_TOP));
        }
        this.addSlotToContainer(new SlotOutput(aggregator.getOutputSlot(), 0, OUTPUT_LEFT, SLOT_TOP, -1));
    }
}
