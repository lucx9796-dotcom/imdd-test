package br.com.irmandade.imdd.mixin.client;

import br.com.irmandade.imdd.client.ArmorHider;
import br.com.irmandade.imdd.relic.RelicRegistry;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.EquipmentSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Relíquia da Invisibilidade: esconde a armadura do jogador (só elytra, cabeças de mob/player e abóbora continuam). */
@Mixin(PlayerEntityRenderer.class)
public abstract class LivingEntityRendererMixin {

    @Inject(method = "updateRenderState(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V",
            at = @At("TAIL"), require = 0)
    private void imdd$esconderArmadura(AbstractClientPlayerEntity player, PlayerEntityRenderState state, float tickProgress, CallbackInfo ci) {
        if (RelicRegistry.is(player.getEquippedStack(EquipmentSlot.FEET), "invisibilidade")) {
            ArmorHider.hide(state);
        }
    }
}
