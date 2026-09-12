/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.tile;

import net.minecraft.util.EnumFacing;

import lazyae2.util.SideConfig;
import appeng.helpers.ICustomNameObject;

/**
 * What every machine of this mod's block has, whether it runs on Forge Energy of its own or sits on an ME
 * network: a face it is turned towards, a lit look while it is doing something, and a map of which faces let
 * items through.
 */
public interface IMachineTile extends ICustomNameObject {

    EnumFacing getFront();

    void setFront(EnumFacing facing);

    /**
     * Whether the block wears its lit texture.
     */
    boolean isWorking();

    SideConfig getSides();

    void saveChanges();

    void markForUpdate();
}
