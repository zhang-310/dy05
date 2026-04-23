package cn.gaifan.douyinOperations.module.benchmark.service.impl;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.benchmark.entity.BenchmarkQualityScript;
import cn.gaifan.douyinOperations.module.benchmark.repository.BenchmarkQualityScriptRepository;
import cn.gaifan.douyinOperations.module.benchmark.service.BenchmarkQualityScriptService;
import cn.gaifan.douyinOperations.module.benchmark.vo.BenchmarkQualityScriptSearchVO;
import cn.gaifan.douyinOperations.module.benchmark.vo.BenchmarkQualityScriptSaveVO;
import cn.gaifan.douyinOperations.module.benchmark.vo.BenchmarkQualityScriptVO;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 质量脚本知识库服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BenchmarkQualityScriptServiceImpl implements BenchmarkQualityScriptService {

    private final BenchmarkQualityScriptRepository scriptRepository;

    @Override
    @Cacheable(value = "benchmark:quality:script:list", key = "#searchVO.hashCode() + '_' + #ownerId")
    public PageResultVO<BenchmarkQualityScriptVO> search(BenchmarkQualityScriptSearchVO searchVO, Long ownerId) {
        searchVO.validateParams();

        log.debug("查询质量脚本列表: ownerId={}, industry={}, sceneType={}, minQualityScore={}",
                ownerId, searchVO.getIndustry(), searchVO.getSceneType(), searchVO.getMinQualityScore());

        Specification<BenchmarkQualityScript> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 数据隔离
            predicates.add(cb.equal(root.get("ownerId"), ownerId));

            // 视频 ID 过滤
            if (searchVO.getVideoId() != null) {
                predicates.add(cb.equal(root.get("videoId"), searchVO.getVideoId()));
            }

            // 脚本类型过滤
            if (StringUtils.hasText(searchVO.getScriptType())) {
                predicates.add(cb.equal(root.get("scriptType"), searchVO.getScriptType()));
            }

            // 行业分类过滤
            if (StringUtils.hasText(searchVO.getIndustry())) {
                predicates.add(cb.equal(root.get("industry"), searchVO.getIndustry()));
            }

            // 场景类型过滤
            if (StringUtils.hasText(searchVO.getSceneType())) {
                predicates.add(cb.equal(root.get("sceneType"), searchVO.getSceneType()));
            }

            // 最小质量评分过滤
            if (searchVO.getMinQualityScore() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("qualityScore"), BigDecimal.valueOf(searchVO.getMinQualityScore())));
            }

            // 最小互动率过滤
            if (searchVO.getMinEngagementRate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("engagementRate"), BigDecimal.valueOf(searchVO.getMinEngagementRate())));
            }

            // 最小传播力评分过滤
            if (searchVO.getMinViralScore() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("viralScore"), BigDecimal.valueOf(searchVO.getMinViralScore())));
            }

            // 关键词模糊查询
            if (StringUtils.hasText(searchVO.getKeyword())) {
                predicates.add(cb.like(root.get("scriptContent"), "%" + searchVO.getKeyword() + "%"));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Sort sort = Sort.by(Sort.Direction.DESC, "qualityScore", "createTime");
        Pageable pageable = PageRequest.of(searchVO.getPage(), searchVO.getRows(), sort);
        Page<BenchmarkQualityScript> page = scriptRepository.findAll(spec, pageable);

        List<BenchmarkQualityScriptVO> voList = page.getContent().stream()
                .map(this::entityToVO)
                .toList();

        log.debug("查询质量脚本列表完成: total={}, page=", page.getTotalElements(), searchVO.getPage());

        return new PageResultVO<>(page.getTotalElements(), voList, searchVO.getPage(), searchVO.getRows());
    }

    @Override
    public BenchmarkQualityScriptVO getById(Long id, Long ownerId) {
        log.debug("查询质量脚本详情: id={}, ownerId={}", id, ownerId);

        BenchmarkQualityScript script = scriptRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("质量脚本不存在"));

        if (!script.getOwnerId().equals(ownerId)) {
            log.warn("无权访问质量脚本: id={}, ownerId={}, scriptOwnerId={}", id, ownerId, script.getOwnerId());
            throw new RuntimeException("无权访问该质量脚本");
        }

        return entityToVO(script);
    }

    @Override
    @Transactional
    @CacheEvict(value = "benchmark:quality:script:list", allEntries = true)
    public BenchmarkQualityScriptVO save(BenchmarkQualityScriptSaveVO saveVO, Long ownerId) {
        log.info("保存质量脚本: id={}, videoId={}, ownerId={}", saveVO.getId(), saveVO.getVideoId(), ownerId);

        BenchmarkQualityScript script;

        if (saveVO.getId() != null) {
            // 更新
            script = scriptRepository.findById(saveVO.getId())
                    .orElseThrow(() -> new RuntimeException("质量脚本不存在"));

            if (!script.getOwnerId().equals(ownerId)) {
                throw new RuntimeException("无权修改该质量脚本");
            }
        } else {
            // 新增 - 检查是否已存在
            scriptRepository.findByVideoIdAndDeleted(saveVO.getVideoId(), 0)
                    .ifPresent(existing -> {
                        throw new RuntimeException("该视频的质量脚本已存在");
                    });

            script = new BenchmarkQualityScript();
            script.setOwnerId(ownerId);
        }

        // 复制属性
        BeanUtils.copyProperties(saveVO, script, "id", "ownerId", "embeddingVector", "referenceCount", "lastReferencedAt", "createTime", "updateTime", "deleted");

        // 如果没有提供质量评分，自动计算
        if (saveVO.getQualityScore() == null) {
            BigDecimal calculatedScore = calculateQualityScore(saveVO);
            script.setQualityScore(calculatedScore);
        }

        script = scriptRepository.save(script);

        log.info("质量脚本保存成功: id={}, videoId={}, qualityScore={}", script.getId(), script.getVideoId(), script.getQualityScore());

        return entityToVO(script);
    }

    @Override
    @Transactional
    @CacheEvict(value = "benchmark:quality:script:list", allEntries = true)
    public void delete(Long id, Long ownerId) {
        log.info("删除质量脚本: id={}, ownerId={}", id, ownerId);

        BenchmarkQualityScript script = scriptRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("质量脚本不存在"));

        if (!script.getOwnerId().equals(ownerId)) {
            throw new RuntimeException("无权删除该质量脚本");
        }

        script.setDeleted(1);
        scriptRepository.save(script);

        log.info("质量脚本删除成功: id={}", id);
    }

    @Override
    public BenchmarkQualityScriptVO getByVideoId(Long videoId, Long ownerId) {
        log.debug("根据视频 ID 查询质量脚本: videoId={}, ownerId={}", videoId, ownerId);

        BenchmarkQualityScript script = scriptRepository.findByVideoIdAndDeleted(videoId, 0)
                .orElseThrow(() -> new RuntimeException("质量脚本不存在"));

        if (!script.getOwnerId().equals(ownerId)) {
            throw new RuntimeException("无权访问该质量脚本");
        }

        return entityToVO(script);
    }

    @Override
    public BenchmarkQualityScriptVO getByAnalysisId(Long analysisId, Long ownerId) {
        log.debug("根据分析 ID 查询质量脚本: analysisId={}, ownerId={}", analysisId, ownerId);

        BenchmarkQualityScript script = scriptRepository.findByAnalysisIdAndDeleted(analysisId, 0)
                .orElseThrow(() -> new RuntimeException("质量脚本不存在"));

        if (!script.getOwnerId().equals(ownerId)) {
            throw new RuntimeException("无权访问该质量脚本");
        }

        return entityToVO(script);
    }

    @Override
    public List<BenchmarkQualityScriptVO> getHighQualityScriptsByIndustry(String industry, BigDecimal minScore, Long ownerId) {
        log.debug("查询行业高质量脚本: industry=, minScore={}, ownerId={}", industry, minScore, ownerId);

        List<BenchmarkQualityScript> scripts = scriptRepository
                .findByOwnerIdAndIndustryAndQualityScoreGreaterThanEqualAndDeletedOrderByQualityScoreDesc(
                        ownerId, industry, minScore, 0);

        return scripts.stream()
                .map(this::entityToVO)
                .toList();
    }

    @Override
    public List<BenchmarkQualityScriptVO> getHighQualityScriptsByScene(String sceneType, BigDecimal minScore, Long ownerId) {
        log.debug("查询场景高质量脚本: sceneType={}, minScore={}, ownerId={}", sceneType, minScore, ownerId);

        List<BenchmarkQualityScript> scripts = scriptRepository
                .findByOwnerIdAndSceneTypeAndQualityScoreGreaterThanEqualAndDeletedOrderByQualityScoreDesc(
                        ownerId, sceneType, minScore, 0);

        return scripts.stream()
                .map(this::entityToVO)
                .toList();
    }

    @Override
    public List<BenchmarkQualityScriptVO> getTopEngagementScripts(Integer limit, Long ownerId) {
        log.debug("查询互动率最高脚本: limit={}, ownerId={}", limit, ownerId);

        List<BenchmarkQualityScript> scripts = scriptRepository
                .findByOwnerIdAndDeletedOrderByEngagementRateDesc(ownerId, 0);

        return scripts.stream()
                .limit(limit != null ? limit : 10)
                .map(this::entityToVO)
                .toList();
    }

    @Override
    public List<BenchmarkQualityScriptVO> getTopViralScripts(Integer limit, Long ownerId) {
        log.debug("查询传播力最高脚本: limit={}, ownerId={}", limit, ownerId);

        List<BenchmarkQualityScript> scripts = scriptRepository
                .findByOwnerIdAndDeletedOrderByViralScoreDesc(ownerId, 0);

        return scripts.stream()
                .limit(limit != null ? limit : 10)
                .map(this::entityToVO)
                .toList();
    }

    @Override
    @Transactional
    public void updateReferenceStats(Long scriptId) {
        log.debug("更新脚本引用统计: scriptId={}", scriptId);

        scriptRepository.findById(scriptId).ifPresent(script -> {
            script.setReferenceCount(script.getReferenceCount() + 1);
            script.setLastReferencedAt(LocalDateTime.now());
            scriptRepository.save(script);
        });
    }

    @Override
    public BigDecimal calculateQualityScore(BenchmarkQualityScriptSaveVO saveVO) {
        log.debug("计算质量评分: videoId={}", saveVO.getVideoId());

        // 综合评分算法：
        // 质量分 = 互动率权重 * 30% + 传播力权重 * 25% + 完播率权重 * 20% + AI评分权重 * 25%

        BigDecimal score = BigDecimal.ZERO;
        int weightCount = 0;

        // 互动率评分（30%）
        if (saveVO.getEngagementRate() != null) {
            score = score.add(saveVO.getEngagementRate().multiply(BigDecimal.valueOf(0.3)));
            weightCount++;
        }

        // 传播力评分（25%）
        if (saveVO.getViralScore() != null) {
            score = score.add(saveVO.getViralScore().multiply(BigDecimal.valueOf(0.25)));
            weightCount++;
        }

        // 完播率评分（20%）
        if (saveVO.getCompletionRate() != null) {
            score = score.add(saveVO.getCompletionRate().multiply(BigDecimal.valueOf(0.2)));
            weightCount++;
        }

        // AI 评分（25%）
        if (saveVO.getAiRating() != null) {
            score = score.add(saveVO.getAiRating().multiply(BigDecimal.valueOf(0.25)));
            weightCount++;
        }

        // 如果没有任何评分数据，返回默认值
        if (weightCount == 0) {
            return BigDecimal.valueOf(50.0);
        }

        // 归一化到 0-100 范围
        BigDecimal normalizedScore = score.min(BigDecimal.valueOf(100.0)).max(BigDecimal.ZERO);

        log.debug("质量评分计算完成: score={}, weightCount={}", normalizedScore, weightCount);

        return normalizedScore.setScale(2, RoundingMode.HALF_UP);
    }

    private BenchmarkQualityScriptVO entityToVO(BenchmarkQualityScript entity) {
        BenchmarkQualityScriptVO vo = new BenchmarkQualityScriptVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
