package cn.gaifan.douyinOperations.module.live.service;

import cn.gaifan.douyinOperations.module.live.vo.LiveScriptCommentSaveVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveScriptCommentVO;

import java.util.List;
import java.util.Map;

/**
 * 话术行内评论服务接口
 */
public interface LiveScriptCommentService {

    /**
     * 获取指定话术的所有评论
     */
    List<LiveScriptCommentVO> getByScript(Long scriptId);

    /**
     * 获取指定场次的所有评论
     */
    List<LiveScriptCommentVO> getBySession(Long sessionId);

    /**
     * 添加评论
     */
    LiveScriptCommentVO addComment(LiveScriptCommentSaveVO vo, Long userId);

    /**
     * 标记评论为已解决
     */
    void resolveComment(Long commentId, Long userId);

    /**
     * 软删除评论（仅评论作者可删除）
     */
    void deleteComment(Long commentId, Long userId);

    /**
     * 统计场次下未解决评论数量
     */
    long countUnresolved(Long sessionId);

    /**
     * 按话术统计场次下的未解决评论数量
     *
     * @return map of scriptId → unresolved count
     */
    Map<Long, Long> countUnresolvedByScript(Long sessionId);
}
