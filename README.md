<div align="center">

# ⚔️ Yuan · 原

**Minecraft Forge 1.20.1** 武器 / 防具 / 渲染模组 ——
神器之刃的宇宙星空渲染、世界时停、空间斩与时间回溯

[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-FF9900)](https://www.minecraft.net/)
[![Forge](https://img.shields.io/badge/Forge-47.4.20-00A7E1)](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html)
[![Java](https://img.shields.io/badge/Java-17-5382A1)]()
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)
[![Platform](https://img.shields.io/badge/平台-Windows-lightgrey)]()
[![Version](https://img.shields.io/badge/Mod-1.0.0-blue)]()

</div>

---

## 📑 目录

- [两把神剑](#两把神剑)
- [特性详解](#特性详解)
- [快速开始与安装](#快速开始与安装)
- [按键](#按键)
- [构建](#构建)
- [兼容性与已知限制](#兼容性与已知限制)
- [目录结构](#目录结构)
- [许可与致谢](#许可与致谢)

---

## ⚔️ 两把神剑

一个模组,两把定位截然相反的剑:**「原·神器之刃」炫技**,「原剑」秒杀。

| | ⚔️ 原·神器之刃 | 🗡️ 原剑 |
|---|---|---|
| 物品 ID | `yuan:yuan_god_sword` | `yuan:yuan_sword` |
| 定位 | 特效 / 控场 / 回溯 | 一刀流 / 秒杀 |
| 普通伤害 | 约 9 点(正常近战) | 高额伤害 |
| 渲染 | cosmic 星空 / 丝绸 / 隧道 / 晶格 / 原版 多风格 | 狐狸剑风格 Tooltip |
| 秒杀手段 | ❌ 无 | ✅ 五种攻击模式 |
| 专属技能 | 时停 · 空间斩 · 时间回溯 | 模式切换 |
| 配置界面 | G 键(Compose 三页界面) | G 键(传统界面) |
| 维护状态 | 🟢 活跃开发(主力) | 🔴 停更 · 仅维护 |

---

## ✨ 特性详解

### ⚔️ 原·神器之刃 `yuan:yuan_god_sword`

- **多套渲染风格**:宇宙星空(cosmic)/ 丝绸(silk)/ 隧道 / 晶格(Voronoi)/ 原版 / 关闭,
  逐剑 NBT 保存,随时切换
- **星空渲染**:ArcaneVortex(秘法涡流)风格 cosmic 星云星点,基于掩膜(mask)的动态几何烘焙;
  **Oculus / Iris 光影下同样正常显示**(延迟渲染管线)
- 丝绸 / 隧道 / 晶格 风格在光影下自动回退为原版(仅 cosmic 星空支持光影)
- **专属配置界面**:主手持剑按 **G** 打开,三页导航「时停 / 武器渲染 / 斩击」+ 子页 Tab,
  含真实贴图预览、搜索过滤、预设槽与逐页恢复默认
- ⚠️ **无秒杀手段**:伤害为普通近战数值,秒杀类攻击模式是**原剑专属**,本剑没有
- 🟢 **持续开发中**:本模组的主力维护对象,新功能与优化都会集中在这里

### 🗡️ 原剑 `yuan:yuan_sword`

- ☠️ **自带秒杀/湮灭手段(本模组唯一)**——手持时 **Shift + 滚轮** 切换攻击模式:

  | 模式 | 效果 |
  |---|---|
  | 诛灭(默认) | 一击必杀 |
  | 凌迟 | 血量减半 |
  | 崩坏 | 极大击退 |
  | 寂灭 | 直接移除 |
  | 绝对 | 绝对抹除(可拉黑目标、抑制掉落) |

- ⚠️ **神器之刃没有秒杀手段**:秒杀是本剑独有的能力
- 🔴 **已停更(仅维护)**:原剑是老式系统,不再新增功能,只处理严重缺陷;主力开发都在「神器之刃」上
- **Tooltip(悬停信息)样式参考秘法涡流(ArcaneVortex)的「狐狸剑」**:动态狐狸纹背景 +
  彩虹光晕环 + 液态玻璃质感的文字层(悬停时可看到)
- 老式交互:G 打开传统配置界面;G+Ctrl 一键全预设、G+Shift 循环切换预设

### ⏸️ 世界时停

- **触发**:右键(可改为 Shift+右键 或 自定义键 R),进入时停世界;玩家自身保持可动且无敌
- **完整冻结链**:实体 / 方块 / 流体 / 粒子 / 声音 / 天气 / 世界时间 / Boss AI / 光照,全部停住
- **局域网同步**:时停状态、冻结配置与施法者位置广播给其他玩家
- **高度自定义**(全部在 G 界面配置):

  | 维度 | 可调项 |
  |---|---|
  | 画面 | 黑白 / 复古 / 暗角 滤镜、灰度强度、动画 |
  | 光球 | 颜色(8 预设 + 自定义 RGB)、大小、数量、透明度、旋转 |
  | 音效 | 开关、音量 |
  | 冻结 | 范围(0–128 格或全场)、实体 / 方块 / 流体 / Boss AI 分类开关、是否冻结自己 |
  | 时长 | 手动或 1–60 秒自动结束;冷却 0–100 tick;触发方式 |

- **与 mega时停 等其它时停模组共存**(MixinExtras 兼容)

### ⚡ 空间斩

- 神剑**左键命中**目标时,在命中点生成一道 **3D 黑白斩击**(黑芯 + 白边发光),约 0.26s 淡出
- 刀光跟随目标、相机面朝向、命中点精确;Oculus/Iris 光影下走实体渲染同样可见
- 完整参数面板:黑芯 / 白边 / 发光颜色(RGB)、长度、宽度、厚度、扫开与淡出动画、深浅测试等

### ⏪ 时间回溯

- **触发**:背包中持有神剑时按 **H**(播放中再按 H 取消)
- **动画式回放**:方块逐个倒放、实体平滑回退、世界时间回滚,而非瞬间瞬移
- **健壮性**:防物品/容器复制、跨维度换算(下界/主世界、传送门对端)、距离半径过滤、
  死亡回溯避熔岩/虚空等危险点
- 记录窗口 1–600 秒可配置,播放秒数可调

### 🗡️ 装备套件

| 物品 | ID | 说明 |
|---|---|---|
| 原·神器之刃 | `yuan:yuan_god_sword` | 星空渲染 + 时停 + 空间斩 + 时间回溯 |
| 原剑 | `yuan:yuan_sword` | 高额伤害;秒杀攻击模式;狐狸剑风格 Tooltip |
| 原·初始之刃 | `yuan:yuan_origin_blade` | 原银金剑造型 |
| 斧 / 镐 / 弓 | `yuan:yuan_axe` / `yuan:yuan_pickaxe` / `yuan:yuan_bow` | YUAN 工具与远程武器 |
| 防具四件 | `yuan:yuan_helmet` … `yuan:yuan_boots` | YUAN 材料装甲 |

---

## 🚀 快速开始与安装

1. 准备好 **Forge 47.4.20**(Forge 1.20.1,要求 Java 17)
2. 下载 `yuan-forge-1.0.0-all.jar`(⚠️ **请用 `-all` 包**:内含 MixinExtras 运行时,JarJar 打包)
3. 放入游戏实例的 `mods/` 目录,启动即可

> 💡 可选装 Oculus / Embeddium 以获得光影;本模组对两者已做适配。

---

## ⌨️ 按键

### 神剑系(yuan:yuan_god_sword)

| 按键 | 功能 |
|---|---|
| **右键** | 触发时停(可配置为 Shift+右键 或 自定义键) |
| **G** | 打开专属配置界面 |
| **H** | 触发时间回溯;播放中再按取消 |
| **R** | 时停自定义触发键(在 G 界面选「自定义键」后生效) |

### 原剑系(yuan:yuan_sword)

| 按键 | 功能 |
|---|---|
| **Shift + 滚轮** | 切换攻击模式(诛灭 / 凌迟 / 崩坏 / 寂灭 / 绝对) |
| **G** | 打开传统配置界面 |
| **G + Ctrl** | 一键全预设 |
| **G + Shift** | 循环切换预设 |

> ⚠️ 原剑**没有时停**:世界时停是「神器之刃」专属技能(见下节)。原剑老式时停已于 2026-10-11 移除。

> 所有按键均可在 游戏内「选项 → 控制 → Yuan」中改绑。

---

## 🛠️ 构建

```powershell
# 必须使用 JDK 17(ForgeGradle 6 在更新 JDK 上 reobf 会失败)
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat :forge:build --console=plain
```

构建产物:`forge/build/libs/yuan-forge-1.0.0-all.jar`(部署用 `-all` 包)。

---

## 💬 兼容性与已知限制

| 类型 | 说明 |
|---|---|
| ✔️ 必要 | Forge `[47,48)`、Java 17、单人 / 局域网 |
| ✔️ 可选 | Iris / Oculus 光影、Embeddium |
| ✔️ 共存 | 可与 mega时停 等其它时停模组同装 |

**已知限制**

- 时间回溯的**动画回放**在局域网内其它玩家视角暂不可见(进行中)
- 时停中液体贴图动画暂未冻结

---

## 📦 目录结构

```
forge/src/main/java/com/yuan/
├── client/          # 客户端:cosmic 渲染、Compose 配置界面、粒子、渲染器
├── command/         # 命令
├── common/          # 通用组件(DataComponent)
├── event/           # 事件:武器事件、掉落保护、防御状态
├── item/            # 物品:神剑、工具、防具、NBT 配置
├── mixin/           # Mixin 注入(时停冻结链、cosmic 接管、回溯等)
├── network/         # 网络包
├── registry/        # 物品注册
├── space_slash/     # 空间斩特效(shader + 渲染 + 实体)
├── timerewind/      # 时间回溯(记录器/还原器/回放)
└── timestop/        # 世界时停(状态/着色器/粒子/渲染)
```

---

## 📜 许可与致谢

**许可**

- 代码:**MIT**,见 [LICENSE](LICENSE)
- 复用/移植的第三方内容(ArcaneVortex 贴图与 cosmic shader、megatimestop 音效与后处理 shader、
  Silk WebGL shader、paper-design/shaders 晶格渲染、Mojang 资源)**不适用 MIT**
  —— 详见 [THIRD_PARTY_NOTICES](THIRD_PARTY_NOTICES.md)

**致谢**

| 项目 | 来源 | 用途 |
|---|---|---|
| ArcaneVortex(秘法涡流) | 哔哩哔哩 UP主「洛谔谔」[(UID 3546888156481679)](https://space.bilibili.com/3546888156481679) | cosmic 星空贴图与 shader;原剑 Tooltip 参考其「狐狸剑」 |
| 超级时停(megatimestop) | 第三方模组 | 时停实现参考;时停音效与 the_world/rewind 后处理 shader |
| Silk WebGL shader | 开源 shader 项目 | 丝绸渲染移植来源 |
| paper-design/shaders | 开源项目(Apache-2.0) | 晶格(Voronoi)渲染移植来源 |

---

<div align="center">⭐ 如果喜欢,欢迎 Star —— 也欢迎在 Issues 提出建议</div>