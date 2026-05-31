package cn.gaifan.douyinOperations.module.product.service;

import cn.gaifan.douyinOperations.module.product.vo.ProductScriptExportToShortVideoResultVO;
import cn.gaifan.douyinOperations.module.product.vo.ProductScriptExportToShortVideoVO;

/**
 * 商品话术导出短视频桥接服务。
 *
 * <p>接口放在 product 模块，实际写入 sv_script/sv_project 的实现由 app 模块提供。</p>
 */
public interface ProductScriptShortVideoExportService {

    ProductScriptExportToShortVideoResultVO exportToShortVideoProject(ProductScriptExportToShortVideoVO vo, Long ownerId);
}
