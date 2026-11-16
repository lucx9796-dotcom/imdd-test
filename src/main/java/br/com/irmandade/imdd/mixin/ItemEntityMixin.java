package br.com.irmandade.imdd.mixin;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.DamageTypeTags;
import br.com.irmandade.imdd.expurgo.Expurgo;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Item com unbreakable={} no chão: não toma dano (explosão, fogo, lava, cacto...) e não some por tempo. */
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void imdd$indestrutivel(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (((ItemEntity) (Object) this).getStack().contains(DataComponentTypes.UNBREAKABLE)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void imdd$semDespawn(CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        if (self.getStack().contains(DataComponentTypes.UNBREAKABLE)) {
            self.setNeverDespawn();
        }
    }

    /** Nether star destruída pelo fogo/lava => Dia do Expurgo. */
    @Inject(method = "damage", at = @At("RETURN"))
    private void imdd$expurgo(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        ItemEntity self = (ItemEntity) (Object) this;
        if (self.isRemoved() && self.getStack().isOf(Items.NETHER_STAR) && source.isIn(DamageTypeTags.IS_FIRE)) {
            Expurgo.onNetherStarBurned(world, new net.minecraft.util.math.Vec3d(self.getX(), self.getY(), self.getZ()), self.getStack().getCount());
        }
    }
}
