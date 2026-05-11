package cn.gaifan.douyinOperations.module.product.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.product.entity.DyProduct;
import cn.gaifan.douyinOperations.module.product.entity.StylePreset;
import cn.gaifan.douyinOperations.module.product.repository.DyProductRepository;
import cn.gaifan.douyinOperations.module.product.repository.StylePresetRepository;
import cn.gaifan.douyinOperations.module.product.service.StylePresetService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class StylePresetServiceImpl implements StylePresetService {

    private static final Logger log = LoggerFactory.getLogger(StylePresetServiceImpl.class);

    @Resource
    private StylePresetRepository stylePresetRepository;

    @Resource
    private DyProductRepository productRepository;

    @Override
    public List<StylePreset> listEnabled() {
        return stylePresetRepository.findByIsEnabledTrueOrderBySortOrderAsc();
    }

    @Override
    public List<StylePreset> listAll() {
        return stylePresetRepository.findAllByOrderBySortOrderAsc();
    }

    @Override
    public StylePreset getById(Long id) {
        return stylePresetRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "风格预设不存在"));
    }

    @Override
    public StylePreset getByCode(String presetCode) {
        return stylePresetRepository.findByPresetCodeAndDeleted(presetCode, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "风格预设不存在: " + presetCode));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StylePreset save(StylePreset preset, Long userId) {
        if (preset == null) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "参数不能为空");
        if (preset.getPresetName() == null || preset.getPresetName().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "风格名称不能为空");
        }
        if (preset.getPresetCode() == null || preset.getPresetCode().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "风格编码不能为空");
        }
        if (preset.getStyleValue() == null || preset.getStyleValue().isBlank()) {
            preset.setStyleValue(preset.getPresetCode());
        }
        preset.setCreatedBy(userId);
        return stylePresetRepository.save(preset);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, Long userId) {
        StylePreset preset = stylePresetRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "风格预设不存在"));

        // P0-1: 数据所有权校验
        if (preset.getCreatedBy() != null && !preset.getCreatedBy().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限删除该风格预设");
        }

        preset.setDeleted(1);
        stylePresetRepository.save(preset);
    }

    @Override
    public List<String> recommendStyles(Long productId, Long userId) {
        DyProduct product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "产品不存在"));

        List<String> recommended = new ArrayList<>();

        // 规则 1: 根据产品分类推荐
        String category = product.getProductCategory();
        if (category != null && !category.isBlank()) {
            if (category.contains("护肤") || category.contains("精华") || category.contains("面膜")) {
                recommended.add("professional"); // 专业
                recommended.add("warm"); // 温暖
            } else if (category.contains("彩妆") || category.contains("口红")) {
                recommended.add("enthusiastic"); // 热情
                recommended.add("trendy"); // 时尚
            }
        }

        // 规则 2: 根据价格区间推荐
        if (product.getPrice() != null) {
            double price = product.getPrice().doubleValue();
            if (price >= 500) {
                // 高端产品：专业、优雅
                if (!recommended.contains("professional")) recommended.add("professional");
                if (!recommended.contains("elegant")) recommended.add("elegant");
            } else if (price >= 200) {
                // 中端产品：温暖、亲和
                if (!recommended.contains("warm")) recommended.add("warm");
                if (!recommended.contains("friendly")) recommended.add("friendly");
            } else {
                // 平价产品：热情、活力
                if (!recommended.contains("enthusiastic")) recommended.add("enthusiastic");
                if (!recommended.contains("energetic")) recommended.add("energetic");
            }
        }

        // 规则 3: 根据卖点关键词推荐
        String sellingPoints = product.getAiSellingPoints();
        if (sellingPoints != null && !sellingPoints.isBlank()) {
            if (sellingPoints.contains("科技") || sellingPoints.contains("成分") || sellingPoints.contains("专利")) {
                if (!recommended.contains("professional")) recommended.add("professional");
            }
            if (sellingPoints.contains("天然") || sellingPoints.contains("温和") || sellingPoints.contains("敏感肌")) {
                if (!recommended.contains("warm")) recommended.add("warm");
            }
            if (sellingPoints.contains("网红") || sellingPoints.contains("爆款") || sellingPoints.contains("明星")) {
                if (!recommended.contains("trendy")) recommended.add("trendy");
            }
        }

        // 默认推荐：如果没有匹配到任何规则，返回通用风格
        if (recommended.isEmpty()) {
            recommended.add("professional");
            recommended.add("warm");
            recommended.add("enthusiastic");
        }

        // 限制返回数量为 3-5 个
        if (recommended.size() > 5) {
            recommended = recommended.subList(0, 5);
        }

        log.info("[风格推荐] productId={}, category={}, price={}, recommended={}",
                productId, category, product.getPrice(), recommended);

        return recommended;
    }
}
