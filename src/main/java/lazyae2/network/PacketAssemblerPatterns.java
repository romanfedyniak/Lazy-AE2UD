/*
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.network;

import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.ByteBuf;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import lazyae2.client.gui.GuiAssembler;

/**
 * What some of a chamber's pattern modules hold, for its window: a module each, with only the slots that
 * changed. {@code clear} says the modules themselves changed and the window should forget what it had.
 */
public final class PacketAssemblerPatterns implements IMessage {

    private boolean clear;
    private List<NBTTagCompound> modules;

    public PacketAssemblerPatterns() {
    }

    public PacketAssemblerPatterns(final boolean clear, final List<NBTTagCompound> modules) {
        this.clear = clear;
        this.modules = modules;
    }

    @Override
    public void fromBytes(final ByteBuf buf) {
        this.clear = buf.readBoolean();
        final int count = buf.readInt();
        this.modules = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            this.modules.add(ByteBufUtils.readTag(buf));
        }
    }

    @Override
    public void toBytes(final ByteBuf buf) {
        buf.writeBoolean(this.clear);
        buf.writeInt(this.modules.size());
        for (final NBTTagCompound module : this.modules) {
            ByteBufUtils.writeTag(buf, module);
        }
    }

    public static final class Handler implements IMessageHandler<PacketAssemblerPatterns, IMessage> {

        @Override
        public IMessage onMessage(final PacketAssemblerPatterns message, final MessageContext context) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                final GuiScreen screen = Minecraft.getMinecraft().currentScreen;
                if (screen instanceof GuiAssembler) {
                    ((GuiAssembler) screen).postUpdate(message.clear, message.modules);
                }
            });
            return null;
        }
    }
}
