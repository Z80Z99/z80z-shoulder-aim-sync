package io.github.z80z99.shoulderaimsync;

import io.github.z80z99.shoulderaimsync.client.AutoViewSwitch;
import io.github.z80z99.shoulderaimsync.client.ClientEvents;
import io.github.z80z99.shoulderaimsync.client.ModBusEvents;
import io.github.z80z99.shoulderaimsync.client.PlayerFade;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * 越肩视角与枪械瞄准兼容（shoulder_aim_sync）
 *
 * 纯客户端模组：
 * - 平滑接管原版 F5 视角切换，新增左右越肩模式；
 * - X 键秒换肩（默认 0.15 秒平滑横移，保留惯性，可改键）；
 * - 枪械（如 TACZ）瞄准时渐进变焦 + 机身前探贴肩；
 * - 狙击枪瞄准时可强制切换第一人称，带镜片黑框过渡；
 * - 越肩持枪准星；
 * - 三栏设置界面（默认 K 键），参数尽量完全开放自定义。
 */
@Mod(ShoulderAimSyncMod.MODID)
public final class ShoulderAimSyncMod {
    public static final String MODID = "shoulder_aim_sync";

    @SuppressWarnings("removal")
    public ShoulderAimSyncMod() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, OSConfig.SPEC, MODID + "-client.toml");
        if (FMLEnvironment.dist == Dist.CLIENT) {
            IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
            // 两条总线必须用不同的监听器类：mod 总线只接受 IModBusEvent
            modBus.register(new ModBusEvents());
            MinecraftForge.EVENT_BUS.register(new ClientEvents());
            MinecraftForge.EVENT_BUS.register(PlayerFade.create());
            MinecraftForge.EVENT_BUS.register(AutoViewSwitch.create());
        }
    }
}
