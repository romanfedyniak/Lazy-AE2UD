/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.client.gui;

import net.minecraft.client.resources.I18n;

import lazyae2.container.ContainerAggregator;

public final class GuiAggregator extends GuiProcessor {

    /** Where the arrow between the inputs and the output sits, and where it is cut from the picture. */
    private static final int ARROW_LEFT = 92;
    private static final int ARROW_TOP = 36;
    private static final int ARROW_U = 0;
    private static final int ARROW_V = 200;
    private static final int ARROW_WIDTH = 24;
    private static final int ARROW_HEIGHT = 14;

    public GuiAggregator(final ContainerAggregator container) {
        super(container, "aggregator");
    }

    @Override
    protected String getScreenTitle() {
        return I18n.format("tile.threng.machine.aggregator.name");
    }

    @Override
    protected void drawMachine(final int offsetX, final int offsetY) {
        final int filled = Math.round(this.getContainer().getWorkFraction() * ARROW_WIDTH);
        if (filled > 0) {
            this.drawTexturedModalRect(offsetX + ARROW_LEFT, offsetY + ARROW_TOP, ARROW_U, ARROW_V, filled, ARROW_HEIGHT);
        }
    }
}
