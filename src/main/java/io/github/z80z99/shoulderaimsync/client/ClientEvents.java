package io.github.z80z99.shoulderaimsync.client;

import io.github.z80z99.shoulderaimsync.OSConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Forge 事件总线监听器（游戏运行期事件）。
 * Mod 总线事件（注册键位/覆盖层、配置加载等）在 {@link ModBusEvents}。
 */
public class ClientEvents {

    private long lastFrameNano = System.nanoTime();
    private boolean autoApplied;
    /** 平滑后的鞘翅滚转角（度） */
    private float elytraRoll;

    // ---------------- forge bus ----------------

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        Minecraft mc = Minecraft.getInstance();
        OSConfig.Cfg c = OSConfig.CACHE;
        if (mc.player == null || mc.level == null) {
            autoApplied = false;
            return;
        }
        if (!autoApplied) {
            autoApplied = true;
            ViewManager.syncDefaultSide(c);
            if (c.enabled && c.autoShoulder && c.takeover && !mc.player.isSpectator()) {
                ViewManager.applyMode(ViewManager.Mode.SHOULDER, mc);
            }
        }
        // F5 接管：在原版 handleKeybinds 之前消费掉点击。
        // 原版是 if(consumeClick())，只消费一次；我们循环把所有点击都吃掉，
        // 否则连点时剩余次数会被原版接管并直接切相机，打乱状态机。
        if (mc.screen == null) {
            if (c.enabled && c.takeover) {
                while (mc.options.keyTogglePerspective.consumeClick()) {
                    ViewManager.cycle(mc);
                }
            }
        } else {
            while (mc.options.keyTogglePerspective.consumeClick()) { /* 界面期间丢弃 */ }
        }
        if (mc.screen == null) {
            while (KeyBindings.TOGGLE_SHOULDER.consumeClick()) {
                if (c.enabled) ViewManager.flipSide(mc);
            }
        } else {
            // 界面打开时丢弃按键计数，避免关闭瞬间误触发
            while (KeyBindings.TOGGLE_SHOULDER.consumeClick()) { /* noop */ }
        }
        while (KeyBindings.SETTINGS.consumeClick()) {
            if (mc.screen instanceof SettingsScreen) {
                mc.setScreen(null);
            } else if (mc.screen == null) {
                mc.setScreen(new SettingsScreen());
            }
        }
        ViewManager.tickSync(c, mc);
    }

    /** 重生/换维度会克隆玩家，旧锚点作废 */
    @SubscribeEvent
    public void onPlayerClone(ClientPlayerNetworkEvent.Clone event) {
        ViewManager.reset();
        AimTracker.reset();
        GunDetect.invalidate();
    }

    @SubscribeEvent
    public void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        ViewManager.reset();
        AimTracker.reset();
        GunDetect.invalidate();
    }

    @SubscribeEvent
    public void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ViewManager.reset();
        AimTracker.reset();
    }

    /**
     * 每帧捕获“投影 × 视图旋转”矩阵，供准星把弹道目标点投影到屏幕坐标。
     * AFTER_LEVEL 的投影矩阵（matrix4f）已包含 bobHurt/bobView 等视角晃动，
     * 与地形实际渲染一致；视图旋转取 RenderSystem 里本帧设置的逆视图旋转矩阵（转置即为正）。
     */
    @SubscribeEvent
    public void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        Matrix4f projection = event.getProjectionMatrix();
        Matrix3f invRot = RenderSystem.getInverseViewRotationMatrix();
        if (projection == null || invRot == null) return;
        Matrix4f viewRot = new Matrix4f().set(new Matrix3f(invRot).transpose());
        Camera cam = event.getCamera();
        ShoulderCrosshair.captureFrame(
                new Matrix4f(projection).mul(viewRot),
                cam != null ? cam.getPosition() : Minecraft.getInstance().gameRenderer.getMainCamera().getPosition());
    }

    /** 记录本帧最终相机角度（最低优先级，保证是其他模组改写后的值），仅用于调试显示 */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onCameraAnglesLast(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || event.getCamera().getEntity() != mc.player) return;
        ShoulderCrosshair.captureDebugAngles(event.getYaw(), event.getPitch(), event.getRoll());
        applyElytraRoll(event, mc.player);
    }

    /**
     * 鞘翅滑翔时按“视线水平方向 × 水平速度方向”的叉积给画面加滚转，
     * 转弯时身体倾斜、画面随之倾斜。用 LOWEST 优先级保证在 TACZ 等改写之后叠加。
     */
    private void applyElytraRoll(ViewportEvent.ComputeCameraAngles event, net.minecraft.client.player.LocalPlayer player) {
        OSConfig.Cfg c = OSConfig.CACHE;
        if (!c.enabled || !c.elytraRoll || !player.isFallFlying() || c.elytraRollStrength <= 0) {
            elytraRoll = 0.0f;
            return;
        }
        float pt = (float) event.getPartialTick();
        Vec3 look = player.getViewVector(pt);
        Vec3 motion = player.getDeltaMovement();
        Vec3 lookFlat = new Vec3(look.x, 0.0, look.z);
        Vec3 moveFlat = new Vec3(motion.x, 0.0, motion.z);
        if (lookFlat.lengthSqr() < 1.0E-6 || moveFlat.lengthSqr() < 1.0E-6) {
            elytraRoll = 0.0f;
            return;
        }
        double crossY = lookFlat.normalize().cross(moveFlat.normalize()).y;
        float target = (float) (crossY * c.elytraRollStrength);
        // 每帧平滑，避免剧烈抖动
        elytraRoll += (target - elytraRoll) * Math.min(1.0f, 0.25f + pt * 0.1f);
        event.setRoll(event.getRoll() + elytraRoll);
    }

    /** 记录本帧最终 FOV（最低优先级），仅用于调试显示 */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onComputeFovLast(ViewportEvent.ComputeFov event) {
        if (!event.usedConfiguredFov()) return;
        ShoulderCrosshair.captureDebugFov((float) event.getFOV());
    }

    /**
     * 相机位置覆写点：在 vanilla Camera.setup 之后、投影/裁剪之前。
     */
    @SubscribeEvent
    public void onCameraSetup(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        long now = System.nanoTime();
        double dt = Math.min(0.125, Math.max(0.0, (now - lastFrameNano) / 1e9));
        lastFrameNano = now;

        if (mc.player == null || mc.level == null) {
            AimTracker.reset();
            return;
        }
        OSConfig.Cfg c = OSConfig.CACHE;
        AimTracker.frame(dt, (float) event.getPartialTick(), c);

        if (!ViewManager.shoulderActive(c, mc)) {
            // 不接管（含 TACZ-addon 强制第一人称）：清掉锚点，避免切回时镜头滑动
            ViewManager.markInactive();
            return;
        }
        if (event.getCamera().getEntity() != mc.player) return;

        Vec3 pos = ViewManager.step(mc, event.getCamera(), (float) event.getPartialTick(), dt, c);
        CameraBridge.setPosition(event.getCamera(), pos);
    }

    /**
     * 瞄准渐进变焦：FOV 按配置比例收窄（默认 0.3 秒收到 70%）。
     * 狙击枪走“强制第一人称”路线时不参与变焦，避免与瞄具叠加。
     * 装了倍镜时默认交给 TACZ 自己的镜片倍率，避免两层缩放相乘。
     */
    @SubscribeEvent
    public void onComputeFov(ViewportEvent.ComputeFov event) {
        if (!event.usedConfiguredFov()) return;
        OSConfig.Cfg c = OSConfig.CACHE;
        if (!c.enabled || !c.zoom || AimTracker.level <= 0.001) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (AimTracker.sniper && c.forceFpSniper) return;
        boolean shoulder = ViewManager.shoulderActive(c, mc);
        boolean fp = mc.options.getCameraType() == CameraType.FIRST_PERSON;
        if (!shoulder && !(fp && c.zoomFp)) return;
        if (c.deferToTacz && hasScope(mc.player.getMainHandItem())) return;
        double factor = 1.0 - (1.0 - c.zoomFactor) * AimTracker.easedLevel();
        event.setFOV(event.getFOV() * factor);
    }

    /** 当前枪械是否装了倍镜（瞄具倍率 > 1） */
    private static boolean hasScope(ItemStack stack) {
        return !stack.isEmpty() && TaczCompat.aimingZoom(stack) > 1.01;
    }
}
