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
import appeng.container.implementations.ContainerSetAmount;
import appeng.core.sync.GuiBridge;
import appeng.core.sync.packets.PacketSwitchGuis;
import appeng.helpers.IAmountTarget;

/**
 * A middle click on a row in the terminal: type how much that row keeps.
 * <p>
 * AE2's own middle click can only reach a slot of the window it was clicked in, and a row here belongs to a
 * machine somewhere else on the network - so the amount screen is opened from this side instead, on a target
 * the container builds from the machine and the row.
 */
public final class PacketTerminalAmount implements IMessage {

    private long machine;
    private int row;

    public PacketTerminalAmount() {
    }

    public PacketTerminalAmount(final long machine, final int row) {
        this.machine = machine;
        this.row = row;
    }

    @Override
    public void fromBytes(final ByteBuf buf) {
        this.machine = buf.readLong();
        this.row = buf.readByte();
    }

    @Override
    public void toBytes(final ByteBuf buf) {
        buf.writeLong(this.machine);
        buf.writeByte(this.row);
    }

    public static final class Handler implements IMessageHandler<PacketTerminalAmount, IMessage> {

        @Override
        public IMessage onMessage(final PacketTerminalAmount message, final MessageContext context) {
            final EntityPlayerMP player = context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                if (!(player.openContainer instanceof ContainerLevelMaintainerTerminal)) {
                    return;
                }

                final ContainerLevelMaintainerTerminal terminal =
                        (ContainerLevelMaintainerTerminal) player.openContainer;
                final IAmountTarget target = terminal.amountTargetFor(message.machine, message.row);
                if (target == null || !PacketSwitchGuis.reopen(player, terminal, GuiBridge.GUI_SET_AMOUNT)) {
                    return;
                }

                if (player.openContainer instanceof ContainerSetAmount) {
                    final ContainerSetAmount amount = (ContainerSetAmount) player.openContainer;
                    amount.setAmountTarget(target);
                    amount.detectAndSendChanges();
                }
            });
            return null;
        }
    }
}
