/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.tile;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

import lazyae2.block.BlockMachine;
import lazyae2.core.LazyAE2Config;
import lazyae2.recipe.AggregatorRecipe;
import lazyae2.recipe.LazyRecipes;
import lazyae2.recipe.TriItemRecipe;
import lazyae2.util.IoMode;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.util.inv.InvOperation;
import appeng.util.inv.WrapperChainedItemHandler;
import appeng.util.inv.WrapperFilteredItemHandler;
import appeng.util.inv.filter.IAEItemFilter;

/**
 * The Fluix Aggregator: the in-world crafting of three things thrown into a puddle, done in a machine.
 */
public final class TileAggregator extends TileProcessor {

    public static final int UPGRADE_SLOTS = 8;

    private final AppEngInternalInventory input = new AppEngInternalInventory(this, TriItemRecipe.SLOTS);
    private final AppEngInternalInventory output = new AppEngInternalInventory(this, 1);
    private final AppEngInternalInventory upgrades;

    private final IItemHandler inventory;
    private final IItemHandler insertOnly;
    private final IItemHandler extractOnly;
    private final IItemHandler both;

    @Nullable
    private AggregatorRecipe recipe;

    public TileAggregator() {
        super(LazyAE2Config.instance().getAggregator(), IoMode.NONE);
        this.upgrades = new MachineUpgradeInventory(BlockMachine.Type.AGGREGATOR, this, UPGRADE_SLOTS);
        this.inventory = new WrapperChainedItemHandler(this.input, this.output, this.upgrades);
        this.insertOnly = new WrapperFilteredItemHandler(this.input, new InputOnly());
        this.extractOnly = new WrapperFilteredItemHandler(this.output, new OutputOnly());
        this.both = new WrapperChainedItemHandler(this.insertOnly, this.extractOnly);
    }

    public IItemHandler getInputSlots() {
        return this.input;
    }

    public IItemHandler getOutputSlot() {
        return this.output;
    }

    @Override
    public IItemHandlerModifiable getUpgradeInventory() {
        return this.upgrades;
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
            case "input":
                return this.input;
            case "output":
                return this.output;
            default:
                return null;
        }
    }

    @Override
    protected IItemHandlerModifiable getOutputInventory() {
        return this.output;
    }

    @Override
    protected IItemHandler getAutomationInventory(final IoMode mode) {
        switch (mode) {
            case INPUT:
                return this.insertOnly;
            case OUTPUT:
                return this.extractOnly;
            default:
                return this.both;
        }
    }

    @Override
    public void onChangeInventory(final IItemHandler inv, final int slot, final InvOperation operation,
            final ItemStack removed, final ItemStack added) {
        if (inv == this.upgrades) {
            this.markUpgradesDirty();
        } else {
            this.markWorkStateDirty();
        }
    }

    private List<ItemStack> slots() {
        final List<ItemStack> slots = new ArrayList<>(TriItemRecipe.SLOTS);
        for (int slot = 0; slot < TriItemRecipe.SLOTS; slot++) {
            slots.add(this.input.getStackInSlot(slot));
        }
        return slots;
    }

    @Override
    protected boolean recomputeCanWork() {
        final AggregatorRecipe found = LazyRecipes.findAggregator(this.slots());
        if (found == null) {
            this.recipe = null;
            return false;
        }
        if (found != this.recipe) {
            this.recipe = found;
            this.resetWork();
        }
        return found.fits(this.output.getStackInSlot(0));
    }

    @Override
    protected void finishWork() {
        if (this.recipe == null) {
            return;
        }

        final List<ItemStack> left = TriItemRecipe.consume(this.slots());
        for (int slot = 0; slot < TriItemRecipe.SLOTS; slot++) {
            this.input.setStackInSlot(slot, left.get(slot));
        }

        final ItemStack made = this.recipe.getOutput();
        final ItemStack held = this.output.getStackInSlot(0);
        if (held.isEmpty()) {
            this.output.setStackInSlot(0, made.copy());
        } else {
            final ItemStack grown = held.copy();
            grown.grow(made.getCount());
            this.output.setStackInSlot(0, grown);
        }
    }

    @Override
    protected void readLegacyNBT(final NBTTagCompound data) {
        super.readLegacyNBT(data);
        legacyInventory(this.input, data.getCompoundTag("InvInput"));
        legacySlot(this.output, 0, data.getCompoundTag("SlotOutput"));
    }

    @Override
    public void getDrops(final World world, final BlockPos pos, final List<ItemStack> drops) {
        dropAll(this.inventory, drops);
    }

    /**
     * Automation may put things in the three slots the recipe is read from, and nothing else.
     */
    private static final class InputOnly implements IAEItemFilter {

        @Override
        public boolean allowExtract(final IItemHandler inv, final int slot, final int amount) {
            return false;
        }

        @Override
        public boolean allowInsert(final IItemHandler inv, final int slot, final ItemStack stack) {
            return true;
        }
    }

    /**
     * ... and take out of the one slot the machine fills, and nothing else.
     */
    private static final class OutputOnly implements IAEItemFilter {

        @Override
        public boolean allowExtract(final IItemHandler inv, final int slot, final int amount) {
            return true;
        }

        @Override
        public boolean allowInsert(final IItemHandler inv, final int slot, final ItemStack stack) {
            return false;
        }
    }
}
