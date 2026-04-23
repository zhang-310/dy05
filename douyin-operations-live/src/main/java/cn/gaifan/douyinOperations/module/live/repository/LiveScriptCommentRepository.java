package cn.gaifan.douyinOperations.module.live.repository;

import cn.gaifan.douyinOperations.module.live.entity.LiveScriptComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 话术行内评论 Repository
 */
public interface LiveScriptCommentRepository extends JpaRepository<LiveScriptComment, Long> {

    /**
     * 根据话术 ID 查询评论（按创建时间升序，用于评论线程）
     */
    List<LiveScriptComment> findByScriptIdOrderByCreateTimeAsc(Long scriptId);

    /**
     * 根据话术 ID 和删除状态查询评论（按创建时间升序）
     */
    List<LiveScriptComment> findByScriptIdAndDeletedOrderByCreateTimeAsc(Long scriptId, int deleted);

    /**
     * 根据直播场次 ID 查询所有评论（按创建时间升序）
     */
    List<LiveScriptComment> findBySessionIdOrderByCreateTimeAsc(Long sessionId);

    /**
     * 统计场次下指定解决状态的评论数量
     */
    long countBySessionIdAndResolvedAndDeleted(Long sessionId, Integer resolved, Integer deleted);

    /**
     * 统计话术下指定解决状态的评论数量
     */
    long countByScriptIdAndResolvedAndDeleted(Long scriptId, Integer resolved, Integer deleted);
}
