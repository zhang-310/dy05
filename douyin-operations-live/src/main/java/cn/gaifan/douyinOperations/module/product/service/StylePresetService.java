package cn.gaifan.douyinOperations.module.product.service;

import cn.gaifan.douyinOperations.module.product.entity.StylePreset;

import java.util.List;

/**
 * 风格预设模板服务
 */
public interface StylePresetService {

    List<StylePreset> listEnabled();

    List<StylePreset> listAll();

    StylePreset getById(Long id);

    StylePreset getByCode(String presetCode);

    StylePreset save(StylePreset preset, Long userId);

    void delete(Long id, Long userId);

    /**
     * 智能推荐风格：根据产品特征（分类、价格、标签）推荐最佳风格
     *
     * @param productId 产品 ID
     * @param userId    用户 ID
     * @return 推荐的风格编码列表（按推荐度排序）
     */
    List<String> recommendStyles(Long productId, Long userId);
}
