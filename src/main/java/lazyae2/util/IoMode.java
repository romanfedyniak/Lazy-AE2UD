/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.util;

import java.util.Locale;

/**
 * What a face of a machine lets through. The names and their order are the ones a saved world holds.
 */
public enum IoMode {

    INPUT(true, false),
    OUTPUT(false, true),
    OMNI(true, true),
    NONE(false, false);

    private static final IoMode[] VALUES = values();

    private final boolean input;
    private final boolean output;

    IoMode(final boolean input, final boolean output) {
        this.input = input;
        this.output = output;
    }

    public boolean allowsInput() {
        return this.input;
    }

    public boolean allowsOutput() {
        return this.output;
    }

    public IoMode next() {
        return VALUES[(this.ordinal() + 1) % VALUES.length];
    }

    public IoMode previous() {
        return VALUES[(this.ordinal() + VALUES.length - 1) % VALUES.length];
    }

    public static IoMode of(final int ordinal) {
        return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : NONE;
    }

    public static IoMode of(final String name) {
        for (final IoMode mode : VALUES) {
            if (mode.name().equals(name)) {
                return mode;
            }
        }
        return NONE;
    }

    public String getTranslationKey() {
        return "gui.threng.io." + this.name().toLowerCase(Locale.ROOT);
    }
}
