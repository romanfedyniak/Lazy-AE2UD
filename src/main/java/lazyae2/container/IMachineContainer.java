/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.container;

import lazyae2.tile.IMachineTile;
import lazyae2.util.IoMode;
import lazyae2.util.RelativeSide;

/**
 * A window showing one of this mod's machines, which is all the face map needs to know to be clicked in any
 * of them.
 */
public interface IMachineContainer {

    IMachineTile getMachine();

    default IoMode getFace(final RelativeSide side) {
        return this.getMachine().getSides().get(side);
    }
}
