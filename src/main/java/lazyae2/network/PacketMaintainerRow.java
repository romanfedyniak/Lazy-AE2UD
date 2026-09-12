/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.network;

import io.netty.buffer.ByteBuf;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import lazyae2.container.ContainerLevelMaintainer;
import lazyae2.tile.TileLevelMaintainer;

/**
 * One row of the ME Level Maintainer as the player left it: what to keep, how much to order at a time, and
 * whether the row is doing anything at all.
 */
public final class PacketMaintainerRow implements IMessage {

    private int row;
    private long target;
    private long batch;
    private boolean enabled;

    public PacketMaintainerRow() {
    }

    public PacketMaintainerRow(final int row, final long target, final long batch, final boolean enabled) {
        this.row = row;
        this.target = target;
        this.batch = batch;
        this.enabled = enabled;
    }

    @Override
    public void fromBytes(final ByteBuf buf) {
        this.row = buf.readByte();
        this.target = buf.readLong();
        this.batch = buf.readLong();
        this.enabled = buf.readBoolean();
    }

    @Override
    public void toBytes(final ByteBuf buf) {
        buf.writeByte(this.row);
        buf.writeLong(this.target);
        buf.writeLong(this.batch);
        buf.writeBoolean(this.enabled);
    }

    public static final class Handler implements IMessageHandler<PacketMaintainerRow, IMessage> {

        @Override
        public IMessage onMessage(final PacketMaintainerRow message, final MessageContext context) {
            final EntityPlayerMP player = context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                if (!(player.openContainer instanceof ContainerLevelMaintainer)
                        || message.row < 0 || message.row >= TileLevelMaintainer.ROWS) {
                    return;
                }
                final TileLevelMaintainer machine =
                        ((ContainerLevelMaintainer) player.openContainer).getMachine();
                machine.setBatch(message.row, message.batch);
                machine.setTarget(message.row, message.target);
                machine.setRowEnabled(message.row, message.enabled);
            });
            return null;
        }
    }
}
