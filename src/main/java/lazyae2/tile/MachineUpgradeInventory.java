/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.tile;

import net.minecraft.item.ItemStack;

import lazyae2.block.BlockMachine;
import lazyae2.core.Registration;
import appeng.parts.automation.UpgradeInventory;
import appeng.util.inv.IAEAppEngInventory;

/**
 * The upgrade slots of one machine. Which cards fit is asked of AE2's registry, and the machines share a
 * block, so the answer has to name the subtype rather than the block.
 */
public final class MachineUpgradeInventory extends UpgradeInventory {

    private final BlockMachine.Type type;

    public MachineUpgradeInventory(final BlockMachine.Type type, final IAEAppEngInventory parent, final int slots) {
        super(parent, slots);
        this.type = type;
    }

    @Override
    public ItemStack getUpgradableItem() {
        return Registration.machine == null ? ItemStack.EMPTY : new ItemStack(Registration.machine, 1, this.type.ordinal());
    }
}
