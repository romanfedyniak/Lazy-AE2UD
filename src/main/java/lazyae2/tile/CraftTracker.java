/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.tile;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.google.common.collect.ImmutableSet;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import appeng.api.AEApi;
import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.crafting.ICraftingJob;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;

/**
 * One crafting job per row, from the moment it is planned to the moment the network says it is done.
 * <p>
 * A job is not ordered in one go: asking for it returns a plan that is worked out on another thread, so a row
 * holds that plan until it is ready and only then submits it. Until the link that comes back is gone, the row
 * asks for nothing more.
 */
final class CraftTracker {

    private final ICraftingRequester owner;
    private final int rows;

    private final Future<ICraftingJob>[] plans;
    private final ICraftingLink[] links;

    @SuppressWarnings("unchecked")
    CraftTracker(final ICraftingRequester owner, final int rows) {
        this.owner = owner;
        this.rows = rows;
        this.plans = new Future[rows];
        this.links = new ICraftingLink[rows];
    }

    /**
     * Carries a row one step: with nothing in hand it plans a job, and with a plan that is ready it submits
     * it. Called every time the row is looked at, however much is missing, because a plan already being
     * worked out still has to be picked up.
     *
     * @param amount what the row is short of, which is only read when a new plan is started
     * @return whether anything moved, so the machine knows to keep looking rather than go back to sleep
     */
    boolean request(final int row, final AEKey what, final long amount, final World world, final IGrid grid,
            final ICraftingGrid crafting, final IActionSource source) {
        if (this.links[row] != null) {
            return false;
        }

        final Future<ICraftingJob> plan = this.plans[row];
        if (plan == null) {
            if (amount <= 0) {
                return false;
            }
            this.plans[row] = crafting.beginCraftingJob(world, grid, source, new GenericStack(what, amount), null);
            return true;
        }

        if (!plan.isDone()) {
            return false;
        }

        this.plans[row] = null;
        try {
            final ICraftingJob job = plan.get();
            // A simulation is the network saying it cannot actually make this, and submitting one would
            // start a job that waits for something nobody is making.
            if (job == null || job.isSimulation()) {
                return false;
            }
            final ICraftingLink link = crafting.submitJob(job, this.owner, null, false, source).link();
            if (link == null) {
                return false;
            }
            this.links[row] = link;
            return true;
        } catch (final InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return false;
        } catch (final ExecutionException failed) {
            return false;
        }
    }

    /**
     * @return the row that ordered that job, or -1
     */
    int rowOf(final ICraftingLink link) {
        for (int row = 0; row < this.rows; row++) {
            if (this.links[row] == link) {
                return row;
            }
        }
        return -1;
    }

    /**
     * A job that ended, one way or the other. Its row is free again.
     */
    void forget(final ICraftingLink link) {
        final int row = this.rowOf(link);
        if (row >= 0) {
            this.links[row] = null;
        }
    }

    /**
     * A row that no longer asks for anything: a plan still being worked out is thrown away, and a job already
     * running is cancelled.
     */
    void clear(final int row) {
        if (this.plans[row] != null) {
            this.plans[row].cancel(true);
            this.plans[row] = null;
        }
        if (this.links[row] != null) {
            this.links[row].cancel();
            this.links[row] = null;
        }
    }

    ImmutableSet<ICraftingLink> getRequestedJobs() {
        final ImmutableSet.Builder<ICraftingLink> jobs = ImmutableSet.builder();
        for (final ICraftingLink link : this.links) {
            if (link != null) {
                jobs.add(link);
            }
        }
        return jobs.build();
    }

    void writeToNBT(final NBTTagCompound data) {
        for (int row = 0; row < this.rows; row++) {
            final ICraftingLink link = this.links[row];
            if (link != null) {
                final NBTTagCompound saved = new NBTTagCompound();
                link.writeToNBT(saved);
                data.setTag("link" + row, saved);
            }
        }
    }

    void readFromNBT(final NBTTagCompound data) {
        for (int row = 0; row < this.rows; row++) {
            final NBTTagCompound saved = data.getCompoundTag("link" + row);
            if (!saved.isEmpty()) {
                this.links[row] = AEApi.instance().storage().loadCraftingLink(saved, this.owner);
            }
        }
    }
}
