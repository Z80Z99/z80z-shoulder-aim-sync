package io.github.z80z99.shoulderaimsync.client;

import io.github.z80z99.shoulderaimsync.OSConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 越肩相机状态机：视角模式、左右肩动画、跟随平滑/弹簧、贴墙收缩。
 * 每渲染帧在相机 setup 之后调用一次 step()，把相机位置覆写到肩部锚点。
 */
public final class ViewManager {

    public enum Mode { FIRST, SHOULDER, THIRD_BACK, THIRD_FRONT }

    private static final Vec3 UP = new Vec3(0.0, 1.0, 0.0);
    private static final Vec3 X_AXIS = new Vec3(1.0, 0.0, 0.0);
    /** 单帧位移超过该距离视为传送/重生，直接重置锚点。 */
    private static final double TELEPORT_THRESHOLD_SQR = 4.0;

    public static volatile Mode mode = Mode.FIRST;
    /** +1 = 右肩, -1 = 左肩 */
    public static volatile int side = 1;
    /** 狙击开镜是我们自己请求的第一人称（区别于 TACZ 外部切换） */
    public static volatile boolean ourFp;

    private static volatile double sideAnim = 1.0;
    private static volatile double sideFrom = 1.0;
    private static volatile double sideTo = 1.0;
    private static volatile long slideStart = -1;

    private static Vec3 anchor = null;
    private static Vec3 velocity = Vec3.ZERO;
    /** 平滑后的贴墙裁剪距离；与 anchor 分离，避免“裁剪结果反馈回平滑状态”造成抖动 */
    private static double clipDist = Double.MAX_VALUE;
    private static ClientLevel lastLevel = null;
    private static Vec3 lastPlayerPos = null;
    /** 上一帧是否真正处于越肩覆写状态，用于识别“从外部第一人称/普通第三人称切回” */
    private static boolean wasShoulderActive = false;

    private ViewManager() {
    }

    // ---------------- 模式判定 ----------------

    /** 期望的原版相机类型（越肩在原版层表现第三人称后方） */
    public static CameraType expectedCamType(Mode m) {
        return switch (m) {
            case FIRST -> CameraType.FIRST_PERSON;
            case THIRD_FRONT -> CameraType.THIRD_PERSON_FRONT;
            default -> CameraType.THIRD_PERSON_BACK;
        };
    }

    /** 本帧是否应把相机偏移到肩部 */
    public static boolean shoulderActive(OSConfig.Cfg c, Minecraft mc) {
        if (!c.enabled || mode != Mode.SHOULDER || mc.player == null) return false;
        if (mc.player.isSpectator() || mc.player.isDeadOrDying()) return false;
        return mc.options.getCameraType() == CameraType.THIRD_PERSON_BACK && !ourFp;
    }

    /** TACZ/TACZ-addon 等外部模组自行切到第一人称（例如“瞄准时切换第一人称”） */
    public static boolean externalFp(Minecraft mc) {
        return mode == Mode.SHOULDER && !ourFp && mc.options.getCameraType().isFirstPerson();
    }

    /** 是否有外部模组正在接管第一人称（TACZ-addon 等） */
    public static boolean externalFirstPersonActive(Minecraft mc) {
        return externalFp(mc);
    }

    // ---------------- 模式切换 ----------------

    public static void cycle(Minecraft mc) {
        Mode next = switch (mode) {
            case FIRST -> Mode.SHOULDER;
            case SHOULDER -> Mode.THIRD_BACK;
            case THIRD_BACK -> Mode.THIRD_FRONT;
            case THIRD_FRONT -> Mode.FIRST;
        };
        applyMode(next, mc);
    }

    public static void applyMode(Mode m, Minecraft mc) {
        mode = m;
        ourFp = false;
        anchor = null;
        mc.options.setCameraType(expectedCamType(m));
    }

    /** R 键换肩：0.15 秒平滑横移（保留惯性由跟随引擎负责） */
    public static void flipSide(Minecraft mc) {
        side = -side;
        slideStart = System.nanoTime();
        sideFrom = sideAnim;
        sideTo = side;
        if (mode != Mode.SHOULDER) {
            applyMode(Mode.SHOULDER, mc);
        } else if (externalFirstPersonActive(mc)) {
            // 外部模组（TACZ-addon）正在瞄准并强制第一人称：不改相机类型，
            // 只更新肩侧，瞄准结束后自然切换到新的一侧。
            return;
        }
    }

    public static void syncDefaultSide(OSConfig.Cfg c) {
        if (slideStart < 0) {
            side = c.rightSide ? 1 : -1;
            sideAnim = side;
            sideTo = side;
            sideFrom = side;
        }
    }

    public static void reset() {
        anchor = null;
        velocity = Vec3.ZERO;
        clipDist = Double.MAX_VALUE;
        lastPlayerPos = null;
        wasShoulderActive = false;
    }

    /**
     * 本帧不接管相机（外部第一人称/普通第三人称/旁观）。
     * 必须每帧调用，否则从外部第一人称切回越肩时会沿用冻结的旧锚点。
     */
    public static void markInactive() {
        anchor = null;
        clipDist = Double.MAX_VALUE;
        lastPlayerPos = null;
        wasShoulderActive = false;
    }

    /** 每个 client tick：吸收外部（非本模组）对相机类型的改动 */
    public static void tickSync(OSConfig.Cfg c, Minecraft mc) {
        CameraType ct = mc.options.getCameraType();
        if (mode == Mode.SHOULDER && ct.isFirstPerson() && (ourFp || externalFp(mc))) {
            return; // 合法的偏离：狙击强制开镜或 TACZ/TACZ-addon 自动第一人称
        }
        CameraType expected = expectedCamType(mode);
        if (ct == expected) return;
        if (ct.isFirstPerson()) mode = Mode.FIRST;
        else if (ct == CameraType.THIRD_PERSON_FRONT) mode = Mode.THIRD_FRONT;
        else if (mode != Mode.SHOULDER) mode = Mode.THIRD_BACK;
    }

    // ---------------- 每帧锚点解算 ----------------

    /**
     * 计算并平滑本帧相机锚点。dt 为帧间隔（秒）。
     */
    public static Vec3 step(Minecraft mc, Camera cam, float partialTick, double dt, OSConfig.Cfg c) {
        if (mc.level != lastLevel) {
            lastLevel = mc.level;
            reset();
        }
        Vec3 playerPos = mc.player.getPosition(partialTick);
        // 传送/重生：单帧位移过大直接落位，避免镜头从旧坐标飞过来
        if (lastPlayerPos != null && lastPlayerPos.distanceToSqr(playerPos) > TELEPORT_THRESHOLD_SQR) {
            reset();
        }
        lastPlayerPos = playerPos;

        boolean active = shoulderActive(c, mc);
        if (!active) {
            // 本帧不接管（例如外部第一人称），下一帧重新接管时不要沿用旧锚点
            wasShoulderActive = false;
            anchor = null;
            return cam.getPosition();
        }
        if (!wasShoulderActive) {
            // 从第一人称/普通第三人称切回越肩：直接落到目标点，避免滑镜。
            // 不清 lastPlayerPos，否则下一帧的传送检测会失去参考点。
            anchor = null;
            velocity = Vec3.ZERO;
            wasShoulderActive = true;
        }

        updateSideAnim(c);
        boolean sliding = slideStart >= 0;
        Vec3 target = computeTarget(mc, cam, partialTick, c);
        if (anchor == null) {
            anchor = target;
            velocity = Vec3.ZERO;
        } else if (c.fixed) {
            // 固定跟随：位置不做跟随平滑，但换肩横移仍由 sideAnim 驱动目标点，
            // 因此 R 键换肩照样是平滑移动，只是没有延迟/惯性
            anchor = target;
            velocity = Vec3.ZERO;
        } else if (!c.spring) {
            // 平滑插值：整体指数缓动；换肩期间 X 轴用更短的时间常数保证“秒换”
            double tau = Math.max(0.016, c.transition);
            double tauX = sliding ? Math.max(0.016, c.slide * 0.45) : tau;
            double ax = 1.0 - Math.exp(-dt / tauX);
            double ayz = 1.0 - Math.exp(-dt / tau);
            velocity = Vec3.ZERO;
            anchor = new Vec3(
                    lerp(anchor.x, target.x, ax),
                    lerp(anchor.y, target.y, ayz),
                    lerp(anchor.z, target.z, ayz));
        } else {
            // 弹簧阻尼：k 弹性 + c 阻尼，子步积分防失稳，天然保留惯性
            int sub = Math.max(1, (int) Math.ceil(dt / 0.02));
            double h = dt / sub;
            double k = c.stiffness;
            double cd = Math.min(c.damping, 0.98 / h);
            Vec3 vel = velocity;
            Vec3 pos = anchor;
            for (int i = 0; i < sub; i++) {
                Vec3 err = target.subtract(pos);
                vel = vel.add(err.scale(k * h)).scale(Math.max(0.0, 1.0 - cd * h));
                pos = pos.add(vel.scale(h));
            }
            if (!finite(pos) || !finite(vel)) {
                pos = target;
                vel = Vec3.ZERO;
            }
            anchor = pos;
            velocity = vel;
        }
        return c.avoidWall ? applyWallClip(mc, partialTick, dt, anchor) : anchor;
    }

    private static void updateSideAnim(OSConfig.Cfg c) {
        if (slideStart < 0) return;
        double t = (System.nanoTime() - slideStart) / 1e9 / Math.max(0.02, c.slide);
        if (t >= 1.0) {
            sideAnim = sideTo;
            slideStart = -1;
        } else {
            sideAnim = lerp(sideFrom, sideTo, easeInOut(clamp01(t)));
        }
    }

    /** 肩部目标锚点（世界坐标） */
    private static Vec3 computeTarget(Minecraft mc, Camera cam, float partialTick, OSConfig.Cfg c) {
        Vec3 eye = mc.player.getEyePosition(partialTick);
        org.joml.Vector3f lf = cam.getLookVector();
        Vec3 look = new Vec3(lf.x, lf.y, lf.z);
        double push = c.push ? AimTracker.easedLevel() * c.pushDist : 0.0;
        double back = Math.max(0.2, c.back - push);
        Vec3 right = horizontalRight(cam, look);
        return eye.add(look.scale(-back))
                .add(right.scale(c.offX * sideAnim))
                .add(0.0, c.offY, 0.0);
    }

    /** 水平向右单位向量：look × up；直视上下时用相机左向量展平 */
    private static Vec3 horizontalRight(Camera cam, Vec3 look) {
        Vec3 right = look.cross(UP);
        if (right.lengthSqr() < 1.0E-4) {
            org.joml.Vector3f lf = cam.getLeftVector();
            Vec3 flat = new Vec3(-lf.x, 0.0, -lf.z);
            double len = flat.length();
            return len < 1.0E-4 ? X_AXIS : flat.scale(1.0 / len);
        }
        return right.normalize();
    }

    /**
     * 贴墙收缩：对“眼→期望锚点”做射线检测，把相机拉回墙内。
     *
     * 关键点（抖动修复）：
     * 1. 裁剪距离单独平滑，且不回写 anchor——否则裁剪结果会反馈进跟随状态，
     *    下一帧又从墙边往墙里插值，形成每帧来回弹的抖动环。
     * 2. 非对称平滑：贴近墙时快速收缩（0.02s，避免穿模），离开墙时缓慢恢复（0.35s，避免弹跳）。
     */
    private static Vec3 applyWallClip(Minecraft mc, float partialTick, double dt, Vec3 desired) {
        if (mc.level == null || mc.player == null) return desired;
        Vec3 eye = mc.player.getEyePosition(partialTick);
        Vec3 dir = desired.subtract(eye);
        double len = dir.length();
        if (len < 1.0E-4) return desired;

        HitResult hit = mc.level.clip(new ClipContext(eye, desired,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
        double hitLen = hit.getType() == HitResult.Type.MISS ? len : hit.getLocation().distanceTo(eye);
        double targetDist = Math.min(len, Math.max(0.08, hitLen - 0.125));

        if (clipDist == Double.MAX_VALUE) {
            clipDist = targetDist;
        } else {
            double tau = targetDist < clipDist ? 0.02 : 0.35;
            double a = 1.0 - Math.exp(-dt / Math.max(0.016, tau));
            clipDist = lerp(clipDist, targetDist, a);
        }
        clipDist = Math.max(0.05, Math.min(clipDist, len));
        return eye.add(dir.scale(clipDist / len));
    }

    // ---------------- 小工具 ----------------

    private static double clamp01(double v) {
        return v < 0 ? 0 : Math.min(1, v);
    }

    private static double easeInOut(double t) {
        return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static boolean finite(Vec3 v) {
        return Double.isFinite(v.x) && Double.isFinite(v.y) && Double.isFinite(v.z);
    }
}
