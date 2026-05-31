package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvScript;
import cn.gaifan.douyinOperations.module.ai.service.OperationalStrategyKnowledgeService;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvScriptRepository;
import cn.gaifan.douyinOperations.module.shortvideo.service.ShortVideoAiService;
import cn.gaifan.douyinOperations.module.shortvideo.service.SvScriptService;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvViralVideo;
import cn.gaifan.douyinOperations.module.shortvideo.service.ViralVideoService;
import cn.gaifan.douyinOperations.module.shortvideo.vo.AiScriptGenerateVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.ViralCollectVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvScriptSaveVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvScriptSearchVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvScriptVO;
import cn.gaifan.douyinOperations.contract.product.FeatureCode;
import cn.gaifan.douyinOperations.contract.product.ProductCode;
import cn.gaifan.douyinOperations.module.platform.credit.CommercialProductChargeService;
import cn.gaifan.douyinOperations.module.platform.product.DeliveryProduct;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

@Service
public class SvScriptServiceImpl implements SvScriptService {

    @Resource
    private SvScriptRepository scriptRepository;
    @Resource
    private ShortVideoAiService aiService;
    @Resource
    private ViralVideoService viralVideoService;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private OperationalStrategyKnowledgeService operationalStrategyKnowledgeService;

    @Autowired(required = false)
    private CommercialProductChargeService commercialProductChargeService;

    @Override
    public PageResultVO<SvScriptVO> search(SvScriptSearchVO vo, Long ownerId) {
        if (ownerId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        vo.validateParams();
        Specification<SvScript> spec = (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            preds.add(cb.equal(root.get("ownerId"), ownerId));
            preds.add(cb.equal(root.get("deleted"), 0));
            if (StringUtils.hasText(vo.getScriptType())) preds.add(cb.equal(root.get("scriptType"), vo.getScriptType()));
            if (StringUtils.hasText(vo.getStyle())) preds.add(cb.equal(root.get("style"), vo.getStyle()));
            if (StringUtils.hasText(vo.getTitle())) preds.add(cb.like(root.get("title"), "%" + vo.getTitle().trim() + "%"));
            return cb.and(preds.toArray(new Predicate[0]));
        };
        String sortName = StringUtils.hasText(vo.getSortName()) ? vo.getSortName() : "createTime";
        Sort sort = "asc".equalsIgnoreCase(vo.getSortOrder()) ? Sort.by(sortName).ascending() : Sort.by(sortName).descending();
        vo.validateParams();
        Pageable pageable = PageRequest.of(vo.getPage(), vo.getRows(), sort);
        Page<SvScript> page = scriptRepository.findAll(spec, pageable);
        List<SvScriptVO> list = page.getContent().stream().map(this::toVO).toList();
        return PageResultVO.of(page.getTotalElements(), list, vo.getPage(), vo.getRows());
    }

    @Override
    public SvScriptVO get(Long id, Long ownerId) {
        if (ownerId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        SvScript e = scriptRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "脚本不存在"));
        if (!e.getOwnerId().equals(ownerId)) throw new BusinessException(ErrorCode.FORBIDDEN, "无权限访问");
        if (e.getDeleted() != 0) throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "脚本不存在");
        return toVO(e);
    }

    @Override
    public Long save(SvScriptSaveVO vo, Long ownerId) {
        if (ownerId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        SvScript e;
        if (vo.getId() != null && vo.getId() > 0) {
            e = scriptRepository.findById(vo.getId()).orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "脚本不存在"));
            if (!e.getOwnerId().equals(ownerId)) throw new BusinessException(ErrorCode.FORBIDDEN, "无权限修改");
        } else {
            e = new SvScript();
            e.setOwnerId(ownerId);
        }
        e.setTitle(vo.getTitle());
        e.setContent(vo.getContent());
        e.setScriptType(vo.getScriptType());
        e.setGenerationType(vo.getGenerationType());
        e.setReferenceViralId(vo.getReferenceViralId());
        e.setTheme(vo.getTheme());
        e.setStyle(vo.getStyle());
        e.setDuration(vo.getDuration());
        e.setWordCount(vo.getWordCount());
        e.setTags(vo.getTags());
        e.setAiPrompt(vo.getAiPrompt());
        e.setAiModel(vo.getAiModel());
        e = scriptRepository.save(e);
        return e.getId();
    }

    @Override
    public void delete(Long id, Long ownerId) {
        if (ownerId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        SvScript e = scriptRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "脚本不存在"));
        if (!e.getOwnerId().equals(ownerId)) throw new BusinessException(ErrorCode.FORBIDDEN, "无权限删除");
        e.setDeleted(1);
        scriptRepository.save(e);
    }

    @Override
    public String generate(String type, String theme, Long viralVideoId, String productInfo, String style, Integer duration, Long ownerId) {
        if (ownerId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        if (commercialProductChargeService != null) {
            commercialProductChargeService.charge(
                    CommercialProductChargeService.CommercialProductChargeCommand.of(
                            ProductCode.SHORTVIDEO_MAKER,
                            FeatureCode.SHORTVIDEO_SCRIPT_GENERATE,
                            "短视频脚本生成 ownerId=" + ownerId,
                            DeliveryProduct.SHORTVIDEO_MAKER
                    ));
        }
        AiScriptGenerateVO vo = new AiScriptGenerateVO();
        String seed = buildScriptSeed(type, theme, productInfo, style, duration);
        seed = appendOfficialAndPatternContext(seed, ownerId, String.join(" ",
                type != null ? type : "",
                theme != null ? theme : "",
                productInfo != null ? productInfo : "",
                style != null ? style : "",
                "短视频脚本 分镜 数字人成片 官方规则 违规规则 爆款模式"));
        vo.setCopyText(seed);
        vo.setViralId(viralVideoId);
        vo.setPersonaId(null);
        vo.setSceneType(resolveSceneType(type, style));
        vo.setDuration(duration);
        return aiService.generateScript(vo, ownerId);
    }

    @Override
    public String analyzeViral(String viralVideoUrl, String extractLevel, Long ownerId) {
        if (ownerId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        if (!StringUtils.hasText(viralVideoUrl)) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "爆款视频 URL 不能为空");
        if (commercialProductChargeService != null) {
            commercialProductChargeService.charge(
                    CommercialProductChargeService.CommercialProductChargeCommand.of(
                            ProductCode.VIDEO_INSIGHT,
                            FeatureCode.VIDEO_VIRAL_ANALYSIS,
                            "爆款 URL 分析 ownerId=" + ownerId,
                            DeliveryProduct.VIDEO_INSIGHT
                    ));
        }
        ViralCollectVO collectVO = new ViralCollectVO();
        collectVO.setVideoUrl(viralVideoUrl);
        collectVO.setDouyinVideoId("url_" + System.currentTimeMillis());
        collectVO.setTitle("爆款分析");
        Long viralId = viralVideoService.collectViralVideo(collectVO, ownerId);
        viralVideoService.triggerAnalysis(viralId, ownerId);
        SvViralVideo viral = viralVideoService.getViralVideo(viralId, ownerId);
        return viral.getAnalysisResult() != null ? viral.getAnalysisResult() : "分析已触发，请稍后查看";
    }

    private SvScriptVO toVO(SvScript e) {
        SvScriptVO vo = new SvScriptVO();
        vo.setId(e.getId());
        vo.setOwnerId(e.getOwnerId());
        vo.setTitle(e.getTitle());
        vo.setContent(e.getContent());
        vo.setScriptType(e.getScriptType());
        vo.setGenerationType(e.getGenerationType());
        vo.setReferenceViralId(e.getReferenceViralId());
        vo.setTheme(e.getTheme());
        vo.setStyle(e.getStyle());
        vo.setDuration(e.getDuration());
        vo.setWordCount(e.getWordCount());
        vo.setTags(e.getTags());
        vo.setAiPrompt(e.getAiPrompt());
        vo.setAiModel(e.getAiModel());
        vo.setCreateTime(e.getCreateTime());
        vo.setUpdateTime(e.getUpdateTime());
        return vo;
    }

    private String buildScriptSeed(String type, String theme, String productInfo, String style, Integer duration) {
        StringBuilder sb = new StringBuilder();
        sb.append("请生成一条可直接进入分镜和成片制作的短视频脚本。\n");
        appendIfText(sb, "视频类型", type);
        appendIfText(sb, "主题", theme);
        appendIfText(sb, "风格", style);
        if (duration != null && duration > 0) {
            sb.append("- 目标时长：").append(duration).append("秒\n");
        }
        appendIfText(sb, "补充信息/创作简报", productInfo);
        if (isDigitalHumanCommerce(type, theme, productInfo, style)) {
            sb.append("""

                    数字人口播带货专项要求：
                    1. 脚本按「数字人正脸钩子 - 产品入镜 - 产品细节特写 - 使用演示/对比 - 数字人总结 - 合规 CTA」组织。
                    2. 数字人口播只承担信任建立和承接，不能整条视频只有头像念稿；产品细节、手部演示、参数卡和证据镜头要占主要画面。
                    3. 每 5-8 秒明确写出画面角色：digital_human、product_closeup、usage_demo、proof_overlay 或 cta。
                    4. 商品卖点必须能被产品信息、画面或官方规则支撑；不能写「秒变」「永久」「百分百」「全网最低」等无法证明表达。
                    5. 结尾要包含适合人群/不适合人群判断，降低硬广感。
                    """);
        }
        sb.append("""

                输出要求：
                1. 先给 3 秒钩子，必须具体，不要空泛口号。
                2. 正文按「问题/冲突 - 证据/步骤 - 结论/行动」推进。
                3. 每 5-8 秒给出画面建议，方便后续拆分分镜。
                4. 同时包含口播、屏幕字幕、画面动作和素材提示。
                5. 避免绝对化、医疗化、夸大效果和无法证明的承诺。
                """);
        return sb.toString();
    }

    private String appendOfficialAndPatternContext(String seed, Long ownerId, String query) {
        if (operationalStrategyKnowledgeService == null || ownerId == null || ownerId <= 0) {
            return seed;
        }
        StringBuilder sb = new StringBuilder(seed);
        try {
            OperationalStrategyKnowledgeService.PromptContext official =
                    operationalStrategyKnowledgeService.buildShortVideoGenerationContext(ownerId, query, 2600);
            if (official != null && official.hasText()) {
                sb.append("\n\n").append(official.promptBlock());
            }
        } catch (Exception ignored) {
            // 短视频生成不能因为知识库检索失败整体中断
        }
        try {
            OperationalStrategyKnowledgeService.PromptContext viral =
                    operationalStrategyKnowledgeService.buildViralPatternContext(ownerId, query, 1800);
            if (viral != null && viral.hasText()) {
                sb.append("\n\n").append(viral.promptBlock());
            }
        } catch (Exception ignored) {
            // 爆款模式知识库为空时继续按基础策略生成
        }
        sb.append("""

                强制知识库约束：
                - 必须优先遵守 douyin 官方学习资料和 douyin_weigui 违规规则。
                - 爆款模式知识库只能用于提炼结构、节奏、钩子和镜头组合，不能抄袭原文或复刻违规表达。
                - 输出末尾必须列出「官方规则引用要点」和「爆款模式借鉴点」。
                """);
        return sb.toString();
    }

    private static void appendIfText(StringBuilder sb, String label, String value) {
        if (StringUtils.hasText(value)) {
            sb.append("- ").append(label).append("：").append(value.trim()).append("\n");
        }
    }

    private String resolveSceneType(String type, String style) {
        String text = ((type == null ? "" : type) + " " + (style == null ? "" : style)).toLowerCase();
        if (text.contains("soft_ad") || text.contains("测评") || text.contains("干货")) {
            return "studio / desk setup";
        }
        if (text.contains("viral") || text.contains("剧情")) {
            return "mixed scenes";
        }
        return "indoor";
    }

    private static boolean isDigitalHumanCommerce(String type, String theme, String productInfo, String style) {
        String text = ((type == null ? "" : type) + " "
                + (theme == null ? "" : theme) + " "
                + (productInfo == null ? "" : productInfo) + " "
                + (style == null ? "" : style)).toLowerCase();
        return containsAny(text,
                "digital_human", "数字人", "avatar", "talking head", "口播带货", "带货口播",
                "带货", "商品", "产品", "开箱", "测评", "种草", "软广", "product", "commerce", "ecommerce");
    }

    private static boolean containsAny(String text, String... words) {
        if (text == null) {
            return false;
        }
        for (String word : words) {
            if (word != null && text.contains(word.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
