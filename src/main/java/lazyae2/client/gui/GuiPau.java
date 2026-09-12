/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.client.gui;

import java.awt.Rectangle;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Slot;

import lazyae2.container.ContainerPau;
import lazyae2.network.ModNetwork;
import lazyae2.network.PacketSideConfig;
import lazyae2.tile.TilePau;
import lazyae2.util.IoMode;
import lazyae2.util.RelativeSide;
import appeng.client.gui.AEBaseGui;
import appeng.container.slot.AppEngSlot;

/**
 * The unit's window, drawn rather than worn: the old mod's picture had room for nine patterns and one row of
 * buffers, and this one holds four rows of patterns beside a column of cards.
 */
public final class GuiPau extends AEBaseGui {

    private static final int LABEL_COLOR = 4210752;

    private static final int SIDES_LEFT = 151;
    private static final int SIDES_TOP = 6;
    /** The column of upgrade slots, which stands outside the window as on every AE2 screen. */
    private static final int COLUMN_LEFT = 177;
    private static final int COLUMN_WIDTH = 32;
    private static final int LABEL_TOP = 20;
    /**
     * What a slot no card has paid for is filled with. The frame stays as dark as any other slot's and only
     * the inside goes pale, which is how the ME Interface's picture draws the rows past its first.
     */
    private static final int DISABLED_FILL = 0xFFAFAFAF;

    private final ContainerPau container;

    public GuiPau(final ContainerPau container) {
        super(container);
        this.container = container;
        this.xSize = 176;
        this.ySize = ContainerPau.HEIGHT;
    }

    @Override
    public void drawBG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        this.drawPanel(offsetX, offsetY, this.xSize, this.ySize);
        this.drawPanel(offsetX + COLUMN_LEFT, offsetY, COLUMN_WIDTH,
                14 + TilePau.UPGRADE_SLOTS * 18);

        // Every pattern slot gets its well, paid for or not, so the four rows read as one block the way
        // the ME Interface's do; the ones no card has bought yet are greyed over instead of left blank.
        for (final Slot slot : this.inventorySlots.inventorySlots) {
            drawSlotWell(offsetX + slot.xPos, offsetY + slot.yPos);
            if (slot instanceof AppEngSlot && !((AppEngSlot) slot).isSlotEnabled()) {
                drawRect(offsetX + slot.xPos, offsetY + slot.yPos, offsetX + slot.xPos + 16,
                        offsetY + slot.yPos + 16, DISABLED_FILL);
            }
        }

        // drawRect leaves its colour set, and the face map is a texture
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GuiSideConfig.draw(offsetX + SIDES_LEFT, offsetY + SIDES_TOP, this.container);
    }

    @Override
    public void drawFG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        this.fontRenderer.drawString(this.container.getMachine().hasCustomInventoryName()
                ? this.container.getMachine().getCustomInventoryName()
                : I18n.format("tile.threng.machine.pau.name"), 8, 6, LABEL_COLOR);
        this.fontRenderer.drawString(I18n.format("gui.threng.pau.toMachine"), ContainerPau.EXPORT_LEFT,
                LABEL_TOP, LABEL_COLOR);
        this.fontRenderer.drawString(I18n.format("gui.threng.pau.fromMachine"), ContainerPau.IMPORT_LEFT,
                LABEL_TOP, LABEL_COLOR);
        this.fontRenderer.drawString(I18n.format("container.inventory"), 8, this.ySize - 96 + 3, LABEL_COLOR);
    }

    @Override
    public void drawScreen(final int mouseX, final int mouseY, final float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);

        final RelativeSide hovered = GuiSideConfig.cellAt(this.guiLeft + SIDES_LEFT, this.guiTop + SIDES_TOP,
                mouseX, mouseY);
        if (hovered != null) {
            this.drawHoveringText(GuiSideConfig.tooltip(hovered, this.container.getFace(hovered)), mouseX, mouseY);
        }
    }

    @Override
    protected void mouseClicked(final int mouseX, final int mouseY, final int button) throws IOException {
        final RelativeSide clicked = GuiSideConfig.cellAt(this.guiLeft + SIDES_LEFT, this.guiTop + SIDES_TOP,
                mouseX, mouseY);
        if (clicked != null && button <= 1) {
            final IoMode mode = this.container.getFace(clicked);
            ModNetwork.CHANNEL.sendToServer(new PacketSideConfig(clicked, button == 0 ? mode.next() : mode.previous()));
            return;
        }
        super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * The column of cards stands outside the window, so HEI's item list keeps off it.
     */
    @Override
    public List<Rectangle> getJEIExclusionArea() {
        return Collections.singletonList(new Rectangle(this.guiLeft + COLUMN_LEFT, this.guiTop, COLUMN_WIDTH,
                14 + TilePau.UPGRADE_SLOTS * 18));
    }
}
