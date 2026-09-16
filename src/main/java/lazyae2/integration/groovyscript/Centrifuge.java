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
import com.cleanroommc.groovyscript.api.documentation.annotations.RecipeBuilderRegistrationMethod;
import com.cleanroommc.groovyscript.api.documentation.annotations.RegistryDescription;
import com.cleanroommc.groovyscript.helper.recipe.AbstractRecipeBuilder;
import com.cleanroommc.groovyscript.registry.StandardListRegistry;

import lazyae2.recipe.LazyRecipes;
import lazyae2.recipe.PurifyRecipe;

@RegistryDescription
public final class Centrifuge extends StandardListRegistry<PurifyRecipe> {

    @RecipeBuilderDescription(example = {
            @Example(".input(ore('blockGlass')).output(item('minecraft:diamond'))"),
            @Example(".input(item('minecraft:gold_ingot')).output(item('minecraft:diamond'))")
    })
    public RecipeBuilder recipeBuilder() {
        return new RecipeBuilder(this);
    }

    @Override
    public Collection<PurifyRecipe> getRecipes() {
        return LazyRecipes.centrifuge();
    }

    @MethodDescription(example = @Example("item('appliedenergistics2:material')"))
    public void removeByInput(final IIngredient input) {
        this.getRecipes().removeIf(recipe -> {
            if (ScriptIngredient.anyMatches(input, recipe.getInput())) {
                this.addBackup(recipe);
                return true;
            }
            return false;
        });
    }

    @MethodDescription(example = @Example("item('appliedenergistics2:material:4')"))
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
    public static final class RecipeBuilder extends AbstractRecipeBuilder<PurifyRecipe> {

        private final Centrifuge registry;

        RecipeBuilder(final Centrifuge registry) {
            this.registry = registry;
        }

        @Override
        public String getErrorMsg() {
            return "Error adding Lazy AE2 Centrifuge recipe";
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
        }

        @Override
        @RecipeBuilderRegistrationMethod
        public @Nullable PurifyRecipe register() {
            if (!this.validate()) {
                return null;
            }
            final PurifyRecipe recipe = new PurifyRecipe(ScriptIngredient.of(this.input.get(0)), this.output.get(0));
            this.registry.add(recipe);
            return recipe;
        }
    }
}
