/*
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.network;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.netty.buffer.ByteBuf;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import lazyae2.client.gui.GuiAssembler;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;

/**
 * What a chamber is crafting, for its window: the kinds with the most crafts in each of two lists, the work
 * still being made and the work done and waiting for room, each with how many kinds it holds in all.
 * The amount of each stack is a number of crafts, not of items.
 */
public final class PacketAssemblerWork implements IMessage {

    private List<GenericStack> working;
    private int workingKinds;
    private List<GenericStack> waiting;
    private int waitingKinds;

    public PacketAssemblerWork() {
    }

    public PacketAssemblerWork(final List<GenericStack> working, final int workingKinds,
            final List<GenericStack> waiting, final int waitingKinds) {
        this.working = working;
        this.workingKinds = workingKinds;
        this.waiting = waiting;
        this.waitingKinds = waitingKinds;
    }

    public List<GenericStack> getWorking() {
        return this.working;
    }

    public int getWorkingKinds() {
        return this.workingKinds;
    }

    public List<GenericStack> getWaiting() {
        return this.waiting;
    }

    public int getWaitingKinds() {
        return this.waitingKinds;
    }

    @Override
    public void fromBytes(final ByteBuf buf) {
        this.working = readList(buf);
        this.workingKinds = buf.readInt();
        this.waiting = readList(buf);
        this.waitingKinds = buf.readInt();
    }

    @Override
    public void toBytes(final ByteBuf buf) {
        writeList(buf, this.working);
        buf.writeInt(this.workingKinds);
        writeList(buf, this.waiting);
        buf.writeInt(this.waitingKinds);
    }

    private static List<GenericStack> readList(final ByteBuf buf) {
        final int size = buf.readInt();
        final List<GenericStack> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            try {
                final AEKey key = AEKey.readKey(buf);
                final long crafts = buf.readLong();
                if (key != null) {
                    list.add(new GenericStack(key, crafts));
                }
            } catch (final IOException e) {
                throw new IllegalStateException("Unreadable chamber work list", e);
            }
        }
        return list;
    }

    private static void writeList(final ByteBuf buf, final List<GenericStack> list) {
        buf.writeInt(list.size());
        for (final GenericStack stack : list) {
            try {
                AEKey.writeKey(buf, stack.what());
            } catch (final IOException e) {
                throw new IllegalStateException("Unwritable chamber work list", e);
            }
            buf.writeLong(stack.amount());
        }
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof PacketAssemblerWork)) {
            return false;
        }
        final PacketAssemblerWork that = (PacketAssemblerWork) other;
        return this.workingKinds == that.workingKinds && this.waitingKinds == that.waitingKinds
                && this.working.equals(that.working) && this.waiting.equals(that.waiting);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.working, this.workingKinds, this.waiting, this.waitingKinds);
    }

    public static final class Handler implements IMessageHandler<PacketAssemblerWork, IMessage> {

        @Override
        public IMessage onMessage(final PacketAssemblerWork message, final MessageContext context) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                final GuiScreen screen = Minecraft.getMinecraft().currentScreen;
                if (screen instanceof GuiAssembler) {
                    ((GuiAssembler) screen).postWork(message);
                }
            });
            return null;
        }
    }
}
