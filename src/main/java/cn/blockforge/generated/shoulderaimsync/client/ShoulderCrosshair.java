package cn.blockforge.generated.shoulderaimsync.client;

import cn.blockforge.generated.shoulderaimsync.OSConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.Locale;

/**
 * 越肩准星：画在“弹道命中点”的屏幕投影位置，而不是屏幕中心。
 *
 * 越肩相机横移后屏幕中心对应的是相机平行射线，与从眼睛发出的弹道错开，
 * 近距离偏差明显。这里把实时射线的命中点用本帧真实投影矩阵投到 GUI 坐标。
 *
 * 注意：不能依赖原版 hitResult——它的方块检测距离只有 4.5 格，超出即是 MISS，
 * 而枪械弹道可达上百格。
 */
public final class ShoulderCrosshair {

    /** 射线最大距离（格），与枪械实际射程同量级 */
    private static final double RAY_DIST = 128.0;
    /** TACZ 子弹出生点比眼睛高 0.1 格 */
    private static final double TACZ_MUZZLE_LIFT = 0.1;
    private static final int MARGIN = 4;

    /** 本帧真实“投影 × 视图旋转”矩阵（AFTER_LEVEL 捕获，含视角晃动） */
    private static volatile Matrix4f mvp;
    private static volatile Vec3 camPos = Vec3.ZERO;

    /** 以下仅用于调试显示 */
    private static volatile float dbgYaw;
    private static volatile float dbgPitch;
    private static volatile float dbgRoll;
    private static volatile float dbgFov = -1.0f;

    private ShoulderCrosshair() {
    }

    static void captureFrame(Matrix4f matrix, Vec3 cameraPos) {
        mvp = matrix;
        camPos = cameraPos;
    }

    static void captureDebugAngles(float yaw, float pitch, float roll) {
        dbgYaw = yaw;
        dbgPitch = pitch;
        dbgRoll = roll;
    }

    static void captureDebugFov(float fovDeg) {
        dbgFov = fovDeg;
    }

    public static void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        OSConfig.Cfg c = OSConfig.CACHE;
        if (!c.enabled || !c.crosshair) return;
        if (mc.player == null || mc.level == null) return;
        if (mc.screen != null || mc.options.hideGui) return;
        if (mc.player.isSpectator() || mc.player.isDeadOrDying()) return;
        if (!ViewManager.shoulderActive(c, mc)) return;
        if (ScopeFade.isActive()) return;
        if (!GunDetect.isGun(mc.player.getMainHandItem(), c)) return;

        // 弹道原点：TACZ 子弹在眼睛高度 +0.1 处生成（X/Z 与眼睛相同）
        Vec3 eye = mc.player.getEyePosition(partialTick);
        Vec3 origin = GunDetect.usesTaczMuzzle(mc.player.getMainHandItem())
                ? eye.add(0.0, TACZ_MUZZLE_LIFT, 0.0)
                : eye;
        Vec3 dir = mc.player.getViewVector(partialTick);
        RayHit ray = castRay(mc, origin, dir);
        Vec3 target = ray.point();

        Matrix4f m = mvp;
        if (m == null) return;
        Vec3 rel = target.subtract(camPos);
        Vector4f clip = m.transform(new Vector4f((float) rel.x, (float) rel.y, (float) rel.z, 1.0f));
        if (clip.w <= 0.05f) return; // 目标在相机后方

        float ndcX = clip.x / clip.w;
        float ndcY = clip.y / clip.w;
        int cx = Math.max(MARGIN, Math.min(width - MARGIN - 1,
                Math.round((0.5f + 0.5f * ndcX) * width)));
        int cy = Math.max(MARGIN, Math.min(height - MARGIN - 1,
                Math.round((0.5f - 0.5f * ndcY) * height)));

        drawCross(graphics, cx, cy, 0xD8FFFFFF, 0x80FFFFFF);

        if (c.debugCrosshair) {
            drawDebug(mc, graphics, width, height, ndcX, ndcY, ray, origin, dir, target);
        }
    }

    // ---------------- 绘制 ----------------

    private static void drawCross(GuiGraphics g, int cx, int cy, int color, int centerColor) {
        int gap = 3;
        int len = 5;
        g.fill(cx - gap - len, cy, cx - gap, cy + 1, color);
        g.fill(cx + gap, cy, cx + gap + len, cy + 1, color);
        g.fill(cx, cy - gap - len, cx + 1, cy - gap, color);
        g.fill(cx, cy + gap, cx + 1, cy + gap + len, color);
        g.fill(cx, cy, cx + 1, cy + 1, centerColor);
    }

    private static void drawDebug(Minecraft mc, GuiGraphics g, int width, int height,
                                  float ndcX, float ndcY, RayHit ray,
                                  Vec3 origin, Vec3 dir, Vec3 target) {
        // 屏幕几何中心红叉：用于直观对比“屏幕中心”与“实际弹道投影点”
        int sx = width / 2;
        int sy = height / 2;
        int red = 0xFFFF3030;
        g.fill(sx - 8, sy, sx + 8, sy + 1, red);
        g.fill(sx, sy - 8, sx + 1, sy + 8, red);

        String[] lines = {
                String.format(Locale.ROOT, "fov=%.2f  aspect=%.3f", dbgFov,
                        (float) mc.getWindow().getWidth() / Math.max(1, mc.getWindow().getHeight())),
                String.format(Locale.ROOT, "yaw=%.2f pitch=%.2f roll=%.2f", dbgYaw, dbgPitch, dbgRoll),
                String.format(Locale.ROOT, "ndc=(%.4f, %.4f)", ndcX, ndcY),
                String.format(Locale.ROOT, "hit=%s dist=%.2f", ray.type(), ray.distance()),
                String.format(Locale.ROOT, "cam=(%.2f, %.2f, %.2f)", camPos.x, camPos.y, camPos.z),
                String.format(Locale.ROOT, "eye=(%.2f, %.2f, %.2f)", origin.x, origin.y, origin.z),
                String.format(Locale.ROOT, "dir=(%.3f, %.3f, %.3f)", dir.x, dir.y, dir.z),
                String.format(Locale.ROOT, "target=(%.2f, %.2f, %.2f)", target.x, target.y, target.z),
        };
        int y = 6;
        for (String line : lines) {
            g.drawString(mc.font, line, 6, y, 0xFFFF80, false);
            y += 10;
        }
    }

    // ---------------- 实时射线 ----------------

    private record RayHit(Vec3 point, String type, double distance) {
    }

    private static RayHit castRay(Minecraft mc, Vec3 origin, Vec3 dir) {
        Vec3 end = origin.add(dir.scale(RAY_DIST));

        // 方块：用远超原版 pick 的距离，保证远处目标也能取到命中点
        HitResult block = mc.level.clip(new ClipContext(origin, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
        Vec3 best = block.getType() == HitResult.Type.MISS ? end : block.getLocation();
        String type = block.getType() == HitResult.Type.MISS ? "MISS" : "BLOCK";
        double bestDist = best.distanceTo(origin);

        // 实体：沿射线找最近的 AABB 命中
        AABB search = new AABB(origin, end).inflate(1.0);
        Entity hitEntity = null;
        for (Entity e : mc.level.getEntities(mc.player, search, Entity::isAlive)) {
            var opt = e.getBoundingBox().inflate(0.15).clip(origin, end);
            if (opt.isEmpty()) continue;
            double d = opt.get().distanceTo(origin);
            if (d < bestDist) {
                bestDist = d;
                best = opt.get();
                hitEntity = e;
            }
        }
        if (hitEntity != null) {
            type = "ENTITY";
        }
        return new RayHit(best, type, bestDist);
    }
}
