/*
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.tile;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;

/**
 * What a chamber is crafting: one batch per push, however many copies it holds, each with what it will hand
 * back and how far along it is.
 * <p>
 * A batch finishes as a whole, so a thousand copies cost one entry and one delivery rather than a thousand.
 * Power goes to the oldest batch first, so a network short of it still finishes something.
 */
public final class AssemblerWork {

    /** How long a finished batch the network would not take waits before offering it again, in ticks. */
    static final long RETRY_TICKS = 20;

    /** Where finished work goes; answers how much of it was taken. */
    public interface Sink {

        long insert(AEKey what, long amount);
    }

    private static final class Batch {

        /** How many crafts it stands for, which is how many of the chamber's slots it holds. */
        final int copies;
        /** Everything it hands back, results and emptied containers, already multiplied by the copies. */
        final List<GenericStack> outputs;
        /** From 0 to 1. */
        double progress;
        /** The tick before which a refused delivery is not tried again. */
        long retryAt;

        Batch(final int copies, final List<GenericStack> outputs, final double progress) {
            this.copies = copies;
            this.outputs = outputs;
            this.progress = progress;
        }

        boolean isFinished() {
            return this.progress >= 1;
        }
    }

    private final List<Batch> batches = new ArrayList<>();
    private int busy;

    public boolean isEmpty() {
        return this.batches.isEmpty();
    }

    /** How many of the chamber's slots the work holds, finished batches still waiting to be delivered included. */
    public int getBusy() {
        return this.busy;
    }

    public void add(final int copies, final List<GenericStack> outputs) {
        this.batches.add(new Batch(copies, new ArrayList<>(outputs), 0));
        this.busy += copies;
    }

    /** Work that is already done: it holds no slot and only waits to be delivered. */
    public void addFinished(final List<GenericStack> outputs) {
        if (!outputs.isEmpty()) {
            this.batches.add(new Batch(0, new ArrayList<>(outputs), 1));
        }
    }

    /** The power this tick's work asks for. */
    public double powerWanted(final int ticksPerJob, final double energyPerJob) {
        final double step = 1D / ticksPerJob;
        double wanted = 0;
        for (final Batch batch : this.batches) {
            if (!batch.isFinished()) {
                wanted += batch.copies * energyPerJob * Math.min(step, 1 - batch.progress);
            }
        }
        return wanted;
    }

    /**
     * Moves the work on by one tick with the power that could be had: the oldest batch is paid first, and one
     * paid only in part moves on by that part.
     *
     * @return whether any batch moved.
     */
    public boolean spend(final double power, final int ticksPerJob, final double energyPerJob) {
        final double step = 1D / ticksPerJob;
        double left = power;
        boolean moved = false;

        for (final Batch batch : this.batches) {
            if (batch.isFinished()) {
                continue;
            }
            final double advance = Math.min(step, 1 - batch.progress);
            final double cost = batch.copies * energyPerJob * advance;
            if (cost <= 0) {
                batch.progress += advance;
            } else if (left >= cost) {
                batch.progress += advance;
                left -= cost;
            } else if (left > 0) {
                batch.progress += advance * left / cost;
                left = 0;
            } else {
                break;
            }
            // A share of a share never quite adds up to one
            if (batch.progress > 1 - 1e-9) {
                batch.progress = 1;
            }
            moved = true;
        }
        return moved;
    }

    /**
     * Hands every finished batch to the sink. What the sink takes is gone; a batch it takes only in part keeps
     * the rest, and its slots, and is offered again a second later.
     *
     * @return whether anything was delivered.
     */
    public boolean deliver(final Sink sink, final long now) {
        boolean delivered = false;
        final Iterator<Batch> it = this.batches.iterator();

        while (it.hasNext()) {
            final Batch batch = it.next();
            if (!batch.isFinished() || now < batch.retryAt) {
                continue;
            }

            final Iterator<GenericStack> outputs = batch.outputs.listIterator();
            final List<GenericStack> kept = new ArrayList<>();
            while (outputs.hasNext()) {
                final GenericStack output = outputs.next();
                final long taken = Math.max(0, Math.min(output.amount(), sink.insert(output.what(), output.amount())));
                if (taken > 0) {
                    delivered = true;
                }
                if (taken < output.amount()) {
                    kept.add(new GenericStack(output.what(), output.amount() - taken));
                }
            }

            batch.outputs.clear();
            batch.outputs.addAll(kept);
            if (kept.isEmpty()) {
                it.remove();
                this.busy -= batch.copies;
            } else {
                batch.retryAt = now + RETRY_TICKS;
            }
        }
        return delivered;
    }

    /** Everything every batch will hand back, finished or not, and the work with it: for a controller broken. */
    public List<GenericStack> takeAll() {
        final List<GenericStack> all = new ArrayList<>();
        for (final Batch batch : this.batches) {
            all.addAll(batch.outputs);
        }
        this.batches.clear();
        this.busy = 0;
        return all;
    }

    public NBTTagList writeToNBT() {
        final NBTTagList list = new NBTTagList();
        for (final Batch batch : this.batches) {
            final NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("copies", batch.copies);
            tag.setDouble("progress", batch.progress);
            final NBTTagList outputs = new NBTTagList();
            for (final GenericStack output : batch.outputs) {
                final NBTTagCompound stack = new NBTTagCompound();
                GenericStack.writeTag(stack, output);
                outputs.appendTag(stack);
            }
            tag.setTag("outputs", outputs);
            list.appendTag(tag);
        }
        return list;
    }

    public void readFromNBT(final NBTTagList list) {
        this.batches.clear();
        this.busy = 0;
        for (int i = 0; i < list.tagCount(); i++) {
            final NBTTagCompound tag = list.getCompoundTagAt(i);
            final List<GenericStack> outputs = new ArrayList<>();
            final NBTTagList saved = tag.getTagList("outputs", Constants.NBT.TAG_COMPOUND);
            for (int j = 0; j < saved.tagCount(); j++) {
                final GenericStack output = GenericStack.readTag(saved.getCompoundTagAt(j));
                if (output != null) {
                    outputs.add(output);
                }
            }
            final Batch batch = new Batch(tag.getInteger("copies"), outputs, tag.getDouble("progress"));
            this.batches.add(batch);
            this.busy += batch.copies;
        }
    }
}
