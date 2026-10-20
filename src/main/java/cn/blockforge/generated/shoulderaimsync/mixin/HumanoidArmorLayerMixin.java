package cn.blockforge.generated.shoulderaimsync.mixin;

import cn.blockforge.generated.shoulderaimsync.client.PlayerFade;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 盔甲层跟着淡出：淡出期间把 armorCutoutNoCull 换成半透明版本。
 */
@Mixin(HumanoidArmorLayer.class)
public class HumanoidArmorLayerMixin {

    @Redirect(
            method = "renderModel(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/item/ArmorItem;Lnet/minecraft/client/model/HumanoidModel;ZFFFLjava/lang/String;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/RenderType;armorCutoutNoCull(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;"))
    private RenderType shoulder$translucentArmor(ResourceLocation texture) {
        if (PlayerFade.inModelScope()) {
            return RenderType.entityTranslucentCull(texture);
        }
        return RenderType.armorCutoutNoCull(texture);
    }
}
