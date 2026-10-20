package cn.blockforge.generated.shoulderaimsync.client;

import cn.blockforge.generated.shoulderaimsync.OSConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 自动切换视角：上坐骑 / 开始滑翔时自动进入越肩，结束后恢复。
 *
 * 与 Nimble 的做法不同，这里不改 Options.cameraType，只在状态机层面切
 * {@link ViewManager.Mode}；玩家手动按 F5 切走即视为接管，不再强行拉回。
 */
public final class AutoViewSwitch {

    private static Entity lastVehicle;
    /** 当前越肩是否由“自动切换”触发（只有自己触发的才自动恢复） */
    private static boolean autoRidingActive;
    private static boolean autoElytraActive;
    private static boolean wasFallFlying;

    private AutoViewSwitch() {
    }

    public static AutoViewSwitch create() {
        return new AutoViewSwitch();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        OSConfig.Cfg c = OSConfig.CACHE;
        if (player == null || mc.level == null || !c.enabled) {
            reset();
            return;
        }
        // 玩家自己切了视角就放弃自动接管
        if (autoRidingActive && ViewManager.mode != ViewManager.Mode.SHOULDER) autoRidingActive = false;
        if (autoElytraActive && ViewManager.mode != ViewManager.Mode.SHOULDER) autoElytraActive = false;

        handleRiding(mc, player, c);
        handleElytra(mc, player, c);
    }

    // ---------------- 骑乘 ----------------

    private static void handleRiding(Minecraft mc, LocalPlayer player, OSConfig.Cfg c) {
        Entity vehicle = player.getVehicle();
        boolean changed = vehicle != lastVehicle;
        lastVehicle = vehicle;

        if (!c.autoRiding) {
            if (autoRidingActive) {
                autoRidingActive = false;
                restore(mc, player, c, autoElytraActive);
            }
            return;
        }
        if (!changed) return;

        if (vehicle == null) {
            // 下马
            if (autoRidingActive) {
                autoRidingActive = false;
                restore(mc, player, c, autoElytraActive);
            }
            return;
        }

        if (ViewManager.mode != ViewManager.Mode.FIRST) return; // 玩家已自选视角，不干预
        if (isBlocked(vehicle, c)) return;
        if (vehicle instanceof AbstractHorse horse && !horse.isSaddled()) return;

        ViewManager.applyMode(ViewManager.Mode.SHOULDER, mc);
        autoRidingActive = true;
    }

    private static boolean isBlocked(Entity vehicle, OSConfig.Cfg c) {
        if (c.ridingBlocklist.isEmpty()) return false;
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(vehicle.getType());
        return id != null && c.ridingBlocklist.contains(id.toString().toLowerCase(java.util.Locale.ROOT));
    }

    // ---------------- 鞘翅 ----------------

    private static void handleElytra(Minecraft mc, LocalPlayer player, OSConfig.Cfg c) {
        boolean flying = player.isFallFlying();
        if (c.autoElytra && flying && !wasFallFlying) {
            // 刚开始滑翔：记录本段滑翔的 tick 起算点由原版计数提供
            wasFallFlying = true;
        } else if (!flying) {
            if (wasFallFlying && autoElytraActive) {
                autoElytraActive = false;
                restore(mc, player, c, autoRidingActive);
            }
            wasFallFlying = false;
            return;
        }
        if (!c.autoElytra) {
            if (autoElytraActive) {
                autoElytraActive = false;
                restore(mc, player, c, autoRidingActive);
            }
            return;
        }
        if (autoElytraActive) return;
        if (ViewManager.mode != ViewManager.Mode.FIRST) return;
        // 恰好达到配置的延迟 tick 数时触发（与 Nimble 一致，每段滑翔只触发一次）
        if (player.getFallFlyingTicks() == c.elytraDelay) {
            ViewManager.applyMode(ViewManager.Mode.SHOULDER, mc);
            autoElytraActive = true;
        }
    }

    // ---------------- 恢复 ----------------

    private static void restore(Minecraft mc, LocalPlayer player, OSConfig.Cfg c, boolean otherAutoActive) {
        if (otherAutoActive) return; // 另一个自动模式仍在生效，不回第一人称
        if (ViewManager.mode != ViewManager.Mode.SHOULDER) return;
        ViewManager.applyMode(ViewManager.Mode.FIRST, mc);
    }

    private static void reset() {
        lastVehicle = null;
        autoRidingActive = false;
        autoElytraActive = false;
        wasFallFlying = false;
    }
}
