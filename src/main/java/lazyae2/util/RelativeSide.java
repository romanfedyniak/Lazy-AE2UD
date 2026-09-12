/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.util;

import java.util.Locale;

import net.minecraft.util.EnumFacing;

/**
 * A face of a machine as the player sees it, whichever way the machine was placed. The names are the ones a
 * saved world holds: the old mod wrote them into its side settings as strings.
 */
public enum RelativeSide {

    FRONT,
    BACK,
    UP,
    LEFT,
    DOWN,
    RIGHT;

    private static final RelativeSide[] VALUES = values();

    public EnumFacing getDirection(final EnumFacing front) {
        switch (this) {
            case FRONT:
                return front;
            case BACK:
                return front.getOpposite();
            case UP:
                return EnumFacing.UP;
            case DOWN:
                return EnumFacing.DOWN;
            case LEFT:
                return front.rotateY();
            default:
                return front.rotateYCCW();
        }
    }

    public static RelativeSide of(final EnumFacing front, final EnumFacing face) {
        for (final RelativeSide side : VALUES) {
            if (side.getDirection(front) == face) {
                return side;
            }
        }
        return FRONT;
    }

    public static RelativeSide[] all() {
        return VALUES;
    }

    public String getTranslationKey() {
        return "gui.threng.side." + this.name().toLowerCase(Locale.ROOT);
    }
}
