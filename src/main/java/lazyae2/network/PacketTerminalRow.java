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

import lazyae2.container.ContainerLevelMaintainerTerminal;

/**
 * One row of a maintainer as the terminal left it: how much to order at a time, and whether the row is doing
 * anything at all. How much the row keeps is typed on the number screen instead, or turned with the wheel.
 * <p>
 * The machine is named by the id the terminal handed out, so a player can only reach the maintainers their
 * own open window is listing.
 */
public final class PacketTerminalRow implements IMessage {

    private long machine;
    private int row;
    private long batch;
    private boolean enabled;

    public PacketTerminalRow() {
    }

    public PacketTerminalRow(final long machine, final int row, final long batch, final boolean enabled) {
        this.machine = machine;
        this.row = row;
        this.batch = batch;
        this.enabled = enabled;
    }

    @Override
    public void fromBytes(final ByteBuf buf) {
        this.machine = buf.readLong();
        this.row = buf.readByte();
        this.batch = buf.readLong();
        this.enabled = buf.readBoolean();
    }

    @Override
    public void toBytes(final ByteBuf buf) {
        buf.writeLong(this.machine);
        buf.writeByte(this.row);
        buf.writeLong(this.batch);
        buf.writeBoolean(this.enabled);
    }

    public static final class Handler implements IMessageHandler<PacketTerminalRow, IMessage> {

        @Override
        public IMessage onMessage(final PacketTerminalRow message, final MessageContext context) {
            final EntityPlayerMP player = context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                if (!(player.openContainer instanceof ContainerLevelMaintainerTerminal)) {
                    return;
                }
                final ContainerLevelMaintainerTerminal terminal =
                        (ContainerLevelMaintainerTerminal) player.openContainer;
                terminal.setBatch(message.machine, message.row, message.batch);
                terminal.setRowEnabled(message.machine, message.row, message.enabled);
            });
            return null;
        }
    }
}
