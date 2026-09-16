/*
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.integration.theoneprobe;

import com.google.common.base.Function;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import mcjty.theoneprobe.api.ElementAlignment;
import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.IProbeInfoProvider;
import mcjty.theoneprobe.api.ITheOneProbe;
import mcjty.theoneprobe.api.ProbeMode;

import lazyae2.Tags;
import lazyae2.integration.probe.ProbeLine;
import lazyae2.integration.probe.ProbeReport;

/**
 * What The One Probe shows for this mod's machines. Handed to the probe by an inter-mod message, so the probe's
 * classes load only when it is installed.
 */
public final class LazyAE2Probe implements Function<ITheOneProbe, Void>, IProbeInfoProvider {

    @Override
    public Void apply(final ITheOneProbe probe) {
        probe.registerProvider(this);
        return null;
    }

    @Override
    public String getID() {
        return Tags.MOD_ID + ":machines";
    }

    @Override
    public void addProbeInfo(final ProbeMode mode, final IProbeInfo info, final EntityPlayer player, final World world,
            final IBlockState state, final IProbeHitData data) {
        for (final ProbeLine line : ProbeReport.of(world.getTileEntity(data.getPos()))) {
            final IProbeInfo row = info.horizontal(info.defaultLayoutStyle().alignment(ElementAlignment.ALIGN_CENTER));
            final String label = line.getColour() + IProbeInfo.STARTLOC + line.getLabel() + IProbeInfo.ENDLOC;
            if (!line.getItem().isEmpty()) {
                row.text(label + ":").item(line.getItem()).itemLabel(line.getItem());
            } else if (line.getValue().isEmpty()) {
                row.text(label);
            } else {
                row.text(label + ": " + (line.isValueKey()
                        ? IProbeInfo.STARTLOC + line.getValue() + IProbeInfo.ENDLOC
                        : line.getValue()));
            }
        }
    }
}
