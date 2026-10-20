package io.github.z80z99.shoulderaimsync.client;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TACZ 软兼容层：所有对 TACZ 的访问都走反射，编译期不依赖 TACZ。
 * 这里集中处理“TACZ 是否存在 / 是否持枪 / 是否在瞄准 / 瞄准进度 / 枪械 ID / 枪械类型 / 瞄具倍率”，
 * 避免这些判断散落在各业务类里。
 */
public final class TaczCompat {

    private static final Logger LOGGER = LogManager.getLogger("shoulder_aim_sync");
    private static final String TACZ_MODID = "tacz";
    private static final String TACZ_ITEM_PACKAGE = "com.tacz.guns.";
    /** TACZ 把枪械 ID 存在物品根标签的这个键上（GunItemDataAccessor.GUN_ID_TAG）。 */
    private static final String GUN_ID_TAG = "GunId";

    private static volatile int loadedState = -1; // -1 未检测, 0 无, 1 有
    private static boolean reflected;

    private static Class<?> gunOperatorClass;
    private static Method fromLocalPlayer;
    private static Method isAim;
    private static Method aimingProgress;

    private static Class<?> gunClass;
    private static Method mainHandHoldGun;
    private static Method getGunId;
    private static Method getAimingZoom;

    private static Class<?> timelessApiClass;
    private static Method getCommonGunIndex;

    private static final Map<Class<?>, Boolean> GUN_ITEM_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, String> GUN_TYPE_CACHE = new ConcurrentHashMap<>();

    private TaczCompat() {
    }

    // ---------------- 存在性与初始化 ----------------

    public static boolean isLoaded() {
        int s = loadedState;
        if (s < 0) {
            ModList modList = ModList.get();
            if (modList == null) {
                // ModList 尚未就绪：不缓存结果，下次调用再判断
                return false;
            }
            boolean present = modList.isLoaded(TACZ_MODID);
            if (present) {
                loadedState = 1;
                initReflection();
            } else {
                loadedState = 0;
            }
            s = loadedState;
        }
        return s == 1;
    }

    private static synchronized void initReflection() {
        if (reflected) return;
        reflected = true;
        try {
            gunOperatorClass = find("com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator");
            if (gunOperatorClass != null) {
                fromLocalPlayer = gunOperatorClass.getMethod("fromLocalPlayer", net.minecraft.client.player.LocalPlayer.class);
                isAim = gunOperatorClass.getMethod("isAim");
                aimingProgress = gunOperatorClass.getMethod("getClientAimingProgress", float.class);
            }

            gunClass = find("com.tacz.guns.api.item.IGun");
            if (gunClass != null) {
                mainHandHoldGun = gunClass.getMethod("mainHandHoldGun", LivingEntity.class);
                getGunId = gunClass.getMethod("getGunId", ItemStack.class);
                getAimingZoom = gunClass.getMethod("getAimingZoom", ItemStack.class);
            }

            timelessApiClass = find("com.tacz.guns.api.TimelessAPI");
            if (timelessApiClass != null) {
                getCommonGunIndex = timelessApiClass.getMethod("getCommonGunIndex", ResourceLocation.class);
            }

            if (gunOperatorClass == null || gunClass == null) {
                LOGGER.warn("[shoulder_aim_sync] 检测到 tacz 但反射接口不完整，将退回通用枪械识别（瞄准同步功能不可用）");
            }
        } catch (Throwable t) {
            LOGGER.warn("[shoulder_aim_sync] TACZ 反射初始化失败，将退回通用枪械识别", t);
        }
    }

    private static Class<?> find(String name) {
        try {
            return Class.forName(name, false, TaczCompat.class.getClassLoader());
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** 资源包/枪包重载或换世界后清掉按枪 ID 的缓存。 */
    public static void clearCache() {
        GUN_TYPE_CACHE.clear();
    }

    // ---------------- 物品判定 ----------------

    /** 物品是否属于 TACZ（只看包名，不依赖物品是否为枪）。 */
    public static boolean isTaczItem(Item item) {
        return item != null && item.getClass().getName().startsWith(TACZ_ITEM_PACKAGE);
    }

    /** 物品是否为 TACZ 枪械：走 IGun 接口判定，NBT GunId 作为兜底。 */
    public static boolean isGunItem(ItemStack stack) {
        if (!isLoaded() || stack.isEmpty()) return false;
        Class<?> cls = stack.getItem().getClass();
        Boolean cached = GUN_ITEM_CACHE.get(cls);
        if (Boolean.TRUE.equals(cached)) return true;

        boolean result = gunClass != null && gunClass.isAssignableFrom(cls);
        if (!result) {
            String nbt = gunIdFromNbt(stack);
            result = nbt != null && !nbt.isEmpty();
        }
        if (result) {
            GUN_ITEM_CACHE.put(cls, Boolean.TRUE);
        }
        return result;
    }

    /** 从物品 NBT 直接读枪械 ID（不依赖反射，TACZ 稳定写入根标签）。 */
    public static String gunIdFromNbt(ItemStack stack) {
        try {
            CompoundTag tag = stack.getTag();
            if (tag != null && tag.contains(GUN_ID_TAG, Tag.TAG_STRING)) {
                String id = tag.getString(GUN_ID_TAG);
                return id.isEmpty() ? null : id;
            }
        } catch (Throwable ignored) {
            // 数据异常时按“没有 ID”处理
        }
        return null;
    }

    /** 通过 IGun.getGunId 读枪械 ID（兼容只有接口 default 实现的情况）。 */
    public static String gunId(ItemStack stack) {
        if (getGunId == null || stack.isEmpty()) return null;
        try {
            Object r = getGunId.invoke(stack.getItem(), stack);
            return r instanceof ResourceLocation rl ? rl.toString() : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** 枪械类型字符串（tacz 的 sniper/rifle/pistol...），取不到返回 null。 */
    public static String gunType(ItemStack stack) {
        if (getCommonGunIndex == null) return null;
        String id = gunIdFromNbt(stack);
        if (id == null) id = gunId(stack);
        if (id == null || id.isEmpty()) return null;

        String key = id.toLowerCase(java.util.Locale.ROOT);
        String cached = GUN_TYPE_CACHE.get(key);
        if (cached != null) {
            return cached;
        }

        String type = null;
        try {
            ResourceLocation rl = ResourceLocation.tryParse(key);
            if (rl != null) {
                Object opt = getCommonGunIndex.invoke(null, rl);
                if (opt instanceof Optional<?> o && o.isPresent()) {
                    Object index = o.get();
                    Method getType = index.getClass().getMethod("getType");
                    Object r = getType.invoke(index);
                    if (r instanceof String s) type = s;
                }
            }
        } catch (Throwable ignored) {
            // 枪包尚未加载完成时按未知处理
        }
        // 只缓存成功结果：枪包是异步加载的，首次查询拿不到时不能永久缓存为“无类型”
        if (type != null) {
            GUN_TYPE_CACHE.put(key, type);
        }
        return type;
    }

    /** 当前枪械的瞄具倍率；铁瞄/TACZ 不可用时返回 1.0。 */
    public static double aimingZoom(ItemStack stack) {
        if (getAimingZoom == null || stack.isEmpty()) return 1.0;
        try {
            Object r = getAimingZoom.invoke(stack.getItem(), stack);
            if (r instanceof Number n) return n.doubleValue();
        } catch (Throwable ignored) {
            // 忽略：按无倍率处理
        }
        return 1.0;
    }

    // ---------------- 玩家状态 ----------------

    public static boolean holdsGun(LivingEntity entity) {
        if (mainHandHoldGun == null || entity == null) return false;
        try {
            return Boolean.TRUE.equals(mainHandHoldGun.invoke(null, entity));
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean isAiming(net.minecraft.client.player.LocalPlayer player) {
        Object op = operator(player);
        if (op == null || isAim == null) return false;
        try {
            return Boolean.TRUE.equals(isAim.invoke(op));
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** 0..1 瞄准进度；TACZ 不可用时返回 -1 表示“无数据”。 */
    public static double aimingProgress(net.minecraft.client.player.LocalPlayer player, float partialTick) {
        Object op = operator(player);
        if (op == null || aimingProgress == null) return -1.0;
        try {
            Object r = aimingProgress.invoke(op, partialTick);
            if (r instanceof Number n) return n.doubleValue();
        } catch (Throwable ignored) {
            // 返回 -1 让调用方回退到本地计时
        }
        return -1.0;
    }

    private static Object operator(net.minecraft.client.player.LocalPlayer player) {
        if (player == null || fromLocalPlayer == null) return null;
        try {
            return fromLocalPlayer.invoke(null, player);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
