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
import java.util.Arrays;
import java.util.List;

import org.lwjgl.input.Mouse;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;

import lazyae2.Tags;
import lazyae2.container.ContainerProcessor;
import lazyae2.network.ModNetwork;
import lazyae2.network.PacketSideConfig;
import lazyae2.util.IoMode;
import lazyae2.util.RelativeSide;
import appeng.api.config.Settings;
import appeng.api.config.YesNo;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.widgets.GuiImgButton;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketConfigButton;

/**
 * The window all four machines share: the work bar, the power the machine holds, its faces, the auto-export
 * switch and a column of upgrade slots down the right.
 */
public abstract class GuiProcessor extends AEBaseGui {

    private static final int LABEL_COLOR = 4210752;
    /** Where the column of setting buttons runs, as on every AE2 screen of this kind. */
    private static final int SIDE_BUTTON_LEFT = -18;

    private static final int SIDES_LEFT = 146;
    private static final int SIDES_TOP = 7;
    private static final int ENERGY_LEFT = 165;
    private static final int ENERGY_TOP = 7;
    private static final int ENERGY_WIDTH = 4;
    private static final int ENERGY_HEIGHT = 72;

    private static final String ENERGY_TEXTURE = "gui/component/energy.png";
    /** Where the filled arrow was moved to in every machine's picture, out of the upgrade column's way. */
    private static final int ARROW_U = 0;
    private static final int ARROW_V = 200;
    /** The whole picture the bar is cut from; it is nothing like the 256 square the plain helper assumes. */
    private static final int ENERGY_TEXTURE_WIDTH = 6;
    private static final int ENERGY_TEXTURE_HEIGHT = 72;

    private final ContainerProcessor container;
    private final String background;
    private GuiImgButton autoExport;

    protected GuiProcessor(final ContainerProcessor container, final String background) {
        super(container);
        this.container = container;
        this.background = "gui/" + background + ".png";
        this.xSize = 176;
        this.ySize = ContainerProcessor.HEIGHT;
    }

    protected ContainerProcessor getContainer() {
        return this.container;
    }

    /**
     * The machine's own name, drawn at the top left.
     */
    protected abstract String getScreenTitle();

    /**
     * Whatever the machine draws over its background - a progress bar, usually.
     */
    protected void drawMachine(final int offsetX, final int offsetY) {
    }

    /**
     * The arrow between a machine's slots, filled as far as the work has come. Every machine's picture keeps
     * it out of the way of the upgrade column, at the same place.
     */
    protected void drawProgressArrow(final int offsetX, final int offsetY, final int left, final int top,
            final int width, final int height) {
        final int filled = Math.round(this.container.getWorkFraction() * width);
        if (filled > 0) {
            this.drawTexturedModalRect(offsetX + left, offsetY + top, ARROW_U, ARROW_V, filled, height);
        }
    }

    @Override
    public void initGui() {
        super.initGui();
        this.autoExport = new GuiImgButton(this.guiLeft + SIDE_BUTTON_LEFT, this.guiTop + 8, Settings.AUTO_EXPORT, YesNo.NO);
        this.buttonList.add(this.autoExport);
    }

    @Override
    protected void actionPerformed(final GuiButton button) throws IOException {
        super.actionPerformed(button);
        if (button == this.autoExport) {
            NetworkHandler.instance().sendToServer(new PacketConfigButton(this.autoExport.getSetting(), Mouse.isButtonDown(1)));
        }
    }

    @Override
    public void drawBG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        this.bindTexture(Tags.MOD_ID, this.background);
        this.drawTexturedModalRect(offsetX, offsetY, 0, 0, this.xSize, this.ySize);
        this.drawTexturedModalRect(offsetX + 177, offsetY, 177, 0, 35,
                14 + this.container.getMachine().getUpgradeInventory().getSlots() * 18);

        this.drawMachine(offsetX, offsetY);
        this.drawEnergyBar(offsetX, offsetY);
        this.drawSides(offsetX, offsetY);
    }

    @Override
    public void drawFG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        this.fontRenderer.drawString(this.getScreenTitle(), 8, 6, LABEL_COLOR);
        this.fontRenderer.drawString(I18n.format("container.inventory"), 8, this.ySize - 96 + 3, LABEL_COLOR);
        this.autoExport.set(this.container.autoExport);
    }

    private void drawEnergyBar(final int offsetX, final int offsetY) {
        this.bindTexture(Tags.MOD_ID, ENERGY_TEXTURE);
        Gui.drawModalRectWithCustomSizedTexture(offsetX + ENERGY_LEFT, offsetY + ENERGY_TOP, 0, 0,
                ENERGY_WIDTH, ENERGY_HEIGHT, ENERGY_TEXTURE_WIDTH, ENERGY_TEXTURE_HEIGHT);

        final int filled = Math.round(this.container.getEnergyFraction() * (ENERGY_HEIGHT - 2));
        if (filled > 0) {
            final int top = ENERGY_HEIGHT - 2 - filled;
            Gui.drawModalRectWithCustomSizedTexture(offsetX + ENERGY_LEFT + 1, offsetY + ENERGY_TOP + 1 + top,
                    4, top, 2, filled, ENERGY_TEXTURE_WIDTH, ENERGY_TEXTURE_HEIGHT);
        }
    }

    private void drawSides(final int offsetX, final int offsetY) {
        this.bindTexture(Tags.MOD_ID, GuiSideConfig.TEXTURE);
        Gui.drawModalRectWithCustomSizedTexture(offsetX + SIDES_LEFT, offsetY + SIDES_TOP, 0, 0,
                GuiSideConfig.WIDTH, GuiSideConfig.HEIGHT, GuiSideConfig.TEXTURE_WIDTH, GuiSideConfig.TEXTURE_HEIGHT);

        for (final RelativeSide side : GuiSideConfig.cells()) {
            final IoMode mode = this.container.getFace(side);
            if (mode == IoMode.NONE) {
                continue;
            }
            Gui.drawModalRectWithCustomSizedTexture(offsetX + SIDES_LEFT + GuiSideConfig.cellLeft(side),
                    offsetY + SIDES_TOP + GuiSideConfig.cellTop(side), GuiSideConfig.WIDTH, GuiSideConfig.markTop(mode),
                    GuiSideConfig.markSize(), GuiSideConfig.markSize(),
                    GuiSideConfig.TEXTURE_WIDTH, GuiSideConfig.TEXTURE_HEIGHT);
        }
    }

    @Override
    public void drawScreen(final int mouseX, final int mouseY, final float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);

        final RelativeSide hovered = GuiSideConfig.cellAt(this.guiLeft + SIDES_LEFT, this.guiTop + SIDES_TOP, mouseX, mouseY);
        if (hovered != null) {
            this.drawHoveringText(GuiSideConfig.tooltip(hovered, this.container.getFace(hovered)), mouseX, mouseY);
            return;
        }

        if (mouseX >= this.guiLeft + ENERGY_LEFT && mouseX < this.guiLeft + ENERGY_LEFT + ENERGY_WIDTH
                && mouseY >= this.guiTop + ENERGY_TOP && mouseY < this.guiTop + ENERGY_TOP + ENERGY_HEIGHT) {
            this.drawHoveringText(Arrays.asList(I18n.format("gui.threng.energy",
                    this.container.getEnergyStored(), this.container.getMaxEnergyStored())), mouseX, mouseY);
        }
    }

    @Override
    protected void mouseClicked(final int mouseX, final int mouseY, final int button) throws IOException {
        final RelativeSide clicked = GuiSideConfig.cellAt(this.guiLeft + SIDES_LEFT, this.guiTop + SIDES_TOP, mouseX, mouseY);
        if (clicked != null && button <= 1) {
            final IoMode mode = this.container.getFace(clicked);
            ModNetwork.CHANNEL.sendToServer(new PacketSideConfig(clicked, button == 0 ? mode.next() : mode.previous()));
            return;
        }
        super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * Everything of ours that stands outside the window, so HEI's item list stays off it.
     */
    @Override
    public List<Rectangle> getJEIExclusionArea() {
        final List<Rectangle> areas = new ArrayList<>(2);
        areas.add(new Rectangle(this.guiLeft + SIDE_BUTTON_LEFT, this.guiTop + 8, 18, 18));
        areas.add(new Rectangle(this.guiLeft + 177, this.guiTop, 35,
                14 + this.container.getMachine().getUpgradeInventory().getSlots() * 18));
        return areas;
    }
}
