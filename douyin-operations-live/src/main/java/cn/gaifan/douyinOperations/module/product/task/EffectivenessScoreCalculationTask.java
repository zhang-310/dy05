package cn.gaifan.douyinOperations.module.product.task;

import cn.gaifan.douyinOperations.module.product.entity.ProductScriptVersion;
import cn.gaifan.douyinOperations.module.product.repository.ProductScriptVersionRepository;
import cn.gaifan.douyinOperations.module.product.service.EffectivenessScoreService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 商品话术效果评分定时计算任务
 * 每天凌晨 02:00 执行
 *
 * @author Claude Code
 * @since 2026-03-06
 */
@Slf4j
@Component
public class EffectivenessScoreCalculationTask {

    @Resource
    private ProductScriptVersionRepository versionRepository;

    @Resource
    private EffectivenessScoreService scoreService;

    @Value("${app.product.effectiveness-score.scheduler.enabled:true}")
    private boolean schedulerEnabled;

    /**
     * 定时计算评分任务
     * 每天 02:00 执行
     */
    @Scheduled(cron = "${app.product.effectiveness-score.scheduler.cron:0 0 2 * * ?}")
    @Transactional(rollbackFor = Exception.class)
    public void calculateEffectivenessScoresScheduled() {
        if (!schedulerEnabled) {
            log.debug("效果评分定时计算已禁用，跳过");
            return;
        }
        log.info("开始执行定时效果评分计算任务");
        long startTime = System.currentTimeMillis();

        try {
            // 查询过去 24h 有更新的版本（created_at 或 updated_at 在最近 24h）
            LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
            Specification<ProductScriptVersion> spec = (root, query, cb) -> cb.and(
                    cb.or(
                            cb.greaterThan(root.get("updatedAt"), oneDayAgo),
                            cb.greaterThan(root.get("createdAt"), oneDayAgo)
                    ),
                    cb.equal(root.get("deleted"), 0)
            );

            List<ProductScriptVersion> versions = versionRepository.findAll(spec);
            log.info("查询到过去 24h 有更新的版本: 共 {} 个", versions.size());

            // 按产品分组处理
            Map<Long, List<ProductScriptVersion>> versionsByProduct = new HashMap<>();
            for (ProductScriptVersion version : versions) {
                versionsByProduct.computeIfAbsent(version.getProductId(), k -> new ArrayList<>()).add(version);
            }

            // 逐个产品重新计算评分
            int totalRecalculated = 0;
            for (Map.Entry<Long, List<ProductScriptVersion>> entry : versionsByProduct.entrySet()) {
                Long productId = entry.getKey();
                List<ProductScriptVersion> productVersions = entry.getValue();

                // 获取产品的所有者（使用第一个版本的 owner_id）
                if (!productVersions.isEmpty()) {
                    Long ownerId = productVersions.get(0).getOwnerId();
                    try {
                        Integer recalculated = scoreService.recalculateAllScores(productId, ownerId);
                        totalRecalculated += recalculated;
                        log.info("产品 {} 重新计算评分完成: 更新 {} 个版本", productId, recalculated);
                    } catch (Exception e) {
                        log.error("产品 {} 重新计算评分失败", productId, e);
                    }
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("定时效果评分计算任务完成: 总计重新计算 {} 个版本，耗时 {}ms", totalRecalculated, duration);
        } catch (Exception e) {
            log.error("定时效果评分计算任务异常", e);
        }
    }

    /**
     * 手动触发评分计算（用于调试和测试）
     */
    public void manualCalculate(Long productId, Long userId) {
        log.info("手动触发评分计算: productId={}, userId={}", productId, userId);
        try {
            Integer recalculated = scoreService.recalculateAllScores(productId, userId);
            log.info("手动评分计算完成: 更新 {} 个版本", recalculated);
        } catch (Exception e) {
            log.error("手动评分计算失败", e);
            throw e;
        }
    }
}
