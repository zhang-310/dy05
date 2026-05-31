package cn.gaifan.douyinOperations.module.product.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.util.RequestRoleResolver;
import cn.gaifan.douyinOperations.module.product.entity.DyProduct;
import cn.gaifan.douyinOperations.module.product.entity.DyProductScript;
import cn.gaifan.douyinOperations.module.product.entity.ProductScriptVersion;
import cn.gaifan.douyinOperations.module.product.repository.DyProductRepository;
import cn.gaifan.douyinOperations.module.product.repository.DyProductScriptRepository;
import cn.gaifan.douyinOperations.module.product.repository.ProductScriptVersionRepository;
import cn.gaifan.douyinOperations.module.product.service.ProductScriptShortVideoExportService;
import cn.gaifan.douyinOperations.module.product.vo.ProductScriptExportToShortVideoResultVO;
import cn.gaifan.douyinOperations.module.product.vo.ProductScriptExportToShortVideoVO;
import cn.gaifan.douyinOperations.module.shortvideo.service.SvProjectService;
import cn.gaifan.douyinOperations.module.shortvideo.service.SvScriptService;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvProjectSaveVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvScriptSaveVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 商品话术到短视频项目的 app 层桥接实现。
 */
@Service
public class ProductScriptShortVideoExportServiceImpl implements ProductScriptShortVideoExportService {

    @Resource
    private DyProductRepository productRepository;
    @Resource
    private DyProductScriptRepository productScriptRepository;
    @Resource
    private ProductScriptVersionRepository productScriptVersionRepository;
    @Resource
    private SvScriptService svScriptService;
    @Resource
    private SvProjectService svProjectService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductScriptExportToShortVideoResultVO exportToShortVideoProject(ProductScriptExportToShortVideoVO vo, Long ownerId) {
        if (vo == null || vo.getProductId() == null || vo.getProductId() <= 0 || ownerId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "参数无效");
        }

        DyProduct product = productRepository.findByIdAndDeleted(vo.getProductId(), 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "商品不存在"));
        if (!RequestRoleResolver.isAdmin() && !ownerId.equals(product.getUserId())) {
            throw new BusinessException(ErrorCode.PRODUCT_FORBIDDEN, "无权限操作此商品");
        }

        ScriptSource source = resolveScriptSource(vo, product, ownerId);
        int duration = sanitizeDuration(vo.getDuration() != null ? vo.getDuration() : source.duration());
        String style = firstText(vo.getStyle(), source.style(), "种草");
        Long personaId = vo.getPersonaId() != null ? vo.getPersonaId() : source.personaId();

        String title = "商品转短视频·" + safeProductName(product);
        String content = buildShortVideoScriptContent(product, source.content(), style, duration);

        SvScriptSaveVO scriptVo = new SvScriptSaveVO();
        scriptVo.setTitle(title);
        scriptVo.setContent(content);
        scriptVo.setScriptType("soft_ad");
        scriptVo.setGenerationType(source.generationType());
        scriptVo.setTheme("product:" + product.getId());
        scriptVo.setStyle(style);
        scriptVo.setDuration(duration);
        scriptVo.setWordCount(content.length());
        if (personaId != null) {
            scriptVo.setPersonaId(personaId);
        }
        scriptVo.setTags("[\"product:" + product.getId() + "\"]");
        Long scriptId = svScriptService.save(scriptVo, ownerId);

        SvProjectSaveVO projectVo = new SvProjectSaveVO();
        projectVo.setTitle(title);
        projectVo.setProjectType("soft_ad");
        projectVo.setStatus("draft");
        projectVo.setScriptId(scriptId);
        projectVo.setPersonaId(personaId);
        projectVo.setDuration(duration);
        projectVo.setThumbnailUrl(product.getImageUrl());
        projectVo.setRelatedProductIds(List.of(product.getId()));
        Long projectId = svProjectService.save(projectVo, ownerId);

        return new ProductScriptExportToShortVideoResultVO(scriptId, projectId, title);
    }

    private ScriptSource resolveScriptSource(ProductScriptExportToShortVideoVO vo, DyProduct product, Long ownerId) {
        if (vo.getVersionId() != null && vo.getVersionId() > 0) {
            ProductScriptVersion version = productScriptVersionRepository.findById(vo.getVersionId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_SCRIPT_NOT_FOUND, "指定话术版本不存在"));
            if (!product.getId().equals(version.getProductId())) {
                throw new BusinessException(ErrorCode.VALIDATION_FAIL, "话术版本不属于该商品");
            }
            if (!RequestRoleResolver.isAdmin() && !ownerId.equals(version.getOwnerId())) {
                throw new BusinessException(ErrorCode.PRODUCT_FORBIDDEN, "无权限使用该话术版本");
            }
            if (!StringUtils.hasText(version.getContent())) {
                throw new BusinessException(ErrorCode.PRODUCT_SCRIPT_NOT_FOUND, "指定话术版本内容为空");
            }
            return new ScriptSource(
                    version.getContent(),
                    version.getStyle(),
                    null,
                    "from_product_version",
                    null
            );
        }

        List<DyProductScript> scripts = productScriptRepository.findByProductIdAndIsActiveAndDeleted(product.getId(), true, 0);
        if (scripts.isEmpty()) {
            scripts = productScriptRepository.findByProductIdAndDeletedOrderByCreateTimeDesc(product.getId(), 0);
        }
        DyProductScript selected = scripts.stream()
                .filter(s -> StringUtils.hasText(s.getScriptContent()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_SCRIPT_NOT_FOUND, "该商品无可导出的有效话术"));
        if (selected.getCreatedBy() != null && !RequestRoleResolver.isAdmin() && !ownerId.equals(selected.getCreatedBy())
                && !ownerId.equals(product.getUserId())) {
            throw new BusinessException(ErrorCode.PRODUCT_FORBIDDEN, "无权限使用该商品话术");
        }
        return new ScriptSource(
                selected.getScriptContent(),
                selected.getStyle(),
                selected.getDuration(),
                "from_product_script",
                selected.getPersonaId()
        );
    }

    private String buildShortVideoScriptContent(DyProduct product, String scriptContent, String style, int duration) {
        StringBuilder content = new StringBuilder();
        content.append("# ").append(safeProductName(product)).append("短视频脚本\n\n");
        content.append("目标风格：").append(style).append("\n");
        content.append("目标时长：").append(duration).append("秒\n\n");
        if (StringUtils.hasText(product.getProductCategory())) {
            content.append("商品品类：").append(product.getProductCategory().trim()).append("\n");
        }
        if (product.getPrice() != null) {
            content.append("商品价格：").append(product.getPrice()).append("\n");
        }
        if (StringUtils.hasText(product.getAiSellingPoints())) {
            content.append("\n## 商品卖点\n").append(product.getAiSellingPoints().trim()).append("\n");
        } else if (StringUtils.hasText(product.getDescription())) {
            content.append("\n## 商品描述\n").append(product.getDescription().trim()).append("\n");
        }
        content.append("\n## 成片话术\n").append(scriptContent.trim()).append("\n");
        return content.toString().trim();
    }

    private static int sanitizeDuration(Integer duration) {
        if (duration == null || duration <= 0) return 60;
        return Math.max(15, Math.min(duration, 300));
    }

    private static String safeProductName(DyProduct product) {
        return StringUtils.hasText(product.getProductName()) ? product.getProductName().trim() : "商品" + product.getId();
    }

    private static String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) return value.trim();
        }
        return "";
    }

    private record ScriptSource(String content, String style, Integer duration, String generationType, Long personaId) {
    }
}
