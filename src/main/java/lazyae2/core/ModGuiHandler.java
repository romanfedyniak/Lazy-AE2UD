/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.core;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.network.IGuiHandler;

import lazyae2.block.BlockMachine;
import lazyae2.client.gui.GuiAggregator;
import lazyae2.client.gui.GuiAssembler;
import lazyae2.client.gui.GuiCentrifuge;
import lazyae2.client.gui.GuiEnergizer;
import lazyae2.client.gui.GuiLevelMaintainer;
import lazyae2.client.gui.GuiLevelMaintainerTerminal;
import lazyae2.client.gui.GuiPau;
import lazyae2.client.gui.GuiEtcher;
import lazyae2.client.gui.GuiWirelessLevelMaintainerTerminal;
import lazyae2.container.ContainerAggregator;
import lazyae2.container.ContainerAssembler;
import lazyae2.container.ContainerCentrifuge;
import lazyae2.container.ContainerEnergizer;
import lazyae2.container.ContainerLevelMaintainer;
import lazyae2.container.ContainerLevelMaintainerTerminal;
import lazyae2.container.ContainerPau;
import lazyae2.container.ContainerEtcher;
import lazyae2.container.ContainerWirelessLevelMaintainerTerminal;
import lazyae2.part.PartLevelMaintainerTerminal;
import lazyae2.tile.TileAggregator;
import lazyae2.tile.TileAssemblerController;
import lazyae2.tile.TileCentrifuge;
import lazyae2.tile.TileEnergizer;
import lazyae2.tile.TileLevelMaintainer;
import lazyae2.tile.TilePau;
import lazyae2.tile.TileEtcher;
import appeng.api.AEApi;
import appeng.api.features.IWirelessTermHandler;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.util.AEPartLocation;
import appeng.container.AEBaseContainer;
import appeng.container.ContainerOpenContext;
import appeng.helpers.WirelessTerminalGuiObject;
import appeng.util.Platform;
import baubles.api.BaublesApi;

/**
 * Which window a machine opens. The id is the machine's subtype, so a new machine needs nothing here beyond
 * its own case. The terminal is no machine and takes ids of its own, past every subtype a block could have.
 */
public final class ModGuiHandler implements IGuiHandler {

    /** The terminal on a cable, one id for each face it can sit on. */
    public static final int TERMINAL = 16;

    /** The same terminal carried: x is the slot it sits in, and y says whether that is a bauble slot. */
    public static final int WIRELESS_TERMINAL = 24;

    /** A Mass Assembly Chamber, opened at its controller whichever of its blocks was clicked. */
    public static final int ASSEMBLER = 32;

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
        if (BlockMachine.Type.of(id) == BlockMachine.Type.ENERGIZER && tile instanceof TileEnergizer) {
            return withContext(new ContainerEnergizer(player.inventory, (TileEnergizer) tile), world, x, y, z);
        }
        if (BlockMachine.Type.of(id) == BlockMachine.Type.PAU && tile instanceof TilePau) {
            return withContext(new ContainerPau(player.inventory, (TilePau) tile), world, x, y, z);
        }
        if (BlockMachine.Type.of(id) == BlockMachine.Type.LEVEL_MAINTAINER && tile instanceof TileLevelMaintainer) {
            return withContext(new ContainerLevelMaintainer(player.inventory, (TileLevelMaintainer) tile),
                    world, x, y, z);
        }
        if (id == ASSEMBLER && tile instanceof TileAssemblerController) {
            return withContext(new ContainerAssembler(player.inventory, (TileAssemblerController) tile), world, x, y, z);
        }
        if (id >= TERMINAL && id < TERMINAL + EnumFacing.VALUES.length) {
            final PartLevelMaintainerTerminal terminal = terminalAt(world, x, y, z, id - TERMINAL);
            return terminal == null ? null
                    : withContext(new ContainerLevelMaintainerTerminal(player.inventory, terminal), world, x, y, z,
                            sideOf(id));
        }
        if (id == WIRELESS_TERMINAL) {
            final WirelessTerminalGuiObject carried = carriedTerminal(player, x, y == 1);
            return carried == null ? null
                    : withContext(new ContainerWirelessLevelMaintainerTerminal(player.inventory, carried),
                            world, x, y, z);
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
        if (BlockMachine.Type.of(id) == BlockMachine.Type.ENERGIZER && tile instanceof TileEnergizer) {
            return new GuiEnergizer(
                    withContext(new ContainerEnergizer(player.inventory, (TileEnergizer) tile), world, x, y, z));
        }
        if (BlockMachine.Type.of(id) == BlockMachine.Type.PAU && tile instanceof TilePau) {
            return new GuiPau(withContext(new ContainerPau(player.inventory, (TilePau) tile), world, x, y, z));
        }
        if (BlockMachine.Type.of(id) == BlockMachine.Type.LEVEL_MAINTAINER && tile instanceof TileLevelMaintainer) {
            return new GuiLevelMaintainer(withContext(
                    new ContainerLevelMaintainer(player.inventory, (TileLevelMaintainer) tile), world, x, y, z));
        }
        if (id == ASSEMBLER && tile instanceof TileAssemblerController) {
            return new GuiAssembler(
                    withContext(new ContainerAssembler(player.inventory, (TileAssemblerController) tile), world, x, y, z));
        }
        if (id >= TERMINAL && id < TERMINAL + EnumFacing.VALUES.length) {
            final PartLevelMaintainerTerminal terminal = terminalAt(world, x, y, z, id - TERMINAL);
            return terminal == null ? null
                    : new GuiLevelMaintainerTerminal(withContext(
                            new ContainerLevelMaintainerTerminal(player.inventory, terminal), world, x, y, z,
                            sideOf(id)), terminal);
        }
        if (id == WIRELESS_TERMINAL) {
            final WirelessTerminalGuiObject carried = carriedTerminal(player, x, y == 1);
            return carried == null ? null
                    : new GuiWirelessLevelMaintainerTerminal(withContext(
                            new ContainerWirelessLevelMaintainerTerminal(player.inventory, carried),
                            world, x, y, z));
        }
        return null;
    }

    private static AEPartLocation sideOf(final int id) {
        return AEPartLocation.fromFacing(EnumFacing.byIndex(id - TERMINAL));
    }

    @Nullable
    private static PartLevelMaintainerTerminal terminalAt(final World world, final int x, final int y, final int z,
            final int facing) {
        final TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
        if (!(tile instanceof IPartHost)) {
            return null;
        }

        final IPart part = ((IPartHost) tile).getPart(AEPartLocation.fromFacing(EnumFacing.byIndex(facing)));
        return part instanceof PartLevelMaintainerTerminal ? (PartLevelMaintainerTerminal) part : null;
    }

    /**
     * The wireless terminal this window was opened from, wherever the player keeps it.
     */
    @Nullable
    private static WirelessTerminalGuiObject carriedTerminal(final EntityPlayer player, final int slot,
            final boolean bauble) {
        final ItemStack terminal;
        if (bauble) {
            terminal = Platform.isModLoaded("baubles") ? baubleStack(player, slot) : ItemStack.EMPTY;
        } else {
            terminal = player.inventory.getStackInSlot(slot);
        }

        if (terminal.isEmpty()) {
            return null;
        }

        final IWirelessTermHandler handler = AEApi.instance().registries().wireless()
                .getWirelessTerminalHandler(terminal);
        return handler == null ? null
                : new WirelessTerminalGuiObject(handler, terminal, player, player.world, slot, bauble ? 1 : 0,
                        Integer.MIN_VALUE);
    }

    @Optional.Method(modid = "baubles")
    private static ItemStack baubleStack(final EntityPlayer player, final int slot) {
        return BaublesApi.getBaublesHandler(player).getStackInSlot(slot);
    }

    private static <T extends AEBaseContainer> T withContext(final T container, final World world, final int x, final int y,
            final int z) {
        // A machine is opened from no particular face, which is to say from inside itself
        return withContext(container, world, x, y, z, AEPartLocation.INTERNAL);
    }

    private static <T extends AEBaseContainer> T withContext(final T container, final World world, final int x,
            final int y, final int z, final AEPartLocation side) {
        final ContainerOpenContext context = new ContainerOpenContext(container.getTarget());
        // A side is not optional: reopening this window - which is what typing an amount does on the way
        // back - reads it, and a part is only found again on the face it sits on
        context.setSide(side);
        context.setWorld(world);
        context.setX(x);
        context.setY(y);
        context.setZ(z);
        container.setOpenContext(context);
        return container;
    }
}
