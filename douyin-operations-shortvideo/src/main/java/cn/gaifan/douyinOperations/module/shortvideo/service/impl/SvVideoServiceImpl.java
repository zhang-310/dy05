package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvVideo;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvVideoData;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvVideoDataRepository;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvVideoRepository;
import cn.gaifan.douyinOperations.module.shortvideo.vo.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import jakarta.persistence.criteria.Predicate;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SvVideoServiceImpl implements cn.gaifan.douyinOperations.module.shortvideo.service.SvVideoService {

    private static final Set<String> SORTABLE = Set.of("id", "ownerId", "accountId", "viewCount", "likeCount", "publishTime", "createTime");

    @Resource
    private SvVideoRepository svVideoRepository;

    @Resource
    private SvVideoDataRepository svVideoDataRepository;

    public PageResultVO<SvVideoVO> search(SvVideoSearchVO vo) {
        vo.validateParams();
        String sortName = SORTABLE.contains(vo.getSortName()) ? vo.getSortName() : "publishTime";
        Pageable pageable = PageRequest.of(vo.getPage(), vo.getRows(),
                Sort.by("desc".equalsIgnoreCase(vo.getSortOrder()) ? Sort.Direction.DESC : Sort.Direction.ASC, sortName));

        Specification<SvVideo> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("deleted"), 0));
            if (vo.getOwnerId() != null && vo.getOwnerId() > 0) {
                predicates.add(cb.equal(root.get("ownerId"), vo.getOwnerId()));
            } else if (vo.getOwnerIds() != null && !vo.getOwnerIds().isEmpty()) {
                predicates.add(root.get("ownerId").in(vo.getOwnerIds()));
            }
            if (vo.getAccountId() != null) predicates.add(cb.equal(root.get("accountId"), vo.getAccountId()));
            if (vo.getCategoryId() != null) predicates.add(cb.equal(root.get("categoryId"), vo.getCategoryId()));
            if (vo.getIsViral() != null) predicates.add(cb.equal(root.get("isViral"), vo.getIsViral()));
            if (vo.getKeyword() != null && !vo.getKeyword().isBlank()) {
                String kw = "%" + vo.getKeyword().trim() + "%";
                predicates.add(cb.or(cb.like(root.get("title"), kw), cb.like(root.get("description"), kw)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<SvVideo> page = svVideoRepository.findAll(spec, pageable);
        return PageResultVO.of(page.getTotalElements(),
                page.getContent().stream().map(this::toVO).collect(Collectors.toList()),
                vo.getPage(), vo.getRows());
    }

    public SvVideoVO getById(Long id, List<Long> visibleOwnerIds) {
        if (id == null || id <= 0) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "视频 ID 无效");
        SvVideo entity = svVideoRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.VIDEO_NOT_FOUND, "视频不存在"));
        if (visibleOwnerIds != null && !visibleOwnerIds.isEmpty() && !visibleOwnerIds.contains(entity.getOwnerId()))
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限查看该视频");
        return toVO(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public long save(SvVideoSaveVO vo, List<Long> visibleOwnerIds) {
        SvVideo entity;
        if (vo.getId() != null && vo.getId() > 0) {
            entity = svVideoRepository.findByIdAndDeleted(vo.getId(), 0)
                    .orElseThrow(() -> new BusinessException(ErrorCode.VIDEO_NOT_FOUND, "视频不存在"));
            if (visibleOwnerIds != null && !visibleOwnerIds.isEmpty() && !visibleOwnerIds.contains(entity.getOwnerId()))
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权限修改该视频");
        } else {
            entity = new SvVideo();
            entity.setAccountId(vo.getAccountId());
            Long ownerId = vo.getOwnerId();
            if (ownerId == null) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "ownerId 不能为空");
            if (visibleOwnerIds != null && !visibleOwnerIds.isEmpty() && !visibleOwnerIds.contains(ownerId))
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权限为该用户创建视频");
            entity.setOwnerId(ownerId);
        }
        if (vo.getDouyinVideoId() != null) entity.setDouyinVideoId(vo.getDouyinVideoId());
        if (vo.getTitle() != null) entity.setTitle(vo.getTitle());
        if (vo.getDescription() != null) entity.setDescription(vo.getDescription());
        if (vo.getCoverUrl() != null) entity.setCoverUrl(vo.getCoverUrl());
        if (vo.getVideoUrl() != null) entity.setVideoUrl(vo.getVideoUrl());
        if (vo.getDuration() != null) entity.setDuration(vo.getDuration());
        if (vo.getTags() != null) entity.setTags(vo.getTags());
        if (vo.getCategoryId() != null) entity.setCategoryId(vo.getCategoryId());
        if (vo.getPublishTime() != null) entity.setPublishTime(vo.getPublishTime());
        if (vo.getIsViral() != null) entity.setIsViral(vo.getIsViral());
        if (vo.getAiCallLogId() != null) entity.setAiCallLogId(vo.getAiCallLogId());
        if (vo.getAiGenerated() != null) entity.setAiGenerated(vo.getAiGenerated());
        if (vo.getPlanId() != null) entity.setPlanId(vo.getPlanId());
        return svVideoRepository.save(entity).getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, List<Long> visibleOwnerIds) {
        SvVideo entity = svVideoRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.VIDEO_NOT_FOUND, "视频不存在"));
        if (visibleOwnerIds != null && !visibleOwnerIds.isEmpty() && !visibleOwnerIds.contains(entity.getOwnerId()))
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限删除该视频");
        entity.setDeleted(1);
        svVideoRepository.save(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void incrementViewCount(Long id, List<Long> visibleOwnerIds) {
        SvVideo entity = svVideoRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.VIDEO_NOT_FOUND, "视频不存在"));
        if (visibleOwnerIds != null && !visibleOwnerIds.isEmpty() && !visibleOwnerIds.contains(entity.getOwnerId()))
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限操作该视频");
        svVideoRepository.incrementViewCount(id);
    }

    @Override
    public List<Map<String, Object>> getDataTrend(Long videoId, List<Long> visibleOwnerIds) {
        if (videoId == null) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "videoId 不能为空");
        SvVideo video = svVideoRepository.findByIdAndDeleted(videoId, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.VIDEO_NOT_FOUND, "视频不存在"));
        if (visibleOwnerIds != null && !visibleOwnerIds.isEmpty() && !visibleOwnerIds.contains(video.getOwnerId()))
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限查看该视频数据");
        List<SvVideoData> list = svVideoDataRepository.findByVideoIdOrderBySnapshotDateDesc(videoId);
        Collections.reverse(list);  // 折线图按日期升序
        List<Map<String, Object>> result = new ArrayList<>();
        for (SvVideoData d : list) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("date", d.getSnapshotDate() != null ? d.getSnapshotDate().toString() : null);
            m.put("viewCount", d.getViewCount());
            m.put("likeCount", d.getLikeCount());
            m.put("commentCount", d.getCommentCount());
            m.put("shareCount", d.getShareCount());
            m.put("favoriteCount", d.getFavoriteCount());
            m.put("viewDelta", d.getViewDelta());
            m.put("likeDelta", d.getLikeDelta());
            result.add(m);
        }
        return result;
    }

    private SvVideoVO toVO(SvVideo e) {
        SvVideoVO vo = new SvVideoVO();
        vo.setId(e.getId());
        vo.setOwnerId(e.getOwnerId());
        vo.setAccountId(e.getAccountId());
        vo.setDouyinVideoId(e.getDouyinVideoId());
        vo.setTitle(e.getTitle());
        vo.setDescription(e.getDescription());
        vo.setCoverUrl(e.getCoverUrl());
        vo.setVideoUrl(e.getVideoUrl());
        vo.setDuration(e.getDuration());
        vo.setTags(e.getTags());
        vo.setCategoryId(e.getCategoryId());
        vo.setPublishTime(e.getPublishTime());
        vo.setViewCount(e.getViewCount());
        vo.setLikeCount(e.getLikeCount());
        vo.setCommentCount(e.getCommentCount());
        vo.setShareCount(e.getShareCount());
        vo.setFavoriteCount(e.getFavoriteCount());
        vo.setIsViral(e.getIsViral());
        vo.setAiCallLogId(e.getAiCallLogId());
        vo.setAiGenerated(e.getAiGenerated());
        vo.setPlanId(e.getPlanId());
        vo.setSyncStatus(e.getSyncStatus());
        vo.setCreateTime(e.getCreateTime());
        vo.setUpdateTime(e.getUpdateTime());
        return vo;
    }
}
