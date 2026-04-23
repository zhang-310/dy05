package cn.gaifan.douyinOperations.module.copy.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.copy.entity.CopyLibrary;
import cn.gaifan.douyinOperations.module.copy.repository.CopyLibraryRepository;
import cn.gaifan.douyinOperations.module.copy.service.CopyLibraryService;
import cn.gaifan.douyinOperations.module.copy.vo.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import jakarta.persistence.criteria.Predicate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CopyLibraryServiceImpl implements CopyLibraryService {

    private static final Set<String> SORTABLE_FIELDS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("id", "userId", "useCount", "rating", "status", "createTime", "updateTime")));

    @Resource
    private CopyLibraryRepository copyLibraryRepository;

    @Override
    public PageResultVO<CopyLibraryVO> search(CopyLibrarySearchVO vo) {
        vo.validateParams();
        String sortName = SORTABLE_FIELDS.contains(vo.getSortName()) ? vo.getSortName() : "id";
        Pageable pageable = PageRequest.of(vo.getPage(), vo.getRows(),
                Sort.by("desc".equalsIgnoreCase(vo.getSortOrder()) ? Sort.Direction.DESC : Sort.Direction.ASC, sortName));

        Specification<CopyLibrary> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("deleted"), 0));

            if (vo.getUserId() != null && vo.getUserId() > 0) {
                predicates.add(cb.equal(root.get("userId"), vo.getUserId()));
            } else if (vo.getUserIds() != null && !vo.getUserIds().isEmpty()) {
                predicates.add(root.get("userId").in(vo.getUserIds()));
            }
            if (vo.getKeyword() != null && !vo.getKeyword().trim().isEmpty()) {
                String kw = "%" + vo.getKeyword().trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("title"), kw),
                        cb.like(root.get("content"), kw)
                ));
            }
            if (vo.getTitle() != null && !vo.getTitle().trim().isEmpty()) {
                predicates.add(cb.like(root.get("title"), "%" + vo.getTitle().trim() + "%"));
            }
            if (vo.getCategory() != null && !vo.getCategory().trim().isEmpty()) {
                predicates.add(cb.equal(root.get("category"), vo.getCategory().trim()));
            }
            if (vo.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), vo.getStatus()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<CopyLibrary> page = copyLibraryRepository.findAll(spec, pageable);
        List<CopyLibraryVO> list = page.getContent().stream().map(this::toVO).collect(Collectors.toList());
        return PageResultVO.of(page.getTotalElements(), list, vo.getPage(), vo.getRows());
    }

    @Override
    public CopyLibraryVO getById(Long id) {
        if (id == null || id <= 0) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "文案 ID 无效");
        CopyLibrary entity = copyLibraryRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "文案不存在"));
        return toVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long save(CopyLibrarySaveVO vo) {
        CopyLibrary entity;
        if (vo.getId() != null && vo.getId() > 0) {
            entity = copyLibraryRepository.findByIdAndDeleted(vo.getId(), 0)
                    .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "文案不存在"));
        } else {
            entity = new CopyLibrary();
            entity.setUserId(vo.getUserId());
        }
        entity.setTitle(vo.getTitle());
        entity.setContent(vo.getContent());
        // 自动计算字数
        entity.setWordCount(vo.getContent() != null ? vo.getContent().length() : 0);
        if (vo.getCategory() != null) entity.setCategory(vo.getCategory());
        if (vo.getTags() != null) entity.setTags(vo.getTags());
        if (vo.getRating() != null) entity.setRating(vo.getRating());
        if (vo.getStatus() != null) entity.setStatus(vo.getStatus());
        entity = copyLibraryRepository.save(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (id == null || id <= 0) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "文案 ID 无效");
        CopyLibrary entity = copyLibraryRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "文案不存在"));
        entity.setDeleted(1);
        copyLibraryRepository.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        if (id == null || id <= 0) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "文案 ID 无效");
        copyLibraryRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "文案不存在"));
        copyLibraryRepository.updateStatus(id, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void incrementUseCount(Long id) {
        if (id == null || id <= 0) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "文案 ID 无效");
        copyLibraryRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "文案不存在"));
        copyLibraryRepository.incrementUseCount(id);
    }

    private CopyLibraryVO toVO(CopyLibrary e) {
        CopyLibraryVO vo = new CopyLibraryVO();
        vo.setId(e.getId());
        vo.setUserId(e.getUserId());
        vo.setTitle(e.getTitle());
        vo.setContent(e.getContent());
        vo.setCategory(e.getCategory());
        vo.setTags(e.getTags());
        vo.setWordCount(e.getWordCount());
        vo.setUseCount(e.getUseCount());
        vo.setRating(e.getRating());
        vo.setStatus(e.getStatus());
        vo.setCreateTime(e.getCreateTime());
        vo.setUpdateTime(e.getUpdateTime());
        return vo;
    }
}
