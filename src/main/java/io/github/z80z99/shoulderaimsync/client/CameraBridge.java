package io.github.z80z99.shoulderaimsync.client;

import net.minecraft.client.Camera;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 相机位置写入桥。
 * 不依赖名字：Forge 1.20.1 生产环境是 SRG 命名、开发环境是官方命名，
 * 字符串名跨不过去；所以按“结构签名”解析——
 * 取 Camera 里唯一的 (Vec3)->void 方法（即 setPosition），
 * 退化路径取唯一的 Vec3 实例字段（position）+ MutableBlockPos 字段。
 * 结果缓存在 MethodHandle/Field 上，每帧开销可忽略。
 */
final class CameraBridge {

    private static final Logger LOGGER = LogManager.getLogger("shoulder_aim_sync");

    private static MethodHandle SET_POSITION;
    private static Field POSITION_FIELD;
    private static Field BLOCK_POS_FIELD;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            for (Method m : Camera.class.getDeclaredMethods()) {
                if (m.getParameterCount() == 1
                        && m.getParameterTypes()[0] == Vec3.class
                        && m.getReturnType() == void.class) {
                    m.setAccessible(true);
                    SET_POSITION = lookup.unreflect(m);
                    break;
                }
            }
        } catch (Throwable t) {
            LOGGER.debug("[shoulder_aim_sync] Camera.setPosition 方法桥解析失败", t);
            SET_POSITION = null;
        }
        if (SET_POSITION == null) {
            try {
                for (Field f : Camera.class.getDeclaredFields()) {
                    if (f.getType() == Vec3.class && !java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                        f.setAccessible(true);
                        POSITION_FIELD = f;
                    } else if (f.getType() == BlockPos.MutableBlockPos.class
                            && !java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                        f.setAccessible(true);
                        BLOCK_POS_FIELD = f;
                    }
                }
            } catch (Throwable t) {
                LOGGER.debug("[shoulder_aim_sync] Camera 字段桥解析失败", t);
                POSITION_FIELD = null;
                BLOCK_POS_FIELD = null;
            }
        }
        if (SET_POSITION == null && POSITION_FIELD == null) {
            LOGGER.warn("[shoulder_aim_sync] 无法定位 Camera 位置写入点，越肩相机将不生效"
                    + "（该问题通常意味着 Minecraft/Forge 版本不匹配）");
        } else {
            LOGGER.debug("[shoulder_aim_sync] Camera 位置桥就绪：{}",
                    SET_POSITION != null ? "setPosition 方法" : "position 字段");
        }
    }

    private CameraBridge() {
    }

    static void setPosition(Camera camera, Vec3 pos) {
        MethodHandle h = SET_POSITION;
        if (h != null) {
            try {
                h.invoke(camera, pos);
                return;
            } catch (Throwable ignored) {
                // 落到字段方式
            }
        }
        Field pf = POSITION_FIELD;
        if (pf != null) {
            try {
                pf.set(camera, pos);
                Field bf = BLOCK_POS_FIELD;
                if (bf != null && bf.get(camera) instanceof BlockPos.MutableBlockPos mutable) {
                    mutable.set(pos.x, pos.y, pos.z);
                }
            } catch (Throwable ignored) {
                // 静默：本帧不覆写相机
            }
        }
    }
}
