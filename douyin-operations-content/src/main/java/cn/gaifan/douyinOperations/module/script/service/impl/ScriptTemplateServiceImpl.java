package cn.gaifan.douyinOperations.module.script.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.script.entity.ScriptTemplate;
import cn.gaifan.douyinOperations.module.script.repository.ScriptTemplateRepository;
import cn.gaifan.douyinOperations.module.script.service.ScriptTemplateService;
import cn.gaifan.douyinOperations.module.script.vo.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import jakarta.persistence.criteria.Predicate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ScriptTemplateServiceImpl implements ScriptTemplateService {

    private static final Set<String> SORTABLE = Set.of("id", "useCount", "status", "createTime", "updateTime");

    @Resource
    private ScriptTemplateRepository templateRepository;

    @Override
    public PageResultVO<ScriptTemplateVO> search(ScriptTemplateSearchVO vo) {
        vo.validateParams();
        String sortName = SORTABLE.contains(vo.getSortName()) ? vo.getSortName() : "id";
        Pageable pageable = PageRequest.of(vo.getPage(), vo.getRows(),
                Sort.by("desc".equalsIgnoreCase(vo.getSortOrder()) ? Sort.Direction.DESC : Sort.Direction.ASC, sortName));

        Specification<ScriptTemplate> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("deleted"), 0));
            if (vo.getKeyword() != null && !vo.getKeyword().isBlank()) {
                String kw = "%" + vo.getKeyword().trim() + "%";
                predicates.add(cb.or(cb.like(root.get("templateName"), kw), cb.like(root.get("content"), kw)));
            }
            if (vo.getTemplateType() != null && !vo.getTemplateType().isBlank()) {
                predicates.add(cb.equal(root.get("templateType"), vo.getTemplateType().trim()));
            }
            if (vo.getScene() != null && !vo.getScene().isBlank()) {
                predicates.add(cb.equal(root.get("scene"), vo.getScene().trim()));
            }
            if (vo.getStatus() != null) predicates.add(cb.equal(root.get("status"), vo.getStatus()));
            if (vo.getUserId() != null) predicates.add(cb.equal(root.get("userId"), vo.getUserId()));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<ScriptTemplate> page = templateRepository.findAll(spec, pageable);
        return PageResultVO.of(page.getTotalElements(),
                page.getContent().stream().map(this::toVO).collect(Collectors.toList()),
                vo.getPage(), vo.getRows());
    }

    @Override
    public ScriptTemplateVO getById(Long id) {
        if (id == null || id <= 0) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "模板 ID 无效");
        return toVO(templateRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "模板不存在")));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long save(ScriptTemplateSaveVO vo) {
        ScriptTemplate entity;
        if (vo.getId() != null && vo.getId() > 0) {
            entity = templateRepository.findByIdAndDeleted(vo.getId(), 0)
                    .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "模板不存在"));
        } else {
            entity = new ScriptTemplate();
            if (vo.getUserId() != null) entity.setUserId(vo.getUserId());
        }
        entity.setTemplateName(vo.getTemplateName());
        entity.setContent(vo.getContent());
        if (vo.getTemplateType() != null) entity.setTemplateType(vo.getTemplateType());
        if (vo.getScene() != null) entity.setScene(vo.getScene());
        if (vo.getDescription() != null) entity.setDescription(vo.getDescription());
        if (vo.getStatus() != null) entity.setStatus(vo.getStatus());
        return templateRepository.save(entity).getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (id == null || id <= 0) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "模板 ID 无效");
        ScriptTemplate entity = templateRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "模板不存在"));
        entity.setDeleted(1);
        templateRepository.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void incrementUseCount(Long id) {
        if (id == null || id <= 0) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "模板 ID 无效");
        templateRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "模板不存在"));
        templateRepository.incrementUseCount(id);
    }

    @Override
    public List<ScriptTemplateVO> listByScene(String scene) {
        return templateRepository.findBySceneAndStatusAndDeleted(scene, 1, 0)
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long saveSystemTemplate(ScriptTemplateSaveVO vo) {
        ScriptTemplate entity;
        if (vo.getId() != null && vo.getId() > 0) {
            entity = templateRepository.findByIdAndDeleted(vo.getId(), 0)
                    .orElseThrow(() -> new BusinessException(ErrorCode.TEMPLATE_NOT_FOUND, "模板不存在"));
            if (!"system".equals(entity.getTemplateType())) {
                throw new BusinessException(ErrorCode.TEMPLATE_SYSTEM_FORBIDDEN, "只能编辑系统模板");
            }
        } else {
            entity = new ScriptTemplate();
            entity.setTemplateType("system");
            entity.setUserId(null);
        }
        entity.setTemplateName(vo.getTemplateName());
        entity.setContent(vo.getContent());
        if (vo.getScene() != null) entity.setScene(vo.getScene());
        if (vo.getDescription() != null) entity.setDescription(vo.getDescription());
        if (vo.getStatus() != null) entity.setStatus(vo.getStatus());
        return templateRepository.save(entity).getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSystemTemplate(Long id) {
        if (id == null || id <= 0) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "模板 ID 无效");
        ScriptTemplate entity = templateRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEMPLATE_NOT_FOUND, "模板不存在"));
        if (!"system".equals(entity.getTemplateType())) {
            throw new BusinessException(ErrorCode.TEMPLATE_SYSTEM_FORBIDDEN, "只能删除系统模板");
        }
        entity.setDeleted(1);
        templateRepository.save(entity);
    }

    private ScriptTemplateVO toVO(ScriptTemplate e) {
        ScriptTemplateVO vo = new ScriptTemplateVO();
        vo.setId(e.getId());
        vo.setTemplateName(e.getTemplateName());
        vo.setTemplateType(e.getTemplateType());
        vo.setScene(e.getScene());
        vo.setContent(e.getContent());
        vo.setDescription(e.getDescription());
        vo.setUserId(e.getUserId());
        vo.setUseCount(e.getUseCount());
        vo.setStatus(e.getStatus());
        vo.setCreateTime(e.getCreateTime());
        vo.setUpdateTime(e.getUpdateTime());
        return vo;
    }
}
