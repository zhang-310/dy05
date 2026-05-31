package cn.gaifan.douyinOperations.module.platform.ai;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 从 gf_ai_invocation 聚合 AI 用量（替代内存计数）。
 */
@Service
public class AiUsageStatsService {

    private final JdbcTemplate jdbcTemplate;

    public AiUsageStatsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Long> countByFeature(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return Map.of();
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                """
                        select feature_code, count(*) as cnt
                        from gf_ai_invocation
                        where tenant_id = ?
                        group by feature_code
                        """,
                tenantId
        );
        Map<String, Long> result = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String feature = String.valueOf(row.get("feature_code"));
            Long cnt = ((Number) row.get("cnt")).longValue();
            result.put(feature, cnt);
        }
        return result;
    }

    public long totalInvocations(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return 0L;
        }
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from gf_ai_invocation where tenant_id = ?",
                Long.class,
                tenantId
        );
        return count != null ? count : 0L;
    }
}
