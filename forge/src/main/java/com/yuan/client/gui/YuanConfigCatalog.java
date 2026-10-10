package com.yuan.client.gui;

import com.yuan.item.YuanConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class YuanConfigCatalog {
    public enum Detail { CONCISE, PRACTICAL, REFERENCE }
    public enum Kind { BOOLEAN, NUMBER, INTEGER, ENUM }

    public record Setting(String key, String category, String label, Kind kind,
                          float min, float max, float step, float defaultValue,
                          String purpose, String operation, String effect,
                          String dependency, String warning, String recommendation,
                          String keywords, boolean dangerous, String group) {
        public List<String> explain(Detail detail) {
            List<String> lines = new ArrayList<>();
            lines.add(purpose);
            if (detail != Detail.CONCISE) {
                if (!operation.isEmpty()) lines.add("操作: " + operation);
                lines.add("默认: " + format(defaultValue));
                if (!effect.isEmpty()) lines.add("调节: " + effect);
            }
            if (detail == Detail.REFERENCE) {
                if (!dependency.isEmpty()) lines.add("依赖: " + dependency);
                if (!warning.isEmpty()) lines.add("警告: " + warning);
                if (!recommendation.isEmpty()) lines.add("建议: " + recommendation);
                lines.add("配置键: " + key);
            }
            return lines;
        }

        public boolean matches(String query) {
            String q = query.toLowerCase(Locale.ROOT).trim();
            return q.isEmpty() || (label + " " + purpose + " " + operation + " " + keywords + " " + key)
                    .toLowerCase(Locale.ROOT).contains(q);
        }
    }

    private static final List<Setting> ALL = List.of(
            bool(YuanConfig.K_SNIPE, "攻击", "射线狙击", true, "Shift+左键发射远距离射线。", "手持虚渊并按 Shift+左键。", "需要启用攻击功能。", "适合远距离单体目标。", "狙击 64格 shift 左键"),
            bool(YuanConfig.K_LIGHTNING, "攻击", "连锁闪电", true, "攻击命中后产生连锁闪电。", "普通攻击或范围攻击命中。", "需要有效目标。", "适合密集目标。", "闪电 连锁"),
            bool(YuanConfig.K_SWEEP, "攻击", "5格横扫", true, "普通攻击同时影响周围目标。", "左键攻击。", "与当前攻击模式共同生效。", "目标密集时启用。", "横扫 范围"),
            bool(YuanConfig.K_RIGHT_AOE, "攻击", "右键 AOE", true, "右键立即攻击周围目标并支持蓄力。", "按住或点击右键。", "范围由 AOE 范围控制。", "大范围会影响更多实体。", "右键 aoe 范围"),
            bool(YuanConfig.K_RIGHT_LIGHTNING_ENABLED, "攻击", "右键攻击闪电", true, "右键攻击成功命中后生成独立的视觉闪电。", "点击、持续、走廊和蓄力右键攻击。", "仅影响视觉，不造成伤害或点火。", "关闭可减少大量实体同步。", "右键 闪电 visual lightning"),
            new Setting(YuanConfig.K_RIGHT_LIGHTNING_COUNT_MODE, "攻击", "右键闪电数量模式", Kind.ENUM,
                    0, 1, 1, 0, "选择固定数量或按本次实际命中目标数生成。", "右键攻击命中后生效。",
                    "按命中数时，每个目标生成本次命中数道闪电。", "需要启用右键攻击闪电。",
                    "无总量上限；命中 512 个目标会生成 262,144 道闪电，可能卡死服务器或客户端。",
                    "固定数量更稳定。", "右键 闪电 数量 命中 262144", true, ""),
            integer(YuanConfig.K_RIGHT_LIGHTNING_COUNT, "攻击", "右键闪电固定数量", 1, 128, 1, 1,
                    "固定数量模式下每个目标生成的闪电数。", "数量模式选择固定数量时生效。", "数值越高视觉和同步开销越大。"),
            number(YuanConfig.K_RIGHT_LIGHTNING_SPREAD, "攻击", "右键闪电散布半径", 0, 128, 1, 0,
                    "闪电围绕命中目标随机散布的最大半径。", "右键闪电生成时生效。", "0 落在目标位置，增大后覆盖更大区域。"),
            bool(YuanConfig.K_RIGHT_LIGHTNING_SOUND, "攻击", "右键闪电雷声", true, "控制右键视觉闪电是否播放雷声。", "右键闪电生成时生效。", "关闭后闪电保持可见但静音。", "大量闪电时建议关闭。", "右键 闪电 雷声 sound"),
            bool(YuanConfig.K_WORLD_KILL, "范围能力", "全维秒杀", true, "蓄力 20 tick 释放时扩大当前维度攻击范围，80 tick 时攻击跨维度已加载目标。", "完成右键蓄力后释放。", "需要右键攻击流程。", "会影响极大范围，请谨慎启用。", "仅在确认目标范围时使用。", "全维 世界 秒杀 cross dimension"),
            enumSetting(YuanConfig.K_KILL_STRENGTH, "攻击", "诛灭强度", 0, 3, 0, "选择诛灭模式的处理强度。", "0秒杀，1半血，2保留10%，3击退。", "数值越大越偏向非致命控制。"),
            bool(YuanConfig.K_HIT11, "攻击", "11连击", true, "诛灭时执行连续伤害链。", "左键命中。", "依赖诛灭攻击。", "用于提高兼容性和击杀确定性。", "11 hit"),
            bool(YuanConfig.K_STRIP, "攻击", "吸收剥离", true, "攻击前移除目标吸收生命。", "命中自动执行。", "依赖攻击。", "用于处理高吸收目标。", "吸收 护盾"),
            bool(YuanConfig.K_REFLECT, "攻击", "反射直写", true, "使用反射补充写入生命值。", "攻击链自动执行。", "依赖诛灭攻击。", "兼容特殊实体时启用。", "反射 生命"),
            number(YuanConfig.K_TORMENT_PCT, "攻击", "凌迟比例", 1, 99, 5, 50, "凌迟模式后目标保留的生命百分比。", "切换到凌迟模式后攻击。", "数值越低伤害越高。"),
            number(YuanConfig.K_RUIN_POWER, "攻击", "崩坏倍率", 1, 100, 1, 10, "崩坏模式的击退强度。", "切换到崩坏模式后攻击。", "数值越高击退越强。"),
            bool(YuanConfig.K_RUIN_EXPLODE, "攻击", "崩坏爆炸粒子", true, "崩坏命中时显示爆炸粒子。", "崩坏模式命中。", "仅影响视觉。", "关闭可减少视觉干扰。", "爆炸 粒子"),
            bool(YuanConfig.K_OBLIVION_DROP, "攻击", "寂灭掉落", false, "寂灭目标时保留掉落。", "寂灭模式命中。", "依赖寂灭模式。", "关闭时清除更彻底。", "掉落 寂灭"),
            bool(YuanConfig.K_OBLIVION_DEATH, "攻击", "寂灭死亡动画", false, "寂灭目标时保留死亡动画。", "寂灭模式命中。", "依赖寂灭模式。", "关闭时立即移除。", "死亡 动画"),
            bool(YuanConfig.K_ABSOLUTE_DROP, "攻击", "绝对抹除掉落", false, "绝对抹除时保留目标掉落。", "绝对模式命中。", "依赖绝对抹除模式。", "默认关闭以保持彻底清除。", "绝对 掉落"),
            enumSetting(YuanConfig.K_ABSOLUTE_REENTRY, "攻击", "绝对重入策略", 0, 2, 0, "选择绝对抹除后的重入限制。", "0仅实体，1会话，2永久。", "数值越高限制越持久。", true, "reentry 永久重入"),
            enumSetting(YuanConfig.K_ATTACK_ATTRIBUTE_MODE, "攻击", "攻击属性", 0, 2, 0, "选择原始、有限极高或无限攻击属性。", "装备虚渊时生效。", "绝对抹除不依赖此属性。", true, "attack attribute infinity 无限攻击"),
            bool(YuanConfig.K_ATTACK_PLAYERS, "攻击", "攻击玩家", false, "允许将玩家作为攻击目标。", "所有虚渊攻击共享。", "默认关闭。", "多人服务器谨慎启用。", "玩家 pvp", true),
            bool(YuanConfig.K_ATTACK_ALLIES, "攻击", "攻击友军", false, "允许攻击同队友军。", "所有虚渊攻击共享。", "独立于玩家过滤。", "默认关闭。", "友军 队伍"),
            bool(YuanConfig.K_ATTACK_TAMED, "攻击", "攻击驯服生物", false, "允许攻击驯服生物。", "所有虚渊攻击共享。", "独立目标过滤。", "默认关闭。", "宠物 驯服"),
            bool(YuanConfig.K_ATTACK_VILLAGERS, "攻击", "攻击村民", false, "允许攻击村民。", "所有虚渊攻击共享。", "独立目标过滤。", "默认关闭。", "村民"),
            bool(YuanConfig.K_ATTACK_BOSSES, "攻击", "攻击 Boss", true, "允许攻击 Boss。", "所有虚渊攻击共享。", "Boss 仍遵循专用死亡流程。", "默认开启。", "boss 首领"),
            integer(YuanConfig.K_MAX_ATTACK_TARGETS, "范围能力", "最大攻击目标", 1, 4096, 64, 512, "限制一次范围攻击处理的实体数。", "范围攻击时生效。", "降低可限制单次处理成本。"),
            bool(YuanConfig.K_CORRIDOR_BLOCK_CLIP, "范围能力", "走廊遮挡检查", true, "视线走廊攻击检查方块遮挡。", "松开右键时生效。", "依赖走廊释放。", "默认开启。", "走廊 遮挡 clip"),
            number(YuanConfig.K_CORRIDOR_DISTANCE, "范围能力", "走廊距离", 1, 128, 1, 27, "视线走廊攻击距离。", "松开右键时生效。", "数值越高走廊越长。"),
            number(YuanConfig.K_CORRIDOR_RADIUS, "范围能力", "走廊半径", .5f, 16, .5f, 3, "视线走廊固定半径。", "松开右键时生效。", "数值越高走廊越宽。"),
            enumSetting(YuanConfig.K_RELEASE_MODE, "范围能力", "释放模式", 0, 3, 3, "选择关闭、立即、蓄力或两者。", "松开右键时生效。", "默认保留蓄力并启用走廊。", false, "release mode corridor"),
            number(YuanConfig.K_PURGE_RANGE, "范围能力", "潜行清除范围", 1, 1024, 1, 400, "潜行松开时当前维度球形清除半径。", "潜行释放右键。", "范围越大处理目标越多。"),
            bool(YuanConfig.K_INVINCIBLE, "防御", "持剑无敌", true, "手持虚渊时阻止伤害。", "主手持有虚渊。", "需要服务端配置同步。", "会显著改变生存体验。", "无敌 防御"),
            bool(YuanConfig.K_COUNTER, "防御", "反伤秒杀", true, "受到攻击时反击攻击者。", "手持虚渊并受到攻击。", "依赖持剑状态。", "可能击杀误伤来源。", "反伤 反击"),
            bool(YuanConfig.K_FLIGHT, "防御", "自动飞行", true, "手持虚渊时授予飞行。", "主手持有虚渊。", "离开持剑状态时回收。", "生存模式移动辅助。", "飞行 mayfly"),
            enumSetting(YuanConfig.K_DEFENSE_SCOPE, "防御", "最高防御作用域", 0, 2, 2, "选择主手、双手或原生背包。", "持有虚渊时生效。", "默认检查整个原生背包。", false, "defense scope"),
            bool(YuanConfig.K_DEFENSE_BLOCKING, "防御", "持续格挡", true, "使用虚渊时维持原生格挡保护。", "按住使用键。", "依赖最高防御。", "默认开启。", "格挡 blocking"),
            bool(YuanConfig.K_DEFENSE_ATTACK, "防御", "攻击事件防御", true, "取消普通攻击事件。", "最高防御生效时。", "绝对攻击可穿透。", "默认开启。", "攻击 event"),
            bool(YuanConfig.K_DEFENSE_HURT, "防御", "实际伤害防御", true, "在实际伤害入口提供兜底。", "最高防御生效时。", "绝对攻击可穿透。", "默认开启。", "伤害 hurt"),
            bool(YuanConfig.K_DEFENSE_HEALTH, "防御", "真实生命恢复", true, "恢复非法或被直写的生命值。", "服务端检查生命时。", "依赖最高防御。", "默认开启。", "生命 health"),
            bool(YuanConfig.K_DEFENSE_DEATH, "防御", "死亡防御", true, "阻止普通死亡及其掉落经验。", "死亡流程触发时。", "绝对攻击可穿透。", "默认开启。", "死亡 death"),
            bool(YuanConfig.K_DEFENSE_REMOVAL, "防御", "攻击性移除防御", true, "阻止攻击性 KILLED 和 DISCARDED 移除。", "攻击性移除时。", "维度切换、卸载、重生、退出和允许的管理命令放行。", "默认开启。", "移除 removal"),
            bool(YuanConfig.K_DEFENSE_KNOCKBACK, "防御", "击退免疫", true, "免疫标准攻击击退。", "受到击退时。", "依赖最高防御。", "默认开启。", "击退 knockback"),
            bool(YuanConfig.K_DEFENSE_PUSH, "防御", "碰撞推动免疫", true, "免疫实体碰撞推动。", "实体碰撞时。", "不阻止主动移动。", "默认开启。", "推动 push"),
            bool(YuanConfig.K_DEFENSE_FIRE, "防御", "清除火焰", true, "持续清除着火状态。", "服务端防御检查时。", "依赖最高防御。", "默认开启。", "火焰 fire"),
            bool(YuanConfig.K_DEFENSE_AIR, "防御", "恢复空气", true, "持续恢复空气值。", "服务端防御检查时。", "依赖最高防御。", "默认开启。", "空气 air"),
            bool(YuanConfig.K_DEFENSE_FREEZE, "防御", "防止冻结", true, "清除冻结进度。", "服务端防御检查时。", "依赖最高防御。", "默认开启。", "冻结 freeze"),
            bool(YuanConfig.K_DEFENSE_FALL, "防御", "清除摔落距离", true, "清除摔落伤害累计距离。", "服务端防御检查时。", "依赖最高防御。", "默认开启。", "摔落 fall"),
            bool(YuanConfig.K_DEFENSE_HUNGER, "防御", "饥饿防御", true, "防止饥饿伤害。", "饥饿伤害触发时。", "依赖最高防御。", "默认开启。", "饥饿 hunger"),
            bool(YuanConfig.K_DEFENSE_SUFFOCATION, "防御", "窒息防御", true, "防止方块内窒息伤害。", "窒息伤害触发时。", "依赖最高防御。", "默认开启。", "窒息 suffocation"),
            bool(YuanConfig.K_DEFENSE_CLEANSE, "防御", "清除负面效果", true, "移除负面效果并保留正面效果。", "服务端防御检查时。", "依赖最高防御。", "默认开启。", "效果 cleanse"),
            bool(YuanConfig.K_DEFENSE_ABSORPTION, "防御", "恢复吸收生命", true, "恢复最高防御的吸收生命。", "服务端防御检查时。", "依赖最高防御。", "默认开启。", "吸收 absorption"),
            bool(YuanConfig.K_DEFENSE_VOID, "防御", "虚空救援", true, "低于安全高度时返回安全位置。", "服务端防御检查时。", "依赖最高防御。", "默认开启。", "虚空 void 救援"),
            enumSetting(YuanConfig.K_BINDING_MODE, "绑定", "绑定模式", 0, 3, 1, "选择关闭、敌对防缴械、灵魂绑定或绝对绑定。", "服务端权威绑定判定。", "默认仅阻止敌对缴械。", true, "binding mode absolute binding"),
            bool(YuanConfig.K_ALLOW_MANUAL_DROP, "绑定", "允许主动丢弃", true, "允许持有者主动丢弃武器。", "绑定启用时。", "合法转移策略。", "默认允许。", "丢弃 toss"),
            bool(YuanConfig.K_ALLOW_CONTAINER, "绑定", "允许放入容器", true, "允许持有者放入合法容器；容器托管保留原绑定，须实际拾取或明确绑定后才转移。", "绑定启用时。", "容器托管期间暂停自动召回，不推断接收者。", "默认允许。", "容器 container custody"),
            bool(YuanConfig.K_ALLOW_PLAYER_TRANSFER, "绑定", "允许交给玩家", true, "允许持有者转交其他玩家。", "绑定启用时。", "合法转移策略。", "默认允许。", "转移 transfer"),
            bool(YuanConfig.K_KEEP_ON_DEATH, "绑定", "死亡保留", true, "死亡时保留绑定武器。", "玩家死亡时。", "依赖绑定模式。", "默认开启。", "死亡 保留"),
            bool(YuanConfig.K_AUTO_RECALL, "绑定", "自动召回", true, "敌对丢失后自动查找并召回。", "宽限期后生效。", "依赖绑定模式。", "默认开启。", "召回 recall"),
            bool(YuanConfig.K_RESTORE_ON_LOGIN, "绑定", "重登恢复", true, "重登后恢复缺失的权威武器。", "玩家登录时。", "依赖绑定模式。", "默认开启。", "重登 login"),
            bool(YuanConfig.K_UNIQUE_WEAPON, "绑定", "唯一性检查", true, "同一武器 UUID 只保留权威实例。", "召回和恢复时。", "依赖绑定模式。", "默认开启。", "唯一 uuid"),
            integer(YuanConfig.K_RECALL_GRACE_TICKS, "绑定", "召回宽限", 0, 1200, 20, 40, "敌对丢失后的召回等待 tick。", "自动召回前。", "用于跨维度和同步窗口。"),
            integer(YuanConfig.K_BINDING_DEFENSE_GRACE_TICKS, "绑定", "防御延续", 0, 200, 5, 20, "疑似缴械后最高防御延续 tick。", "自动召回期间。", "默认 20 tick。"),
            bool(YuanConfig.K_BINDING_ADMIN_BYPASS, "绑定", "管理员放行", true, "允许管理员明确操作绕过绑定。", "管理员操作时。", "避免阻塞管理生命周期。", "默认开启。", "管理员 admin"),
            bool(YuanConfig.K_DROP_DAMAGE_PROTECTION, "绑定", "掉落武器防毁", true, "掉落虚渊免疫普通环境伤害。", "物品实体存在时。", "不阻止正常卸载。", "默认开启。", "掉落 防毁 damage"),
            bool(YuanConfig.K_DROP_CAN_DESPAWN, "绑定", "允许自然消失", false, "允许掉落虚渊自然消失。", "物品实体到期时。", "不影响区块卸载和停服。", "默认关闭。", "掉落 despawn"),
            bool(YuanConfig.K_DROP_VOID_RESCUE, "绑定", "掉落虚空救援", true, "掉落虚渊进入虚空时召回或移至安全位置。", "低于世界最低高度时。", "依赖绑定状态决定召回目标。", "默认开启。", "掉落 虚空 rescue"),
            bool(YuanConfig.K_BAN_LIST, "清除", "永久禁止重生", true, "寂灭目标后阻止其再次生成。", "寂灭模式攻击。", "依赖服务端封禁表。", "会持续影响世界实体生成。", "仅对明确需要永久清除的目标使用。", "ban 封禁 重生 persistent ban"),
            bool(YuanConfig.K_BAN_PERSIST, "清除", "跨重启保存", true, "将封禁表写入世界存档。", "封禁目标后自动保存。", "依赖永久禁止重生。", "关闭后重启可能恢复。", "持久化 存档 persistent ban", true),
            bool(YuanConfig.K_BAN_MARK, "清除", "封禁标记", true, "记录封禁目标标记。", "永久清除时自动执行。", "依赖永久禁止重生。", "便于管理封禁列表。", "标记 ban"),
            number(YuanConfig.K_REACH, "数值", "攻击距离", 1, 32, 1, 10, "近战攻击判定距离。", "左键攻击时生效。", "数值越高可攻击更远。"),
            number(YuanConfig.K_SPEED, "数值", "攻击速度", 10, 1000, 10, 100, "虚渊攻击速度属性。", "装备时生效。", "数值越高攻击间隔越短。"),
            number(YuanConfig.K_AOE_RANGE, "数值", "AOE 范围", 5, 500, 5, 30, "右键范围攻击半径。", "右键 AOE 启用时生效。", "范围过大会影响大量实体。"),
            bool(YuanConfig.K_GLASS_ENABLED, "液态玻璃", "启用玻璃", true, "显示 Tooltip ReGlass 背景。", "悬停虚渊时自动渲染。", "需要客户端 shader。", "关闭可使用原背景。", "玻璃 reglass"),
            glass(YuanConfig.K_GLASS_RADIUS, "圆角", 0, 30, 1, 12, "玻璃圆角半径。", "增大更圆润，减小更硬朗。"),
            glass(YuanConfig.K_GLASS_BLUR, "模糊半径", 0, 32, 1, 12, "背景高斯模糊半径。", "增大更柔雾，减小更清透。"),
            glass(YuanConfig.K_GLASS_TINT_R, "色调 R", 0, 255, 5, 0, "玻璃红色通道。", "与色调强度共同生效。"),
            glass(YuanConfig.K_GLASS_TINT_G, "色调 G", 0, 255, 5, 0, "玻璃绿色通道。", "与色调强度共同生效。"),
            glass(YuanConfig.K_GLASS_TINT_B, "色调 B", 0, 255, 5, 0, "玻璃蓝色通道。", "与色调强度共同生效。"),
            glass(YuanConfig.K_GLASS_TINT_ALPHA, "色调强度", 0, 100, 5, 0, "玻璃染色比例。", "0 为无色，增大颜色更明显。"),
            glass(YuanConfig.K_GLASS_SHADOW_EXPAND, "阴影扩展", 0, 60, 1, 30, "外阴影衰减范围。", "增大阴影更宽。"),
            glass(YuanConfig.K_GLASS_SHADOW_FACTOR, "阴影强度", 0, 100, 5, 25, "玻璃悬浮阴影强度。", "增大层次更明显。"),
            glass(YuanConfig.K_GLASS_SHADOW_X, "阴影 X", -20, 20, 1, 0, "阴影水平偏移。", "负值向左，正值向右。"),
            glass(YuanConfig.K_GLASS_SHADOW_Y, "阴影 Y", -20, 20, 1, 2, "阴影垂直偏移。", "负值向上，正值向下。"),
            glass(YuanConfig.K_GLASS_SHADOW_R, "阴影 R", 0, 255, 5, 0, "阴影红色通道。", "用于自定义阴影颜色。"),
            glass(YuanConfig.K_GLASS_SHADOW_G, "阴影 G", 0, 255, 5, 0, "阴影绿色通道。", "用于自定义阴影颜色。"),
            glass(YuanConfig.K_GLASS_SHADOW_B, "阴影 B", 0, 255, 5, 0, "阴影蓝色通道。", "用于自定义阴影颜色。"),
            glass(YuanConfig.K_GLASS_SHADOW_ALPHA, "阴影透明度", 0, 100, 5, 100, "阴影颜色不透明度。", "增大阴影颜色更实。"),
            glass(YuanConfig.K_GLASS_REF_THICKNESS, "折射厚度", 1, 60, 1, 20, "边缘折射区域厚度。", "增大影响区域更宽。"),
            glass(YuanConfig.K_GLASS_REF_FACTOR, "折射率", 1, 2, .05f, 1.4f, "玻璃介质折射率。", "增大背景弯折更强。"),
            glass(YuanConfig.K_GLASS_DISPERSION, "色散", 0, 20, .5f, 7, "边缘 RGB 分离程度。", "增大彩色边缘更明显。"),
            glass(YuanConfig.K_GLASS_FRESNEL_RANGE, "菲涅尔范围", 1, 100, 1, 30, "边缘反射覆盖范围。", "增大反射区域更宽。"),
            glass(YuanConfig.K_GLASS_FRESNEL_HARDNESS, "菲涅尔硬度", 0, 100, 5, 20, "边缘反射过渡硬度。", "增大边缘更锐利。"),
            glass(YuanConfig.K_GLASS_FRESNEL_FACTOR, "菲涅尔强度", 0, 100, 5, 20, "白色边缘反射强度。", "增大玻璃边缘更亮。"),
            glass(YuanConfig.K_GLASS_GLARE_RANGE, "高光范围", 1, 100, 1, 30, "ReGlass 高光覆盖范围。", "增大高光更宽。"),
            glass(YuanConfig.K_GLASS_GLARE_HARDNESS, "高光硬度", 0, 100, 5, 20, "ReGlass 高光过渡硬度。", "增大高光更集中。")
    );

    private YuanConfigCatalog() {}

    public static List<Setting> all() { return ALL; }

    public static Setting byKey(String key) {
        return ALL.stream().filter(s -> s.key.equals(key)).findFirst().orElse(null);
    }

    public static List<Setting> find(String query) {
        return ALL.stream().filter(s -> s.matches(query)).toList();
    }

    public static List<String> categories() {
        return ALL.stream().map(Setting::category).distinct().toList();
    }

    public static boolean enabled(String key, net.minecraft.nbt.CompoundTag config) {
        if (key.equals(YuanConfig.K_AOE_RANGE))
            return get(config, YuanConfig.K_RIGHT_AOE, true)
                    || releaseEnabled(config, 2) && get(config, YuanConfig.K_WORLD_KILL, true);
        if (key.equals(YuanConfig.K_WORLD_KILL)) return releaseEnabled(config, 2);
        if (key.equals(YuanConfig.K_RIGHT_LIGHTNING_COUNT_MODE)
                || key.equals(YuanConfig.K_RIGHT_LIGHTNING_SPREAD)
                || key.equals(YuanConfig.K_RIGHT_LIGHTNING_SOUND))
            return get(config, YuanConfig.K_RIGHT_LIGHTNING_ENABLED, true);
        if (key.equals(YuanConfig.K_RIGHT_LIGHTNING_COUNT))
            return get(config, YuanConfig.K_RIGHT_LIGHTNING_ENABLED, true)
                    && getInt(config, YuanConfig.K_RIGHT_LIGHTNING_COUNT_MODE, 0) == 0;
        if (key.equals(YuanConfig.K_CORRIDOR_DISTANCE) || key.equals(YuanConfig.K_CORRIDOR_RADIUS)
                || key.equals(YuanConfig.K_CORRIDOR_BLOCK_CLIP) || key.equals(YuanConfig.K_PURGE_RANGE))
            return (getInt(config, YuanConfig.K_RELEASE_MODE, 3) & 1) != 0;
        if (byKey(key) != null && byKey(key).category().equals("防御")
                && !key.equals(YuanConfig.K_INVINCIBLE) && !key.equals(YuanConfig.K_COUNTER)
                && !key.equals(YuanConfig.K_FLIGHT) && !key.equals(YuanConfig.K_DEFENSE_SCOPE)
                && !key.equals(YuanConfig.K_DEFENSE_BLOCKING))
            return get(config, YuanConfig.K_INVINCIBLE, true) || get(config, YuanConfig.K_DEFENSE_BLOCKING, true);
        if (byKey(key) != null && byKey(key).category().equals("绑定")
                && !key.equals(YuanConfig.K_BINDING_MODE)
                && !key.equals(YuanConfig.K_DROP_DAMAGE_PROTECTION)
                && !key.equals(YuanConfig.K_DROP_CAN_DESPAWN)
                && !key.equals(YuanConfig.K_DROP_VOID_RESCUE))
            return getInt(config, YuanConfig.K_BINDING_MODE, 1) > 0;
        if (key.equals(YuanConfig.K_BAN_PERSIST) || key.equals(YuanConfig.K_BAN_MARK))
            return get(config, YuanConfig.K_BAN_LIST, true);
        if (byKey(key) != null && byKey(key).category().equals("液态玻璃") && !key.equals(YuanConfig.K_GLASS_ENABLED))
            return get(config, YuanConfig.K_GLASS_ENABLED, true);
        return true;
    }

    public static boolean releaseEnabled(net.minecraft.nbt.CompoundTag config, int bit) {
        return (getInt(config, YuanConfig.K_RELEASE_MODE, 3) & bit) != 0;
    }

    public static String valueLabel(String key, int value) {
        String[] labels = switch (key) {
            case YuanConfig.K_ATTACK_ATTRIBUTE_MODE -> new String[]{"原始", "有限极高", "无限"};
            case YuanConfig.K_RELEASE_MODE -> new String[]{"关闭", "立即", "蓄力", "立即与蓄力"};
            case YuanConfig.K_DEFENSE_SCOPE -> new String[]{"仅主手", "双手", "原生背包"};
            case YuanConfig.K_BINDING_MODE -> new String[]{"关闭", "敌对缴械恢复", "灵魂绑定", "绝对绑定"};
            case YuanConfig.K_ABSOLUTE_REENTRY -> new String[]{"仅实体", "会话", "永久"};
            case YuanConfig.K_RIGHT_LIGHTNING_COUNT_MODE -> new String[]{"固定数量", "按命中数"};
            case YuanConfig.K_KILL_STRENGTH -> new String[]{"秒杀", "半血", "保留10%", "击退"};
            default -> null;
        };
        return labels != null && value >= 0 && value < labels.length ? labels[value] : Integer.toString(value);
    }

    public static List<Setting> visibleSettings(String category, String search, boolean modifiedOnly,
                                                 Set<String> favorites, Set<String> changed) {
        boolean searching = !search.isBlank();
        return ALL.stream()
                .filter(setting -> searching || category.equals("收藏") || category.equals("已修改")
                        || setting.category().equals(category))
                .filter(setting -> setting.group().isEmpty() || setting.key().equals(groupRepresentative(setting.group())))
                .filter(setting -> groupMatches(setting, search, modifiedOnly, favorites, changed))
                .filter(setting -> category.equals("收藏") ? groupFavorite(setting, favorites)
                        : category.equals("已修改") ? groupChanged(setting, changed) : true)
                .toList();
    }

    private static boolean groupMatches(Setting setting, String search, boolean modifiedOnly,
                                        Set<String> favorites, Set<String> changed) {
        if (setting.group().isEmpty())
            return (search.isBlank() || setting.matches(search)) && (!modifiedOnly || changed.contains(setting.key()));
        return ALL.stream().filter(member -> member.group().equals(setting.group()))
                .anyMatch(member -> (search.isBlank() || member.matches(search))
                        && (!modifiedOnly || changed.contains(member.key())));
    }

    public static boolean groupFavorite(Setting setting, Set<String> favorites) {
        return setting.group().isEmpty() ? favorites.contains(setting.key())
                : ALL.stream().anyMatch(member -> member.group().equals(setting.group()) && favorites.contains(member.key()));
    }

    public static boolean groupChanged(Setting setting, Set<String> changed) {
        return setting.group().isEmpty() ? changed.contains(setting.key())
                : ALL.stream().anyMatch(member -> member.group().equals(setting.group()) && changed.contains(member.key()));
    }

    public static void toggleFavorite(Setting setting, Set<String> favorites) {
        List<String> keys = setting.group().isEmpty() ? List.of(setting.key())
                : ALL.stream().filter(member -> member.group().equals(setting.group())).map(Setting::key).toList();
        if (groupFavorite(setting, favorites)) favorites.removeAll(keys); else favorites.addAll(keys);
    }

    private static String groupRepresentative(String group) {
        return ALL.stream().filter(setting -> setting.group().equals(group)).findFirst().map(Setting::key).orElse("");
    }

    public static List<Setting> groupSettings(String group) {
        return ALL.stream().filter(setting -> setting.group().equals(group)).toList();
    }

    private static Setting bool(String key, String category, String label, boolean def, String purpose,
                                String operation, String dependency, String recommendation, String keywords) {
        return new Setting(key, category, label, Kind.BOOLEAN, 0, 1, 1, def ? 1 : 0,
                purpose, operation, "开启时生效，关闭时跳过。", dependency, "", recommendation, keywords,
                false, "");
    }

    private static Setting bool(String key, String category, String label, boolean def, String purpose,
                                String operation, String dependency, String warning, String recommendation, String keywords) {
        return new Setting(key, category, label, Kind.BOOLEAN, 0, 1, 1, def ? 1 : 0,
                purpose, operation, "开启时生效，关闭时跳过。", dependency, warning, recommendation, keywords,
                !warning.isEmpty(), "");
    }

    private static Setting bool(String key, String category, String label, boolean def, String purpose,
                                String operation, String dependency, String recommendation, String keywords,
                                boolean dangerous) {
        return new Setting(key, category, label, Kind.BOOLEAN, 0, 1, 1, def ? 1 : 0,
                purpose, operation, "开启时生效，关闭时跳过。", dependency,
                dangerous ? "此选项具有高影响范围，请确认后启用。" : "", recommendation, keywords,
                dangerous, "");
    }

    private static Setting number(String key, String category, String label, float min, float max, float step,
                                  float def, String purpose, String operation, String effect) {
        return new Setting(key, category, label, Kind.NUMBER, min, max, step, def,
                purpose, operation, effect, "", "", "保持默认值可获得稳定行为。", label,
                false, "");
    }

    private static Setting integer(String key, String category, String label, int min, int max, int step, int def,
                                   String purpose, String operation, String effect) {
        return new Setting(key, category, label, Kind.INTEGER, min, max, step, def,
                purpose, operation, effect, "", "", "保持默认值可获得稳定行为。", label, false, "");
    }

    private static Setting enumSetting(String key, String category, String label, int min, int max, int def,
                                       String purpose, String operation, String effect) {
        return new Setting(key, category, label, Kind.ENUM, min, max, 1, def,
                purpose, operation, effect, "", "", "保持默认值可获得稳定行为。", label,
                false, "");
    }

    private static Setting enumSetting(String key, String category, String label, int min, int max, int def,
                                       String purpose, String operation, String effect, boolean dangerous,
                                       String keywords) {
        return new Setting(key, category, label, Kind.ENUM, min, max, 1, def,
                purpose, operation, effect, "", dangerous ? "包含高影响选项，请确认选择。" : "",
                "保持默认值可获得稳定行为。", keywords, dangerous, "");
    }

    private static Setting glass(String key, String label, float min, float max, float step, float def,
                                 String purpose, String effect) {
        return new Setting(key, "液态玻璃", label, Kind.NUMBER, min, max, step, def,
                purpose, "调整后可在右侧实时预览。", effect, "需要启用玻璃和客户端 shader。",
                "", "从 ReGlass 默认值开始调整。", "玻璃 reglass " + label, false, glassGroup(key));
    }

    private static boolean get(net.minecraft.nbt.CompoundTag config, String key, boolean fallback) {
        return config.contains(key) ? config.getBoolean(key) : fallback;
    }

    private static int getInt(net.minecraft.nbt.CompoundTag config, String key, int fallback) {
        return config.contains(key) ? config.getInt(key) : fallback;
    }

    private static String glassGroup(String key) {
        if (key.equals(YuanConfig.K_GLASS_TINT_R) || key.equals(YuanConfig.K_GLASS_TINT_G) || key.equals(YuanConfig.K_GLASS_TINT_B))
            return "tint";
        if (key.equals(YuanConfig.K_GLASS_SHADOW_R) || key.equals(YuanConfig.K_GLASS_SHADOW_G) || key.equals(YuanConfig.K_GLASS_SHADOW_B))
            return "shadow";
        return "";
    }

    private static String format(float value) {
        return value == (int)value ? Integer.toString((int)value) : String.format(Locale.ROOT, "%.2f", value);
    }
}
