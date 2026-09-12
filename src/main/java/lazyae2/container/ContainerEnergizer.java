/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.container;

import net.minecraft.entity.player.InventoryPlayer;

import lazyae2.tile.TileEnergizer;
import lazyae2.tile.TileProcessor;
import appeng.container.slot.AppEngSlot;
import appeng.container.slot.SlotOutput;

public final class ContainerEnergizer extends ContainerProcessor {

    public static final int INPUT_LEFT = 56;
    public static final int OUTPUT_LEFT = 116;
    public static final int SLOT_TOP = 35;

    public ContainerEnergizer(final InventoryPlayer ip, final TileEnergizer machine) {
        super(ip, machine);
    }

    @Override
    protected void setupSlots(final TileProcessor machine) {
        final TileEnergizer energizer = (TileEnergizer) machine;
        this.addSlotToContainer(new AppEngSlot(energizer.getInputSlot(), 0, INPUT_LEFT, SLOT_TOP));
        this.addSlotToContainer(new SlotOutput(energizer.getOutputSlot(), 0, OUTPUT_LEFT, SLOT_TOP, -1));
    }
}
