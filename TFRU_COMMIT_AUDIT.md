# libVulpes-TFRU ahead commits 移植审计

## 范围与原则

- 目标分支：`zerozaki666/libVulpes-Continuation` 的 `unstable1_7`
- 目标基线：`0511d0ead3e0205ca6ae522262ef94a675f4042f`
- 来源分支：`kuzuanpa/libVulpes-TFRU` 的 `unstable1_7`
- 来源末端：`0396b3da36109918ad640ec814b448b901f6a552`
- `git rev-list --left-right --count unstable1_7...tfru/unstable1_7` 的结果为
  `0 32`，因此下表覆盖全部 32 条来源独有提交。

本次移植保留通用 bugfix、通用 API 和 AdvancedRocketry Continuation
实际需要的功能；不引入 GT6、TerraFirmaCraft、TFRU 整合包环境检测、
戴森球/戴森云，以及来源仓库中的本地二进制依赖。目标基线原有的
`before:gregtech` 仅是既存的 FML 加载顺序声明，不代表新增 GT
集成，故不作破坏性删除。

结论含义：

- **完整移植**：行为可直接用于 Continuation，只做代码整理或额外边界检查。
- **修改后移植**：保留目标行为，但为了兼容旧 API、移除整合包依赖或修复来源实现缺陷而重写。
- **部分移植**：同一提交同时包含通用内容与明确不需要的内容，只保留通用部分。
- **不移植**：只有 GT6/TFC/TFRU/戴森内容、仅有无关格式化，或会引入已知回归。

## 逐提交结论

| # | 来源 commit | 标题 | 结论 | 必要性、修改点与实际落地 |
|---:|---|---|---|---|
| 1 | `a4cfdee0724759aeaeed21fcd479c3507837ac47` | fix bug: GT tank can duplicate fluids; buckets cannot fill rockets anymore | 修改后移植 | libVulpes 侧实际只有构建系统更新，没有标题所述的玩法修复；保留 Anatawa ForgeGradle、Gradle 7.4.2 wrapper 和 HTTPS 仓库的现代化基础，移除 Aliyun、`mavenLocal()`、GT/AE2/IC2 Classic 依赖及旧发布插件。标题中的流体修复应由 AdvancedRocketry 仓库处理。 |
| 2 | `ac100162e167ebf30ad9725dbfff86aadddf9ad2` | Add NBTTag feature for Recipe XML Editing | 修改后移植 | 必要。去掉来源的 YAMCore `ItemDescriptor` 依赖，改用 1.7.10 原生 Item/Block registry 与 `JsonToNBT`；读取和写出都支持 compound NBT，并对 XML 特殊字符转义。 |
| 3 | `b08d3c841d031175281a8e1f8684a21db724f288` | change every divider to ";" ;versioning | 修改后移植 | 必要。新格式统一用分号，同时继续接受旧空格分隔格式；未移植无关 import/版本噪声。 |
| 4 | `3ff2f868dc9bfe94ee63b0ae500d3e2a177f8b41` | change every divider to ";" ,reduce warnings when MC starting | 修改后移植 | 必要。`itemStack` 使用 `split(";", 4)`，保证 SNBT 内的分号不会被继续拆开；Ore Dictionary 与 fluid 同样保留旧格式兼容，并减少成功配方刷屏。 |
| 5 | `49f36d99d3b471f9b526de771c53b2e4e7da9b15` | use OreDictName instead of directly "equals" when burn Dilithium | 修改后移植 | 必要。将 LibVulpes 自有双锂晶体以 `gemDilithium` 注册，供 AR 的 FTL 引擎按矿辞消耗；注册增加 null 检查。同期 `EmbeddedInventory` 槽位检查扩展为同时拒绝负数和越界索引。 |
| 6 | `71c8522e2b36fe1994ed50a7699404e7d7eeca31` | optimize PlanetSelecting Gui | 不移植 | libVulpes 侧只有 IC2 Classic/NEI 开发依赖改动，且用户模组环境使用 IC2 Experimental 2.2.827；行星 GUI 行为属于 AdvancedRocketry。 |
| 7 | `c0f635876b274c097c69b0ca799becf5bba8d233` | a API to let other mods use the Projector | 修改后移植 | 必要。新增 `IDummyMultiBlockRegisterer`、`DummyTileMultiBlock` 和投影器注册入口；注册列表、结构、allowable block、描述文本均增加 null/空值检查，并保持三组内部索引对齐。 |
| 8 | `5a7362d26856e6fa0aadf02d980342fbba7915ee` | fix bug | 完整移植 | 必要。第三方 dummy 注册入口保持为 static API；在此基础上增加晚注册即时生效和重复注册保护。 |
| 9 | `2915eb51ad6bdc8c101e7b1404e2634633dcb01f` | TFRU version tag | 不移植 | 仅 TFRU 版本/发布身份，不适用于 Continuation。 |
| 10 | `e83afd0880b56c2f1bd384dfd9bc22d20800f1e5` | add projector support for gregtech tileentities | 部分移植 | 不保留 gregapi、GT tile 或 schematic 数据；保留可供任何模组使用的 `BlockMeta.overrideName` 与通用 schematic NBT 持久化能力。 |
| 11 | `8be328bfc2ae7cf87eb232437444b5ceb47370da` | optimize gregtech tileentities projection | 部分移植 | 排除 GT/AE2 类型分支；保留通用投影描述聚合、空列表处理、实际 machine ID 与可见按钮 ID 对齐等修复。 |
| 12 | `d8c64c5e1a347473cfa791f30391e5f358bd3eb7` | fix bug | 完整移植 | 必要。dummy 的 structure 改为实例字段，避免不同注册项互相覆盖。 |
| 13 | `e7ec05fb421bf33bf33a1c29160e66b3f6baadd5` | allow multiblocks hidden in projector | 完整移植 | 必要。加入 `isVisibleInProjector` 与 getter；GUI、自动识别和网络选择都尊重隐藏状态。 |
| 14 | `3832edfa5cbb4717c43da693e6c97e84b634430d` | allow dummy multiblocks hidden in projector | 修改后移植 | 必要。API 使用 `List<DummyTileMultiBlock>`；dummy 与真实机器共享选择列表，但使用显式 null controller block 占位，dummy 内的 `c` 会跳过，避免来源实现的 `blockList`/`machineList` 错位崩溃。 |
| 15 | `989a9738ffda2ee34add021676058face68bec24` | not adding null input recipes(fix void outputs) | 修改后移植 | 必要。来源实现只忽略单个坏输入，可能把残缺配方注册成“虚空产物”；现在任一输入或输出无效都会拒绝整条配方，空输入/空输出同样拒绝。 |
| 16 | `b1b68282b00a8a8a4559e775946128160cbba7e6` | fix misplaced mainblock | 修改后移植 | 必要。dummy 注册推迟到 `FMLLoadCompleteEvent`，并采用可清空的 pending queue；load-complete 后的新注册会立即落地，避免重复注册或生命周期错位。 |
| 17 | `f79af5c50b5973bd060e89a3cfdd60d2e28f12bf` | fix l10n | 完整移植 | 必要。投影器三条操作提示加入英中本地化，不再硬编码英文。 |
| 18 | `591da9e3a05ced7d9d3ed706478f9a1113cbe467` | fix bug | 不移植 | 来源删除了 lib 侧 load-complete 自动注册并暴露可变 public 列表，是为了配合 AR 端手工遍历，会造成重复注册风险；Continuation 保持单一的库侧生命周期入口和受控 API。 |
| 19 | `f2d8a85362631f9453d1ebf06067e7e09e659855` | TFRU env detect; remove logging of Successful added recipe | 部分移植 | 保留移除“成功添加配方”刷屏；不加入任何 TFRU 环境检测。 |
| 20 | `fd2c2f8f2a9784ad71f0205fcccf10bf27a77f66` | use nullable checks to avoid bugs | 部分移植 | 来源包含大量注解、diamond 与 import churn。只移植实际边界检查：损坏 item NBT、缺失坐标、空结构、null block/list、无效槽位等；不引入 JetBrains annotations 运行/编译依赖。 |
| 21 | `380ee5445e34d96a4f1be17082df8cf5d02924b5` | fix warnings and nullable wrong; optimize projector | 部分移植 | 保留 projector 的 base position、structure、controller、machine ID、network payload 检查，清理 phantom TileEntity，并修正字符串比较。清理旧投影时使用与重建完全相同的倒序 Y 坐标公式，避免结构切换后留下 phantom。明确不保留全局 `doubleClick` 状态（会跨玩家/物品串扰），也不保留该提交误删可替换方块清理循环的回归。旧无参 wildcard API 继续保留，新 Character overload 默认委托旧方法。 |
| 22 | `584d11897252c92b4d68a8d079a31f9caeeece10` | bugfix | 不移植 | 仅 GT schematic try/catch、jar 与 `.gitignore` 变化；其异常后不前进索引的实现还有死循环风险。 |
| 23 | `017a7ef3947446bd4aeef7489bd43af6a8f18c1b` | bugfix | 修改后移植 | 只采用现代 Gradle 的 `archiveClassifier`/`destinationDirectory` API，并移除不兼容的 Grgit 构建依赖；不复制来源发布环境。 |
| 24 | `50e904782a7d1821a7104ae517a62c2ae0afdf8d` | Phantom Block translatable; Dyson Cloud | 不移植 | libVulpes 侧只有导入/格式变化，没有独立的可验证通用行为；戴森云明确排除。 |
| 25 | `39f7414e65b54a3e03750b03ff88ed9d9f354256` | fix bug | 完整移植 | 必要。`PacketEntity` 的 NBT 存在位修正为 `nbt != null && !nbt.hasNoTags()`，使布尔位和后续 payload 一致。 |
| 26 | `3ed3ce3cafa13ee8fe7b520eab72b77eda978673` | fix libraries | 不移植 | 来源把 YAMCore/GT jar 作为本地二进制依赖提交；本次原生重写已不需要 YAMCore，也禁止引入 GT jar。 |
| 27 | `00d48af6b5422b36b034daa65b3f3431c2f57bfb` | new skey render, add space dim to travel | 部分移植 | lib 侧仅保留独立的 phantom 可被树叶替换兼容。AR 最终不采用该提交的 SEAT 模型路径，因此同步排除 `models.SEAT`、seat renderer/bounding-box 配套；GT schematic 内容也排除。太空维度旅行由 AR 仓库单独审计。 |
| 28 | `bf1779239948e3b413b49e7ef822b9ae3b4de244` | sort version number | 修改后移植 | 版本提升到 Continuation `0.2.10`，产物名改为 `LibVulpes-Continuation`；修正 `@BUILD@` 前的多余点号，并忽略配置阶段生成的版本文本；不保留 TFRU artifact/publish 身份。 |
| 29 | `935f1b6880795f9385973602b290387677d3a556` | temporary archives current render | 完整移植 | 保留独立、低风险的 `Vector3F.toString()`，便于投影坐标诊断；不带入临时渲染归档。 |
| 30 | `6ffc95e4d098a7b71aa7d0c4f831d184c3e07291` | update readme, license | 修改后移植 | 新建 Continuation README；由于合并了 TFRU 的 AGPLv3 代码，主 `LICENSE` 使用 AGPLv3，同时以 `LICENSE-MIT` 保留原项目 MIT 文本与来源说明。 |
| 31 | `ebd6cdfc23a1c053b360716ee40916e8f79bc5f7` | update readme | 修改后移植 | README 改写为 Continuation 项目说明，不保留“整合包专用”定位。 |
| 32 | `0396b3da36109918ad640ec814b448b901f6a552` | add features describe in readme | 修改后移植 | 移植通用 Features：结构投影、XML/NBT 配方、基础结构/马达、机器基类与模块化 GUI；删除 GT6 描述，并明确列出 `gemDilithium` 支持和排除项。 |

## 关键兼容性修改

### 投影器 API

- 保留旧的 `TileMultiBlock#getAllowableWildCardBlocks()`，标记为 deprecated。
- 新的 `getAllowableWildCardBlocks(Character)` 默认委托旧无参方法，所以按旧
  libVulpes 编译的第三方模组不会因方法消失而 `NoSuchMethodError`，旧 override
  也仍可通过虚调用生效。
- AR Continuation 可以 override Character 版本以区分不同结构字符。
- dummy 注册时三组列表始终同索引；dummy 没有真实 controller block，遇到
  `c` 明确跳过，不会访问错误的 `blockList` 项。
- 所有来自物品 NBT 或网络的 machine ID、方向、层数和坐标都会先校验；
  方向只接受水平四向，服务端还会拒绝超出世界高度/坐标边界或距玩家
  64 格以上的投影原点，避免伪造网络包远程放置 phantom。
- `objectMouseOver` 可为 null/非方块；phantom TileEntity 也不再被无条件强转
  为 `TileSchematic`。

### XML 配方

- 新格式：`registryName;count;meta;{SNBT}`。
- `split(";", 4)` 保留 SNBT 中的分号；旧空格格式仍可读取。
- NBT 必须解析为 compound；错误 NBT、未知物品、空矿辞、非正数量会让整条
  配方被拒绝。
- 写出时保留输入和输出 ItemStack NBT，并转义 `&`、`<`、`>`。
- XML parser 禁止 DOCTYPE、外部通用实体和外部参数实体。
- Ore Dictionary 输出复制原 ItemStack，保留其 NBT。

### 构建系统

构建改造来自 `a4cfdee`、`017a7ef` 和 `bf17792`，但按 Continuation
重新收敛：

- Gradle wrapper 升级到 7.4.2，ForgeGradle 固定为维护中的
  `com.anatawa12.forge:ForgeGradle:1.2-1.1.1`，目标 Java 8，避免动态版本
  造成不可复现构建。
- 依赖配置从已移除的 `compile` 改为 `implementation`；IC2 仍锁定用户环境
  对应的 Experimental API 2.2.827。
- 使用 HTTPS Forge/CodeChicken/IC2 源；没有 Aliyun 镜像和
  `mavenLocal()`，不会把地域性或本机缓存设为构建必需条件。
- 删除已不兼容的 CurseGradle、build-scan、git-changelog、Grgit 发布链；
  保留 universal/deobf jar，并用 Gradle 7 的 archive API。
- `3ed3ce3` 的 YAMCore/GT 本地 jar、`71c8522` 的 IC2 Classic 和来源中的
  GT/AE2 开发依赖均未加入。

## 已排除内容核对

- 无 `gregapi`、GT tile、MultiTileEntity、YAMCore 或 ItemDescriptor import。
- 无 TerraFirmaCraft/TFRU 环境检测。
- 无 Dyson sphere/cloud 代码。
- 无来源仓库提交的 `libs/gregtech.jar` 或 `libs/YAMCore.jar`。
- 无全局 projector `doubleClick` 状态。
- 无仅一半实现的 SEAT model/renderer API。

## 验证记录

- `git diff --check HEAD`：通过，无空白错误。
- 使用 `java-parser` 解析 `src/main/java` 下全部 Java 源文件：0 个语法失败。
- 排除项扫描：代码/构建中未发现 gregapi、GT tile、MultiTileEntity、
  YAMCore、ItemDescriptor、TerraFirmaCraft、Dyson、JetBrains annotations、
  Aliyun、`mavenLocal()`、`doubleClick` 或 SEAT renderer 残留。命中的
  `before:gregtech` 是目标基线既存加载顺序；README 中的命中是排除说明和
  来源署名。
- 执行
  `GRADLE_USER_HOME=.gradle-lib ./gradlew --no-daemon tasks` 时，wrapper
  尝试下载 `gradle-7.4.2-bin.zip`，但当前沙箱报
  `java.net.SocketException: Network is unreachable`。Gradle 尚未进入配置、
  依赖解析或 Java 编译阶段，所以这不是编译失败，也不能声称完整构建通过。
- 未在本环境启动 Minecraft/Forge 客户端；运行时联调仍需在能下载 Gradle
  分发包与 1.7.10 开发依赖的环境完成。
