package cn.blockforge.generated.shoulderaimsync.client;

import cn.blockforge.generated.shoulderaimsync.OSConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 贴墙虚化：镜头被墙压得离玩家很近时把本地玩家渲染成半透明，
 * 而不是让镜头钻进人物模型里。
 *
 * 这里只负责“算透明度”和“极近距离直接隐藏”；实际的半透明渲染由
 * {@code mixin} 包下的 4 个 Mixin 完成（身体、盔甲、鞘翅一起淡出）。
 */
public final class PlayerFade {

    private static final Logger LOGGER = LogManager.getLogger("z80z_shoulder_aim_sync");

    /** 相机距眼睛 ≥ 该值：完全不虚化（正常越肩约 1.28 格，不受影响） */
    private static final double FADE_FAR = 0.9;
    /** 相机距眼睛 ≤ 该值：最透明 */
    private static final double FADE_NEAR = 0.45;
    /** 更近时直接不画身体，避免镜头贴脸 */
    public static final double HIDE_DIST = 0.22;
    /** 最透明时的 alpha */
    private static final float MIN_ALPHA = 0.18f;

    /** 本帧淡出系数（1 = 不透明）；Mixin 每帧读取 */
    private static volatile float alpha = 1.0f;
    /** 本帧是否处于淡出（含完全隐藏前的最透明状态） */
    private static volatile boolean fading;
    /** 当前是否正在渲染“需要淡出的那个模型”（供 ModelPart / 盔甲层判断） */
    private static volatile boolean inModelScope;

    private PlayerFade() {
    }

    /** 供模块入口注册用 */
    public static PlayerFade create() {
        return new PlayerFade();
    }

    // ---------------- 供 Mixin 查询 ----------------

    /** 本地玩家模型应使用的 alpha 系数（0..1）。 */
    public static float alphaFor(LivingEntity entity) {
        if (!fading) return 1.0f;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || entity != mc.player) return 1.0f;
        return alpha;
    }

    /** 本地玩家当前是否正在淡出（Mixin 用它决定是否切换半透明 RenderType）。 */
    public static boolean isFading(LivingEntity entity) {
        if (!fading) return false;
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && entity == mc.player;
    }

    /** 进入/退出“正在渲染淡出模型”的作用域（由 LivingEntityRendererMixin 调用）。 */
    public static void enterModelScope(LivingEntity entity) {
        inModelScope = isFading(entity);
    }

    public static void exitModelScope() {
        inModelScope = false;
    }

    /** 作用域内：把顶点 alpha 乘以淡出系数。 */
    public static float scopeAlpha(float alphaIn) {
        return inModelScope ? alphaIn * alpha : alphaIn;
    }

    /** 作用域内：盔甲/鞘翅等图层应改用半透明渲染类型。 */
    public static boolean inModelScope() {
        return inModelScope;
    }

    /** 当前淡出系数（Mixin 乘算用）。 */
    public static float currentAlpha() {
        return alpha;
    }

    // ---------------- 每帧计算 ----------------

    @SubscribeEvent
    public void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        fading = false;
        alpha = 1.0f;

        Minecraft mc = Minecraft.getInstance();
        OSConfig.Cfg c = OSConfig.CACHE;
        LocalPlayer player = mc.player;
        if (!c.enabled || !c.fadePlayer || player == null) return;
        if (event.getEntity() != player || mc.level == null) return;
        // 打开界面时（如背包里的玩家预览）不参与淡出
        if (mc.screen != null) return;
        // 越肩或视角切换动画期间参与淡入淡出：
        // 切换动画的镜头会从眼睛出发/穿过头部，正好复用距离淡出规则
        boolean switching = ViewManager.isSwitchAnimating();
        if (!ViewManager.shoulderActive(c, mc) && !switching) return;

        double dist = mc.gameRenderer.getMainCamera().getPosition()
                .distanceTo(player.getEyePosition(event.getPartialTick()));
        if (dist >= FADE_FAR) return;

        // 切换动画期间不整体隐藏（避免“隐藏→可见”跳变），只用连续 alpha
        if (!switching && dist <= HIDE_DIST) {
            // 镜头几乎贴在头上：本帧整个玩家不画
            event.setCanceled(true);
            return;
        }

        float a = (float) (MIN_ALPHA
                + (1.0 - MIN_ALPHA) * (dist - FADE_NEAR) / (FADE_FAR - FADE_NEAR));
        alpha = Math.max(MIN_ALPHA, Math.min(1.0f, a));
        fading = true;
    }

    @SubscribeEvent
    public void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        fading = false;
        alpha = 1.0f;
    }

    static {
        // 仅用于诊断：确认该类被加载（日志排查用）
        LOGGER.debug("[z80z_shoulder_aim_sync] PlayerFade 已加载（mixin 渲染路径）");
    }
}
