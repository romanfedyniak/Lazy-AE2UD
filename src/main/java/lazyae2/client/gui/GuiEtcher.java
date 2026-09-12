/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.client.gui;

import net.minecraft.client.resources.I18n;

import lazyae2.container.ContainerEtcher;

public final class GuiEtcher extends GuiProcessor {

    /** Where the arrow between the slots sits; HEI opens the machine's recipes from the same place. */
    public static final int ARROW_LEFT = 84;
    public static final int ARROW_TOP = 36;
    public static final int ARROW_WIDTH = 22;
    public static final int ARROW_HEIGHT = 14;

    /** The two presses, which close over the material before anything is etched. */
    private static final int PRESS_LEFT = 55;
    private static final int PRESS_TOP = 22;
    private static final int PRESS_BOTTOM = 54;
    private static final int PRESS_WIDTH = 22;
    private static final int PRESS_HEIGHT = 11;
    private static final int PRESS_TOP_U = 24;
    private static final int PRESS_BOTTOM_U = 48;

    public GuiEtcher(final ContainerEtcher container) {
        super(container, "etcher", ARROW_LEFT, ARROW_TOP, ARROW_WIDTH, ARROW_HEIGHT);
    }

    @Override
    protected String getScreenTitle() {
        return I18n.format("tile.threng.machine.etcher.name");
    }

    /**
     * The presses close over the first half of the work, and the arrow fills over the second, as the old mod
     * drew it.
     */
    @Override
    protected void drawProgress(final int offsetX, final int offsetY) {
        final float fraction = this.getContainer().getWorkFraction();
        if (fraction < 0.5F) {
            this.drawPresses(offsetX, offsetY, fraction * 2F);
        } else {
            this.drawPresses(offsetX, offsetY, 1F);
            this.drawProgressArrow(offsetX, offsetY, (fraction - 0.5F) * 2F);
        }
    }

    /**
     * The top press comes down and the bottom one rises, each showing as much of itself as it has travelled.
     */
    private void drawPresses(final int offsetX, final int offsetY, final float closed) {
        final int shown = Math.round(closed * PRESS_HEIGHT);
        if (shown <= 0) {
            return;
        }
        final int hidden = PRESS_HEIGHT - shown;
        this.drawTexturedModalRect(offsetX + PRESS_LEFT, offsetY + PRESS_TOP, PRESS_TOP_U, SPRITE_V,
                PRESS_WIDTH, shown);
        this.drawTexturedModalRect(offsetX + PRESS_LEFT, offsetY + PRESS_BOTTOM + hidden, PRESS_BOTTOM_U,
                SPRITE_V + hidden, PRESS_WIDTH, shown);
    }
}
