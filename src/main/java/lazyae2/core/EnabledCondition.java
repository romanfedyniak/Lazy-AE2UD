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
 */
public final class EnabledCondition implements IConditionFactory {

    @Override
    public BooleanSupplier parse(final JsonContext context, final JsonObject json) {
        final boolean enabled = LazyAE2Config.instance().isEnabled(JsonUtils.getString(json, "machine"));
        return () -> enabled;
    }
}
