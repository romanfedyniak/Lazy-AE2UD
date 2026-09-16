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
import lazyae2.recipe.EtchRecipe;
import lazyae2.recipe.LazyRecipes;
import lazyae2.util.IoMode;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.util.inv.InvOperation;
import appeng.util.inv.WrapperChainedItemHandler;
import appeng.util.inv.WrapperFilteredItemHandler;
import appeng.util.inv.filter.IAEItemFilter;

/**
 * The ME Circuit Etcher: the Inscriber's work of pressing a processor out of a material, done without a
 * network and in one machine rather than four.
 */
public final class TileEtcher extends TileProcessor {

    public static final int UPGRADE_SLOTS = 8;

    private final AppEngInternalInventory input = new AppEngInternalInventory(this, EtchRecipe.SLOTS);
    private final AppEngInternalInventory output = new AppEngInternalInventory(this, 1);
    private final AppEngInternalInventory upgrades;

    private final IItemHandler inventory;
    private final IItemHandler insertOnly;
    private final IItemHandler extractOnly;
    private final IItemHandler both;

    @Nullable
    private EtchRecipe recipe;

    public TileEtcher() {
        super(LazyAE2Config.instance().getEtcher(), IoMode.OMNI);
        this.input.setFilter(new EtchFilter());
        this.upgrades = new MachineUpgradeInventory(BlockMachine.Type.ETCHER, this, UPGRADE_SLOTS);
        this.inventory = new WrapperChainedItemHandler(this.input, this.output, this.upgrades);
        this.insertOnly = new WrapperFilteredItemHandler(this.input, SlotFilters.INSERT_ONLY);
        this.extractOnly = new WrapperFilteredItemHandler(this.output, SlotFilters.EXTRACT_ONLY);
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
        final List<ItemStack> slots = new ArrayList<>(EtchRecipe.SLOTS);
        for (int slot = 0; slot < EtchRecipe.SLOTS; slot++) {
            slots.add(this.input.getStackInSlot(slot));
        }
        return slots;
    }

    @Override
    public ItemStack getWorkOutput() {
        return this.recipe == null ? ItemStack.EMPTY : this.recipe.getOutput();
    }

    @Override
    protected boolean recomputeCanWork() {
        final EtchRecipe found = LazyRecipes.findEtcher(this.slots());
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

        for (int slot = 0; slot < EtchRecipe.SLOTS; slot++) {
            final ItemStack left = this.input.getStackInSlot(slot).copy();
            left.shrink(1);
            this.input.setStackInSlot(slot, left.isEmpty() ? ItemStack.EMPTY : left);
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
     * Each slot takes only what a recipe reads out of it, so the two pressing agents cannot be swapped and
     * the material cannot be dropped in on top of them.
     */
    private static final class EtchFilter implements IAEItemFilter {

        @Override
        public boolean allowInsert(final IItemHandler inv, final int slot, final ItemStack stack) {
            return LazyRecipes.isEtchable(slot, stack);
        }

        @Override
        public boolean allowExtract(final IItemHandler inv, final int slot, final int amount) {
            return true;
        }
    }
}
