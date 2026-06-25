/**
 * @file FoxMixin.java
 *
 * @version 1.0.0
 * @author Bleckwolf25
 * @license MIT
 *
 * @summary Mixin for foxes to restrict what items they can hold.
 *
 * @description
 * Injects a check into the Fox entity's canHoldItem method to only allow holding items
 * defined within the mod's custom fox_holdable item tag.
 *
 * @since 25/06/2026
 * @updated 25/06/2026
 */
// ---------- PACKAGE
package com.aetasferrea.aetasferreamod.mixin;

// ---------- IMPORTS
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import java.util.Objects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.aetasferrea.aetasferreamod.AetasFerreaMod;

// ---------- CLASS: FOXMIXIN
@Mixin(Fox.class)
public class FoxMixin {

    // ---------- CONSTANTS
    private static final TagKey<Item> FOX_HOLDABLE = TagKey.create(Objects.requireNonNull(Registries.ITEM), Objects.requireNonNull(ResourceLocation.fromNamespaceAndPath(AetasFerreaMod.MODID, "fox_holdable")));

    // ---------- ITEM HOLDING RESTRICTION
    @Inject(method = "canHoldItem", at = @At("HEAD"), cancellable = true)
    private void restrictHeldItems(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        // Allow holding if the item is designated in the custom tag
        if (stack.is(Objects.requireNonNull(FOX_HOLDABLE))) {
            return;
        }

        // Cancel holding behavior for all other items
        cir.setReturnValue(false);
    }
}
