/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.tile;

import lazyae2.util.SideConfig;

/**
 * A machine that moves items through its faces, and so has a map saying which of them let what through. The
 * ME Level Maintainer has none: it only ever talks to the network.
 */
public interface ISidedMachineTile extends IMachineTile {

    SideConfig getSides();
}
