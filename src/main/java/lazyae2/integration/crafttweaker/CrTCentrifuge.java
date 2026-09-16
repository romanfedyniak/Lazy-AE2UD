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

import lazyae2.recipe.LazyRecipes;
import lazyae2.recipe.PurifyRecipe;

@ModOnly("crafttweaker")
@ZenRegister
@ZenClass("mods.threng.Centrifuge")
public final class CrTCentrifuge {

    private CrTCentrifuge() {
    }

    @ZenMethod
    public static void addRecipe(final IItemStack output, final IIngredient input) {
        LateAction.add(LazyRecipes.centrifuge(),
                () -> new PurifyRecipe(ZenIngredient.of(input), CraftTweakerMC.getItemStack(output)),
                "Centrifuge", output);
    }

    @ZenMethod
    public static void removeRecipe(final IItemStack output) {
        LateAction.remove(LazyRecipes.centrifuge(), PurifyRecipe::getOutput, "Centrifuge", output);
    }
}
