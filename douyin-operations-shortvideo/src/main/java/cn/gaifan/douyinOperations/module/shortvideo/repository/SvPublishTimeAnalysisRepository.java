package cn.gaifan.douyinOperations.module.shortvideo.repository;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvPublishTimeAnalysis;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 发布时间分析 Repository
 */
public interface SvPublishTimeAnalysisRepository extends JpaRepository<SvPublishTimeAnalysis, Long> {

    List<SvPublishTimeAnalysis> findByAccountIdAndRecommendedOrderByAvgViewCountDesc(Long accountId, Boolean recommended);

    List<SvPublishTimeAnalysis> findByAccountIdOrderByAvgViewCountDesc(Long accountId, Pageable pageable);
}
