package cn.blockforge.generated.shoulderaimsync.client;

import cn.blockforge.generated.shoulderaimsync.OSConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;

/**
 * 镜片黑框过渡：强制第一人称切换瞬间的 0.2 秒黑框覆盖（进/出各一次脉冲）。
 */
public final class ScopeFade {

    private static long startNano = -1;
    private static double durSec = 0.2;

    private ScopeFade() {
    }

    public static void pulse(OSConfig.Cfg c) {
        if (c != null && c.scopeFade) {
            startNano = System.nanoTime();
            durSec = Math.max(0.05, c.fadeTime);
        }
    }

    /** 黑框过渡进行中（用于让准星等 HUD 在过渡时避让） */
    public static boolean isActive() {
        if (startNano < 0) return false;
        return (System.nanoTime() - startNano) / 1e9 / durSec < 1.0;
    }

    public static void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        if (startNano < 0) return;
        double t = (System.nanoTime() - startNano) / 1e9 / durSec;
        if (t >= 1.0) {
            startNano = -1;
            return;
        }
        if (t < 0.0) return;
        float a = (float) Math.sin(Math.PI * t); // 平滑脉冲：0 → 1 → 0
        if (a < 0.015f) return;
        int alpha = (int) (a * 235.0f);
        graphics.fill(0, 0, width, height, alpha << 24);
    }
}
