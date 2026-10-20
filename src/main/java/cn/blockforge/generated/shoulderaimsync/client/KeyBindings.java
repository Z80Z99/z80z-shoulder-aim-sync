package cn.blockforge.generated.shoulderaimsync.client;

import cn.blockforge.generated.shoulderaimsync.ShoulderAimSyncMod;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

/** 键位：默认 X 换肩（避开 TACZ 的 R 换弹）、K 打开设置，均可在原版“按键设置”中自定义 */
public final class KeyBindings {

    public static final String CATEGORY = "key.categories." + ShoulderAimSyncMod.MODID;

    public static final KeyMapping TOGGLE_SHOULDER = new KeyMapping(
            "key." + ShoulderAimSyncMod.MODID + ".toggle_shoulder", GLFW.GLFW_KEY_X, CATEGORY);
    public static final KeyMapping SETTINGS = new KeyMapping(
            "key." + ShoulderAimSyncMod.MODID + ".settings", GLFW.GLFW_KEY_K, CATEGORY);

    private KeyBindings() {
    }

    static void register(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_SHOULDER);
        event.register(SETTINGS);
    }
}
