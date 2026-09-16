/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.tile;

import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.util.inv.IAEAppEngInventory;
import appeng.util.inv.InvOperation;
import appeng.util.inv.filter.IAEItemFilter;

/**
 * A pattern module: 36 patterns for the chamber it stands in.
 */
public final class TileAssemblerPatterns extends TileAssemblerPart implements IAEAppEngInventory {

    public static final int SLOTS = 36;

    private final AppEngInternalInventory patterns = new AppEngInternalInventory(this, SLOTS, 1, new IAEItemFilter() {
        @Override
        public boolean allowExtract(final IItemHandler inv, final int slot, final int amount) {
            return true;
        }

        /** A chamber is a crafting table many times over, so a processing pattern has no use here. */
        @Override
        public boolean allowInsert(final IItemHandler inv, final int slot, final ItemStack stack) {
            if (!(stack.getItem() instanceof ICraftingPatternItem) || TileAssemblerPatterns.this.world == null) {
                return false;
            }
            final ICraftingPatternDetails details = ((ICraftingPatternItem) stack.getItem())
                    .getPatternForItem(stack, TileAssemblerPatterns.this.world);
            return details != null && details.isCraftable() && !TileAssemblerPatterns.this.holdsAlready(stack);
        }
    });

    /**
     * Whether the same encoded pattern is already in the chamber - every module of it once assembled, only this
     * one before. A second copy would only take a slot: the chamber runs one recipe as many times at once as
     * one copy lets it.
     */
    private boolean holdsAlready(final ItemStack pattern) {
        final TileAssemblerController controller = this.getController();
        final IItemHandler patterns = controller == null ? null : controller.getAllPatterns();
        return contains(patterns == null ? this.patterns : patterns, pattern);
    }

    private static boolean contains(final IItemHandler inventory, final ItemStack pattern) {
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            final ItemStack held = inventory.getStackInSlot(slot);
            if (!held.isEmpty() && ItemStack.areItemsEqual(held, pattern) && ItemStack.areItemStackTagsEqual(held, pattern)) {
                return true;
            }
        }
        return false;
    }

    public IItemHandlerModifiable getPatterns() {
        return this.patterns;
    }

    @Override
    public void onChangeInventory(final IItemHandler inv, final int slot, final InvOperation operation,
            final ItemStack removed, final ItemStack added) {
        final TileAssemblerController controller = this.getController();
        if (controller != null) {
            controller.onPatternsChanged();
        }
        this.saveChanges();
    }

    @Override
    public void readFromNBT(final NBTTagCompound data) {
        super.readFromNBT(data);
        if (data.hasKey("PatternInv")) {
            TileProcessor.legacyInventory(this.patterns, data.getCompoundTag("PatternInv"));
        } else {
            this.patterns.readFromNBT(data, "patterns");
        }
    }

    @Override
    public NBTTagCompound writeToNBT(final NBTTagCompound data) {
        super.writeToNBT(data);
        this.patterns.writeToNBT(data, "patterns");
        return data;
    }

    @Override
    public void getDrops(final World world, final BlockPos pos, final List<ItemStack> drops) {
        TileProcessor.dropAll(this.patterns, drops);
    }
}
