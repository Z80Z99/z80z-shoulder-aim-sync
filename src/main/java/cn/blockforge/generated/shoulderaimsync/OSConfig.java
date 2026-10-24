package cn.blockforge.generated.shoulderaimsync;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 客户端配置。运行期不直接读 ConfigValue（避免每帧解析开销），
 * 所有参数快照进 {@link Cfg}，加载/保存/界面修改时刷新。
 */
public final class OSConfig {

    // ---------- 默认值（界面“恢复默认”也引用这里） ----------
    public static final boolean DEF_ENABLED = true;
    public static final boolean DEF_TAKEOVER = true;
    public static final boolean DEF_AUTO_SHOULDER = true;
    public static final boolean DEF_RIGHT_SIDE = true;
    public static final String DEF_FOLLOW_MODE = "SMOOTH";
    public static final double DEF_TRANSITION = 0.25;
    public static final double DEF_STIFFNESS = 110;
    public static final double DEF_DAMPING = 17;
    public static final double DEF_OFFSET_X = 0.55;
    public static final double DEF_OFFSET_Y = 0.08;
    public static final double DEF_BACK = 1.15;
    public static final double DEF_SLIDE = 0.15;
    public static final double DEF_VIEW_SWITCH = 0.25;
    public static final boolean DEF_AVOID_WALL = true;
    public static final boolean DEF_ZOOM = true;
    public static final double DEF_ZOOM_FACTOR = 0.7;
    public static final boolean DEF_PUSH = true;
    public static final double DEF_PUSH_DIST = 0.4;
    public static final double DEF_AIM_TIME = 0.3;
    public static final boolean DEF_ZOOM_FP = false;
    public static final int DEF_AIM_DELAY = 2;
    public static final boolean DEF_DEFER_TACZ = true;
    public static final boolean DEF_CROSSHAIR = true;
    public static final boolean DEF_FOLLOW_TACZ = false;
    public static final boolean DEF_FADE_PLAYER = true;
    public static final boolean DEF_DEBUG_CROSSHAIR = false;
    public static final boolean DEF_AUTO_RIDING = true;
    public static final boolean DEF_AUTO_ELYTRA = true;
    public static final int DEF_ELYTRA_DELAY = 10;
    public static final boolean DEF_ELYTRA_ROLL = true;
    public static final int DEF_ELYTRA_ROLL_STRENGTH = 20;
    public static final List<String> DEF_RIDING_BLOCKLIST = List.of();
    public static final boolean DEF_FORCE_FP_SNIPER = true;
    public static final boolean DEF_SCOPE_FADE = true;
    public static final double DEF_FADE_TIME = 0.2;
    public static final List<String> DEF_GUN_MODS = List.of("tacz");
    public static final List<String> DEF_SNIPER_KEYWORDS =
            List.of("ai_awp", "kar98", "m107", "m700", "m95", "springfield1873",
                    "awm", "m200", "svd", "barrett", "as50");
    public static final List<String> DEF_SNIPER_IDS = List.of();

    public static final ForgeConfigSpec SPEC;

    public static ForgeConfigSpec.BooleanValue ENABLED;
    public static ForgeConfigSpec.BooleanValue TAKEOVER;
    public static ForgeConfigSpec.BooleanValue AUTO_SHOULDER;
    public static ForgeConfigSpec.BooleanValue RIGHT_SIDE;
    public static ForgeConfigSpec.ConfigValue<String> FOLLOW_MODE;
    public static ForgeConfigSpec.DoubleValue TRANSITION;
    public static ForgeConfigSpec.DoubleValue STIFFNESS;
    public static ForgeConfigSpec.DoubleValue DAMPING;
    public static ForgeConfigSpec.DoubleValue OFFSET_X;
    public static ForgeConfigSpec.DoubleValue OFFSET_Y;
    public static ForgeConfigSpec.DoubleValue BACK;
    public static ForgeConfigSpec.DoubleValue SLIDE;
    public static ForgeConfigSpec.DoubleValue VIEW_SWITCH;
    public static ForgeConfigSpec.BooleanValue AVOID_WALL;
    public static ForgeConfigSpec.BooleanValue ZOOM;
    public static ForgeConfigSpec.DoubleValue ZOOM_FACTOR;
    public static ForgeConfigSpec.BooleanValue PUSH;
    public static ForgeConfigSpec.DoubleValue PUSH_DIST;
    public static ForgeConfigSpec.DoubleValue AIM_TIME;
    public static ForgeConfigSpec.BooleanValue ZOOM_FP;
    public static ForgeConfigSpec.IntValue AIM_DELAY;
    public static ForgeConfigSpec.BooleanValue DEFER_TACZ;
    public static ForgeConfigSpec.BooleanValue CROSSHAIR;
    public static ForgeConfigSpec.BooleanValue FOLLOW_TACZ;
    public static ForgeConfigSpec.BooleanValue FADE_PLAYER;
    public static ForgeConfigSpec.BooleanValue DEBUG_CROSSHAIR;
    public static ForgeConfigSpec.BooleanValue AUTO_RIDING;
    public static ForgeConfigSpec.BooleanValue AUTO_ELYTRA;
    public static ForgeConfigSpec.IntValue ELYTRA_DELAY;
    public static ForgeConfigSpec.BooleanValue ELYTRA_ROLL;
    public static ForgeConfigSpec.IntValue ELYTRA_ROLL_STRENGTH;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> RIDING_BLOCKLIST;
    public static ForgeConfigSpec.BooleanValue FORCE_FP_SNIPER;
    public static ForgeConfigSpec.BooleanValue SCOPE_FADE;
    public static ForgeConfigSpec.DoubleValue FADE_TIME;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> GUN_MODS;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> SNIPER_KEYWORDS;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> SNIPER_IDS;

    /** 由 FML 在配置加载事件里注入，界面保存时调用 save() */
    public static volatile ModConfig modConfig;

    /** 运行期快照 */
    public static volatile Cfg CACHE = Cfg.defaults();

    private OSConfig() {
    }

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("general");
        ENABLED = b.comment("总开关：关闭后完全交还原版视角").define("enabled", DEF_ENABLED);
        TAKEOVER = b.comment("接管原版 F5：切换顺序 第一人称→越肩→第三人称(后)→第三人称(前)").define("takeoverF5", DEF_TAKEOVER);
        AUTO_SHOULDER = b.comment("进入世界时自动切到越肩视角（需同时开启接管F5）").define("autoShoulder", DEF_AUTO_SHOULDER);
        AUTO_RIDING = b.comment("上坐骑时自动切到越肩视角（下马恢复；马需已上鞍）").define("autoShoulderRiding", DEF_AUTO_RIDING);

        b.pop(1);
        b.push("camera");
        RIGHT_SIDE = b.comment("默认右肩(true)/左肩(false)").define("rightSide", DEF_RIGHT_SIDE);
        FOLLOW_MODE = b.comment("跟随模式：SMOOTH=平滑插值 SPRING=弹簧阻尼(更拟真更有重量) FIXED=固定跟随(无延迟，镜头刚性锁定)").defineInList("followMode", DEF_FOLLOW_MODE, List.of("SMOOTH", "SPRING", "FIXED"));
        TRANSITION = b.comment("平滑过渡时间(秒)，0.25≈无跳变缓动").defineInRange("transitionTime", DEF_TRANSITION, 0.02, 2.0);
        STIFFNESS = b.comment("弹簧刚度（仅 SPRING 模式）").defineInRange("springStiffness", DEF_STIFFNESS, 10, 400);
        DAMPING = b.comment("弹簧阻尼（仅 SPRING 模式），越小回摆惯性越大").defineInRange("springDamping", DEF_DAMPING, 2, 80);
        OFFSET_X = b.comment("左右偏移(格)：越靠近肩膀越小").defineInRange("shoulderOffsetX", DEF_OFFSET_X, 0.05, 1.5);
        OFFSET_Y = b.comment("竖直偏移(格)，相对眼睛").defineInRange("shoulderOffsetY", DEF_OFFSET_Y, -0.5, 0.6);
        BACK = b.comment("相机在玩家身后的距离(格)").defineInRange("backDistance", DEF_BACK, 0.25, 2.5);
        SLIDE = b.comment("R 键换肩横移时间(秒)，默认 0.15").defineInRange("shoulderSlideTime", DEF_SLIDE, 0.02, 0.8);
        VIEW_SWITCH = b.comment("F5 视角切换的平滑过渡时间(秒)，0=瞬时切换").defineInRange("viewSwitchTime", DEF_VIEW_SWITCH, 0.0, 0.8);
        AVOID_WALL = b.comment("镜头贴墙自动收缩").define("avoidWall", DEF_AVOID_WALL);
        FADE_PLAYER = b.comment("镜头贴得很近时虚化人物模型（更近则直接隐藏），避免身体糊住屏幕").define("fadePlayerWhenClose", DEF_FADE_PLAYER);

        b.pop(1);
        b.push("aim");
        ZOOM = b.comment("瞄准时渐进变焦（FOV 收窄）").define("aimZoom", DEF_ZOOM);
        ZOOM_FACTOR = b.comment("变焦后 FOV 占原比例，0.7=收窄至 70%").defineInRange("zoomFactor", DEF_ZOOM_FACTOR, 0.35, 1.0);
        PUSH = b.comment("机身前探贴肩：瞄准时相机前移").define("aimPush", DEF_PUSH);
        PUSH_DIST = b.comment("前探距离(格)，默认 0.4").defineInRange("pushDistance", DEF_PUSH_DIST, 0.0, 1.5);
        AIM_TIME = b.comment("变焦/前探过渡时间(秒)，默认 0.3").defineInRange("aimTransitionTime", DEF_AIM_TIME, 0.02, 1.5);
        ZOOM_FP = b.comment("第一人称下也变焦。注意：与 TACZ 的瞄具倍率叠加会导致实际倍率偏大；装了 TACZ-addon 且开着“瞄准切换第一人称”时建议保持关闭").define("zoomInFirstPerson", DEF_ZOOM_FP);
        AIM_DELAY = b.comment("长按右键多少 tick 后才算进入瞄准（防连点闪烁），0=立即").defineInRange("aimHoldTicks", DEF_AIM_DELAY, 0, 20);
        DEFER_TACZ = b.comment("装了倍镜（瞄具倍率>1）时把变焦交给 TACZ，避免两层 FOV 相乘；铁瞄仍使用本模组的轻微变焦").define("deferToTaczScope", DEF_DEFER_TACZ);
        CROSSHAIR = b.comment("越肩持枪时绘制准星（TACZ 只在第一人称绘制准星）").define("showShoulderCrosshair", DEF_CROSSHAIR);
        DEBUG_CROSSHAIR = b.comment("调试：额外绘制屏幕中心红叉与投影数值，用于排查准星与弹着点偏差").define("debugCrosshair", DEF_DEBUG_CROSSHAIR);
        FOLLOW_TACZ = b.comment("瞄准进度跟随 TACZ 的开镜动画（用枪包自带的开镜时间）。关闭时使用上方“变焦/前探时间”，效果可自行调节").define("followTaczAimProgress", DEF_FOLLOW_TACZ);

        b.pop(1);
        b.push("elytra");
        AUTO_ELYTRA = b.comment("滑翔时自动切到越肩视角（停止滑翔后恢复）").define("autoShoulderElytra", DEF_AUTO_ELYTRA);
        ELYTRA_DELAY = b.comment("开始滑翔后经过多少 tick 触发自动越肩").defineInRange("elytraStartDelayTicks", DEF_ELYTRA_DELAY, 1, 200);
        ELYTRA_ROLL = b.comment("滑翔转弯时画面随倾斜滚转").define("elytraRollScreen", DEF_ELYTRA_ROLL);
        ELYTRA_ROLL_STRENGTH = b.comment("滚转强度（度），0=不滚转").defineInRange("elytraRollStrength", DEF_ELYTRA_ROLL_STRENGTH, 0, 100);

        b.pop(1);
        b.push("sniper");
        FORCE_FP_SNIPER = b.comment("狙击枪瞄准时强制第一人称，松手立刻恢复越肩").define("forceFirstPersonSniper", DEF_FORCE_FP_SNIPER);
        SCOPE_FADE = b.comment("切换时镜片黑框覆盖过渡").define("scopeFade", DEF_SCOPE_FADE);
        FADE_TIME = b.comment("黑框过渡时长(秒)，默认 0.2").defineInRange("scopeFadeTime", DEF_FADE_TIME, 0.05, 1.0);

        b.pop(1);
        b.push("compat");
        GUN_MODS = b.comment("哪些命名空间的物品算枪械（可扩展其他枪械模组）").defineList("gunModIds", DEF_GUN_MODS, o -> o instanceof String);
        SNIPER_KEYWORDS = b.comment("枪械 ID 含以下关键词即视为狙击枪（TACZ 自带类型字段优先，此项用于其他枪械模组）").defineList("sniperKeywords", DEF_SNIPER_KEYWORDS, o -> o instanceof String);
        SNIPER_IDS = b.comment("额外指定狙击枪完整 ID（modid:path，逐个精确匹配）").defineList("sniperGunIds", DEF_SNIPER_IDS, o -> o instanceof String);
        RIDING_BLOCKLIST = b.comment("骑乘自动越肩的实体黑名单（实体类型 ID，如 minecraft:boat）").defineList("ridingEntityBlocklist", DEF_RIDING_BLOCKLIST, o -> o instanceof String);
        b.pop(1);
        SPEC = b.build();
    }

    /** 从 ConfigValue 重建快照 */
    public static void refreshCache() {
        Cfg c = new Cfg();
        c.enabled = ENABLED.get();
        c.takeover = TAKEOVER.get();
        c.autoShoulder = AUTO_SHOULDER.get();
        c.rightSide = RIGHT_SIDE.get();
        c.spring = "SPRING".equalsIgnoreCase(FOLLOW_MODE.get());
        c.fixed = "FIXED".equalsIgnoreCase(FOLLOW_MODE.get());
        c.transition = clamp(TRANSITION.get(), 0.02, 2.0);
        c.stiffness = clamp(STIFFNESS.get(), 10, 400);
        c.damping = clamp(DAMPING.get(), 2, 80);
        c.offX = clamp(OFFSET_X.get(), 0.05, 1.5);
        c.offY = clamp(OFFSET_Y.get(), -0.5, 0.6);
        c.back = clamp(BACK.get(), 0.25, 2.5);
        c.slide = clamp(SLIDE.get(), 0.02, 0.8);
        c.viewSwitchTime = clamp(VIEW_SWITCH.get(), 0.0, 0.8);
        c.avoidWall = AVOID_WALL.get();
        c.zoom = ZOOM.get();
        c.zoomFactor = clamp(ZOOM_FACTOR.get(), 0.35, 1.0);
        c.push = PUSH.get();
        c.pushDist = clamp(PUSH_DIST.get(), 0.0, 1.5);
        c.aimTime = clamp(AIM_TIME.get(), 0.02, 1.5);
        c.zoomFp = ZOOM_FP.get();
        c.aimDelayTicks = clamp(AIM_DELAY.get(), 0, 20);
        c.deferToTacz = DEFER_TACZ.get();
        c.crosshair = CROSSHAIR.get();
        c.followTacz = FOLLOW_TACZ.get();
        c.fadePlayer = FADE_PLAYER.get();
        c.debugCrosshair = DEBUG_CROSSHAIR.get();
        c.autoRiding = AUTO_RIDING.get();
        c.autoElytra = AUTO_ELYTRA.get();
        c.elytraDelay = clamp(ELYTRA_DELAY.get(), 1, 200);
        c.elytraRoll = ELYTRA_ROLL.get();
        c.elytraRollStrength = clamp(ELYTRA_ROLL_STRENGTH.get(), 0, 100);
        c.ridingBlocklist = lower(RIDING_BLOCKLIST.get());
        c.forceFpSniper = FORCE_FP_SNIPER.get();
        c.scopeFade = SCOPE_FADE.get();
        c.fadeTime = clamp(FADE_TIME.get(), 0.05, 1.0);
        c.gunMods = lower(  GUN_MODS.get());
        c.sniperKeywords = lower(SNIPER_KEYWORDS.get());
        c.sniperIds = lower(SNIPER_IDS.get());
        CACHE = c;
    }

    public static void saveNow() {
        ModConfig mc = modConfig;
        if (mc != null) {
            mc.save();
        }
    }

    private static List<String> lower(List<? extends String> src) {
        List<String> out = new ArrayList<>(src.size());
        for (String s : src) {
            if (s != null) out.add(s.trim().toLowerCase(Locale.ROOT));
        }
        return List.copyOf(out);
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    /** 运行期参数快照（字段带默认值 = defaults()） */
    public static final class Cfg {
        public boolean enabled = DEF_ENABLED;
        public boolean takeover = DEF_TAKEOVER;
        public boolean autoShoulder = DEF_AUTO_SHOULDER;
        public boolean rightSide = DEF_RIGHT_SIDE;
        public boolean spring;
        public boolean fixed;
        public double transition = DEF_TRANSITION;
        public double stiffness = DEF_STIFFNESS;
        public double damping = DEF_DAMPING;
        public double offX = DEF_OFFSET_X;
        public double offY = DEF_OFFSET_Y;
        public double back = DEF_BACK;
        public double slide = DEF_SLIDE;
        public double viewSwitchTime = DEF_VIEW_SWITCH;
        public boolean avoidWall = DEF_AVOID_WALL;
        public boolean zoom = DEF_ZOOM;
        public double zoomFactor = DEF_ZOOM_FACTOR;
        public boolean push = DEF_PUSH;
        public double pushDist = DEF_PUSH_DIST;
        public double aimTime = DEF_AIM_TIME;
        public boolean zoomFp = DEF_ZOOM_FP;
        public int aimDelayTicks = DEF_AIM_DELAY;
        public boolean deferToTacz = DEF_DEFER_TACZ;
        public boolean crosshair = DEF_CROSSHAIR;
        public boolean followTacz = DEF_FOLLOW_TACZ;
        public boolean fadePlayer = DEF_FADE_PLAYER;
        public boolean debugCrosshair = DEF_DEBUG_CROSSHAIR;
        public boolean autoRiding = DEF_AUTO_RIDING;
        public boolean autoElytra = DEF_AUTO_ELYTRA;
        public int elytraDelay = DEF_ELYTRA_DELAY;
        public boolean elytraRoll = DEF_ELYTRA_ROLL;
        public int elytraRollStrength = DEF_ELYTRA_ROLL_STRENGTH;
        public List<String> ridingBlocklist = DEF_RIDING_BLOCKLIST;
        public boolean forceFpSniper = DEF_FORCE_FP_SNIPER;
        public boolean scopeFade = DEF_SCOPE_FADE;
        public double fadeTime = DEF_FADE_TIME;
        public List<String> gunMods = DEF_GUN_MODS;
        public List<String> sniperKeywords = DEF_SNIPER_KEYWORDS;
        public List<String> sniperIds = DEF_SNIPER_IDS;

        public static Cfg defaults() {
            return new Cfg();
        }
    }
}
