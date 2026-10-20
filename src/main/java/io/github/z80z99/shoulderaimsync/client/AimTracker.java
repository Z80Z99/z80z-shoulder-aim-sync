package io.github.z80z99.shoulderaimsync.client;

import io.github.z80z99.shoulderaimsync.OSConfig;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

/**
 * 瞄准状态机：优先跟随 TACZ 的真实瞄准状态与开镜进度，TACZ 不可用时回退到“长按右键”。
 * 同时驱动变焦/前探进度，并在狙击枪瞄准时按配置强制第一人称。
 */
public final class AimTracker {

    /** 0..1 瞄准进度（渐进变焦与前探共用） */
    public static volatile double level = 0.0;
    /** 当前是否处于瞄准 */
    public static volatile boolean aiming = false;
    /** 当前枪械是否识别为狙击枪 */
    public static volatile boolean sniper = false;

    private static double heldFor = 0.0;
    private static long aimStartNano = -1;

    private AimTracker() {
    }

    /** 每个渲染帧调用一次（在相机 setup 之后、FOV 计算之前） */
    public static void frame(double dt, float partialTick, OSConfig.Cfg c) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            reset();
            return;
        }
        ItemStack hand = mc.player.getMainHandItem();
        boolean gun = c.enabled && GunDetect.isGun(hand, c);

        // TACZ 持枪时以它的瞄准状态为准（支持改键、切换式瞄准、开镜进度）
        boolean taczGun = c.enabled && TaczCompat.isLoaded() && TaczCompat.holdsGun(mc.player);
        boolean raw;
        double taczProgress = -1.0;
        if (taczGun) {
            raw = TaczCompat.isAiming(mc.player)
                    && mc.screen == null
                    && !mc.player.isSpectator()
                    && !mc.player.isDeadOrDying();
            taczProgress = TaczCompat.aimingProgress(mc.player, partialTick);
        } else {
            raw = gun
                    && mc.options.keyUse.isDown()
                    && mc.screen == null
                    && !mc.player.isSpectator()
                    && !mc.player.isDeadOrDying();
        }
        heldFor = raw ? heldFor + dt : 0.0;
        boolean want = raw && heldFor >= c.aimDelayTicks / 20.0;

        if (want && !aiming) {
            aiming = true;
            aimStartNano = System.nanoTime();
            sniper = gun && GunDetect.isSniper(hand, c);
            maybeForceFirstPerson(mc, c, true);
        } else if (!want && aiming) {
            aiming = false;
            // 松手立刻恢复（“仅瞄准期间强制”）
            if (ViewManager.ourFp) {
                releaseFirstPerson(mc, c);
            }
        } else if (aiming) {
            // 瞄准时换枪（滚轮切换）实时重判狙击标识
            boolean nowSniper = gun && GunDetect.isSniper(hand, c);
            if (nowSniper != sniper) {
                sniper = nowSniper;
                if (nowSniper) {
                    maybeForceFirstPerson(mc, c, true);
                } else if (ViewManager.ourFp) {
                    // 狙击 → 普通枪：立刻退出强制第一人称，否则会一直卡在镜内
                    releaseFirstPerson(mc, c);
                }
            }
        }

        if (aiming) {
            if (c.followTacz && taczProgress >= 0.0) {
                // 跟随 TACZ 开镜动画：进度由枪包的 aimTime 决定，与本模组配置无关
                level = clamp01(taczProgress);
            } else {
                // 本模组自计时：变焦/前探时间配置真正可控
                level = Math.min(1.0, (System.nanoTime() - aimStartNano) / 1e9 / Math.max(0.02, c.aimTime));
            }
        } else if (level > 0.0) {
            level = Math.max(0.0, level - dt / Math.max(0.02, c.aimTime * 0.5));
        }
    }

    public static void reset() {
        Minecraft mc = Minecraft.getInstance();
        aiming = false;
        level = 0.0;
        sniper = false;
        heldFor = 0.0;
        aimStartNano = -1;
        if (ViewManager.ourFp && mc != null) {
            ViewManager.ourFp = false;
            if (ViewManager.mode == ViewManager.Mode.SHOULDER) {
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            }
        }
    }

    /** 狙击开镜：镜内强制第一人称（仅当越肩且当前确实是第三人称后视时） */
    private static void maybeForceFirstPerson(Minecraft mc, OSConfig.Cfg c, boolean pulseFade) {
        if (!c.enabled || !c.forceFpSniper || !sniper) return;
        if (ViewManager.mode != ViewManager.Mode.SHOULDER) return;
        if (mc.options.getCameraType() != CameraType.THIRD_PERSON_BACK) return;
        ViewManager.ourFp = true;
        mc.options.setCameraType(CameraType.FIRST_PERSON);
        if (pulseFade && c.scopeFade) ScopeFade.pulse(c);
    }

    private static void releaseFirstPerson(Minecraft mc, OSConfig.Cfg c) {
        ViewManager.ourFp = false;
        if (ViewManager.mode == ViewManager.Mode.SHOULDER) {
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            if (c.scopeFade) ScopeFade.pulse(c);
        }
        // 第一人称期间锚点没有更新，清掉避免松手后镜头从旧位置滑过来
        ViewManager.reset();
    }

    /** 前探/变焦进度用 smoothstep，观感更顺滑 */
    public static double easedLevel() {
        double t = clamp01(level);
        return t * t * (3.0 - 2.0 * t);
    }

    private static double clamp01(double v) {
        return v < 0.0 ? 0.0 : Math.min(1.0, v);
    }
}
