package cn.gaifan.douyinOperations.module.agent.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.agent.entity.Agent;
import cn.gaifan.douyinOperations.module.agent.entity.AgentReview;
import cn.gaifan.douyinOperations.module.agent.repository.AgentRepository;
import cn.gaifan.douyinOperations.module.agent.repository.AgentReviewRepository;
import cn.gaifan.douyinOperations.module.agent.service.AgentReviewService;
import cn.gaifan.douyinOperations.module.agent.vo.AgentReviewSaveVO;
import cn.gaifan.douyinOperations.module.agent.vo.AgentReviewVO;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import jakarta.persistence.criteria.Predicate;
import java.sql.Timestamp;
import java.util.*;

/**
 * 智能体评论服务实现
 */
@Service
public class AgentReviewServiceImpl implements AgentReviewService {

    @Resource
    private AgentReviewRepository reviewRepository;

    @Resource
    private AgentRepository agentRepository;

    @Override
    public Map<String, Object> getRatingStats(Long agentId) {
        // 验证智能体存在
        if (!agentRepository.findByIdAndDeleted(agentId, 0).isPresent()) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "智能体不存在");
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("agentId", agentId);

        // 平均分
        Double avg = reviewRepository.averageRatingByAgentId(agentId);
        stats.put("averageRating", avg != null ? Math.round(avg * 10) / 10.0 : 0.0);

        // 评分数量
        long count = reviewRepository.countByAgentIdAndDeleted(agentId, 0);
        stats.put("reviewCount", count);

        // 星级分布
        Object[] distribution = reviewRepository.ratingDistributionByAgentId(agentId);
        int[] stars = new int[5]; // index 0=1星 ... index 4=5星
        for (Object row : distribution) {
            if (row instanceof Object[] arr && arr.length == 2) {
                Integer star = (Integer) arr[0];
                Long cnt = (Long) arr[1];
                if (star != null && star >= 1 && star <= 5) {
                    stars[star - 1] = cnt.intValue();
                }
            }
        }
        Map<String, Integer> distMap = new LinkedHashMap<>();
        for (int i = 5; i >= 1; i--) {
            distMap.put(i + "星", stars[i - 1]);
        }
        stats.put("distribution", distMap);

        return stats;
    }

    @Override
    public PageResultVO<AgentReviewVO> listReviews(Long agentId, int page, int rows) {
        // 限制分页参数
        if (page < 0) page = 0;
        if (rows < 1) rows = 10;
        if (rows > 50) rows = 50;

        Pageable pageable = PageRequest.of(page, rows, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<AgentReview> p = reviewRepository.findByAgentIdAndStatusAndDeletedOrderByCreateTimeDesc(
                agentId, 1, 0, pageable);

        return PageResultVO.of(p.getTotalElements(),
                p.getContent().stream().map(this::reviewToVO).collect(java.util.stream.Collectors.toList()),
                page + 1, rows);
    }

    @Override
    @Transactional
    public long submitReview(Long userId, AgentReviewSaveVO vo) {
        // 验证智能体存在
        if (!agentRepository.findByIdAndDeleted(vo.getAgentId(), 0).isPresent()) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "智能体不存在");
        }

        // 幂等：已有则更新
        AgentReview existing = reviewRepository.findByAgentIdAndUserIdAndDeleted(vo.getAgentId(), userId, 0);
        if (existing != null) {
            existing.setRating(vo.getRating());
            existing.setContent(vo.getContent());
            reviewRepository.save(existing);
            updateAgentRatingStats(vo.getAgentId());
            return existing.getId();
        }

        // 新增
        AgentReview review = new AgentReview();
        review.setAgentId(vo.getAgentId());
        review.setUserId(userId);
        review.setRating(vo.getRating());
        review.setContent(vo.getContent());
        review.setStatus(1);
        review.setDeleted(0);
        AgentReview saved = reviewRepository.save(review);

        // 更新智能体评分统计
        updateAgentRatingStats(vo.getAgentId());

        return saved.getId();
    }

    @Override
    public AgentReviewVO getUserReview(Long agentId, Long userId) {
        AgentReview r = reviewRepository.findByAgentIdAndUserIdAndDeleted(agentId, userId, 0);
        return r != null ? reviewToVO(r) : null;
    }

    /**
     * 更新智能体的 rating_count / rating_sum 聚合字段
     */
    private void updateAgentRatingStats(Long agentId) {
        Double avg = reviewRepository.averageRatingByAgentId(agentId);
        long count = reviewRepository.countByAgentIdAndDeleted(agentId, 0);

        agentRepository.findByIdAndDeleted(agentId, 0).ifPresent(agent -> {
            agent.setRatingCount((int) count);
            agent.setRatingSum(avg != null ? (int) Math.round(avg * count) : 0);
            agentRepository.save(agent);
        });
    }

    private AgentReviewVO reviewToVO(AgentReview r) {
        AgentReviewVO vo = new AgentReviewVO();
        vo.setId(r.getId());
        vo.setAgentId(r.getAgentId());
        vo.setUserId(r.getUserId());
        vo.setRating(r.getRating());
        vo.setContent(r.getContent());
        vo.setReplyContent(r.getReplyContent());
        vo.setReplyTime(r.getReplyTime());
        vo.setCreatedAt(r.getCreateTime());
        return vo;
    }
}