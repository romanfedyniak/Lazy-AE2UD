/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.tile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.ImmutableSet;

import io.netty.buffer.ByteBuf;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

import lazyae2.core.LazyAE2Config;
import appeng.api.config.Actionable;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.networking.events.MENetworkChannelsChanged;
import appeng.api.networking.events.MENetworkEventSubscribe;
import appeng.api.networking.events.MENetworkPowerStatusChange;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStackWatcher;
import appeng.api.networking.storage.IStorageWatcherNode;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.api.util.AECableType;
import appeng.api.util.AEPartLocation;
import appeng.api.util.DimensionalCoord;
import appeng.me.GridAccessException;
import appeng.me.helpers.MachineSource;
import appeng.tile.grid.AENetworkInvTile;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.util.Platform;
import appeng.util.inv.InvOperation;

/**
 * The ME Level Maintainer: five rows, each naming something the network should always have some of, and how
 * much to order at a time when it runs short.
 * <p>
 * A row stands for any key the network can hold, not only an item: a fluid or anything an addon registers is
 * kept to a level the same way. What a row ordered is not carried back through this machine - the crafting
 * job leaves it in network storage, which is where it was wanted - so there is nothing here to unload.
 */
public final class TileLevelMaintainer extends AENetworkInvTile
        implements IMachineTile, IGridTickable, ICraftingRequester, IStorageWatcherNode {

    public static final int ROWS = 5;

    /**
     * How many digits a batch takes, in either window that types one. Fifteen is past anything a network
     * will ever craft, still short of where a long stops counting, and the most that fits in the narrower
     * of the two fields.
     */
    public static final int BATCH_DIGITS = 15;

    /** An amount nobody has looked up yet. */
    private static final long UNKNOWN = -1;

    private final AppEngInternalInventory requests = new AppEngInternalInventory(this, ROWS, 1);
    private final long[] targets = new long[ROWS];
    private final long[] batches = new long[ROWS];
    private final long[] known = new long[ROWS];
    /** A row switched off keeps what it says and orders nothing. */
    private final boolean[] enabled = new boolean[ROWS];
    /** Ticks a row still waits before planning again, after the network turned its last plan down. */
    private final int[] retryIn = new int[ROWS];

    private final CraftTracker crafter = new CraftTracker(this, ROWS);
    private final IActionSource source = new MachineSource(this);

    @Nullable
    private IStackWatcher watcher;

    private EnumFacing front = EnumFacing.NORTH;
    private boolean active;
    @Nullable
    private String customName;

    /**
     * What the old mod's five result slots held. It had a buffer for what came back from a craft; this one
     * has none, so whatever was in there is handed to the network and forgotten.
     */
    private final List<ItemStack> legacyResults = new ArrayList<>();

    public TileLevelMaintainer() {
        Arrays.fill(this.batches, 1);
        Arrays.fill(this.enabled, true);
        Arrays.fill(this.known, UNKNOWN);
        this.getProxy().setFlags(GridFlags.REQUIRE_CHANNEL);
        this.getProxy().setIdlePowerUsage(LazyAE2Config.instance().getLevelMaintainerIdlePower());
    }

    public IItemHandlerModifiable getRequests() {
        return this.requests;
    }

    public long getTarget(final int row) {
        return this.targets[row];
    }

    public long getBatch(final int row) {
        return this.batches[row];
    }

    public boolean isRowEnabled(final int row) {
        return this.enabled[row];
    }

    /**
     * A row switched off stops asking for anything; whatever it had ordered is called off with it.
     */
    public void setRowEnabled(final int row, final boolean on) {
        if (row < 0 || row >= ROWS || this.enabled[row] == on) {
            return;
        }
        this.enabled[row] = on;
        this.retryIn[row] = 0;
        if (!on) {
            this.crafter.clear(row);
        }
        this.saveChanges();
        this.wake();
    }

    /**
     * What a row is set to keep in the network. Zero clears the row.
     */
    public void setTarget(final int row, final long target) {
        if (row < 0 || row >= ROWS) {
            return;
        }
        if (target <= 0) {
            this.requests.setStackInSlot(row, ItemStack.EMPTY);
            return;
        }
        this.targets[row] = target;
        this.retryIn[row] = 0;
        this.saveChanges();
        this.wake();
    }

    /**
     * The most a row orders at once, so a level that is short by a stack is not ordered a stack at a time.
     */
    public void setBatch(final int row, final long batch) {
        if (row < 0 || row >= ROWS || batch < 0) {
            return;
        }
        this.batches[row] = batch;
        this.retryIn[row] = 0;
        this.saveChanges();
        this.wake();
    }

    /**
     * What that row watches for, whatever kind of thing it is, or null while the row is empty.
     */
    @Nullable
    public AEKey keyOf(final int row) {
        final ItemStack filter = this.requests.getStackInSlot(row);
        if (filter.isEmpty()) {
            return null;
        }
        final GenericStack wrapped = GenericStack.unwrapItemStack(filter);
        return wrapped != null ? wrapped.what() : AEItemKey.of(filter);
    }

    @Nonnull
    @Override
    public IItemHandler getInternalInventory() {
        return this.requests;
    }

    @Override
    public void onChangeInventory(final IItemHandler inv, final int slot, final InvOperation operation,
            final ItemStack removed, final ItemStack added) {
        if (this.requests.getStackInSlot(slot).isEmpty()) {
            this.targets[slot] = 0;
            this.crafter.clear(slot);
        } else if (this.targets[slot] <= 0) {
            this.targets[slot] = 1;
        }
        this.known[slot] = UNKNOWN;
        this.retryIn[slot] = 0;
        this.resetWatcher();
        this.saveChanges();
        this.wake();
    }

    // ---- the work ------------------------------------------------------------------------------------

    @Override
    public TickingRequest getTickingRequest(final IGridNode node) {
        final LazyAE2Config config = LazyAE2Config.instance();
        return new TickingRequest(config.getLevelMaintainerSleepMin(), config.getLevelMaintainerSleepMax(),
                false, true);
    }

    @Override
    public TickRateModulation tickingRequest(final IGridNode node, final int ticksSinceLastCall) {
        // The tick manager knows nothing of channels, so a machine that has none is still asked to work.
        // Without one it is not on the network at all and has no business ordering anything.
        if (!this.getProxy().isActive()) {
            return TickRateModulation.IDLE;
        }

        boolean worked = this.returnLegacyResults();

        try {
            final IGrid grid = this.getProxy().getGrid();
            // The network's own tally, not a fresh count of every cell on it
            final KeyCounter stock = this.getProxy().getStorage().getCachedInventory();
            final ICraftingGrid crafting = this.getProxy().getCrafting();

            // Asked for at most once a pass, and only by a row that actually wants to plan: the answer
            // costs a walk of the network's processors
            Boolean cpuFree = null;

            for (int row = 0; row < ROWS; row++) {
                final AEKey what = this.keyOf(row);
                if (what == null || !this.enabled[row] || this.targets[row] <= 0 || this.batches[row] <= 0) {
                    continue;
                }

                if (this.known[row] == UNKNOWN) {
                    this.known[row] = stock.get(what);
                    worked = true;
                }

                if (this.retryIn[row] > 0) {
                    this.retryIn[row] -= ticksSinceLastCall;
                    continue;
                }

                // A whole batch is ordered the moment the level drops below what the row keeps, rather than
                // exactly what is missing: ordering the three that were taken out would start a crafting job
                // for three. Asked every time, not only when something is missing, because a plan already
                // being worked out still has to be picked up and submitted once it is ready.
                final long order = this.known[row] < this.targets[row] ? this.batches[row] : 0;

                boolean mayPlan = false;
                if (order > 0) {
                    // Nothing on the network makes this, so a plan could only come back a simulation; and a
                    // plan worked out while every processor is busy is a plan thrown away at the door
                    if (crafting.isCraftable(what)) {
                        if (cpuFree == null) {
                            cpuFree = anyFreeCpu(crafting);
                        }
                        mayPlan = cpuFree;
                    }
                }

                switch (this.crafter.request(row, what, order, mayPlan, this.world, grid, crafting, this.source)) {
                    case WORKING:
                        worked = true;
                        break;
                    case REFUSED:
                        this.retryIn[row] = LazyAE2Config.instance().getLevelMaintainerRetryTicks();
                        break;
                    default:
                        break;
                }
            }
        } catch (final GridAccessException offline) {
            return TickRateModulation.IDLE;
        }

        return worked ? TickRateModulation.FASTER : TickRateModulation.SLOWER;
    }

    private static boolean anyFreeCpu(final ICraftingGrid crafting) {
        for (final ICraftingCPU cpu : crafting.getCpus()) {
            if (!cpu.isBusy()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Every row tries again at once, whatever it was waiting out. Called when somebody opens a window that
     * shows this machine: what is on the screen should be what the machine thinks now, not what it settled
     * on before it went to sleep.
     */
    public void retryNow() {
        Arrays.fill(this.retryIn, 0);
        this.wake();
    }

    /**
     * What the old mod's result slots still held, put into the network a slot at a time.
     */
    private boolean returnLegacyResults() {
        if (this.legacyResults.isEmpty()) {
            return false;
        }

        try {
            final MEStorage storage = this.getProxy().getStorage().getInventory();
            final ItemStack waiting = this.legacyResults.get(0);
            final long taken = Platform.poweredInsert(this.getProxy().getEnergy(), storage, AEItemKey.of(waiting),
                    waiting.getCount(), this.source, Actionable.MODULATE);
            if (taken >= waiting.getCount()) {
                this.legacyResults.remove(0);
            } else if (taken > 0) {
                waiting.shrink((int) taken);
            }
            this.saveChanges();
            return taken > 0;
        } catch (final GridAccessException offline) {
            return false;
        }
    }

    private void wake() {
        try {
            this.getProxy().getTick().alertDevice(this.getProxy().getNode());
        } catch (final GridAccessException offline) {
            // nothing to wake while the machine is off a network
        }
    }

    // ---- watching the network ------------------------------------------------------------------------

    @Override
    public void updateWatcher(final IStackWatcher newWatcher) {
        this.watcher = newWatcher;
        this.resetWatcher();
    }

    private void resetWatcher() {
        if (this.watcher == null) {
            return;
        }
        this.watcher.reset();
        for (int row = 0; row < ROWS; row++) {
            final AEKey what = this.keyOf(row);
            if (what != null) {
                this.watcher.add(what);
            }
        }
    }

    @Override
    public void onStackChange(final AEKey what, final long amount) {
        for (int row = 0; row < ROWS; row++) {
            if (what.equals(this.keyOf(row))) {
                this.known[row] = amount;
                this.retryIn[row] = 0;
                if (amount < this.targets[row]) {
                    this.wake();
                }
            }
        }
    }

    // ---- the crafting side ---------------------------------------------------------------------------

    @Override
    public ImmutableSet<ICraftingLink> getRequestedJobs() {
        return this.crafter.getRequestedJobs();
    }

    /**
     * Nothing is taken in: what a row ordered has already been made and is sitting in network storage, which
     * is where it was wanted. Refusing it leaves it exactly there.
     */
    @Override
    public GenericStack injectCraftedItems(final ICraftingLink link, final GenericStack items,
            final Actionable mode) {
        if (mode == Actionable.MODULATE) {
            final int row = this.crafter.rowOf(link);
            if (row >= 0) {
                this.known[row] = UNKNOWN;
            }
            this.wake();
        }
        return items;
    }

    @Override
    public void jobStateChange(final ICraftingLink link) {
        final int row = this.crafter.rowOf(link);
        this.crafter.forget(link);
        if (row >= 0) {
            this.known[row] = UNKNOWN;
            this.saveChanges();
            this.wake();
        }
    }

    @MENetworkEventSubscribe
    public void onPowerStatusChange(final MENetworkPowerStatusChange event) {
        this.updateActive();
    }

    @MENetworkEventSubscribe
    public void onChannelsChanged(final MENetworkChannelsChanged event) {
        this.updateActive();
    }

    /**
     * The look of the block follows the network, and the events that carry that only fire once something
     * changes - so the state is also read the moment the machine joins a grid.
     */
    @Override
    public void onReady() {
        super.onReady();
        this.updateActive();
    }

    @Override
    public void gridChanged() {
        this.updateActive();
    }

    private void updateActive() {
        final boolean nowActive = this.getProxy().isActive();
        if (this.active != nowActive) {
            this.active = nowActive;
            this.markForUpdate();
        }
    }

    // ---- the block side ------------------------------------------------------------------------------

    @Override
    public boolean isWorking() {
        return this.active;
    }

    @Override
    public EnumFacing getFront() {
        return this.front;
    }

    @Override
    public void setFront(final EnumFacing facing) {
        this.front = facing.getAxis().isHorizontal() ? facing : EnumFacing.NORTH;
        this.saveChanges();
        this.markForUpdate();
    }

    @Override
    public String getCustomInventoryName() {
        return this.customName == null ? "" : this.customName;
    }

    @Override
    public boolean hasCustomInventoryName() {
        return this.customName != null && !this.customName.isEmpty();
    }

    @Override
    public void setCustomName(final String name) {
        this.customName = name == null || name.isEmpty() ? null : name;
        this.saveChanges();
        this.markForUpdate();
    }

    @Override
    public boolean canBeRotated() {
        return false; // the machine is turned by the face it was placed against, and a wrench turns it
    }

    @Override
    public AECableType getCableConnectionType(final AEPartLocation side) {
        return AECableType.SMART;
    }

    @Override
    public DimensionalCoord getLocation() {
        return new DimensionalCoord(this);
    }

    /**
     * The rows hold what to watch for, not stock, so there is nothing of a player's in here to give back.
     */
    @Override
    public void getDrops(final World world, final BlockPos pos, final List<ItemStack> drops) {
        drops.addAll(this.legacyResults);
    }

    @Override
    public void readFromNBT(final NBTTagCompound data) {
        // The old mod's library kept the network node under a name of its own
        if (data.hasKey("Requests") && !data.hasKey("proxy")) {
            data.setTag("proxy", data.getCompoundTag("aeproxy"));
        }
        super.readFromNBT(data);

        if (data.hasKey("Requests")) {
            this.readLegacyNBT(data);
        } else {
            for (int row = 0; row < ROWS; row++) {
                this.targets[row] = data.getLong("target" + row);
                this.batches[row] = data.getLong("batch" + row);
                this.enabled[row] = !data.hasKey("off" + row);
            }
            final EnumFacing saved = EnumFacing.byName(data.getString("front"));
            this.front = saved == null ? EnumFacing.NORTH : saved;
            this.crafter.readFromNBT(data);
        }
        this.customName = data.hasKey("customName") ? data.getString("customName") : null;
        Arrays.fill(this.known, UNKNOWN);
    }

    @Override
    public NBTTagCompound writeToNBT(final NBTTagCompound data) {
        super.writeToNBT(data);
        for (int row = 0; row < ROWS; row++) {
            data.setLong("target" + row, this.targets[row]);
            data.setLong("batch" + row, this.batches[row]);
            if (!this.enabled[row]) {
                data.setBoolean("off" + row, true);
            }
        }
        data.setString("front", this.front.getName());
        if (this.customName != null) {
            data.setString("customName", this.customName);
        }
        this.crafter.writeToNBT(data);

        if (!this.legacyResults.isEmpty()) {
            final NBTTagList waiting = new NBTTagList();
            for (final ItemStack stack : this.legacyResults) {
                waiting.appendTag(stack.serializeNBT());
            }
            data.setTag("legacyResults", waiting);
        }
        return data;
    }

    /**
     * A machine the old mod saved: five rows of a stack, an amount and a batch size, and five slots of
     * whatever a craft had handed back but not yet put away.
     */
    private void readLegacyNBT(final NBTTagCompound data) {
        final NBTTagList rows = data.getTagList("Requests", Constants.NBT.TAG_COMPOUND);
        for (int row = 0; row < Math.min(ROWS, rows.tagCount()); row++) {
            final NBTTagCompound saved = rows.getCompoundTagAt(row);
            if (saved.hasKey("Stack")) {
                this.requests.setStackInSlot(row, new ItemStack(saved.getCompoundTag("Stack")));
                this.targets[row] = saved.getLong("Count");
            }
            this.batches[row] = Math.max(1, saved.getLong("Batch"));
        }

        final NBTTagList results = data.getCompoundTag("Results").getTagList("Items", Constants.NBT.TAG_COMPOUND);
        for (int slot = 0; slot < results.tagCount(); slot++) {
            final NBTTagCompound item = results.getCompoundTagAt(slot);
            if (!item.getKeySet().isEmpty() && !item.getBoolean("Empty")) {
                final ItemStack stack = new ItemStack(item);
                if (!stack.isEmpty()) {
                    this.legacyResults.add(stack);
                }
            }
        }

        this.front = EnumFacing.byIndex(data.getShort("FrontFace"));
        if (!this.front.getAxis().isHorizontal()) {
            this.front = EnumFacing.NORTH;
        }
    }

    @Override
    protected boolean readFromStream(final ByteBuf data) throws IOException {
        boolean changed = super.readFromStream(data);
        final boolean nowActive = data.readBoolean();
        if (this.active != nowActive) {
            this.active = nowActive;
            changed = true;
        }
        final EnumFacing nowFront = EnumFacing.byIndex(data.readByte());
        if (this.front != nowFront) {
            this.front = nowFront;
            changed = true;
        }
        final String name = ByteBufUtils.readUTF8String(data);
        if (!name.equals(this.getCustomInventoryName())) {
            this.customName = name.isEmpty() ? null : name;
            changed = true;
        }
        return changed;
    }

    @Override
    protected void writeToStream(final ByteBuf data) throws IOException {
        super.writeToStream(data);
        data.writeBoolean(this.active);
        data.writeByte(this.front.getIndex());
        ByteBufUtils.writeUTF8String(data, this.getCustomInventoryName());
    }
}
