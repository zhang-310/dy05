package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.module.live.entity.LiveSession;
import cn.gaifan.douyinOperations.module.live.repository.LiveSessionRepository;
import cn.gaifan.douyinOperations.module.live.service.LiveSessionReadService;
import jakarta.annotation.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 直播场次只读查询服务实现。
 *
 * <p>为 attribution、dashboard、payment 等跨模块消费者提供类型安全的只读 DTO 访问，
 * 避免这些模块直接依赖 {@code LiveSession} Entity 或 {@code LiveSessionRepository}。
 */
@Service
@Transactional(readOnly = true)
public class LiveSessionReadServiceImpl implements LiveSessionReadService {

    @Resource
    private LiveSessionRepository liveSessionRepository;

    @Override
    public Optional<SessionSummary> findSummaryById(Long sessionId) {
        if (sessionId == null) return Optional.empty();
        return liveSessionRepository.findByIdAndDeleted(sessionId, 0)
                .map(this::toSummary);
    }

    @Override
    public List<SessionSummary> findSummariesByUserId(Long userId, int page, int pageSize) {
        if (userId == null) return List.of();
        int safePageSize = Math.min(pageSize, 100);
        return liveSessionRepository.findByUserIdAndDeleted(
                        userId, 0,
                        PageRequest.of(page, safePageSize, Sort.by(Sort.Direction.DESC, "createTime")))
                .getContent()
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @Override
    public Map<Integer, Long> countByStatus(Long orgId) {
        // 使用内存聚合避免额外 JPQL，场次数量有限（单机构通常百~千量级）
        List<LiveSession> sessions = orgId == null
                ? liveSessionRepository.findAll(
                        (root, q, cb) -> cb.equal(root.get("deleted"), 0))
                : liveSessionRepository.findAll(
                        (root, q, cb) -> cb.and(
                                cb.equal(root.get("deleted"), 0),
                                cb.equal(root.get("orgId"), orgId)));
        return sessions.stream()
                .collect(Collectors.groupingBy(s -> s.getStatus() != null ? s.getStatus() : 0, Collectors.counting()));
    }

    @Override
    public BigDecimal sumGmvByDateRange(Long orgId, Timestamp startTime, Timestamp endTime) {
        // 注：live_session 本身无 cumulative_gmv 列，GMV 需由 live_product.revenue 聚合。
        // 此处返回 -1 作为"需调用方自行聚合"的信号，不做内存全表扫描。
        // 实际使用时，dashboard 等模块应直接用 LiveProductRepository 做 @Query 聚合。
        return BigDecimal.valueOf(-1);
    }

    private SessionSummary toSummary(LiveSession s) {
        return new SessionSummary(
                s.getId(),
                s.getUserId(),
                s.getOrgId(),
                s.getAccountId(),
                s.getLiveTitle(),
                s.getStatus(),
                s.getLiveFormat(),
                s.getSessionType(),
                null,   // cumulativeGmv 需聚合 live_product.revenue，此处留空，调用方按需查询
                null,   // productCount 按需查询
                null,   // scriptCount 按需查询
                s.getStartTime(),
                s.getEndTime(),
                s.getCreateTime()
        );
    }
}
