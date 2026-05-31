package cn.gaifan.douyinOperations.module.benchmark.service.impl;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.benchmark.config.BenchmarkCacheConfig;
import cn.gaifan.douyinOperations.module.benchmark.entity.BenchmarkAccount;
import cn.gaifan.douyinOperations.module.benchmark.entity.BenchmarkVideo;
import cn.gaifan.douyinOperations.module.benchmark.metrics.BenchmarkMetrics;
import cn.gaifan.douyinOperations.module.benchmark.repository.BenchmarkAccountRepository;
import cn.gaifan.douyinOperations.module.benchmark.repository.BenchmarkVideoRepository;
import cn.gaifan.douyinOperations.module.benchmark.service.BenchmarkVideoService;
import cn.gaifan.douyinOperations.module.benchmark.service.DouyinCookieService;
import cn.gaifan.douyinOperations.module.benchmark.vo.*;
import cn.gaifan.douyinOperations.module.shortvideo.service.impl.AccountVideoScraper;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 对标视频管理服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BenchmarkVideoServiceImpl implements BenchmarkVideoService {

    private final BenchmarkVideoRepository videoRepository;
    private final BenchmarkAccountRepository accountRepository;
    private final DouyinCookieService cookieService;
    private final AccountVideoScraper accountVideoScraper;
    private final BenchmarkMetrics metrics;

    @Override
    @Cacheable(value = BenchmarkCacheConfig.CACHE_VIDEO_LIST,
               key = "#ownerId + ':' + #searchVO.hashCode()",
               unless = "#result == null || #result.total == 0")
    public PageResultVO<BenchmarkVideoVO> search(BenchmarkVideoSearchVO searchVO, Long ownerId) {
        searchVO.validateParams();

        // 数据隔离：优先使用 benchmark_video.ownerId，兼容老数据用账号归属兜底。
        List<Long> accountIds = accountRepository.findByOwnerIdAndPlatform(ownerId, "douyin").stream()
                .map(BenchmarkAccount::getId)
                .toList();
        if (searchVO.getBenchmarkAccountId() != null) {
            BenchmarkAccount account = accountRepository.findById(searchVO.getBenchmarkAccountId())
                    .orElseThrow(() -> new RuntimeException("账号不存在"));
            if (!account.getOwnerId().equals(ownerId)) {
                throw new RuntimeException("无权访问该账号的视频");
            }
        }

        Specification<BenchmarkVideo> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (searchVO.getBenchmarkAccountId() != null) {
                predicates.add(cb.equal(root.get("benchmarkAccountId"), searchVO.getBenchmarkAccountId()));
            } else {
                Predicate ownerPredicate = cb.equal(root.get("ownerId"), ownerId);
                if (!accountIds.isEmpty()) {
                    predicates.add(cb.or(ownerPredicate, root.get("benchmarkAccountId").in(accountIds)));
                } else {
                    predicates.add(ownerPredicate);
                }
            }

            // 关键词搜索
            if (StringUtils.hasText(searchVO.getKeyword())) {
                String keyword = "%" + searchVO.getKeyword() + "%";
                predicates.add(cb.like(root.get("title"), keyword));
            }

            // 分析状态过滤
            if (StringUtils.hasText(searchVO.getAnalysisStatus())) {
                predicates.add(cb.equal(root.get("analysisStatus"), searchVO.getAnalysisStatus()));
            }

            // 是否符合条件过滤
            if (searchVO.getIsQualified() != null) {
                predicates.add(cb.equal(root.get("isQualified"), searchVO.getIsQualified()));
            }

            // 点赞数范围过滤
            if (searchVO.getMinLikeCount() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("likeCount"), searchVO.getMinLikeCount()));
            }
            if (searchVO.getMaxLikeCount() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("likeCount"), searchVO.getMaxLikeCount()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Sort sort = Sort.by(Sort.Direction.DESC, "likeCount");
        Pageable pageable = PageRequest.of(searchVO.getPage(), searchVO.getRows(), sort);
        Page<BenchmarkVideo> page = videoRepository.findAll(spec, pageable);

        List<BenchmarkVideoVO> voList = page.getContent().stream()
                .map(this::entityToVO)
                .toList();

        return new PageResultVO<>(page.getTotalElements(), voList, searchVO.getPage(), searchVO.getRows());
    }

    @Override
    public BenchmarkVideoVO getById(Long id, Long ownerId) {
        BenchmarkVideo video = videoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("视频不存在"));
        if (video.getOwnerId() != null && video.getOwnerId().equals(ownerId)) {
            return entityToVO(video);
        }
        if (video.getOwnerId() != null && video.getOwnerId() != 0L) {
            throw new RuntimeException("无权访问该视频");
        }

        // 验证权限：通过账号的ownerId
        BenchmarkAccount account = accountRepository.findById(video.getBenchmarkAccountId())
                .orElseThrow(() -> new RuntimeException("关联账号不存在"));

        if (!account.getOwnerId().equals(ownerId)) {
            throw new RuntimeException("无权访问该视频");
        }
        video.setOwnerId(ownerId);
        video = videoRepository.save(video);

        return entityToVO(video);
    }

    @Override
    @Transactional
    @CacheEvict(value = BenchmarkCacheConfig.CACHE_VIDEO_LIST, allEntries = true)
    public BenchmarkVideoVO save(BenchmarkVideoSaveVO saveVO, Long ownerId) {
        // 验证账号权限
        BenchmarkAccount account = accountRepository.findById(saveVO.getBenchmarkAccountId())
                .orElseThrow(() -> new RuntimeException("账号不存在"));

        if (!account.getOwnerId().equals(ownerId)) {
            throw new RuntimeException("无权操作该账号的视频");
        }

        BenchmarkVideo video;

        if (saveVO.getId() != null) {
            // 更新
            video = videoRepository.findById(saveVO.getId())
                    .orElseThrow(() -> new RuntimeException("视频不存在"));
            if (video.getOwnerId() != null && !video.getOwnerId().equals(ownerId) && video.getOwnerId() != 0L) {
                throw new RuntimeException("无权修改该视频");
            }
            if (!video.getBenchmarkAccountId().equals(saveVO.getBenchmarkAccountId())) {
                throw new RuntimeException("不允许跨账号移动对标视频");
            }
        } else {
            // 新增
            video = new BenchmarkVideo();
            video.setOwnerId(ownerId);
        }

        BeanUtils.copyProperties(saveVO, video, "id");
        video.setOwnerId(ownerId);
        video = videoRepository.save(video);

        return entityToVO(video);
    }

    @Override
    @Transactional
    @CacheEvict(value = BenchmarkCacheConfig.CACHE_VIDEO_LIST, allEntries = true)
    public void delete(Long id, Long ownerId) {
        BenchmarkVideo video = videoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("视频不存在"));

        // 验证权限
        BenchmarkAccount account = accountRepository.findById(video.getBenchmarkAccountId())
                .orElseThrow(() -> new RuntimeException("关联账号不存在"));

        if (!account.getOwnerId().equals(ownerId)) {
            throw new RuntimeException("无权删除该视频");
        }

        video.setDeleted(1);
        video.setOwnerId(ownerId);
        videoRepository.save(video);
    }

    @Override
    @Transactional
    public List<BenchmarkVideoVO> collectAccountVideos(CollectAccountVideosVO collectVO, Long ownerId) {
        log.info("采集账号视频: accountId={}, minLikeCount={}",
                collectVO.getBenchmarkAccountId(), collectVO.getMinLikeCount());
        long startTime = System.currentTimeMillis();

        if (!accountVideoScraper.isAvailable()) {
            metrics.recordPlaywrightError("collectVideos", "unavailable");
            throw new RuntimeException("Playwright 不可用，无法采集视频");
        }

        // 验证账号权限
        BenchmarkAccount account = accountRepository.findById(collectVO.getBenchmarkAccountId())
                .orElseThrow(() -> new RuntimeException("账号不存在"));

        if (!account.getOwnerId().equals(ownerId)) {
            throw new RuntimeException("无权操作该账号");
        }

        // 获取可用Cookie
        String cookie = getCookieValue(collectVO.getCookieId(), ownerId);
        if (cookie == null) {
            throw new RuntimeException("没有可用的Cookie，请先添加Cookie");
        }

        List<BenchmarkVideoVO> results = new ArrayList<>();

        try {
            // 使用 AccountVideoScraper 采集视频
            AccountVideoScraper.ScrapeResult scrapeResult =
                    accountVideoScraper.scrapeAccountVideos(account.getAccountUrl(), ownerId);

            if (scrapeResult == null || scrapeResult.getVideos().isEmpty()) {
                log.warn("未采集到任何视频");
                return results;
            }

            int minLikeCount = collectVO.getMinLikeCount() != null ? collectVO.getMinLikeCount() : 1000;
            int maxVideos = collectVO.getMaxVideos() != null ? collectVO.getMaxVideos() : 50;
            int savedCount = 0;

            // 过滤并保存视频
            for (AccountVideoScraper.ScrapedVideo scrapedVideo : scrapeResult.getVideos()) {
                if (savedCount >= maxVideos) {
                    break;
                }

                // 检查点赞数
                if (scrapedVideo.getLikeCount() == null || scrapedVideo.getLikeCount() < minLikeCount) {
                    continue;
                }

                // 检查是否已存在
                if (videoRepository.findByVideoIdAndBenchmarkAccountId(
                        scrapedVideo.getVideoId(), account.getId()).isPresent()) {
                    log.debug("视频已存在: {}", scrapedVideo.getVideoId());
                    continue;
                }

                // 创建新视频记录
                BenchmarkVideo video = new BenchmarkVideo();
                video.setOwnerId(ownerId);
                video.setBenchmarkAccountId(account.getId());
                video.setVideoId(scrapedVideo.getVideoId());
                video.setVideoUrl(scrapedVideo.getVideoUrl());
                video.setTitle(scrapedVideo.getTitle());
                video.setCoverUrl(scrapedVideo.getCoverUrl());
                video.setLikeCount(scrapedVideo.getLikeCount().intValue());
                video.setViewCount(scrapedVideo.getViewCount());
                if (scrapedVideo.getCommentCount() != null) {
                    video.setCommentCount(scrapedVideo.getCommentCount().intValue());
                }
                if (scrapedVideo.getShareCount() != null) {
                    video.setShareCount(scrapedVideo.getShareCount().intValue());
                }
                if (scrapedVideo.getFavoriteCount() != null) {
                    video.setFavoriteCount(scrapedVideo.getFavoriteCount().intValue());
                }
                video.setIsQualified(true);
                video.setAnalysisStatus("pending");

                video = videoRepository.save(video);
                results.add(entityToVO(video));
                savedCount++;

                log.debug("保存视频: {} (点赞: {})", video.getTitle(), video.getLikeCount());
            }

            // 更新账号的最后采集时间
            account.setLastCollectTime(LocalDateTime.now());
            account.setVideoCount(videoRepository.countByBenchmarkAccountId(account.getId()));
            accountRepository.save(account);

            metrics.recordVideoCollected(account.getId(), savedCount);
            metrics.recordVideoCollectDuration(account.getId(), System.currentTimeMillis() - startTime);
            log.info("采集完成，保存 {} 个符合条件的视频", savedCount);

        } catch (Exception e) {
            metrics.recordPlaywrightError("collectVideos", e.getClass().getSimpleName());
            log.error("采集视频失败: {}", e.getMessage(), e);
            throw new RuntimeException("采集视频失败: " + e.getMessage());
        }

        return results;
    }

    @Override
    @Transactional
    @CacheEvict(value = BenchmarkCacheConfig.CACHE_VIDEO_LIST, allEntries = true)
    public void updateAnalysisStatus(Long videoId, String status) {
        videoRepository.findById(videoId).ifPresent(video -> {
            video.setAnalysisStatus(status);
            videoRepository.save(video);
        });
    }

    /**
     * 获取Cookie值
     */
    private String getCookieValue(Long cookieId, Long ownerId) {
        if (cookieId != null) {
            DouyinCookieVO cookie = cookieService.getById(cookieId, ownerId);
            return cookie.getCookieValue();
        } else {
            return cookieService.getAvailableCookie(ownerId, "douyin");
        }
    }

    private BenchmarkVideoVO entityToVO(BenchmarkVideo entity) {
        BenchmarkVideoVO vo = new BenchmarkVideoVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
