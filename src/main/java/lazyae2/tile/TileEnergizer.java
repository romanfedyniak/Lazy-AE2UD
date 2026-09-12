/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.tile;

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
import lazyae2.recipe.EnergizeRecipe;
import lazyae2.recipe.LazyRecipes;
import lazyae2.util.IoMode;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.util.inv.InvOperation;
import appeng.util.inv.WrapperChainedItemHandler;
import appeng.util.inv.WrapperFilteredItemHandler;

/**
 * The Crystal Energizer: AE2's charger done on this mod's own power, taking whatever a recipe prices rather
 * than one charge for everything.
 */
public final class TileEnergizer extends TileProcessor {

    public static final int UPGRADE_SLOTS = 8;

    /** What a charge costs while nothing in the slot names a price, as the old mod had it. */
    private static final int DEFAULT_COST = 8100;

    private final AppEngInternalInventory input = new AppEngInternalInventory(this, 1);
    private final AppEngInternalInventory output = new AppEngInternalInventory(this, 1);
    private final AppEngInternalInventory upgrades;

    private final IItemHandler inventory;
    private final IItemHandler insertOnly;
    private final IItemHandler extractOnly;
    private final IItemHandler both;

    @Nullable
    private EnergizeRecipe recipe;

    public TileEnergizer() {
        super(LazyAE2Config.instance().getEnergizer(), IoMode.OMNI);
        this.upgrades = new MachineUpgradeInventory(BlockMachine.Type.ENERGIZER, this, UPGRADE_SLOTS);
        this.inventory = new WrapperChainedItemHandler(this.input, this.output, this.upgrades);
        this.insertOnly = new WrapperFilteredItemHandler(this.input, SlotFilters.INSERT_ONLY);
        this.extractOnly = new WrapperFilteredItemHandler(this.output, SlotFilters.EXTRACT_ONLY);
        this.both = new WrapperChainedItemHandler(this.insertOnly, this.extractOnly);
    }

    public IItemHandler getInputSlot() {
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

    /**
     * What the crystal in the slot costs to charge, rather than one price for everything the machine makes.
     */
    @Override
    protected int baseEnergyCost() {
        return this.recipe == null ? DEFAULT_COST : this.recipe.getEnergy();
    }

    @Override
    protected boolean recomputeCanWork() {
        final EnergizeRecipe found = LazyRecipes.findEnergizer(this.input.getStackInSlot(0));
        if (found == null) {
            this.recipe = null;
            return false;
        }
        if (found != this.recipe) {
            this.recipe = found;
            this.markEnergyCostDirty();
            this.resetWork();
        }
        return found.fits(this.output.getStackInSlot(0));
    }

    @Override
    protected void finishWork() {
        if (this.recipe == null) {
            return;
        }

        final ItemStack taken = this.input.getStackInSlot(0).copy();
        taken.shrink(1);
        this.input.setStackInSlot(0, taken.isEmpty() ? ItemStack.EMPTY : taken);

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
        legacySlot(this.input, 0, data.getCompoundTag("SlotInput"));
        legacySlot(this.output, 0, data.getCompoundTag("SlotOutput"));
    }

    @Override
    public void getDrops(final World world, final BlockPos pos, final List<ItemStack> drops) {
        dropAll(this.inventory, drops);
    }
}
