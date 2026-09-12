/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.tile;

import net.minecraft.util.EnumFacing;

import appeng.helpers.ICustomNameObject;

/**
 * What every machine of this mod's block has, whether it runs on Forge Energy of its own or sits on an ME
 * network: a face it is turned towards, a lit look while it is doing something, and a name a player may give
 * it. A machine that moves items has a map of faces too; see {@link ISidedMachineTile}.
 */
public interface IMachineTile extends ICustomNameObject {

    EnumFacing getFront();

    void setFront(EnumFacing facing);

    /**
     * Whether the block wears its lit texture.
     */
    boolean isWorking();

    void saveChanges();

    void markForUpdate();
}
