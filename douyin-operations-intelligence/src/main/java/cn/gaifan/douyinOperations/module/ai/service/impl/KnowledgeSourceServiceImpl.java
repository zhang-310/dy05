package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.ai.entity.AiKnowledgeSource;
import cn.gaifan.douyinOperations.module.ai.repository.AiKnowledgeSourceRepository;
import cn.gaifan.douyinOperations.module.ai.service.KnowledgeSourceService;
import cn.gaifan.douyinOperations.module.ai.vo.KnowledgeSourceSearchVO;
import cn.gaifan.douyinOperations.module.ai.vo.KnowledgeSourceSaveVO;
import jakarta.annotation.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Service
public class KnowledgeSourceServiceImpl implements KnowledgeSourceService {

    @Resource
    private AiKnowledgeSourceRepository repository;

    @Override
    public PageResultVO<AiKnowledgeSource> search(KnowledgeSourceSearchVO vo) {
        int page = vo.getPage() != null ? vo.getPage() : 0;
        int rows = vo.getRows() != null ? Math.min(vo.getRows(), 100) : 20;
        Sort sort = Sort.by(Sort.Direction.DESC, "createTime");
        PageRequest pr = PageRequest.of(page, rows, sort);

        Specification<AiKnowledgeSource> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> preds = new ArrayList<>();
            preds.add(cb.equal(root.get("deleted"), 0));
            String nameLike = (vo.getSourceName() != null && !vo.getSourceName().isBlank())
                ? vo.getSourceName().trim()
                : (vo.getKeyword() != null && !vo.getKeyword().isBlank() ? vo.getKeyword().trim() : null);
            if (nameLike != null) {
                preds.add(cb.like(root.get("sourceName"), "%" + nameLike + "%"));
            }
            if (vo.getSourceType() != null && !vo.getSourceType().isBlank()) {
                preds.add(cb.equal(root.get("sourceType"), vo.getSourceType().trim()));
            }
            if (vo.getStatus() != null) {
                preds.add(cb.equal(root.get("status"), vo.getStatus()));
            }
            return cb.and(preds.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<AiKnowledgeSource> p = repository.findAll(spec, pr);
        PageResultVO<AiKnowledgeSource> result = new PageResultVO<>();
        result.setList(p.getContent());
        result.setTotal(p.getTotalElements());
        result.setPageNum(page);
        result.setPageSize(rows);
        return result;
    }

    @Override
    public AiKnowledgeSource getById(Long id) {
        return repository.findById(id)
                .filter(e -> e.getDeleted() == 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "知识源不存在"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long save(KnowledgeSourceSaveVO vo) {
        AiKnowledgeSource entity;
        if (vo.getId() != null && vo.getId() > 0) {
            entity = repository.findById(vo.getId())
                    .filter(e -> e.getDeleted() == 0)
                    .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "知识源不存在"));
        } else {
            if (repository.existsBySourcePathAndDeleted(vo.getSourcePath().trim(), 0)) {
                throw new BusinessException(ErrorCode.DATA_ALREADY_EXISTS, "该路径已存在");
            }
            entity = new AiKnowledgeSource();
        }
        entity.setSourceName(vo.getSourceName().trim());
        entity.setSourcePath(vo.getSourcePath().trim());
        entity.setSourceType(vo.getSourceType() != null && !vo.getSourceType().isBlank() ? vo.getSourceType().trim() : "local");
        if (vo.getStatus() != null) entity.setStatus(vo.getStatus());
        return repository.save(entity).getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        repository.findById(id).ifPresent(e -> {
            e.setDeleted(1);
            repository.save(e);
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateIndexStats(Long id, int fileCount, int indexCount) {
        repository.findById(id).filter(e -> e.getDeleted() == 0).ifPresent(e -> {
            e.setFileCount(fileCount);
            e.setIndexCount(indexCount);
            e.setLastIndexTime(new Timestamp(System.currentTimeMillis()));
            repository.save(e);
        });
    }
}
