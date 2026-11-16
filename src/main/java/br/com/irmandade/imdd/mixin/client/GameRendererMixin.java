package br.com.irmandade.imdd.mixin.client;

import br.com.irmandade.imdd.relic.RelicRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Animação do totem: se o jogador segura a Relíquia da Imortalidade, mostra ela (encantada, com brilho). */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @ModifyVariable(method = "showFloatingItem", at = @At("HEAD"), argsOnly = true, require = 0)
    private ItemStack imdd$totemEncantado(ItemStack shown) {
        if (shown == null || !shown.isOf(Items.TOTEM_OF_UNDYING)) return shown;
        PlayerEntity p = MinecraftClient.getInstance().player;
        if (p == null) return shown;
        if (RelicRegistry.is(p.getMainHandStack(), "imortalidade")) return p.getMainHandStack().copy();
        if (RelicRegistry.is(p.getOffHandStack(), "imortalidade")) return p.getOffHandStack().copy();
        return shown;
    }
}
