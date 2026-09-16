/*
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.integration.probe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.IItemHandler;

import appeng.api.networking.crafting.MachineIdentity;

import lazyae2.tile.RowState;
import lazyae2.tile.TileAssemblerController;
import lazyae2.tile.TileAssemblerPart;
import lazyae2.tile.TileLevelMaintainer;
import lazyae2.tile.TilePau;
import lazyae2.tile.TileProcessor;

/**
 * What The One Probe and Waila both show for this mod's machines, worked out once for the two of them.
 */
public final class ProbeReport {

    private static final String TAG = "threngProbe";

    private ProbeReport() {
    }

    /** The lines for a tile, on the server; none for a tile this mod has nothing to say about. */
    public static List<ProbeLine> of(final TileEntity tile) {
        if (tile instanceof TileProcessor) {
            return processor((TileProcessor) tile);
        }
        if (tile instanceof TileAssemblerController) {
            return chamber((TileAssemblerController) tile);
        }
        if (tile instanceof TileAssemblerPart) {
            return chamber(((TileAssemblerPart) tile).getController());
        }
        if (tile instanceof TileLevelMaintainer) {
            return maintainer((TileLevelMaintainer) tile);
        }
        if (tile instanceof TilePau) {
            return pau((TilePau) tile);
        }
        return Collections.emptyList();
    }

    private static List<ProbeLine> processor(final TileProcessor processor) {
        final ItemStack making = processor.getWorkOutput();
        if (making.isEmpty()) {
            return Collections.emptyList();
        }
        final List<ProbeLine> lines = new ArrayList<>();
        lines.add(ProbeLine.item("probe.threng.making", making));
        lines.add(ProbeLine.of("probe.threng.progress", Math.round(processor.getWorkFraction() * 100) + "%",
                TextFormatting.GREEN));
        return lines;
    }

    private static List<ProbeLine> chamber(final TileAssemblerController controller) {
        if (controller == null || !controller.isAssembled()) {
            return Collections.singletonList(ProbeLine.of("probe.threng.assembler.notAssembled", TextFormatting.RED));
        }
        final List<ProbeLine> lines = new ArrayList<>();
        final BlockPos size = controller.getSize();
        if (size != null) {
            lines.add(ProbeLine.of("probe.threng.assembler.size",
                    size.getX() + " × " + size.getY() + " × " + size.getZ()));
        }
        lines.add(ProbeLine.of("probe.threng.assembler.crafting", controller.getBusy() + " / " + controller.getParallel()));

        final IItemHandler patterns = controller.getAllPatterns();
        if (patterns != null) {
            int filled = 0;
            for (int slot = 0; slot < patterns.getSlots(); slot++) {
                if (!patterns.getStackInSlot(slot).isEmpty()) {
                    filled++;
                }
            }
            lines.add(ProbeLine.of("probe.threng.assembler.patterns", filled + " / " + patterns.getSlots()));
        }
        return lines;
    }

    /** How many rows are in each state, in the colours the machine's own window gives them. */
    private static List<ProbeLine> maintainer(final TileLevelMaintainer maintainer) {
        final int[] counts = new int[RowState.values().length];
        for (int row = 0; row < TileLevelMaintainer.ROWS; row++) {
            counts[maintainer.getRowState(row).ordinal()]++;
        }
        final List<ProbeLine> lines = new ArrayList<>();
        for (final RowState state : RowState.values()) {
            if (state != RowState.NONE && counts[state.ordinal()] > 0) {
                lines.add(ProbeLine.of(state.nameKey(), String.valueOf(counts[state.ordinal()]), state.tone()));
            }
        }
        return lines;
    }

    private static List<ProbeLine> pau(final TilePau pau) {
        final MachineIdentity neighbour = pau.getNeighbour();
        if (neighbour == null) {
            return Collections.singletonList(ProbeLine.of("probe.threng.pau.noTarget", TextFormatting.GRAY));
        }
        return Collections.singletonList(neighbour.getIcon().isEmpty()
                ? ProbeLine.named("probe.threng.pau.target", neighbour.getName())
                : ProbeLine.item("probe.threng.pau.target", neighbour.getIcon()));
    }

    /** The lines, for a probe whose server and client talk in NBT. */
    public static void write(final List<ProbeLine> lines, final NBTTagCompound tag) {
        final NBTTagList list = new NBTTagList();
        for (final ProbeLine line : lines) {
            list.appendTag(line.write());
        }
        tag.setTag(TAG, list);
    }

    public static List<ProbeLine> read(final NBTTagCompound tag) {
        final NBTTagList list = tag.getTagList(TAG, Constants.NBT.TAG_COMPOUND);
        final List<ProbeLine> lines = new ArrayList<>(list.tagCount());
        for (int i = 0; i < list.tagCount(); i++) {
            lines.add(ProbeLine.read(list.getCompoundTagAt(i)));
        }
        return lines;
    }
}
