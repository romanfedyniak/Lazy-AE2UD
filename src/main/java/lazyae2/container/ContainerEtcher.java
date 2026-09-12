/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.container;

import net.minecraft.entity.player.InventoryPlayer;

import lazyae2.tile.TileEtcher;
import lazyae2.tile.TileProcessor;
import appeng.container.slot.AppEngSlot;
import appeng.container.slot.SlotOutput;

public final class ContainerEtcher extends ContainerProcessor {

    /** The two pressing agents sit one above the other, with the material between them. */
    public static final int AGENT_LEFT = 37;
    public static final int TOP_AGENT_TOP = 17;
    public static final int BOTTOM_AGENT_TOP = 53;
    public static final int MATERIAL_LEFT = 60;
    public static final int SLOT_TOP = 35;
    public static final int OUTPUT_LEFT = 120;

    public ContainerEtcher(final InventoryPlayer ip, final TileEtcher machine) {
        super(ip, machine);
    }

    @Override
    protected void setupSlots(final TileProcessor machine) {
        final TileEtcher etcher = (TileEtcher) machine;
        this.addSlotToContainer(new AppEngSlot(etcher.getInputSlots(), 0, AGENT_LEFT, TOP_AGENT_TOP));
        this.addSlotToContainer(new AppEngSlot(etcher.getInputSlots(), 1, AGENT_LEFT, BOTTOM_AGENT_TOP));
        this.addSlotToContainer(new AppEngSlot(etcher.getInputSlots(), 2, MATERIAL_LEFT, SLOT_TOP));
        this.addSlotToContainer(new SlotOutput(etcher.getOutputSlot(), 0, OUTPUT_LEFT, SLOT_TOP, -1));
    }
}
