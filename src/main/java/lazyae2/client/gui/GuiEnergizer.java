/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.client.gui;

import net.minecraft.client.resources.I18n;

import lazyae2.container.ContainerEnergizer;

public final class GuiEnergizer extends GuiProcessor {

    /** Where the bolts between the slots sit; HEI opens the machine's recipes from the same place. */
    public static final int ARROW_LEFT = 81;
    public static final int ARROW_TOP = 29;
    public static final int ARROW_WIDTH = 22;
    public static final int ARROW_HEIGHT = 29;

    public GuiEnergizer(final ContainerEnergizer container) {
        super(container, "energizer", ARROW_LEFT, ARROW_TOP, ARROW_WIDTH, ARROW_HEIGHT);
    }

    @Override
    protected String getScreenTitle() {
        return I18n.format("tile.threng.machine.energizer.name");
    }

}
