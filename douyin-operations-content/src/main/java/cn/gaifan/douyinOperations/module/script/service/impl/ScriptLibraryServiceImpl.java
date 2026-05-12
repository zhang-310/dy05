package cn.gaifan.douyinOperations.module.script.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.script.entity.ScriptLibrary;
import cn.gaifan.douyinOperations.module.script.repository.ScriptLibraryRepository;
import cn.gaifan.douyinOperations.module.script.service.ScriptLibraryService;
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
public class ScriptLibraryServiceImpl implements ScriptLibraryService {

    private static final Set<String> SORTABLE = Set.of("id", "userId", "useCount", "status", "createTime", "updateTime");

    @Resource
    private ScriptLibraryRepository scriptLibraryRepository;

    @Override
    public PageResultVO<ScriptVO> search(ScriptSearchVO vo) {
        vo.validateParams();
        String sortName = SORTABLE.contains(vo.getSortName()) ? vo.getSortName() : "id";
        Pageable pageable = PageRequest.of(vo.getPage(), vo.getRows(),
                Sort.by("desc".equalsIgnoreCase(vo.getSortOrder()) ? Sort.Direction.DESC : Sort.Direction.ASC, sortName));

        Specification<ScriptLibrary> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("deleted"), 0));
            if (vo.getUserId() != null && vo.getUserId() > 0) {
                predicates.add(cb.equal(root.get("userId"), vo.getUserId()));
            } else if (vo.getUserIds() != null && !vo.getUserIds().isEmpty()) {
                predicates.add(root.get("userId").in(vo.getUserIds()));
            }
            if (vo.getKeyword() != null && !vo.getKeyword().isBlank()) {
                String kw = "%" + vo.getKeyword().trim() + "%";
                predicates.add(cb.or(cb.like(root.get("title"), kw), cb.like(root.get("content"), kw)));
            }
            if (vo.getCategory() != null && !vo.getCategory().isBlank()) {
                predicates.add(cb.equal(root.get("category"), vo.getCategory().trim()));
            }
            if (vo.getSource() != null && !vo.getSource().isBlank()) {
                predicates.add(cb.equal(root.get("source"), vo.getSource().trim()));
            }
            if (vo.getStatus() != null) predicates.add(cb.equal(root.get("status"), vo.getStatus()));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<ScriptLibrary> page = scriptLibraryRepository.findAll(spec, pageable);
        return PageResultVO.of(page.getTotalElements(),
                page.getContent().stream().map(this::toVO).collect(Collectors.toList()),
                vo.getPage(), vo.getRows());
    }

    @Override
    public ScriptVO getById(Long id, Long userId) {
        if (id == null || id <= 0) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "话术 ID 无效");
        ScriptLibrary entity = scriptLibraryRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "话术不存在"));
        // P1-6: IDOR 防护 - 校验所有权
        if (!entity.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问他人的话术");
        }
        return toVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long save(ScriptSaveVO vo) {
        ScriptLibrary entity;
        if (vo.getId() != null && vo.getId() > 0) {
            entity = scriptLibraryRepository.findByIdAndDeleted(vo.getId(), 0)
                    .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "话术不存在"));
        } else {
            entity = new ScriptLibrary();
            entity.setUserId(vo.getUserId());
        }
        entity.setTitle(vo.getTitle());
        if (vo.getContent() != null) entity.setContent(vo.getContent());
        if (vo.getCategory() != null) entity.setCategory(vo.getCategory());
        if (vo.getSource() != null && !vo.getSource().isBlank()) entity.setSource(vo.getSource().trim());
        else if (entity.getSource() == null) entity.setSource("manual");
        if (vo.getSourceId() != null) entity.setSourceId(vo.getSourceId());
        if (vo.getTags() != null) entity.setTags(vo.getTags());
        if (vo.getStatus() != null) entity.setStatus(vo.getStatus());
        return scriptLibraryRepository.save(entity).getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, Long userId) {
        if (id == null || id <= 0) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "话术 ID 无效");
        ScriptLibrary entity = scriptLibraryRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "话术不存在"));
        // P1-6: IDOR 防护 - 校验所有权
        if (!entity.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权删除他人的话术");
        }
        entity.setDeleted(1);
        scriptLibraryRepository.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void incrementUseCount(Long id) {
        if (id == null || id <= 0) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "话术 ID 无效");
        scriptLibraryRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "话术不存在"));
        scriptLibraryRepository.incrementUseCount(id);
    }

    @Override
    public List<String> listCategories() {
        return scriptLibraryRepository.findDistinctCategories();
    }

    private ScriptVO toVO(ScriptLibrary e) {
        ScriptVO vo = new ScriptVO();
        vo.setId(e.getId());
        vo.setUserId(e.getUserId());
        vo.setTitle(e.getTitle());
        vo.setContent(e.getContent());
        vo.setCategory(e.getCategory());
        vo.setSource(e.getSource());
        vo.setSourceId(e.getSourceId());
        vo.setTags(e.getTags());
        vo.setUseCount(e.getUseCount());
        vo.setStatus(e.getStatus());
        vo.setCreateTime(e.getCreateTime());
        vo.setUpdateTime(e.getUpdateTime());
        return vo;
    }
}
