package com.github.z80z.shoulderaimsync.mixin;

import com.github.z80z.shoulderaimsync.client.PlayerFade;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 盔甲层跟着淡出：淡出期间把 armorCutoutNoCull 换成半透明版本。
 *
 * 注意：Forge 1.20.1 的补丁在 HumanoidArmorLayer 里新增了一个可读名的
 * {@code renderModel(..., Model, ..., ResourceLocation)}，实际调用的是它，
 * 而不是原版的 {@code m_289609_(..., A, ..., String)}。因此这里：
 * - method 用 Forge 新增方法的完整描述符，并以 {@code remap = false} 按名匹配；
 * - @At 的 target 仍需要重映射（RenderType.armorCutoutNoCull → m_110431_）；
 * - require = 0：万一未来 Forge 再次改签名，只跳过本功能，不让游戏崩溃。
 */
@Mixin(HumanoidArmorLayer.class)
public class HumanoidArmorLayerMixin {

    @Redirect(
            method = "renderModel(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/item/ArmorItem;Lnet/minecraft/client/model/Model;ZFFFLnet/minecraft/resources/ResourceLocation;)V",
            remap = false,
            require = 0,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/RenderType;armorCutoutNoCull(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;",
                    remap = true))
    private RenderType shoulder$translucentArmor(ResourceLocation texture) {
        if (PlayerFade.inModelScope()) {
            return RenderType.entityTranslucentCull(texture);
        }
        return RenderType.armorCutoutNoCull(texture);
    }
}
