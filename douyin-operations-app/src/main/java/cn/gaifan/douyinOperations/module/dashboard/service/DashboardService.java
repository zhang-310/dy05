package cn.gaifan.douyinOperations.module.dashboard.service;

import cn.gaifan.douyinOperations.module.ai.repository.AiCallLogRepository;
import cn.gaifan.douyinOperations.module.auth.repository.AuthUserRepository;
import cn.gaifan.douyinOperations.module.copy.repository.CopyLibraryRepository;
import cn.gaifan.douyinOperations.module.douyin.repository.DouyinVideoRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveSessionRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveProductRepository;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvVideoRepository;
import jakarta.annotation.Resource;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Dashboard 统计服务
 * 提供管理员和机构级别的统计数据，包含用户、视频、直播、文案、AI 等多个维度
 */
@Service
public class DashboardService {

    @Resource
    private AuthUserRepository userRepository;

    @Resource
    private DouyinVideoRepository videoRepository;

    @Resource
    private LiveSessionRepository sessionRepository;

    @Resource
    private LiveProductRepository productRepository;

    @Resource
    private SvVideoRepository svVideoRepository;

    @Resource
    private CopyLibraryRepository copyLibraryRepository;

    @Resource
    private AiCallLogRepository aiCallLogRepository;

    /**
     * 获取管理员 Dashboard 统计数据
     * 缓存 5 分钟，包含用户、视频、直播、短视频、文案、AI 调用等全量统计
     */
    @Cacheable(value = "dashboard:admin", unless = "#result == null")
    public Map<String, Object> getAdminStats() {
        Map<String, Object> stats = new HashMap<>();

        // 今日开始时间戳
        Timestamp todayStart = Timestamp.valueOf(LocalDateTime.of(LocalDate.now(), LocalTime.MIN));

        // ===== 用户统计 =====
        stats.put("totalUsers", userRepository.countByDeleted(0));
        stats.put("activeUsers", userRepository.countByStatusAndDeleted(0, 0));
        stats.put("todayUsers", userRepository.countByCreateTimeAfterAndDeleted(todayStart, 0));

        // ===== 抖音视频统计 =====
        stats.put("totalVideos", videoRepository.countByDeleted(0));
        stats.put("publishedVideos", videoRepository.countByStatusAndDeleted(1, 0));
        stats.put("todayVideos", videoRepository.countByCreateTimeAfterAndDeleted(todayStart, 0));

        // ===== 直播统计 =====
        stats.put("totalLiveSessions", sessionRepository.countByDeleted(0));
        stats.put("completedSessions", sessionRepository.countByStatusAndDeleted(2, 0));
        stats.put("todaySessions", sessionRepository.countByCreateTimeAfterAndDeleted(todayStart, 0));

        // ===== 短视频统计 =====
        stats.put("totalShortVideos", svVideoRepository.countByDeleted(0));
        stats.put("publishedShortVideos", svVideoRepository.countPublishedVideos());
        stats.put("todayShortVideos", svVideoRepository.countByCreateTimeAfterAndDeleted(todayStart, 0));

        // ===== 文案库统计 =====
        stats.put("totalCopyItems", copyLibraryRepository.countByDeleted(0));
        stats.put("approvedCopyItems", copyLibraryRepository.countApprovedCopies());
        stats.put("todayCopyItems", copyLibraryRepository.countByCreateTimeAfterAndDeleted(todayStart, 0));

        // ===== AI 调用统计 =====
        long totalAiCalls = aiCallLogRepository.countByCreateTimeAfterAndStatus(todayStart, 1);
        long totalAiAttempts = totalAiCalls + aiCallLogRepository.countFailedCallsSince(todayStart);
        stats.put("todayAiCalls", totalAiCalls);
        stats.put("todayAiAttempts", totalAiAttempts);
        // 成功率：成功数 / 总尝试数
        BigDecimal aiSuccessRate = calculateSuccessRate(totalAiCalls, totalAiAttempts);
        stats.put("todayAiSuccessRate", aiSuccessRate);

        // ===== 财务统计 =====
        BigDecimal todayRevenue = calculateTodayRevenue(todayStart);
        stats.put("todayRevenue", todayRevenue);

        return stats;
    }

    /**
     * 获取机构 Dashboard 统计数据
     * 按 userId（机构用户 ID）过滤，缓存 5 分钟
     * 注意：调用方应确保已做权限校验
     *
     * @param userId 机构用户 ID（通常是机构管理员 ID）
     * @return 机构维度统计数据
     */
    @Cacheable(value = "dashboard:org", key = "#userId", unless = "#result == null")
    public Map<String, Object> getOrgStats(Long userId) {
        Map<String, Object> stats = new HashMap<>();

        // 今日开始时间戳
        Timestamp todayStart = Timestamp.valueOf(LocalDateTime.of(LocalDate.now(), LocalTime.MIN));

        // ===== 抖音视频统计 =====
        stats.put("totalVideos", videoRepository.countByOwnerIdAndDeleted(userId, 0));
        stats.put("publishedVideos", videoRepository.countByOwnerIdAndStatusAndDeleted(userId, 1, 0));
        stats.put("todayVideos", videoRepository.countByOwnerIdAndCreateTimeAfterAndDeleted(userId, todayStart, 0));

        // ===== 直播统计 =====
        stats.put("totalLiveSessions", sessionRepository.countByOwnerIdAndDeleted(userId, 0));
        stats.put("completedSessions", sessionRepository.countByOwnerIdAndStatusAndDeleted(userId, 2, 0));
        stats.put("todaySessions", sessionRepository.countByOwnerIdAndCreateTimeAfterAndDeleted(userId, todayStart, 0));

        // ===== 短视频统计 =====
        stats.put("totalShortVideos", svVideoRepository.countByOwnerIdAndDeleted(userId, 0));
        stats.put("publishedShortVideos", svVideoRepository.countPublishedVideosByOwnerId(userId));
        stats.put("todayShortVideos", svVideoRepository.countByOwnerIdAndCreateTimeAfterAndDeleted(userId, todayStart, 0));

        // ===== 文案库统计 =====
        stats.put("totalCopyItems", copyLibraryRepository.countByUserIdAndDeleted(userId, 0));
        stats.put("approvedCopyItems", copyLibraryRepository.countApprovedCopiesByUserId(userId));
        stats.put("todayCopyItems", copyLibraryRepository.countByUserIdAndCreateTimeAfterAndDeleted(userId, todayStart, 0));

        // ===== AI 调用统计 =====
        long userAiCalls = aiCallLogRepository.countByUserIdAndCreateTimeAfterAndStatus(userId, todayStart, 1);
        long userAiAttempts = userAiCalls + aiCallLogRepository.countFailedCallsByUserIdSince(userId, todayStart);
        stats.put("todayAiCalls", userAiCalls);
        stats.put("todayAiAttempts", userAiAttempts);
        // 成功率：成功数 / 总尝试数
        BigDecimal aiSuccessRate = calculateSuccessRate(userAiCalls, userAiAttempts);
        stats.put("todayAiSuccessRate", aiSuccessRate);

        // ===== 财务统计 =====
        BigDecimal todayRevenue = calculateOrgTodayRevenue(userId, todayStart);
        stats.put("todayRevenue", todayRevenue);

        return stats;
    }

    /**
     * 计算全局今日总收入
     *
     * @param todayStart 今日开始时间戳
     * @return 今日收入（BigDecimal）
     */
    private BigDecimal calculateTodayRevenue(Timestamp todayStart) {
        // 从 live_product 汇总今日销售额
        BigDecimal revenue = productRepository.sumRevenueByCreateTimeAfter(todayStart);
        return revenue != null ? revenue : BigDecimal.ZERO;
    }

    /**
     * 计算机构今日收入
     *
     * @param userId     机构用户 ID
     * @param todayStart 今日开始时间戳
     * @return 机构今日收入（BigDecimal）
     */
    private BigDecimal calculateOrgTodayRevenue(Long userId, Timestamp todayStart) {
        // 从 live_product 汇总机构今日销售额
        BigDecimal revenue = productRepository.sumRevenueByOwnerIdAndCreateTimeAfter(userId, todayStart);
        return revenue != null ? revenue : BigDecimal.ZERO;
    }

    /**
     * 计算成功率
     *
     * @param successCount 成功数
     * @param totalCount   总数
     * @return 成功率（百分比形式，保留两位小数）
     */
    private BigDecimal calculateSuccessRate(long successCount, long totalCount) {
        if (totalCount <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(successCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalCount), 2, java.math.RoundingMode.HALF_UP);
    }
}
