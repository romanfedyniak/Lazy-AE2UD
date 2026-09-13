/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.container;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.math.BlockPos;

import lazyae2.core.ModGuiBridges;
import lazyae2.network.ModNetwork;
import lazyae2.network.PacketTerminalUpdate;
import lazyae2.part.PartLevelMaintainerTerminal;
import lazyae2.tile.TileLevelMaintainer;
import appeng.api.behaviors.ContainerItemStrategies;
import appeng.api.config.SecurityPermissions;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.container.AEBaseContainer;
import appeng.core.sync.GuiBridge;
import appeng.helpers.IAmountTarget;
import appeng.helpers.InventoryAction;
import appeng.helpers.WirelessTerminalGuiObject;
import appeng.util.Platform;

import static appeng.helpers.ItemStackHelper.stackWriteToNBT;

/**
 * Every ME Level Maintainer on the network, as one window.
 * <p>
 * The rows shown belong to machines elsewhere, so none of them is a real slot: the server keeps a copy of
 * what this client has been told and sends only the rows that have moved since, and a click comes back
 * naming the machine and the row rather than a slot number.
 */
public class ContainerLevelMaintainerTerminal extends AEBaseContainer {

    public static final int HEIGHT = 235;

    /** Ids are only ever compared, never saved, so any number nobody else is using will do. */
    private static long autoBase = Long.MIN_VALUE;

    private final Map<TileLevelMaintainer, Tracker> byMachine = new HashMap<>();
    private final Map<Long, Tracker> byId = new HashMap<>();

    @Nullable
    private IGrid grid;
    private NBTTagCompound data = new NBTTagCompound();

    public ContainerLevelMaintainerTerminal(final InventoryPlayer ip, final PartLevelMaintainerTerminal anchor) {
        super(ip, anchor);

        if (Platform.isServer()) {
            final IGridNode node = anchor.getActionableNode();
            this.grid = node == null ? null : node.getGrid();
        }

        this.bindPlayerInventory(ip, 14, HEIGHT - 82);
    }

    /**
     * Opened from a wireless terminal rather than from a panel on a cable. The inventory is left unbound so
     * the subclass can put its upgrade slots in first.
     */
    public ContainerLevelMaintainerTerminal(final InventoryPlayer ip, final WirelessTerminalGuiObject guiObject,
            final boolean bindInventory) {
        super(ip, guiObject);

        if (Platform.isServer()) {
            final IGridNode node = guiObject.getActionableNode();
            if (node != null && node.isActive()) {
                this.grid = node.getGrid();
            }
        }

        if (bindInventory) {
            this.bindPlayerInventory(ip, 14, HEIGHT - 82);
        }
    }

    @Override
    public void detectAndSendChanges() {
        if (Platform.isClient()) {
            return;
        }

        // A window of ours is opened by our own handler, which is past the check AE2 makes for its own
        // screens - so the terminal asks for itself, and closes on a player who may not build here
        this.verifyPermissions(SecurityPermissions.BUILD, false);

        super.detectAndSendChanges();

        if (this.grid == null) {
            return;
        }

        int total = 0;
        boolean missing = false;

        final IActionHost host = this.getActionHost();
        final IGridNode terminal = host == null ? null : host.getActionableNode();
        if (terminal != null && terminal.isActive()) {
            for (final IGridNode node : this.grid.getMachines(TileLevelMaintainer.class)) {
                if (!node.isActive()) {
                    continue;
                }
                if (!this.byMachine.containsKey((TileLevelMaintainer) node.getMachine())) {
                    missing = true;
                }
                total++;
            }
        }

        if (total != this.byMachine.size() || missing) {
            this.regenList();
        } else {
            for (final Tracker tracker : this.byMachine.values()) {
                if (!tracker.name().equals(tracker.sentName)) {
                    tracker.sentName = tracker.name();
                    this.addHeader(tracker);
                }
                for (int row = 0; row < TileLevelMaintainer.ROWS; row++) {
                    if (tracker.isRowStale(row)) {
                        this.addRow(tracker, row);
                    }
                }
            }
        }

        if (!this.data.isEmpty()) {
            ModNetwork.CHANNEL.sendTo(new PacketTerminalUpdate(this.data),
                    (EntityPlayerMP) this.getPlayerInv().player);
            this.data = new NBTTagCompound();
        }
    }

    private void regenList() {
        this.byMachine.clear();
        this.byId.clear();

        final IActionHost host = this.getActionHost();
        final IGridNode terminal = host == null ? null : host.getActionableNode();
        if (terminal != null && terminal.isActive() && this.grid != null) {
            for (final IGridNode node : this.grid.getMachines(TileLevelMaintainer.class)) {
                if (node.isActive()) {
                    final TileLevelMaintainer machine = (TileLevelMaintainer) node.getMachine();
                    final Tracker tracker = new Tracker(machine);
                    this.byMachine.put(machine, tracker);
                    this.byId.put(tracker.id, tracker);
                }
            }
        }

        this.data.setBoolean("clear", true);

        for (final Tracker tracker : this.byMachine.values()) {
            tracker.sentName = tracker.name();
            this.addHeader(tracker);
            for (int row = 0; row < TileLevelMaintainer.ROWS; row++) {
                this.addRow(tracker, row);
            }
        }
    }

    /**
     * What the window needs to head a machine with: where it stands, and what it is called.
     */
    private void addHeader(final Tracker tracker) {
        final NBTTagCompound tag = this.tagFor(tracker);
        tag.setLong("sort", tracker.sortBy);
        tag.setString("name", tracker.sentName);
        tag.setBoolean("custom", tracker.machine.hasCustomInventoryName());
        tag.setTag("pos", NBTUtil.createPosTag(tracker.machine.getPos()));
        tag.setInteger("dim", tracker.machine.getWorld().provider.getDimension());
    }

    private void addRow(final Tracker tracker, final int row) {
        final NBTTagCompound tag = this.tagFor(tracker);
        final NBTTagCompound saved = new NBTTagCompound();

        final ItemStack filter = tracker.machine.getRequests().getStackInSlot(row);
        if (!filter.isEmpty()) {
            final NBTTagCompound what = new NBTTagCompound();
            stackWriteToNBT(filter, what);
            saved.setTag("f", what);
        }
        saved.setLong("t", tracker.machine.getTarget(row));
        saved.setLong("b", tracker.machine.getBatch(row));
        saved.setBoolean("e", tracker.machine.isRowEnabled(row));

        tag.setTag(Integer.toString(row), saved);
        tracker.remember(row);
    }

    private NBTTagCompound tagFor(final Tracker tracker) {
        final String key = "=" + Long.toString(tracker.id, Character.MAX_RADIX);
        final NBTTagCompound tag = this.data.getCompoundTag(key);
        this.data.setTag(key, tag);
        return tag;
    }

    // ---- what the window sends back ------------------------------------------------------------------

    @Nullable
    private Tracker get(final long id, final int row) {
        final Tracker tracker = this.byId.get(id);
        return tracker == null || row < 0 || row >= TileLevelMaintainer.ROWS ? null : tracker;
    }

    public void setBatch(final long id, final int row, final long batch) {
        final Tracker tracker = this.get(id, row);
        if (tracker != null) {
            tracker.machine.setBatch(row, batch);
        }
    }

    public void setRowEnabled(final long id, final int row, final boolean on) {
        final Tracker tracker = this.get(id, row);
        if (tracker != null) {
            tracker.machine.setRowEnabled(row, on);
        }
    }

    /**
     * What HEI dropped on a row, which is a key rather than an item: a fluid never arrives as one.
     */
    public void setFilter(final long id, final int row, final ItemStack filter) {
        final Tracker tracker = this.get(id, row);
        if (tracker != null) {
            tracker.machine.getRequests().setStackInSlot(row, filter);
        }
    }

    /**
     * How much a row keeps, for the number screen. The row belongs to a machine elsewhere on the network, so
     * the way back is this terminal rather than that machine's own window.
     */
    @Nullable
    public IAmountTarget amountTargetFor(final long id, final int row) {
        final Tracker tracker = this.get(id, row);
        if (tracker == null || tracker.machine.keyOf(row) == null) {
            return null;
        }

        final GuiBridge origin = this.getOriginGui();
        return origin == null ? null : new MaintainerAmount(tracker.machine, row, origin);
    }

    /**
     * Which window a player typing an amount is sent back to. Ours is no screen of AE2's, so it has to name
     * the bridge {@link ModGuiBridges} made for it.
     */
    @Nullable
    @Override
    public GuiBridge getOriginGui() {
        return ModGuiBridges.levelMaintainerTerminal();
    }

    /**
     * A click on a row's filter. The row names the kind of thing to keep, one of it, so a click that would
     * change an amount changes the machine's own number instead - the gestures the maintainer's own window
     * answers, on a row that is not here.
     */
    @Override
    public void doAction(final EntityPlayerMP player, final InventoryAction action, final int slot, final long id) {
        final Tracker tracker = this.get(id, slot);
        if (tracker == null) {
            return;
        }

        final TileLevelMaintainer machine = tracker.machine;
        final ItemStack hand = player.inventory.getItemStack();
        final AEKey what = machine.keyOf(slot);

        // Holding something means the player is naming a different thing to keep, not tuning this one
        if (what != null && (hand.isEmpty() || action == InventoryAction.HALVE || action == InventoryAction.DOUBLE)) {
            final long adjusted = AEBaseContainer.adjustAmount(machine.getTarget(slot), what.getAmountPerUnit(),
                    action, 1);
            if (adjusted >= 0) {
                machine.setTarget(slot, adjusted);
                return;
            }
        }

        switch (action) {
            case CREATIVE_DUPLICATE:
                if (!player.capabilities.isCreativeMode) {
                    return;
                }
                // falls through
            case PICKUP_OR_SET_DOWN:
            case SPLIT_OR_PLACE_SINGLE:
            case PLACE_SINGLE:
                if (!hand.isEmpty()) {
                    final ItemStack one = hand.copy();
                    one.setCount(1);
                    machine.getRequests().setStackInSlot(slot, one);
                } else if (action == InventoryAction.PICKUP_OR_SET_DOWN) {
                    machine.getRequests().setStackInSlot(slot, ItemStack.EMPTY);
                }
                break;
            case EMPTY_ITEM: {
                // A held container names what is inside it rather than itself, as on any filter slot
                final GenericStack contained = ContainerItemStrategies.getContainedStack(hand);
                if (contained != null) {
                    machine.getRequests().setStackInSlot(slot, GenericStack.wrapInItemStack(contained.what(), 1));
                }
                break;
            }
            case SHIFT_CLICK:
                machine.getRequests().setStackInSlot(slot, ItemStack.EMPTY);
                break;
            default:
        }
    }

    /**
     * One maintainer as this client has last been told about it.
     */
    private static final class Tracker {

        private final long id = autoBase++;
        private final TileLevelMaintainer machine;
        private final long sortBy;

        private final ItemStack[] filters = new ItemStack[TileLevelMaintainer.ROWS];
        private final long[] targets = new long[TileLevelMaintainer.ROWS];
        private final long[] batches = new long[TileLevelMaintainer.ROWS];
        private final boolean[] enabled = new boolean[TileLevelMaintainer.ROWS];
        private String sentName = "";

        private Tracker(final TileLevelMaintainer machine) {
            this.machine = machine;
            final BlockPos pos = machine.getPos();
            this.sortBy = ((long) pos.getZ() << 24) ^ ((long) pos.getX() << 8) ^ pos.getY();
            for (int row = 0; row < TileLevelMaintainer.ROWS; row++) {
                this.filters[row] = ItemStack.EMPTY;
            }
        }

        /**
         * A machine with no name of its own is named after what it is, which the window translates.
         */
        private String name() {
            return this.machine.hasCustomInventoryName() ? this.machine.getCustomInventoryName()
                    : "tile.threng.machine.level_maintainer.name";
        }

        private boolean isRowStale(final int row) {
            return this.targets[row] != this.machine.getTarget(row)
                    || this.batches[row] != this.machine.getBatch(row)
                    || this.enabled[row] != this.machine.isRowEnabled(row)
                    || !ItemStack.areItemStacksEqual(this.filters[row],
                            this.machine.getRequests().getStackInSlot(row));
        }

        private void remember(final int row) {
            this.targets[row] = this.machine.getTarget(row);
            this.batches[row] = this.machine.getBatch(row);
            this.enabled[row] = this.machine.isRowEnabled(row);
            this.filters[row] = this.machine.getRequests().getStackInSlot(row).copy();
        }
    }
}
