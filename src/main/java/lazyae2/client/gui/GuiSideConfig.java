/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.client.gui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;

import lazyae2.Tags;
import lazyae2.container.IMachineContainer;
import lazyae2.util.IoMode;
import lazyae2.util.RelativeSide;

/**
 * The little map of a machine's six faces: click a face to let items in, out, both ways or not at all. AE2
 * has nothing of the kind, so this one is ours, drawn from the old mod's picture.
 */
public final class GuiSideConfig {

    public static final int WIDTH = 17;
    public static final int HEIGHT = 17;

    static final String TEXTURE = "gui/component/side_io.png";
    static final int TEXTURE_WIDTH = 22;
    static final int TEXTURE_HEIGHT = 17;

    /** Which face each cell of the map stands for, laid out like the faces of an unfolded box. */
    private static final RelativeSide[] CELLS = {
            RelativeSide.UP, RelativeSide.LEFT, RelativeSide.FRONT,
            RelativeSide.RIGHT, RelativeSide.DOWN, RelativeSide.BACK
    };
    private static final int[] CELL_X = { 6, 1, 6, 11, 6, 11 };
    private static final int[] CELL_Y = { 1, 6, 6, 6, 11, 11 };
    private static final int CELL_SIZE = 5;

    private GuiSideConfig() {
    }

    /**
     * The map itself, with a mark on every face that lets something through. The picture is 22 by 17, not
     * the 256 square the plain helper assumes, so it is drawn with its own size given.
     */
    public static void draw(final int left, final int top, final IMachineContainer container) {
        Minecraft.getMinecraft().getTextureManager()
                .bindTexture(new ResourceLocation(Tags.MOD_ID, "textures/" + TEXTURE));
        Gui.drawModalRectWithCustomSizedTexture(left, top, 0, 0, WIDTH, HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);

        for (final RelativeSide side : CELLS) {
            final IoMode mode = container.getFace(side);
            if (mode != IoMode.NONE) {
                Gui.drawModalRectWithCustomSizedTexture(left + cellLeft(side), top + cellTop(side), WIDTH,
                        markTop(mode), CELL_SIZE, CELL_SIZE, TEXTURE_WIDTH, TEXTURE_HEIGHT);
            }
        }
    }

    /**
     * @return the face the cursor is over, or null
     */
    public static RelativeSide cellAt(final int left, final int top, final int mouseX, final int mouseY) {
        for (int cell = 0; cell < CELLS.length; cell++) {
            final int x = left + CELL_X[cell];
            final int y = top + CELL_Y[cell];
            if (mouseX >= x && mouseX < x + CELL_SIZE && mouseY >= y && mouseY < y + CELL_SIZE) {
                return CELLS[cell];
            }
        }
        return null;
    }

    public static int cellLeft(final RelativeSide side) {
        return CELL_X[indexOf(side)];
    }

    public static int cellTop(final RelativeSide side) {
        return CELL_Y[indexOf(side)];
    }

    public static RelativeSide[] cells() {
        return CELLS;
    }

    private static int indexOf(final RelativeSide side) {
        for (int cell = 0; cell < CELLS.length; cell++) {
            if (CELLS[cell] == side) {
                return cell;
            }
        }
        return 0;
    }

    /**
     * Where in the picture the mark for one setting is.
     */
    public static int markTop(final IoMode mode) {
        switch (mode) {
            case INPUT:
                return 0;
            case OUTPUT:
                return 5;
            default:
                return 10;
        }
    }

    public static int markSize() {
        return CELL_SIZE;
    }

    public static List<String> tooltip(final RelativeSide side, final IoMode mode) {
        final List<String> lines = new ArrayList<>(2);
        lines.add(TextFormatting.YELLOW + I18n.format(side.getTranslationKey()));
        lines.add(I18n.format(mode.getTranslationKey()));
        return lines;
    }
}
