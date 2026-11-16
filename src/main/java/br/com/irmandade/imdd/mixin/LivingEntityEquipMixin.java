package br.com.irmandade.imdd.mixin;

import br.com.irmandade.imdd.client.ArmorHider;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Relíquia da Invisibilidade: no cliente a armadura "some" para o renderizador (próprio jogador e os outros). */
@Mixin(LivingEntity.class)
public abstract class LivingEntityEquipMixin {

    @Inject(method = "getEquippedStack", at = @At("RETURN"), cancellable = true, require = 0)
    private void imdd$esconderArmadura(EquipmentSlot slot, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack original = cir.getReturnValue();
        ItemStack result = ArmorHider.filter((LivingEntity) (Object) this, slot, original);
        if (result != original) cir.setReturnValue(result);
    }
}
