/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.core;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

import lazyae2.block.BlockMachine;
import lazyae2.client.gui.GuiAggregator;
import lazyae2.client.gui.GuiCentrifuge;
import lazyae2.client.gui.GuiEtcher;
import lazyae2.container.ContainerAggregator;
import lazyae2.container.ContainerCentrifuge;
import lazyae2.container.ContainerEtcher;
import lazyae2.tile.TileAggregator;
import lazyae2.tile.TileCentrifuge;
import lazyae2.tile.TileEtcher;
import appeng.container.AEBaseContainer;
import appeng.container.ContainerOpenContext;

/**
 * Which window a machine opens. The id is the machine's subtype, so a new machine needs nothing here beyond
 * its own case.
 */
public final class ModGuiHandler implements IGuiHandler {

    @Nullable
    @Override
    public Object getServerGuiElement(final int id, final EntityPlayer player, final World world, final int x, final int y,
            final int z) {
        final TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
        if (BlockMachine.Type.of(id) == BlockMachine.Type.AGGREGATOR && tile instanceof TileAggregator) {
            return withContext(new ContainerAggregator(player.inventory, (TileAggregator) tile), world, x, y, z);
        }
        if (BlockMachine.Type.of(id) == BlockMachine.Type.CENTRIFUGE && tile instanceof TileCentrifuge) {
            return withContext(new ContainerCentrifuge(player.inventory, (TileCentrifuge) tile), world, x, y, z);
        }
        if (BlockMachine.Type.of(id) == BlockMachine.Type.ETCHER && tile instanceof TileEtcher) {
            return withContext(new ContainerEtcher(player.inventory, (TileEtcher) tile), world, x, y, z);
        }
        return null;
    }

    @Nullable
    @Override
    public Object getClientGuiElement(final int id, final EntityPlayer player, final World world, final int x, final int y,
            final int z) {
        final TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
        if (BlockMachine.Type.of(id) == BlockMachine.Type.AGGREGATOR && tile instanceof TileAggregator) {
            return new GuiAggregator(
                    withContext(new ContainerAggregator(player.inventory, (TileAggregator) tile), world, x, y, z));
        }
        if (BlockMachine.Type.of(id) == BlockMachine.Type.CENTRIFUGE && tile instanceof TileCentrifuge) {
            return new GuiCentrifuge(
                    withContext(new ContainerCentrifuge(player.inventory, (TileCentrifuge) tile), world, x, y, z));
        }
        if (BlockMachine.Type.of(id) == BlockMachine.Type.ETCHER && tile instanceof TileEtcher) {
            return new GuiEtcher(
                    withContext(new ContainerEtcher(player.inventory, (TileEtcher) tile), world, x, y, z));
        }
        return null;
    }

    private static <T extends AEBaseContainer> T withContext(final T container, final World world, final int x, final int y,
            final int z) {
        final ContainerOpenContext context = new ContainerOpenContext(container.getTarget());
        context.setWorld(world);
        context.setX(x);
        context.setY(y);
        context.setZ(z);
        container.setOpenContext(context);
        return container;
    }
}
