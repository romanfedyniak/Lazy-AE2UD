/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.network;

import io.netty.buffer.ByteBuf;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import lazyae2.container.ContainerLevelMaintainerTerminal;

/**
 * What HEI dropped on a row of the terminal. A click is answered by AE2's own inventory action, which knows
 * nothing of a drag that never touched the hand - and a fluid dragged out of HEI is no item, so what travels
 * here is the key wrapped in a placeholder.
 */
public final class PacketTerminalFilter implements IMessage {

    private long machine;
    private int row;
    private ItemStack filter = ItemStack.EMPTY;

    public PacketTerminalFilter() {
    }

    public PacketTerminalFilter(final long machine, final int row, final ItemStack filter) {
        this.machine = machine;
        this.row = row;
        this.filter = filter;
    }

    @Override
    public void fromBytes(final ByteBuf buf) {
        this.machine = buf.readLong();
        this.row = buf.readByte();
        this.filter = ByteBufUtils.readItemStack(buf);
    }

    @Override
    public void toBytes(final ByteBuf buf) {
        buf.writeLong(this.machine);
        buf.writeByte(this.row);
        ByteBufUtils.writeItemStack(buf, this.filter);
    }

    public static final class Handler implements IMessageHandler<PacketTerminalFilter, IMessage> {

        @Override
        public IMessage onMessage(final PacketTerminalFilter message, final MessageContext context) {
            final EntityPlayerMP player = context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                if (player.openContainer instanceof ContainerLevelMaintainerTerminal) {
                    final ItemStack one = message.filter.copy();
                    one.setCount(1);
                    ((ContainerLevelMaintainerTerminal) player.openContainer)
                            .setFilter(message.machine, message.row, one);
                }
            });
            return null;
        }
    }
}
