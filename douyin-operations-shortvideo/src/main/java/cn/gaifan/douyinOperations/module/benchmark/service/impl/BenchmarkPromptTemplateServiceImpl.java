package cn.gaifan.douyinOperations.module.benchmark.service.impl;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.benchmark.entity.BenchmarkPromptTemplate;
import cn.gaifan.douyinOperations.module.benchmark.repository.BenchmarkPromptTemplateRepository;
import cn.gaifan.douyinOperations.module.benchmark.service.BenchmarkPromptTemplateService;
import cn.gaifan.douyinOperations.module.benchmark.vo.BenchmarkPromptTemplateSearchVO;
import cn.gaifan.douyinOperations.module.benchmark.vo.BenchmarkPromptTemplateSaveVO;
import cn.gaifan.douyinOperations.module.benchmark.vo.BenchmarkPromptTemplateVO;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Prompt 模板管理服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BenchmarkPromptTemplateServiceImpl implements BenchmarkPromptTemplateService {

    private final BenchmarkPromptTemplateRepository templateRepository;

    @Override
    @Cacheable(value = "benchmark:prompt:template:list", key = "#searchVO.hashCode() + '_' + #ownerId")
    public PageResultVO<BenchmarkPromptTemplateVO> search(BenchmarkPromptTemplateSearchVO searchVO, Long ownerId) {
        searchVO.validateParams();

        log.debug("查询 Prompt 模板列表: ownerId={}, templateName={}, sceneType={}, industry={}",
                ownerId, searchVO.getTemplateName(), searchVO.getSceneType(), searchVO.getIndustry());

        Specification<BenchmarkPromptTemplate> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 数据隔离
            predicates.add(cb.equal(root.get("ownerId"), ownerId));

            // 模板名称模糊查询
            if (StringUtils.hasText(searchVO.getTemplateName())) {
                predicates.add(cb.like(root.get("templateName"), "%" + searchVO.getTemplateName() + "%"));
            }

            // 模板编码精确查询
            if (StringUtils.hasText(searchVO.getTemplateCode())) {
                predicates.add(cb.equal(root.get("templateCode"), searchVO.getTemplateCode()));
            }

            // 场景类型过滤
            if (StringUtils.hasText(searchVO.getSceneType())) {
                predicates.add(cb.equal(root.get("sceneType"), searchVO.getSceneType()));
            }

            // 行业分类过滤
            if (StringUtils.hasText(searchVO.getIndustry())) {
                predicates.add(cb.equal(root.get("industry"), searchVO.getIndustry()));
            }

            // 激活状态过滤
            if (searchVO.getIsActive() != null) {
                predicates.add(cb.equal(root.get("isActive"), searchVO.getIsActive()));
            }

            // 最小平均评分过滤
            if (searchVO.getMinAvgScore() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("avgScore"), BigDecimal.valueOf(searchVO.getMinAvgScore())));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Sort sort = Sort.by(Sort.Direction.DESC, "updateTime");
        Pageable pageable = PageRequest.of(searchVO.getPage(), searchVO.getRows(), sort);
        Page<BenchmarkPromptTemplate> page = templateRepository.findAll(spec, pageable);

        List<BenchmarkPromptTemplateVO> voList = page.getContent().stream()
                .map(this::entityToVO)
                .toList();

        log.debug("查询 Prompt 模板列表完成: total={}, page={}", page.getTotalElements(), searchVO.getPage());

        return new PageResultVO<>(page.getTotalElements(), voList, searchVO.getPage(), searchVO.getRows());
    }

    @Override
    public BenchmarkPromptTemplateVO getById(Long id, Long ownerId) {
        log.debug("查询 Prompt 模板详情: id={}, ownerId={}", id, ownerId);

        BenchmarkPromptTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Prompt 模板不存在"));

        if (!template.getOwnerId().equals(ownerId)) {
            log.warn("无权访问 Prompt 模板: id={}, ownerId={}, templateOwnerId={}", id, ownerId, template.getOwnerId());
            throw new RuntimeException("无权访问该 Prompt 模板");
        }

        return entityToVO(template);
    }

    @Override
    @Transactional
    @CacheEvict(value = "benchmark:prompt:template:list", allEntries = true)
    public BenchmarkPromptTemplateVO save(BenchmarkPromptTemplateSaveVO saveVO, Long ownerId) {
        log.info("保存 Prompt 模板: id={}, templateCode={}, ownerId={}", saveVO.getId(), saveVO.getTemplateCode(), ownerId);

        BenchmarkPromptTemplate template;

        if (saveVO.getId() != null) {
            // 更新
            template = templateRepository.findById(saveVO.getId())
                    .orElseThrow(() -> new RuntimeException("Prompt 模板不存在"));

            if (!template.getOwnerId().equals(ownerId)) {
                throw new RuntimeException("无权修改该 Prompt 模板");
            }

            // 检查模板编码是否重复（排除自己）
            templateRepository.findByTemplateCodeAndDeleted(saveVO.getTemplateCode(), 0)
                    .ifPresent(existing -> {
                        if (!existing.getId().equals(saveVO.getId())) {
                            throw new RuntimeException("模板编码已存在");
                        }
                    });

            template.setVersion(template.getVersion() + 1);
        } else {
            // 新增
            templateRepository.findByTemplateCodeAndDeleted(saveVO.getTemplateCode(), 0)
                    .ifPresent(existing -> {
                        throw new RuntimeException("模板编码已存在");
                    });

            template = new BenchmarkPromptTemplate();
            template.setOwnerId(ownerId);
        }

        // 复制属性
        BeanUtils.copyProperties(saveVO, template, "id", "ownerId", "version", "usageCount", "avgScore", "createTime", "updateTime", "deleted");

        template = templateRepository.save(template);

        log.info("Prompt 模板保存成功: id={}, templateCode={}", template.getId(), template.getTemplateCode());

        return entityToVO(template);
    }

    @Override
    @Transactional
    @CacheEvict(value = "benchmark:prompt:template:list", allEntries = true)
    public void delete(Long id, Long ownerId) {
        log.info("删除 Prompt 模板: id={}, ownerId={}", id, ownerId);

        BenchmarkPromptTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Prompt 模板不存在"));

        if (!template.getOwnerId().equals(ownerId)) {
            throw new RuntimeException("无权删除该 Prompt 模板");
        }

        template.setDeleted(1);
        templateRepository.save(template);

        log.info("Prompt 模板删除成功: id={}", id);
    }

    @Override
    @Transactional
    @CacheEvict(value = "benchmark:prompt:template:list", allEntries = true)
    public void toggleActive(Long id, Boolean isActive, Long ownerId) {
        log.info("切换 Prompt 模板激活状态: id={}, isActive={}, ownerId={}", id, isActive, ownerId);

        BenchmarkPromptTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Prompt 模板不存在"));

        if (!template.getOwnerId().equals(ownerId)) {
            throw new RuntimeException("无权修改该 Prompt 模板");
        }

        template.setIsActive(isActive);
        templateRepository.save(template);

        log.info("Prompt 模板激活状态切换成功: id={}, isActive={}", id, isActive);
    }

    @Override
    public List<BenchmarkPromptTemplateVO> getActiveTemplatesByScene(String sceneType, Long ownerId) {
        log.debug("查询场景类型的激活模板: sceneType={}, ownerId={}", sceneType, ownerId);

        List<BenchmarkPromptTemplate> templates = templateRepository
                .findByOwnerIdAndSceneTypeAndIsActiveAndDeleted(ownerId, sceneType, true, 0);

        return templates.stream()
                .map(this::entityToVO)
                .toList();
    }

    @Override
    public List<BenchmarkPromptTemplateVO> getActiveTemplatesByIndustry(String industry, Long ownerId) {
        log.debug("查询行业的激活模板: industry={}, ownerId={}", industry, ownerId);

        List<BenchmarkPromptTemplate> templates = templateRepository
                .findByOwnerIdAndIndustryAndIsActiveAndDeleted(ownerId, industry, true, 0);

        return templates.stream()
                .map(this::entityToVO)
                .toList();
    }

    @Override
    public BenchmarkPromptTemplateVO getByTemplateCode(String templateCode, Long ownerId) {
        log.debug("根据模板编码查询: templateCode={}, ownerId={}", templateCode, ownerId);

        BenchmarkPromptTemplate template = templateRepository.findByTemplateCodeAndDeleted(templateCode, 0)
                .orElseThrow(() -> new RuntimeException("Prompt 模板不存在"));

        if (!template.getOwnerId().equals(ownerId)) {
            throw new RuntimeException("无权访问该 Prompt 模板");
        }

        return entityToVO(template);
    }

    @Override
    @Transactional
    public void updateUsageStats(Long templateId, Double rating) {
        log.debug("更新模板使用统计: templateId={}, rating={}", templateId, rating);

        templateRepository.findById(templateId).ifPresent(template -> {
            // 更新使用次数
            template.setUsageCount(template.getUsageCount() + 1);

            // 更新平均评分（增量计算）
            if (rating != null) {
                BigDecimal currentAvg = template.getAvgScore();
                int currentCount = template.getUsageCount();
                BigDecimal newAvg = currentAvg
                        .multiply(BigDecimal.valueOf(currentCount - 1))
                        .add(BigDecimal.valueOf(rating))
                        .divide(BigDecimal.valueOf(currentCount), 2, BigDecimal.ROUND_HALF_UP);
                template.setAvgScore(newAvg);
            }

            templateRepository.save(template);
        });
    }

    private BenchmarkPromptTemplateVO entityToVO(BenchmarkPromptTemplate entity) {
        BenchmarkPromptTemplateVO vo = new BenchmarkPromptTemplateVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
