/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.ResourceLocation;

/**
 * The game's own font with the shadow taken off.
 * <p>
 * A text field always draws its text with a shadow and offers no way to say otherwise, while a number drawn
 * beside it by hand has none - so a field standing where a number was drawn suddenly grew one. Everything
 * here is handed straight to the real font, which is whatever a font mod has put there; nothing of this
 * one's own is ever used.
 */
final class PlainFont extends FontRenderer {

    private final FontRenderer real;

    PlainFont(final FontRenderer real) {
        super(Minecraft.getMinecraft().gameSettings, new ResourceLocation("textures/font/ascii.png"),
                Minecraft.getMinecraft().renderEngine, false);
        this.real = real;
        this.FONT_HEIGHT = real.FONT_HEIGHT;
    }

    @Override
    public int drawStringWithShadow(final String text, final float x, final float y, final int color) {
        return this.real.drawString(text, x, y, color, false);
    }

    @Override
    public int drawString(final String text, final float x, final float y, final int color, final boolean shadow) {
        return this.real.drawString(text, x, y, color, false);
    }

    @Override
    public int drawString(final String text, final int x, final int y, final int color) {
        return this.real.drawString(text, x, y, color);
    }

    @Override
    public int getCharWidth(final char character) {
        return this.real.getCharWidth(character);
    }

    @Override
    public int getStringWidth(final String text) {
        return this.real.getStringWidth(text);
    }

    @Override
    public String trimStringToWidth(final String text, final int width) {
        return this.real.trimStringToWidth(text, width);
    }

    @Override
    public String trimStringToWidth(final String text, final int width, final boolean reverse) {
        return this.real.trimStringToWidth(text, width, reverse);
    }
}
