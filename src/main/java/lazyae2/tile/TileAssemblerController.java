/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.tile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import io.netty.buffer.ByteBuf;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.IItemHandler;

import lazyae2.block.BlockAssembler;
import lazyae2.core.LazyAE2Config;
import appeng.api.networking.GridFlags;
import appeng.core.MultiblockLimits;
import appeng.hooks.TickHandler;
import appeng.tile.grid.AENetworkTile;
import appeng.util.inv.WrapperChainedItemHandler;

/**
 * The controller of a Mass Assembly Chamber: the block a player clicks to assemble the chamber around it, and
 * the one that connects it to the network.
 */
public final class TileAssemblerController extends AENetworkTile implements BlockAssembler.IAssemblerBlock {

    /** Everything the old mod's chamber was doing, kept as it was saved until the chamber can read it. */
    private static final String[] LEGACY_WORK = { "JobQueue", "OutputBuffer", "CraftingBuffer", "CpuCount", "Work" };

    private boolean assembled;
    @Nullable
    private BlockPos min;
    @Nullable
    private BlockPos max;
    private int parallel;
    private List<BlockPos> patternModules = Collections.emptyList();

    /** Set by a chamber the old mod saved assembled, which never wrote down where its walls are. */
    private boolean legacyAssembled;
    private final NBTTagCompound legacyWork = new NBTTagCompound();

    public TileAssemblerController() {
        this.getProxy().setFlags(GridFlags.REQUIRE_CHANNEL);
        this.getProxy().setIdlePowerUsage(LazyAE2Config.instance().getMassAssemblerIdlePower());
        this.getProxy().setVisualRepresentation(BlockAssembler.Type.CONTROLLER.newStack(1));
    }

    @Override
    public boolean isAssembled() {
        return this.assembled;
    }

    /** How many crafts the chamber runs at once, or none while it is not assembled. */
    public int getParallel() {
        return this.assembled ? this.parallel : 0;
    }

    public int getPatternModuleCount() {
        return this.patternModules.size();
    }

    /**
     * A click on the controller: assembles the chamber, or takes an assembled one apart, and says which - or,
     * when it cannot be assembled, why not.
     */
    public void toggleAssembly(final EntityPlayer player) {
        if (this.assembled) {
            this.disassemble();
            player.sendMessage(BlockAssembler.message("chat.threng.assembler.disassembled", TextFormatting.YELLOW));
            return;
        }

        final AssemblerStructure.Outcome outcome = AssemblerStructure.check(this.world, this.pos);
        if (outcome.layout == null) {
            player.sendMessage(outcome.refusal);
            return;
        }
        this.assemble(outcome.layout);
        final BlockPos size = outcome.layout.max.subtract(outcome.layout.min).add(1, 1, 1);
        player.sendMessage(BlockAssembler.message("chat.threng.assembler.assembled", TextFormatting.GREEN,
                size.getX(), size.getY(), size.getZ(), this.parallel, this.patternModules.size()));
    }

    private void assemble(final AssemblerStructure.Layout layout) {
        this.min = layout.min;
        this.max = layout.max;
        this.parallel = layout.parallel;
        this.patternModules = new ArrayList<>(layout.patternModules);
        this.assembled = true;
        this.legacyAssembled = false;

        for (final BlockPos at : BlockPos.getAllInBox(layout.min, layout.max)) {
            final TileEntity tile = this.world.getTileEntity(at);
            if (tile instanceof TileAssemblerPart) {
                ((TileAssemblerPart) tile).setControllerPos(this.pos);
            }
        }
        this.onPatternsChanged();
        this.saveChanges();
        this.markForUpdate();
    }

    /**
     * Takes the chamber apart. A block in a chunk that is not loaded still points here and still looks assembled
     * until it is assembled again or broken; nothing it does depends on that, since a controller that is not
     * assembled offers no patterns and takes no work.
     */
    public void disassemble() {
        if (!this.assembled && !this.legacyAssembled) {
            return;
        }
        this.assembled = false;
        this.legacyAssembled = false;

        if (this.world != null) {
            // An old chamber never said where its walls are, so every block that could be one is asked
            final MultiblockLimits.Limit limit = MultiblockLimits.get(AssemblerStructure.LIMIT);
            final BlockPos from = this.min != null ? this.min
                    : this.pos.add(1 - limit.getX(), 1 - limit.getY(), 1 - limit.getZ());
            final BlockPos to = this.max != null ? this.max
                    : this.pos.add(limit.getX() - 1, limit.getY() - 1, limit.getZ() - 1);
            for (final BlockPos at : BlockPos.getAllInBox(from, to)) {
                if (!this.world.isBlockLoaded(at)) {
                    continue;
                }
                final TileEntity tile = this.world.getTileEntity(at);
                if (tile instanceof TileAssemblerPart && this.pos.equals(((TileAssemblerPart) tile).getControllerPos())) {
                    ((TileAssemblerPart) tile).setControllerPos(null);
                }
            }
        }
        this.min = null;
        this.max = null;
        this.parallel = 0;
        this.patternModules = Collections.emptyList();
        this.onPatternsChanged();
        this.saveChanges();
        this.markForUpdate();
    }

    /** Called when a pattern module of this chamber changes, and when the chamber is assembled or taken apart. */
    void onPatternsChanged() {
    }

    /** Every pattern slot of the chamber, module after module, or null while it is not assembled. */
    @Nullable
    public IItemHandler getAllPatterns() {
        if (!this.assembled) {
            return null;
        }
        final List<IItemHandler> handlers = new ArrayList<>(this.patternModules.size());
        for (final BlockPos at : this.patternModules) {
            if (!this.world.isBlockLoaded(at)) {
                continue;
            }
            final TileEntity tile = this.world.getTileEntity(at);
            if (tile instanceof TileAssemblerPatterns) {
                handlers.add(((TileAssemblerPatterns) tile).getPatterns());
            }
        }
        return new WrapperChainedItemHandler(handlers.toArray(new IItemHandler[0]));
    }

    /**
     * A chamber is checked again once its controller is back, in case a wall went while it was unloaded - and a
     * chamber the old mod saved is assembled again here, having never said where its walls are. The check waits
     * for the end of the tick, when the chunks around have loaded too.
     */
    @Override
    public void onReady() {
        super.onReady();
        if (!this.assembled && !this.legacyAssembled) {
            return;
        }
        TickHandler.INSTANCE.addCallable(this.world, world -> {
            this.recheck();
            return null;
        });
    }

    private void recheck() {
        if (this.isInvalid() || (!this.assembled && !this.legacyAssembled)) {
            return;
        }
        if (this.min != null && this.max != null && !this.world.isAreaLoaded(this.min, this.max)) {
            return;
        }
        final AssemblerStructure.Outcome outcome = AssemblerStructure.check(this.world, this.pos);
        if (outcome.layout != null) {
            this.assemble(outcome.layout);
        } else {
            this.disassemble();
        }
    }

    @Override
    public boolean canBeRotated() {
        return false;
    }

    @Override
    public void readFromNBT(final NBTTagCompound data) {
        // The old mod's library kept the network node under a name of its own
        final boolean legacy = data.hasKey("MultiBlock");
        if (legacy && !data.hasKey("proxy")) {
            data.setTag("proxy", data.getCompoundTag("aeproxy"));
        }
        super.readFromNBT(data);

        if (legacy) {
            this.legacyAssembled = data.getCompoundTag("MultiBlock").getBoolean("Formed");
            for (final String key : LEGACY_WORK) {
                if (data.hasKey(key)) {
                    this.legacyWork.setTag(key, data.getTag(key).copy());
                }
            }
            return;
        }

        this.assembled = data.getBoolean("assembled");
        if (this.assembled) {
            this.min = BlockPos.fromLong(data.getLong("min"));
            this.max = BlockPos.fromLong(data.getLong("max"));
            this.parallel = data.getInteger("parallel");
            final NBTTagList modules = data.getTagList("patternModules", Constants.NBT.TAG_LONG);
            this.patternModules = new ArrayList<>(modules.tagCount());
            for (int i = 0; i < modules.tagCount(); i++) {
                this.patternModules.add(BlockPos.fromLong(((NBTTagLong) modules.get(i)).getLong()));
            }
        }
        this.legacyAssembled = data.getBoolean("legacyAssembled");
        final NBTTagCompound work = data.getCompoundTag("legacyWork");
        for (final String key : work.getKeySet()) {
            this.legacyWork.setTag(key, work.getTag(key).copy());
        }
    }

    @Override
    public NBTTagCompound writeToNBT(final NBTTagCompound data) {
        super.writeToNBT(data);
        if (this.assembled && this.min != null && this.max != null) {
            data.setBoolean("assembled", true);
            data.setLong("min", this.min.toLong());
            data.setLong("max", this.max.toLong());
            data.setInteger("parallel", this.parallel);
            final NBTTagList modules = new NBTTagList();
            for (final BlockPos at : this.patternModules) {
                modules.appendTag(new NBTTagLong(at.toLong()));
            }
            data.setTag("patternModules", modules);
        }
        if (this.legacyAssembled) {
            data.setBoolean("legacyAssembled", true);
        }
        if (!this.legacyWork.isEmpty()) {
            data.setTag("legacyWork", this.legacyWork.copy());
        }
        return data;
    }

    @Override
    protected boolean readFromStream(final ByteBuf data) throws IOException {
        final boolean changed = super.readFromStream(data);
        final boolean nowAssembled = data.readBoolean();
        if (nowAssembled == this.assembled) {
            return changed;
        }
        this.assembled = nowAssembled;
        return true;
    }

    @Override
    protected void writeToStream(final ByteBuf data) throws IOException {
        super.writeToStream(data);
        data.writeBoolean(this.assembled);
    }
}
