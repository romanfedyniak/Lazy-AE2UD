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
import net.minecraftforge.fml.common.Loader;

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
    /** Whether a recipe viewer is there to open, which is the only reason to offer the arrow as a button. */
    private static final boolean RECIPE_VIEWER = Loader.isModLoaded("jei");

    /** Where the filled arrow was moved to in every machine's picture, out of the upgrade column's way. */
    private static final int ARROW_U = 0;
    /** The row every machine's picture keeps its moved sprites on. */
    protected static final int SPRITE_V = 200;
    /** The whole picture the bar is cut from; it is nothing like the 256 square the plain helper assumes. */
    private static final int ENERGY_TEXTURE_WIDTH = 6;
    private static final int ENERGY_TEXTURE_HEIGHT = 72;

    private final ContainerProcessor container;
    private final String background;
    private final Rectangle arrow;
    private GuiImgButton autoExport;

    protected GuiProcessor(final ContainerProcessor container, final String background, final int arrowLeft,
            final int arrowTop, final int arrowWidth, final int arrowHeight) {
        super(container);
        this.container = container;
        this.background = "gui/" + background + ".png";
        this.arrow = new Rectangle(arrowLeft, arrowTop, arrowWidth, arrowHeight);
        this.xSize = 176;
        this.ySize = ContainerProcessor.HEIGHT;
    }

    /**
     * Where the arrow sits in the window, which is also where HEI opens the machine's recipes from.
     */
    public Rectangle getArrowArea() {
        return this.arrow;
    }

    protected ContainerProcessor getContainer() {
        return this.container;
    }

    /**
     * The machine's own name, drawn at the top left unless the player renamed it.
     */
    protected abstract String getScreenTitle();

    private String getTitle() {
        return this.container.getMachine().hasCustomInventoryName()
                ? this.container.getMachine().getCustomInventoryName()
                : this.getScreenTitle();
    }

    /**
     * How far along the machine's work is drawn. The arrow alone, unless a machine has more to show.
     */
    protected void drawProgress(final int offsetX, final int offsetY) {
        this.drawProgressArrow(offsetX, offsetY, this.container.getWorkFraction());
    }

    /**
     * The arrow between a machine's slots, filled as far as the given fraction. Every machine's picture keeps
     * it out of the way of the upgrade column, at the same place.
     */
    protected final void drawProgressArrow(final int offsetX, final int offsetY, final float fraction) {
        final int filled = Math.round(fraction * this.arrow.width);
        if (filled > 0) {
            this.drawTexturedModalRect(offsetX + this.arrow.x, offsetY + this.arrow.y, ARROW_U, SPRITE_V, filled,
                    this.arrow.height);
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

        this.drawProgress(offsetX, offsetY);
        this.drawEnergyBar(offsetX, offsetY);
        GuiSideConfig.draw(offsetX + SIDES_LEFT, offsetY + SIDES_TOP, this.container);
    }

    @Override
    public void drawFG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        this.fontRenderer.drawString(this.getTitle(), 8, 6, LABEL_COLOR);
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
            return;
        }

        // HEI opens this machine's recipes from the arrow; the line is its own, so it is already translated
        if (RECIPE_VIEWER && this.arrow.contains(mouseX - this.guiLeft, mouseY - this.guiTop)) {
            this.drawHoveringText(Arrays.asList(I18n.format("jei.tooltip.show.recipes")), mouseX, mouseY);
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
