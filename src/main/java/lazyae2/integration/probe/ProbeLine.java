/*
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.integration.probe;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;

/**
 * One line a probe shows for a machine: a label, and after it a value, or an item drawn with its name.
 * <p>
 * It is worked out on the server and put into words on the client, so it carries translation keys and no text.
 */
public final class ProbeLine {

    private final String label;
    private final String value;
    /** Whether the value is a translation key as well, as a machine's name is. */
    private final boolean valueIsKey;
    private final ItemStack item;
    private final TextFormatting colour;

    private ProbeLine(final String label, final String value, final boolean valueIsKey, final ItemStack item,
            final TextFormatting colour) {
        this.label = label;
        this.value = value;
        this.valueIsKey = valueIsKey;
        this.item = item;
        this.colour = colour;
    }

    static ProbeLine of(final String label, final TextFormatting colour) {
        return new ProbeLine(label, "", false, ItemStack.EMPTY, colour);
    }

    static ProbeLine of(final String label, final String value) {
        return new ProbeLine(label, value, false, ItemStack.EMPTY, TextFormatting.WHITE);
    }

    static ProbeLine of(final String label, final String value, final TextFormatting colour) {
        return new ProbeLine(label, value, false, ItemStack.EMPTY, colour);
    }

    static ProbeLine named(final String label, final String nameKey) {
        return new ProbeLine(label, nameKey, true, ItemStack.EMPTY, TextFormatting.WHITE);
    }

    static ProbeLine item(final String label, final ItemStack item) {
        return new ProbeLine(label, "", false, item.copy(), TextFormatting.WHITE);
    }

    public String getLabel() {
        return this.label;
    }

    public String getValue() {
        return this.value;
    }

    public boolean isValueKey() {
        return this.valueIsKey;
    }

    public ItemStack getItem() {
        return this.item;
    }

    public TextFormatting getColour() {
        return this.colour;
    }

    /** The whole line in the reader's language, the item as its name; for a probe that draws text only. */
    public String toText() {
        final StringBuilder text = new StringBuilder().append(this.colour)
                .append(new TextComponentTranslation(this.label).getUnformattedText());
        final String shown = !this.item.isEmpty() ? this.item.getDisplayName()
                : this.valueIsKey ? new TextComponentTranslation(this.value).getUnformattedText() : this.value;
        if (!shown.isEmpty()) {
            text.append(": ").append(shown);
        }
        return text.toString();
    }

    NBTTagCompound write() {
        final NBTTagCompound tag = new NBTTagCompound();
        tag.setString("l", this.label);
        tag.setString("v", this.value);
        tag.setBoolean("k", this.valueIsKey);
        tag.setByte("c", (byte) this.colour.ordinal());
        if (!this.item.isEmpty()) {
            tag.setTag("i", this.item.writeToNBT(new NBTTagCompound()));
        }
        return tag;
    }

    static ProbeLine read(final NBTTagCompound tag) {
        final TextFormatting[] colours = TextFormatting.values();
        final int colour = tag.getByte("c");
        return new ProbeLine(tag.getString("l"), tag.getString("v"), tag.getBoolean("k"),
                tag.hasKey("i") ? new ItemStack(tag.getCompoundTag("i")) : ItemStack.EMPTY,
                colour >= 0 && colour < colours.length ? colours[colour] : TextFormatting.WHITE);
    }
}
