/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.container;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import lazyae2.core.ModGuiBridges;
import lazyae2.tile.RowState;
import lazyae2.tile.TileLevelMaintainer;
import appeng.api.config.SecurityPermissions;
import appeng.api.stacks.AEKey;
import appeng.container.AEBaseContainer;
import appeng.container.guisync.GuiSync;
import appeng.container.slot.SlotFakeTypeOnly;
import appeng.core.sync.GuiBridge;
import appeng.helpers.IAmountTarget;
import appeng.helpers.InventoryAction;
import appeng.util.Platform;

import javax.annotation.Nullable;

/**
 * The maintainer's window: a row for each thing to keep in stock, with what to keep and how much to order.
 * <p>
 * The two numbers of each row are synced one field at a time rather than as arrays, which is all the sync
 * this base offers.
 */
public final class ContainerLevelMaintainer extends AEBaseContainer implements IMachineContainer {

    public static final int HEIGHT = 214;

    public static final int FILTER_LEFT = 23;
    public static final int ROW_TOP = 19;
    public static final int ROW_HEIGHT = 20;

    private final TileLevelMaintainer machine;

    @GuiSync(20)
    public long target0;
    @GuiSync(21)
    public long target1;
    @GuiSync(22)
    public long target2;
    @GuiSync(23)
    public long target3;
    @GuiSync(24)
    public long target4;

    @GuiSync(25)
    public long batch0 = 1;
    @GuiSync(26)
    public long batch1 = 1;
    @GuiSync(27)
    public long batch2 = 1;
    @GuiSync(28)
    public long batch3 = 1;
    @GuiSync(29)
    public long batch4 = 1;

    /** Which rows are switched on, one bit each. */
    @GuiSync(30)
    public int enabledRows;

    /**
     * What each row is doing, three bits each. Zero is {@link RowState#NONE}, so the frames drawn before the
     * first answer arrives say nothing rather than claiming the rows are stocked.
     */
    @GuiSync(31)
    public int rowStates;

    public ContainerLevelMaintainer(final InventoryPlayer ip, final TileLevelMaintainer machine) {
        super(ip, machine, null);
        this.machine = machine;

        if (Platform.isServer()) {
            // Whatever the machine had settled on, it looks again now that somebody is watching
            machine.retryNow();
        }

        for (int row = 0; row < TileLevelMaintainer.ROWS; row++) {
            this.addSlotToContainer(new SlotFakeTypeOnly(machine.getRequests(), row, FILTER_LEFT,
                    ROW_TOP + ROW_HEIGHT * row));
        }

        this.bindPlayerInventory(ip, 0, HEIGHT - 82);
    }

    @Override
    public TileLevelMaintainer getMachine() {
        return this.machine;
    }

    public long getTarget(final int row) {
        switch (row) {
            case 0:
                return this.target0;
            case 1:
                return this.target1;
            case 2:
                return this.target2;
            case 3:
                return this.target3;
            default:
                return this.target4;
        }
    }

    /**
     * What the window shows for a row until the server has caught up with what was asked of it. The server
     * writes these fields from the machine every tick, so its word is the one that lasts.
     */
    public void showBatch(final int row, final long batch) {
        switch (row) {
            case 0:
                this.batch0 = batch;
                break;
            case 1:
                this.batch1 = batch;
                break;
            case 2:
                this.batch2 = batch;
                break;
            case 3:
                this.batch3 = batch;
                break;
            default:
                this.batch4 = batch;
        }
    }

    public void showRowEnabled(final int row, final boolean on) {
        this.enabledRows = on ? this.enabledRows | 1 << row : this.enabledRows & ~(1 << row);
    }

    public RowState getRowState(final int row) {
        return RowState.byIndex(this.rowStates >> row * 3 & 7);
    }

    public boolean isRowEnabled(final int row) {
        return (this.enabledRows & 1 << row) != 0;
    }

    public long getBatch(final int row) {
        switch (row) {
            case 0:
                return this.batch0;
            case 1:
                return this.batch1;
            case 2:
                return this.batch2;
            case 3:
                return this.batch3;
            default:
                return this.batch4;
        }
    }

    /**
     * Which window a player typing an amount is sent back to. Ours is no screen of AE2's, so it has to name
     * the bridge {@link ModGuiBridges} made for it.
     */
    @Nullable
    @Override
    public GuiBridge getOriginGui() {
        return ModGuiBridges.levelMaintainer();
    }

    /**
     * The middle click on a row types how much that row keeps, which is the machine's number rather than
     * anything the slot itself holds - the slot stands for the kind of thing, one of it.
     */
    @Nullable
    @Override
    public IAmountTarget amountTargetFor(final GuiBridge origin, final int slot) {
        if (slot < 0 || slot >= TileLevelMaintainer.ROWS) {
            return null;
        }
        final AEKey what = this.machine.keyOf(slot);
        return what == null ? null : new MaintainerAmount(this.machine, slot, origin);
    }

    /**
     * The wheel over a row steps how much it keeps, through the same path and with the same gestures as any
     * other filter slot of AE2's - a whole unit a notch, halved or doubled with Ctrl.
     */
    @Override
    protected boolean adjustAmountElsewhere(final Slot slot, final InventoryAction action, final ItemStack hand) {
        final int row = this.inventorySlots.indexOf(slot);
        if (row < 0 || row >= TileLevelMaintainer.ROWS) {
            return false;
        }

        // Holding something means the player is placing a different thing to keep, not tuning this one
        if (!hand.isEmpty() && action != InventoryAction.HALVE && action != InventoryAction.DOUBLE) {
            return false;
        }

        final AEKey what = this.machine.keyOf(row);
        if (what == null) {
            return false;
        }

        final long adjusted = AEBaseContainer.adjustAmount(this.machine.getTarget(row),
                what.getAmountPerUnit(), action, 1);
        if (adjusted < 0) {
            return false;
        }

        this.machine.setTarget(row, adjusted);
        return true;
    }

    @Override
    public void detectAndSendChanges() {
        this.verifyPermissions(SecurityPermissions.BUILD, false);

        if (Platform.isServer()) {
            this.target0 = this.machine.getTarget(0);
            this.target1 = this.machine.getTarget(1);
            this.target2 = this.machine.getTarget(2);
            this.target3 = this.machine.getTarget(3);
            this.target4 = this.machine.getTarget(4);

            this.batch0 = this.machine.getBatch(0);
            this.batch1 = this.machine.getBatch(1);
            this.batch2 = this.machine.getBatch(2);
            this.batch3 = this.machine.getBatch(3);
            this.batch4 = this.machine.getBatch(4);

            int rows = 0;
            for (int row = 0; row < TileLevelMaintainer.ROWS; row++) {
                if (this.machine.isRowEnabled(row)) {
                    rows |= 1 << row;
                }
            }
            this.enabledRows = rows;

            int states = 0;
            for (int row = 0; row < TileLevelMaintainer.ROWS; row++) {
                states |= this.machine.getRowState(row).ordinal() << row * 3;
            }
            this.rowStates = states;
        }

        super.detectAndSendChanges();
    }
}
