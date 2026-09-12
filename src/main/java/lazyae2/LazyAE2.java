/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import lazyae2.core.LazyAE2Config;
import lazyae2.core.Registration;

@Mod(modid = Tags.MOD_ID, name = Tags.MOD_NAME, version = Tags.VERSION, dependencies = LazyAE2.DEPENDENCIES)
public final class LazyAE2 {

    // No version range until AE2UD 1.6.0 is tagged: a JitPack build of a commit reports a bare hash, which sorts below it
    static final String DEPENDENCIES = "required-after:appliedenergistics2";

    public static final Logger LOG = LogManager.getLogger(Tags.MOD_ID);

    @Mod.Instance(Tags.MOD_ID)
    public static LazyAE2 instance;

    @Mod.EventHandler
    public void preInit(final FMLPreInitializationEvent event) {
        LazyAE2Config.init(event.getModConfigurationDirectory());
    }

    @Mod.EventHandler
    public void init(final FMLInitializationEvent event) {
        Registration.registerOres();
        Registration.registerRecipes();
    }
}
