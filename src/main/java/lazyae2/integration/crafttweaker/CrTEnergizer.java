/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.integration.crafttweaker;

import crafttweaker.annotations.ModOnly;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.item.IIngredient;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

import lazyae2.recipe.EnergizeRecipe;
import lazyae2.recipe.LazyRecipes;

@ModOnly("crafttweaker")
@ZenRegister
@ZenClass("mods.threng.Energizer")
public final class CrTEnergizer {

    private CrTEnergizer() {
    }

    @ZenMethod
    public static void addRecipe(final IItemStack output, final IIngredient input, final int energy) {
        LateAction.add(LazyRecipes.energizer(),
                () -> new EnergizeRecipe(ZenIngredient.of(input), energy, CraftTweakerMC.getItemStack(output)),
                "Energizer", output);
    }

    @ZenMethod
    public static void removeRecipe(final IItemStack output) {
        LateAction.remove(LazyRecipes.energizer(), EnergizeRecipe::getOutput, "Energizer", output);
    }
}
