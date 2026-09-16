/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.integration.opencomputers;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;

import lazyae2.tile.TileLevelMaintainer;

/**
 * What a computer may do with an ME Level Maintainer, apart from the component that carries it, so it can be
 * called without OpenComputers around. A row is numbered from 0, as the old mod numbered it.
 */
final class MaintainerCalls {

    private final TileLevelMaintainer tile;

    MaintainerCalls(final TileLevelMaintainer tile) {
        this.tile = tile;
    }

    boolean isRequestValid(final int row) {
        return this.tile.keyOf(check(row)) != null;
    }

    void clearRequest(final int row) {
        this.tile.setTarget(check(row), 0);
    }

    void clearAll() {
        for (int row = 0; row < TileLevelMaintainer.ROWS; row++) {
            this.tile.setTarget(row, 0);
        }
    }

    /** The item a row keeps, or nothing when the row is empty or keeps something that is no item. */
    ItemStack getRequestItem(final int row) {
        final AEKey key = this.tile.keyOf(check(row));
        return key instanceof AEItemKey ? ((AEItemKey) key).toStack(1) : ItemStack.EMPTY;
    }

    /** An empty stack clears the row, as a database slot with nothing in it did in the old mod. */
    void setRequestItem(final int row, final ItemStack stack, @Nullable final Long quantity,
            @Nullable final Long batch) {
        this.set(row, stack.isEmpty() ? null : AEItemKey.of(stack), quantity, batch);
    }

    void setRequestFluid(final int row, final String name, @Nullable final Long quantity,
            @Nullable final Long batch) {
        final Fluid fluid = FluidRegistry.getFluid(name);
        if (fluid == null) {
            throw new IllegalArgumentException("no such fluid: " + name);
        }
        this.set(row, AEFluidKey.of(fluid), quantity, batch);
    }

    void setRequestKey(final int row, final String key, @Nullable final Long quantity, @Nullable final Long batch) {
        AEKey parsed;
        try {
            parsed = AEKey.fromTagGeneric(JsonToNBT.getTagFromJson(key));
        } catch (final NBTException e) {
            parsed = null;
        }
        if (parsed == null) {
            throw new IllegalArgumentException("not a key: " + key);
        }
        this.set(row, parsed, quantity, batch);
    }

    /** Nothing for an empty row, whatever it was last set to keep. */
    long getRequestQuantity(final int row) {
        return this.isRequestValid(row) ? this.tile.getTarget(row) : 0;
    }

    /** Ignored on an empty row; zero clears the row. */
    void setRequestQuantity(final int row, final long quantity) {
        if (this.isRequestValid(row)) {
            this.tile.setTarget(row, quantity);
        }
    }

    long getRequestBatchSize(final int row) {
        return this.tile.getBatch(check(row));
    }

    /** Ignored on an empty row. */
    void setRequestBatchSize(final int row, final long batch) {
        if (this.isRequestValid(row)) {
            this.tile.setBatch(row, batch);
        }
    }

    boolean isRequestEnabled(final int row) {
        return this.tile.isRowEnabled(check(row));
    }

    void setRequestEnabled(final int row, final boolean enabled) {
        this.tile.setRowEnabled(check(row), enabled);
    }

    /** Everything about a row, or null for an empty one. */
    @Nullable
    Map<String, Object> getRequest(final int row) {
        final AEKey key = this.tile.keyOf(check(row));
        if (key == null) {
            return null;
        }
        final NBTTagCompound tag = new NBTTagCompound();
        key.toTagGeneric(tag);

        final Map<String, Object> request = new LinkedHashMap<>();
        request.put("type", key.getType().getId().toString());
        request.put("name", key.getId().toString());
        request.put("label", key.getDisplayName().getUnformattedText());
        request.put("key", tag.toString());
        request.put("quantity", this.tile.getTarget(row));
        request.put("batchSize", this.tile.getBatch(row));
        request.put("enabled", this.tile.isRowEnabled(row));
        request.put("state", this.tile.getRowState(row).name().toLowerCase(Locale.ROOT));
        return request;
    }

    private void set(final int row, @Nullable final AEKey key, @Nullable final Long quantity,
            @Nullable final Long batch) {
        check(row);
        if (key == null) {
            this.tile.setTarget(row, 0);
            return;
        }
        this.tile.getRequests().setStackInSlot(row, GenericStack.wrapInItemStack(key, 1));
        // Left out, the amount stays what the row had, and a row that had none keeps one
        this.tile.setTarget(row, quantity != null ? quantity : Math.max(1, this.tile.getTarget(row)));
        if (quantity != null && batch != null) {
            this.tile.setBatch(row, batch);
        }
    }

    private static int check(final int row) {
        if (row < 0 || row >= TileLevelMaintainer.ROWS) {
            throw new IllegalArgumentException("request index " + row + " out of bounds");
        }
        return row;
    }
}
