package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvScriptTemplate;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvScriptTemplateRepository;
import cn.gaifan.douyinOperations.module.shortvideo.service.SvScriptTemplateService;
import cn.gaifan.douyinOperations.module.shortvideo.vo.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import jakarta.persistence.criteria.Predicate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SvScriptTemplateServiceImpl implements SvScriptTemplateService {

    private static final Set<String> SORTABLE = Set.of("id", "useCount", "status", "createTime", "updateTime");

    @Resource
    private SvScriptTemplateRepository templateRepository;

    private static final String TEMPLATE_TYPE_SYSTEM = "system";

    @Override
    public PageResultVO<SvScriptTemplateVO> search(SvScriptTemplateSearchVO vo, Long ownerId) {
        if (ownerId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        vo.validateParams();
        String sortName = SORTABLE.contains(vo.getSortName()) ? vo.getSortName() : "id";
        Pageable pageable = PageRequest.of(vo.getPage(), vo.getRows(),
                Sort.by("desc".equalsIgnoreCase(vo.getSortOrder()) ? Sort.Direction.DESC : Sort.Direction.ASC, sortName));

        Specification<SvScriptTemplate> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("deleted"), 0));
            // 数据隔离：仅返回 system 或当前用户模板
            predicates.add(cb.or(
                    cb.and(cb.equal(root.get("templateType"), TEMPLATE_TYPE_SYSTEM)),
                    cb.equal(root.get("ownerId"), ownerId)
            ));
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
            if (vo.getCategoryId() != null) predicates.add(cb.equal(root.get("categoryId"), vo.getCategoryId()));
            if (vo.getStatus() != null) predicates.add(cb.equal(root.get("status"), vo.getStatus()));
            if (vo.getOwnerId() != null) predicates.add(cb.equal(root.get("ownerId"), vo.getOwnerId()));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<SvScriptTemplate> page = templateRepository.findAll(spec, pageable);
        return PageResultVO.of(page.getTotalElements(),
                page.getContent().stream().map(this::toVO).collect(Collectors.toList()),
                vo.getPage(), vo.getRows());
    }

    @Override
    public SvScriptTemplateVO getById(Long id, Long ownerId) {
        if (ownerId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        if (id == null || id <= 0) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "模板 ID 无效");
        SvScriptTemplate entity = templateRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCRIPT_TEMPLATE_NOT_FOUND, "模板不存在"));
        if (!TEMPLATE_TYPE_SYSTEM.equals(entity.getTemplateType()) && (entity.getOwnerId() == null || !entity.getOwnerId().equals(ownerId)))
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限访问该模板");
        return toVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long save(SvScriptTemplateSaveVO vo, Long ownerId) {
        if (ownerId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        SvScriptTemplate entity;
        if (vo.getId() != null && vo.getId() > 0) {
            entity = templateRepository.findByIdAndDeleted(vo.getId(), 0)
                    .orElseThrow(() -> new BusinessException(ErrorCode.SCRIPT_TEMPLATE_NOT_FOUND, "模板不存在"));
            if (TEMPLATE_TYPE_SYSTEM.equals(entity.getTemplateType()))
                throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED, "系统模板不可修改");
            if (entity.getOwnerId() == null || !entity.getOwnerId().equals(ownerId))
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权限修改该模板");
        } else {
            entity = new SvScriptTemplate();
            entity.setOwnerId(ownerId);
            entity.setTemplateType(vo.getTemplateType() != null ? vo.getTemplateType() : "user");
        }
        entity.setTemplateName(vo.getTemplateName());
        entity.setContent(vo.getContent());
        if (vo.getTemplateType() != null) entity.setTemplateType(vo.getTemplateType());
        if (vo.getScene() != null) entity.setScene(vo.getScene());
        if (vo.getCategoryId() != null) entity.setCategoryId(vo.getCategoryId());
        if (vo.getDescription() != null) entity.setDescription(vo.getDescription());
        if (vo.getDurationHint() != null) entity.setDurationHint(vo.getDurationHint());
        if (vo.getStatus() != null) entity.setStatus(vo.getStatus());
        return templateRepository.save(entity).getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, Long ownerId) {
        if (ownerId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        if (id == null || id <= 0) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "模板 ID 无效");
        SvScriptTemplate entity = templateRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCRIPT_TEMPLATE_NOT_FOUND, "模板不存在"));
        if (TEMPLATE_TYPE_SYSTEM.equals(entity.getTemplateType()))
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED, "系统模板不可删除");
        if (entity.getOwnerId() == null || !entity.getOwnerId().equals(ownerId))
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限删除该模板");
        entity.setDeleted(1);
        templateRepository.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void incrementUseCount(Long id, Long ownerId) {
        if (ownerId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        if (id == null || id <= 0) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "模板 ID 无效");
        SvScriptTemplate entity = templateRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCRIPT_TEMPLATE_NOT_FOUND, "模板不存在"));
        if (!TEMPLATE_TYPE_SYSTEM.equals(entity.getTemplateType()) && (entity.getOwnerId() == null || !entity.getOwnerId().equals(ownerId)))
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限操作该模板");
        templateRepository.incrementUseCount(id);
    }

    @Override
    public List<SvScriptTemplateVO> listByScene(String scene, Long ownerId) {
        if (ownerId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        List<SvScriptTemplate> all = templateRepository.findBySceneAndStatusAndDeleted(scene, 1, 0);
        return all.stream()
                .filter(t -> TEMPLATE_TYPE_SYSTEM.equals(t.getTemplateType()) || (t.getOwnerId() != null && t.getOwnerId().equals(ownerId)))
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    private SvScriptTemplateVO toVO(SvScriptTemplate e) {
        SvScriptTemplateVO vo = new SvScriptTemplateVO();
        vo.setId(e.getId());
        vo.setOwnerId(e.getOwnerId());
        vo.setTemplateName(e.getTemplateName());
        vo.setTemplateType(e.getTemplateType());
        vo.setScene(e.getScene());
        vo.setCategoryId(e.getCategoryId());
        vo.setContent(e.getContent());
        vo.setDescription(e.getDescription());
        vo.setDurationHint(e.getDurationHint());
        vo.setUseCount(e.getUseCount());
        vo.setStatus(e.getStatus());
        vo.setCreateTime(e.getCreateTime());
        vo.setUpdateTime(e.getUpdateTime());
        return vo;
    }
}
