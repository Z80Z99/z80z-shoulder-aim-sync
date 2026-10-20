# Shoulder Aim Sync

A client-side **over-the-shoulder camera** mod for **Minecraft 1.20.1 (Forge)**, with
soft compatibility for **TACZ (Timeless & Classics Guns: Zero)**.

[中文说明](#中文说明) · [License: MIT](LICENSE)

> Client-side only — install it on the client; it does not need to be on the server.

---

## Features

- **F5 takeover** — cycles `first person → over-the-shoulder → third person (back) → third person (front)`
- **Swap shoulders** — `X` by default, 0.15 s smooth slide with inertia, fully rebindable
  - Not `R`, to avoid clashing with TACZ's default reload key
- **Three camera follow modes** — `SMOOTH` (exponential), `SPRING` (damped, weighty), `FIXED` (no latency)
- **Aim zoom + body push** — FOV narrows and the camera moves toward the shoulder while aiming
- **Sniper force-first-person** — detected by TACZ gun type or keyword lists; scopes in to first person and snaps back on release, with an optional scope-fade
- **Shoulder crosshair** — TACZ only draws its crosshair in first person, so this mod draws one at the **projected bullet-impact point** (not the screen center)
- **Wall fade** — the player model fades out (body, armor, elytra) as the camera is pushed against a wall, instead of clipping through it
- **Auto shoulder** — when mounting a rideable, and when starting to glide with an elytra
- **Elytra screen roll** — the view banks while turning during gliding
- **Settings screen** — `K`; three columns on wide screens, paged single-column on narrow ones

## Default keys

| Action | Default |
| --- | --- |
| Cycle perspective | `F5` (taken over from vanilla) |
| Swap shoulders | `X` |
| Open/close settings | `K` |

## Configuration

Client config: `config/shoulder_aim_sync-client.toml` (or edit in-game via `K`).

| Key | Default | Description |
| --- | --- | --- |
| `general.enabled` | true | Master switch; off restores vanilla behavior |
| `general.takeoverF5` | true | Take over vanilla F5 cycling |
| `general.autoShoulder` | true | Enter shoulder view on world join (needs `takeoverF5`) |
| `general.autoShoulderRiding` | true | Auto shoulder when mounting (horses must be saddled) |
| `camera.rightSide` | true | Default to right shoulder |
| `camera.followMode` | SMOOTH | `SMOOTH` / `SPRING` / `FIXED` |
| `camera.transitionTime` | 0.25 | Follow transition (seconds) |
| `camera.springStiffness` / `springDamping` | 110 / 17 | Spring params (`SPRING` only) |
| `camera.shoulderOffsetX` / `shoulderOffsetY` | 0.55 / 0.08 | Shoulder offset (blocks) |
| `camera.backDistance` | 1.15 | Camera distance behind the player |
| `camera.shoulderSlideTime` | 0.15 | Shoulder-swap slide time |
| `camera.avoidWall` | true | Shrink camera when close to walls |
| `camera.fadePlayerWhenClose` | true | Fade the player model when the camera is pushed close |
| `aim.aimZoom` | true | Progressive aim zoom |
| `aim.zoomFactor` | 0.7 | Resulting FOV ratio (0.7 = 70 %) |
| `aim.aimPush` / `pushDistance` | true / 0.4 | Body push while aiming |
| `aim.aimTransitionTime` | 0.3 | Zoom / push time |
| `aim.zoomInFirstPerson` | false | Also zoom in first person (stacks with TACZ scope magnification) |
| `aim.aimHoldTicks` | 2 | Hold ticks before aim registers |
| `aim.deferToTaczScope` | true | Let TACZ handle zoom when a scope is attached |
| `aim.followTaczAimProgress` | false | Follow TACZ's aiming animation instead of `aimTransitionTime` |
| `aim.showShoulderCrosshair` | true | Draw the shoulder crosshair |
| `aim.debugCrosshair` | false | Debug overlay: screen-center cross + projection values |
| `sniper.forceFirstPersonSniper` | true | Force first person when scoped with a sniper |
| `sniper.scopeFade` / `scopeFadeTime` | true / 0.2 | Scope fade transition |
| `elytra.autoShoulderElytra` | true | Auto shoulder while gliding |
| `elytra.elytraStartDelayTicks` | 10 | Glide ticks before auto-switch |
| `elytra.elytraRollScreen` / `elytraRollStrength` | true / 20 | Glide banking roll |
| `compat.gunModIds` | `["tacz"]` | Namespaces treated as guns |
| `compat.sniperKeywords` | TACZ sniper IDs | Keyword matching for other gun mods |
| `compat.sniperGunIds` | `[]` | Exact sniper gun IDs |
| `compat.ridingEntityBlocklist` | `[]` | Entity IDs excluded from auto-shoulder-on-mount |

## Compatibility

### TACZ — Timeless & Classics Guns: Zero

- Guns are detected through the `IGun` interface plus the item's `GunId` NBT tag, so ammo,
  attachments and the gun smith table are never mistaken for guns.
- Aim state is read from TACZ's `IClientPlayerGunOperator` (`isAim()` / `getClientAimingProgress()`),
  so rebinding or toggle-to-aim keeps everything in sync.
- Sniper detection prefers the gun pack's own `type` field (`"sniper"`), which also covers custom
  gun packs. Defaults: `ai_awp`, `kar98`, `m107`, `m700`, `m95`, `springfield1873`.

### TACZ-addon

- Its `enableBetterAimCamera` (default **on**) switches to first person ~110 ms after you start
  aiming. While that is enabled, this mod's aim-zoom/push panel has little effect because the
  camera is already in first person. Disable that option if you want shoulder-view zoom.
- Do **not** enable `zoomInFirstPerson` together with TACZ scopes — the two magnifications stack.

### ShoulderSurfing

No dedicated integration; both mods take over the camera, so running them together is not recommended.

## Known limitations

- **Bullet/screen parallax** — the shoulder camera is offset sideways while TACZ bullets leave from
  the player's eyes. The crosshair is therefore drawn at the *projected impact point*, which is
  geometrically correct but means it is not at the screen center.
- **Hip-fire spread** — TACZ's `inaccuracy` scatters shots around the aim direction; that is a gun
  property, not crosshair error.
- **Recoil** — TACZ kicks the view on firing, so the screen direction and the bullet direction differ
  for a moment.
- Wall fade depends on Mixin; if injection ever fails the mod logs a warning and simply skips fading.

## Building

Requires **JDK 17** and **Gradle 8.x** (ForgeGradle 6 + MixinGradle).

```bash
./gradlew build
# output: build/libs/shoulder_aim_sync-<version>.jar
```

The first build downloads ForgeGradle and Minecraft dependencies; afterwards `--offline` works.

## Credits

- Wall-fade and auto-third-person ideas were informed by [Nimble](https://github.com/Snownee/Nimble)
  by Snownee (MIT). This mod's implementation is independent and extends it with shoulder offset,
  aim logic and bullet-projected crosshair.

## License

[MIT](LICENSE) © 2026 Z80Z99

---

## 中文说明

Forge 1.20.1 **纯客户端**越肩视角模组，软兼容 **TACZ（永恒枪械工坊：零）**。
装在客户端即可，不需要装在服务端。

### 功能

- **接管 F5**：`第一人称 → 越肩 → 第三人称后 → 第三人称前`
- **X 键换肩**：0.15 秒平滑横移，保留惯性，可改键（不用 R，避免与 TACZ 换弹冲突）
- **三种跟随模式**：平滑插值 / 弹簧阻尼 / 固定跟随
- **瞄准变焦 + 机身前探**
- **狙击强制第一人称**：按枪械类型识别，开镜切第一人称，松手恢复，带镜片黑框过渡
- **越肩准星**：按弹道命中点投影对齐，不画在屏幕中心
- **贴墙虚化**：镜头贴近时身体、盔甲、鞘翅一起淡出，不再穿模
- **自动越肩**：上坐骑、鞘翅滑翔时自动切换
- **鞘翅滚转**：滑翔转弯时画面倾斜
- **K 键设置界面**

### 默认键位

| 功能 | 键 |
| --- | --- |
| 切换视角 | F5 |
| 换肩 | X |
| 打开设置 | K |

### 兼容性要点

- 装了 **TACZ-addon** 且其 `enableBetterAimCamera` 保持开启时，瞄准会立刻切第一人称，
  本模组的越肩变焦面板基本不生效；想用越肩变焦请关掉 addon 该项。
- 同时开启“第一人称也变焦”与 TACZ 倍镜会双重放大，建议保持关闭。
- 与 **ShoulderSurfing** 都会接管相机，不建议同时安装。

### 构建

需要 JDK 17 + Gradle 8.x：`./gradlew build`，产物在 `build/libs/`。

### 许可

[MIT](LICENSE) © 2026 Z80Z99
