package cn.gaifan.douyinOperations.module.script.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.script.entity.UserViolationWord;
import cn.gaifan.douyinOperations.module.script.repository.UserViolationWordRepository;
import cn.gaifan.douyinOperations.module.script.service.UserViolationWordService;
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
public class UserViolationWordServiceImpl implements UserViolationWordService {

    private static final Set<String> SORTABLE = Set.of("id", "level", "status", "createTime", "updateTime");

    @Resource
    private UserViolationWordRepository uvwRepository;

    @Override
    public PageResultVO<UserViolationWordVO> search(UserViolationWordSearchVO vo) {
        vo.validateParams();
        String sortName = SORTABLE.contains(vo.getSortName()) ? vo.getSortName() : "id";
        Pageable pageable = PageRequest.of(vo.getPage(), vo.getRows(),
                Sort.by("desc".equalsIgnoreCase(vo.getSortOrder()) ? Sort.Direction.DESC : Sort.Direction.ASC, sortName));

        Specification<UserViolationWord> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("deleted"), 0));
            if (vo.getUserId() != null) predicates.add(cb.equal(root.get("userId"), vo.getUserId()));
            if (vo.getKeyword() != null && !vo.getKeyword().isBlank()) {
                String kw = "%" + vo.getKeyword().trim() + "%";
                predicates.add(cb.like(root.get("word"), kw));
            }
            if (vo.getLevel() != null) predicates.add(cb.equal(root.get("level"), vo.getLevel()));
            if (vo.getStatus() != null) predicates.add(cb.equal(root.get("status"), vo.getStatus()));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<UserViolationWord> page = uvwRepository.findAll(spec, pageable);
        return PageResultVO.of(page.getTotalElements(),
                page.getContent().stream().map(this::toVO).collect(Collectors.toList()),
                vo.getPage(), vo.getRows());
    }

    @Override
    public UserViolationWordVO getById(Long id) {
        if (id == null || id <= 0) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "违规词 ID 无效");
        return toVO(uvwRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "违规词不存在")));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long save(UserViolationWordSaveVO vo) {
        UserViolationWord entity;
        if (vo.getId() != null && vo.getId() > 0) {
            entity = uvwRepository.findByIdAndDeleted(vo.getId(), 0)
                    .orElseThrow(() -> new BusinessException(ErrorCode.VIOLATION_WORD_NOT_FOUND, "违规词不存在"));
        } else {
            // 检查同一用户下是否已存在相同违规词
            if (uvwRepository.existsByUserIdAndWordAndDeleted(vo.getUserId(), vo.getWord(), 0)) {
                throw new BusinessException(ErrorCode.VALIDATION_FAIL, "该违规词已存在");
            }
            entity = new UserViolationWord();
            entity.setUserId(vo.getUserId());
        }
        entity.setWord(vo.getWord());
        if (vo.getLevel() != null) entity.setLevel(vo.getLevel());
        if (vo.getReason() != null) entity.setReason(vo.getReason());
        if (vo.getReplacement() != null) entity.setReplacement(vo.getReplacement());
        if (vo.getScope() != null && !vo.getScope().isBlank()) entity.setScope(vo.getScope().trim());
        else if (entity.getScope() == null) entity.setScope("all");
        if (vo.getStatus() != null) entity.setStatus(vo.getStatus());
        return uvwRepository.save(entity).getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (id == null || id <= 0) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "违规词 ID 无效");
        UserViolationWord entity = uvwRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.VIOLATION_WORD_NOT_FOUND, "违规词不存在"));
        entity.setDeleted(1);
        uvwRepository.save(entity);
    }

    @Override
    public List<UserViolationWordVO> listActiveByUserId(Long userId) {
        return uvwRepository.findByUserIdAndStatusAndDeleted(userId, 1, 0)
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    private UserViolationWordVO toVO(UserViolationWord e) {
        UserViolationWordVO vo = new UserViolationWordVO();
        vo.setId(e.getId());
        vo.setUserId(e.getUserId());
        vo.setWord(e.getWord());
        vo.setLevel(e.getLevel());
        vo.setReason(e.getReason());
        vo.setReplacement(e.getReplacement());
        vo.setScope(e.getScope());
        vo.setStatus(e.getStatus());
        vo.setCreateTime(e.getCreateTime());
        vo.setUpdateTime(e.getUpdateTime());
        return vo;
    }
}
