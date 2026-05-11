package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.common.config.ShortVideoBusinessConfig;
import cn.gaifan.douyinOperations.module.shortvideo.service.ContrastVideoTemplateService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ContrastVideoTemplateServiceImpl implements ContrastVideoTemplateService {

    @Resource
    private ShortVideoBusinessConfig shortVideoBusinessConfig;

    @Override
    public Map<String, Object> getShotTemplate(String contrastType, int duration) {
        if ("social_identity_contrast".equals(contrastType)) {
            return buildSocialIdentityTemplate(duration);
        } else if ("personal_state_contrast".equals(contrastType)) {
            return buildPersonalStateTemplate(duration);
        }
        return Map.of("error", "未知反差类型: " + contrastType);
    }

    @Override
    public Map<String, Object> getComedyConfig(String contrastType) {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("contrastType", contrastType);

        List<Map<String, Object>> comedyPoints = new ArrayList<>();

        Map<String, Object> stage1 = new LinkedHashMap<>();
        stage1.put("stage", "前段（0-40%时长）");
        stage1.put("type", "细节搞笑");
        if ("social_identity_contrast".equals(contrastType)) {
            stage1.put("techniques", List.of(
                "霸总出门时绊了一下（差点摔倒）",
                "秘书露出「又来了」的无奈表情",
                "特写名牌手表但手上有老茧"
            ));
        } else {
            stage1.put("techniques", List.of(
                "头发乱到鸟都能筑巢",
                "照镜子被自己吓到",
                "孩子嫌弃的眼神特写"
            ));
        }
        comedyPoints.add(stage1);

        Map<String, Object> stage2 = new LinkedHashMap<>();
        stage2.put("stage", "中段（40-70%时长）");
        stage2.put("type", "动作搞笑");
        if ("social_identity_contrast".equals(contrastType)) {
            stage2.put("techniques", List.of(
                "挖地动作夸张如舞蹈",
                "挖到石头假装很重搬不动",
                "用公文包装菜回家"
            ));
        } else {
            stage2.put("techniques", List.of(
                "化妆过程中的夸张动作",
                "跟镜子较劲的搞笑表情",
                "衣服穿一半被孩子打断"
            ));
        }
        comedyPoints.add(stage2);

        Map<String, Object> stage3 = new LinkedHashMap<>();
        stage3.put("stage", "末段（70-100%时长）");
        stage3.put("type", "结局搞笑");
        if ("social_identity_contrast".equals(contrastType)) {
            stage3.put("techniques", List.of(
                "挖出的「金条」其实是巧克力",
                "咬一口发现是假的，表情崩溃",
                "对镜头眨眼+搞笑音效"
            ));
        } else {
            stage3.put("techniques", List.of(
                "老公/孩子看到变身后的惊讶表情",
                "变身女王但踩到猫/滑倒",
                "精致到不认识自己，对着镜头比心"
            ));
        }
        comedyPoints.add(stage3);

        config.put("comedyPoints", comedyPoints);
        config.put("tip", "笑点密度建议：每5-8秒至少一个微笑点，每15秒一个爆笑点");
        return config;
    }

    @Override
    public Map<String, Object> getBgmStrategy(String contrastType) {
        Map<String, Object> strategy = new LinkedHashMap<>();
        strategy.put("contrastType", contrastType);

        if ("social_identity_contrast".equals(contrastType)) {
            strategy.put("beforeTransition", Map.of(
                "style", "奢华钢琴曲",
                "tempo", "缓慢，高级感",
                "examples", List.of("Classical Piano Concerto", "Luxury Brand BGM"),
                "emotion", "高傲冷漠"
            ));
            strategy.put("transition", Map.of(
                "effect", "BGM突然中断，1秒静音",
                "purpose", "制造悬念和反差期待"
            ));
            strategy.put("afterTransition", Map.of(
                "style", "土味嗨曲",
                "tempo", "突然炸裂，节奏强烈",
                "examples", List.of("土味电音", "社会摇BGM"),
                "emotion", "搞笑释放"
            ));
        } else {
            strategy.put("beforeTransition", Map.of(
                "style", "喜感配乐",
                "tempo", "轻快搞笑",
                "examples", List.of("Funny Walk BGM", "卡通音效配乐"),
                "emotion", "轻松愉悦"
            ));
            strategy.put("transition", Map.of(
                "effect", "鼓点过渡，渐强",
                "purpose", "制造变身期待感"
            ));
            strategy.put("afterTransition", Map.of(
                "style", "气场音乐",
                "tempo", "大气磅礴",
                "examples", List.of("Fashion Show BGM", "Queen Walk BGM"),
                "emotion", "惊艳震撼"
            ));
        }

        strategy.put("visualContrast", Map.of(
            "before", "social_identity_contrast".equals(contrastType) ?
                "冷色调，硬光，突出高级感" : "凌乱场景，昏暗灯光",
            "after", "social_identity_contrast".equals(contrastType) ?
                "暖色调，自然光+补光，突出乡土感" : "整洁场景，明亮灯光"
        ));

        ShortVideoBusinessConfig.BgmVolume vol = shortVideoBusinessConfig.getBgmVolume();
        strategy.put("volumeCurve", Map.of(
            "frontVolume", vol.getFrontVolumeMin() + "-" + vol.getFrontVolumeMax() + "%",
            "backVolume", vol.getBackVolumeMin() + "-" + vol.getBackVolumeMax() + "%",
            "tip", "前段保持" + vol.getFrontVolumeMin() + "-" + vol.getFrontVolumeMax() + "%音量营造氛围，" +
                   "转场后提升至" + vol.getBackVolumeMin() + "-" + vol.getBackVolumeMax() + "%增强冲击力"
        ));

        return strategy;
    }

    @Override
    public List<Map<String, Object>> listTemplates() {
        List<Map<String, Object>> templates = new ArrayList<>();
        templates.add(Map.of(
            "type", "social_identity_contrast",
            "label", "社会身份反差",
            "examples", List.of("霸总→农村大姐", "CEO→快递小哥", "贵妇→工地大姐"),
            "difficulty", "中等",
            "expectedPerformance", Map.of("completionRate", ">70%", "likeRate", ">15%", "shareRate", ">8%")
        ));
        templates.add(Map.of(
            "type", "personal_state_contrast",
            "label", "个人状态反差",
            "examples", List.of("邋遢主妇→精致女王", "素颜→完美妆容", "运动风→晚礼服"),
            "difficulty", "低",
            "expectedPerformance", Map.of("completionRate", ">65%", "likeRate", ">12%", "shareRate", ">6%")
        ));
        return templates;
    }

    private Map<String, Object> buildSocialIdentityTemplate(int duration) {
        Map<String, Object> template = new LinkedHashMap<>();
        template.put("type", "social_identity_contrast");
        template.put("label", "社会身份反差（如：霸总→农村大姐）");
        template.put("totalDuration", duration);

        List<Map<String, Object>> shots = new ArrayList<>();
        int s1End = (int)(duration * 0.2);
        int s2End = (int)(duration * 0.4);
        int s3End = (int)(duration * 0.6);
        int s4End = (int)(duration * 0.8);

        shots.add(Map.of("timeRange", "0-" + s1End + "s", "visual", "豪华办公室，霸总冷脸签文件，特写名牌手表",
            "copy", "王总，这是本季度财报", "bgm", "奢华钢琴曲（缓慢、高级）", "mood", "高傲冷漠，眼神不屑"));
        shots.add(Map.of("timeRange", s1End + "-" + s2End + "s", "visual", "霸总起身走向门口，背影，门缓缓打开",
            "copy", "（无文案，让画面说话）", "bgm", "BGM突然中断，1秒静音", "mood", "神秘感，悬念制造"));
        shots.add(Map.of("timeRange", s2End + "-" + s3End + "s", "visual", "特效转场：穿过门瞬间变装，农村大姐造型",
            "copy", "返场！（大字特效）", "bgm", "土味嗨曲（突然炸裂）", "mood", "戏剧性转折，夸张表情"));
        shots.add(Map.of("timeRange", s3End + "-" + s4End + "s", "visual", "田间挖地，动作卖力，汗水特写",
            "copy", "还是挖地适合老子！", "bgm", "继续土味嗨曲+搞笑声效", "mood", "投入专注，带点自嘲"));
        shots.add(Map.of("timeRange", s4End + "-" + duration + "s", "visual", "挖出道具（金条/宝贝），对镜头眨眼",
            "copy", "诶？这地里还有宝贝？", "bgm", "金币声效+搞笑音效", "mood", "惊喜搞笑，表情夸张"));

        template.put("shots", shots);
        template.put("shootingTips", List.of(
            "前段灯光：冷色调，硬光，突出高级感",
            "后段灯光：暖色调，自然光+补光，突出乡土感",
            "转场特效：DaVinci Resolve闪光转场",
            "音频处理：前段音乐逐渐淡出，后段突然切入"
        ));
        return template;
    }

    @Override
    public Map<String, Object> getPresetTemplate(String contrastType, String preset) {
        return switch (preset) {
            case "15s" -> build15sTemplate(contrastType);
            case "25s" -> getShotTemplate(contrastType, 25);
            case "30s" -> build30sTemplate(contrastType);
            case "45s" -> build45sTemplate(contrastType);
            case "60s" -> build60sTemplate(contrastType);
            case "90s" -> getShotTemplate(contrastType, 90);
            default -> getShotTemplate(contrastType, 25);
        };
    }

    private Map<String, Object> build15sTemplate(String contrastType) {
        Map<String, Object> template = new LinkedHashMap<>();
        template.put("type", contrastType);
        template.put("preset", "15s");
        template.put("label", "15秒极速反差（适合卡点变装）");
        template.put("totalDuration", 15);

        List<Map<String, Object>> shots = new ArrayList<>();
        if ("social_identity_contrast".equals(contrastType)) {
            shots.add(Map.of("timeRange", "0-5s", "visual", "高端身份场景，冷漠表情", "copy", "（身份展示）", "bgm", "高级感BGM", "mood", "高傲"));
            shots.add(Map.of("timeRange", "5-7s", "visual", "转场特效，闪光/百叶窗", "copy", "返场！", "bgm", "静音1秒", "mood", "悬念"));
            shots.add(Map.of("timeRange", "7-12s", "visual", "反差身份，夸张动作", "copy", "这才是真正的我", "bgm", "嗨曲炸裂", "mood", "搞笑释放"));
            shots.add(Map.of("timeRange", "12-15s", "visual", "搞笑结尾，对镜头眨眼", "copy", "关注看更多反差", "bgm", "音效收尾", "mood", "引导关注"));
        } else {
            shots.add(Map.of("timeRange", "0-5s", "visual", "邋遢/素颜状态", "copy", "每天都这样...", "bgm", "搞笑配乐", "mood", "无奈"));
            shots.add(Map.of("timeRange", "5-7s", "visual", "决心变身，快速剪辑", "copy", "（蒙太奇）", "bgm", "鼓点加速", "mood", "期待"));
            shots.add(Map.of("timeRange", "7-12s", "visual", "精致亮相，慢动作", "copy", "惊不惊喜", "bgm", "气场音乐", "mood", "自信"));
            shots.add(Map.of("timeRange", "12-15s", "visual", "回头比心/眨眼", "copy", "姐妹你也可以", "bgm", "音乐高潮", "mood", "温暖"));
        }
        template.put("shots", shots);
        template.put("tip", "15秒视频节奏要快，每个镜头不超过4秒，卡点要精准");
        return template;
    }

    private Map<String, Object> build30sTemplate(String contrastType) {
        Map<String, Object> template = new LinkedHashMap<>();
        template.put("type", contrastType);
        template.put("preset", "30s");
        template.put("label", "30秒标准反差（含完整三段笑点）");
        template.put("totalDuration", 30);

        List<Map<String, Object>> shots = new ArrayList<>();
        if ("social_identity_contrast".equals(contrastType)) {
            shots.add(Map.of("timeRange", "0-6s", "visual", "豪华场景，高端身份展示", "copy", "王总，会议准备好了", "bgm", "奢华钢琴曲", "mood", "高傲冷漠"));
            shots.add(Map.of("timeRange", "6-10s", "visual", "细节搞笑：绊倒/秘书无奈表情", "copy", "（让观众发现笑点）", "bgm", "BGM继续", "mood", "细节搞笑"));
            shots.add(Map.of("timeRange", "10-13s", "visual", "走向门口，转场特效", "copy", "返场！", "bgm", "突然静音→嗨曲", "mood", "悬念→爆发"));
            shots.add(Map.of("timeRange", "13-20s", "visual", "反差身份，卖力干活", "copy", "还是这个适合我！", "bgm", "土味嗨曲", "mood", "动作搞笑"));
            shots.add(Map.of("timeRange", "20-25s", "visual", "挖到宝贝/发现意外", "copy", "诶？这还有好东西？", "bgm", "惊喜音效", "mood", "惊喜反转"));
            shots.add(Map.of("timeRange", "25-30s", "visual", "宝贝是假的，表情崩溃，对镜头眨眼", "copy", "关注我下次变什么", "bgm", "搞笑音效收尾", "mood", "结局搞笑"));
        } else {
            shots.add(Map.of("timeRange", "0-6s", "visual", "凌乱家中，蓬头垢面", "copy", "又是忙碌的一天", "bgm", "喜感配乐", "mood", "无奈搞笑"));
            shots.add(Map.of("timeRange", "6-10s", "visual", "搞笑日常：被孩子嫌弃", "copy", "妈你能不能洗个头", "bgm", "搞笑音效", "mood", "细节搞笑"));
            shots.add(Map.of("timeRange", "10-13s", "visual", "下定决心，开始变身", "copy", "今天，做自己！", "bgm", "鼓点过渡渐强", "mood", "决心"));
            shots.add(Map.of("timeRange", "13-20s", "visual", "快速化妆换装蒙太奇", "copy", "（蒙太奇剪辑）", "bgm", "节奏加快", "mood", "专注期待"));
            shots.add(Map.of("timeRange", "20-25s", "visual", "精致女王亮相，慢动作", "copy", "女人可以重新定义自己", "bgm", "气场音乐高潮", "mood", "自信震撼"));
            shots.add(Map.of("timeRange", "25-30s", "visual", "家人惊讶→踩到猫滑倒", "copy", "姐妹们你也可以！", "bgm", "搞笑音效+掌声", "mood", "搞笑收尾"));
        }
        template.put("shots", shots);
        template.put("tip", "30秒视频是反差变装的黄金时长，三段笑点必须精准卡住");
        return template;
    }

    private Map<String, Object> build45sTemplate(String contrastType) {
        Map<String, Object> template = new LinkedHashMap<>();
        template.put("type", contrastType);
        template.put("preset", "45s");
        template.put("label", "45秒完整版（含产品植入空间）");
        template.put("totalDuration", 45);

        List<Map<String, Object>> shots = new ArrayList<>();
        shots.add(Map.of("timeRange", "0-8s", "visual", "身份/状态建立，日常场景", "copy", "（角色建立）", "bgm", "前段BGM", "mood", "铺垫"));
        shots.add(Map.of("timeRange", "8-14s", "visual", "细节搞笑，加深印象", "copy", "（笑点1）", "bgm", "继续", "mood", "细节搞笑"));
        shots.add(Map.of("timeRange", "14-18s", "visual", "转折契机+转场特效", "copy", "够了！今天要改变！", "bgm", "转场音效", "mood", "转折"));
        shots.add(Map.of("timeRange", "18-26s", "visual", "变身过程+产品植入", "copy", "用了XX之后...", "bgm", "鼓点加速", "mood", "期待+植入"));
        shots.add(Map.of("timeRange", "26-35s", "visual", "完美亮相，慢动作展示", "copy", "这才是真正的我", "bgm", "高潮音乐", "mood", "震撼"));
        shots.add(Map.of("timeRange", "35-40s", "visual", "互动+搞笑结尾", "copy", "你们更喜欢哪个版本？", "bgm", "互动音效", "mood", "搞笑互动"));
        shots.add(Map.of("timeRange", "40-45s", "visual", "引导关注+预告", "copy", "关注我下期更精彩", "bgm", "收尾音乐", "mood", "引导"));

        template.put("shots", shots);
        template.put("tip", "45秒可以加入产品植入，变身过程是最佳植入时机");
        return template;
    }

    private Map<String, Object> build60sTemplate(String contrastType) {
        Map<String, Object> template = new LinkedHashMap<>();
        template.put("type", contrastType);
        template.put("preset", "60s");
        template.put("label", "60秒剧情版（含完整故事线）");
        template.put("totalDuration", 60);

        List<Map<String, Object>> shots = new ArrayList<>();
        shots.add(Map.of("timeRange", "0-10s", "visual", "环境交代+角色建立", "copy", "（开场白/旁白）", "bgm", "前段BGM", "mood", "代入"));
        shots.add(Map.of("timeRange", "10-18s", "visual", "生活困境/痛点展示", "copy", "每天都这样...", "bgm", "略带忧伤", "mood", "共鸣"));
        shots.add(Map.of("timeRange", "18-24s", "visual", "搞笑日常+细节笑点", "copy", "（笑点密集区）", "bgm", "搞笑配乐", "mood", "轻松搞笑"));
        shots.add(Map.of("timeRange", "24-28s", "visual", "触发事件→决心改变", "copy", "不！我要改变！", "bgm", "情绪转折", "mood", "决心"));
        shots.add(Map.of("timeRange", "28-38s", "visual", "变身过程（蒙太奇+产品植入）", "copy", "（变身蒙太奇）", "bgm", "鼓点递进", "mood", "期待"));
        shots.add(Map.of("timeRange", "38-48s", "visual", "惊艳亮相+多角度展示", "copy", "感受到了吗？自信的力量", "bgm", "气场音乐", "mood", "震撼自信"));
        shots.add(Map.of("timeRange", "48-54s", "visual", "反应镜头+搞笑收尾", "copy", "（家人/朋友惊讶）", "bgm", "喜剧音效", "mood", "搞笑反转"));
        shots.add(Map.of("timeRange", "54-60s", "visual", "价值升华+引导关注", "copy", "每个女人都值得被看见", "bgm", "温暖收尾", "mood", "升华"));

        template.put("shots", shots);
        template.put("tip", "60秒可以讲完整故事，痛点→改变→惊艳→升华，情绪弧线完整");
        return template;
    }

    private Map<String, Object> buildPersonalStateTemplate(int duration) {
        Map<String, Object> template = new LinkedHashMap<>();
        template.put("type", "personal_state_contrast");
        template.put("label", "个人状态反差（如：邋遢主妇→精致女王）");
        template.put("totalDuration", duration);

        List<Map<String, Object>> shots = new ArrayList<>();
        int s1End = (int)(duration * 0.25);
        int s2End = (int)(duration * 0.45);
        int s3End = (int)(duration * 0.65);
        int s4End = (int)(duration * 0.85);

        shots.add(Map.of("timeRange", "0-" + s1End + "s", "visual", "凌乱家中，蓬头垢面，穿着睡衣",
            "copy", "又是为家忙碌的一天...", "bgm", "喜感配乐（轻快搞笑）", "mood", "无奈疲惫但带点搞笑"));
        shots.add(Map.of("timeRange", s1End + "-" + s2End + "s", "visual", "对镜自照叹气，下定决心，开始变身",
            "copy", "不行！今天必须做自己！", "bgm", "鼓点渐强（期待感）", "mood", "决心转变，期待"));
        shots.add(Map.of("timeRange", s2End + "-" + s3End + "s", "visual", "快速化妆/换装过程，镜像分屏",
            "copy", "（蒙太奇剪辑，无文案）", "bgm", "节奏加快，鼓点密集", "mood", "专注认真"));
        shots.add(Map.of("timeRange", s3End + "-" + s4End + "s", "visual", "精致女王登场，慢动作，发丝飘动",
            "copy", "女人，永远可以重新定义自己", "bgm", "气场音乐（大气磅礴）", "mood", "自信优雅，气场全开"));
        shots.add(Map.of("timeRange", s4End + "-" + duration + "s", "visual", "走出门，回头对镜头比心/眨眼",
            "copy", "姐妹们，你也可以！", "bgm", "音乐高潮+掌声音效", "mood", "温暖鼓励，搞笑收尾"));

        template.put("shots", shots);
        template.put("shootingTips", List.of(
            "前段场景：凌乱家居环境，昏暗灯光",
            "后段场景：整洁优雅环境，明亮灯光",
            "化妆过程：使用蒙太奇快剪，配合鼓点",
            "转场效果：镜像分屏或百叶窗转场"
        ));
        return template;
    }
}
