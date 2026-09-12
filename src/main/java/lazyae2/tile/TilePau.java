/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.tile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.netty.buffer.ByteBuf;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.fml.common.network.ByteBufUtils;

import lazyae2.block.BlockMachine;
import lazyae2.core.LazyAE2Config;
import lazyae2.util.IoMode;
import lazyae2.util.RelativeSide;
import lazyae2.util.SideConfig;
import appeng.api.config.Actionable;
import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.implementations.tiles.ISegmentedInventory;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingMedium;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.crafting.ICraftingProviderHelper;
import appeng.api.networking.crafting.IPatternContainer;
import appeng.api.networking.crafting.MachineIdentity;
import appeng.api.networking.events.MENetworkChannelsChanged;
import appeng.api.networking.events.MENetworkCraftingPatternChange;
import appeng.api.networking.events.MENetworkEventSubscribe;
import appeng.api.networking.events.MENetworkPowerStatusChange;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.storage.MEStorage;
import appeng.api.upgrades.CardTrait;
import appeng.api.upgrades.CardTraits;
import appeng.api.util.AECableType;
import appeng.api.util.AEPartLocation;
import appeng.api.util.DimensionalCoord;
import appeng.helpers.DualityInterface;
import appeng.helpers.ICustomIconObject;
import appeng.me.GridAccessException;
import appeng.me.helpers.MachineSource;
import appeng.tile.grid.AENetworkInvTile;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.util.InventoryAdaptor;
import appeng.util.Platform;
import appeng.util.inv.InvOperation;
import appeng.util.inv.WrapperChainedItemHandler;
import appeng.util.inv.WrapperFilteredItemHandler;

/**
 * The Preemptive Assembly Unit: a crafting medium that keeps taking work while it is busy.
 * <p>
 * An interface tells the crafting CPU it is busy until what it holds has gone, and the CPU waits. This one
 * says busy but takes the pattern anyway as long as its buffer has room, so a machine that can queue a
 * hundred operations is filled instead of being fed one at a time. The old mod did that by reaching into the
 * CPU's private task list and pulling every remaining iteration out at once; here it is the CPU that keeps
 * pushing, at the rate its co-processors allow.
 */
public final class TilePau extends AENetworkInvTile
        implements ISidedMachineTile, IGridTickable, ICraftingProvider, ICraftingMedium, IPatternContainer,
        ICustomIconObject, ISegmentedInventory {

    /** Nine to a row, as in an interface: the row it is born with, plus one for every expansion card. */
    public static final int PATTERN_SLOTS = 36;
    public static final int UPGRADE_SLOTS = 4;
    public static final int BUFFER_SLOTS = 9;

    private final AppEngInternalInventory patterns = new AppEngInternalInventory(this, PATTERN_SLOTS, 1);
    private final AppEngInternalInventory imports = new AppEngInternalInventory(this, BUFFER_SLOTS);
    private final AppEngInternalInventory exports = new AppEngInternalInventory(this, BUFFER_SLOTS);
    private final MachineUpgradeInventory upgrades =
            new MachineUpgradeInventory(BlockMachine.Type.PAU, this, UPGRADE_SLOTS);

    private final IItemHandler inventory =
            new WrapperChainedItemHandler(this.patterns, this.imports, this.exports, this.upgrades);
    /** All a pipe against this machine may do is hand it what its neighbour made. */
    private final IItemHandler insertOnly = new WrapperFilteredItemHandler(this.imports, SlotFilters.INSERT_ONLY);

    private final SideConfig sides = new SideConfig(IoMode.OUTPUT);
    private final IActionSource source = new MachineSource(this);

    private EnumFacing front = EnumFacing.NORTH;
    private boolean active;
    /** What the player renamed it to with the knife, or null. */
    @Nullable
    private String customName;
    /** The picture the player chose for it there, or empty to let it picture itself. */
    private ItemStack customIcon = ItemStack.EMPTY;

    public TilePau() {
        this.getProxy().setFlags(GridFlags.REQUIRE_CHANNEL);
        this.getProxy().setIdlePowerUsage(LazyAE2Config.instance().getPauIdlePower());
    }

    public IItemHandlerModifiable getPatterns() {
        return this.patterns;
    }

    public IItemHandlerModifiable getImports() {
        return this.imports;
    }

    public IItemHandlerModifiable getExports() {
        return this.exports;
    }

    public IItemHandlerModifiable getUpgradeInventory() {
        return this.upgrades;
    }

    /** How many pattern slots the cards inside pay for. */
    public int getUsablePatternSlots() {
        return 9 * (1 + this.getInstalledPoints(CardTraits.PATTERN_EXPANSION));
    }

    /** How many there would be if the card in that slot were taken out. */
    public int getUsablePatternSlotsWithout(final int upgradeSlot) {
        return 9 * (1 + this.upgrades.getInstalledPointsWithout(CardTraits.PATTERN_EXPANSION, upgradeSlot));
    }

    /**
     * A card only comes out when nothing stands in the rows it pays for, so patterns are never spilled.
     */
    public boolean canRemoveUpgrade(final int upgradeSlot) {
        for (int slot = this.getUsablePatternSlotsWithout(upgradeSlot); slot < PATTERN_SLOTS; slot++) {
            if (!this.patterns.getStackInSlot(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Nonnull
    @Override
    public IItemHandler getInternalInventory() {
        return this.inventory;
    }

    @Override
    public IItemHandler getInventoryByName(final String name) {
        switch (name) {
            case "upgrades":
                return this.upgrades;
            case "patterns":
                return this.patterns;
            case "import":
                return this.imports;
            case "export":
                return this.exports;
            default:
                return null;
        }
    }

    @Override
    public void onChangeInventory(final IItemHandler inv, final int slot, final InvOperation operation,
            final ItemStack removed, final ItemStack added) {
        if (inv == this.patterns || inv == this.upgrades) {
            this.postPatternChange();
        }
        if (inv == this.upgrades) {
            this.getProxy().setIdlePowerUsage(LazyAE2Config.instance().getPauIdlePower());
        }
        if (inv == this.imports || inv == this.exports) {
            this.wake();
        }
        this.saveChanges();
    }

    private void postPatternChange() {
        if (this.world == null || this.world.isRemote) {
            return;
        }
        try {
            this.getProxy().getGrid().postEvent(new MENetworkCraftingPatternChange(this, this.getProxy().getNode()));
        } catch (final GridAccessException ignored) {
            // no grid to tell; it asks again itself once there is one
        }
    }

    // ---- the network side ----------------------------------------------------------------------------

    @Override
    public void provideCrafting(final ICraftingProviderHelper helper) {
        final int usable = Math.min(this.getUsablePatternSlots(), PATTERN_SLOTS);
        for (int slot = 0; slot < usable; slot++) {
            final ItemStack stack = this.patterns.getStackInSlot(slot);
            if (stack.isEmpty() || !(stack.getItem() instanceof ICraftingPatternItem)) {
                continue;
            }
            final ICraftingPatternDetails details =
                    ((ICraftingPatternItem) stack.getItem()).getPatternForItem(stack, this.world);
            // A crafting pattern belongs in an interface: the assembler beside it reads the pattern itself,
            // while all this machine can do is hand a neighbour the ingredients.
            if (details != null && !details.isCraftable()) {
                helper.addCraftingOption(this, details);
            }
        }
    }

    @Override
    public boolean pushPattern(final ICraftingPatternDetails details, final InventoryCrafting table) {
        if (!this.getProxy().isActive()) {
            return false;
        }

        final List<ItemStack> ingredients = contentsOf(table);
        if (ingredients.isEmpty() || !this.bufferWouldTake(ingredients) || !this.neighboursWouldTake(ingredients)) {
            return false;
        }

        for (final ItemStack ingredient : ingredients) {
            ItemHandlerHelper.insertItem(this.exports, ingredient.copy(), false);
        }
        this.saveChanges();
        this.wake();
        return true;
    }

    /**
     * Busy the moment anything is waiting to go out, exactly as an interface is.
     */
    @Override
    public boolean isBusy() {
        for (int slot = 0; slot < this.exports.getSlots(); slot++) {
            if (!this.exports.getStackInSlot(slot).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * And this is the whole point of the machine: busy or not, one more pattern is taken while the buffer
     * still holds it. Whether the machine next door will take it is settled in {@link #pushPattern}.
     */
    @Override
    public boolean acceptsWhileBusy(final ICraftingPatternDetails details) {
        return this.bufferWouldTake(itemsOf(details));
    }

    /**
     * What a terminal calls this: the machine it stands against, the way an interface names itself after the
     * machine it serves, and a name the player gave it wins over both.
     */
    @Override
    public MachineIdentity getMachineIdentity() {
        final MachineIdentity neighbour = this.identifyNeighbour();
        if (!this.hasCustomInventoryName() && this.customIcon.isEmpty()) {
            return neighbour;
        }
        return new MachineIdentity(
                this.hasCustomInventoryName() ? this.getCustomInventoryName() : neighbour.getName(),
                this.customIcon.isEmpty() ? neighbour.getIcon() : this.customIcon);
    }

    @Nonnull
    @Override
    public ItemStack getCustomIcon() {
        return this.customIcon;
    }

    @Override
    public void setCustomIcon(@Nonnull final ItemStack icon) {
        this.customIcon = icon.isEmpty() ? ItemStack.EMPTY : icon.copy();
        this.saveChanges();
    }

    @Nonnull
    @Override
    public ItemStack getDefaultIcon() {
        return this.identifyNeighbour().getIcon();
    }

    private MachineIdentity identifyNeighbour() {
        for (final RelativeSide side : RelativeSide.all()) {
            if (!this.sides.get(side).allowsOutput()) {
                continue;
            }
            final MachineIdentity identity =
                    DualityInterface.identifyMachine(this.world, this.pos, side.getDirection(this.front));
            if (identity != MachineIdentity.NOTHING) {
                return identity;
            }
        }
        return new MachineIdentity("tile.threng.machine.pau.name", BlockMachine.Type.PAU.newStack(1));
    }

    // ---- what the pattern terminals see -------------------------------------------------------------

    @Nonnull
    @Override
    public IItemHandler getTerminalPatternInventory() {
        return this.patterns;
    }

    /**
     * The same rule {@link #provideCrafting} goes by: this machine can only hand a neighbour ingredients,
     * which is no use to a pattern that wants a crafting table.
     */
    @Override
    public boolean canAccept(@Nonnull final ItemStack pattern, @Nullable final ICraftingPatternDetails details) {
        return details != null && !details.isCraftable();
    }

    /**
     * Nothing here is pretended: both the medium and the terminal ask, and both get the same answer.
     */
    @Override
    public boolean isFakeCrafting() {
        return false;
    }

    @Nonnull
    @Override
    public MachineIdentity getTerminalIdentity() {
        return this.getMachineIdentity();
    }

    @Nullable
    @Override
    public DimensionalCoord getTerminalLocation() {
        return new DimensionalCoord(this);
    }

    @Override
    public DimensionalCoord getMachineLocation() {
        return new DimensionalCoord(this);
    }

    @Override
    public TickingRequest getTickingRequest(final IGridNode node) {
        // Alertable, because what wakes it is a pattern arriving between two ticks
        return new TickingRequest(1, 20, this.isIdle(), true);
    }

    @Override
    public TickRateModulation tickingRequest(final IGridNode node, final int ticksSinceLastCall) {
        boolean moved = this.importToNetwork();
        moved |= this.exportToNeighbours();

        if (this.isIdle()) {
            return TickRateModulation.SLEEP;
        }
        return moved ? TickRateModulation.URGENT : TickRateModulation.SLOWER;
    }

    private boolean isIdle() {
        return isEmpty(this.imports) && isEmpty(this.exports);
    }

    /**
     * What a pipe put into the machine goes into network storage, so a machine's results need no import bus
     * of their own.
     */
    private boolean importToNetwork() {
        boolean moved = false;
        try {
            final MEStorage storage = this.getProxy().getStorage().getInventory();
            for (int slot = 0; slot < this.imports.getSlots(); slot++) {
                final ItemStack stack = this.imports.getStackInSlot(slot);
                if (stack.isEmpty()) {
                    continue;
                }
                final long taken = Platform.poweredInsert(this.getProxy().getEnergy(), storage,
                        AEItemKey.of(stack), stack.getCount(), this.source, Actionable.MODULATE);
                if (taken > 0) {
                    this.imports.extractItem(slot, (int) taken, false);
                    moved = true;
                }
            }
        } catch (final GridAccessException ignored) {
            return false;
        }

        if (moved) {
            this.saveChanges();
        }
        return moved;
    }

    /**
     * The ingredients waiting in the buffer are handed to whatever stands against a face that lets items out.
     */
    private boolean exportToNeighbours() {
        if (!this.isBusy()) {
            return false;
        }

        boolean moved = false;
        for (final RelativeSide side : RelativeSide.all()) {
            if (!this.sides.get(side).allowsOutput()) {
                continue;
            }
            final EnumFacing direction = side.getDirection(this.front);
            final TileEntity neighbour = this.world.getTileEntity(this.pos.offset(direction));
            final InventoryAdaptor target = neighbour == null ? null
                    : InventoryAdaptor.getAdaptor(neighbour, direction.getOpposite());
            if (target == null) {
                continue;
            }

            for (int slot = 0; slot < this.exports.getSlots(); slot++) {
                final ItemStack waiting = this.exports.getStackInSlot(slot);
                if (waiting.isEmpty()) {
                    continue;
                }
                final ItemStack refused = target.simulateAdd(waiting.copy());
                final int movable = waiting.getCount() - refused.getCount();
                if (movable <= 0) {
                    continue;
                }
                final ItemStack left = target.addItems(this.exports.extractItem(slot, movable, false));
                if (!left.isEmpty()) {
                    this.exports.insertItem(slot, left, false);
                }
                moved = true;
            }
        }

        if (moved) {
            this.saveChanges();
        }
        return moved;
    }

    /**
     * Whether the buffer has room for all of it. Simulated against a tally of what each slot would hold, so
     * two ingredients that stack together are not both counted into the same empty slot.
     */
    private boolean bufferWouldTake(final List<ItemStack> ingredients) {
        if (ingredients.isEmpty()) {
            return false;
        }

        final ItemStack[] buffer = new ItemStack[this.exports.getSlots()];
        for (int slot = 0; slot < buffer.length; slot++) {
            buffer[slot] = this.exports.getStackInSlot(slot).copy();
        }

        for (final ItemStack ingredient : ingredients) {
            int left = ingredient.getCount();
            for (int slot = 0; slot < buffer.length && left > 0; slot++) {
                if (buffer[slot].isEmpty()) {
                    buffer[slot] = ItemHandlerHelper.copyStackWithSize(ingredient, Math.min(left,
                            Math.min(ingredient.getMaxStackSize(), this.exports.getSlotLimit(slot))));
                    left -= buffer[slot].getCount();
                } else if (ItemHandlerHelper.canItemStacksStack(buffer[slot], ingredient)) {
                    final int room = Math.min(ingredient.getMaxStackSize(), this.exports.getSlotLimit(slot))
                            - buffer[slot].getCount();
                    final int fits = Math.min(room, left);
                    buffer[slot].grow(fits);
                    left -= fits;
                }
            }
            if (left > 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Whether a machine against an outgoing face would take every ingredient. With nothing beside it the
     * pattern is refused rather than piling up in a buffer that has nowhere to go.
     */
    private boolean neighboursWouldTake(final List<ItemStack> ingredients) {
        final List<InventoryAdaptor> targets = new ArrayList<>(RelativeSide.all().length);
        for (final RelativeSide side : RelativeSide.all()) {
            if (!this.sides.get(side).allowsOutput()) {
                continue;
            }
            final EnumFacing direction = side.getDirection(this.front);
            final TileEntity neighbour = this.world.getTileEntity(this.pos.offset(direction));
            final InventoryAdaptor target = neighbour == null ? null
                    : InventoryAdaptor.getAdaptor(neighbour, direction.getOpposite());
            if (target != null) {
                targets.add(target);
            }
        }

        for (final ItemStack ingredient : ingredients) {
            boolean fits = false;
            for (final InventoryAdaptor target : targets) {
                if (target.simulateAdd(ingredient.copy()).isEmpty()) {
                    fits = true;
                    break;
                }
            }
            if (!fits) {
                return false;
            }
        }
        return !targets.isEmpty();
    }

    private void wake() {
        try {
            this.getProxy().getTick().alertDevice(this.getProxy().getNode());
        } catch (final GridAccessException ignored) {
            // nothing to wake while the machine is off a network
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
    public SideConfig getSides() {
        return this.sides;
    }

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

    public int getInstalledPoints(final CardTrait trait) {
        return this.upgrades.getInstalledPoints(trait);
    }

    @Override
    public boolean hasCapability(@Nonnull final Capability<?> capability, @Nullable final EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && facing != null) {
            return this.sides.get(RelativeSide.of(this.front, facing)).allowsInput();
        }
        return super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(@Nonnull final Capability<T> capability, @Nullable final EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && facing != null
                && this.sides.get(RelativeSide.of(this.front, facing)).allowsInput()) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(this.insertOnly);
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public void readFromNBT(final NBTTagCompound data) {
        // The old mod's library kept the network node under a name of its own
        if (data.hasKey("PatternInventory") && !data.hasKey("proxy")) {
            data.setTag("proxy", data.getCompoundTag("aeproxy"));
        }
        super.readFromNBT(data);

        if (data.hasKey("PatternInventory")) {
            this.readLegacyNBT(data);
        } else {
            this.sides.readFromNBT(data.getCompoundTag("sides"));
            final EnumFacing saved = EnumFacing.byName(data.getString("front"));
            this.front = saved == null ? EnumFacing.NORTH : saved;
        }
        this.customName = data.hasKey("customName") ? data.getString("customName") : null;
        this.customIcon = data.hasKey("customIcon") ? new ItemStack(data.getCompoundTag("customIcon")) : ItemStack.EMPTY;
    }

    @Override
    public NBTTagCompound writeToNBT(final NBTTagCompound data) {
        super.writeToNBT(data);
        data.setString("front", this.front.getName());
        if (this.customName != null) {
            data.setString("customName", this.customName);
        }
        if (!this.customIcon.isEmpty()) {
            data.setTag("customIcon", this.customIcon.writeToNBT(new NBTTagCompound()));
        }
        final NBTTagCompound sideTag = new NBTTagCompound();
        this.sides.writeToNBT(sideTag);
        data.setTag("sides", sideTag);
        return data;
    }

    /**
     * A machine the old mod saved: every field under its own name with a capital letter, and its inventories
     * as compounds of that name.
     */
    private void readLegacyNBT(final NBTTagCompound data) {
        TileProcessor.legacyInventory(this.patterns, data.getCompoundTag("PatternInventory"));
        TileProcessor.legacyInventory(this.imports, data.getCompoundTag("ImportInventory"));
        TileProcessor.legacyInventory(this.exports, data.getCompoundTag("ExportInventory"));
        this.sides.readFromNBT(data.getCompoundTag("Sides"));
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
        changed |= this.sides.readFromStream(data);
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
        this.sides.writeToStream(data);
        ByteBufUtils.writeUTF8String(data, this.getCustomInventoryName());
    }

    @Override
    public void getDrops(final World world, final BlockPos pos, final List<ItemStack> drops) {
        TileProcessor.dropAll(this.inventory, drops);
    }

    /**
     * What the crafting CPU laid out on the table, which is what actually goes to the machine: it holds the
     * items the network really took, substitutions included.
     */
    private static List<ItemStack> contentsOf(final InventoryCrafting table) {
        final List<ItemStack> ingredients = new ArrayList<>(table.getSizeInventory());
        for (int slot = 0; slot < table.getSizeInventory(); slot++) {
            final ItemStack stack = table.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                ingredients.add(stack);
            }
        }
        return ingredients;
    }

    /**
     * The item half of a pattern's ingredients, for the question asked before the table is laid out.
     */
    private static List<ItemStack> itemsOf(final ICraftingPatternDetails details) {
        final List<ItemStack> ingredients = new ArrayList<>();
        for (final GenericStack input : details.getPatternInputs().getCondensed()) {
            if (input != null && input.what() instanceof AEItemKey) {
                ingredients.add(((AEItemKey) input.what()).toStack((int) input.amount()));
            }
        }
        return ingredients;
    }

    private static boolean isEmpty(final IItemHandler inventory) {
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            if (!inventory.getStackInSlot(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
