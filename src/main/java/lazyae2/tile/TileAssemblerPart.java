/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.tile;

import java.io.IOException;

import javax.annotation.Nullable;

import io.netty.buffer.ByteBuf;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

import lazyae2.block.BlockAssembler;
import appeng.tile.AEBaseTile;

/**
 * Any block of a chamber but its controller. All it knows is which controller, if any, assembled it.
 */
public class TileAssemblerPart extends AEBaseTile implements BlockAssembler.IAssemblerBlock {

    @Nullable
    private BlockPos controller;
    /** All the client is told: whether there is a controller, not where. */
    private boolean assembledOnClient;

    /** The controller that assembled this block, while its chunk is loaded. */
    @Nullable
    public TileAssemblerController getController() {
        if (this.controller == null || this.world == null || !this.world.isBlockLoaded(this.controller)) {
            return null;
        }
        final TileEntity tile = this.world.getTileEntity(this.controller);
        return tile instanceof TileAssemblerController ? (TileAssemblerController) tile : null;
    }

    @Nullable
    public BlockPos getControllerPos() {
        return this.controller;
    }

    void setControllerPos(@Nullable final BlockPos controller) {
        if (controller == null ? this.controller == null : controller.equals(this.controller)) {
            return;
        }
        this.controller = controller;
        this.saveChanges();
        this.markForUpdate();
        this.onAssemblyChanged();
    }

    /** For a block that has to tell anything when it joins or leaves a chamber. */
    protected void onAssemblyChanged() {
    }

    @Override
    public boolean isAssembled() {
        return this.controller != null || this.assembledOnClient;
    }

    @Override
    public boolean canBeRotated() {
        return false;
    }

    @Override
    public void readFromNBT(final NBTTagCompound data) {
        super.readFromNBT(data);
        if (data.hasKey("controller")) {
            this.controller = BlockPos.fromLong(data.getLong("controller"));
        } else if (data.hasKey("MultiBlock")) {
            // The old mod's library kept the controller as three ints under its own name
            final NBTTagCompound old = data.getCompoundTag("MultiBlock");
            this.controller = old.hasKey("CorePos") ? legacyPos(old.getCompoundTag("CorePos")) : null;
        } else {
            this.controller = null;
        }
    }

    @Override
    public NBTTagCompound writeToNBT(final NBTTagCompound data) {
        super.writeToNBT(data);
        if (this.controller != null) {
            data.setLong("controller", this.controller.toLong());
        }
        return data;
    }

    @Override
    protected boolean readFromStream(final ByteBuf data) throws IOException {
        final boolean changed = super.readFromStream(data);
        final boolean assembled = data.readBoolean();
        if (assembled == this.isAssembled()) {
            return changed;
        }
        this.assembledOnClient = assembled;
        return true;
    }

    @Override
    protected void writeToStream(final ByteBuf data) throws IOException {
        super.writeToStream(data);
        data.writeBoolean(this.isAssembled());
    }

    static BlockPos legacyPos(final NBTTagCompound tag) {
        return new BlockPos(tag.getInteger("x"), tag.getInteger("y"), tag.getInteger("z"));
    }
}
