/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.tile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.EmptyHandler;

/**
 * An IO port: every pattern of the chamber it stands in, for a pipe to fill or empty. Nothing while the
 * chamber is not assembled.
 */
public final class TileAssemblerIoPort extends TileAssemblerPart {

    @Override
    public boolean hasCapability(@Nonnull final Capability<?> capability, @Nullable final EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(@Nonnull final Capability<T> capability, @Nullable final EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            final TileAssemblerController controller = this.getController();
            final IItemHandler patterns = controller == null ? null : controller.getAllPatterns();
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(patterns == null ? EmptyHandler.INSTANCE : patterns);
        }
        return super.getCapability(capability, facing);
    }
}
