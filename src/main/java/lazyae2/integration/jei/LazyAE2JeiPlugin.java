/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.integration.jei;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.recipe.IRecipeCategoryRegistration;

import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;

import lazyae2.block.BlockMachine;
import lazyae2.client.gui.GuiAggregator;
import lazyae2.client.gui.GuiCentrifuge;
import lazyae2.client.gui.GuiEnergizer;
import lazyae2.client.gui.GuiEtcher;
import lazyae2.core.Registration;
import lazyae2.recipe.AggregatorRecipe;
import lazyae2.recipe.EnergizeRecipe;
import lazyae2.recipe.EtchRecipe;
import lazyae2.recipe.LazyRecipes;
import lazyae2.recipe.PurifyRecipe;

@JEIPlugin
public final class LazyAE2JeiPlugin implements IModPlugin {

    @Override
    public void registerCategories(final IRecipeCategoryRegistration registry) {
        final ItemStack aggregator = machineStack(BlockMachine.Type.AGGREGATOR);
        if (aggregator != null) {
            registry.addRecipeCategories(new AggregatorCategory(registry.getJeiHelpers().getGuiHelper(), aggregator));
        }

        final ItemStack centrifuge = machineStack(BlockMachine.Type.CENTRIFUGE);
        if (centrifuge != null) {
            registry.addRecipeCategories(new CentrifugeCategory(registry.getJeiHelpers().getGuiHelper(), centrifuge));
        }

        final ItemStack etcher = machineStack(BlockMachine.Type.ETCHER);
        if (etcher != null) {
            registry.addRecipeCategories(new EtcherCategory(registry.getJeiHelpers().getGuiHelper(), etcher));
        }

        final ItemStack energizer = machineStack(BlockMachine.Type.ENERGIZER);
        if (energizer != null) {
            registry.addRecipeCategories(new EnergizerCategory(registry.getJeiHelpers().getGuiHelper(), energizer));
        }
    }

    @Override
    public void register(final IModRegistry registry) {
        final ItemStack aggregator = machineStack(BlockMachine.Type.AGGREGATOR);
        if (aggregator != null) {
            final List<MachineRecipe> recipes = new ArrayList<>();
            for (final AggregatorRecipe recipe : LazyRecipes.aggregator()) {
                recipes.add(new MachineRecipe(recipe.getInputs(), recipe.getOutput()));
            }
            registry.addRecipes(recipes, AggregatorCategory.UID);
            registry.addRecipeCatalyst(aggregator, AggregatorCategory.UID);
            registry.addRecipeClickArea(GuiAggregator.class, GuiAggregator.ARROW_LEFT, GuiAggregator.ARROW_TOP,
                    GuiAggregator.ARROW_WIDTH, GuiAggregator.ARROW_HEIGHT, AggregatorCategory.UID);
        }

        final ItemStack centrifuge = machineStack(BlockMachine.Type.CENTRIFUGE);
        if (centrifuge != null) {
            final List<MachineRecipe> recipes = new ArrayList<>();
            for (final PurifyRecipe recipe : LazyRecipes.centrifuge()) {
                recipes.add(new MachineRecipe(recipe.getInputs(), recipe.getOutput()));
            }
            registry.addRecipes(recipes, CentrifugeCategory.UID);
            registry.addRecipeCatalyst(centrifuge, CentrifugeCategory.UID);
            registry.addRecipeClickArea(GuiCentrifuge.class, GuiCentrifuge.ARROW_LEFT, GuiCentrifuge.ARROW_TOP,
                    GuiCentrifuge.ARROW_WIDTH, GuiCentrifuge.ARROW_HEIGHT, CentrifugeCategory.UID);
        }

        final ItemStack etcher = machineStack(BlockMachine.Type.ETCHER);
        if (etcher != null) {
            final List<MachineRecipe> recipes = new ArrayList<>();
            for (final EtchRecipe recipe : LazyRecipes.etcher()) {
                recipes.add(new MachineRecipe(recipe.getInputs(), recipe.getOutput()));
            }
            registry.addRecipes(recipes, EtcherCategory.UID);
            registry.addRecipeCatalyst(etcher, EtcherCategory.UID);
            registry.addRecipeClickArea(GuiEtcher.class, GuiEtcher.ARROW_LEFT, GuiEtcher.ARROW_TOP,
                    GuiEtcher.ARROW_WIDTH, GuiEtcher.ARROW_HEIGHT, EtcherCategory.UID);
        }

        final ItemStack energizer = machineStack(BlockMachine.Type.ENERGIZER);
        if (energizer != null) {
            final List<MachineRecipe> recipes = new ArrayList<>();
            for (final EnergizeRecipe recipe : LazyRecipes.energizer()) {
                recipes.add(new MachineRecipe(recipe.getInputs(), recipe.getOutput(), cost(recipe.getEnergy())));
            }
            registry.addRecipes(recipes, EnergizerCategory.UID);
            registry.addRecipeCatalyst(energizer, EnergizerCategory.UID);
            registry.addRecipeClickArea(GuiEnergizer.class, GuiEnergizer.ARROW_LEFT, GuiEnergizer.ARROW_TOP,
                    GuiEnergizer.ARROW_WIDTH, GuiEnergizer.ARROW_HEIGHT, EnergizerCategory.UID);
        }
    }

    /**
     * What one charge costs, written the way the player's own language groups digits.
     */
    private static String cost(final int energy) {
        return I18n.format("gui.threng.energy.cost", NumberFormat.getIntegerInstance().format(energy));
    }

    /**
     * @return the machine as an item, or null while it is switched off in the config
     */
    @Nullable
    private static ItemStack machineStack(final BlockMachine.Type type) {
        return Registration.machine == null || !type.isEnabled() ? null
                : new ItemStack(Registration.machine, 1, type.getMeta());
    }
}
