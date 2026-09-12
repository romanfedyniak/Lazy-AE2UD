/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;

import lazyae2.tile.TilePau;
import appeng.api.config.SecurityPermissions;
import appeng.container.AEBaseContainer;
import appeng.container.guisync.GuiSync;
import appeng.container.slot.AppEngSlot;
import appeng.container.slot.IOptionalSlotHost;
import appeng.container.slot.OptionalSlotRestrictedInput;
import appeng.container.slot.SlotRestrictedInput;
import appeng.util.Platform;

/**
 * The unit's window: what it holds for the machine next door, what came back from it, and the patterns it
 * offers the network.
 */
public final class ContainerPau extends AEBaseContainer implements IMachineContainer, IOptionalSlotHost {

    public static final int HEIGHT = 258;

    public static final int BUFFER_TOP = 31;
    public static final int EXPORT_LEFT = 8;
    public static final int IMPORT_LEFT = 98;
    public static final int PATTERN_LEFT = 8;
    public static final int PATTERN_TOP = 89;
    public static final int UPGRADE_LEFT = 187;
    public static final int UPGRADE_TOP = 8;

    private final TilePau machine;

    @GuiSync(20)
    public int patternRows = 1;

    public ContainerPau(final InventoryPlayer ip, final TilePau machine) {
        super(ip, machine, null);
        this.machine = machine;

        for (int slot = 0; slot < TilePau.UPGRADE_SLOTS; slot++) {
            this.addSlotToContainer(new PatternAwareUpgradeSlot(slot, ip).setNotDraggable());
        }

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                final int slot = row * 3 + column;
                this.addSlotToContainer(new AppEngSlot(machine.getExports(), slot,
                        EXPORT_LEFT + column * 18, BUFFER_TOP + row * 18));
                this.addSlotToContainer(new AppEngSlot(machine.getImports(), slot,
                        IMPORT_LEFT + column * 18, BUFFER_TOP + row * 18));
            }
        }

        for (int row = 0; row < TilePau.PATTERN_SLOTS / 9; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlotToContainer(new OptionalSlotRestrictedInput(
                        SlotRestrictedInput.PlacableItemType.ENCODED_PATTERN, machine.getPatterns(), this,
                        column + row * 9, PATTERN_LEFT + 18 * column, PATTERN_TOP + 18 * row, row, ip)
                                .setStackLimit(1));
            }
        }

        this.bindPlayerInventory(ip, 0, HEIGHT - 82);
    }

    @Override
    public TilePau getMachine() {
        return this.machine;
    }

    /**
     * The first row is always there; each further row waits for the card that pays for it. The client goes
     * by the synced count, which is a tick behind, while the server asks the machine itself.
     */
    @Override
    public boolean isSlotEnabled(final int row) {
        final int rows = Platform.isServer() ? this.machine.getUsablePatternSlots() / 9 : this.patternRows;
        return rows > row;
    }

    @Override
    public void detectAndSendChanges() {
        this.verifyPermissions(SecurityPermissions.BUILD, false);
        this.patternRows = this.machine.getUsablePatternSlots() / 9;
        super.detectAndSendChanges();
    }

    /**
     * As everywhere else, except that a Pattern Expansion Card will not come out from under its patterns.
     */
    private final class PatternAwareUpgradeSlot extends SlotRestrictedInput {

        PatternAwareUpgradeSlot(final int index, final InventoryPlayer ip) {
            super(PlacableItemType.UPGRADES, ContainerPau.this.machine.getUpgradeInventory(), index,
                    UPGRADE_LEFT, UPGRADE_TOP + index * 18, ip);
        }

        @Override
        public boolean canTakeStack(final EntityPlayer player) {
            return super.canTakeStack(player)
                    && ContainerPau.this.machine.canRemoveUpgrade(this.getSlotIndex());
        }
    }
}
