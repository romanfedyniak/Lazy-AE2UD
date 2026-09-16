/*
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.core;

import java.util.function.BooleanSupplier;

import com.google.gson.JsonObject;

import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.crafting.IConditionFactory;
import net.minecraftforge.common.crafting.JsonContext;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

/**
 * {@code threng:item_exists}: true while the item named by {@code item} is registered - for a recipe made of
 * something AE2 can be configured not to have. Forge 1.12 has no such condition of its own.
 */
public final class ItemExistsCondition implements IConditionFactory {

    @Override
    public BooleanSupplier parse(final JsonContext context, final JsonObject json) {
        final boolean exists = ForgeRegistries.ITEMS.containsKey(new ResourceLocation(JsonUtils.getString(json, "item")));
        return () -> exists;
    }
}
