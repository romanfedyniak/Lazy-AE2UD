/*
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.integration.groovyscript;

import java.util.Collection;

import org.jetbrains.annotations.Nullable;

import com.cleanroommc.groovyscript.api.GroovyLog;
import com.cleanroommc.groovyscript.api.IIngredient;
import com.cleanroommc.groovyscript.api.documentation.annotations.Comp;
import com.cleanroommc.groovyscript.api.documentation.annotations.Example;
import com.cleanroommc.groovyscript.api.documentation.annotations.MethodDescription;
import com.cleanroommc.groovyscript.api.documentation.annotations.Property;
import com.cleanroommc.groovyscript.api.documentation.annotations.RecipeBuilderDescription;
import com.cleanroommc.groovyscript.api.documentation.annotations.RecipeBuilderMethodDescription;
import com.cleanroommc.groovyscript.api.documentation.annotations.RecipeBuilderRegistrationMethod;
import com.cleanroommc.groovyscript.api.documentation.annotations.RegistryDescription;
import com.cleanroommc.groovyscript.helper.recipe.AbstractRecipeBuilder;
import com.cleanroommc.groovyscript.registry.StandardListRegistry;

import lazyae2.recipe.EnergizeRecipe;
import lazyae2.recipe.LazyRecipes;

@RegistryDescription
public final class Energizer extends StandardListRegistry<EnergizeRecipe> {

    @RecipeBuilderDescription(example = {
            @Example(".input(ore('blockGlass')).energy(50).output(item('minecraft:diamond'))"),
            @Example(".input(item('minecraft:gold_ingot')).energy(10000).output(item('minecraft:diamond'))")
    })
    public RecipeBuilder recipeBuilder() {
        return new RecipeBuilder(this);
    }

    @Override
    public Collection<EnergizeRecipe> getRecipes() {
        return LazyRecipes.energizer();
    }

    @MethodDescription(example = @Example(value = "item('appliedenergistics2:material')", commented = true))
    public void removeByInput(final IIngredient input) {
        this.getRecipes().removeIf(recipe -> {
            if (ScriptIngredient.anyMatches(input, recipe.getInput())) {
                this.addBackup(recipe);
                return true;
            }
            return false;
        });
    }

    @MethodDescription(example = @Example("item('appliedenergistics2:material:1')"))
    public void removeByOutput(final IIngredient output) {
        this.getRecipes().removeIf(recipe -> {
            if (output.test(recipe.getOutput())) {
                this.addBackup(recipe);
                return true;
            }
            return false;
        });
    }

    @Property(property = "input", comp = @Comp(eq = 1))
    @Property(property = "output", comp = @Comp(eq = 1))
    public static final class RecipeBuilder extends AbstractRecipeBuilder<EnergizeRecipe> {

        private final Energizer registry;

        @Property(comp = @Comp(gt = 0))
        private int energy;

        RecipeBuilder(final Energizer registry) {
            this.registry = registry;
        }

        @RecipeBuilderMethodDescription
        public RecipeBuilder energy(final int energy) {
            this.energy = energy;
            return this;
        }

        @Override
        public String getErrorMsg() {
            return "Error adding Lazy AE2 Energizer recipe";
        }

        @Override
        protected int getMaxItemInput() {
            return 1;
        }

        @Override
        public void validate(final GroovyLog.Msg msg) {
            // Up to three, as GroovyScript's own support allowed; only the first is read
            this.validateItems(msg, 1, 3, 1, 1);
            this.validateFluids(msg);
            msg.add(this.energy <= 0, "energy must be greater than 0, yet it was {}", this.energy);
        }

        @Override
        @RecipeBuilderRegistrationMethod
        public @Nullable EnergizeRecipe register() {
            if (!this.validate()) {
                return null;
            }
            final EnergizeRecipe recipe =
                    new EnergizeRecipe(ScriptIngredient.of(this.input.get(0)), this.energy, this.output.get(0));
            this.registry.add(recipe);
            return recipe;
        }
    }
}
