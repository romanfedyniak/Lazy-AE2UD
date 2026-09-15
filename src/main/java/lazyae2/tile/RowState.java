/*
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.tile;

import net.minecraft.util.text.TextFormatting;

/**
 * What one row of a maintainer is doing, which both of its windows wear as a colour.
 * <p>
 * The order the values stand in is what goes over the wire - three bits a row in the machine's window, a
 * byte a row in the terminal's - so nothing may be inserted between them.
 */
public enum RowState {

    /** The row is empty, switched off, or asks for nothing. Nothing is drawn for it. */
    NONE(0, TextFormatting.GRAY),
    /** The network holds as much as the row keeps. */
    STOCKED(0xFF55FF55, TextFormatting.GREEN),
    /** A plan for what the row is short of is being worked out. */
    PLANNING(0xFF55FFFF, TextFormatting.AQUA),
    /** A crafting job the row ordered is running. */
    CRAFTING(0xFFFFFF55, TextFormatting.YELLOW),
    /** Every crafting processor is busy, so there is nowhere to put a job yet. */
    WAITING(0xFFFFAA00, TextFormatting.GOLD),
    /** Nothing on the network makes this. */
    NO_RECIPE(0xFFFF5555, TextFormatting.RED),
    /** The network was asked and turned the job down - most often something is missing further down. */
    REFUSED(0xFFFF5555, TextFormatting.RED);

    private static final RowState[] VALUES = values();

    private final int color;
    private final TextFormatting tone;

    RowState(final int color, final TextFormatting tone) {
        this.color = color;
        this.tone = tone;
    }

    /** The colour its mark is drawn in, or 0 for a row with nothing to say. */
    public int color() {
        return this.color;
    }

    /** The same colour for the line a tooltip writes, which has only the sixteen to pick from. */
    public TextFormatting tone() {
        return this.tone;
    }

    public String nameKey() {
        return "gui.threng.maintainer_state." + this.name().toLowerCase();
    }

    public static RowState byIndex(final int index) {
        return index >= 0 && index < VALUES.length ? VALUES[index] : NONE;
    }
}
