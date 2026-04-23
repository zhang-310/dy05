package cn.gaifan.douyinOperations.module.product.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.product.vo.*;

public interface ProductService {

    PageResultVO<ProductVO> search(ProductSearchVO vo);

    ProductVO getById(Long id);

    long save(ProductSaveVO vo);

    void delete(Long id);

    void batchDelete(java.util.List<Long> ids);

    void updateInventory(Long id, Long quantity);

    void publish(Long id);

    void unpublish(Long id);

    void setFeatured(Long id, Integer featured);

    /**
     * 根据产品属性自动推断产品分类（用于直播选品话术生成）
     * @param productId 产品ID
     * @return 分类组合，逗号分隔，如 "hot,profit"；无产品返回 null
     */
    String inferProductType(Long productId);
}
