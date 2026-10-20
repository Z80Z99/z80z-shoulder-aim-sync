package io.github.z80z99.shoulderaimsync.client;

import io.github.z80z99.shoulderaimsync.OSConfig;
import io.github.z80z99.shoulderaimsync.ShoulderAimSyncMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Mod 事件总线监听器。
 * 必须与 Forge 总线监听器（ClientEvents）分成两个类：mod 总线只接受 IModBusEvent，
 * 混在一起注册会在加载时抛 IllegalArgumentException。
 */
public class ModBusEvents {

    @SubscribeEvent
    public void onClientSetup(FMLClientSetupEvent event) {
        // FMLClientSetupEvent 在并行工作线程触发，配置快照与 TACZ 检测回到主线程做
        event.enqueueWork(() -> {
            OSConfig.refreshCache();
            TaczCompat.isLoaded();
        });
    }

    @SubscribeEvent
    public void onRegisterKeys(RegisterKeyMappingsEvent event) {
        KeyBindings.register(event);
    }

    @SubscribeEvent
    public void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
        // 准星在下、镜片黑框在上
        event.registerAboveAll("shoulder_crosshair", ShoulderCrosshair::render);
        event.registerAbove(ResourceLocation.fromNamespaceAndPath(ShoulderAimSyncMod.MODID, "shoulder_crosshair"),
                "scope_fade", ScopeFade::render);
    }

    @SubscribeEvent
    public void onConfigEvent(ModConfigEvent event) {
        ModConfig cfg = event.getConfig();
        if (ShoulderAimSyncMod.MODID.equals(cfg.getModId())) {
            OSConfig.modConfig = cfg;
            OSConfig.refreshCache();
            GunDetect.invalidate();
        }
    }
}
