package cn.gaifan.douyinOperations.module.abtest.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.abtest.entity.*;
import cn.gaifan.douyinOperations.module.abtest.repository.*;
import cn.gaifan.douyinOperations.module.abtest.vo.*;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
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
public class AbTestServiceImpl implements cn.gaifan.douyinOperations.module.abtest.service.AbTestService {

    private static final Set<String> SORTABLE = Set.of("id", "ownerId", "status", "experimentType", "createTime", "startTime");

    @Resource
    private AbExperimentRepository experimentRepository;
    @Resource
    private AbVariantRepository variantRepository;
    @Resource
    private AbEventRepository eventRepository;

    // ==================== 实验管理 ====================

    public PageResultVO<AbExperimentVO> search(AbExperimentSearchVO vo) {
        vo.validateParams();
        String sortName = SORTABLE.contains(vo.getSortName()) ? vo.getSortName() : "createTime";
        Pageable pageable = PageRequest.of(vo.getPage(), vo.getRows(),
                Sort.by("desc".equalsIgnoreCase(vo.getSortOrder()) ? Sort.Direction.DESC : Sort.Direction.ASC, sortName));

        Specification<AbExperiment> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("deleted"), 0));
            if (vo.getOwnerId() != null && vo.getOwnerId() > 0) {
                predicates.add(cb.equal(root.get("ownerId"), vo.getOwnerId()));
            } else if (vo.getOwnerIds() != null && !vo.getOwnerIds().isEmpty()) {
                predicates.add(root.get("ownerId").in(vo.getOwnerIds()));
            }
            if (vo.getExperimentType() != null && !vo.getExperimentType().isBlank()) {
                predicates.add(cb.equal(root.get("experimentType"), vo.getExperimentType().trim()));
            }
            if (vo.getStatus() != null) predicates.add(cb.equal(root.get("status"), vo.getStatus()));
            if (vo.getKeyword() != null && !vo.getKeyword().isBlank()) {
                predicates.add(cb.like(root.get("name"), "%" + vo.getKeyword().trim() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<AbExperiment> page = experimentRepository.findAll(spec, pageable);
        List<AbExperimentVO> list = page.getContent().stream().map(e -> {
            AbExperimentVO vo2 = toExperimentVO(e);
            vo2.setVariants(variantRepository.findByExperimentIdAndDeleted(e.getId(), 0)
                    .stream().map(this::toVariantVO).collect(Collectors.toList()));
            return vo2;
        }).collect(Collectors.toList());
        return PageResultVO.of(page.getTotalElements(), list, vo.getPage(), vo.getRows());
    }

    @Cacheable(value = "abtest:experiment", key = "#id", unless = "#result == null")
    public AbExperimentVO getById(Long id) {
        AbExperiment e = experimentRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "实验不存在"));
        AbExperimentVO vo = toExperimentVO(e);
        vo.setVariants(variantRepository.findByExperimentIdAndDeleted(id, 0)
                .stream().map(this::toVariantVO).collect(Collectors.toList()));
        return vo;
    }

    // P0-1: 带所有权校验的 getById
    public AbExperimentVO getById(Long id, Long userId) {
        AbExperiment e = experimentRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "实验不存在"));
        if (!e.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问此实验");
        }
        AbExperimentVO vo = toExperimentVO(e);
        vo.setVariants(variantRepository.findByExperimentIdAndDeleted(id, 0)
                .stream().map(this::toVariantVO).collect(Collectors.toList()));
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "abtest:experiment", key = "#result")
    public long save(AbExperimentSaveVO vo) {
        // P1-2: 业务规则校验
        if (vo.getVariants() != null && !vo.getVariants().isEmpty()) {
            if (vo.getVariants().size() < 2) {
                throw new BusinessException(ErrorCode.VALIDATION_FAIL, "至少需要 2 个变体");
            }
            Set<String> types = vo.getVariants().stream()
                    .map(v -> v.getVariantType())
                    .collect(Collectors.toSet());
            if (!types.contains("A") || !types.contains("B")) {
                throw new BusinessException(ErrorCode.VALIDATION_FAIL, "必须包含 A 和 B 变体");
            }
        }

        if (vo.getExperimentType() != null &&
            !Set.of("video", "live", "copy", "script_style").contains(vo.getExperimentType())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "实验类型不合法");
        }

        AbExperiment entity;
        if (vo.getId() != null && vo.getId() > 0) {
            entity = experimentRepository.findByIdAndDeleted(vo.getId(), 0)
                    .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "实验不存在"));
        } else {
            entity = new AbExperiment();
            entity.setOwnerId(vo.getOwnerId());
        }
        entity.setName(vo.getName());
        entity.setExperimentType(vo.getExperimentType());
        if (vo.getDescription() != null) entity.setDescription(vo.getDescription());
        if (vo.getStatus() != null) entity.setStatus(vo.getStatus());
        if (vo.getConclusion() != null) entity.setConclusion(vo.getConclusion());
        if (vo.getTargetEntityType() != null) entity.setTargetEntityType(vo.getTargetEntityType());
        if (vo.getTargetEntityId() != null) entity.setTargetEntityId(vo.getTargetEntityId());
        entity = experimentRepository.save(entity);

        // 保存变体
        if (vo.getVariants() != null) {
            for (AbVariantSaveVO variantVO : vo.getVariants()) {
                variantVO.setExperimentId(entity.getId());
                saveVariant(variantVO);
            }
        }
        return entity.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "abtest:experiment", key = "#id")
    public void delete(Long id) {
        AbExperiment entity = experimentRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "实验不存在"));
        entity.setDeleted(1);
        experimentRepository.save(entity);
        // 同步删除变体
        variantRepository.findByExperimentIdAndDeleted(id, 0).forEach(v -> {
            v.setDeleted(1);
            variantRepository.save(v);
        });
    }

    // P0-1: 带所有权校验的 delete
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "abtest:experiment", key = "#id")
    public void delete(Long id, Long userId) {
        AbExperiment entity = experimentRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "实验不存在"));
        if (!entity.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权删除此实验");
        }
        entity.setDeleted(1);
        experimentRepository.save(entity);
        // 同步删除变体
        variantRepository.findByExperimentIdAndDeleted(id, 0).forEach(v -> {
            v.setDeleted(1);
            variantRepository.save(v);
        });
    }

    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "abtest:experiment", key = "#id")
    public void updateStatus(Long id, Integer status) {
        experimentRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "实验不存在"));
        experimentRepository.updateStatus(id, status, new Timestamp(System.currentTimeMillis()));
    }

    // P0-1: 带所有权校验的 updateStatus
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "abtest:experiment", key = "#id")
    public void updateStatus(Long id, Integer status, Long userId) {
        AbExperiment entity = experimentRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "实验不存在"));
        if (!entity.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权修改此实验");
        }
        experimentRepository.updateStatus(id, status, new Timestamp(System.currentTimeMillis()));
    }

    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "abtest:experiment", key = "#vo.experimentId")
    public void setWinner(AbSetWinnerVO vo) {
        experimentRepository.findByIdAndDeleted(vo.getExperimentId(), 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "实验不存在"));
        variantRepository.findByIdAndDeleted(vo.getVariantId(), 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "变体不存在"));
        experimentRepository.setWinner(vo.getExperimentId(), vo.getVariantId(),
                vo.getConclusion(), new Timestamp(System.currentTimeMillis()));
        variantRepository.updateIsWinner(vo.getVariantId(), 1);
    }

    // P0-1: 带所有权校验的 setWinner
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "abtest:experiment", key = "#vo.experimentId")
    public void setWinner(AbSetWinnerVO vo, Long userId) {
        AbExperiment entity = experimentRepository.findByIdAndDeleted(vo.getExperimentId(), 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "实验不存在"));
        if (!entity.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权修改此实验");
        }
        variantRepository.findByIdAndDeleted(vo.getVariantId(), 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "变体不存在"));
        experimentRepository.setWinner(vo.getExperimentId(), vo.getVariantId(),
                vo.getConclusion(), new Timestamp(System.currentTimeMillis()));
        variantRepository.updateIsWinner(vo.getVariantId(), 1);
    }

    // ==================== 变体管理 ====================

    @Transactional(rollbackFor = Exception.class)
    public long saveVariant(AbVariantSaveVO vo) {
        AbVariant entity;
        if (vo.getId() != null && vo.getId() > 0) {
            entity = variantRepository.findByIdAndDeleted(vo.getId(), 0)
                    .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "变体不存在"));
        } else {
            entity = new AbVariant();
            entity.setExperimentId(vo.getExperimentId());
            entity.setVariantType(vo.getVariantType());
        }
        entity.setVariantName(vo.getVariantName());
        if (vo.getContent() != null) entity.setContent(vo.getContent());
        if (vo.getEntityType() != null) entity.setEntityType(vo.getEntityType());
        if (vo.getEntityId() != null) entity.setEntityId(vo.getEntityId());
        if (vo.getStyleCode() != null) entity.setStyleCode(vo.getStyleCode());
        return variantRepository.save(entity).getId();
    }

    // P0-1: 带所有权校验的 saveVariant
    @Transactional(rollbackFor = Exception.class)
    public long saveVariant(AbVariantSaveVO vo, Long userId) {
        // 校验实验所有权
        AbExperiment exp = experimentRepository.findByIdAndDeleted(vo.getExperimentId(), 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "实验不存在"));
        if (!exp.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作此实验");
        }

        AbVariant entity;
        if (vo.getId() != null && vo.getId() > 0) {
            entity = variantRepository.findByIdAndDeleted(vo.getId(), 0)
                    .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "变体不存在"));
        } else {
            entity = new AbVariant();
            entity.setExperimentId(vo.getExperimentId());
            entity.setVariantType(vo.getVariantType());
        }
        entity.setVariantName(vo.getVariantName());
        if (vo.getContent() != null) entity.setContent(vo.getContent());
        if (vo.getEntityType() != null) entity.setEntityType(vo.getEntityType());
        if (vo.getEntityId() != null) entity.setEntityId(vo.getEntityId());
        if (vo.getStyleCode() != null) entity.setStyleCode(vo.getStyleCode());
        return variantRepository.save(entity).getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteVariant(Long id) {
        AbVariant entity = variantRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "变体不存在"));
        entity.setDeleted(1);
        variantRepository.save(entity);
    }

    // P0-1: 带所有权校验的 deleteVariant
    @Transactional(rollbackFor = Exception.class)
    public void deleteVariant(Long id, Long userId) {
        AbVariant entity = variantRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "变体不存在"));
        // 校验实验所有权
        AbExperiment exp = experimentRepository.findByIdAndDeleted(entity.getExperimentId(), 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "实验不存在"));
        if (!exp.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作此实验");
        }
        entity.setDeleted(1);
        variantRepository.save(entity);
    }

    // ==================== 事件记录 ====================

    @Transactional(rollbackFor = Exception.class)
    public void recordEvent(AbEventSaveVO vo) {
        // P1-1: SHA256 哈希 user_fingerprint
        String hashedFingerprint = org.apache.commons.codec.digest.DigestUtils.sha256Hex(vo.getUserFingerprint());

        // 去重：同一用户同一变体同一事件类型只记录一次
        if (eventRepository.existsByVariantIdAndEventTypeAndUserFingerprint(
                vo.getVariantId(), vo.getEventType(), hashedFingerprint)) {
            return;
        }
        AbEvent event = new AbEvent();
        event.setExperimentId(vo.getExperimentId());
        event.setVariantId(vo.getVariantId());
        event.setEventType(vo.getEventType());
        event.setUserFingerprint(hashedFingerprint);
        event.setSessionId(vo.getSessionId());
        eventRepository.save(event);

        // 同步更新变体计数
        switch (vo.getEventType()) {
            case "view" -> variantRepository.incrementViewCount(vo.getVariantId());
            case "click" -> variantRepository.incrementClickCount(vo.getVariantId());
            case "conversion" -> variantRepository.incrementConversionCount(vo.getVariantId());
        }
    }

    // P0-1: 带所有权校验的 recordEvent
    @Transactional(rollbackFor = Exception.class)
    public void recordEvent(AbEventSaveVO vo, Long userId) {
        // 校验实验所有权
        AbExperiment exp = experimentRepository.findByIdAndDeleted(vo.getExperimentId(), 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "实验不存在"));
        if (!exp.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作此实验");
        }

        // 去重：同一用户同一变体同一事件类型只记录一次
        if (eventRepository.existsByVariantIdAndEventTypeAndUserFingerprint(
                vo.getVariantId(), vo.getEventType(), vo.getUserFingerprint())) {
            return;
        }
        AbEvent event = new AbEvent();
        event.setExperimentId(vo.getExperimentId());
        event.setVariantId(vo.getVariantId());
        event.setEventType(vo.getEventType());
        event.setUserFingerprint(vo.getUserFingerprint());
        event.setSessionId(vo.getSessionId());
        eventRepository.save(event);

        // 同步更新变体计数
        switch (vo.getEventType()) {
            case "view" -> variantRepository.incrementViewCount(vo.getVariantId());
            case "click" -> variantRepository.incrementClickCount(vo.getVariantId());
            case "conversion" -> variantRepository.incrementConversionCount(vo.getVariantId());
        }
    }

    // ==================== toVO ====================

    private AbExperimentVO toExperimentVO(AbExperiment e) {
        AbExperimentVO vo = new AbExperimentVO();
        vo.setId(e.getId());
        vo.setOwnerId(e.getOwnerId());
        vo.setName(e.getName());
        vo.setDescription(e.getDescription());
        vo.setExperimentType(e.getExperimentType());
        vo.setTargetEntityType(e.getTargetEntityType());
        vo.setTargetEntityId(e.getTargetEntityId());
        vo.setStatus(e.getStatus());
        vo.setStartTime(e.getStartTime());
        vo.setEndTime(e.getEndTime());
        vo.setWinnerVariantId(e.getWinnerVariantId());
        vo.setConclusion(e.getConclusion());
        vo.setCreateTime(e.getCreateTime());
        vo.setUpdateTime(e.getUpdateTime());
        return vo;
    }

    private AbVariantVO toVariantVO(AbVariant e) {
        AbVariantVO vo = new AbVariantVO();
        vo.setId(e.getId());
        vo.setExperimentId(e.getExperimentId());
        vo.setVariantName(e.getVariantName());
        vo.setVariantType(e.getVariantType());
        vo.setContent(e.getContent());
        vo.setEntityType(e.getEntityType());
        vo.setEntityId(e.getEntityId());
        vo.setStyleCode(e.getStyleCode());
        vo.setViewCount(e.getViewCount());
        vo.setClickCount(e.getClickCount());
        vo.setConversionCount(e.getConversionCount());
        vo.setConversionRate(e.getConversionRate());
        vo.setIsWinner(e.getIsWinner());
        vo.setCreateTime(e.getCreateTime());
        vo.setUpdateTime(e.getUpdateTime());
        return vo;
    }

    // ==================== 统计分析 ====================

    @Cacheable(value = "abtest:statistics", key = "#experimentId", unless = "#result == null")
    public AbExperimentStatisticsVO getExperimentStatistics(Long experimentId) {
        AbExperiment experiment = experimentRepository.findByIdAndDeleted(experimentId, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "实验不存在"));

        AbExperimentStatisticsVO result = new AbExperimentStatisticsVO();
        result.setExperimentId(experimentId);
        result.setExperimentName(experiment.getName());
        result.setStatus(experiment.getStatus());

        // 获取变体统计数据
        List<Object[]> variantStatsArray = eventRepository.getVariantStatistics(experimentId);
        List<AbVariantStatsVO> variantStats = variantStatsArray.stream()
                .map(row -> new AbVariantStatsVO(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        (String) row[2],
                        ((Number) row[3]).longValue(),
                        ((Number) row[4]).longValue(),
                        row[5] != null ? ((Number) row[5]).doubleValue() : 0.0,
                        row[6] != null ? ((Number) row[6]).doubleValue() : 0.0
                ))
                .collect(Collectors.toList());
        result.setVariantStats(variantStats);

        // 计算总样本量和总转化数
        long totalSamples = variantStats.stream().mapToLong(AbVariantStatsVO::getViewCount).sum();
        long totalConversions = variantStats.stream().mapToLong(AbVariantStatsVO::getConversionCount).sum();
        double overallConversionRate = totalSamples > 0 ? (100.0 * totalConversions / totalSamples) : 0.0;
        result.setTotalSamples(totalSamples);
        result.setTotalConversions(totalConversions);
        result.setOverallConversionRate(Math.round(overallConversionRate * 100.0) / 100.0);

        // 执行卡方检验
        if (variantStats.size() >= 2) {
            AbStatisticalTestVO statisticalTest = calculateChiSquareTest(variantStats);
            result.setStatisticalTest(statisticalTest);
        }

        // 获取日趋势数据
        List<Object[]> dailyTrendArray = eventRepository.getDailyTrend(experimentId);
        List<AbDailyTrendVO> dailyTrends = dailyTrendArray.stream()
                .map(row -> {
                    long aViews = row[1] != null ? ((Number) row[1]).longValue() : 0;
                    long bViews = row[2] != null ? ((Number) row[2]).longValue() : 0;
                    long aConversions = row[3] != null ? ((Number) row[3]).longValue() : 0;
                    long bConversions = row[4] != null ? ((Number) row[4]).longValue() : 0;
                    double aRate = aViews > 0 ? (100.0 * aConversions / aViews) : 0.0;
                    double bRate = bViews > 0 ? (100.0 * bConversions / bViews) : 0.0;
                    return new AbDailyTrendVO(
                            ((java.sql.Date) row[0]).toLocalDate(),
                            aViews,
                            bViews,
                            aConversions,
                            bConversions,
                            Math.round(aRate * 100.0) / 100.0,
                            Math.round(bRate * 100.0) / 100.0
                    );
                })
                .collect(Collectors.toList());
        result.setDailyTrends(dailyTrends);

        return result;
    }

    /**
     * 执行卡方检验并返回结果
     */
    private AbStatisticalTestVO calculateChiSquareTest(List<AbVariantStatsVO> variantStats) {
        if (variantStats.size() < 2) {
            return null;
        }

        AbVariantStatsVO variantA = variantStats.stream()
                .filter(v -> "A".equals(v.getVariantType()))
                .findFirst()
                .orElse(variantStats.get(0));

        AbVariantStatsVO variantB = variantStats.stream()
                .filter(v -> "B".equals(v.getVariantType()))
                .findFirst()
                .orElse(variantStats.size() > 1 ? variantStats.get(1) : variantA);

        try {
            org.apache.commons.math3.stat.inference.ChiSquareTest chiTest = new org.apache.commons.math3.stat.inference.ChiSquareTest();

            // 建立列联表：[非转化, 转化]
            long[][] counts = {
                    {variantA.getViewCount() - variantA.getConversionCount(), variantA.getConversionCount()},
                    {variantB.getViewCount() - variantB.getConversionCount(), variantB.getConversionCount()}
            };

            double chiSquare = chiTest.chiSquare(counts);
            double pValue = chiTest.chiSquareTest(counts);
            double confidenceLevel = 1 - pValue;
            boolean isSignificant = pValue < 0.05;

            // 确定赢家（转化率更高）
            double rateA = variantA.getViewCount() > 0 ? (double) variantA.getConversionCount() / variantA.getViewCount() : 0;
            double rateB = variantB.getViewCount() > 0 ? (double) variantB.getConversionCount() / variantB.getViewCount() : 0;
            Long winnerVariantId = rateA > rateB ? variantA.getVariantId() : variantB.getVariantId();
            String winnerName = rateA > rateB ? variantA.getVariantName() : variantB.getVariantName();

            String conclusion = String.format(
                    "变体 A 转化率 %.2f%%，变体 B 转化率 %.2f%%，%s有显著差异（p=%.4f）",
                    rateA * 100, rateB * 100, isSignificant ? "" : "无", pValue
            );

            return new AbStatisticalTestVO(
                    Math.round(chiSquare * 10000.0) / 10000.0,
                    Math.round(pValue * 10000.0) / 10000.0,
                    Math.round(confidenceLevel * 100.0) / 100.0,
                    isSignificant,
                    winnerVariantId,
                    winnerName,
                    conclusion
            );
        } catch (Exception e) {
            return new AbStatisticalTestVO(
                    0.0, 1.0, 0.0, false, variantA.getVariantId(), variantA.getVariantName(),
                    "统计检验失败: " + e.getMessage()
            );
        }
    }

    /**
     * 获取日趋势数据（带时间范围）
     */
    public List<AbDailyTrendVO> getDailyTrend(Long experimentId, java.time.LocalDate startDate, java.time.LocalDate endDate) {
        experimentRepository.findByIdAndDeleted(experimentId, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "实验不存在"));

        java.time.LocalDateTime startDateTime = startDate.atStartOfDay();
        java.time.LocalDateTime endDateTime = endDate.atTime(java.time.LocalTime.MAX);

        Timestamp startTs = Timestamp.valueOf(startDateTime);
        Timestamp endTs = Timestamp.valueOf(endDateTime);

        List<Object[]> dailyTrendArray = eventRepository.getDailyTrendByDateRange(experimentId, startTs, endTs);
        return dailyTrendArray.stream()
                .map(row -> {
                    long aViews = row[1] != null ? ((Number) row[1]).longValue() : 0;
                    long bViews = row[2] != null ? ((Number) row[2]).longValue() : 0;
                    long aConversions = row[3] != null ? ((Number) row[3]).longValue() : 0;
                    long bConversions = row[4] != null ? ((Number) row[4]).longValue() : 0;
                    double aRate = aViews > 0 ? (100.0 * aConversions / aViews) : 0.0;
                    double bRate = bViews > 0 ? (100.0 * bConversions / bViews) : 0.0;
                    return new AbDailyTrendVO(
                            ((java.sql.Date) row[0]).toLocalDate(),
                            aViews,
                            bViews,
                            aConversions,
                            bConversions,
                            Math.round(aRate * 100.0) / 100.0,
                            Math.round(bRate * 100.0) / 100.0
                    );
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int autoConvergeAll() {
        List<AbExperiment> running = experimentRepository.findAll(
            (root, query, cb) -> cb.and(
                cb.equal(root.get("deleted"), 0),
                cb.equal(root.get("status"), 1)
            )
        );
        int converged = 0;
        for (AbExperiment experiment : running) {
            try {
                List<AbVariant> variants = variantRepository.findByExperimentIdAndDeleted(experiment.getId(), 0);
                if (variants.size() < 2) continue;
                long totalViews = variants.stream().mapToLong(v -> v.getViewCount() != null ? v.getViewCount() : 0L).sum();
                if (totalViews < 100) continue;
                AbVariant best = variants.stream()
                        .max(Comparator.comparingLong(v -> v.getConversionCount() != null ? v.getConversionCount() : 0L))
                        .orElse(null);
                if (best == null) continue;
                experiment.setStatus(2);
                experiment.setWinnerVariantId(best.getId());
                experimentRepository.save(experiment);
                converged++;
            } catch (Exception e) {
                // skip individual failures
            }
        }
        return converged;
    }
}
