/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.core;

import java.util.function.BooleanSupplier;

import com.google.gson.JsonObject;

import net.minecraft.util.JsonUtils;
import net.minecraftforge.common.crafting.IConditionFactory;
import net.minecraftforge.common.crafting.JsonContext;

/**
 * {@code threng:enabled}: true while the machine named by {@code machine} is switched on in the config.
 * {@code levelMaintainerTerminal} is not a machine but is named the same way, being a thing the config
 * takes away on its own.
 */
public final class EnabledCondition implements IConditionFactory {

    @Override
    public BooleanSupplier parse(final JsonContext context, final JsonObject json) {
        final String machine = JsonUtils.getString(json, "machine");
        final boolean enabled = "levelMaintainerTerminal".equals(machine)
                ? LazyAE2Config.instance().isLevelMaintainerTerminalEnabled()
                : LazyAE2Config.instance().isEnabled(machine);
        return () -> enabled;
    }
}
