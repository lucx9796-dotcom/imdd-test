package br.com.irmandade.imdd.mixin;

import br.com.irmandade.imdd.vanish.VanishState;
import net.minecraft.server.command.ServerCommandSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

/** Sugestões de nomes de jogadores (ServerCommandSource.getPlayerNames = method_9262): esconde os desaparecidos de quem não é op. */
@Pseudo
@Mixin(targets = "net.minecraft.class_2168")
public abstract class ServerCommandSourceMixin {

    @Inject(method = "method_9262", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private void imdd$esconderNomes(CallbackInfoReturnable<Collection<String>> cir) {
        cir.setReturnValue(VanishState.filterNames((ServerCommandSource) (Object) this, cir.getReturnValue()));
    }
}
