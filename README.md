# Z80Z 越肩视角与枪械瞄准 (Shoulder Aim Sync)

Minecraft 1.20.1 Forge **客户端**模组：把原版第三人称改成可左右换肩的越肩相机，并对 TACZ 枪械做瞄准、变焦与狙击开镜的软兼容。

Minecraft 1.20.1 · Forge 47.4.0 · Java 17 · 仅客户端

## 功能

- **接管 F5**：`第一人称 → 越肩 → 第三人称后 → 第三人称前`，切换带平滑过渡与模型淡入淡出（可调时长，0=瞬时）。
- **X 键换肩**：0.15 秒平滑横移，保留惯性；默认不用 R，避免与 TACZ 换弹冲突。
- **三种跟随模式**：平滑插值（SMOOTH）、弹簧阻尼（SPRING）、固定跟随（FIXED，无延迟）。
- **瞄准变焦 + 机身前探**：手持枪械瞄准时 FOV 渐进收窄、相机前移贴肩。
- **狙击强制第一人称**：按 TACZ 枪包类型识别，开镜切第一人称，松手恢复，带镜片黑框过渡。
- **越肩准星**：TACZ 只在第一人称绘制准星，本模组在越肩持枪时补一个，并按真实弹道命中点投影对齐。
- **贴墙虚化**：镜头被墙挤近时，身体、盔甲、鞘翅一起变半透明，不再穿模。
- **自动越肩**：上坐骑、鞘翅滑翔时自动切换，结束自动恢复。
- **鞘翅滚转**：滑翔转弯时画面随倾斜滚转。
- **K 键设置界面**：参数全部可调，宽屏三栏、窄屏单栏分页。

## 默认键位

| 功能 | 默认键 |
| --- | --- |
| 切换视角 | `F5`（接管原版键） |
| 换肩 | `X` |
| 打开/关闭设置 | `K` |

## 怎么玩

1. 把 jar 放进客户端 `mods` 文件夹（**不需要装服务端**）。
2. 进世界后默认自动进入越肩视角；按 `F5` 在四种视角间循环。
3. 按 `X` 左右换肩；按 `K` 打开设置，可调肩部偏移、跟随模式、变焦强度等。
4. 手持枪械按住右键瞄准：越肩下会看到 FOV 收窄、机身前探。
5. 狙击枪开镜会自动切第一人称，松手立刻回到越肩。
6. 上坐骑或开始滑翔时自动切越肩，下来/落地自动恢复。

## 配置

客户端配置：`config/z80z_shoulder_aim_sync-client.toml`，或游戏内按 `K` 修改。

| 键 | 默认 | 说明 |
| --- | --- | --- |
| `general.enabled` | true | 总开关，关闭后完全交还原版 |
| `general.takeoverF5` | true | 接管原版 F5 |
| `general.autoShoulder` | true | 进世界自动越肩（需开启接管 F5） |
| `general.autoShoulderRiding` | true | 上坐骑自动越肩（马需已上鞍） |
| `camera.followMode` | SMOOTH | `SMOOTH` / `SPRING` / `FIXED` |
| `camera.transitionTime` | 0.25 | 跟随过渡时间（秒） |
| `camera.springStiffness` / `springDamping` | 110 / 17 | 弹簧参数（仅 SPRING） |
| `camera.shoulderOffsetX` / `shoulderOffsetY` | 0.55 / 0.08 | 肩部左右/高度偏移（格） |
| `camera.backDistance` | 1.15 | 相机后退距离（格） |
| `camera.shoulderSlideTime` | 0.15 | 换肩横移时间（秒） |
| `camera.viewSwitchTime` | 0.25 | F5 视角切换平滑过渡（秒），0=瞬时 |
| `camera.avoidWall` | true | 贴墙自动收缩 |
| `camera.fadePlayerWhenClose` | true | 镜头贴近时虚化人物 |
| `aim.aimZoom` | true | 瞄准渐进变焦 |
| `aim.zoomFactor` | 0.7 | 变焦后 FOV 占原比例（界面以“拉近强度”呈现） |
| `aim.aimPush` / `pushDistance` | true / 0.4 | 机身前探 |
| `aim.aimTransitionTime` | 0.3 | 变焦/前探时间（秒） |
| `aim.zoomInFirstPerson` | false | 第一人称也变焦（会与 TACZ 倍镜叠乘） |
| `aim.aimHoldTicks` | 2 | 长按右键多少 tick 后算瞄准 |
| `aim.deferToTaczScope` | true | 装倍镜时把变焦交给 TACZ |
| `aim.followTaczAimProgress` | false | 跟随 TACZ 开镜动画而非自身计时 |
| `aim.showShoulderCrosshair` | true | 越肩准星 |
| `aim.debugCrosshair` | false | 准星调试显示 |
| `sniper.forceFirstPersonSniper` | true | 狙击开镜强制第一人称 |
| `sniper.scopeFade` / `scopeFadeTime` | true / 0.2 | 镜片黑框过渡 |
| `elytra.autoShoulderElytra` | true | 滑翔自动越肩 |
| `elytra.elytraStartDelayTicks` | 10 | 滑翔多少 tick 后触发 |
| `elytra.elytraRollScreen` / `elytraRollStrength` | true / 20 | 滑翔转弯滚转 |
| `compat.gunModIds` | `["tacz"]` | 哪些命名空间算枪械 |
| `compat.sniperKeywords` | TACZ 狙击 ID | 其他枪械模组的狙击关键词 |
| `compat.sniperGunIds` | `[]` | 精确指定狙击枪 ID |
| `compat.ridingEntityBlocklist` | `[]` | 骑乘自动越肩的实体黑名单 |

## 兼容性

- **TACZ（永恒枪械工坊：零）**：通过 `IGun` 接口和物品 NBT 的 `GunId` 识别枪械，弹药/配件/工作台不会被误判；瞄准状态读取 TACZ 的 `isAim()` 与开镜进度，改键或切换式瞄准也能同步；狙击识别优先用枪包自带的 `type` 字段，自定义枪包同样有效。
- **TACZ-addon**：其 `enableBetterAimCamera` 默认开启，会在瞄准时自动切第一人称，此时本模组的越肩变焦面板基本不生效；想用越肩变焦请关闭该项。
- **ShoulderSurfing**：两者都接管相机，不建议同时安装。

## 已知限制

- 越肩相机横向偏移，而 TACZ 弹道从眼睛发出，因此准星画在弹道命中点的投影位置、不在屏幕中心——这是几何正确行为。
- 腰射散布（TACZ 的 `inaccuracy`）会让弹着点围绕准星随机分布，属枪械属性。
- 开火后坐力会短暂踢动视角，此时画面朝向与弹道方向不完全一致。

## 安装

1. 安装 [Minecraft Forge 1.20.1-47.4.0](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html)。
2. 从 [Releases](https://github.com/Z80Z99/z80z-shoulder-aim-sync/releases) 下载最新的 `z80z-shoulder-aim-sync-<版本>.jar`。
3. 放入客户端 `mods` 文件夹，启动游戏。

模组是纯客户端实现，服务端不需要安装；TACZ 为可选依赖，不装也能用基础越肩功能。

## 从源码构建

需要 JDK 17，项目自带 Gradle Wrapper：

```bash
./gradlew build
```

产物位于 `build/libs/z80z-shoulder-aim-sync-<版本>.jar`。开发调试：`./gradlew runClient`。

## 环境

- Minecraft 1.20.1
- Forge 47.4.0
- Java 17
- 仅客户端

## 致谢

贴墙虚化与自动第三人称的实现思路参考了 [Nimble](https://github.com/Snownee/Nimble)（作者 Snownee，MIT 许可）；本模组的实现独立编写，并额外提供肩部偏移、瞄准逻辑与弹道投影准星。

## 许可证

[MIT](LICENSE) © 2026 Z80Z99
