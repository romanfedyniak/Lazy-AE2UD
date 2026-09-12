/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.integration.jei;

import javax.annotation.Nullable;

import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeCategory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import lazyae2.Tags;

/**
 * What one of this mod's machines makes, as a recipe screen: a row of slots, an arrow, and one slot out. The
 * slots and the arrow are cut out of the furnace, the way the categories that have no window of their own do
 * it in AE2.
 */
abstract class MachineCategory extends Object implements IRecipeCategory<MachineRecipe> {

    static final int SLOT = 18;

    private static final ResourceLocation FURNACE = new ResourceLocation("minecraft", "textures/gui/container/furnace.png");
    private static final int WIDTH = 150;
    private static final int ARROW_X = 68;
    private static final int ARROW_WIDTH = 24;
    private static final int OUTPUT_X = 104;
    /**
     * The shortest a category may be: HEI stacks an entry's three buttons upwards from its bottom edge, so a
     * shorter one leaves them hanging above the entry they belong to.
     */
    private static final int BUTTON_COLUMN = 13 * 3 + 4;
    private static final int HEIGHT = Math.max(SLOT, BUTTON_COLUMN);
    private static final int SLOT_TOP = (HEIGHT - SLOT) / 2;

    private final String uid;
    private final String titleKey;
    private final int inputs;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable slot;
    private final IDrawable arrow;

    MachineCategory(final IGuiHelper helper, final String name, final String titleKey, final int inputs,
            final ItemStack machine) {
        this.uid = Tags.MOD_ID + "." + name;
        this.titleKey = titleKey;
        this.inputs = inputs;
        this.background = helper.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = helper.createDrawableIngredient(machine);
        this.slot = helper.createDrawable(FURNACE, 55, 16, SLOT, SLOT);
        this.arrow = helper.createDrawable(FURNACE, 79, 35, ARROW_WIDTH, 17);
    }

    /**
     * The inputs sit in a row that ends just before the arrow, so a machine with one input has it where the
     * eye expects it rather than out at the left edge.
     */
    private int inputX(final int input) {
        return ARROW_X - 6 - (this.inputs - input) * SLOT;
    }

    @Override
    public String getUid() {
        return this.uid;
    }

    @Override
    public String getTitle() {
        return I18n.format(this.titleKey);
    }

    @Override
    public String getModName() {
        return Tags.MOD_NAME;
    }

    @Override
    public IDrawable getBackground() {
        return this.background;
    }

    @Nullable
    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void drawExtras(final Minecraft minecraft) {
        for (int input = 0; input < this.inputs; input++) {
            this.slot.draw(minecraft, this.inputX(input), SLOT_TOP);
        }
        this.arrow.draw(minecraft, ARROW_X, SLOT_TOP);
        this.slot.draw(minecraft, OUTPUT_X, SLOT_TOP);
    }

    @Override
    public void setRecipe(final IRecipeLayout layout, final MachineRecipe recipe, final IIngredients ingredients) {
        final IGuiItemStackGroup items = layout.getItemStacks();
        for (int input = 0; input < this.inputs; input++) {
            items.init(input, true, this.inputX(input), SLOT_TOP);
        }
        items.init(this.inputs, false, OUTPUT_X, SLOT_TOP);
        items.set(ingredients);
    }
}
