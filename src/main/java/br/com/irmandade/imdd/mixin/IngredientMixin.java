package br.com.irmandade.imdd.mixin;

import br.com.irmandade.imdd.relic.RelicRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Relíquias não servem como ingrediente de receitas normais (só a receita especial do machado do Bizonho as usa). */
@Mixin(Ingredient.class)
public abstract class IngredientMixin {

    @Inject(method = "test(Lnet/minecraft/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true, require = 0)
    private void imdd$semRelicias(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (stack != null && RelicRegistry.isRelic(stack)) cir.setReturnValue(false);
    }
}
