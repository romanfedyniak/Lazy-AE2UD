/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.integration.crafttweaker;

import javax.annotation.Nullable;

import net.minecraftforge.oredict.OreIngredient;

import crafttweaker.CraftTweakerAPI;
import crafttweaker.annotations.ModOnly;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.item.IIngredient;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import stanhebben.zenscript.annotations.Optional;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

import lazyae2.recipe.EtchRecipe;
import lazyae2.recipe.LazyRecipes;

@ModOnly("crafttweaker")
@ZenRegister
@ZenClass("mods.threng.Etcher")
public final class CrTEtcher {

    private CrTEtcher() {
    }

    /**
     * Left out, the two presses are redstone on top and silicon below, as the machine's own recipes have them.
     */
    @ZenMethod
    public static void addRecipe(final IItemStack output, final IIngredient input,
            @Nullable @Optional final IIngredient topInput, @Nullable @Optional final IIngredient bottomInput) {
        if (topInput != null && bottomInput == null) {
            CraftTweakerAPI.logWarning("Etcher.addRecipe for " + output + " names a top press without a bottom one");
            return;
        }
        LateAction.add(LazyRecipes.etcher(), () -> new EtchRecipe(
                topInput == null ? new OreIngredient("dustRedstone") : ZenIngredient.of(topInput),
                bottomInput == null ? new OreIngredient("itemSilicon") : ZenIngredient.of(bottomInput),
                ZenIngredient.of(input), CraftTweakerMC.getItemStack(output)), "Etcher", output);
    }

    @ZenMethod
    public static void removeRecipe(final IItemStack output) {
        LateAction.remove(LazyRecipes.etcher(), EtchRecipe::getOutput, "Etcher", output);
    }
}
