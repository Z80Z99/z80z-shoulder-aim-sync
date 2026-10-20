package cn.blockforge.generated.shoulderaimsync.mixin;

import cn.blockforge.generated.shoulderaimsync.client.PlayerFade;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * 把模型顶点的 alpha 乘以淡出系数。
 * 注入点：ModelPart.render(...8 个参数...) 调用 compile 时的第 8 个参数（alpha）。
 */
@Mixin(ModelPart.class)
public class ModelPartMixin {

    @ModifyArg(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/model/geom/ModelPart;compile(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"),
            index = 7,
            require = 0)
    private float shoulder$fadeAlpha(float alpha) {
        return PlayerFade.scopeAlpha(alpha);
    }
}
