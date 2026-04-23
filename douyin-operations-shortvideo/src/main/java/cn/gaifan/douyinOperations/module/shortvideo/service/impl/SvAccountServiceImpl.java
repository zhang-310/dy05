package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvAccount;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvAccountCollectTask;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvViralVideo;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvAccountCollectTaskRepository;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvAccountRepository;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvViralVideoRepository;
import cn.gaifan.douyinOperations.module.shortvideo.service.SvAccountService;
import cn.gaifan.douyinOperations.module.shortvideo.vo.*;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 短视频账号管理服务实现
 */
@Slf4j
@Service
public class SvAccountServiceImpl implements SvAccountService {

    @Resource
    private SvAccountRepository accountRepository;

    @Resource
    private SvViralVideoRepository viralVideoRepository;

    @Resource
    private SvAccountCollectTaskRepository taskRepository;

    @Override
    public PageResultVO<SvAccountVO> searchAccounts(SvAccountSearchVO vo, Long userId) {
        vo.validateParams();

        Specification<SvAccount> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 数据隔离
            predicates.add(cb.equal(root.get("ownerId"), userId));

            // 关键词搜索（昵称、抖音号、sec_uid）
            if (StringUtils.hasText(vo.getKeyword())) {
                String keyword = "%" + vo.getKeyword().trim() + "%";
                predicates.add(cb.or(
                    cb.like(root.get("nickname"), keyword),
                    cb.like(root.get("douyinId"), keyword),
                    cb.like(root.get("secUid"), keyword)
                ));
            }

            // 账号分类
            if (StringUtils.hasText(vo.getAccountCategory())) {
                predicates.add(cb.equal(root.get("accountCategory"), vo.getAccountCategory()));
            }

            // 来源类型
            if (StringUtils.hasText(vo.getSourceType())) {
                predicates.add(cb.equal(root.get("sourceType"), vo.getSourceType()));
            }

            // 来源关键词
            if (StringUtils.hasText(vo.getSourceKeyword())) {
                predicates.add(cb.like(root.get("sourceKeyword"), "%" + vo.getSourceKeyword() + "%"));
            }

            // 账号状态
            if (StringUtils.hasText(vo.getStatus())) {
                predicates.add(cb.equal(root.get("status"), vo.getStatus()));
            }

            // 最小粉丝数
            if (vo.getMinFollowerCount() != null && vo.getMinFollowerCount() > 0) {
                predicates.add(cb.ge(root.get("followerCount"), vo.getMinFollowerCount()));
            }

            // 最小爆款评分
            if (vo.getMinViralScore() != null && vo.getMinViralScore() > 0) {
                predicates.add(cb.ge(root.get("avgViralScore"), vo.getMinViralScore()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // 排序
        Sort sort = Sort.by(Sort.Direction.DESC, "updateTime");
        if (StringUtils.hasText(vo.getSortName())) {
            Sort.Direction direction = "asc".equalsIgnoreCase(vo.getSortOrder())
                ? Sort.Direction.ASC : Sort.Direction.DESC;
            sort = Sort.by(direction, vo.getSortName());
        }

        PageRequest pageRequest = PageRequest.of(vo.getPage(), vo.getRows(), sort);
        Page<SvAccount> page = accountRepository.findAll(spec, pageRequest);

        List<SvAccountVO> list = page.getContent().stream()
            .map(this::toVO)
            .toList();

        return PageResultVO.of(page.getTotalElements(), list, vo.getPage(), vo.getRows());
    }

    @Override
    public SvAccountDetailVO getAccountDetail(Long id, Long userId) {
        SvAccount account = accountRepository.findByIdAndDeleted(id, 0)
            .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "账号不存在"));

        if (!account.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该账号");
        }

        SvAccountDetailVO vo = new SvAccountDetailVO();
        BeanUtils.copyProperties(account, vo);

        // 统计采集任务数
        long taskCount = taskRepository.count((root, query, cb) -> cb.and(
            cb.equal(root.get("svAccountId"), id),
            cb.equal(root.get("deleted"), 0)
        ));
        vo.setTaskCount((int) taskCount);

        // 统计视频数
        long totalVideos = viralVideoRepository.count((root, query, cb) -> cb.and(
            cb.equal(root.get("svAccountId"), id),
            cb.equal(root.get("deleted"), 0)
        ));

        long analyzedVideos = viralVideoRepository.count((root, query, cb) -> cb.and(
            cb.equal(root.get("svAccountId"), id),
            cb.equal(root.get("deleted"), 0),
            cb.isNotNull(root.get("deepAnalysisResult"))
        ));

        vo.setPendingAnalysisCount((int) (totalVideos - analyzedVideos));
        vo.setAnalyzedCount((int) analyzedVideos);

        // 最近一次采集任务
        taskRepository.findAll((root, query, cb) -> {
            query.orderBy(cb.desc(root.get("createTime")));
            return cb.and(
                cb.equal(root.get("svAccountId"), id),
                cb.equal(root.get("deleted"), 0)
            );
        }, PageRequest.of(0, 1)).stream().findFirst()
            .ifPresent(task -> vo.setLatestTaskId(task.getId()));

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAccount(SvAccountUpdateVO vo, Long userId) {
        SvAccount account = accountRepository.findByIdAndDeleted(vo.getId(), 0)
            .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "账号不存在"));

        if (!account.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权修改该账号");
        }

        // 更新字段
        if (StringUtils.hasText(vo.getAccountCategory())) {
            account.setAccountCategory(vo.getAccountCategory());
        }
        if (vo.getIndustryTags() != null) {
            account.setIndustryTags(vo.getIndustryTags());
        }
        if (vo.getContentTags() != null) {
            account.setContentTags(vo.getContentTags());
        }
        if (vo.getNotes() != null) {
            account.setNotes(vo.getNotes());
        }
        if (StringUtils.hasText(vo.getStatus())) {
            account.setStatus(vo.getStatus());
        }

        accountRepository.save(account);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAccount(Long id, Long userId) {
        SvAccount account = accountRepository.findByIdAndDeleted(id, 0)
            .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "账号不存在"));

        if (!account.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权删除该账号");
        }

        account.setDeleted(1);
        accountRepository.save(account);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SvAccount findOrCreateAccount(String secUid, String nickname, String sourceType,
                                         String sourceKeyword, Long sourceTaskId, Long userId) {
        if (!StringUtils.hasText(secUid)) {
            return null;
        }

        // 查找现有账号
        Optional<SvAccount> existing = accountRepository.findByOwnerIdAndSecUidAndDeleted(userId, secUid, 0);
        if (existing.isPresent()) {
            SvAccount account = existing.get();
            // 更新采集次数
            account.setCollectCount(account.getCollectCount() + 1);
            account.setLastCollectTime(new Timestamp(System.currentTimeMillis()));
            return accountRepository.save(account);
        }

        // 创建新账号
        SvAccount account = new SvAccount();
        account.setOwnerId(userId);
        account.setSecUid(secUid);
        account.setNickname(nickname);
        account.setSourceType(sourceType != null ? sourceType : "manual");
        account.setSourceKeyword(sourceKeyword);
        account.setSourceTaskId(sourceTaskId);
        account.setCollectCount(1);
        account.setLastCollectTime(new Timestamp(System.currentTimeMillis()));

        log.info("[账号管理] 创建新账号: secUid={}, nickname={}, sourceType={}", secUid, nickname, sourceType);
        return accountRepository.save(account);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAccountStatistics(Long accountId) {
        if (accountId == null) {
            return;
        }

        SvAccount account = accountRepository.findByIdAndDeleted(accountId, 0).orElse(null);
        if (account == null) {
            return;
        }

        // 查询该账号的所有视频
        List<SvViralVideo> videos = viralVideoRepository.findAll((root, query, cb) -> cb.and(
            cb.equal(root.get("svAccountId"), accountId),
            cb.equal(root.get("deleted"), 0)
        ));

        if (videos.isEmpty()) {
            return;
        }

        // 计算统计数据
        int totalVideos = videos.size();
        account.setTotalCollectedVideos(totalVideos);

        // 平均播放量
        long totalViews = videos.stream()
            .mapToLong(v -> v.getViewCount() != null ? v.getViewCount() : 0)
            .sum();
        account.setAvgViewCount(totalViews / totalVideos);

        // 平均点赞数
        long totalLikes = videos.stream()
            .mapToLong(v -> v.getLikeCount() != null ? v.getLikeCount() : 0)
            .sum();
        account.setAvgLikeCount((int) (totalLikes / totalVideos));

        // 平均分享数
        long totalShares = videos.stream()
            .mapToLong(v -> v.getShareCount() != null ? v.getShareCount() : 0)
            .sum();
        account.setAvgShareCount((int) (totalShares / totalVideos));

        // 平均爆款评分
        double avgScore = videos.stream()
            .mapToInt(v -> v.getViralScore() != null ? v.getViralScore() : 0)
            .average()
            .orElse(0.0);
        account.setAvgViralScore(BigDecimal.valueOf(avgScore).setScale(2, RoundingMode.HALF_UP));

        // 最高爆款评分
        int topScore = videos.stream()
            .mapToInt(v -> v.getViralScore() != null ? v.getViralScore() : 0)
            .max()
            .orElse(0);
        account.setTopViralScore(BigDecimal.valueOf(topScore));

        accountRepository.save(account);
        log.info("[账号管理] 更新账号统计: accountId={}, totalVideos={}, avgScore={}",
            accountId, totalVideos, avgScore);
    }

    @Override
    public PageResultVO<ViralVideoVO> getAccountVideos(AccountVideosQueryVO query, Long userId) {
        if (query == null || query.getAccountId() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "accountId 不能为空");
        }
        Long accountId = query.getAccountId();
        int page = query.getPage() != null && query.getPage() >= 0 ? query.getPage() : 0;
        int rows = query.getRows() != null && query.getRows() >= 1 ? Math.min(query.getRows(), 200) : 20;

        SvAccount account = accountRepository.findByIdAndDeleted(accountId, 0)
            .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "账号不存在"));
        if (!account.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该账号");
        }

        Specification<SvViralVideo> spec = (root, q, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("svAccountId"), accountId));
            predicates.add(cb.equal(root.get("deleted"), 0));
            if (StringUtils.hasText(query.getKeyword())) {
                String kw = "%" + query.getKeyword().trim() + "%";
                predicates.add(cb.like(root.get("title"), kw));
            }
            if (StringUtils.hasText(query.getDeepAnalyzeStatus())) {
                predicates.add(cb.equal(root.get("deepAnalyzeStatus"), query.getDeepAnalyzeStatus().trim()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Sort sort = resolveAccountVideoSort(query.getSortName(), query.getSortOrder());
        PageRequest pageRequest = PageRequest.of(page, rows, sort);
        Page<SvViralVideo> videoPage = viralVideoRepository.findAll(spec, pageRequest);

        List<ViralVideoVO> list = videoPage.getContent().stream()
            .map(this::toViralVideoListItemVO)
            .toList();

        return PageResultVO.of(videoPage.getTotalElements(), list, page, rows);
    }

    private static Sort resolveAccountVideoSort(String sortName, String sortOrder) {
        String field = "createTime";
        if (StringUtils.hasText(sortName)) {
            String s = sortName.trim();
            if (Set.of("createTime", "viewCount", "viralScore", "updateTime", "id").contains(s)) {
                field = s;
            }
        }
        Sort.Direction dir = Sort.Direction.DESC;
        if (StringUtils.hasText(sortOrder) && "asc".equalsIgnoreCase(sortOrder.trim())) {
            dir = Sort.Direction.ASC;
        }
        return Sort.by(dir, field);
    }

    @Override
    public SvAccountAnalyticsVO getAccountAnalytics(Long accountId, Long userId) {
        SvAccount account = accountRepository.findByIdAndDeleted(accountId, 0)
            .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "账号不存在"));
        if (!account.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该账号");
        }

        SvAccountAnalyticsVO vo = new SvAccountAnalyticsVO();
        Object[] row = viralVideoRepository.aggregateStatsBySvAccountId(accountId);
        if (row == null || row.length < 7) {
            applyDeepAnalyzeCounts(vo, accountId);
            return vo;
        }

        long videoCount = toLongPrimitive(row[0]);
        vo.setVideoCount((int) Math.min(videoCount, Integer.MAX_VALUE));
        if (videoCount <= 0) {
            applyDeepAnalyzeCounts(vo, accountId);
            return vo;
        }

        vo.setSumViewCount(toLongPrimitive(row[1]));
        vo.setSumLikeCount(toLongPrimitive(row[2]));
        vo.setSumShareCount(toLongPrimitive(row[3]));
        vo.setAvgViralScore(toDoublePrimitive(row[4]));
        vo.setMaxViewCount(toLongPrimitive(row[5]));
        vo.setMinViewCount(toLongPrimitive(row[6]));

        double n = videoCount;
        vo.setAvgViewPerVideo(vo.getSumViewCount() / n);
        vo.setAvgLikePerVideo(vo.getSumLikeCount() / n);
        vo.setAvgSharePerVideo(vo.getSumShareCount() / n);

        applyDeepAnalyzeCounts(vo, accountId);
        return vo;
    }

    private void applyDeepAnalyzeCounts(SvAccountAnalyticsVO vo, Long accountId) {
        List<Object[]> groups = viralVideoRepository.countDeepAnalyzeGroupedBySvAccountId(accountId);
        for (Object[] g : groups) {
            String st = g[0] != null ? String.valueOf(g[0]).trim().toLowerCase() : "";
            int c = toIntPrimitive(g[1]);
            if ("pending".equals(st)) {
                vo.setDeepPendingCount(vo.getDeepPendingCount() + c);
            } else if ("processing".equals(st)) {
                vo.setDeepProcessingCount(vo.getDeepProcessingCount() + c);
            } else if ("completed".equals(st)) {
                vo.setDeepCompletedCount(vo.getDeepCompletedCount() + c);
            } else if ("failed".equals(st)) {
                vo.setDeepFailedCount(vo.getDeepFailedCount() + c);
            } else {
                vo.setDeepOtherCount(vo.getDeepOtherCount() + c);
            }
        }
    }

    private static long toLongPrimitive(Object o) {
        return o == null ? 0L : ((Number) o).longValue();
    }

    private static double toDoublePrimitive(Object o) {
        return o == null ? 0d : ((Number) o).doubleValue();
    }

    private static int toIntPrimitive(Object o) {
        return o == null ? 0 : ((Number) o).intValue();
    }

    // ─── 工具方法 ──────────────────────────────────────

    private SvAccountVO toVO(SvAccount entity) {
        SvAccountVO vo = new SvAccountVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    /** 账号视频分页列表：不返回 deepAnalysisResult 等大字段，降低响应体积 */
    private ViralVideoVO toViralVideoListItemVO(SvViralVideo entity) {
        ViralVideoVO vo = new ViralVideoVO();
        BeanUtils.copyProperties(entity, vo);
        vo.setDeepAnalysisResult(null);
        return vo;
    }
}

