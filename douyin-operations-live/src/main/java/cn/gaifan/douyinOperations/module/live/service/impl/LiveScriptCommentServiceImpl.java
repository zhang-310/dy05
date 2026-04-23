package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.live.entity.LiveScriptComment;
import cn.gaifan.douyinOperations.module.live.repository.LiveScriptCommentRepository;
import cn.gaifan.douyinOperations.module.live.service.LiveScriptCommentService;
import cn.gaifan.douyinOperations.module.live.vo.LiveScriptCommentSaveVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveScriptCommentVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 话术行内评论服务实现
 */
@Service
public class LiveScriptCommentServiceImpl implements LiveScriptCommentService {

    @Resource
    private LiveScriptCommentRepository commentRepository;

    @Override
    public List<LiveScriptCommentVO> getByScript(Long scriptId) {
        List<LiveScriptComment> comments = commentRepository.findByScriptIdOrderByCreateTimeAsc(scriptId);
        return comments.stream().map(this::toVO).toList();
    }

    @Override
    public List<LiveScriptCommentVO> getBySession(Long sessionId) {
        List<LiveScriptComment> comments = commentRepository.findBySessionIdOrderByCreateTimeAsc(sessionId);
        return comments.stream().map(this::toVO).toList();
    }

    @Override
    @Transactional
    public LiveScriptCommentVO addComment(LiveScriptCommentSaveVO vo, Long userId) {
        LiveScriptComment comment = new LiveScriptComment();
        comment.setScriptId(vo.getScriptId());
        comment.setSessionId(vo.getSessionId());
        comment.setUserId(userId);
        comment.setContent(vo.getContent());
        comment.setParentId(vo.getParentId());
        comment.setResolved(0);
        comment.setDeleted(0);
        commentRepository.save(comment);
        return toVO(comment);
    }

    @Override
    @Transactional
    public void resolveComment(Long commentId, Long userId) {
        LiveScriptComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LIVE_COMMENT_NOT_FOUND, "评论不存在"));
        comment.setResolved(1);
        comment.setResolvedBy(userId);
        comment.setResolvedAt(LocalDateTime.now());
        commentRepository.save(comment);
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        LiveScriptComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LIVE_COMMENT_NOT_FOUND, "评论不存在"));
        if (!comment.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.LIVE_COMMENT_FORBIDDEN, "仅评论作者可删除");
        }
        comment.setDeleted(1);
        commentRepository.save(comment);
    }

    @Override
    public long countUnresolved(Long sessionId) {
        return commentRepository.countBySessionIdAndResolvedAndDeleted(sessionId, 0, 0);
    }

    @Override
    public Map<Long, Long> countUnresolvedByScript(Long sessionId) {
        List<LiveScriptComment> comments = commentRepository.findBySessionIdOrderByCreateTimeAsc(sessionId);
        return comments.stream()
                .filter(c -> c.getResolved() == 0)
                .collect(Collectors.groupingBy(LiveScriptComment::getScriptId, Collectors.counting()));
    }

    private LiveScriptCommentVO toVO(LiveScriptComment entity) {
        LiveScriptCommentVO vo = new LiveScriptCommentVO();
        vo.setId(entity.getId());
        vo.setScriptId(entity.getScriptId());
        vo.setSessionId(entity.getSessionId());
        vo.setUserId(entity.getUserId());
        vo.setUserName(entity.getUserName());
        vo.setContent(entity.getContent());
        vo.setResolved(entity.getResolved());
        vo.setResolvedBy(entity.getResolvedBy());
        vo.setResolvedAt(entity.getResolvedAt());
        vo.setParentId(entity.getParentId());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }
}
