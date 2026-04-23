package cn.gaifan.douyinOperations.module.ai.repository;

import cn.gaifan.douyinOperations.module.ai.entity.AiEvolveTask;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

/**
 * 进化任务动态查询（列表分页、ROI 聚合等共用）
 */
public final class EvolveTaskSpecifications {

    private EvolveTaskSpecifications() {
    }

    public static Specification<AiEvolveTask> kbIdOptional(Long kbId) {
        if (kbId == null) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.equal(root.get("kbId"), kbId);
    }

    public static Specification<AiEvolveTask> evolveAngleOptional(String evolveAngle) {
        if (evolveAngle == null || evolveAngle.isBlank()) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.equal(root.get("evolveAngle"), evolveAngle);
    }

    /** 与库内 status 字符串精确匹配 */
    public static Specification<AiEvolveTask> statusExactOptional(String status) {
        if (status == null || status.isBlank()) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    /**
     * 与任务列表 UI 状态一致（参见 EvolutionController.mapTaskStatus 分组）
     */
    public static Specification<AiEvolveTask> uiStatusCategoryOptional(Integer uiCategory) {
        if (uiCategory == null) {
            return (root, query, cb) -> cb.conjunction();
        }
        return switch (uiCategory) {
            case 0 -> (root, query, cb) -> root.get("status").in(List.of("pending", "gathering"));
            case 1 -> (root, query, cb) -> root.get("status").in(List.of(
                    "generating", "scoring", "expanding", "indexing", "pending_review"));
            case 2 -> (root, query, cb) -> cb.equal(root.get("status"), "completed");
            case 3 -> (root, query, cb) -> root.get("status").in(List.of("failed", "blocked"));
            case 4 -> (root, query, cb) -> cb.equal(root.get("status"), "canceled");
            default -> (root, query, cb) -> cb.conjunction();
        };
    }

    public static Specification<AiEvolveTask> listFilter(Long kbId, String evolveAngle, String statusExact, Integer uiStatusCategory) {
        Specification<AiEvolveTask> spec = Specification.where(kbIdOptional(kbId))
                .and(evolveAngleOptional(evolveAngle));
        if (statusExact != null && !statusExact.isBlank()) {
            spec = spec.and(statusExactOptional(statusExact));
        } else {
            spec = spec.and(uiStatusCategoryOptional(uiStatusCategory));
        }
        return spec;
    }
}
