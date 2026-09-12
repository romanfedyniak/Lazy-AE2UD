/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.tile;

import java.io.IOException;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.netty.buffer.ByteBuf;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;

import lazyae2.core.LazyAE2Config;
import lazyae2.util.IoMode;
import lazyae2.util.RelativeSide;
import lazyae2.util.SideConfig;
import appeng.api.config.Settings;
import appeng.api.config.YesNo;
import appeng.api.implementations.IUpgradeableHost;
import appeng.api.upgrades.CardTrait;
import appeng.api.util.IConfigManager;
import appeng.tile.AEBaseInvTile;
import appeng.util.InventoryAdaptor;
import appeng.util.ConfigManager;
import appeng.util.IConfigManagerHost;

/**
 * A machine that turns a recipe into an item on power of its own. It is no part of an ME network: energy
 * arrives as Forge Energy through any face, and items move through whichever faces the player opened.
 */
public abstract class TileProcessor extends AEBaseInvTile implements ITickable, IConfigManagerHost, IUpgradeableHost {

    /** How often the machine hands what it made to the inventory beside it, in ticks. */
    private static final int EXPORT_INTERVAL = 16;

    private final LazyAE2Config.Processor settings;
    private final SideConfig sides;
    private final IConfigManager config = new ConfigManager(this);
    private final ProcessorEnergy energy;

    private EnumFacing front = EnumFacing.NORTH;
    private int work;
    private boolean working;
    /** Worked out once and kept until an upgrade card moves. */
    private int maxWork = -1;
    private int energyPerTick = -1;
    /** Whether the slots hold a recipe that can run; unknown until they are looked at. */
    @Nullable
    private Boolean canWork;

    protected TileProcessor(final LazyAE2Config.Processor settings, final IoMode initialSides) {
        this.settings = settings;
        this.sides = new SideConfig(initialSides);
        this.energy = new ProcessorEnergy(settings.getEnergyBuffer(), this::saveChanges);
        this.config.registerSetting(Settings.AUTO_EXPORT, YesNo.NO);
    }

    public abstract IItemHandlerModifiable getUpgradeInventory();

    /**
     * What the machine made, and what auto-export hands on.
     */
    protected abstract IItemHandlerModifiable getOutputInventory();

    /**
     * Whether the machine has a recipe it can run right now. Asked only when something moved.
     */
    protected abstract boolean recomputeCanWork();

    /**
     * Called once the work counter has run its course: the inputs are spent and the output appears.
     */
    protected abstract void finishWork();

    /**
     * What a hopper or a pipe against a face of this mode may touch.
     */
    protected abstract IItemHandler getAutomationInventory(IoMode mode);

    public SideConfig getSides() {
        return this.sides;
    }

    public ProcessorEnergy getEnergy() {
        return this.energy;
    }

    public int getWork() {
        return this.work;
    }

    public boolean isWorking() {
        return this.working;
    }

    public float getWorkFraction() {
        return Math.min((float) this.work / this.getMaxWork(), 1F);
    }

    public boolean isAutoExporting() {
        return this.config.getSetting(Settings.AUTO_EXPORT) == YesNo.YES;
    }

    public EnumFacing getFront() {
        return this.front;
    }

    public void setFront(final EnumFacing facing) {
        this.front = facing.getAxis().isHorizontal() ? facing : EnumFacing.NORTH;
        this.saveChanges();
        this.markForUpdate();
    }

    /**
     * The number of cards, not their upgrade points: the old mod counted cards, and the config numbers are
     * written for cards.
     */
    public int getUpgradeCount() {
        int cards = 0;
        final IItemHandlerModifiable upgrades = this.getUpgradeInventory();
        for (int slot = 0; slot < upgrades.getSlots(); slot++) {
            if (!upgrades.getStackInSlot(slot).isEmpty()) {
                cards++;
            }
        }
        return cards;
    }

    public int getMaxWork() {
        if (this.maxWork == -1) {
            this.maxWork = Math.max(1,
                    this.settings.getWorkTicksBase() - this.settings.getWorkTicksUpgrade() * this.getUpgradeCount());
        }
        return this.maxWork;
    }

    /**
     * A recipe's whole cost, spread over the ticks it takes.
     */
    public int getEnergyPerTick() {
        if (this.energyPerTick == -1) {
            this.energyPerTick = (this.baseEnergyCost() + this.settings.getEnergyCostUpgrade() * this.getUpgradeCount())
                    / this.getMaxWork();
        }
        return this.energyPerTick;
    }

    protected int baseEnergyCost() {
        return this.settings.getEnergyCostBase();
    }

    protected void markWorkStateDirty() {
        this.canWork = null;
    }

    /**
     * For a machine whose price is the recipe's rather than the config's, when that recipe changes.
     */
    protected final void markEnergyCostDirty() {
        this.energyPerTick = -1;
    }

    protected void markUpgradesDirty() {
        this.maxWork = -1;
        this.energyPerTick = -1;
        this.markWorkStateDirty();
    }

    protected void resetWork() {
        this.work = 0;
    }

    @Override
    public void update() {
        if (this.world.isRemote) {
            return;
        }

        if (this.canWork == null) {
            this.canWork = this.recomputeCanWork();
        }

        if (this.canWork) {
            final int cost = this.getEnergyPerTick();
            if (this.energy.getEnergyStored() > cost) {
                this.energy.consume(cost);
                if (++this.work > this.getMaxWork()) {
                    this.work = 0;
                    this.finishWork();
                }
                this.setWorking(true);
                this.saveChanges();
            } else {
                this.setWorking(false);
            }
        } else if (this.work != 0 || this.working) {
            this.work = 0;
            this.setWorking(false);
            this.saveChanges();
        }

        if (this.isAutoExporting() && this.world.getTotalWorldTime() % EXPORT_INTERVAL == 0) {
            this.exportOutput();
        }
    }

    private void setWorking(final boolean nowWorking) {
        if (this.working != nowWorking) {
            this.working = nowWorking;
            this.markForUpdate();
        }
    }

    /**
     * Hands what the machine made to whatever stands against a face that lets items out. Through AE2's own
     * adaptor, which also finds an inventory that has no item handler of its own.
     */
    private void exportOutput() {
        final IItemHandlerModifiable output = this.getOutputInventory();
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

            for (int slot = 0; slot < output.getSlots(); slot++) {
                final ItemStack made = output.getStackInSlot(slot);
                if (made.isEmpty()) {
                    continue;
                }

                // Asked before anything is taken out, so a refusing neighbour costs no inventory change
                final ItemStack refused = target.simulateAdd(made.copy());
                final int movable = made.getCount() - refused.getCount();
                if (movable <= 0) {
                    continue;
                }

                final ItemStack left = target.addItems(output.extractItem(slot, movable, false));
                if (!left.isEmpty()) {
                    output.insertItem(slot, left, false);
                }
                moved = true;
            }
        }

        if (moved) {
            this.saveChanges();
        }
    }

    @Override
    public boolean canBeRotated() {
        return false; // the machine is turned by the face it was placed against, and a wrench turns it
    }

    @Override
    public IConfigManager getConfigManager() {
        return this.config;
    }

    @Override
    public void updateSetting(final IConfigManager manager, final Enum settingName, final Enum newValue) {
        this.saveChanges();
    }

    @Override
    public int getInstalledUpgrades(final ItemStack upgradeCard) {
        return this.getUpgradeCount();
    }

    @Override
    public int getInstalledPoints(final CardTrait trait) {
        return this.getUpgradeCount();
    }

    @Override
    public TileEntity getTile() {
        return this;
    }

    @Override
    public boolean hasCapability(@Nonnull final Capability<?> capability, @Nullable final EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) {
            return true;
        }
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && facing != null) {
            return this.sides.get(RelativeSide.of(this.front, facing)) != IoMode.NONE;
        }
        return super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(@Nonnull final Capability<T> capability, @Nullable final EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) {
            return CapabilityEnergy.ENERGY.cast(this.energy);
        }
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && facing != null) {
            final IoMode mode = this.sides.get(RelativeSide.of(this.front, facing));
            return mode == IoMode.NONE ? null
                    : CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(this.getAutomationInventory(mode));
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public void readFromNBT(final NBTTagCompound data) {
        super.readFromNBT(data);
        if (data.hasKey("Energy")) {
            this.readLegacyNBT(data);
        } else {
            this.work = data.getInteger("work");
            this.energy.setEnergyStored(data.getInteger("energy"));
            this.sides.readFromNBT(data.getCompoundTag("sides"));
            this.front = EnumFacing.byName(data.getString("front")) == null ? EnumFacing.NORTH
                    : EnumFacing.byName(data.getString("front"));
            this.config.readFromNBT(data);
        }
        this.markUpgradesDirty();
    }

    @Override
    public NBTTagCompound writeToNBT(final NBTTagCompound data) {
        super.writeToNBT(data);
        data.setInteger("work", this.work);
        data.setInteger("energy", this.energy.getEnergyStored());
        data.setString("front", this.front.getName());
        final NBTTagCompound sideTag = new NBTTagCompound();
        this.sides.writeToNBT(sideTag);
        data.setTag("sides", sideTag);
        this.config.writeToNBT(data);
        return data;
    }

    /**
     * A machine the old mod saved. Its library wrote every field under its own name with a capital letter,
     * and anything of its own into a compound of that name.
     */
    protected void readLegacyNBT(final NBTTagCompound data) {
        this.work = data.getInteger("Work");
        this.energy.setEnergyStored(data.getCompoundTag("Energy").getInteger("Quantity"));
        this.sides.readFromNBT(data.getCompoundTag("Sides"));
        this.config.putSetting(Settings.AUTO_EXPORT, data.getBoolean("AutoExporting") ? YesNo.YES : YesNo.NO);
        this.front = EnumFacing.byIndex(data.getShort("FrontFace"));
        if (!this.front.getAxis().isHorizontal()) {
            this.front = EnumFacing.NORTH;
        }
        legacySlot(this.getUpgradeInventory(), 0, data.getCompoundTag("SlotUpgrade"));
    }

    /**
     * The one stack of an old library slot. A stack of Acceleration Cards becomes one card per slot, which is
     * how upgrades are held here.
     */
    protected static void legacySlot(final IItemHandlerModifiable inventory, final int slot, final NBTTagCompound tag) {
        final NBTTagCompound item = tag.getCompoundTag("Item");
        if (item.getKeySet().isEmpty() || item.getBoolean("Empty")) {
            return;
        }
        final ItemStack stack = new ItemStack(item);
        if (stack.isEmpty()) {
            return;
        }
        if (stack.getCount() == 1) {
            inventory.setStackInSlot(slot, stack);
            return;
        }
        for (int card = 0; card < stack.getCount() && slot + card < inventory.getSlots(); card++) {
            inventory.setStackInSlot(slot + card, ItemHandlerHelper.copyStackWithSize(stack, 1));
        }
    }

    /**
     * The stacks of an old library inventory, in order.
     */
    protected static void legacyInventory(final IItemHandlerModifiable inventory, final NBTTagCompound tag) {
        final NBTTagList list = tag.getTagList("Items", 10);
        for (int slot = 0; slot < Math.min(inventory.getSlots(), list.tagCount()); slot++) {
            final NBTTagCompound item = list.getCompoundTagAt(slot);
            if (!item.getKeySet().isEmpty() && !item.getBoolean("Empty")) {
                inventory.setStackInSlot(slot, new ItemStack(item));
            }
        }
    }

    @Override
    protected boolean readFromStream(final ByteBuf data) throws IOException {
        boolean changed = super.readFromStream(data);
        final boolean nowWorking = data.readBoolean();
        if (this.working != nowWorking) {
            this.working = nowWorking;
            changed = true;
        }
        final EnumFacing nowFront = EnumFacing.byIndex(data.readByte());
        if (this.front != nowFront) {
            this.front = nowFront;
            changed = true;
        }
        this.work = data.readInt();
        this.energy.setEnergyStored(data.readInt());
        changed |= this.sides.readFromStream(data);
        return changed;
    }

    @Override
    protected void writeToStream(final ByteBuf data) throws IOException {
        super.writeToStream(data);
        data.writeBoolean(this.working);
        data.writeByte(this.front.getIndex());
        data.writeInt(this.work);
        data.writeInt(this.energy.getEnergyStored());
        this.sides.writeToStream(data);
    }

    /**
     * Everything the machine holds falls out when it is broken, upgrades included.
     */
    protected static void dropAll(final IItemHandler inventory, final List<ItemStack> drops) {
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            final ItemStack stack = inventory.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                drops.add(stack);
            }
        }
    }
}
