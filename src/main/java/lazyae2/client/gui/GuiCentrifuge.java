/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.client.gui;

import net.minecraft.client.resources.I18n;

import lazyae2.container.ContainerCentrifuge;

public final class GuiCentrifuge extends GuiProcessor {

    /** Where the arrow between the input and the output sits. */
    private static final int ARROW_LEFT = 80;
    private static final int ARROW_TOP = 36;
    private static final int ARROW_WIDTH = 22;
    private static final int ARROW_HEIGHT = 14;

    public GuiCentrifuge(final ContainerCentrifuge container) {
        super(container, "centrifuge");
    }

    @Override
    protected String getScreenTitle() {
        return I18n.format("tile.threng.machine.centrifuge.name");
    }

    @Override
    protected void drawMachine(final int offsetX, final int offsetY) {
        this.drawProgressArrow(offsetX, offsetY, ARROW_LEFT, ARROW_TOP, ARROW_WIDTH, ARROW_HEIGHT);
    }
}
