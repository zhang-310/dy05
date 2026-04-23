package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvComment;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvVideo;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvCommentRepository;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvVideoRepository;
import cn.gaifan.douyinOperations.module.shortvideo.vo.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SvCommentServiceImpl {

    @Resource
    private SvCommentRepository svCommentRepository;
    @Resource
    private SvVideoRepository svVideoRepository;

    /**
     * 分页搜索评论，需 visibleOwnerIds 校验（null 表示管理员不限制）
     */
    public PageResultVO<SvCommentVO> search(SvCommentSearchVO vo, List<Long> visibleOwnerIds) {
        vo.validateParams();
        Pageable pageable = PageRequest.of(vo.getPage(), vo.getRows(),
                Sort.by(Sort.Direction.DESC, "createTime"));

        Specification<SvComment> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("deleted"), 0));
            if (vo.getVideoId() != null) {
                predicates.add(cb.equal(root.get("videoId"), vo.getVideoId()));
                if (visibleOwnerIds != null && !visibleOwnerIds.isEmpty()) {
                    SvVideo video = svVideoRepository.findByIdAndDeleted(vo.getVideoId(), 0).orElse(null);
                    if (video == null || !visibleOwnerIds.contains(video.getOwnerId()))
                        predicates.add(cb.isNull(root.get("id")));
                }
            } else if (visibleOwnerIds != null && !visibleOwnerIds.isEmpty()) {
                Subquery<Long> sq = query.subquery(Long.class);
                var vRoot = sq.from(SvVideo.class);
                sq.select(vRoot.get("id"))
                        .where(cb.and(
                                vRoot.get("ownerId").in(visibleOwnerIds),
                                cb.equal(vRoot.get("deleted"), 0)));
                predicates.add(root.get("videoId").in(sq));
            }
            if (vo.getSentiment() != null && !vo.getSentiment().isBlank()) {
                predicates.add(cb.equal(root.get("sentiment"), vo.getSentiment().trim()));
            }
            if (vo.getKeyword() != null && !vo.getKeyword().isBlank()) {
                predicates.add(cb.like(root.get("content"), "%" + vo.getKeyword().trim() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<SvComment> page = svCommentRepository.findAll(spec, pageable);
        return PageResultVO.of(page.getTotalElements(),
                page.getContent().stream().map(this::toVO).collect(Collectors.toList()),
                vo.getPage(), vo.getRows());
    }

    /** 获取评论详情，需 visibleOwnerIds 校验（null 表示不限制） */
    public SvCommentVO getById(Long id, List<Long> visibleOwnerIds) {
        SvComment c = svCommentRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "评论不存在"));
        if (visibleOwnerIds != null && !visibleOwnerIds.isEmpty()) {
            SvVideo video = svVideoRepository.findByIdAndDeleted(c.getVideoId(), 0).orElse(null);
            if (video == null || !visibleOwnerIds.contains(video.getOwnerId()))
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权限查看该评论");
        }
        return toVO(c);
    }

    @Transactional(rollbackFor = Exception.class)
    public long save(SvCommentSaveVO vo, List<Long> visibleOwnerIds) {
        Long videoId = vo.getVideoId();
        if (videoId == null && vo.getId() != null && vo.getId() > 0) {
            SvComment existing = svCommentRepository.findByIdAndDeleted(vo.getId(), 0).orElse(null);
            videoId = existing != null ? existing.getVideoId() : null;
        }
        if (visibleOwnerIds != null && !visibleOwnerIds.isEmpty() && videoId != null) {
            SvVideo video = svVideoRepository.findByIdAndDeleted(videoId, 0).orElse(null);
            if (video == null || !visibleOwnerIds.contains(video.getOwnerId()))
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权限操作该视频下的评论");
        }
        SvComment entity;
        if (vo.getId() != null && vo.getId() > 0) {
            entity = svCommentRepository.findByIdAndDeleted(vo.getId(), 0)
                    .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "评论不存在"));
        } else {
            entity = new SvComment();
            entity.setVideoId(vo.getVideoId());
        }
        entity.setContent(vo.getContent());
        if (vo.getDouyinCommentId() != null) entity.setDouyinCommentId(vo.getDouyinCommentId());
        if (vo.getAuthorName() != null) entity.setAuthorName(vo.getAuthorName());
        if (vo.getAuthorAvatar() != null) entity.setAuthorAvatar(vo.getAuthorAvatar());
        if (vo.getSentiment() != null) entity.setSentiment(vo.getSentiment());
        if (vo.getSentimentScore() != null) entity.setSentimentScore(vo.getSentimentScore());
        if (vo.getCommentTime() != null) entity.setCommentTime(vo.getCommentTime());
        return svCommentRepository.save(entity).getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, List<Long> visibleOwnerIds) {
        SvComment entity = svCommentRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "评论不存在"));
        if (visibleOwnerIds != null && !visibleOwnerIds.isEmpty()) {
            SvVideo video = svVideoRepository.findByIdAndDeleted(entity.getVideoId(), 0).orElse(null);
            if (video == null || !visibleOwnerIds.contains(video.getOwnerId()))
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权限删除该评论");
        }
        entity.setDeleted(1);
        svCommentRepository.save(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void incrementLikeCount(Long id, List<Long> visibleOwnerIds) {
        SvComment entity = svCommentRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "评论不存在"));
        if (visibleOwnerIds != null && !visibleOwnerIds.isEmpty()) {
            SvVideo video = svVideoRepository.findByIdAndDeleted(entity.getVideoId(), 0).orElse(null);
            if (video == null || !visibleOwnerIds.contains(video.getOwnerId()))
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权限操作该评论");
        }
        svCommentRepository.incrementLikeCount(id);
    }

    private SvCommentVO toVO(SvComment e) {
        SvCommentVO vo = new SvCommentVO();
        vo.setId(e.getId());
        vo.setVideoId(e.getVideoId());
        vo.setDouyinCommentId(e.getDouyinCommentId());
        vo.setContent(e.getContent());
        vo.setAuthorName(e.getAuthorName());
        vo.setAuthorAvatar(e.getAuthorAvatar());
        vo.setLikeCount(e.getLikeCount());
        vo.setReplyCount(e.getReplyCount());
        vo.setSentiment(e.getSentiment());
        vo.setSentimentScore(e.getSentimentScore());
        vo.setCommentTime(e.getCommentTime());
        vo.setCreateTime(e.getCreateTime());
        return vo;
    }
}
