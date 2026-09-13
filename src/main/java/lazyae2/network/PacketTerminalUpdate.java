/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.network;

import io.netty.buffer.ByteBuf;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import lazyae2.client.gui.GuiLevelMaintainerTerminal;

/**
 * What the Level Maintainer Terminal has to redraw: the machines that are new to it, and the rows that have
 * moved since it was last told. A tag holding nothing but rows leaves the rest of the window alone.
 */
public final class PacketTerminalUpdate implements IMessage {

    private NBTTagCompound data;

    public PacketTerminalUpdate() {
    }

    public PacketTerminalUpdate(final NBTTagCompound data) {
        this.data = data;
    }

    @Override
    public void fromBytes(final ByteBuf buf) {
        this.data = ByteBufUtils.readTag(buf);
    }

    @Override
    public void toBytes(final ByteBuf buf) {
        ByteBufUtils.writeTag(buf, this.data);
    }

    public static final class Handler implements IMessageHandler<PacketTerminalUpdate, IMessage> {

        @Override
        public IMessage onMessage(final PacketTerminalUpdate message, final MessageContext context) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                final GuiScreen screen = Minecraft.getMinecraft().currentScreen;
                if (screen instanceof GuiLevelMaintainerTerminal && message.data != null) {
                    ((GuiLevelMaintainerTerminal) screen).postUpdate(message.data);
                }
            });
            return null;
        }
    }
}
