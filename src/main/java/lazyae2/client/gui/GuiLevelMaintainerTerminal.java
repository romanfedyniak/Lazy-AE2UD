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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import mezz.jei.api.gui.IGhostIngredientHandler.Target;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;

import lazyae2.Tags;
import lazyae2.block.BlockMachine;
import lazyae2.container.ContainerLevelMaintainerTerminal;
import lazyae2.network.ModNetwork;
import lazyae2.network.PacketTerminalAmount;
import lazyae2.network.PacketTerminalFilter;
import lazyae2.network.PacketTerminalRow;
import lazyae2.part.PartLevelMaintainerTerminal;
import lazyae2.tile.TileLevelMaintainer;
import appeng.api.config.ActionItems;
import appeng.api.config.Settings;
import appeng.api.config.TerminalStyle;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AmountFormat;
import appeng.api.stacks.GenericStack;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.KeySearchTarget;
import appeng.client.gui.widgets.GuiImgButton;
import appeng.client.gui.widgets.GuiScrollbar;
import appeng.client.gui.widgets.GuiSettingsDrawer;
import appeng.client.gui.widgets.MEGuiTextField;
import appeng.client.me.ClientDCInternalInv;
import appeng.client.me.SlotDisconnected;
import appeng.client.me.search.RepoSearch;
import appeng.client.render.BlockPosHighlighter;
import appeng.container.interfaces.IJEIGhostIngredients;
import appeng.container.slot.AppEngSlot;
import appeng.core.AEClientConfig;
import appeng.core.localization.ButtonToolTips;
import appeng.util.BlockPosUtils;
import appeng.util.Platform;

import static appeng.helpers.ItemStackHelper.stackFromNBT;

/**
 * Every ME Level Maintainer on the network in one list: a heading for each machine and its rows under it,
 * every one of them editable from here.
 * <p>
 * Nothing in the list is a slot of this window - a row belongs to a machine somewhere else - so a click is
 * answered by naming the machine and the row, and what the window holds is only what the server last told
 * it.
 */
public class GuiLevelMaintainerTerminal extends AEBaseGui implements IJEIGhostIngredients {

    private static final int MIN_ROWS = 6;
    private static final int ROW_HEIGHT = 18;
    private static final int FIXED_HEIGHT = 127;
    /** Where the first line of the list is drawn, and where the frame under it starts. */
    private static final int LIST_TOP = 30;

    private static final int LABEL_COLOR = 4210752;

    /**
     * The wells this mod paints, taken from the maintainer's own window: a dark edge above and to the left,
     * a lit one below and to the right, and a floor that is pale under a slot and dark under a number.
     */
    private static final int WELL_EDGE = 0xFF373737;
    private static final int WELL_LIT = 0xFFFFFFFF;
    private static final int SLOT_FLOOR = 0xFFAFAFAF;
    /**
     * A number well wears the colour a text field paints over it: in the maintainer's own window every row
     * carries a field of its own, so its wells are never seen bare, and one painted the texture's own dark
     * floor read as a hole beside them.
     */
    private static final int FIELD_FLOOR = 0xFFA8A8A8;

    /** The rows field sits over the well drawn into the texture; the names field beside it in the clear. */
    private static final int SEARCH_TOP = 17;
    private static final int SEARCH_HEIGHT = 12;
    private static final int ROWS_LEFT = 32;
    private static final int ROWS_WIDTH = 65;
    private static final int NAMES_LEFT = 111;
    private static final int NAMES_WIDTH = 75;

    /** The magnifier drawn beside a search box, and where the texture keeps the one it came with. */
    private static final int ICON_SIZE = 10;
    private static final int ICON_TOP = 18;
    private static final int ICON_SOURCE_LEFT = 21;

    /** One line of a machine: the switch, the thing to keep, and how much to order at a time. */
    private static final int TOGGLE_LEFT = 24;
    private static final int TOGGLE_TOP = 3;
    private static final int TOGGLE_SIZE = 10;
    private static final int TOGGLE_ON_COLOR = 0xFF54B54C;
    private static final int TOGGLE_OFF_COLOR = 0xFF4A4A4A;
    private static final int TOGGLE_HOVER_COLOR = 0x40FFFFFF;
    private static final int SLOT_LEFT = 38;
    private static final int BATCH_LEFT = 58;
    private static final int BATCH_WIDTH = 104;
    private static final int BATCH_HEIGHT = 12;
    private static final int BATCH_TOP = 2;
    /** Where the number itself sits, which is where the field puts it once one is opened over it. */
    private static final int BATCH_TEXT_LEFT = 2;
    private static final int BATCH_TEXT_TOP = 4;

    /** The tick beside the number being typed, which takes it just as Enter does. */
    private static final int SUBMIT_LEFT = 166;
    private static final int SUBMIT_TOP = 1;
    private static final int SUBMIT_SIZE = 13;
    private static final String SUBMIT_TEXTURE = "gui/component/submit.png";
    private static final int SUBMIT_SHEET_WIDTH = 39;
    private static final int SUBMIT_READY_U = 0;
    private static final int SUBMIT_IDLE_U = 13;
    private static final int SUBMIT_HOVERED_U = 26;

    /** The machine's name in its heading, and how much room it has before the list runs out. */
    private static final int NAME_LEFT = 41;
    private static final int NAME_WIDTH = 140;
    private static final int NAME_TEXT_TOP = 5;

    @Nullable
    private final PartLevelMaintainerTerminal part;

    private final Map<Long, ClientMaintainer> byId = new HashMap<>();
    private final List<Object> lines = new ArrayList<>();
    private final Set<Row> matchedRows = new HashSet<>();
    private final Set<ClientMaintainer> matchedMachines = new HashSet<>();
    private final Map<GuiButton, ClientMaintainer> highlightButtons = new HashMap<>();
    private final GuiSettingsDrawer settings = new GuiSettingsDrawer();
    /** Which row each of HEI's drop targets stands for. */
    private final Map<Target<?>, Object> ghostTargets = new HashMap<>();

    /** The rows box reads the terminals' grammar; the names box beside it is a plain substring. */
    private final RepoSearch rowSearch = new RepoSearch();

    private int rows = MIN_ROWS;
    private boolean needsRefresh;

    /** The font the row editor is given, so that what is typed looks like what it replaces. */
    private PlainFont plainFont;

    private MEGuiTextField searchFieldRows;
    private MEGuiTextField searchFieldNames;
    private GuiImgButton terminalStyleBox;
    private GuiImgButton searchKeepBtn;

    /** The one number being typed, and which machine and row it belongs to. */
    @Nullable
    private MEGuiTextField editor;
    private long editingMachine;
    private int editingRow;
    private int editingScroll;

    public GuiLevelMaintainerTerminal(final InventoryPlayer ip, final PartLevelMaintainerTerminal part) {
        this(new ContainerLevelMaintainerTerminal(ip, part), part);
    }

    /**
     * The panel is where this screen remembers its search between openings, and a terminal in a pocket has no
     * panel - so it is allowed to be absent, and a subclass says where to keep the text instead.
     */
    public GuiLevelMaintainerTerminal(final ContainerLevelMaintainerTerminal container,
            @Nullable final PartLevelMaintainerTerminal part) {
        super(container);
        this.part = part;
        this.setScrollBar(new GuiScrollbar());
        this.xSize = 208;
        this.ySize = ContainerLevelMaintainerTerminal.HEIGHT;
    }

    @Override
    public void initGui() {
        final TerminalStyle style = (TerminalStyle) AEClientConfig.instance().getConfigManager()
                .getSetting(Settings.TERMINAL_STYLE);
        this.rows = Math.max(MIN_ROWS, style.getRows((this.height - FIXED_HEIGHT) / ROW_HEIGHT));
        this.ySize = FIXED_HEIGHT + this.rows * ROW_HEIGHT;
        super.initGui();

        this.getScrollBar().setLeft(189);
        this.getScrollBar().setHeight(this.rows * ROW_HEIGHT - 2);
        this.getScrollBar().setTop(31);

        this.settings.attach(this.buttonList, this.guiLeft - 18, this.guiTop + 8);
        this.settings.take(this.terminalStyleBox = new GuiImgButton(0, 0, Settings.TERMINAL_STYLE, style));
        this.settings.take(this.searchKeepBtn = new GuiImgButton(0, 0, Settings.SEARCH_KEEP,
                AEClientConfig.instance().getConfigManager().getSetting(Settings.SEARCH_KEEP)));

        // The window grows with the list, so the player's own inventory follows it down
        for (final Object slot : this.inventorySlots.inventorySlots) {
            if (slot instanceof AppEngSlot) {
                final AppEngSlot appEngSlot = (AppEngSlot) slot;
                appEngSlot.yPos = appEngSlot.getY() + this.ySize - ContainerLevelMaintainerTerminal.HEIGHT;
            }
        }

        this.closeEditor();
        this.plainFont = new PlainFont(this.fontRenderer);

        final boolean keep = AEClientConfig.instance().keepsSearch();
        final MEGuiTextField previousRows = this.searchFieldRows;
        final MEGuiTextField previousNames = this.searchFieldNames;
        this.searchFieldRows = this.createSearchField(ROWS_LEFT, ROWS_WIDTH, keep ? this.loadSearchRows() : "");
        this.searchFieldNames = this.createSearchField(NAMES_LEFT, NAMES_WIDTH, keep ? this.loadSearchNames() : "");

        if (previousRows != null) {
            carryOver(previousRows, this.searchFieldRows);
            carryOver(previousNames, this.searchFieldNames);
        } else {
            this.searchFieldRows.setFocused(AEClientConfig.instance().focusesSearchOnOpen());
        }

        this.refreshList();
    }

    private MEGuiTextField createSearchField(final int left, final int width, final String text) {
        final MEGuiTextField field = new MEGuiTextField(this.fontRenderer, this.guiLeft + left,
                this.guiTop + SEARCH_TOP, width, SEARCH_HEIGHT);
        field.setEnableBackgroundDrawing(false);
        field.setMaxStringLength(100);
        field.setTextColor(MEGuiTextField.TEXT_COLOR);
        field.setVisible(true);
        field.setFocused(false);
        field.setText(text);
        return field;
    }

    protected String loadSearchRows() {
        return this.part == null ? "" : this.part.getSearchRows();
    }

    protected String loadSearchNames() {
        return this.part == null ? "" : this.part.getSearchNames();
    }

    protected void saveSearchText(final String rows, final String names) {
        if (this.part != null) {
            this.part.saveSearchStrings(rows, names);
        }
    }

    /**
     * Anything a subclass draws beside this window. The button list is emptied and refilled while the screen
     * is drawn, so whatever is added here has to be added again on every frame.
     */
    protected void addExtraButtons() {
    }

    @Override
    public void onGuiClosed() {
        final boolean keep = AEClientConfig.instance().keepsSearch();
        this.saveSearchText(keep ? this.searchFieldRows.getText() : "",
                keep ? this.searchFieldNames.getText() : "");
        super.onGuiClosed();
    }

    /**
     * The rows box only. The one beside it asks about a machine's name, which is not a key.
     */
    @Override
    public List<KeySearchTarget> getKeySearchTargets() {
        if (this.searchFieldRows == null) {
            return Collections.emptyList();
        }

        return Collections.singletonList(new KeySearchTarget(this.searchFieldRows.getArea(), what -> {
            this.searchFieldRows.setText(RepoSearch.termFor(what));
            this.refreshList();
        }));
    }

    @Override
    public List<Rectangle> getJEIExclusionArea() {
        if (this.terminalStyleBox == null) {
            return Collections.emptyList();
        }

        final List<Rectangle> area = new ArrayList<>(2);
        addButtonArea(area, this.terminalStyleBox);
        addButtonArea(area, this.searchKeepBtn);
        this.settings.addExclusionAreas(area);
        return area;
    }

    // ---- what the server sends -----------------------------------------------------------------------

    public void postUpdate(final NBTTagCompound in) {
        if (in.getBoolean("clear")) {
            this.byId.clear();
            this.needsRefresh = true;
        }

        for (final String key : in.getKeySet()) {
            if (!key.startsWith("=")) {
                continue;
            }

            try {
                final long id = Long.parseLong(key.substring(1), Character.MAX_RADIX);
                final NBTTagCompound saved = in.getCompoundTag(key);
                final ClientMaintainer machine = this.byId.computeIfAbsent(id,
                        which -> new ClientMaintainer(which, saved.getLong("sort")));

                if (saved.hasKey("name")) {
                    machine.name = saved.getString("name");
                    machine.custom = saved.getBoolean("custom");
                    machine.pos = NBTUtil.getPosFromTag(saved.getCompoundTag("pos"));
                    machine.dim = saved.getInteger("dim");
                    this.needsRefresh = true;
                }

                for (int row = 0; row < TileLevelMaintainer.ROWS; row++) {
                    final String which = Integer.toString(row);
                    if (!saved.hasKey(which)) {
                        continue;
                    }

                    final NBTTagCompound line = saved.getCompoundTag(which);
                    machine.inv.getInventory().setStackInSlot(row,
                            line.hasKey("f") ? stackFromNBT(line.getCompoundTag("f")) : ItemStack.EMPTY);
                    machine.targets[row] = line.getLong("t");
                    machine.batches[row] = line.getLong("b");
                    machine.enabled[row] = line.getBoolean("e");
                    this.needsRefresh = true;
                }
            } catch (final NumberFormatException ignored) {
            }
        }

        if (this.needsRefresh) {
            this.refreshList();
        }
    }

    /**
     * Which machines the search leaves, and which of their rows it points at.
     */
    private void refreshList() {
        this.needsRefresh = false;
        this.closeEditor();
        this.lines.clear();
        this.matchedRows.clear();
        this.matchedMachines.clear();

        final String rowQuery = this.searchFieldRows == null ? "" : this.searchFieldRows.getText();
        final String nameQuery = this.searchFieldNames == null ? ""
                : this.searchFieldNames.getText().toLowerCase();

        this.rowSearch.setSearchString(rowQuery);
        this.rowSearch.refresh();

        final List<ClientMaintainer> machines = new ArrayList<>(this.byId.values());
        Collections.sort(machines);

        for (final ClientMaintainer machine : machines) {
            if (!nameQuery.isEmpty() && !machine.displayName().toLowerCase().contains(nameQuery)) {
                continue;
            }

            if (rowQuery.isEmpty()) {
                // Nothing was asked of the rows, so none of them is dimmed as a miss
                this.matchedMachines.add(machine);
            } else if (!this.matchRows(machine)) {
                continue;
            }

            this.lines.add(machine);
            for (int row = 0; row < TileLevelMaintainer.ROWS; row++) {
                this.lines.add(machine.rows[row]);
            }
        }

        this.getScrollBar().setRange(0, Math.max(0, this.lines.size() - this.rows), 1);
    }

    /**
     * Whether a machine's rows answer the query, and which of them to mark. The query is asked of the whole
     * machine at once: {@code -iron} means none of its rows is iron, which no single row can answer.
     */
    private boolean matchRows(final ClientMaintainer machine) {
        final List<AEKey> keys = new ArrayList<>(TileLevelMaintainer.ROWS);
        for (int row = 0; row < TileLevelMaintainer.ROWS; row++) {
            final AEKey what = machine.keyOf(row);
            if (what != null) {
                keys.add(what);
            }
        }

        if (!this.rowSearch.matchesAny(keys)) {
            return false;
        }

        if (this.rowSearch.hasPositiveTerms()) {
            for (int row = 0; row < TileLevelMaintainer.ROWS; row++) {
                final AEKey what = machine.keyOf(row);
                if (what != null && this.rowSearch.matches(what)) {
                    this.matchedRows.add(machine.rows[row]);
                }
            }
        }

        return true;
    }

    // ---- drawing -------------------------------------------------------------------------------------

    /**
     * The buttons and the slots the lines carry, settled before any layer is painted - drawing them while
     * the foreground is painted leaves each of them a frame behind the rows underneath.
     */
    @Override
    public void drawScreen(final int mouseX, final int mouseY, final float partialTicks) {
        if (this.editor != null && this.getScrollBar().getCurrentScroll() != this.editingScroll) {
            this.closeEditor();
        }

        this.buttonList.clear();
        this.highlightButtons.clear();
        this.inventorySlots.inventorySlots.removeIf(slot -> slot instanceof SlotDisconnected);

        this.terminalStyleBox.set(AEClientConfig.instance().getConfigManager().getSetting(Settings.TERMINAL_STYLE));
        this.settings.addTo(this.buttonList);
        this.addExtraButtons();

        int offset = LIST_TOP;
        final int scroll = this.getScrollBar().getCurrentScroll();
        for (int line = 0; line < this.rows && scroll + line < this.lines.size(); line++) {
            final Object shown = this.lines.get(scroll + line);
            if (shown instanceof ClientMaintainer) {
                final GuiButton highlight = new HighlightButton(this.guiLeft + 4, this.guiTop + offset);
                this.highlightButtons.put(highlight, (ClientMaintainer) shown);
                this.buttonList.add(highlight);
            } else {
                final Row row = (Row) shown;
                this.inventorySlots.inventorySlots.add(
                        new SlotDisconnected(row.machine.inv, row.row, SLOT_LEFT, offset));
            }
            offset += ROW_HEIGHT;
        }

        super.drawScreen(mouseX, mouseY, partialTicks);

        this.drawHovered(mouseX, mouseY);
    }

    @Override
    public void drawBG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        this.bindTexture(Tags.MOD_ID, "gui/level_maintainer_terminal.png");
        // The list's top border belongs to the header, not to the lines: a strip taken from the border
        // itself repeats it every eighteen pixels and boxes in each empty line
        this.drawTexturedModalRect(offsetX, offsetY, 0, 0, this.xSize, 31);
        for (int line = 0; line < this.rows; line++) {
            this.drawTexturedModalRect(offsetX, offsetY + 31 + line * ROW_HEIGHT, 0, 31, 188, ROW_HEIGHT);
            this.drawTexturedModalRect(offsetX + 188, offsetY + 31 + line * ROW_HEIGHT, 188, 31,
                    this.xSize - 188, ROW_HEIGHT);
        }
        this.drawTexturedModalRect(offsetX, offsetY + 29 + this.rows * ROW_HEIGHT, 0, 137, this.xSize, 98);

        // The names box is ours, so it borrows the magnifier the texture already draws beside the other
        this.drawTexturedModalRect(offsetX + NAMES_LEFT - ICON_SIZE - 1, offsetY + ICON_TOP,
                ICON_SOURCE_LEFT, ICON_TOP, ICON_SIZE, ICON_SIZE);

        int offset = LIST_TOP;
        final int scroll = this.getScrollBar().getCurrentScroll();
        for (int line = 0; line < this.rows && scroll + line < this.lines.size(); line++) {
            final Object shown = this.lines.get(scroll + line);
            if (shown instanceof Row) {
                final Row row = (Row) shown;
                // The well is the frame around the slot, not the slot: it takes the pixel above and the
                // pixel to the left of what it holds, and the lit pixel on the other two sides
                drawLazyWell(offsetX + SLOT_LEFT - 1, offsetY + offset - 1, 18, 18, SLOT_FLOOR);
                drawLazyWell(offsetX + BATCH_LEFT, offsetY + offset + BATCH_TOP, BATCH_WIDTH, BATCH_HEIGHT,
                        FIELD_FLOOR);
                this.drawToggle(offsetX, offsetY + offset, row, mouseX, mouseY);
            }
            offset += ROW_HEIGHT;
        }

        this.drawSubmits(offsetX, offsetY, mouseX, mouseY);

        if (this.searchFieldRows != null) {
            // The rows field wears the well drawn into the texture; the names field is ours, so its own is
            // cut from the same shape the rest of the mod's wells are
            drawWell(offsetX + NAMES_LEFT, offsetY + SEARCH_TOP, NAMES_WIDTH, SEARCH_HEIGHT);

            final boolean matched = !this.lines.isEmpty() || (this.searchFieldRows.getText().isEmpty()
                    && this.searchFieldNames.getText().isEmpty());
            this.searchFieldRows.setMatched(matched);
            this.searchFieldNames.setMatched(matched);
            this.searchFieldRows.drawTextBox();
            this.searchFieldNames.drawTextBox();
        }

        if (this.editor != null) {
            this.editor.setTextColor(parse(this.editor) < 0 ? MEGuiTextField.REFUSED_COLOR
                    : MEGuiTextField.TEXT_COLOR);
            this.editor.drawTextBox();
        }
    }

    private void drawToggle(final int offsetX, final int top, final Row row, final int mouseX, final int mouseY) {
        final int left = offsetX + TOGGLE_LEFT;
        final int toggleTop = top + TOGGLE_TOP;
        drawLazyWell(left, toggleTop, TOGGLE_SIZE, TOGGLE_SIZE, FIELD_FLOOR);
        drawRect(left + 1, toggleTop + 1, left + TOGGLE_SIZE - 1, toggleTop + TOGGLE_SIZE - 1,
                row.machine.enabled[row.row] ? TOGGLE_ON_COLOR : TOGGLE_OFF_COLOR);
        if (isIn(left, toggleTop, TOGGLE_SIZE, TOGGLE_SIZE, mouseX, mouseY)) {
            drawRect(left + 1, toggleTop + 1, left + TOGGLE_SIZE - 1, toggleTop + TOGGLE_SIZE - 1,
                    TOGGLE_HOVER_COLOR);
        }
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void drawLazyWell(final int x, final int y, final int width, final int height,
            final int floor) {
        drawRect(x, y, x + width, y + height, WELL_EDGE);
        drawRect(x + 1, y + 1, x + width, y + height, WELL_LIT);
        drawRect(x + 1, y + 1, x + width - 1, y + height - 1, floor);
        // drawRect leaves whatever colour it painted with set, and the next thing drawn is textured
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /**
     * The tick each row wears: greyed out while there is nothing to save, and lit once the number being
     * typed differs from what that row was told to order.
     */
    private void drawSubmits(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        this.bindTexture(Tags.MOD_ID, SUBMIT_TEXTURE);

        int offset = LIST_TOP;
        final int scroll = this.getScrollBar().getCurrentScroll();
        for (int line = 0; line < this.rows && scroll + line < this.lines.size(); line++) {
            final Object shown = this.lines.get(scroll + line);
            final int left = offsetX + SUBMIT_LEFT;
            final int top = offsetY + offset + SUBMIT_TOP;
            offset += ROW_HEIGHT;

            if (!(shown instanceof Row)) {
                continue;
            }

            final Row row = (Row) shown;
            final int u;
            if (!this.isEdited(row)) {
                u = SUBMIT_IDLE_U;
            } else {
                u = isIn(left, top, SUBMIT_SIZE, SUBMIT_SIZE, mouseX, mouseY) ? SUBMIT_HOVERED_U
                        : SUBMIT_READY_U;
            }

            Gui.drawModalRectWithCustomSizedTexture(left, top, u, 0, SUBMIT_SIZE, SUBMIT_SIZE,
                    SUBMIT_SHEET_WIDTH, SUBMIT_SIZE);
        }
    }

    /**
     * Whether that row is being typed into and holds a number the machine has not been told about yet.
     */
    private boolean isEdited(final Row row) {
        if (this.editor == null || !this.isEditing(row.machine.id, row.row)) {
            return false;
        }

        final long typed = parse(this.editor);
        return typed >= 0 && typed != row.machine.batches[row.row];
    }

    @Override
    public void drawFG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        this.fontRenderer.drawString(this.getGuiDisplayName(I18n.format("gui.threng.maintainer_terminal.name")),
                8, 6, LABEL_COLOR);
        this.fontRenderer.drawString(I18n.format("container.inventory"), 23, this.ySize - 96 + 3, LABEL_COLOR);

        int offset = LIST_TOP;
        final int scroll = this.getScrollBar().getCurrentScroll();
        for (int line = 0; line < this.rows && scroll + line < this.lines.size(); line++) {
            final Object shown = this.lines.get(scroll + line);
            if (shown instanceof ClientMaintainer) {
                this.drawHeading((ClientMaintainer) shown, offset);
            } else {
                this.drawRow((Row) shown, offset);
            }
            offset += ROW_HEIGHT;
        }

        final int tooltipX = Mouse.getEventX() * this.width / this.mc.displayWidth - offsetX;
        if (this.searchFieldRows.isMouseIn(mouseX, mouseY)) {
            drawTooltip(tooltipX, mouseY - this.guiTop,
                    RepoSearch.syntaxTooltip(ButtonToolTips.SearchFieldConfigured.getLocal()));
        } else if (this.searchFieldNames.isMouseIn(mouseX, mouseY)) {
            drawTooltip(tooltipX, mouseY - this.guiTop, ButtonToolTips.SearchFieldNames.getLocal());
        }
    }

    private void drawHeading(final ClientMaintainer machine, final int offset) {
        final ItemStack icon = machine.icon();
        if (!icon.isEmpty()) {
            // Centred on the name beside it: the text sits five pixels into the line and is eight tall, so
            // a sixteen-tall icon starts one pixel in
            this.drawItem(23, offset + 1, icon);
            drawFlat();
        }

        String name = machine.displayName();
        while (name.length() > 2 && this.fontRenderer.getStringWidth(name) > NAME_WIDTH) {
            name = name.substring(0, name.length() - 1);
        }
        this.fontRenderer.drawString(name, NAME_LEFT, offset + NAME_TEXT_TOP, LABEL_COLOR);
    }

    private void drawRow(final Row row, final int offset) {
        if (this.matchedRows.contains(row)) {
            drawRect(SLOT_LEFT, offset, SLOT_LEFT + 16, offset + 16, 0x8A00FF00);
        } else if (!this.matchedMachines.contains(row.machine)) {
            drawRect(SLOT_LEFT, offset, SLOT_LEFT + 16, offset + 16, 0x6A000000);
        }

        // The number this row orders at a time, unless it is the one being typed
        if (!this.isEditing(row.machine.id, row.row)) {
            this.fontRenderer.drawString(Long.toString(row.machine.batches[row.row]),
                    BATCH_LEFT + BATCH_TEXT_LEFT, offset + BATCH_TEXT_TOP, MEGuiTextField.TEXT_COLOR);
        }

        // How much the row keeps, drawn on its slot the way the level emitter draws its threshold: in the
        // thing's own units, and zero reads as "0" rather than as nothing at all
        final AEKey what = row.machine.keyOf(row.row);
        if (what != null) {
            this.stackSizeRenderer.renderAmount(this.fontRenderer,
                    what.formatAmount(row.machine.targets[row.row], AmountFormat.SLOT), what, SLOT_LEFT, offset);
        }

        drawFlat();
    }

    /**
     * Back to drawing flat things. Both an item and an amount belong to slot drawing, where lighting and
     * depth are on, and each of them leaves that on behind it - so every line drawn after one of them came
     * out shaded, and the list turned pale the moment a row had anything in it.
     */
    private static void drawFlat() {
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /**
     * The lines under the cursor that are not slots, which the window has to say something about itself.
     */
    private void drawHovered(final int mouseX, final int mouseY) {
        int offset = LIST_TOP;
        final int scroll = this.getScrollBar().getCurrentScroll();
        for (int line = 0; line < this.rows && scroll + line < this.lines.size(); line++) {
            final Object shown = this.lines.get(scroll + line);
            final int top = this.guiTop + offset;
            offset += ROW_HEIGHT;

            if (shown instanceof ClientMaintainer) {
                continue;
            }

            final Row row = (Row) shown;
            if (isIn(this.guiLeft + TOGGLE_LEFT, top + TOGGLE_TOP, TOGGLE_SIZE, TOGGLE_SIZE, mouseX, mouseY)) {
                this.drawHoveringText(Collections.singletonList(I18n.format(row.machine.enabled[row.row]
                        ? "gui.threng.maintainer.on" : "gui.threng.maintainer.off")), mouseX, mouseY);
                return;
            }
            if (isIn(this.guiLeft + BATCH_LEFT, top, BATCH_WIDTH, ROW_HEIGHT, mouseX, mouseY)) {
                this.drawHoveringText(Arrays.asList(I18n.format("gui.threng.maintainer.batch"),
                        TextFormatting.GRAY + I18n.format("gui.threng.maintainer.type")), mouseX, mouseY);
                return;
            }
        }
    }

    /**
     * The slot has room for a rounded reading only - "20K" may be anything from 20,000 to 20,999 - so the
     * exact number goes in the tooltip.
     */
    @Override
    protected void renderToolTip(final ItemStack stack, final int x, final int y) {
        final Row row = this.rowOf(this.hoveredSlot);
        final GenericStack filter = row == null ? null : GenericStack.resolveItemStack(stack);
        if (filter == null) {
            super.renderToolTip(stack, x, y);
            return;
        }

        final List<String> lines = this.getItemToolTip(stack);
        lines.add(TextFormatting.GRAY + I18n.format("gui.threng.maintainer.keeping",
                filter.what().formatAmount(row.machine.targets[row.row], AmountFormat.FULL)));
        lines.add(TextFormatting.GRAY + I18n.format("gui.threng.maintainer.wheel"));
        this.drawHoveringText(lines, x, y, this.fontRenderer);
    }

    // ---- what the window sends back ------------------------------------------------------------------

    @Override
    protected void mouseClicked(final int mouseX, final int mouseY, final int button) throws IOException {
        if (this.editor != null) {
            if (button == 0 && isIn(this.guiLeft + SUBMIT_LEFT,
                    this.editor.getArea().y - BATCH_TOP + SUBMIT_TOP, SUBMIT_SIZE, SUBMIT_SIZE, mouseX,
                    mouseY)) {
                this.commitEditor();
                return;
            }

            if (this.editor.isMouseIn(mouseX, mouseY)) {
                this.editor.mouseClicked(mouseX, mouseY, button);
                return;
            }

            // Typing is given up wherever else the player goes, the same way a window left open is
            this.closeEditor();
        }

        this.searchFieldRows.mouseClicked(mouseX, mouseY, button);
        this.searchFieldNames.mouseClicked(mouseX, mouseY, button);

        if (button == 1) {
            if (this.searchFieldRows.isMouseIn(mouseX, mouseY)) {
                this.searchFieldRows.setText("");
                this.refreshList();
                return;
            }
            if (this.searchFieldNames.isMouseIn(mouseX, mouseY)) {
                this.searchFieldNames.setText("");
                this.refreshList();
                return;
            }
        }

        if (this.clickedList(mouseX, mouseY, button)) {
            return;
        }

        super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * @return true when the click belonged to a line rather than to the window under it
     */
    private boolean clickedList(final int mouseX, final int mouseY, final int button) {
        int offset = LIST_TOP;
        final int scroll = this.getScrollBar().getCurrentScroll();

        for (int line = 0; line < this.rows && scroll + line < this.lines.size(); line++) {
            final Object shown = this.lines.get(scroll + line);
            final int top = this.guiTop + offset;
            offset += ROW_HEIGHT;

            if (shown instanceof ClientMaintainer) {
                continue;
            }

            final Row row = (Row) shown;
            if (isIn(this.guiLeft + TOGGLE_LEFT, top + TOGGLE_TOP, TOGGLE_SIZE, TOGGLE_SIZE, mouseX, mouseY)) {
                this.closeEditor();
                row.machine.enabled[row.row] = !row.machine.enabled[row.row];
                ModNetwork.CHANNEL.sendToServer(new PacketTerminalRow(row.machine.id, row.row,
                        row.machine.batches[row.row], row.machine.enabled[row.row]));
                return true;
            }

            // The whole band between the slot and the tick takes the click, not the well alone
            if (isIn(this.guiLeft + BATCH_LEFT, top, BATCH_WIDTH, ROW_HEIGHT, mouseX, mouseY)) {
                this.openEditor(row.machine.id, row.row, this.guiLeft + BATCH_LEFT, top + BATCH_TOP,
                        BATCH_WIDTH, BATCH_HEIGHT, Long.toString(row.machine.batches[row.row]));
                return true;
            }
        }

        return false;
    }

    /**
     * A middle click on a row types how much it keeps. AE2's own middle click can only reach a slot of this
     * window, and these rows belong to machines elsewhere.
     */
    @Override
    protected void handleMouseClick(final Slot slot, final int slotIdx, final int mouseButton,
            final ClickType clickType) {
        final Row row = this.rowOf(slot);
        if (row != null && mouseButton == 2 && slot.getHasStack()) {
            ModNetwork.CHANNEL.sendToServer(new PacketTerminalAmount(row.machine.id, row.row));
            return;
        }

        super.handleMouseClick(slot, slotIdx, mouseButton, clickType);
    }

    @Override
    protected void actionPerformed(final GuiButton btn) throws IOException {
        if (this.settings.actionPerformed(btn)) {
            return;
        }

        if (this.toggleSearchKeep(btn, this.searchKeepBtn)) {
            return;
        }

        if (btn == this.terminalStyleBox) {
            final TerminalStyle current = (TerminalStyle) AEClientConfig.instance().getConfigManager()
                    .getSetting(Settings.TERMINAL_STYLE);
            final TerminalStyle next = (TerminalStyle) Platform.rotateEnum(current, Mouse.isButtonDown(1),
                    Settings.TERMINAL_STYLE.getPossibleValues());
            AEClientConfig.instance().getConfigManager().putSetting(Settings.TERMINAL_STYLE, next);
            this.refreshLayout();
            return;
        }

        final ClientMaintainer machine = this.highlightButtons.get(btn);
        if (machine != null) {
            this.highlight(machine);
        }
    }

    /**
     * Where the machine stands, marked in the world with the player turned towards it - and the screen
     * closed, since the point of asking is to go and look.
     */
    private void highlight(final ClientMaintainer machine) {
        if (machine.pos == null) {
            return;
        }

        if (this.mc.world.provider.getDimension() != machine.dim) {
            this.mc.player.sendStatusMessage(
                    new TextComponentTranslation("gui.threng.maintainer.otherDimension", machine.dim), false);
            return;
        }

        BlockPosHighlighter.hilightBlock(machine.pos, System.currentTimeMillis()
                + 500L * BlockPosUtils.getDistance(machine.pos, this.mc.player.getPosition()), machine.dim);
        BlockPosHighlighter.turnPlayerTowards(machine.pos);
        this.mc.player.sendStatusMessage(new TextComponentTranslation("gui.threng.maintainer.highlighted",
                machine.pos.getX(), machine.pos.getY(), machine.pos.getZ()), false);
        this.mc.player.closeScreen();
    }

    @Override
    public boolean isTextFieldFocused() {
        return (this.editor != null && this.editor.isFocused())
                || (this.searchFieldRows != null
                        && (this.searchFieldRows.isFocused() || this.searchFieldNames.isFocused()));
    }

    @Override
    protected void keyTyped(final char character, final int key) throws IOException {
        if (this.editor != null && this.editor.isFocused()) {
            if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) {
                this.commitEditor();
                return;
            }
            if (key == Keyboard.KEY_ESCAPE) {
                this.closeEditor();
                return;
            }
            if (this.editor.textboxKeyTyped(character, key)) {
                return;
            }
        }

        if (this.checkHotbarKeys(key)) {
            return;
        }

        if (character == ' ' && ((this.searchFieldRows.getText().isEmpty() && this.searchFieldRows.isFocused())
                || (this.searchFieldNames.getText().isEmpty() && this.searchFieldNames.isFocused()))) {
            return;
        }

        if (key == Keyboard.KEY_TAB && this.searchFieldRows.isFocused() != this.searchFieldNames.isFocused()) {
            final boolean onRows = this.searchFieldRows.isFocused();
            this.searchFieldRows.setFocused(!onRows);
            this.searchFieldNames.setFocused(onRows);
            return;
        }

        if (this.searchFieldRows.textboxKeyTyped(character, key)
                || this.searchFieldNames.textboxKeyTyped(character, key)) {
            this.refreshList();
        } else {
            super.keyTyped(character, key);
        }
    }

    // ---- the one field the list carries --------------------------------------------------------------

    private boolean isEditing(final long machine, final int row) {
        return this.editor != null && this.editingMachine == machine && this.editingRow == row;
    }

    private void openEditor(final long machine, final int row, final int left, final int top, final int width,
            final int height, final String text) {
        this.editingMachine = machine;
        this.editingRow = row;
        this.editingScroll = this.getScrollBar().getCurrentScroll();

        // A field is laid out where it is built and cannot be moved afterwards, so the one field this list
        // carries is built again wherever it is opened
        this.editor = new MEGuiTextField(this.plainFont, left, top, width, height);
        this.editor.setEnableBackgroundDrawing(false);
        this.editor.setMaxStringLength(TileLevelMaintainer.BATCH_DIGITS);
        this.editor.setTextColor(MEGuiTextField.TEXT_COLOR);
        this.editor.setVisible(true);
        this.editor.setText(text);
        this.editor.setFocused(true);
        this.editor.selectAll();

        this.searchFieldRows.setFocused(false);
        this.searchFieldNames.setFocused(false);
    }

    private void closeEditor() {
        this.editor = null;
    }

    private void commitEditor() {
        if (this.editor == null) {
            return;
        }

        final ClientMaintainer machine = this.byId.get(this.editingMachine);
        if (machine == null) {
            this.closeEditor();
            return;
        }

        final long batch = parse(this.editor);
        if (batch < 0) {
            return;
        }

        ModNetwork.CHANNEL.sendToServer(new PacketTerminalRow(machine.id, this.editingRow, batch,
                machine.enabled[this.editingRow]));
        machine.batches[this.editingRow] = batch;
        this.closeEditor();
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

    @Nullable
    private Row rowOf(@Nullable final Slot slot) {
        if (!(slot instanceof SlotDisconnected)) {
            return null;
        }

        final ClientMaintainer machine = this.byId.get(((SlotDisconnected) slot).getSlot().getId());
        return machine == null || slot.getSlotIndex() < 0 || slot.getSlotIndex() >= TileLevelMaintainer.ROWS
                ? null
                : machine.rows[slot.getSlotIndex()];
    }

    private static boolean isIn(final int left, final int top, final int width, final int height,
            final int mouseX, final int mouseY) {
        return mouseX >= left && mouseX < left + width && mouseY >= top && mouseY < top + height;
    }

    // ---- HEI ------------------------------------------------------------------------------------------

    /**
     * A row takes anything dragged out of HEI, item or fluid alike. The drop never touches the hand, so it
     * cannot go through the click that a row's slot otherwise answers.
     */
    @Override
    public List<Target<?>> getPhantomTargets(final Object ingredient) {
        final List<Target<?>> targets = new ArrayList<>();

        for (final Slot slot : this.inventorySlots.inventorySlots) {
            final Row row = this.rowOf(slot);
            if (row == null) {
                continue;
            }

            final Target<Object> target = new Target<Object>() {

                @Override
                public Rectangle getArea() {
                    return new Rectangle(getGuiLeft() + slot.xPos, getGuiTop() + slot.yPos, 16, 16);
                }

                @Override
                public void accept(final Object dropped) {
                    final GenericStack what = asGenericStack(dropped);
                    if (what != null) {
                        ModNetwork.CHANNEL.sendToServer(new PacketTerminalFilter(row.machine.id, row.row,
                                GenericStack.wrapInItemStack(what.what(), 1)));
                    }
                }
            };

            targets.add(target);
            this.ghostTargets.putIfAbsent(target, slot);
        }

        return targets;
    }

    @Nullable
    private static GenericStack asGenericStack(final Object ingredient) {
        if (ingredient instanceof FluidStack) {
            final FluidStack fluid = (FluidStack) ingredient;
            return new GenericStack(AEFluidKey.of(fluid), fluid.amount);
        }
        if (ingredient instanceof ItemStack && !((ItemStack) ingredient).isEmpty()) {
            return GenericStack.resolveItemStack((ItemStack) ingredient);
        }
        return null;
    }

    @Override
    public Map<Target<?>, Object> getFakeSlotTargetMap() {
        return this.ghostTargets;
    }

    /**
     * The button that says where a machine is. AE2's own picture for it names an interface, so the line it
     * shows is ours.
     */
    private static final class HighlightButton extends GuiImgButton {

        private HighlightButton(final int x, final int y) {
            super(x, y, Settings.ACTIONS, ActionItems.HIGHLIGHT_INTERFACE);
        }

        @Override
        public String getMessage() {
            return I18n.format("gui.threng.maintainer.highlight");
        }
    }

    /**
     * One row of one machine, which is what a line of the list stands for.
     */
    private static final class Row {

        private final ClientMaintainer machine;
        private final int row;

        private Row(final ClientMaintainer machine, final int row) {
            this.machine = machine;
            this.row = row;
        }
    }

    /**
     * One maintainer as this window last heard of it.
     */
    private static final class ClientMaintainer implements Comparable<ClientMaintainer> {

        private final long id;
        private final long sortBy;
        private final ClientDCInternalInv inv;
        private final Row[] rows = new Row[TileLevelMaintainer.ROWS];
        private final long[] targets = new long[TileLevelMaintainer.ROWS];
        private final long[] batches = new long[TileLevelMaintainer.ROWS];
        private final boolean[] enabled = new boolean[TileLevelMaintainer.ROWS];

        private String name = "";
        private boolean custom;
        @Nullable
        private BlockPos pos;
        private int dim;

        private ClientMaintainer(final long id, final long sortBy) {
            this.id = id;
            this.sortBy = sortBy;
            this.inv = new ClientDCInternalInv(TileLevelMaintainer.ROWS, id, sortBy, "");
            for (int row = 0; row < TileLevelMaintainer.ROWS; row++) {
                this.rows[row] = new Row(this, row);
            }
        }

        private String displayName() {
            return this.custom ? this.name : I18n.format(this.name);
        }

        private ItemStack icon() {
            return BlockMachine.Type.LEVEL_MAINTAINER.newStack(1);
        }

        @Nullable
        private AEKey keyOf(final int row) {
            final GenericStack filter = GenericStack.resolveItemStack(this.inv.getInventory().getStackInSlot(row));
            return filter == null ? null : filter.what();
        }

        @Override
        public int compareTo(final ClientMaintainer other) {
            return Long.compare(this.sortBy, other.sortBy);
        }
    }
}
