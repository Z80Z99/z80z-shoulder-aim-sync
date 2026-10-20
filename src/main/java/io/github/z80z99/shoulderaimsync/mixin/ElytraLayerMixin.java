package io.github.z80z99.shoulderaimsync.mixin;

import io.github.z80z99.shoulderaimsync.client.PlayerFade;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 鞘翅跟着淡出：淡出期间把 armorCutoutNoCull 换成半透明版本。
 */
@Mixin(ElytraLayer.class)
public class ElytraLayerMixin {

    @Redirect(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/RenderType;armorCutoutNoCull(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;"))
    private RenderType shoulder$translucentElytra(ResourceLocation texture) {
        if (PlayerFade.inModelScope()) {
            return RenderType.entityTranslucentCull(texture);
        }
        return RenderType.armorCutoutNoCull(texture);
    }
}
