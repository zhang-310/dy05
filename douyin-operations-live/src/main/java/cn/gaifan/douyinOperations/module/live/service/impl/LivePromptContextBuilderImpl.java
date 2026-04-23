package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.AiHostPersona;
import cn.gaifan.douyinOperations.module.ai.repository.AiPromptTemplateRepository;
import cn.gaifan.douyinOperations.module.ai.service.CompetitorInsightService;
import cn.gaifan.douyinOperations.module.ai.service.brain.HostPersonaService;
import cn.gaifan.douyinOperations.module.douyin.entity.DyPersona;
import cn.gaifan.douyinOperations.module.live.entity.LiveProduct;
import cn.gaifan.douyinOperations.module.live.entity.LiveSession;
import cn.gaifan.douyinOperations.module.live.repository.LiveProductRepository;
import cn.gaifan.douyinOperations.module.live.service.LivePromptContextBuilder;
import cn.gaifan.douyinOperations.module.live.service.LivePromptFormatService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 直播话术 Prompt 上下文构建器实现：人设、主播策略、产品类型。
 */
@Service
public class LivePromptContextBuilderImpl implements LivePromptContextBuilder {

    private static final Logger log = LoggerFactory.getLogger(LivePromptContextBuilderImpl.class);

    @Resource private LiveProductRepository liveProductRepository;
    @Resource private LivePromptFormatService livePromptFormatService;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private HostPersonaService hostPersonaService;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private AiPromptTemplateRepository promptTemplateRepository;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private CompetitorInsightService competitorInsightService;

    @Override
    public String buildPersonaDesc(DyPersona persona) {
        return buildPersonaDesc(persona, null);
    }

    @Override
    public String buildPersonaDesc(DyPersona persona, LiveSession session) {
        if (persona == null) return "主播风格：专业亲切";
        StringBuilder sb = new StringBuilder();
        sb.append("主播人设：").append(persona.getPersonaName());
        sb.append("，语气：").append(persona.getTone() != null && !persona.getTone().isBlank() ? persona.getTone() : "亲切");
        if (persona.getLocalFlavor() != null && !persona.getLocalFlavor().isBlank()) {
            sb.append("。地方特色：").append(persona.getLocalFlavor()).append("，可适当融入方言或地域梗");
        } else if (persona.getPersonaName() != null && (persona.getPersonaName().contains("李阳阳") || persona.getPersonaName().contains("阳阳"))) {
            sb.append("。地方特色：东北，可适当融入方言或地域梗");
        }
        if (persona.getPersonaTraits() != null && !persona.getPersonaTraits().isBlank()) {
            sb.append("。人设记忆点：").append(persona.getPersonaTraits()).append("（口头禅、标签、特色表达请在话术中体现）");
        }
        if (persona.getIpType() != null && !persona.getIpType().isBlank()) {
            sb.append("。IP类型：").append("phenomenal".equals(persona.getIpType()) ? "现象级（高能量、强互动、快节奏）" : "顶级（深度价值、专业信任、慢节奏）");
        }
        if (persona.getAgeRange() != null && !persona.getAgeRange().isBlank()) {
            sb.append("。年龄段：").append(persona.getAgeRange()).append("岁");
        }
        if (persona.getPositioningTags() != null && !persona.getPositioningTags().isBlank()) {
            sb.append("。定位标签：").append(persona.getPositioningTags());
        }
        if (persona.getLiveStyle() != null && !persona.getLiveStyle().isBlank()) {
            String liveStyleDesc = switch (persona.getLiveStyle()) {
                case "high_energy" -> "高能量快节奏：短句为主、互动频繁、情绪饱满";
                case "deep_value" -> "深度价值慢节奏：信息密度高、专业讲解、建立信任";
                case "emotional" -> "情感共鸣型：故事感强、共情表达、走心引导";
                default -> persona.getLiveStyle();
            };
            sb.append("。直播风格：").append(liveStyleDesc);
        }
        if (persona.getContentRatio() != null && !persona.getContentRatio().isBlank()) {
            sb.append("。内容比例参考：").append(persona.getContentRatio());
        }

        String hostCode = matchHostCode(persona.getPersonaName());
        if (hostCode != null) {
            String sessionType = session != null ? session.getSessionType() : null;
            String hostStrategy = buildHostPersonaStrategy(hostCode, sessionType);
            if (hostStrategy != null && !hostStrategy.isBlank()) {
                sb.append("\n").append(hostStrategy);
            }
        }

        return sb.toString();
    }

    private String matchHostCode(String personaName) {
        if (personaName == null) return null;
        if (personaName.contains("肖瑶")) return "xiaoyao";
        if (personaName.contains("肖蝉")) return "xiaochan";
        if (personaName.contains("阳阳") || personaName.contains("李阳")) return "yangyang";
        if (personaName.contains("智慧") || personaName.contains("王智")) return "zhihui";
        if (personaName.contains("田玲红") || personaName.contains("玲红")) return "tianlinghong";
        return null;
    }

    @Override
    public String buildHostPersonaStrategy(String hostCode) {
        return buildHostPersonaStrategy(hostCode, null);
    }

    @Override
    public String buildHostPersonaStrategy(String hostCode, String sessionType) {
        if (hostCode == null || hostCode.isBlank()) return "";

        if ("chat_2h".equals(sessionType) && "yangyang".equals(hostCode)) {
            return """
                【主播策略：李阳阳·2小时拉自然流】
                - 路线：聊家常蓄水 + 人气高时卖利润品
                - 话术特征：东北人、幽默接地气、歇后语、名言金句、古诗格调
                - 留人侧重：情感共鸣、夫妻/婆媳/励志话题，不急于推销
                - 转化时机：高在线时段（45-105分钟）主推利润品
                - 风格：少用强推销话术，突出独特个性与情绪价值""";
        }

        if (hostPersonaService != null && hostPersonaService.isAvailable()) {
            String dbCode = "xiaochan".equals(hostCode) ? "xiachan" : hostCode;
            AiHostPersona persona = hostPersonaService.getByCode(dbCode);
            if (persona == null && "xiachan".equals(dbCode)) {
                persona = hostPersonaService.getByCode("xiaochan");
            }
            if (persona != null) {
                return buildStrategyFromPersonaEntity(persona);
            }
            log.debug("HostPersonaService 未找到 hostCode={}，降级到硬编码", hostCode);
        }

        return buildHardcodedStrategy(hostCode);
    }

    private String buildStrategyFromPersonaEntity(AiHostPersona persona) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("【主播策略：%s·%s】\n", persona.getHostName(),
                persona.getPositioning() != null ? persona.getPositioning() : ""));
        sb.append("- 路线：").append("C".equals(persona.getOrientation()) ? "消费者面向" : "商业面向");
        sb.append("，流量阶段：").append(switch (persona.getFlowPhase() != null ? persona.getFlowPhase() : 0) {
            case 0 -> "流量入口";
            case 1 -> "转化节点";
            case 2 -> "B端沉淀";
            default -> "通用";
        }).append("\n");

        if (persona.getContentMatrix() != null && !persona.getContentMatrix().isBlank()) {
            sb.append("- 内容矩阵：").append(persona.getContentMatrix()).append("\n");
        }
        if (persona.getAiPriorities() != null && !persona.getAiPriorities().isBlank()) {
            sb.append("- AI优先级：").append(persona.getAiPriorities()).append("\n");
        }
        if (persona.getStyleVector() != null && !persona.getStyleVector().isBlank()) {
            sb.append("- 风格向量：").append(persona.getStyleVector()).append("\n");
        }
        return sb.toString();
    }

    private String buildHardcodedStrategy(String hostCode) {
        return switch (hostCode.toLowerCase()) {
            case "xiaoyao" -> """
                【主播策略：肖瑶·校园彩妆达人】
                - 路线：现象级IP，高能量互动
                - 目标用户：大学生、学生党
                - 话术特征：年轻活力、网络用语、闺蜜式分享
                - 内容侧重：平价好物、学生党必备、校园妆容教程
                - 互动风格：猜价格、投票、弹幕接龙""";
            case "xiaochan", "xiachan" -> """
                【主播策略：肖蝉·职场彩妆导师】
                - 路线：顶级IP，专业深度
                - 目标用户：职场女性25-35岁
                - 话术特征：专业可信、成分解析、场景化推荐
                - 内容侧重：通勤妆容、职场护肤、投资级单品
                - 互动风格：知识问答、成分科普、护肤方案定制""";
            case "yangyang" -> """
                【主播策略：李阳阳·精致生活达人】
                - 路线：顶级IP，品质生活
                - 目标用户：追求品质生活的女性
                - 话术特征：精致优雅、仪式感强、品位引导
                - 内容侧重：高端护肤、生活美学、品质好物
                - 互动风格：分享生活方式、品鉴体验、场景展示""";
            case "zhihui" -> """
                【主播策略：王智慧·国风美学达人】
                - 路线：现象级IP，文化差异化
                - 目标用户：热爱传统文化的女性
                - 话术特征：古风雅韵、诗词引用、中式美学
                - 内容侧重：国风妆容、汉方护肤、传统文化融合
                - 互动风格：诗词接龙、文化典故、国风挑战""";
            case "tianlinghong" -> """
                【主播策略：田玲红·供应链女王】
                - 路线：顶级IP，行业权威
                - 目标用户：实体店老板、批发商、B端客户
                - 话术特征：专业犀利、数据驱动、行业洞察
                - 内容侧重：供应链优势、成本分析、批量采购方案
                - 互动风格：行业问答、案例分析、数据对比""";
            default -> "";
        };
    }

    @Override
    public String getProductTypeHintForPrompt(Long productId, Long sessionId) {
        if (productId == null || sessionId == null) return "";
        return liveProductRepository.findBySessionIdAndProductId(sessionId, productId)
                .map(LiveProduct::getProductType)
                .filter(t -> t != null && !t.isBlank())
                .map(livePromptFormatService::getProductTypePromptHint)
                .filter(h -> !h.isBlank())
                .map(h -> "产品类型及话术侧重：" + h)
                .orElse("");
    }

    /**
     * 构建受众画像上下文描述
     */
    public String buildAudienceProfileContext(String audienceProfileJson) {
        if (audienceProfileJson == null || audienceProfileJson.isBlank()) return "";
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var profile = mapper.readValue(audienceProfileJson, cn.gaifan.douyinOperations.module.live.vo.AudienceProfileVO.class);
            StringBuilder sb = new StringBuilder("【目标受众画像】");
            if (profile.getAgeRange() != null) sb.append("年龄段: ").append(profile.getAgeRange()).append("；");
            if (profile.getGender() != null) sb.append("性别: ").append(profile.getGender()).append("；");
            if (profile.getCity() != null) sb.append("城市层级: ").append(profile.getCity()).append("；");
            if (profile.getPurchasePower() != null) sb.append("消费力: ").append(profile.getPurchasePower()).append("；");
            if (profile.getInterests() != null && !profile.getInterests().isEmpty()) {
                sb.append("兴趣标签: ").append(String.join("、", profile.getInterests())).append("；");
            }
            sb.append("\n请根据以上受众特征调整话术风格和用语。");
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 获取直播形式对应的系统提示语
     */
    public String getFormatSystemPrompt(String liveFormat) {
        if (liveFormat == null || liveFormat.isBlank()) return "";
        if (promptTemplateRepository != null) {
            var templates = promptTemplateRepository.findByTemplateCodeAndVariantNameAndDeleted(
                    "live_format_system_prompt", liveFormat, 0);
            if (templates != null && !templates.isEmpty()) {
                String systemPrompt = templates.get(0).getSystemPrompt();
                if (systemPrompt != null && !systemPrompt.isBlank()) {
                    return systemPrompt;
                }
            }
        }
        // Hardcoded fallbacks
        return switch (liveFormat) {
            case "single_sku" -> "围绕一个核心商品，多角度深度展开讲解。";
            case "warehouse" -> "极速过品节奏，每品10-30秒快速展示。";
            case "content_commerce" -> "80%内容+20%商品，以有趣内容自然过渡到商品推荐。";
            case "content_led" -> "以价值内容为主，教学/分享/测评为核心。";
            case "organic_micro_paid" -> "兼顾自然流量留人和付费流量转化。";
            case "heavy_paid_category" -> "面向付费流量，话术简洁直接，转化效率优先。";
            default -> "";
        };
    }

    /**
     * C-2: 构建竞品差异化上下文 — 话术生成时注入同品类竞品洞察
     */
    public String buildCompetitorDifferentiationContext(String category) {
        if (competitorInsightService == null || category == null || category.isBlank()) return "";
        try {
            String advice = competitorInsightService.getDifferentiationAdvice(category);
            if (advice != null && !advice.isBlank()) {
                return "\n" + advice;
            }
        } catch (Exception e) {
            log.debug("[PromptContext] 竞品差异化上下文构建跳过: {}", e.getMessage());
        }
        return "";
    }
}
