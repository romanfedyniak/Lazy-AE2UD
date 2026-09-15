/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.client.gui;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mezz.jei.api.gui.IGhostIngredientHandler.Target;

import org.lwjgl.input.Keyboard;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;

import lazyae2.Tags;
import lazyae2.container.ContainerLevelMaintainer;
import lazyae2.network.ModNetwork;
import lazyae2.network.PacketMaintainerRow;
import lazyae2.tile.RowState;
import lazyae2.tile.TileLevelMaintainer;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AmountFormat;
import appeng.api.stacks.GenericStack;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.widgets.MEGuiTextField;
import appeng.container.interfaces.IJEIGhostIngredients;

/**
 * The maintainer's window: a row for each thing to keep. The slot carries how much to keep - the wheel moves
 * it - and the field beside it how much to order at a time, which is also what one notch of the wheel is
 * worth.
 */
public final class GuiLevelMaintainer extends AEBaseGui implements IJEIGhostIngredients {

    private static final int LABEL_COLOR = 4210752;

    private static final int STEP_LEFT = 43;
    private static final int FIELD_TOP = 21;
    private static final int FIELD_WIDTH = 103;

    /** The switch that takes a row out of the machine's rounds without emptying it. */
    private static final int TOGGLE_LEFT = 8;
    private static final int TOGGLE_TOP = 22;
    private static final int TOGGLE_SIZE = 10;
    private static final int TOGGLE_ON_COLOR = 0xFF54B54C;
    private static final int TOGGLE_OFF_COLOR = 0xFF4A4A4A;
    private static final int TOGGLE_HOVER_COLOR = 0x40FFFFFF;
    private static final int FIELD_HEIGHT = 13;

    /** The strip under a row that says what it is doing, in the gap the next row leaves free. */
    private static final int STATE_TOP = 35;
    private static final int STATE_HEIGHT = 3;
    /** Three pixels are nothing to aim at, so the whole gap between two rows answers for the strip. */
    private static final int STATE_HOVER_TOP = 34;
    private static final int STATE_HOVER_HEIGHT = 7;

    /** The tick beside each number, which takes what was typed just as Enter does. */
    private static final int SUBMIT_LEFT = 147;
    private static final int SUBMIT_SIZE = 13;
    private static final String SUBMIT_TEXTURE = "gui/component/submit.png";
    private static final int SUBMIT_SHEET_WIDTH = 39;
    /** The three the picture holds, side by side: as it stands, greyed out, and under the cursor. */
    private static final int SUBMIT_READY_U = 0;
    private static final int SUBMIT_IDLE_U = 13;
    private static final int SUBMIT_HOVERED_U = 26;

    private final ContainerLevelMaintainer container;
    private final MEGuiTextField[] steps = new MEGuiTextField[TileLevelMaintainer.ROWS];
    /** Which slot each of HEI's drop targets stands for, which a shift-click from HEI looks up. */
    private final Map<Target<?>, Object> ghostTargets = new HashMap<>();

    public GuiLevelMaintainer(final ContainerLevelMaintainer container) {
        super(container);
        this.container = container;
        this.xSize = 176;
        this.ySize = ContainerLevelMaintainer.HEIGHT;
    }

    @Override
    public void initGui() {
        super.initGui();

        for (int row = 0; row < TileLevelMaintainer.ROWS; row++) {
            // Laid out in the window's own coordinates, the ones the foreground pass draws in
            final MEGuiTextField field = new MEGuiTextField(this.fontRenderer, STEP_LEFT,
                    FIELD_TOP + ContainerLevelMaintainer.ROW_HEIGHT * row, FIELD_WIDTH, FIELD_HEIGHT);
            field.setEnableBackgroundDrawing(false);
            field.setMaxStringLength(TileLevelMaintainer.BATCH_DIGITS);
            field.setTextColor(MEGuiTextField.TEXT_COLOR);
            field.setVisible(true);
            field.setText(Long.toString(this.container.getBatch(row)));
            this.steps[row] = field;
        }
    }

    @Override
    public void drawBG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        this.bindTexture(Tags.MOD_ID, "gui/level_maintainer.png");
        this.drawTexturedModalRect(offsetX, offsetY, 0, 0, this.xSize, this.ySize);

        for (int row = 0; row < TileLevelMaintainer.ROWS; row++) {
            final int top = offsetY + TOGGLE_TOP + ContainerLevelMaintainer.ROW_HEIGHT * row;
            final int left = offsetX + TOGGLE_LEFT;
            drawRect(left + 1, top + 1, left + TOGGLE_SIZE - 1, top + TOGGLE_SIZE - 1,
                    this.container.isRowEnabled(row) ? TOGGLE_ON_COLOR : TOGGLE_OFF_COLOR);
            if (this.isIn(left, top, TOGGLE_SIZE, mouseX, mouseY)) {
                drawRect(left + 1, top + 1, left + TOGGLE_SIZE - 1, top + TOGGLE_SIZE - 1, TOGGLE_HOVER_COLOR);
            }
        }
        for (int row = 0; row < TileLevelMaintainer.ROWS; row++) {
            final int color = this.container.getRowState(row).color();
            if (color != 0) {
                final int top = offsetY + STATE_TOP + ContainerLevelMaintainer.ROW_HEIGHT * row;
                drawRect(offsetX + STEP_LEFT, top, offsetX + STEP_LEFT + FIELD_WIDTH, top + STATE_HEIGHT, color);
            }
        }
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        // The tick says whether there is anything to save: lit once the number differs from the machine's
        this.bindTexture(Tags.MOD_ID, SUBMIT_TEXTURE);
        for (int row = 0; row < TileLevelMaintainer.ROWS; row++) {
            final int top = offsetY + FIELD_TOP + ContainerLevelMaintainer.ROW_HEIGHT * row;
            final int u;
            if (!this.isEdited(row)) {
                u = SUBMIT_IDLE_U;
            } else {
                u = this.isSubmit(offsetX + SUBMIT_LEFT, top, mouseX, mouseY) ? SUBMIT_HOVERED_U : SUBMIT_READY_U;
            }
            Gui.drawModalRectWithCustomSizedTexture(offsetX + SUBMIT_LEFT, top, u, 0, SUBMIT_SIZE, SUBMIT_SIZE,
                    SUBMIT_SHEET_WIDTH, SUBMIT_SIZE);
        }
    }

    /**
     * Whether that row's field holds a number the machine has not been told about yet.
     */
    private boolean isEdited(final int row) {
        final long typed = parse(this.steps[row]);
        return typed >= 0 && typed != this.container.getBatch(row);
    }

    @Override
    public void drawFG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        this.fontRenderer.drawString(this.container.getMachine().hasCustomInventoryName()
                ? this.container.getMachine().getCustomInventoryName()
                : I18n.format("tile.threng.machine.level_maintainer.name"), 8, 6, LABEL_COLOR);
        this.fontRenderer.drawString(I18n.format("container.inventory"), 8, this.ySize - 96 + 3, LABEL_COLOR);

        this.drawAmounts();

        for (int row = 0; row < TileLevelMaintainer.ROWS; row++) {
            this.refresh(row);
            this.steps[row].drawTextBox();
        }
    }

    /**
     * How much each row keeps, drawn on its slot the way the level emitter draws its threshold: in the
     * thing's own units, and zero reads as "0" rather than as nothing at all.
     */
    private void drawAmounts() {
        boolean drawn = false;

        for (int row = 0; row < TileLevelMaintainer.ROWS; row++) {
            final GenericStack filter =
                    GenericStack.resolveItemStack(this.container.getSlot(row).getStack());
            if (filter == null) {
                continue;
            }
            final AEKey what = filter.what();
            this.stackSizeRenderer.renderAmount(this.fontRenderer,
                    what.formatAmount(this.container.getTarget(row), AmountFormat.SLOT), what,
                    ContainerLevelMaintainer.FILTER_LEFT,
                    ContainerLevelMaintainer.ROW_TOP + ContainerLevelMaintainer.ROW_HEIGHT * row);
            drawn = true;
        }

        // That renderer belongs to slot drawing, where lighting and depth are on, and it turns both back on
        // when it is done. Anything flat drawn after it comes out shaded - which is what swallowed the
        // numbers in the fields, but only while a row actually held something to draw an amount for.
        if (drawn) {
            GlStateManager.disableLighting();
            GlStateManager.disableDepth();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    /**
     * What the machine holds wins, unless the player is typing into that very field.
     */
    private void refresh(final int row) {
        final MEGuiTextField field = this.steps[row];
        final String text = Long.toString(this.container.getBatch(row));
        if (!field.isFocused() && !field.getText().equals(text)) {
            field.setText(text, true);
        }
        field.setTextColor(parse(field) < 0 ? MEGuiTextField.REFUSED_COLOR : MEGuiTextField.TEXT_COLOR);
    }

    @Override
    public void drawScreen(final int mouseX, final int mouseY, final float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);

        for (int row = 0; row < TileLevelMaintainer.ROWS; row++) {
            if (this.isIn(this.guiLeft + TOGGLE_LEFT, this.guiTop + TOGGLE_TOP
                    + ContainerLevelMaintainer.ROW_HEIGHT * row, TOGGLE_SIZE, mouseX, mouseY)) {
                this.drawHoveringText(Arrays.asList(I18n.format(this.container.isRowEnabled(row)
                        ? "gui.threng.maintainer.on" : "gui.threng.maintainer.off")), mouseX, mouseY);
                return;
            }
            if (this.steps[row].isMouseIn(mouseX - this.guiLeft, mouseY - this.guiTop)) {
                this.drawHoveringText(Arrays.asList(I18n.format("gui.threng.maintainer.batch"),
                        I18n.format("gui.threng.maintainer.wheel")), mouseX, mouseY);
                return;
            }
            final RowState state = this.container.getRowState(row);
            if (state != RowState.NONE && this.isIn(this.guiLeft + STEP_LEFT, this.guiTop + STATE_HOVER_TOP
                    + ContainerLevelMaintainer.ROW_HEIGHT * row, FIELD_WIDTH, STATE_HOVER_HEIGHT, mouseX, mouseY)) {
                this.drawHoveringText(Arrays.asList(state.tone() + I18n.format(state.nameKey())), mouseX, mouseY);
                return;
            }
        }
    }

    /**
     * The slot has room for a rounded reading only - "20K" may be anything from 20,000 to 20,999 - so the
     * exact number goes in the tooltip, where a terminal puts it too.
     */
    @Override
    protected void renderToolTip(final ItemStack stack, final int x, final int y) {
        final int row = this.rowOf(this.hoveredSlot);
        final GenericStack filter = row < 0 ? null : GenericStack.resolveItemStack(stack);
        if (filter == null) {
            super.renderToolTip(stack, x, y);
            return;
        }

        final List<String> lines = this.getItemToolTip(stack);
        lines.add(TextFormatting.GRAY + I18n.format("gui.threng.maintainer.keeping",
                filter.what().formatAmount(this.container.getTarget(row), AmountFormat.FULL)));

        final RowState state = this.container.getRowState(row);
        if (state != RowState.NONE) {
            lines.add(state.tone() + I18n.format(state.nameKey()));
        }
        this.drawHoveringText(lines, x, y, this.fontRenderer);
    }

    /**
     * A middle click on a row types how much it keeps, as it does on any filter slot of AE2's own.
     */
    @Override
    protected boolean allowsTypedAmount(final Slot slot) {
        return this.rowOf(slot) >= 0 && slot.getHasStack();
    }

    /**
     * @return which row that slot belongs to, or -1 for any other slot
     */
    private int rowOf(final Slot slot) {
        if (slot == null) {
            return -1;
        }
        for (int row = 0; row < TileLevelMaintainer.ROWS; row++) {
            if (this.container.getSlot(row) == slot) {
                return row;
            }
        }
        return -1;
    }

    @Override
    protected void mouseClicked(final int mouseX, final int mouseY, final int button) throws IOException {
        for (int row = 0; row < TileLevelMaintainer.ROWS; row++) {
            final int top = this.guiTop + FIELD_TOP + ContainerLevelMaintainer.ROW_HEIGHT * row;
            if (this.isIn(this.guiLeft + TOGGLE_LEFT, this.guiTop + TOGGLE_TOP
                    + ContainerLevelMaintainer.ROW_HEIGHT * row, TOGGLE_SIZE, mouseX, mouseY)) {
                this.container.showRowEnabled(row, !this.container.isRowEnabled(row));
                ModNetwork.CHANNEL.sendToServer(new PacketMaintainerRow(row, this.container.getTarget(row),
                        this.container.getBatch(row), this.container.isRowEnabled(row)));
                return;
            }

            if (this.isSubmit(this.guiLeft + SUBMIT_LEFT, top, mouseX, mouseY)) {
                this.send(row);
                this.steps[row].setFocused(false);
                return;
            }
            this.steps[row].mouseClicked(mouseX - this.guiLeft, mouseY - this.guiTop, button);
        }

        super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isSubmit(final int left, final int top, final int mouseX, final int mouseY) {
        return this.isIn(left, top, SUBMIT_SIZE, mouseX, mouseY);
    }

    private boolean isIn(final int left, final int top, final int size, final int mouseX, final int mouseY) {
        return this.isIn(left, top, size, size, mouseX, mouseY);
    }

    private boolean isIn(final int left, final int top, final int width, final int height, final int mouseX,
            final int mouseY) {
        return mouseX >= left && mouseX < left + width && mouseY >= top && mouseY < top + height;
    }

    @Override
    protected void keyTyped(final char character, final int key) throws IOException {
        for (int row = 0; row < TileLevelMaintainer.ROWS; row++) {
            if (!this.steps[row].isFocused()) {
                continue;
            }
            if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) {
                this.send(row);
                this.steps[row].setFocused(false);
                return;
            }
            if (this.steps[row].textboxKeyTyped(character, key)) {
                return;
            }
        }

        super.keyTyped(character, key);
    }

    private void send(final int row) {
        final long step = parse(this.steps[row]);
        if (step < 0) {
            return;
        }
        ModNetwork.CHANNEL.sendToServer(new PacketMaintainerRow(row, this.container.getTarget(row), step,
                this.container.isRowEnabled(row)));
        this.container.showBatch(row, step);
    }

    /**
     * @return the number in the field, or -1 when it is not one
     */
    private static long parse(final MEGuiTextField field) {
        try {
            final String text = field.getText().trim();
            return text.isEmpty() ? 0 : Long.parseLong(text);
        } catch (final NumberFormatException notANumber) {
            return -1;
        }
    }

    /**
     * Anything dragged out of HEI lands in a row: the rows are filter slots, so the base screen knows how
     * to take it, whether it is an item or a fluid.
     */
    @Override
    public List<Target<?>> getPhantomTargets(final Object ingredient) {
        return this.fakeSlotTargets(ingredient, this.ghostTargets);
    }

    @Override
    public Map<Target<?>, Object> getFakeSlotTargetMap() {
        return this.ghostTargets;
    }

    @Override
    public boolean isTextFieldFocused() {
        for (final MEGuiTextField field : this.steps) {
            if (field.isFocused()) {
                return true;
            }
        }
        return false;
    }
}
