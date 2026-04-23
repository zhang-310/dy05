package cn.gaifan.douyinOperations.module.shortvideo.repository;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SvCommentRepository extends JpaRepository<SvComment, Long>, JpaSpecificationExecutor<SvComment> {

    Optional<SvComment> findByIdAndDeleted(Long id, Integer deleted);

    @Modifying
    @Query("UPDATE SvComment c SET c.likeCount = c.likeCount + 1 WHERE c.id = :id")
    void incrementLikeCount(@Param("id") Long id);

    /** 按视频 ID + 视频来源 + deleted 分页查询（评论提取去重用） */
    java.util.List<SvComment> findByVideoIdAndVideoSourceAndDeleted(Long videoId, String videoSource, Integer deleted, org.springframework.data.domain.Pageable pageable);
}
