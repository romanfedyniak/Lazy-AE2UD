/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.integration.crafttweaker;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.item.crafting.Ingredient;

import crafttweaker.annotations.ModOnly;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.item.IIngredient;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import stanhebben.zenscript.annotations.Optional;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

import lazyae2.recipe.AggregatorRecipe;
import lazyae2.recipe.LazyRecipes;

@ModOnly("crafttweaker")
@ZenRegister
@ZenClass("mods.threng.Aggregator")
public final class CrTAggregator {

    private CrTAggregator() {
    }

    @ZenMethod
    public static void addRecipe(final IItemStack output, final IIngredient in1, final IIngredient in2,
            @Nullable @Optional final IIngredient in3) {
        LateAction.add(LazyRecipes.aggregator(), () -> {
            final List<Ingredient> inputs = new ArrayList<>();
            inputs.add(ZenIngredient.of(in1));
            inputs.add(ZenIngredient.of(in2));
            // Left out, the third slot stays empty
            if (in3 != null) {
                inputs.add(ZenIngredient.of(in3));
            }
            return new AggregatorRecipe(inputs, CraftTweakerMC.getItemStack(output));
        }, "Aggregator", output);
    }

    @ZenMethod
    public static void removeRecipe(final IItemStack output) {
        LateAction.remove(LazyRecipes.aggregator(), AggregatorRecipe::getOutput, "Aggregator", output);
    }
}
