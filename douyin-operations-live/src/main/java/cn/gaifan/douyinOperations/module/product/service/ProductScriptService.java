package cn.gaifan.douyinOperations.module.product.service;

import cn.gaifan.douyinOperations.module.product.entity.DyProductScript;
import cn.gaifan.douyinOperations.module.product.vo.BatchGenerateRequestVO;
import cn.gaifan.douyinOperations.module.product.vo.MultiStyleGenerateRequestVO;
import cn.gaifan.douyinOperations.module.product.vo.MultiStyleGenerateResultVO;
import cn.gaifan.douyinOperations.module.product.vo.ProductScriptSaveVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

    /**
 * 产品话术服务
 */
public interface ProductScriptService {

    /**
     * 保存产品话术
     */
    DyProductScript saveScript(ProductScriptSaveVO vo, Long userId);

    /**
     * 获取产品所有话术
     */
    List<DyProductScript> listScripts(Long productId, Long userId);

    /**
     * 获取产品指定类型的话术
     */
    List<DyProductScript> listScriptsByType(Long productId, String scriptType, Long userId);

    /**
     * 按风格分组列出产品指定类型的话术（v2.0）
     */
    Map<String, List<DyProductScript>> listScriptsByStyle(Long productId, String scriptType, Long userId);

    /**
     * 获取产品的激活话术
     */
    List<DyProductScript> listActiveScripts(Long productId, Long userId);

    /**
     * 按风格获取各风格的激活话术（v2.0）
     */
    Map<String, DyProductScript> getActiveScriptsByStyle(Long productId, String scriptType, Long userId);

    /**
     * AI 多风格生成（同步）
     */
    MultiStyleGenerateResultVO generateMultiStyleScripts(MultiStyleGenerateRequestVO vo, Long userId);

    /**
     * AI 多风格生成（SSE 异步进度推送）
     */
    void generateMultiStyleScriptsWithProgress(MultiStyleGenerateRequestVO vo, Long userId, SseEmitter emitter);

    /**
     * 批量生成（支持 SSE 进度回调）
     */
    void generateBatchWithProgress(BatchGenerateRequestVO vo, Long userId, BatchProgressCallback callback);

    /**
     * 激活话术
     */
    void activateScript(Long scriptId, Long userId);

    /**
     * 更新话术
     */
    DyProductScript updateScript(Long scriptId, ProductScriptSaveVO vo, Long userId);

    /**
     * 删除话术
     */
    void deleteScript(Long scriptId, Long userId);

    /**
     * 获取话术详情
     */
    DyProductScript getScript(Long scriptId, Long userId);

    /**
     * 回滚到指定版本历史
     */
    DyProductScript rollbackToVersion(Long historyId, Long userId);

    /**
     * AI 生成单个话术
     */
    DyProductScript generateSingleScript(Long productId, String style, Long userId);

    /**
     * 获取话术使用统计
     */
    Map<String, Object> getScriptUsageStatistics(Long productId, Long userId);

    /**
     * 预览多个风格的话术片段
     */
    List<cn.gaifan.douyinOperations.module.product.vo.StylePreviewVO> previewStyles(MultiStyleGenerateRequestVO vo, Long userId);

    /**
     * 获取话术统计信息
     */
    Map<String, Object> getScriptStatistics(Long productId, Long userId);
}
