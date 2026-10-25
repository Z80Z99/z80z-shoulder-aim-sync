package com.github.z80z.shoulderaimsync.mixin;

import com.github.z80z.shoulderaimsync.client.PlayerFade;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 贴墙虚化的主体注入：
 * - render 头尾维护“正在渲染淡出模型”的作用域，供 ModelPart 与图层判断；
 * - getRenderType 在淡出时改用半透明渲染类型，让身体纹理支持 alpha 混合。
 *
 * 全部 require = 0：注入失配时只跳过虚化，不让游戏崩溃。
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {

    @Inject(
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"),
            require = 0)
    private void shoulder$renderHead(LivingEntity entity, float yaw, float partialTick,
                                     PoseStack poseStack, MultiBufferSource buffers, int packedLight,
                                     CallbackInfo ci) {
        PlayerFade.enterModelScope(entity);
    }

    @Inject(
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("RETURN"),
            require = 0)
    private void shoulder$renderReturn(LivingEntity entity, float yaw, float partialTick,
                                       PoseStack poseStack, MultiBufferSource buffers, int packedLight,
                                       CallbackInfo ci) {
        PlayerFade.exitModelScope();
    }

    @Inject(
            method = "getRenderType(Lnet/minecraft/world/entity/LivingEntity;ZZZ)Lnet/minecraft/client/renderer/RenderType;",
            at = @At("HEAD"),
            cancellable = true,
            require = 0)
    private void shoulder$translucentWhenFading(LivingEntity entity, boolean bodyVisible,
                                                boolean translucent, boolean glowing,
                                                CallbackInfoReturnable<RenderType> cir) {
        if (!PlayerFade.isFading(entity)) return;
        @SuppressWarnings("unchecked")
        EntityRenderer<LivingEntity> self = (EntityRenderer<LivingEntity>) (Object) this;
        ResourceLocation texture = self.getTextureLocation(entity);
        cir.setReturnValue(RenderType.entityTranslucentCull(texture));
    }
}
