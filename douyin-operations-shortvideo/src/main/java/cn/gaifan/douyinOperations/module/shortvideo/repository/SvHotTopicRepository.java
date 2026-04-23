package cn.gaifan.douyinOperations.module.shortvideo.repository;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvHotTopic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface SvHotTopicRepository extends JpaRepository<SvHotTopic, Long>, JpaSpecificationExecutor<SvHotTopic> {

    /** 统计指定时间之后创建的热点话题数量 */
    long countByCreateTimeAfter(java.sql.Timestamp createTime);

    /** 按创建时间分组统计来源分布 */
    @org.springframework.data.jpa.repository.Query("SELECT h.source, COUNT(h) FROM SvHotTopic h WHERE h.createTime >= :since GROUP BY h.source")
    java.util.List<Object[]> countGroupedBySourceSince(@org.springframework.data.repository.query.Param("since") java.sql.Timestamp since);

    /** 最近 8 条热点话题 */
    java.util.List<SvHotTopic> findTop8ByOrderByCreateTimeDesc();

    /** 按抖音热点 ID 查找 */
    java.util.Optional<SvHotTopic> findByDouyinHotId(String douyinHotId);
}
