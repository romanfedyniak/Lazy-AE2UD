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

import lazyae2.container.IMachineContainer;
import lazyae2.tile.ISidedMachineTile;
import lazyae2.util.IoMode;
import lazyae2.util.RelativeSide;

/**
 * A face of a machine clicked in its window: left turns the setting forward, right turns it back.
 */
public final class PacketSideConfig implements IMessage {

    private int side;
    private int mode;

    public PacketSideConfig() {
    }

    public PacketSideConfig(final RelativeSide side, final IoMode mode) {
        this.side = side.ordinal();
        this.mode = mode.ordinal();
    }

    @Override
    public void fromBytes(final ByteBuf buf) {
        this.side = buf.readByte();
        this.mode = buf.readByte();
    }

    @Override
    public void toBytes(final ByteBuf buf) {
        buf.writeByte(this.side);
        buf.writeByte(this.mode);
    }

    public static final class Handler implements IMessageHandler<PacketSideConfig, IMessage> {

        @Override
        public IMessage onMessage(final PacketSideConfig message, final MessageContext context) {
            final EntityPlayerMP player = context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                if (!(player.openContainer instanceof IMachineContainer)) {
                    return;
                }
                final IMachineContainer container = (IMachineContainer) player.openContainer;
                final RelativeSide[] sides = RelativeSide.all();
                if (message.side < 0 || message.side >= sides.length
                        || !(container.getMachine() instanceof ISidedMachineTile)) {
                    return;
                }
                final ISidedMachineTile machine = (ISidedMachineTile) container.getMachine();
                if (machine.getSides().set(sides[message.side], IoMode.of(message.mode))) {
                    machine.saveChanges();
                    machine.markForUpdate();
                }
            });
            return null;
        }
    }
}
