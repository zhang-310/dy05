package cn.gaifan.douyinOperations.module.product.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.product.entity.ProductScriptSnapshot;
import cn.gaifan.douyinOperations.module.product.entity.ProductScriptVersion;
import cn.gaifan.douyinOperations.module.product.repository.ProductScriptSnapshotRepository;
import cn.gaifan.douyinOperations.module.product.repository.ProductScriptVersionRepository;
import cn.gaifan.douyinOperations.module.product.service.ProductScriptVersionService;
import cn.gaifan.douyinOperations.module.product.vo.ProductScriptRecommendVO;
import cn.gaifan.douyinOperations.module.product.vo.ProductScriptSnapshotVO;
import cn.gaifan.douyinOperations.module.product.vo.ProductScriptVersionSaveVO;
import cn.gaifan.douyinOperations.module.product.vo.ProductScriptVersionSearchVO;
import cn.gaifan.douyinOperations.module.product.vo.ProductScriptVersionVO;
import cn.gaifan.douyinOperations.module.product.vo.ProductVersionDiffVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 商品话术版本业务逻辑实现
 * 提供话术版本的 CRUD、查询、推荐、效果更新等核心业务功能
 *
 * @author Claude Code
 * @since 2026-03-06
 */
@Slf4j
@Service
@Transactional
public class ProductScriptVersionServiceImpl implements ProductScriptVersionService {

    @Resource
    private ProductScriptVersionRepository versionRepository;

    @Resource
    private ProductScriptSnapshotRepository snapshotRepository;

    // ===== 常量定义 =====
    private static final int MAX_USAGE_COUNT = 100;
    private static final double EFFECTIVENESS_WEIGHT = 0.5;
    private static final double USAGE_WEIGHT = 0.3;
    private static final double CONVERSION_WEIGHT = 0.2;

    /**
     * 保存话术版本
     * 自动计算版本号（基于产品现有最大版本号 +1）
     *
     * @param vo     保存参数
     * @param userId 当前用户 ID（作为 owner_id）
     * @return 话术版本返回值
     */
    @Override
    public ProductScriptVersionVO save(ProductScriptVersionSaveVO vo, Long userId) {
        log.info("保存话术版本: productId={}, userId={}", vo.getProductId(), userId);

        // 查询该产品当前的最大版本号
        Integer maxVersionNumber = versionRepository.findMaxVersionNumberByProductId(vo.getProductId());
        Integer nextVersionNumber = (maxVersionNumber != null ? maxVersionNumber : 0) + 1;

        // 创建新实体
        ProductScriptVersion entity = ProductScriptVersion.builder()
                .productId(vo.getProductId())
                .scriptId(vo.getScriptId())
                .versionNumber(nextVersionNumber)
                .content(vo.getContent())
                .style(vo.getStyle())
                .effectivenessScore(vo.getEffectivenessScore() != null ? vo.getEffectivenessScore() : BigDecimal.ZERO)
                .conversionRate(vo.getConversionRate() != null ? vo.getConversionRate() : BigDecimal.ZERO)
                .isActive(vo.getIsActive() != null ? vo.getIsActive() : true)
                .isRecommended(vo.getIsRecommended() != null ? vo.getIsRecommended() : false)
                .ownerId(userId)
                .usageCount(0)
                .likesCount(0)
                .commentsCount(0)
                .deleted(0)
                .build();

        ProductScriptVersion saved = versionRepository.save(entity);
        log.info("话术版本保存成功: id={}, versionNumber={}", saved.getId(), saved.getVersionNumber());

        return convert(saved);
    }

    /**
     * 分页查询话术版本
     * 支持按产品、风格、评分等条件筛选和排序
     *
     * @param searchVO 查询参数
     * @param userId   当前用户 ID（数据隔离）
     * @return 分页结果
     */
    @Override
    public PageResultVO<ProductScriptVersionVO> list(ProductScriptVersionSearchVO searchVO, Long userId) {
        log.info("查询话术版本列表: productId={}, userId={}, page={}, rows={}",
                 searchVO.getProductId(), userId, searchVO.getPage(), searchVO.getRows());

        // 参数验证和修正
        searchVO.validateParams();

        // 构建 Specification 动态查询
        Specification<ProductScriptVersion> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 强制添加 owner_id 隔离条件
            predicates.add(cb.equal(root.get("ownerId"), userId));
            predicates.add(cb.equal(root.get("deleted"), 0));

            // 产品 ID 条件
            if (searchVO.getProductId() != null) {
                predicates.add(cb.equal(root.get("productId"), searchVO.getProductId()));
            }

            // 话术风格条件
            if (searchVO.getStyle() != null && !searchVO.getStyle().isEmpty()) {
                predicates.add(cb.equal(root.get("style"), searchVO.getStyle()));
            }

            // 是否启用条件
            if (searchVO.getIsActive() != null) {
                predicates.add(cb.equal(root.get("isActive"), searchVO.getIsActive()));
            }

            // 是否推荐条件
            if (searchVO.getIsRecommended() != null) {
                predicates.add(cb.equal(root.get("isRecommended"), searchVO.getIsRecommended()));
            }

            // 是否归档条件
            if (searchVO.getArchived() != null) {
                predicates.add(cb.equal(root.get("archived"), searchVO.getArchived()));
            }

            // 最小效果评分条件
            if (searchVO.getMinEffectivenessScore() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("effectivenessScore"),
                                                       searchVO.getMinEffectivenessScore()));
            }

            // 最大效果评分条件
            if (searchVO.getMaxEffectivenessScore() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("effectivenessScore"),
                                                    searchVO.getMaxEffectivenessScore()));
            }

            // 关键词搜索条件（content 字段）
            if (searchVO.getKeyword() != null && !searchVO.getKeyword().isEmpty()) {
                predicates.add(cb.like(cb.upper(root.get("content")),
                                      "%" + searchVO.getKeyword().toUpperCase() + "%"));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // 构建分页和排序信息
        Sort.Direction sortDirection = "desc".equalsIgnoreCase(searchVO.getSortOrder())
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(searchVO.getPage(), searchVO.getRows(),
                                          Sort.by(sortDirection, searchVO.getSortName()));

        // 执行查询
        Page<ProductScriptVersion> page = versionRepository.findAll(spec, pageable);

        // 转换结果
        List<ProductScriptVersionVO> vos = page.getContent().stream()
                .map(this::convert)
                .collect(Collectors.toList());

        return PageResultVO.of(page.getTotalElements(), vos, searchVO.getPage(), searchVO.getRows());
    }

    /**
     * 搜索话术版本
     * 支持关键词搜索、风格过滤、效果评分过滤
     *
     * @param keyword  搜索关键词（搜索 content 字段）
     * @param style    话术风格（可选）
     * @param minScore 最小效果评分（可选）
     * @param page     页码（0-indexed）
     * @param rows     每页行数
     * @param userId   当前用户 ID（数据隔离）
     * @return 分页结果
     */
    @Override
    public PageResultVO<ProductScriptVersionVO> search(String keyword, String style,
                                                       Double minScore, Integer page, Integer rows, Long userId) {
        log.info("搜索话术版本: keyword={}, style={}, minScore={}, userId={}, page={}, rows={}",
                 keyword, style, minScore, userId, page, rows);

        // 参数校验和修正
        if (page == null || page < 0) {
            page = 0;
        }
        if (rows == null || rows < 1) {
            rows = 30;
        } else if (rows > 1000) {
            rows = 1000;
        }

        // 构建 Specification 动态查询
        Specification<ProductScriptVersion> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 强制添加 owner_id 隔离条件
            predicates.add(cb.equal(root.get("ownerId"), userId));
            predicates.add(cb.equal(root.get("deleted"), 0));
            predicates.add(cb.equal(root.get("isActive"), true));

            // 关键词搜索条件
            if (keyword != null && !keyword.isEmpty()) {
                predicates.add(cb.like(cb.upper(root.get("content")),
                                      "%" + keyword.toUpperCase() + "%"));
            }

            // 风格条件
            if (style != null && !style.isEmpty()) {
                predicates.add(cb.equal(root.get("style"), style));
            }

            // 最小效果评分条件
            if (minScore != null && minScore >= 0) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("effectivenessScore"),
                                                       BigDecimal.valueOf(minScore)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // 构建分页和排序信息（按效果评分降序）
        Pageable pageable = PageRequest.of(page, rows, Sort.by(Sort.Direction.DESC, "effectivenessScore"));

        // 执行查询
        Page<ProductScriptVersion> pageResult = versionRepository.findAll(spec, pageable);

        // 转换结果
        List<ProductScriptVersionVO> vos = pageResult.getContent().stream()
                .map(this::convert)
                .collect(Collectors.toList());

        return PageResultVO.of(pageResult.getTotalElements(), vos, page, rows);
    }

    /**
     * 获取话术版本详情
     * 校验 owner_id 以确保数据隔离
     *
     * @param id     话术版本 ID
     * @param userId 当前用户 ID（数据隔离）
     * @return 话术版本详情
     */
    @Override
    public ProductScriptVersionVO getDetail(Long id, Long userId) {
        log.info("获取话术版本详情: id={}, userId={}", id, userId);

        ProductScriptVersion entity = versionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_SCRIPT_NOT_FOUND, "话术版本不存在"));

        // 验证所有权
        if (!entity.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.PRODUCT_FORBIDDEN, "无权访问该话术版本");
        }

        return convert(entity);
    }

    /**
     * 获取产品的推荐话术版本
     * 为每个活跃版本计算推荐分数，按分数排序返回 Top N
     *
     * @param productId 产品 ID
     * @param topN      推荐数量
     * @param userId    当前用户 ID（数据隔离）
     * @return 推荐结果
     */
    @Override
    public ProductScriptRecommendVO recommend(Long productId, Integer topN, Long userId) {
        log.info("获取推荐话术版本: productId={}, topN={}, userId={}", productId, topN, userId);

        if (topN == null || topN <= 0) {
            topN = 5;
        }

        // 查询该产品的所有活跃版本（owner_id = userId）
        Specification<ProductScriptVersion> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("productId"), productId));
            predicates.add(cb.equal(root.get("ownerId"), userId));
            predicates.add(cb.equal(root.get("isActive"), true));
            predicates.add(cb.equal(root.get("deleted"), 0));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // 不分页，获取所有活跃版本
        List<ProductScriptVersion> versions = versionRepository.findAll(spec);

        if (versions.isEmpty()) {
            log.warn("产品没有活跃版本: productId={}", productId);
            return ProductScriptRecommendVO.builder()
                    .versions(Collections.emptyList())
                    .scores(Collections.emptyList())
                    .build();
        }

        // 为每个版本计算推荐分数
        List<Map.Entry<ProductScriptVersion, BigDecimal>> versionScores = versions.stream()
                .map(v -> {
                    double score = calculateRecommendScore(
                            v.getEffectivenessScore().doubleValue(),
                            v.getUsageCount() != null ? v.getUsageCount() : 0,
                            v.getConversionRate().doubleValue()
                    );
                    return new AbstractMap.SimpleEntry<>(v, BigDecimal.valueOf(score));
                })
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .collect(Collectors.toList());

        // 取 Top N
        List<Map.Entry<ProductScriptVersion, BigDecimal>> topVersions = versionScores.stream()
                .limit(topN)
                .collect(Collectors.toList());

        // 转换为返回对象
        List<ProductScriptRecommendVO.RecommendItem> items = topVersions.stream()
                .map(e -> {
                    ProductScriptVersion v = e.getKey();
                    BigDecimal score = e.getValue();
                    return ProductScriptRecommendVO.RecommendItem.builder()
                            .id(v.getId())
                            .versionNumber(v.getVersionNumber())
                            .style(v.getStyle())
                            .effectivenessScore(v.getEffectivenessScore())
                            .usageCount(v.getUsageCount())
                            .conversionRate(v.getConversionRate())
                            .recommendScore(score)
                            .build();
                })
                .collect(Collectors.toList());

        List<BigDecimal> scores = topVersions.stream()
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());

        return ProductScriptRecommendVO.builder()
                .versions(items)
                .scores(scores)
                .build();
    }

    /**
     * 更新话术效果评分
     * 同时更新 conversion_rate 和时间戳
     *
     * @param scriptVersionId      话术版本 ID
     * @param effectivenessScore   效果评分
     * @param conversionRate       转化率
     * @param userId               当前用户 ID（数据隔离）
     * @return 更新后的话术版本
     */
    @Override
    public ProductScriptVersionVO updateEffectiveness(Long scriptVersionId, Double effectivenessScore,
                                                      Double conversionRate, Long userId) {
        log.info("更新话术效果评分: scriptVersionId={}, effectivenessScore={}, conversionRate={}, userId={}",
                 scriptVersionId, effectivenessScore, conversionRate, userId);

        ProductScriptVersion entity = versionRepository.findById(scriptVersionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_SCRIPT_NOT_FOUND, "话术版本不存在"));

        // 验证所有权
        if (!entity.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.PRODUCT_FORBIDDEN, "无权修改该话术版本");
        }

        // 更新字段
        if (effectivenessScore != null) {
            entity.setEffectivenessScore(BigDecimal.valueOf(effectivenessScore));
        }
        if (conversionRate != null) {
            entity.setConversionRate(BigDecimal.valueOf(conversionRate));
        }
        entity.setUpdatedAt(LocalDateTime.now());

        ProductScriptVersion updated = versionRepository.save(entity);
        log.info("话术效果评分更新成功: id={}", updated.getId());

        return convert(updated);
    }

    /**
     * 从话术库引用到直播场次
     * 创建快照记录并增加源话术版本的使用次数
     *
     * @param liveSessionId           直播场次 ID
     * @param productScriptVersionId  话术版本 ID
     * @param userId                  当前用户 ID（数据隔离）
     * @return 快照结果
     */
    @Override
    public ProductScriptSnapshotVO applyFromLibrary(Long liveSessionId, Long productScriptVersionId, Long userId) {
        log.info("从话术库引用: liveSessionId={}, productScriptVersionId={}, userId={}",
                 liveSessionId, productScriptVersionId, userId);

        // 查询源话术版本
        ProductScriptVersion sourceVersion = versionRepository.findById(productScriptVersionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_SCRIPT_NOT_FOUND, "话术版本不存在"));

        // 验证所有权
        if (!sourceVersion.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.PRODUCT_FORBIDDEN, "无权引用该话术版本");
        }

        // 创建快照
        ProductScriptSnapshot snapshot = ProductScriptSnapshot.builder()
                .liveSessionId(liveSessionId)
                .productScriptVersionId(productScriptVersionId)
                .contentSnapshot(sourceVersion.getContent())
                .referencedAt(LocalDateTime.now())
                .ownerId(userId)
                .deleted(0)
                .build();

        ProductScriptSnapshot savedSnapshot = snapshotRepository.save(snapshot);
        log.info("快照创建成功: id={}", savedSnapshot.getId());

        // 增加源话术版本的使用次数
        sourceVersion.setUsageCount((sourceVersion.getUsageCount() != null ? sourceVersion.getUsageCount() : 0) + 1);
        sourceVersion.setUpdatedAt(LocalDateTime.now());
        versionRepository.save(sourceVersion);
        log.info("话术使用次数增加: id={}, usageCount={}", sourceVersion.getId(), sourceVersion.getUsageCount());

        return convertSnapshot(savedSnapshot);
    }

    /**
     * 删除话术版本（逻辑删除）
     * 设置 deleted = 1 和 deletedAt 时间戳
     *
     * @param id     话术版本 ID
     * @param userId 当前用户 ID（数据隔离）
     * @return 是否删除成功
     */
    @Override
    public boolean delete(Long id, Long userId) {
        log.info("删除话术版本: id={}, userId={}", id, userId);

        Optional<ProductScriptVersion> optional = versionRepository.findById(id);
        if (!optional.isPresent()) {
            log.warn("话术版本不存在: id={}", id);
            return false;
        }

        ProductScriptVersion entity = optional.get();

        // 验证所有权
        if (!entity.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.PRODUCT_FORBIDDEN, "无权删除该话术版本");
        }

        // 逻辑删除
        entity.setDeleted(1);
        entity.setDeletedAt(LocalDateTime.now());
        versionRepository.save(entity);

        log.info("话术版本删除成功: id={}", id);
        return true;
    }

    /**
     * 更新话术版本状态
     * 修改 isActive 字段
     *
     * @param id       话术版本 ID
     * @param isActive 是否启用
     * @param userId   当前用户 ID（数据隔离）
     * @return 更新后的话术版本
     */
    @Override
    public ProductScriptVersionVO updateStatus(Long id, Boolean isActive, Long userId) {
        log.info("更新话术版本状态: id={}, isActive={}, userId={}", id, isActive, userId);

        ProductScriptVersion entity = versionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_SCRIPT_NOT_FOUND, "话术版本不存在"));

        // 验证所有权
        if (!entity.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.PRODUCT_FORBIDDEN, "无权修改该话术版本");
        }

        // 更新字段
        if (isActive != null) {
            entity.setIsActive(isActive);
        }
        entity.setUpdatedAt(LocalDateTime.now());

        ProductScriptVersion updated = versionRepository.save(entity);
        log.info("话术版本状态更新成功: id={}, isActive={}", updated.getId(), updated.getIsActive());

        return convert(updated);
    }

    /**
     * 获取特定产品的所有话术版本
     * 按版本号倒序排列
     *
     * @param productId 产品 ID
     * @param userId    当前用户 ID（数据隔离）
     * @return 话术版本列表
     */
    @Override
    public List<ProductScriptVersionVO> listByProductId(Long productId, Long userId) {
        log.info("获取产品的所有话术版本: productId={}, userId={}", productId, userId);

        Specification<ProductScriptVersion> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("productId"), productId));
            predicates.add(cb.equal(root.get("ownerId"), userId));
            predicates.add(cb.equal(root.get("deleted"), 0));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // 按版本号倒序排列
        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE,
                                          Sort.by(Sort.Direction.DESC, "versionNumber"));
        Page<ProductScriptVersion> page = versionRepository.findAll(spec, pageable);

        return page.getContent().stream()
                .map(this::convert)
                .collect(Collectors.toList());
    }

    /**
     * 获取产品的最优版本
     * 查询所有活跃版本，计算推荐分数，返回最高分版本
     *
     * @param productId 产品 ID
     * @param userId    当前用户 ID（数据隔离）
     * @return 最优话术版本，如果不存在返回 null
     */
    @Override
    public ProductScriptVersionVO findBestVersion(Long productId, Long userId) {
        log.info("获取产品的最优版本: productId={}, userId={}", productId, userId);

        Specification<ProductScriptVersion> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("productId"), productId));
            predicates.add(cb.equal(root.get("ownerId"), userId));
            predicates.add(cb.equal(root.get("isActive"), true));
            predicates.add(cb.equal(root.get("deleted"), 0));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // 不分页，获取所有活跃版本
        List<ProductScriptVersion> versions = versionRepository.findAll(spec);

        if (versions.isEmpty()) {
            log.warn("产品没有活跃版本: productId={}", productId);
            return null;
        }

        // 找出分数最高的版本
        ProductScriptVersion bestVersion = versions.stream()
                .max((v1, v2) -> {
                    double score1 = calculateRecommendScore(
                            v1.getEffectivenessScore().doubleValue(),
                            v1.getUsageCount() != null ? v1.getUsageCount() : 0,
                            v1.getConversionRate().doubleValue()
                    );
                    double score2 = calculateRecommendScore(
                            v2.getEffectivenessScore().doubleValue(),
                            v2.getUsageCount() != null ? v2.getUsageCount() : 0,
                            v2.getConversionRate().doubleValue()
                    );
                    return Double.compare(score1, score2);
                })
                .orElse(null);

        if (bestVersion != null) {
            log.info("最优版本: id={}, versionNumber={}", bestVersion.getId(), bestVersion.getVersionNumber());
        }

        return convert(bestVersion);
    }

    /**
     * 增加话术使用次数
     * 将 usage_count + 1
     *
     * @param id     话术版本 ID
     * @param userId 当前用户 ID（数据隔离）
     * @return 更新后的话术版本
     */
    @Override
    public ProductScriptVersionVO increaseUsageCount(Long id, Long userId) {
        log.info("增加话术使用次数: id={}, userId={}", id, userId);

        ProductScriptVersion entity = versionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_SCRIPT_NOT_FOUND, "话术版本不存在"));

        // 验证所有权
        if (!entity.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.PRODUCT_FORBIDDEN, "无权修改该话术版本");
        }

        // 增加使用次数
        entity.setUsageCount((entity.getUsageCount() != null ? entity.getUsageCount() : 0) + 1);
        entity.setUpdatedAt(LocalDateTime.now());

        ProductScriptVersion updated = versionRepository.save(entity);
        log.info("话术使用次数更新成功: id={}, usageCount={}", updated.getId(), updated.getUsageCount());

        return convert(updated);
    }

    /**
     * 更新推荐标记
     * 修改 isRecommended 字段
     *
     * @param id             话术版本 ID
     * @param isRecommended  是否推荐
     * @param userId         当前用户 ID（数据隔离）
     * @return 更新后的话术版本
     */
    @Override
    public ProductScriptVersionVO updateRecommendFlag(Long id, Boolean isRecommended, Long userId) {
        log.info("更新话术推荐标记: id={}, isRecommended={}, userId={}", id, isRecommended, userId);

        ProductScriptVersion entity = versionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_SCRIPT_NOT_FOUND, "话术版本不存在"));

        // 验证所有权
        if (!entity.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.PRODUCT_FORBIDDEN, "无权修改该话术版本");
        }

        // 更新字段
        if (isRecommended != null) {
            entity.setIsRecommended(isRecommended);
        }
        entity.setUpdatedAt(LocalDateTime.now());

        ProductScriptVersion updated = versionRepository.save(entity);
        log.info("话术推荐标记更新成功: id={}, isRecommended={}", updated.getId(), updated.getIsRecommended());

        return convert(updated);
    }

    /**
     * 计算推荐分数
     * 综合考虑效果评分、使用次数、转化率三个维度
     * 公式：score = effectiveness * 0.5 + (usageCount / maxUsageCount) * 100 * 0.3 + conversionRate * 0.2
     *
     * @param effectivenessScore 效果评分（0-100）
     * @param usageCount         使用次数
     * @param conversionRate     转化率（百分比，0-100）
     * @return 推荐分数
     */
    @Override
    public double calculateRecommendScore(double effectivenessScore, int usageCount, double conversionRate) {
        // 确保输入值在有效范围内
        effectivenessScore = Math.max(0, Math.min(100, effectivenessScore));
        usageCount = Math.max(0, usageCount);
        conversionRate = Math.max(0, Math.min(100, conversionRate));

        // 计算推荐分数：加权求和
        double usageNormalized = Math.min(1.0, (double) usageCount / MAX_USAGE_COUNT) * 100;
        double score = effectivenessScore * EFFECTIVENESS_WEIGHT
                     + usageNormalized * USAGE_WEIGHT
                     + conversionRate * CONVERSION_WEIGHT;

        return Math.round(score * 100.0) / 100.0;
    }

    @Override
    public ProductVersionDiffVO diffVersions(Long productId, Integer versionA, Integer versionB) {
        if (productId == null || versionA == null || versionB == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "产品 ID 与版本号不能为空");
        }
        ProductScriptVersion scriptA = versionRepository.findByProductIdAndVersionNumber(productId, versionA)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "版本 " + versionA + " 不存在"));
        ProductScriptVersion scriptB = versionRepository.findByProductIdAndVersionNumber(productId, versionB)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "版本 " + versionB + " 不存在"));
        return computeDiff(scriptA.getContent() != null ? scriptA.getContent() : "", scriptB.getContent() != null ? scriptB.getContent() : "");
    }

    private ProductVersionDiffVO computeDiff(String contentA, String contentB) {
        ProductVersionDiffVO diff = new ProductVersionDiffVO();
        Set<String> setA = new HashSet<>(Arrays.asList(contentA.split("\n")));
        Set<String> setB = new HashSet<>(Arrays.asList(contentB.split("\n")));
        setB.stream().filter(line -> !setA.contains(line)).forEach(diff.added::add);
        setA.stream().filter(line -> !setB.contains(line)).forEach(diff.removed::add);
        return diff;
    }

    // ===== 工具方法 =====

    /**
     * 将 ProductScriptVersion 实体转换为 VO
     *
     * @param entity 实体对象
     * @return VO 对象
     */
    private ProductScriptVersionVO convert(ProductScriptVersion entity) {
        if (entity == null) {
            return null;
        }

        return ProductScriptVersionVO.builder()
                .id(entity.getId())
                .productId(entity.getProductId())
                .scriptId(entity.getScriptId())
                .versionNumber(entity.getVersionNumber())
                .content(entity.getContent())
                .style(entity.getStyle())
                .effectivenessScore(entity.getEffectivenessScore())
                .usageCount(entity.getUsageCount())
                .conversionRate(entity.getConversionRate())
                .likesCount(entity.getLikesCount())
                .commentsCount(entity.getCommentsCount())
                .isActive(entity.getIsActive())
                .isRecommended(entity.getIsRecommended())
                .archived(entity.getArchived())
                .ownerId(entity.getOwnerId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * 将 ProductScriptSnapshot 实体转换为 VO
     *
     * @param entity 实体对象
     * @return VO 对象
     */
    private ProductScriptSnapshotVO convertSnapshot(ProductScriptSnapshot entity) {
        if (entity == null) {
            return null;
        }

        return ProductScriptSnapshotVO.builder()
                .id(entity.getId())
                .liveSessionId(entity.getLiveSessionId())
                .productScriptVersionId(entity.getProductScriptVersionId())
                .contentSnapshot(entity.getContentSnapshot())
                .referencedAt(entity.getReferencedAt())
                .ownerId(entity.getOwnerId())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
