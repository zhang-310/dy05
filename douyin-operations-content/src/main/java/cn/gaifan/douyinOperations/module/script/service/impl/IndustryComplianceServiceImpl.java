package cn.gaifan.douyinOperations.module.script.service.impl;

import cn.gaifan.douyinOperations.module.script.service.IndustryComplianceService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 多行业合规：全业通用摘要（广告法倾向）+ 垂直行业规则 + 抖音公开摘要正则。
 * <p>
 * {@code industryCode} 空或缺省由调用方传 {@code cosmetics}；未知编码仅套用通用+抖音公开规则，不套用任意垂直包。
 */
@Service
public class IndustryComplianceServiceImpl implements IndustryComplianceService {

    /** 全业通用：与垂直包、抖音公开规则互补；reference 为归纳说明非裁判文书 */
    private static final List<ComplianceRule> GENERAL_AD_RULES = List.of(
            new ComplianceRule(
                    "史无前例|巅峰之作|创世|王牌之选|绝无仅有",
                    "warning",
                    "易构成夸大或不当绝对化宣传",
                    "《广告法》及电商宣传通用要求（摘要）"),
            new ComplianceRule(
                    "升值(?:空间)?无限|买到就是赚到|稳赚不赔|无风险高收益",
                    "warning",
                    "投资收益、资产升值或「无风险」类表述风险极高",
                    "广告法·金融/房产/理财营销（摘要）"),
            new ComplianceRule(
                    "国家级(?!标准)|领袖品牌|领军品牌(?!论坛)",
                    "warning",
                    "国家机关或「领军」类背书式表述需有事实依据",
                    "《广告法》第九条（摘要）")
    );

    private static final Map<String, List<ComplianceRule>> VERTICAL_RULES = new LinkedHashMap<>();

    /**
     * 抖音协议 / 社区自律公约 / 电商规则中心等公开材料中的高频情形（与 sc_compliance_word V067/V108 互补）。
     */
    private static final List<ComplianceRule> DOUYIN_PUBLIC_RULE_PATTERNS = List.of(
            new ComplianceRule(
                    "私下交易|线下付款|私信转账|脱离平台|走私单|绕过平台",
                    "error",
                    "涉嫌引导私下交易或脱离平台交易",
                    "抖音电商创作者/直播规范（公开规则摘要·交易秩序）",
                    "douyin_public_summary"),
            new ComplianceRule(
                    "加微信购买|加微信下单|加V购买|加V下单|扫码加微信|扫码加群",
                    "error",
                    "涉嫌引导站外交易或私域导流",
                    "抖音电商创作者管理相关公开规则（摘要）",
                    "douyin_public_summary"),
            new ComplianceRule(
                    "关注点赞抽奖|刷礼物抽奖|虚假抽奖|内幕消息",
                    "warning",
                    "互动/抽奖表述易构成诱导或虚假宣传",
                    "抖音社区自律与电商公开治理通报（摘要）",
                    "douyin_public_summary"),
            new ComplianceRule(
                    "百分百治愈|百分百见效|一定美白|一定瘦身|永不反弹|永不复发",
                    "error",
                    "对功效或结果的不当承诺",
                    "《广告法》+ 化妆品/直播公开合规要求（摘要）",
                    "douyin_public_summary"),
            new ComplianceRule(
                    "高仿|A货|原单正品|1[:：]1复刻",
                    "error",
                    "涉嫌假冒伪劣、误导来源或违规交易表述",
                    "抖音电商规则中心·商品与知识产权（摘要）",
                    "douyin_public_summary"),
            new ComplianceRule(
                    "刷粉|刷赞|刷评论|买粉丝|买点赞|代刷",
                    "warning",
                    "涉嫌流量造假或违规推广",
                    "抖音社区自律公约·作弊与数据真实性（摘要）",
                    "douyin_public_summary"),
            new ComplianceRule(
                    "包治百病|治愈率\\s*\\d+|有效率\\s*100\\s*%",
                    "error",
                    "疗效/治愈率等数据化宣称易构成违规医疗或虚假宣传",
                    "医疗广告管理及平台内容规范（摘要）",
                    "douyin_public_summary"),
            new ComplianceRule(
                    "内部特供|机关专供|领导专用|国宴专用",
                    "error",
                    "涉嫌虚假国家机关/权威背书或不当营销",
                    "《广告法》及平台治理通报（摘要）",
                    "douyin_public_summary"),
            new ComplianceRule(
                    "人肉搜索|开盒挂人",
                    "error",
                    "涉嫌侵犯隐私或网络暴力",
                    "抖音社区自律公约·网暴与隐私保护（摘要）",
                    "douyin_public_summary"),
            new ComplianceRule(
                    "虚构原价|虚假降价|从没卖过这个价|从未上架过这个价",
                    "warning",
                    "价格或促销表述易构成虚假宣传",
                    "抖音电商价格与促销规范（摘要）",
                    "douyin_public_summary"),
            new ComplianceRule(
                    "恶意造谣|编造谣言|假消息实锤",
                    "warning",
                    "未经核实信息易违反社区公约，需可溯源依据",
                    "抖音社区自律公约·虚假信息（摘要）",
                    "douyin_public_summary"),
            new ComplianceRule(
                    "代开发票|虚开发票|不要发票减价",
                    "warning",
                    "税务与发票相关表述需符合法规及平台规则",
                    "电商及财税合规（摘要）",
                    "douyin_public_summary")
    );

    static {
        VERTICAL_RULES.put("cosmetics", List.of(
                new ComplianceRule("速效|超强|全效|特级", "error", "化妆品禁用绝对化用语", "《化妆品广告管理条例》"),
                new ComplianceRule("纯天然|零添加", "warning", "需有认证才可使用", "化妆品标识规范"),
                new ComplianceRule("医学级|药妆", "error", "中国无药妆概念", "《化妆品监督管理条例》"),
                new ComplianceRule("\\d+天美白|\\d+天逆龄|\\d+天祛斑", "error", "禁止具体数字承诺", "化妆品广告法"),
                new ComplianceRule("最好|第一(?!名|天|周|届|轮|步)|唯一|全网最", "error", "禁止绝对化用语", "《广告法》第九条")));

        VERTICAL_RULES.put("food", List.of(
                new ComplianceRule(
                        "抗癌食品|防癌食品|食疗抗癌|降血糖食品|降血压食品|普通食品治病|吃了(?:降|治)(?:血压|血糖)",
                        "error",
                        "普通食品不得涉及疾病预防、治疗功能",
                        "《食品安全法》第七十三条（摘要）"),
                new ComplianceRule(
                        "无糖食品(?:治疗)|(?:普通)食品(?:具有)?保健",
                        "warning",
                        "食品与保健食品宣称边界需符合标签及广告规定",
                        "食品安全及广告合规（摘要）")));

        VERTICAL_RULES.put("health_supplement", List.of(
                new ComplianceRule(
                        "代替药物|代替吃药|停了药|停了西药|无需吃药|停药(?:就行)?",
                        "error",
                        "保健食品不得替代药物治疗",
                        "《保健食品广告审查暂行规定》等（摘要）"),
                new ComplianceRule(
                        "本品预防|预防癌症|治疗高血压|治疗糖尿病",
                        "error",
                        "保健食品不得对疾病作预防、治疗声称",
                        "市场监管总局保健食品广告合规（摘要）")));

        VERTICAL_RULES.put("apparel", List.of(
                new ComplianceRule(
                        "化纤当棉|化纤冒充棉|非真皮.*真皮|假棉|黑心棉",
                        "error",
                        "纺织材质、成分虚假或误导表述",
                        "纺织品与消费品标识（摘要）"),
                new ComplianceRule(
                        "军工品质(?:内衣|内裤)|纳米屏蔽(?:辐射)?",
                        "warning",
                        "缺乏依据的功能性纺织宣传易构成虚假",
                        "广告法·纺织品类（摘要）")));

        VERTICAL_RULES.put("digital_3c", List.of(
                new ComplianceRule(
                        "三无充电|三无适配器|三无充电宝|无3C认证销售|无CCC销售",
                        "error",
                        "强制性产品认证范围内商品需合法标注与销售",
                        "《强制性产品认证管理规定》（摘要）"),
                new ComplianceRule(
                        "山寨机|高仿手机|破解(?:版)?(?:系统|iPhone)",
                        "error",
                        "涉嫌侵犯知识产权或违规改装表述",
                        "电商知识产权与3C规则（摘要）")));

        VERTICAL_RULES.put("mother_baby", List.of(
                new ComplianceRule(
                        "(?:奶粉|配方奶)(?:完全)?代替母乳|替代母乳|比母乳(?:更)?好",
                        "error",
                        "婴幼儿配方乳粉广告不得声称或暗示替代母乳",
                        "《婴幼儿配方乳粉产品配方注册管理办法》广告要求（摘要）"),
                new ComplianceRule(
                        "婴儿(?:食品)?治病|宝宝吃了(?:就)?好(?:了)?(?:不用去医院)",
                        "warning",
                        "婴幼儿食品不得涉及治疗、诊疗暗示",
                        "母婴食品广告合规（摘要）")));

        VERTICAL_RULES.put("jewelry", List.of(
                new ComplianceRule(
                        "千足金(?:首饰)?|万足金|24K纯金(?:无钢印)?",
                        "warning",
                        "贵金属命名与标注须符合国标，避免过时或误导性纯度用语",
                        "GB 11887 贵金属首饰命名（摘要）"),
                new ComplianceRule(
                        "假钻|仿钻(?:当)|培育钻(?:当天然)",
                        "error",
                        "珠宝材质来源、合成与天然须真实准确",
                        "珠宝玉石名称国家标准（摘要）")));

        VERTICAL_RULES.put("pet", List.of(
                new ComplianceRule(
                        "宠物粮治病|猫粮治病|狗粮治病|兽药级(?:粮|罐头)",
                        "error",
                        "宠物饲料不得涉及疾病治疗宣称",
                        "宠物饲料标签及广告合规（摘要）"),
                new ComplianceRule(
                        "处方(?:猫粮|狗粮)(?=.*治)",
                        "warning",
                        "处方宠物食品表述需与注册及兽医指导一致",
                        "宠物食品分类宣传（摘要）")));

        VERTICAL_RULES.put("medical_device", List.of(
                new ComplianceRule(
                        "家用治疗仪包治|医疗器械根治|在家(?:就能)?治愈",
                        "error",
                        "医疗器械广告不得含有不科学的功效断言",
                        "《医疗器械广告审查办法》（摘要）"),
                new ComplianceRule(
                        "最佳(?:治疗)|治愈率最高",
                        "warning",
                        "医疗器械禁止使用绝对化及治愈率比较",
                        "医疗器械广告合规（摘要）")));

        VERTICAL_RULES.put("education", List.of(
                new ComplianceRule(
                        "考试包过|考证包过|保过班|保过承诺|不过(?:全额)?退款.*(?:考试|拿证|上岸)|原题命中|内部题库",
                        "error",
                        "教育培训广告不得对通过考试、获得证书等作保证性承诺",
                        "《广告法》第二十四条（摘要）"),
                new ComplianceRule(
                        "命题人授课|阅卷人(?:亲自)?辅导",
                        "warning",
                        "涉嫌利用考试机构名义作误导宣传",
                        "教育培训广告合规（摘要）")));

        VERTICAL_RULES.put("finance", List.of(
                new ComplianceRule(
                        "保本保息|刚性兑付|年化\\s*\\d{1,3}\\s*%+(?!.*业绩比较基准)",
                        "error",
                        "理财、资管产品不得承诺保本保收益（摘要归纳）",
                        "金融监管与广告合规（摘要）"),
                new ComplianceRule(
                        "跟单(?:必赚)|稳赚不赔|内幕(?:消息)?(?:炒股)",
                        "error",
                        "证券投资咨询类违规诱导表述",
                        "证券期货广告合规（摘要）")));

        VERTICAL_RULES.put("real_estate", List.of(
                new ComplianceRule(
                        "买房送户口|买房(?:包)?上户口|学位(?:包)?过|必上名校",
                        "error",
                        "房地产广告不得含有升学、户口等承诺",
                        "《房地产广告发布规定》（摘要）"),
                new ComplianceRule(
                        "返本销售|售后包租|年回报\\s*\\d+\\s*%",
                        "warning",
                        "商品房销售与租赁回报承诺类表述风险",
                        "房地产广告合规（摘要）")));
    }

    @Override
    public List<Map<String, Object>> checkCompliance(String text, String industryCode) {
        List<ComplianceRule> rules = collectRulesForIndustry(industryCode);
        List<Map<String, Object>> violations = new ArrayList<>();
        String t = text != null ? text : "";
        for (ComplianceRule rule : rules) {
            var matcher = rule.pattern.matcher(t);
            while (matcher.find()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("matchedText", matcher.group());
                row.put("level", rule.level);
                row.put("reason", rule.reason);
                row.put("reference", rule.reference);
                row.put("position", matcher.start());
                row.put("source", rule.source);
                violations.add(Collections.unmodifiableMap(row));
            }
        }
        return violations;
    }

    @Override
    public List<Map<String, Object>> listRules(String industryCode) {
        return collectRulesForIndustry(industryCode).stream()
                .map(r -> Map.<String, Object>of(
                        "pattern", r.pattern.pattern(),
                        "level", r.level,
                        "reason", r.reason,
                        "reference", r.reference,
                        "source", r.source))
                .toList();
    }

    @Override
    public List<String> listSupportedIndustryCodes() {
        return List.copyOf(VERTICAL_RULES.keySet());
    }

    private static List<ComplianceRule> collectRulesForIndustry(String industryCode) {
        List<ComplianceRule> rules = new ArrayList<>(GENERAL_AD_RULES);
        String vertical = resolveVerticalCode(industryCode);
        if (vertical != null) {
            rules.addAll(VERTICAL_RULES.get(vertical));
        }
        rules.addAll(DOUYIN_PUBLIC_RULE_PATTERNS);
        return rules;
    }

    /**
     * @return 垂直行业 key；未知编码返回 null（仅通用+抖音公开）
     */
    private static String resolveVerticalCode(String industryCode) {
        if (industryCode == null || industryCode.isBlank()) {
            return "cosmetics";
        }
        String c = industryCode.trim().toLowerCase(Locale.ROOT);
        return VERTICAL_RULES.containsKey(c) ? c : null;
    }

    private record ComplianceRule(Pattern pattern, String level, String reason, String reference, String source) {
        ComplianceRule(String regex, String level, String reason, String reference) {
            this(Pattern.compile(regex), level, reason, reference, "industry");
        }

        ComplianceRule(String regex, String level, String reason, String reference, String source) {
            this(Pattern.compile(regex), level, reason, reference, source);
        }
    }
}
