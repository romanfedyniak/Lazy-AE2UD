/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.client.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.util.Constants;

import lazyae2.block.BlockAssembler;
import lazyae2.container.ContainerAssembler;
import lazyae2.network.PacketAssemblerWork;
import lazyae2.tile.TileAssemblerController;
import lazyae2.tile.TileAssemblerPatterns;
import appeng.api.config.Settings;
import appeng.api.config.TerminalStyle;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.widgets.GuiScrollbar;
import appeng.client.gui.widgets.MEGuiTooltipTextField;
import appeng.client.me.ClientDCInternalInv;
import appeng.client.me.SlotDisconnected;
import appeng.client.me.search.RepoSearch;
import appeng.container.slot.AppEngSlot;
import appeng.core.AEClientConfig;
import appeng.core.localization.GuiText;
import appeng.util.Platform;

import static appeng.helpers.ItemStackHelper.stackFromNBT;

/**
 * The Mass Assembly Chamber's window: how busy it is, and every pattern of every module, a heading and four
 * rows a module, laid out on the Pattern Access Terminal's frame.
 * <p>
 * The search reads what a pattern makes, in the terminals' grammar. A module with nothing that matches is
 * left out, and what matches is marked, as the Pattern Access Terminal does it.
 */
public final class GuiAssembler extends AEBaseGui {

    private static final String TEXTURE = "guis/newinterfaceterminal.png";
    private static final int WIDTH = 208;
    /** Everything but the rows: the header above them and the player's inventory below. */
    private static final int FIXED_HEIGHT = 52 + 99;
    private static final int ROW_HEIGHT = 18;
    private static final int LIST_TOP = 51;
    private static final int LIST_LEFT = 22;
    private static final int LABEL_COLOR = 4210752;

    /** The wells of the header: the search on the first line, the two bars on the second. */
    private static final int SEARCH_LEFT = 32;
    private static final int SEARCH_TOP = 25;
    /** The inside of the two lower wells, measured off the texture: their frames are left alone. */
    private static final int BAR_TOP = 39;
    private static final int BAR_HEIGHT = 10;
    private static final int SLOTS_BAR_LEFT = 33;
    private static final int SLOTS_BAR_WIDTH = 84;
    private static final int PATTERNS_BAR_LEFT = 132;
    private static final int PATTERNS_BAR_WIDTH = 69;
    /** The ten-pixel square before each lower well, where a half-sized picture of what it counts goes. */
    private static final int ICON_TOP = 40;
    private static final int ICON_LEFT = 22;
    private static final int PATTERNS_ICON_LEFT = 121;

    private static final int SLOTS_FILL = 0xFF54B54C;
    private static final int PATTERNS_FILL = 0xFF4C8CD4;
    private static final int MATCH_COLOR = 0x2A00FF00;

    private final ContainerAssembler container;
    private final MEGuiTooltipTextField search;
    @Nullable
    private PacketAssemblerWork work;
    private final RepoSearch outputSearch = new RepoSearch();

    /** Each module the server told about, by position, and the order the chamber keeps them in. */
    private final Map<Long, ClientDCInternalInv> modules = new HashMap<>();
    private final Map<Long, Integer> order = new HashMap<>();
    /** What is shown: a module's heading, then the module. */
    private final List<Object> lines = new ArrayList<>();
    private final Set<ItemStack> matched = new HashSet<>();

    private int rows = 6;

    public GuiAssembler(final InventoryPlayer inventoryPlayer, final TileAssemblerController controller) {
        this(new ContainerAssembler(inventoryPlayer, controller));
    }

    public GuiAssembler(final ContainerAssembler container) {
        super(container);
        this.container = container;
        this.setScrollBar(new GuiScrollbar());
        this.xSize = WIDTH;

        this.search = new MEGuiTooltipTextField(86, 12,
                () -> RepoSearch.syntaxTooltip(I18n.format("gui.threng.assembler.search"))) {
            @Override
            public void onTextChange(final String oldText) {
                GuiAssembler.this.refreshList();
            }
        };
        this.search.setEnableBackgroundDrawing(false);
        this.search.setMaxStringLength(100);
        this.search.setTextColor(0xFFFFFF);
        this.search.setFocused(AEClientConfig.instance().focusesSearchOnOpen());
    }

    @Override
    public void initGui() {
        final TerminalStyle style =
                (TerminalStyle) AEClientConfig.instance().getConfigManager().getSetting(Settings.TERMINAL_STYLE);
        this.rows = Math.max(6, style.getRows((this.height - FIXED_HEIGHT) / ROW_HEIGHT));

        super.initGui();

        this.ySize = FIXED_HEIGHT + this.rows * ROW_HEIGHT;
        final int unused = this.height - this.ySize;
        this.guiTop = (int) Math.floor(unused / (unused < 0 ? 3.8f : 2.0f));

        this.search.x = this.guiLeft + SEARCH_LEFT;
        this.search.y = this.guiTop + SEARCH_TOP;

        for (final Object slot : this.inventorySlots.inventorySlots) {
            if (slot instanceof AppEngSlot) {
                final AppEngSlot appEngSlot = (AppEngSlot) slot;
                appEngSlot.yPos = this.ySize + appEngSlot.getY() - 78 - 7;
                appEngSlot.xPos = appEngSlot.getX() + 14;
            }
        }
        this.setScrollBar();
    }

    private void setScrollBar() {
        this.getScrollBar().setTop(LIST_TOP + 1).setLeft(189).setHeight(this.rows * ROW_HEIGHT - 2);
        this.getScrollBar().setRange(0, Math.max(0, this.lines.size() - 1), 1);
    }

    @Override
    public void drawScreen(final int mouseX, final int mouseY, final float partialTicks) {
        this.inventorySlots.inventorySlots.removeIf(slot -> slot instanceof SlotDisconnected);

        int offset = LIST_TOP;
        int drawn = 0;
        final int scroll = this.getScrollBar().getCurrentScroll();
        for (int line = scroll; line < this.lines.size() && drawn < this.rows; line++) {
            final Object shown = this.lines.get(line);
            if (shown instanceof ClientDCInternalInv) {
                final ClientDCInternalInv module = (ClientDCInternalInv) shown;
                for (int row = 0; row < TileAssemblerPatterns.SLOTS / 9 && drawn < this.rows; row++) {
                    for (int column = 0; column < 9; column++) {
                        this.inventorySlots.inventorySlots.add(new SlotDisconnected(module, column + row * 9,
                                LIST_LEFT + column * ROW_HEIGHT, offset + 1));
                    }
                    offset += ROW_HEIGHT;
                    drawn++;
                }
            } else {
                offset += ROW_HEIGHT;
                drawn++;
            }
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
        this.drawTooltip(this.search, mouseX, mouseY);
    }

    @Override
    public void drawBG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        this.bindTexture(TEXTURE);
        this.drawTexturedModalRect(offsetX, offsetY, 0, 0, this.xSize, 53);
        for (int row = 0; row < this.rows; row++) {
            this.drawTexturedModalRect(offsetX, offsetY + 53 + row * ROW_HEIGHT, 0, 52, this.xSize, ROW_HEIGHT);
        }

        int offset = LIST_TOP;
        int drawn = 0;
        final int scroll = this.getScrollBar().getCurrentScroll();
        for (int line = scroll; line < this.lines.size() && drawn < this.rows; line++) {
            if (this.lines.get(line) instanceof ClientDCInternalInv) {
                GlStateManager.color(1, 1, 1, 1);
                for (int row = 0; row < TileAssemblerPatterns.SLOTS / 9 && drawn < this.rows; row++) {
                    this.drawTexturedModalRect(offsetX + 20, offsetY + offset, 20, 173, 9 * ROW_HEIGHT, ROW_HEIGHT);
                    offset += ROW_HEIGHT;
                    drawn++;
                }
            } else {
                offset += ROW_HEIGHT;
                drawn++;
            }
        }
        this.drawTexturedModalRect(offsetX, offsetY + 50 + this.rows * ROW_HEIGHT, 0, 158, this.xSize, 99);

        this.search.setMatched(!this.lines.isEmpty() || this.search.getText().isEmpty());
        this.search.drawTextBox();

        this.drawBar(offsetX + SLOTS_BAR_LEFT, offsetY + BAR_TOP, SLOTS_BAR_WIDTH, this.container.busy,
                this.container.parallel, SLOTS_FILL);
        this.drawBar(offsetX + PATTERNS_BAR_LEFT, offsetY + BAR_TOP, PATTERNS_BAR_WIDTH, this.container.patterns,
                this.container.patternSlots, PATTERNS_FILL);
    }

    /** A bar laid inside one of the header's wells, the frame being the well's own. */
    private void drawBar(final int x, final int y, final int width, final long value, final long max, final int color) {
        final int filled = max <= 0 ? 0 : (int) Math.min(width, width * value / max);
        if (filled > 0) {
            drawRect(x, y, x + filled, y + BAR_HEIGHT, color);
        }
    }

    @Override
    public void drawFG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        this.fontRenderer.drawString(this.getGuiDisplayName(I18n.format("gui.threng.assembler.title")), 23, 6,
                LABEL_COLOR);
        this.fontRenderer.drawString(GuiText.inventory.getLocal(), 23, this.ySize - 96, LABEL_COLOR);

        this.drawSmallItem(ICON_LEFT, ICON_TOP, BlockAssembler.Type.MODULE_CPU.newStack(1));
        this.drawSmallItem(PATTERNS_ICON_LEFT, ICON_TOP, BlockAssembler.Type.MODULE_PATTERN.newStack(1));
        this.drawBarText(SLOTS_BAR_LEFT, SLOTS_BAR_WIDTH, this.container.busy + " / " + this.container.parallel);
        this.drawBarText(PATTERNS_BAR_LEFT, PATTERNS_BAR_WIDTH,
                this.container.patterns + " / " + this.container.patternSlots);

        int offset = LIST_TOP;
        int drawn = 0;
        final int scroll = this.getScrollBar().getCurrentScroll();
        for (int line = scroll; line < this.lines.size() && drawn < this.rows; line++) {
            final Object shown = this.lines.get(line);
            if (shown instanceof ClientDCInternalInv) {
                final ClientDCInternalInv module = (ClientDCInternalInv) shown;
                for (int row = 0; row < TileAssemblerPatterns.SLOTS / 9 && drawn < this.rows; row++) {
                    for (int column = 0; column < 9; column++) {
                        if (this.matched.contains(module.getInventory().getStackInSlot(column + row * 9))) {
                            final int x = LIST_LEFT + column * ROW_HEIGHT;
                            drawRect(x, offset + 1, x + 16, offset + 17, MATCH_COLOR);
                        }
                    }
                    offset += ROW_HEIGHT;
                    drawn++;
                }
            } else {
                final ModuleHeading heading = (ModuleHeading) shown;
                this.drawItem(LIST_LEFT, offset + 1, BlockAssembler.Type.MODULE_PATTERN.newStack(1));
                this.fontRenderer.drawString(I18n.format("gui.threng.assembler.module", heading.number),
                        LIST_LEFT + 20, offset + 6, LABEL_COLOR);
                offset += ROW_HEIGHT;
                drawn++;
            }
        }

        final int x = mouseX - offsetX;
        final int y = mouseY - offsetY;
        if (y >= BAR_TOP && y < BAR_TOP + BAR_HEIGHT) {
            if (x >= ICON_LEFT - 1 && x < SLOTS_BAR_LEFT + SLOTS_BAR_WIDTH) {
                this.drawTooltip(x, y, this.slotsTooltip());
            } else if (x >= PATTERNS_ICON_LEFT - 1 && x < PATTERNS_BAR_LEFT + PATTERNS_BAR_WIDTH) {
                this.drawTooltip(x, y, Arrays.asList(
                        I18n.format("gui.threng.assembler.patterns", this.container.patterns, this.container.patternSlots),
                        I18n.format("gui.threng.assembler.modules", this.modules.size())));
            }
        }
    }

    private List<String> slotsTooltip() {
        final double craftsPerSecond = this.container.busy * 20D / Math.max(1, this.container.ticksPerJob);
        final List<String> lines = new ArrayList<>(Arrays.asList(
                I18n.format("gui.threng.assembler.busy", this.container.busy, this.container.parallel),
                I18n.format("gui.threng.assembler.rate", String.format("%.1f", craftsPerSecond)),
                I18n.format("gui.threng.assembler.power", Platform.formatPowerLong(this.container.power, true))));
        if (this.work != null) {
            listCrafts(lines, "gui.threng.assembler.working", this.work.getWorking(), this.work.getWorkingKinds());
            listCrafts(lines, "gui.threng.assembler.waiting", this.work.getWaiting(), this.work.getWaitingKinds());
        }
        return lines;
    }

    /** One of the two lists of what the chamber holds: a heading, a line a kind, and how many kinds are left out. */
    private static void listCrafts(final List<String> lines, final String heading, final List<GenericStack> crafts,
            final int kinds) {
        if (crafts.isEmpty()) {
            return;
        }
        lines.add("");
        lines.add(TextFormatting.GRAY + I18n.format(heading));
        for (final GenericStack craft : crafts) {
            lines.add(I18n.format("gui.threng.assembler.crafts", craft.what().getDisplayName().getFormattedText(),
                    craft.amount()));
        }
        if (kinds > crafts.size()) {
            lines.add(TextFormatting.GRAY + I18n.format("gui.threng.assembler.moreKinds", kinds - crafts.size()));
        }
    }

    /** What the chamber is crafting, sent every half second while the window is open. */
    public void postWork(final PacketAssemblerWork work) {
        this.work = work;
    }

    private void drawBarText(final int left, final int width, final String text) {
        final int textWidth = this.fontRenderer.getStringWidth(text);
        this.fontRenderer.drawStringWithShadow(text, left + (width - textWidth) / 2F, BAR_TOP + 1, 0xFFFFFF);
    }

    private void drawSmallItem(final int x, final int y, final ItemStack stack) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);
        GlStateManager.scale(0.5, 0.5, 1);
        this.drawItem(0, 0, stack);
        GlStateManager.popMatrix();
    }

    @Override
    protected void mouseClicked(final int x, final int y, final int button) throws IOException {
        this.search.mouseClicked(x, y, button);
        super.mouseClicked(x, y, button);
    }

    @Override
    protected void keyTyped(final char character, final int key) throws IOException {
        if (this.checkHotbarKeys(key)) {
            return;
        }
        if (character == ' ' && this.search.isFocused() && this.search.getText().isEmpty()) {
            return;
        }
        if (this.search.textboxKeyTyped(character, key)) {
            this.refreshList();
        } else {
            super.keyTyped(character, key);
        }
    }

    public void postUpdate(final boolean clear, final List<NBTTagCompound> changed) {
        if (clear) {
            this.modules.clear();
            this.order.clear();
        }
        for (final NBTTagCompound tag : changed) {
            final long id = tag.getLong("id");
            final ClientDCInternalInv module = this.modules.computeIfAbsent(id,
                    key -> new ClientDCInternalInv(TileAssemblerPatterns.SLOTS, key, 0, "gui.threng.assembler.module"));
            this.order.put(id, tag.getInteger("index"));
            for (int slot = 0; slot < TileAssemblerPatterns.SLOTS; slot++) {
                final String which = Integer.toString(slot);
                if (tag.hasKey(which)) {
                    module.getInventory().setStackInSlot(slot, stackFromNBT(tag.getCompoundTag(which)));
                }
            }
        }
        this.refreshList();
    }

    /** The modules again, in the chamber's order, less those the search leaves out. */
    private void refreshList() {
        this.lines.clear();
        this.matched.clear();

        final String query = this.search.getText();
        this.outputSearch.setSearchString(query);
        this.outputSearch.refresh();

        final List<Long> ids = new ArrayList<>(this.modules.keySet());
        ids.sort((a, b) -> Integer.compare(this.order.getOrDefault(a, 0), this.order.getOrDefault(b, 0)));

        for (final long id : ids) {
            final ClientDCInternalInv module = this.modules.get(id);
            boolean found = query.isEmpty();
            for (final ItemStack pattern : module.getInventory()) {
                if (!query.isEmpty() && makes(pattern, this.outputSearch)) {
                    found = true;
                    if (this.outputSearch.hasPositiveTerms()) {
                        this.matched.add(pattern);
                    }
                }
            }
            if (found) {
                this.lines.add(new ModuleHeading(this.order.getOrDefault(id, 0) + 1));
                this.lines.add(module);
            }
        }
        this.setScrollBar();
    }

    /** Whether what a pattern makes answers the search, which is asked of all its outputs at once. */
    private static boolean makes(final ItemStack pattern, final RepoSearch search) {
        if (pattern.isEmpty() || pattern.getTagCompound() == null) {
            return false;
        }
        final NBTTagList outputs = pattern.getTagCompound().getTagList("out", Constants.NBT.TAG_COMPOUND);
        final List<AEKey> keys = new ArrayList<>(outputs.tagCount());
        for (int i = 0; i < outputs.tagCount(); i++) {
            final GenericStack stack = GenericStack.resolveItemStack(new ItemStack(outputs.getCompoundTagAt(i)));
            if (stack != null) {
                keys.add(stack.what());
            }
        }
        return search.matchesAny(keys);
    }

    private static final class ModuleHeading {

        final int number;

        ModuleHeading(final int number) {
            this.number = number;
        }
    }
}
