/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.client.gui;

import java.awt.Rectangle;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiButton;

import lazyae2.container.ContainerWirelessLevelMaintainerTerminal;
import appeng.client.gui.widgets.GuiSlotIndicator;
import appeng.client.gui.widgets.GuiTerminalModeSwitch;
import appeng.client.gui.widgets.GuiWirelessUpgradePlate;
import appeng.container.interfaces.IWirelessTerminalContainer;

/**
 * The terminal opened from a wireless terminal: the same list, with the cards and the mode buttons the
 * terminal itself carries beside it.
 */
public final class GuiWirelessLevelMaintainerTerminal extends GuiLevelMaintainerTerminal {

    /**
     * Three pixels clear of the window, and below the step in its right edge. The lower part of this window
     * is narrower than the rest, and the corner between the two takes two rows to come in.
     */
    private static final int PLATE_X = 193;

    /**
     * Where the search boxes are remembered between openings. The panel on a cable keeps them on itself; a
     * terminal in a pocket has nowhere of its own, and one player has one of these screens open at a time.
     */
    private static String rememberedRows = "";
    private static String rememberedNames = "";

    private final GuiTerminalModeSwitch modeSwitch = new GuiTerminalModeSwitch(this);

    public GuiWirelessLevelMaintainerTerminal(final ContainerWirelessLevelMaintainerTerminal container) {
        super(container, null);
    }

    private int plateY() {
        return this.ySize - 74;
    }

    @Override
    protected String loadSearchRows() {
        return rememberedRows;
    }

    @Override
    protected String loadSearchNames() {
        return rememberedNames;
    }

    @Override
    protected void saveSearchText(final String rows, final String names) {
        rememberedRows = rows;
        rememberedNames = names;
    }

    @Override
    protected void addExtraButtons() {
        if (this.inventorySlots instanceof IWirelessTerminalContainer) {
            this.modeSwitch.attach(this.buttonList,
                    ((IWirelessTerminalContainer) this.inventorySlots).getTerminal(), this.guiLeft, this.guiTop);
        }
    }

    @Override
    protected void actionPerformed(final GuiButton btn) throws IOException {
        if (this.modeSwitch.actionPerformed(btn)) {
            return;
        }

        super.actionPerformed(btn);
    }

    @Override
    public void drawBG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        GuiWirelessUpgradePlate.draw(this, offsetX + PLATE_X, offsetY + this.plateY(),
                IWirelessTerminalContainer.UPGRADE_SLOTS);
        super.drawBG(offsetX, offsetY, mouseX, mouseY);
    }

    @Override
    public void drawFG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        super.drawFG(offsetX, offsetY, mouseX, mouseY);
        GuiSlotIndicator.draw(this);
    }

    @Override
    public List<Rectangle> getJEIExclusionArea() {
        final List<Rectangle> area = new ArrayList<>(super.getJEIExclusionArea());
        GuiWirelessUpgradePlate.addExclusionArea(area, this.guiLeft + PLATE_X, this.guiTop + this.plateY(),
                IWirelessTerminalContainer.UPGRADE_SLOTS);
        this.modeSwitch.addExclusionAreas(area);
        return area;
    }
}
