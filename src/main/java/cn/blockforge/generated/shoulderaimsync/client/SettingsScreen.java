package cn.blockforge.generated.shoulderaimsync.client;

import cn.blockforge.generated.shoulderaimsync.OSConfig;
import cn.blockforge.generated.shoulderaimsync.ShoulderAimSyncMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModList;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 设置界面：宽屏时三栏（越肩/瞄准/狙击），窄屏或矮屏时自动改为单栏分页。
 * 所有文本走翻译键，数值完全开放调节，列表可自定义；修改写回客户端 TOML。
 */
public class SettingsScreen extends Screen {

    private static final String P = "screen." + ShoulderAimSyncMod.MODID + ".";
    private static final String O = "option." + ShoulderAimSyncMod.MODID + ".";
    private static final String V = "value." + ShoulderAimSyncMod.MODID + ".";
    private static final String U = "unit." + ShoulderAimSyncMod.MODID + ".";

    private static final int WIDGET_H = 14;
    private static final int ROW = 16;
    private static final int LABEL_H = 10;
    private static final int LIST_ROW = 26;
    private static final int MIN_PANEL_W = 150;
    private static final int MAX_PANEL_W = 228;
    private static final int PANEL_GAP = 8;
    private static final int TAB_H = 18;
    private static final int BOTTOM_RESERVE = 34;
    private static final int BOTTOM_RESERVE_ADDON = 52;

    private final List<Runnable> syncers = new ArrayList<>();
    private final List<BoxEntry> boxes = new ArrayList<>();

    private final List<Row> followRows = new ArrayList<>();
    private final List<Row> aimRows = new ArrayList<>();
    private final List<Row> sniperRows = new ArrayList<>();
    private List<Page> pages = List.of();
    private int page = 0;

    private boolean wide;
    private int panelX0, panelX1, panelX2, panelW;
    private int contentTop;

    public SettingsScreen() {
        super(Component.translatable(P + "title"));
        buildRows();
    }

    // ---------------- 行定义 ----------------

    @FunctionalInterface
    private interface RowBuild {
        int build(int x, int y, int w);
    }

    private record Row(int height, RowBuild build) {
    }

    private record Page(String labelKey, List<Row> rows) {
    }

    private void buildRows() {
        followRows.add(toggleRow("enableMod", OSConfig.ENABLED));
        followRows.add(toggleRow("takeover", OSConfig.TAKEOVER));
        followRows.add(toggleRow("autoShoulder", OSConfig.AUTO_SHOULDER));
        followRows.add(choiceRow("followMode", OSConfig.FOLLOW_MODE, new String[]{"SMOOTH", "SPRING", "FIXED"},
                v -> switch (v) {
                    case "SPRING" -> Component.translatable(V + "spring");
                    case "FIXED" -> Component.translatable(V + "fixed");
                    default -> Component.translatable(V + "smooth");
                }));
        followRows.add(sliderRow("transition", 0.02, 2.0, OSConfig.TRANSITION, SettingsScreen::fmtSec));
        followRows.add(sliderRow("stiffness", 10, 400, OSConfig.STIFFNESS, SettingsScreen::fmtInt));
        followRows.add(sliderRow("damping", 2, 80, OSConfig.DAMPING, SettingsScreen::fmtInt));
        followRows.add(sliderRow("offsetX", 0.05, 1.5, OSConfig.OFFSET_X, SettingsScreen::fmtBlock));
        followRows.add(sliderRow("offsetY", -0.5, 0.6, OSConfig.OFFSET_Y, SettingsScreen::fmtBlock));
        followRows.add(sliderRow("back", 0.25, 2.5, OSConfig.BACK, SettingsScreen::fmtBlock));
        followRows.add(sliderRow("slide", 0.02, 0.8, OSConfig.SLIDE, SettingsScreen::fmtSec));
        followRows.add(sliderRow("viewSwitchTime", 0.0, 0.8, OSConfig.VIEW_SWITCH, SettingsScreen::fmtSec));
        followRows.add(toggleRow("rightSide", OSConfig.RIGHT_SIDE));
        followRows.add(toggleRow("avoidWall", OSConfig.AVOID_WALL));
        followRows.add(toggleRow("fadePlayer", OSConfig.FADE_PLAYER));
        followRows.add(toggleRow("autoRiding", OSConfig.AUTO_RIDING));

        aimRows.add(toggleRow("zoom", OSConfig.ZOOM));
        // 拉近强度 = 1 - zoomFactor：滑块越大变焦越强，避免“1.0=不变焦”的反直觉陷阱
        aimRows.add(new Row(ROW, (x, y, w) -> slider(x, y, w, "zoomStrength", 0.0, 0.65,
                () -> 1.0 - OSConfig.ZOOM_FACTOR.get(),
                v -> OSConfig.ZOOM_FACTOR.set(clamp(1.0 - v, 0.35, 1.0)),
                SettingsScreen::fmtPct)));
        aimRows.add(sliderRow("aimTime", 0.02, 1.5, OSConfig.AIM_TIME, SettingsScreen::fmtSec));
        aimRows.add(toggleRow("followTacz", OSConfig.FOLLOW_TACZ));
        aimRows.add(toggleRow("push", OSConfig.PUSH));
        aimRows.add(sliderRow("pushDist", 0.0, 1.5, OSConfig.PUSH_DIST, SettingsScreen::fmtBlock));
        aimRows.add(toggleRow("zoomFp", OSConfig.ZOOM_FP));
        aimRows.add(sliderIntRow("aimDelay", 0, 20, OSConfig.AIM_DELAY,
                v -> Component.translatable(U + "ticks", fmtInt(v)).getString()));
        aimRows.add(toggleRow("deferTacz", OSConfig.DEFER_TACZ));

        sniperRows.add(toggleRow("forceFpSniper", OSConfig.FORCE_FP_SNIPER));
        sniperRows.add(toggleRow("scopeFade", OSConfig.SCOPE_FADE));
        sniperRows.add(sliderRow("fadeTime", 0.05, 1.0, OSConfig.FADE_TIME, SettingsScreen::fmtSec));
        aimRows.add(toggleRow("crosshair", OSConfig.CROSSHAIR));
        aimRows.add(toggleRow("debugCrosshair", OSConfig.DEBUG_CROSSHAIR));
        aimRows.add(toggleRow("autoElytra", OSConfig.AUTO_ELYTRA));
        aimRows.add(sliderIntRow("elytraDelay", 1, 200, OSConfig.ELYTRA_DELAY,
                v -> Component.translatable(U + "ticks", fmtInt(v)).getString()));
        aimRows.add(toggleRow("elytraRoll", OSConfig.ELYTRA_ROLL));
        aimRows.add(sliderIntRow("elytraRollStrength", 0, 100, OSConfig.ELYTRA_ROLL_STRENGTH,
                v -> fmtInt(v) + "°"));
        sniperRows.add(listBoxRow("sniperIds", OSConfig.SNIPER_IDS));
        sniperRows.add(listBoxRow("sniperKeywords", OSConfig.SNIPER_KEYWORDS));
        sniperRows.add(listBoxRow("gunMods", OSConfig.GUN_MODS));
        sniperRows.add(listBoxRow("ridingBlocklist", OSConfig.RIDING_BLOCKLIST));
    }

    private Row toggleRow(String key, ForgeConfigSpec.BooleanValue val) {
        return new Row(ROW, (x, y, w) -> toggle(x, y, w, key, val));
    }

    private Row choiceRow(String key, ForgeConfigSpec.ConfigValue<String> val, String[] options,
                          Function<String, Component> show) {
        return new Row(ROW, (x, y, w) -> choice(x, y, w, key, val, options, show));
    }

    private Row sliderRow(String key, double min, double max, ForgeConfigSpec.DoubleValue val,
                          Function<Double, String> fmt) {
        return new Row(ROW, (x, y, w) -> slider(x, y, w, key, min, max, val, fmt));
    }

    private Row sliderIntRow(String key, int min, int max, ForgeConfigSpec.IntValue val,
                             Function<Double, String> fmt) {
        return new Row(ROW, (x, y, w) -> sliderInt(x, y, w, key, min, max, val, fmt));
    }

    private Row listBoxRow(String key, ForgeConfigSpec.ConfigValue<List<? extends String>> val) {
        return new Row(LIST_ROW, (x, y, w) -> listBox(x, y, w, key, val));
    }

    // ---------------- 布局 ----------------

    @Override
    protected void init() {
        super.init();
        clearWidgets();
        syncers.clear();
        boxes.clear();

        int gap = PANEL_GAP;
        int maxPanel = Math.max(MIN_PANEL_W, (this.width - 44) / 3);
        panelW = Math.min(MAX_PANEL_W, maxPanel);
        int reserve = addonLoaded() ? BOTTOM_RESERVE_ADDON : BOTTOM_RESERVE;
        int wideNeededH = 26 + Math.max(followRowsHeight(), Math.max(aimRowsHeight(), sniperRowsHeight())) + reserve;
        wide = this.width >= MIN_PANEL_W * 3 + gap * 2 + 32 && this.height >= wideNeededH;

        if (wide) {
            int totalW = panelW * 3 + gap * 2;
            int x0 = (this.width - totalW) / 2;
            panelX0 = x0;
            panelX1 = x0 + panelW + gap;
            panelX2 = panelX1 + panelW + gap;
            contentTop = 24;
            buildColumn(panelX0, contentTop, panelW, followRows);
            buildColumn(panelX1, contentTop, panelW, aimRows);
            buildColumn(panelX2, contentTop, panelW, sniperRows);
        } else {
            int header = 20 + (needsTabs() ? TAB_H + 4 : 0);
            contentTop = header + 4;
            int availH = Math.max(ROW, this.height - contentTop - reserve);
            pages = paginate(availH);
            if (page >= pages.size()) page = 0;
            buildTabs(20);
            Page p = pages.get(page);
            int x = (this.width - panelW) / 2;
            buildColumn(x, contentTop, panelW, p.rows());
        }

        int by = this.height - 30;
        int bw = Math.min(panelW, 110);
        int leftX = wide ? panelX0 : (this.width - panelW) / 2;
        int rightX = wide ? panelX2 + panelW - bw : leftX + panelW - bw;
        addRenderableWidget(Button.builder(Component.translatable(P + "saveClose"), b -> {
            commitLists();
            OSConfig.saveNow();
            onClose();
        }).bounds(leftX, by, bw, 16).build());
        addRenderableWidget(Button.builder(Component.translatable(P + "reset"), b -> resetDefaults())
                .bounds(rightX, by, bw, 16).build());
    }

    private int followRowsHeight() {
        return rowsHeight(followRows);
    }

    private int aimRowsHeight() {
        return rowsHeight(aimRows);
    }

    private int sniperRowsHeight() {
        return rowsHeight(sniperRows);
    }

    private static int rowsHeight(List<Row> rows) {
        int h = 0;
        for (Row r : rows) h += r.height();
        return h;
    }

    private void buildColumn(int x, int y, int w, List<Row> rows) {
        int cy = y;
        for (Row r : rows) {
            cy = r.build().build(x, cy, w);
        }
    }

    private boolean needsTabs() {
        return true; // 单栏模式下无论页数都保留页签位置，避免布局跳动
    }

    private List<Page> paginate(int availH) {
        List<Page> out = new ArrayList<>();
        addSection(out, P + "section.follow", followRows, availH);
        addSection(out, P + "section.aim", aimRows, availH);
        addSection(out, P + "section.sniper", sniperRows, availH);
        if (out.isEmpty()) out.add(new Page(P + "section.follow", List.of()));
        return out;
    }

    private void addSection(List<Page> out, String labelKey, List<Row> rows, int availH) {
        List<Row> cur = new ArrayList<>();
        int used = 0;
        for (Row r : rows) {
            if (used + r.height() > availH && !cur.isEmpty()) {
                out.add(new Page(labelKey, List.copyOf(cur)));
                cur = new ArrayList<>();
                used = 0;
            }
            cur.add(r);
            used += r.height();
        }
        if (!cur.isEmpty()) out.add(new Page(labelKey, List.copyOf(cur)));
    }

    private void buildTabs(int y) {
        if (pages.size() <= 1) return;
        int total = pages.size();
        int gap = 4;
        int tw = Math.min(110, (this.width - 20 - gap * (total - 1)) / total);
        int totalW = tw * total + gap * (total - 1);
        int x = (this.width - totalW) / 2;
        for (int i = 0; i < total; i++) {
            final int idx = i;
            Page p = pages.get(i);
            Component label = Component.translatable(P + "pageTab",
                    Component.translatable(p.labelKey()), i + 1, total);
            Button b = Button.builder(label, bb -> {
                page = idx;
                rebuildWidgets();
            }).bounds(x, y, tw, 16).build();
            b.active = i != page;
            addRenderableWidget(b);
            x += tw + gap;
        }
    }

    // ---------------- 控件 ----------------

    private int toggle(int x, int y, int w, String key, ForgeConfigSpec.BooleanValue val) {
        Button[] holder = new Button[1];
        Runnable sync = () -> holder[0].setMessage(label(key).copy()
                .append(": ")
                .append(val.get() ? Component.translatable(V + "on") : Component.translatable(V + "off")));
        Button b = Button.builder(Component.empty(), bb -> {
            val.set(!val.get());
            OSConfig.refreshCache();
            if (val == OSConfig.RIGHT_SIDE) ViewManager.syncDefaultSide(OSConfig.CACHE);
            OSConfig.saveNow();
            sync.run();
        }).bounds(x, y, w, WIDGET_H).build();
        holder[0] = b;
        sync.run();
        addRenderableWidget(b);
        syncers.add(sync);
        return y + ROW;
    }

    private int choice(int x, int y, int w, String key, ForgeConfigSpec.ConfigValue<String> val,
                       String[] options, Function<String, Component> show) {
        Button[] holder = new Button[1];
        Runnable sync = () -> holder[0].setMessage(label(key).copy()
                .append(": ")
                .append(show.apply(val.get())));
        Button b = Button.builder(Component.empty(), bb -> {
            String cur = val.get();
            String next = options[0];
            for (int i = 0; i < options.length; i++) {
                if (options[i].equals(cur)) {
                    next = options[(i + 1) % options.length];
                    break;
                }
            }
            val.set(next);
            OSConfig.refreshCache();
            OSConfig.saveNow();
            sync.run();
        }).bounds(x, y, w, WIDGET_H).build();
        holder[0] = b;
        sync.run();
        addRenderableWidget(b);
        syncers.add(sync);
        return y + ROW;
    }

    private int slider(int x, int y, int w, String key, double min, double max,
                       ForgeConfigSpec.DoubleValue val, Function<Double, String> fmt) {
        return slider(x, y, w, key, min, max, val::get, v -> val.set(clamp(v, min, max)), fmt);
    }

    private int sliderInt(int x, int y, int w, String key, int min, int max,
                          ForgeConfigSpec.IntValue val, Function<Double, String> fmt) {
        return slider(x, y, w, key, min, max,
                () -> (double) val.get(),
                v -> val.set((int) Math.round(clamp(v, min, max))),
                fmt);
    }

    private int slider(int x, int y, int w, String key, double min, double max,
                       Supplier<Double> getter, Consumer<Double> setter, Function<Double, String> fmt) {
        Slide s = new Slide(x, y, w, key, min, max, getter, setter, fmt);
        addRenderableWidget(s);
        syncers.add(s::syncNow);
        return y + ROW;
    }

    private int listBox(int x, int y, int w, String key,
                        ForgeConfigSpec.ConfigValue<List<? extends String>> val) {
        addRenderableWidget(new StringWidget(x, y, w, 10, label(key), this.font).alignLeft());
        EditBox box = new EditBox(this.font, x, y + LABEL_H, w, 14, label(key));
        box.setMaxLength(1000);
        box.setValue(join(val.get()));
        addRenderableWidget(box);
        boxes.add(new BoxEntry(box, val));
        syncers.add(() -> box.setValue(join(val.get())));
        return y + LIST_ROW;
    }

    /** 文本框 → 配置列表（保存/关闭时提交） */
    private void commitLists() {
        for (BoxEntry e : boxes) {
            e.value.set(parse(e.box.getValue()));
        }
        OSConfig.refreshCache();
    }

    private record BoxEntry(EditBox box, ForgeConfigSpec.ConfigValue<List<? extends String>> value) {
    }

    private static Component label(String key) {
        return Component.translatable(O + key);
    }

    private static List<String> parse(String src) {
        Set<String> out = new LinkedHashSet<>();
        for (String p : src.split(",")) {
            String s = p.trim().toLowerCase(Locale.ROOT);
            if (!s.isEmpty()) out.add(s);
        }
        return List.copyOf(out);
    }

    private static String join(List<? extends String> list) {
        return String.join(", ", list);
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    // ---------------- 恢复默认 ----------------

    private void resetDefaults() {
        OSConfig.ENABLED.set(OSConfig.DEF_ENABLED);
        OSConfig.TAKEOVER.set(OSConfig.DEF_TAKEOVER);
        OSConfig.AUTO_SHOULDER.set(OSConfig.DEF_AUTO_SHOULDER);
        OSConfig.RIGHT_SIDE.set(OSConfig.DEF_RIGHT_SIDE);
        OSConfig.FOLLOW_MODE.set(OSConfig.DEF_FOLLOW_MODE);
        OSConfig.TRANSITION.set(OSConfig.DEF_TRANSITION);
        OSConfig.STIFFNESS.set(OSConfig.DEF_STIFFNESS);
        OSConfig.DAMPING.set(OSConfig.DEF_DAMPING);
        OSConfig.OFFSET_X.set(OSConfig.DEF_OFFSET_X);
        OSConfig.OFFSET_Y.set(OSConfig.DEF_OFFSET_Y);
        OSConfig.BACK.set(OSConfig.DEF_BACK);
        OSConfig.SLIDE.set(OSConfig.DEF_SLIDE);
        OSConfig.VIEW_SWITCH.set(OSConfig.DEF_VIEW_SWITCH);
        OSConfig.AVOID_WALL.set(OSConfig.DEF_AVOID_WALL);
        OSConfig.FADE_PLAYER.set(OSConfig.DEF_FADE_PLAYER);
        OSConfig.AUTO_RIDING.set(OSConfig.DEF_AUTO_RIDING);
        OSConfig.AUTO_ELYTRA.set(OSConfig.DEF_AUTO_ELYTRA);
        OSConfig.ELYTRA_DELAY.set(OSConfig.DEF_ELYTRA_DELAY);
        OSConfig.ELYTRA_ROLL.set(OSConfig.DEF_ELYTRA_ROLL);
        OSConfig.ELYTRA_ROLL_STRENGTH.set(OSConfig.DEF_ELYTRA_ROLL_STRENGTH);
        OSConfig.RIDING_BLOCKLIST.set(OSConfig.DEF_RIDING_BLOCKLIST);
        OSConfig.ZOOM.set(OSConfig.DEF_ZOOM);
        OSConfig.ZOOM_FACTOR.set(OSConfig.DEF_ZOOM_FACTOR);
        OSConfig.PUSH.set(OSConfig.DEF_PUSH);
        OSConfig.PUSH_DIST.set(OSConfig.DEF_PUSH_DIST);
        OSConfig.AIM_TIME.set(OSConfig.DEF_AIM_TIME);
        OSConfig.ZOOM_FP.set(OSConfig.DEF_ZOOM_FP);
        OSConfig.AIM_DELAY.set(OSConfig.DEF_AIM_DELAY);
        OSConfig.DEFER_TACZ.set(OSConfig.DEF_DEFER_TACZ);
        OSConfig.CROSSHAIR.set(OSConfig.DEF_CROSSHAIR);
        OSConfig.DEBUG_CROSSHAIR.set(OSConfig.DEF_DEBUG_CROSSHAIR);
        OSConfig.FOLLOW_TACZ.set(OSConfig.DEF_FOLLOW_TACZ);
        OSConfig.FORCE_FP_SNIPER.set(OSConfig.DEF_FORCE_FP_SNIPER);
        OSConfig.SCOPE_FADE.set(OSConfig.DEF_SCOPE_FADE);
        OSConfig.FADE_TIME.set(OSConfig.DEF_FADE_TIME);
        OSConfig.GUN_MODS.set(OSConfig.DEF_GUN_MODS);
        OSConfig.SNIPER_KEYWORDS.set(OSConfig.DEF_SNIPER_KEYWORDS);
        OSConfig.SNIPER_IDS.set(OSConfig.DEF_SNIPER_IDS);
        OSConfig.refreshCache();
        OSConfig.saveNow();
        syncers.forEach(Runnable::run);
    }

    @Override
    public void onClose() {
        commitLists();
        OSConfig.saveNow();
        super.onClose();
    }

    // ---------------- 渲染 ----------------

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (wide) {
            graphics.drawString(this.font, Component.translatable(P + "section.follow"), panelX0, 9, 0xFFF0A0, false);
            graphics.drawString(this.font, Component.translatable(P + "section.aim"), panelX1, 9, 0xFFF0A0, false);
            graphics.drawString(this.font, Component.translatable(P + "section.sniper"), panelX2, 9, 0xFFF0A0, false);
            int sep = 26;
            for (int i = 0; i < 2; i++) {
                int sx = (i == 0 ? panelX1 : panelX2) - (panelX1 - panelX0 - panelW) / 2 - 1;
                graphics.fill(sx, sep, sx + 1, this.height - 24, 0x60FFFFFF);
            }
            graphics.fill(panelX0, 18, panelX2 + panelW, 19, 0x40FFFFFF);
        } else {
            String title = pages.isEmpty() ? "" : Component.translatable(pages.get(page).labelKey()).getString();
            graphics.drawString(this.font, title, (this.width - this.font.width(title)) / 2, 6, 0xFFF0A0, false);
        }

        graphics.drawCenteredString(this.font, Component.translatable(P + "hint.keys"),
                this.width / 2, this.height - 13, 0xB0B0B0);
        if (addonLoaded()) {
            graphics.drawString(this.font, Component.translatable(P + "hint.addon"),
                    4, this.height - 42, 0xFF8080, false);
        }
    }

    private static boolean addonLoaded() {
        ModList list = ModList.get();
        return list != null && list.isLoaded("taczaddon");
    }

    @Override
    public boolean isPauseScreen() {
        // 单人游戏调参时暂停，避免边调边被打；多人游戏由服务端决定。
        return true;
    }

    // ---------------- 格式化 ----------------

    private static String fmtSec(double v) {
        return Component.translatable(U + "seconds", String.format(Locale.ROOT, "%.2f", v)).getString();
    }

    private static String fmtBlock(double v) {
        return Component.translatable(U + "blocks", String.format(Locale.ROOT, "%.2f", v)).getString();
    }

    private static String fmtPct(double v) {
        return Component.translatable(U + "percent", String.format(Locale.ROOT, "%.0f", v * 100)).getString();
    }

    private static String fmtInt(double v) {
        return String.format(Locale.ROOT, "%.0f", v);
    }

    // ---------------- 滑块控件 ----------------

    private static final class Slide extends AbstractSliderButton {
        private final String key;
        private final double min;
        private final double max;
        private final Supplier<Double> getter;
        private final Consumer<Double> setter;
        private final Function<Double, String> fmt;

        Slide(int x, int y, int w, String key, double min, double max,
              Supplier<Double> getter, Consumer<Double> setter,
              Function<Double, String> fmt) {
            super(x, y, w, WIDGET_H, Component.empty(), norm(getter.get(), min, max));
            this.key = key;
            this.min = min;
            this.max = max;
            this.getter = getter;
            this.setter = setter;
            this.fmt = fmt;
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(label(key).copy().append(": ").append(Component.literal(fmt.apply(raw()))));
        }

        @Override
        protected void applyValue() {
            setter.accept(raw());
            OSConfig.refreshCache();
        }

        @Override
        public void onRelease(double mouseX, double mouseY) {
            super.onRelease(mouseX, mouseY);
            // 拖动结束立即落盘，异常退出也不丢滑块值
            OSConfig.saveNow();
        }

        void syncNow() {
            this.value = norm(getter.get(), min, max);
            this.updateMessage();
        }

        private double raw() {
            return min + (max - min) * this.value;
        }

        private static double norm(double v, double min, double max) {
            if (max <= min) return 0;
            return (v - min) / (max - min);
        }
    }
}
