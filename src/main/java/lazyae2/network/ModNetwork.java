/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.network;

import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

import lazyae2.Tags;

public final class ModNetwork {

    public static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(Tags.MOD_ID);

    private ModNetwork() {
    }

    public static void init() {
        CHANNEL.registerMessage(PacketSideConfig.Handler.class, PacketSideConfig.class, 0, Side.SERVER);
        CHANNEL.registerMessage(PacketMaintainerRow.Handler.class, PacketMaintainerRow.class, 1, Side.SERVER);
    }
}
