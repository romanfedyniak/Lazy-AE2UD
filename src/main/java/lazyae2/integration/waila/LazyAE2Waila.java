/*
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.integration.waila;

import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;
import mcp.mobius.waila.api.IWailaDataProvider;
import mcp.mobius.waila.api.IWailaRegistrar;

import lazyae2.block.BlockAssembler;
import lazyae2.block.BlockMachine;
import lazyae2.integration.probe.ProbeLine;
import lazyae2.integration.probe.ProbeReport;

/**
 * What Waila shows for this mod's machines. The server works the lines out and sends them along; the client
 * only puts them into words.
 */
public final class LazyAE2Waila implements IWailaDataProvider {

    /** Called by Waila, named in an inter-mod message, so its classes load only when it is installed. */
    public static void register(final IWailaRegistrar registrar) {
        final LazyAE2Waila provider = new LazyAE2Waila();
        for (final Class<?> block : new Class<?>[] { BlockMachine.class, BlockAssembler.class }) {
            registrar.registerBodyProvider(provider, block);
            registrar.registerNBTProvider(provider, block);
        }
    }

    @Override
    public List<String> getWailaBody(final ItemStack stack, final List<String> tooltip,
            final IWailaDataAccessor accessor, final IWailaConfigHandler config) {
        for (final ProbeLine line : ProbeReport.read(accessor.getNBTData())) {
            tooltip.add(line.toText());
        }
        return tooltip;
    }

    @Override
    public NBTTagCompound getNBTData(final EntityPlayerMP player, final TileEntity tile, final NBTTagCompound tag,
            final World world, final BlockPos pos) {
        if (tile != null) {
            ProbeReport.write(ProbeReport.of(tile), tag);
        }
        return tag;
    }
}
