/*
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.integration.groovyscript;

import java.util.Arrays;
import java.util.Collection;

import org.jetbrains.annotations.NotNull;

import com.cleanroommc.groovyscript.api.GroovyPlugin;
import com.cleanroommc.groovyscript.compat.mods.GroovyContainer;
import com.cleanroommc.groovyscript.compat.mods.GroovyPropertyContainer;

import lazyae2.Tags;

/**
 * Scripts for this mod's machines. GroovyScript ships its own support under the same mod id, written against the
 * old mod's library, which is gone; this plugin outranks it, so those classes are never loaded, and keeps its
 * names and methods, so a script written for the old mod runs unchanged.
 */
public final class LazyAE2GroovyPlugin implements GroovyPlugin {

    @Override
    public @NotNull String getModId() {
        return Tags.MOD_ID;
    }

    @Override
    public @NotNull String getContainerName() {
        return "Lazy AE2";
    }

    @Override
    public @NotNull Collection<String> getAliases() {
        return Arrays.asList(Tags.MOD_ID, "lazyae2");
    }

    @Override
    public @NotNull Priority getOverridePriority() {
        return Priority.OVERRIDE;
    }

    @Override
    public void onCompatLoaded(final GroovyContainer<?> container) {
    }

    @Override
    public GroovyPropertyContainer createGroovyPropertyContainer() {
        return new Registries();
    }

    public static final class Registries extends GroovyPropertyContainer {

        public final Aggregator aggregator = new Aggregator();
        public final Centrifuge centrifuge = new Centrifuge();
        public final Energizer energizer = new Energizer();
        public final Etcher etcher = new Etcher();
    }
}
