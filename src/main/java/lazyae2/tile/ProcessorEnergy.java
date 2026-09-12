/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.tile;

import net.minecraftforge.energy.IEnergyStorage;

/**
 * A machine's own store of Forge Energy. It takes power from any face and never gives any back: what goes in
 * is spent on the recipe.
 */
public final class ProcessorEnergy implements IEnergyStorage {

    private final int capacity;
    private final Runnable onChange;

    private int stored;

    ProcessorEnergy(final int capacity, final Runnable onChange) {
        this.capacity = capacity;
        this.onChange = onChange;
    }

    @Override
    public int receiveEnergy(final int maxReceive, final boolean simulate) {
        final int taken = Math.min(this.capacity - this.stored, Math.max(0, maxReceive));
        if (!simulate && taken > 0) {
            this.stored += taken;
            this.onChange.run();
        }
        return taken;
    }

    @Override
    public int extractEnergy(final int maxExtract, final boolean simulate) {
        return 0;
    }

    /**
     * What the machine itself spends, which is the only way power leaves.
     */
    void consume(final int amount) {
        this.stored = Math.max(0, this.stored - amount);
    }

    void setEnergyStored(final int amount) {
        this.stored = Math.max(0, Math.min(this.capacity, amount));
    }

    @Override
    public int getEnergyStored() {
        return this.stored;
    }

    @Override
    public int getMaxEnergyStored() {
        return this.capacity;
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    @Override
    public boolean canReceive() {
        return true;
    }
}
