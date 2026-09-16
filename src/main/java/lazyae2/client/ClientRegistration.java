/*
 * Copyright (c) 2020 E. Geng
 * Copyright (c) 2026 Lazy AE2 UD contributors
 *
 * MIT with the "Good, not Evil" clause; see LICENSE.md.
 */

package lazyae2.client;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

import lazyae2.Tags;
import lazyae2.block.BlockAssembler;
import lazyae2.block.BlockMachine;
import lazyae2.core.Registration;
import lazyae2.item.ItemMaterial;
import appeng.api.util.AEColor;
import appeng.client.render.StaticItemColor;

@Mod.EventBusSubscriber(modid = Tags.MOD_ID, value = Side.CLIENT)
public final class ClientRegistration {

    private ClientRegistration() {
    }

    /**
     * The terminal is drawn in four shades of one colour, as every AE2 terminal is, and the shades are tints
     * rather than pixels - so without this the whole screen comes out flat white.
     */
    @SubscribeEvent
    public static void registerItemColors(final ColorHandlerEvent.Item event) {
        if (Registration.terminal != null) {
            event.getItemColors().registerItemColorHandler(new StaticItemColor(AEColor.TRANSPARENT),
                    Registration.terminal);
        }
    }

    @SubscribeEvent
    public static void registerModels(final ModelRegistryEvent event) {
        if (Registration.material != null) {
            for (final ItemMaterial.Type type : ItemMaterial.Type.all()) {
                ModelLoader.setCustomModelResourceLocation(Registration.material, type.ordinal(),
                        new ModelResourceLocation(new ResourceLocation(Tags.MOD_ID, "material/" + type.getName()), "inventory"));
            }
        }

        if (Registration.terminal != null) {
            ModelLoader.setCustomModelResourceLocation(Registration.terminal, 0, new ModelResourceLocation(
                    new ResourceLocation(Tags.MOD_ID, "part/" + Registration.TERMINAL), "inventory"));
        }

        if (Registration.assembler != null) {
            final Item item = Item.getItemFromBlock(Registration.assembler);
            for (final BlockAssembler.Type type : BlockAssembler.Type.all()) {
                ModelLoader.setCustomModelResourceLocation(item, type.getMeta(), new ModelResourceLocation(
                        new ResourceLocation(Tags.MOD_ID, Registration.ASSEMBLER + "_" + type.getName()), "inventory"));
            }
        }

        if (Registration.machine != null) {
            final Item item = Item.getItemFromBlock(Registration.machine);
            for (final BlockMachine.Type type : BlockMachine.Type.all()) {
                ModelLoader.setCustomModelResourceLocation(item, type.getMeta(),
                        new ModelResourceLocation(new ResourceLocation(Tags.MOD_ID, "machine_" + type.getName()), "inventory"));
            }
        }
    }
}
