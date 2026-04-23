package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvRemakeTemplate;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvViralVideo;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvRemakeTemplateRepository;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvViralVideoRepository;
import cn.gaifan.douyinOperations.module.shortvideo.service.RemakeTemplateService;
import cn.gaifan.douyinOperations.module.shortvideo.vo.RemakeTemplateSearchVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.RemakeTemplateSaveVO;
import jakarta.annotation.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 二创模板服务实现（Phase 5）
 */
@Service
public class RemakeTemplateServiceImpl implements RemakeTemplateService {

    @Resource
    private SvRemakeTemplateRepository repository;
    @Resource
    private SvViralVideoRepository viralVideoRepository;

    @Override
    @Transactional
    public SvRemakeTemplate save(RemakeTemplateSaveVO vo, Long ownerId) {
        SvRemakeTemplate entity;
        if (vo.getId() != null && vo.getId() > 0) {
            entity = repository.findById(vo.getId())
                    .filter(e -> ownerId.equals(e.getOwnerId()) || e.getOwnerId() == 0)
                    .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "模板不存在"));
        } else {
            entity = new SvRemakeTemplate();
            entity.setOwnerId(ownerId);
        }
        entity.setTemplateName(vo.getTemplateName());
        entity.setRemakeType(vo.getRemakeType());
        entity.setSourceViralId(vo.getSourceViralId());
        entity.setStructureTemplate(vo.getStructureTemplate());
        entity.setEmotionCurve(vo.getEmotionCurve());
        entity.setBgmStyle(vo.getBgmStyle());
        entity.setDurationRange(vo.getDurationRange());
        entity.setAdaptationGuide(vo.getAdaptationGuide());
        entity.setVariableSlots(vo.getVariableSlots());
        return repository.save(entity);
    }

    @Override
    @Transactional
    public void delete(Long id, Long ownerId) {
        SvRemakeTemplate e = repository.findById(id)
                .filter(x -> ownerId.equals(x.getOwnerId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "模板不存在"));
        e.setDeleted(1);
        repository.save(e);
    }

    @Override
    public PageResultVO<SvRemakeTemplate> search(RemakeTemplateSearchVO vo, Long ownerId) {
        final RemakeTemplateSearchVO q = vo != null ? vo : new RemakeTemplateSearchVO();
        Specification<SvRemakeTemplate> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> preds = new ArrayList<>();
            preds.add(cb.equal(root.get("deleted"), 0));
            preds.add(root.get("ownerId").in(List.of(0L, ownerId)));
            if (q.getRemakeType() != null && !q.getRemakeType().isBlank()) {
                preds.add(cb.equal(root.get("remakeType"), q.getRemakeType()));
            }
            if (q.getTemplateName() != null && !q.getTemplateName().isBlank()) {
                preds.add(cb.like(root.get("templateName"), "%" + q.getTemplateName() + "%"));
            }
            return cb.and(preds.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        q.validateParams();
        String sortName = q.getSortName() != null ? q.getSortName() : "id";
        Sort sort = "asc".equalsIgnoreCase(q.getSortOrder()) ? Sort.by(Sort.Direction.ASC, sortName) : Sort.by(Sort.Direction.DESC, sortName);
        PageRequest pr = PageRequest.of(q.getPage(), q.getRows(), sort);
        Page<SvRemakeTemplate> page = repository.findAll(spec, pr);
        return PageResultVO.of(page.getTotalElements(), page.getContent(), q.getPage(), q.getRows());
    }

    @Override
    public SvRemakeTemplate getById(Long id) {
        return repository.findById(id).filter(e -> e.getDeleted() == 0).orElse(null);
    }

    @Override
    @Transactional
    public SvRemakeTemplate createFromViralAnalysis(Long viralVideoId, String remakeType, Long ownerId) {
        SvViralVideo viral = viralVideoRepository.findById(viralVideoId)
                .filter(v -> ownerId.equals(v.getOwnerId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "爆款视频不存在"));
        SvRemakeTemplate t = new SvRemakeTemplate();
        t.setOwnerId(ownerId);
        t.setTemplateName("基于「" + (viral.getTitle() != null ? viral.getTitle().substring(0, Math.min(20, viral.getTitle().length())) : viral.getId()) + "」的二创");
        t.setRemakeType(remakeType != null ? remakeType : "form_copy");
        t.setSourceViralId(viralVideoId);
        t.setStructureTemplate(Map.of("segments", List.of(Map.of("type", "hook", "desc", "开场"), Map.of("type", "content", "desc", "内容"), Map.of("type", "cta", "desc", "转化"))));
        t.setAdaptationGuide("基于爆款分析结果生成，请根据实际产品调整");
        t.setVariableSlots(List.of(Map.of("slot", "productName", "desc", "产品名称")));
        return repository.save(t);
    }

    @Override
    public String generateFromTemplate(Long templateId, Map<String, String> variables, Long ownerId) {
        SvRemakeTemplate t = repository.findById(templateId)
                .filter(e -> e.getDeleted() == 0 && (ownerId.equals(e.getOwnerId()) || e.getOwnerId() == 0))
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "模板不存在"));
        StringBuilder sb = new StringBuilder();
        sb.append("【二创脚本】").append(t.getTemplateName()).append("\n\n");
        if (t.getAdaptationGuide() != null) sb.append(t.getAdaptationGuide()).append("\n\n");
        if (variables != null && !variables.isEmpty()) {
            sb.append("变量填充：\n");
            variables.forEach((k, v) -> sb.append("- ").append(k).append(": ").append(v).append("\n"));
        }
        return sb.toString();
    }
}
