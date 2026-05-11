package cn.gaifan.douyinOperations.module.douyin.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.douyin.entity.DouyinAccount;
import cn.gaifan.douyinOperations.module.douyin.entity.DouyinVideo;
import cn.gaifan.douyinOperations.module.douyin.repository.DouyinAccountRepository;
import cn.gaifan.douyinOperations.module.douyin.repository.DouyinVideoRepository;
import cn.gaifan.douyinOperations.module.douyin.service.DouyinVideoService;
import cn.gaifan.douyinOperations.module.douyin.vo.*;
import cn.gaifan.douyinOperations.module.douyinapi.client.DouyinApiClient;
import cn.gaifan.douyinOperations.module.douyinapi.service.OAuthTokenService;
import com.github.benmanes.caffeine.cache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import jakarta.persistence.criteria.Predicate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 抖音视频服务实现
 */
@Service
public class DouyinVideoServiceImpl implements DouyinVideoService {

    private static final Logger log = LoggerFactory.getLogger(DouyinVideoServiceImpl.class);

    private static final Set<String> SORTABLE_FIELDS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("id", "accountId", "createTime", "updateTime", "publishTime", "viewCount", "likeCount")));

    @Resource
    private DouyinVideoRepository douyinVideoRepository;

    @Resource(name = "douyinApiClient")
    private DouyinApiClient douyinApiClient;

    @Resource
    private DouyinAccountRepository douyinAccountRepository;

    @Resource
    private OAuthTokenService oauthTokenService;

    @Resource(name = "accountStatisticsCache")
    private Cache<Long, Object> accountStatisticsCache;

    @Override
    public PageResultVO<DouyinVideoVO> search(DouyinVideoSearchVO vo) {
        vo.validateParams();
        String sortName = SORTABLE_FIELDS.contains(vo.getSortName()) ? vo.getSortName() : "id";
        Pageable pageable = PageRequest.of(vo.getPage(), vo.getRows(),
                Sort.by("desc".equalsIgnoreCase(vo.getSortOrder()) ? Sort.Direction.DESC : Sort.Direction.ASC, sortName));

        Specification<DouyinVideo> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("deleted"), 0));

            if (vo.getAccountId() != null && vo.getAccountId() > 0) {
                predicates.add(cb.equal(root.get("accountId"), vo.getAccountId()));
            } else if (vo.getAccountIds() != null && !vo.getAccountIds().isEmpty()) {
                predicates.add(root.get("accountId").in(vo.getAccountIds()));
            }
            if (vo.getTitle() != null && !vo.getTitle().trim().isEmpty()) {
                predicates.add(cb.like(root.get("title"), "%" + vo.getTitle().trim() + "%"));
            }
            if (vo.getVideoType() != null && !vo.getVideoType().trim().isEmpty()) {
                predicates.add(cb.equal(root.get("videoType"), vo.getVideoType().trim()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<DouyinVideo> page = douyinVideoRepository.findAll(spec, pageable);
        List<DouyinVideoVO> list = page.getContent().stream().map(this::toVideoVO).collect(Collectors.toList());
        return PageResultVO.of(page.getTotalElements(), list, vo.getPage(), vo.getRows());
    }

    @Override
    public DouyinVideoVO getVideo(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "视频 ID 无效");
        }
        DouyinVideo video = douyinVideoRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_FAIL, "视频不存在"));
        return toVideoVO(video);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long saveVideo(DouyinVideoSaveVO vo) {
        DouyinVideo video;
        if (vo.getId() != null && vo.getId() > 0) {
            video = douyinVideoRepository.findByIdAndDeleted(vo.getId(), 0)
                    .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_FAIL, "视频不存在"));
        } else {
            if (douyinVideoRepository.existsByVideoIdAndDeleted(vo.getVideoId(), 0)) {
                // P2-8: 避免泄露敏感信息，使用通用错误消息
                log.warn("视频 ID 已存在: videoId={}, accountId={}", vo.getVideoId(), vo.getAccountId());
                throw new BusinessException(ErrorCode.VALIDATION_FAIL, "保存失败，请检查输入");
            }
            video = new DouyinVideo();
            video.setAccountId(vo.getAccountId());
            video.setVideoId(vo.getVideoId());
        }
        video.setTitle(vo.getTitle());
        if (vo.getDescription() != null) video.setDescription(vo.getDescription());
        if (vo.getViewCount() != null) video.setViewCount(vo.getViewCount());
        if (vo.getLikeCount() != null) video.setLikeCount(vo.getLikeCount());
        if (vo.getShareCount() != null) video.setShareCount(vo.getShareCount());
        if (vo.getCommentCount() != null) video.setCommentCount(vo.getCommentCount());
        if (vo.getDownloadCount() != null) video.setDownloadCount(vo.getDownloadCount());
        if (vo.getVideoType() != null) video.setVideoType(vo.getVideoType());
        video = douyinVideoRepository.save(video);
        return video.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "accountStatistics", key = "#accountId")
    public void syncVideos(Long accountId) {
        if (accountId == null || accountId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "账号 ID 无效");
        }
        DouyinAccount account = douyinAccountRepository.findByIdAndDeleted(accountId, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_FAIL, "账号不存在"));

        String accessToken = oauthTokenService.getValidAccessToken(account.getOwnerId(), "douyin");
        if (accessToken == null || accessToken.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "账号未授权或 Token 已失效，请先完成 OAuth 授权");
        }

        // 调用抖音 API 获取视频列表（首页，最多 20 条）
        DouyinApiClient.VideoListResponse resp = douyinApiClient.getVideoList(
                account.getAccountId(), accessToken, 0, 20);
        if (resp == null || resp.list() == null || resp.list().isEmpty()) {
            log.info("账号 {} 无视频数据可同步", accountId);
            return;
        }

        // P0-1 修复：批量查询避免 N+1 问题
        List<String> videoIds = resp.list().stream()
            .map(DouyinApiClient.VideoItem::itemId)
            .collect(java.util.stream.Collectors.toList());

        Map<String, DouyinVideo> existingVideos = douyinVideoRepository
            .findByVideoIdInAndDeleted(videoIds, 0)
            .stream()
            .collect(java.util.stream.Collectors.toMap(DouyinVideo::getVideoId, v -> v));

        List<DouyinVideo> toUpdate = new ArrayList<>();
        List<DouyinVideo> toInsert = new ArrayList<>();

        for (DouyinApiClient.VideoItem item : resp.list()) {
            if (existingVideos.containsKey(item.itemId())) {
                DouyinVideo v = existingVideos.get(item.itemId());
                v.setViewCount(item.playCount());
                v.setLikeCount(item.likeCount());
                v.setCommentCount(item.commentCount());
                v.setShareCount(item.shareCount());
                toUpdate.add(v);
            } else {
                DouyinVideo video = new DouyinVideo();
                video.setAccountId(accountId);
                video.setVideoId(item.itemId());
                video.setTitle(item.title());
                video.setViewCount(item.playCount());
                video.setLikeCount(item.likeCount());
                video.setCommentCount(item.commentCount());
                video.setShareCount(item.shareCount());
                if (item.createTime() > 0) {
                    video.setPublishTime(new java.sql.Timestamp(item.createTime() * 1000));
                }
                toInsert.add(video);
            }
        }

        if (!toUpdate.isEmpty()) douyinVideoRepository.saveAll(toUpdate);
        if (!toInsert.isEmpty()) douyinVideoRepository.saveAll(toInsert);

        // P2-5: 清除 L1 缓存
        accountStatisticsCache.invalidate(accountId);

        int synced = toUpdate.size() + toInsert.size();
        log.info("账号 {} 视频同步完成，共处理 {} 条（更新 {}，新增 {}）",
                accountId, synced, toUpdate.size(), toInsert.size());
    }

    private DouyinVideoVO toVideoVO(DouyinVideo video) {
        DouyinVideoVO vo = new DouyinVideoVO();
        vo.setId(video.getId());
        vo.setAccountId(video.getAccountId());
        vo.setVideoId(video.getVideoId());
        vo.setTitle(video.getTitle());
        vo.setDescription(video.getDescription());
        vo.setViewCount(video.getViewCount());
        vo.setLikeCount(video.getLikeCount());
        vo.setShareCount(video.getShareCount());
        vo.setCommentCount(video.getCommentCount());
        vo.setDownloadCount(video.getDownloadCount());
        vo.setVideoType(video.getVideoType());
        vo.setPublishTime(video.getPublishTime());
        vo.setCreateTime(video.getCreateTime());
        vo.setUpdateTime(video.getUpdateTime());
        return vo;
    }
}
