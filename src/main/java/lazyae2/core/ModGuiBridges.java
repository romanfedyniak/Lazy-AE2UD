/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.core;

import net.minecraft.util.ResourceLocation;

import lazyae2.LazyAE2;
import lazyae2.Tags;
import lazyae2.block.BlockMachine;
import appeng.core.sync.GuiBridge;
import appeng.core.sync.GuiWrapper;

/**
 * This mod's windows as AE2 knows them.
 * <p>
 * A window of ours is opened by our own handler and needs nothing from AE2 to show itself. What does need a
 * name AE2 recognises is a window a player is sent <em>back</em> to: typing an amount opens one of AE2's own
 * screens over ours, and the way back is a {@link GuiBridge}. {@link GuiWrapper} makes one for an addon.
 */
public final class ModGuiBridges {

    private static final ResourceLocation LEVEL_MAINTAINER_ID =
            new ResourceLocation(Tags.MOD_ID, "level_maintainer");

    private static GuiBridge levelMaintainer;

    private ModGuiBridges() {
    }

    public static void init() {
        // Written out rather than as a lambda: the method it implements is generic, which a lambda cannot be
        GuiWrapper.INSTANCE.registerExternalGuiHandler(LEVEL_MAINTAINER_ID, new GuiWrapper.Opener() {
            @Override
            public <T extends GuiWrapper.IExternalGui> void open(final T window, final GuiWrapper.GuiContext context) {
                if (context.pos != null) {
                    context.player.openGui(LazyAE2.instance, BlockMachine.Type.LEVEL_MAINTAINER.getMeta(),
                            context.world, context.pos.getX(), context.pos.getY(), context.pos.getZ());
                }
            }
        });

        levelMaintainer = GuiWrapper.INSTANCE.wrap(() -> LEVEL_MAINTAINER_ID);
    }

    /**
     * @return the maintainer's window, or null before the mod has finished starting
     */
    public static GuiBridge levelMaintainer() {
        return levelMaintainer;
    }
}
