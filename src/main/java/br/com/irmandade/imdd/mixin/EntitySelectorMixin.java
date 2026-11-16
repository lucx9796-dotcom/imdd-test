package br.com.irmandade.imdd.mixin;

import br.com.irmandade.imdd.vanish.VanishState;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Seletores (/tell nome, @a ...): quem não é op não encontra jogadores desaparecidos.
 * Alvos: EntitySelector (class_2300): method_9813 = jogadores, method_9816 = entidades.
 */
@Pseudo
@Mixin(targets = "net.minecraft.class_2300")
public abstract class EntitySelectorMixin {

    @Inject(method = "method_9813", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private void imdd$jogadores(ServerCommandSource source, CallbackInfoReturnable<List<ServerPlayerEntity>> cir) {
        cir.setReturnValue(VanishState.filterPlayers(source, cir.getReturnValue()));
    }

    @Inject(method = "method_9816", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private void imdd$entidades(ServerCommandSource source, CallbackInfoReturnable<List<? extends Entity>> cir) {
        cir.setReturnValue(VanishState.filterPlayers(source, cir.getReturnValue()));
    }
}
