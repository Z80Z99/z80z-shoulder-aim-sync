package com.github.z80z.shoulderaimsync.client;

import com.github.z80z.shoulderaimsync.OSConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 枪械识别：TACZ 走 IGun/NBT 判定，其他枪械模组用“命名空间 + 类名/反射”软兼容。
 * 编译期不依赖任何枪械模组。
 */
public final class GunDetect {

    private static final Logger LOGGER = LogManager.getLogger("z80z_shoulder_aim_sync");

    private static final Map<Class<?>, Method[]> METHOD_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> SNIPER_CACHE = new ConcurrentHashMap<>();
    private static final Method[] NONE = new Method[0];

    /** 其他枪械模组里这些类名片段不是枪（弹匣、子弹、配件、工作台等）。 */
    private static final List<String> NON_GUN_MARKERS = List.of(
            "table", "ammo", "attachment", "bullet", "magazine", "workbench",
            "target", "statue", "box", "crafting", "recipe");

    private GunDetect() {
    }

    public static boolean isGun(ItemStack stack, OSConfig.Cfg c) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        if (id == null) return false;

        // TACZ 物品：只看 IGun 接口/NBT，绝不能用“包名含 guns”之类启发式
        if (TaczCompat.isTaczItem(item)) {
            return TaczCompat.isGunItem(stack);
        }

        if (!c.gunMods.contains(id.getNamespace())) return false;
        String cls = item.getClass().getName().toLowerCase(Locale.ROOT);
        for (String marker : NON_GUN_MARKERS) {
            if (cls.contains(marker)) return false;
        }
        return cls.contains("gun") || findGunIdMethod(item.getClass()) != NONE;
    }

    /** 该枪械的弹道是否从 TACZ 的枪口原点发出（眼睛高度 +0.1 格） */
    public static boolean usesTaczMuzzle(ItemStack stack) {
        return !stack.isEmpty() && TaczCompat.isTaczItem(stack.getItem());
    }

    /** 枪械识别 ID：NBT GunId → IGun.getGunId → 通用反射 → 注册表 ID。 */
    public static String gunId(ItemStack stack) {
        if (stack.isEmpty()) return "";
        String nbt = TaczCompat.gunIdFromNbt(stack);
        if (nbt != null) return nbt.toLowerCase(Locale.ROOT);

        String viaTacz = TaczCompat.gunId(stack);
        if (viaTacz != null) return viaTacz.toLowerCase(Locale.ROOT);

        Item item = stack.getItem();
        String generic = invokeGunId(item, stack);
        if (generic != null) return generic.toLowerCase(Locale.ROOT);

        return registryId(stack);
    }

    public static boolean isSniper(ItemStack stack, OSConfig.Cfg c) {
        String reg = registryId(stack);
        String id = gunId(stack);
        if (c.sniperIds.contains(reg) || (!id.isEmpty() && c.sniperIds.contains(id))) {
            return true;
        }

        // 关键词/精确列表会变化，缓存键带上它们的哈希，配置一改自然失效
        String cacheKey = id + "|" + reg + "|" + c.sniperKeywords.hashCode() + "|" + c.sniperIds.hashCode();
        Boolean cached = SNIPER_CACHE.get(cacheKey);
        if (cached != null) return cached;

        boolean result;
        String type = TaczCompat.gunType(stack);
        boolean definitive = type != null;
        if (definitive) {
            // TACZ 枪包自带类型字段，最可靠（自定义枪包也适用）
            result = "sniper".equalsIgnoreCase(type);
        } else {
            result = false;
            for (String k : c.sniperKeywords) {
                if (k.isEmpty()) continue;
                if (reg.contains(k) || id.contains(k)) {
                    result = true;
                    break;
                }
            }
        }
        // 枪包是异步加载的：类型未知时不要缓存结果，否则会永久判为非狙击
        if (definitive || result) {
            SNIPER_CACHE.put(cacheKey, result);
        }
        return result;
    }

    /** 枪包重载/换世界后清缓存，避免沿用旧类型。 */
    public static void invalidate() {
        SNIPER_CACHE.clear();
        TaczCompat.clearCache();
    }

    private static String registryId(ItemStack stack) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id == null ? "" : id.toString().toLowerCase(Locale.ROOT);
    }

    private static String invokeGunId(Item item, ItemStack stack) {
        Method[] found = findGunIdMethod(item.getClass());
        if (found == NONE) return null;
        Method m = found[0];
        try {
            Object r = m.getParameterCount() == 1
                    ? (Modifier.isStatic(m.getModifiers()) ? m.invoke(null, stack) : m.invoke(item, stack))
                    : m.invoke(item);
            if (r instanceof ResourceLocation rl) return rl.toString();
            if (r instanceof String s) return s;
            return null;
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * 优先用 getMethods()（包含接口 default 方法），再补一遍类继承链上的非 public 方法。
     * TACZ 的 getGunId 就是 GunItemDataAccessor 上的 default 方法，只扫 getDeclaredMethods 会漏掉。
     */
    private static Method[] findGunIdMethod(Class<?> cls) {
        return METHOD_CACHE.computeIfAbsent(cls, k -> {
            for (Method m : k.getMethods()) {
                if (!m.getName().equals("getGunId")) continue;
                if (m.getParameterCount() > 1) continue;
                if (m.getParameterCount() == 1 && m.getParameterTypes()[0] != ItemStack.class) continue;
                return new Method[]{m};
            }
            for (Class<?> cur = k; cur != null && cur != Object.class; cur = cur.getSuperclass()) {
                for (Method m : cur.getDeclaredMethods()) {
                    if (!m.getName().equals("getGunId")) continue;
                    if (m.getParameterCount() > 1) continue;
                    if (m.getParameterCount() == 1 && m.getParameterTypes()[0] != ItemStack.class) continue;
                    try {
                        m.setAccessible(true);
                    } catch (Throwable t) {
                        LOGGER.debug("[z80z_shoulder_aim_sync] 无法访问 {}.getGunId", cur.getName());
                        continue;
                    }
                    return new Method[]{m};
                }
            }
            return NONE;
        });
    }
}
