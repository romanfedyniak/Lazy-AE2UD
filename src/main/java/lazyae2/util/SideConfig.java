/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.util;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

import io.netty.buffer.ByteBuf;

import net.minecraft.nbt.NBTTagCompound;

/**
 * What each face of a machine lets through, kept by relative side so that rotating the machine takes the
 * settings with it.
 */
public final class SideConfig {

    private final Map<RelativeSide, IoMode> faces = new EnumMap<>(RelativeSide.class);

    public SideConfig(final IoMode initial) {
        for (final RelativeSide side : RelativeSide.all()) {
            this.faces.put(side, initial);
        }
    }

    public IoMode get(final RelativeSide side) {
        return this.faces.get(side);
    }

    public boolean set(final RelativeSide side, final IoMode mode) {
        return this.faces.put(side, mode) != mode;
    }

    public void writeToNBT(final NBTTagCompound tag) {
        for (final RelativeSide side : RelativeSide.all()) {
            tag.setString(side.name(), this.faces.get(side).name());
        }
    }

    public void readFromNBT(final NBTTagCompound tag) {
        for (final RelativeSide side : RelativeSide.all()) {
            if (tag.hasKey(side.name())) {
                this.faces.put(side, IoMode.of(tag.getString(side.name())));
            }
        }
    }

    public void writeToStream(final ByteBuf data) throws IOException {
        for (final RelativeSide side : RelativeSide.all()) {
            data.writeByte(this.faces.get(side).ordinal());
        }
    }

    /**
     * @return whether anything changed, so the screen only redraws when it must
     */
    public boolean readFromStream(final ByteBuf data) throws IOException {
        boolean changed = false;
        for (final RelativeSide side : RelativeSide.all()) {
            final IoMode mode = IoMode.of(data.readByte());
            changed |= this.faces.put(side, mode) != mode;
        }
        return changed;
    }
}
