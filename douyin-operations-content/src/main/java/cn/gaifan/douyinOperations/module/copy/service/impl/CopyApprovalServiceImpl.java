package cn.gaifan.douyinOperations.module.copy.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.copy.entity.CopyApproval;
import cn.gaifan.douyinOperations.module.copy.entity.CopyLibrary;
import cn.gaifan.douyinOperations.module.copy.repository.CopyApprovalRepository;
import cn.gaifan.douyinOperations.module.copy.repository.CopyLibraryRepository;
import cn.gaifan.douyinOperations.module.copy.service.CopyApprovalService;
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
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CopyApprovalServiceImpl implements CopyApprovalService {

    private static final Set<String> SORTABLE_FIELDS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("id", "copyId", "approvalStatus", "approvalTime", "createTime")));

    @Resource
    private CopyApprovalRepository copyApprovalRepository;

    @Resource
    private CopyLibraryRepository copyLibraryRepository;

    @Override
    public PageResultVO<CopyApprovalVO> search(CopyApprovalSearchVO vo) {
        vo.validateParams();
        String sortName = SORTABLE_FIELDS.contains(vo.getSortName()) ? vo.getSortName() : "id";
        Pageable pageable = PageRequest.of(vo.getPage(), vo.getRows(),
                Sort.by("desc".equalsIgnoreCase(vo.getSortOrder()) ? Sort.Direction.DESC : Sort.Direction.ASC, sortName));

        Specification<CopyApproval> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("deleted"), 0));
            if (vo.getCopyId() != null) predicates.add(cb.equal(root.get("copyId"), vo.getCopyId()));
            if (vo.getApprovalStatus() != null) predicates.add(cb.equal(root.get("approvalStatus"), vo.getApprovalStatus()));
            if (vo.getUserId() != null) predicates.add(cb.equal(root.get("userId"), vo.getUserId()));
            if (vo.getKeyword() != null && !vo.getKeyword().trim().isEmpty()) {
                List<Long> copyIds = copyLibraryRepository.findIdsByTitleContaining("%" + vo.getKeyword().trim() + "%");
                if (copyIds.isEmpty()) predicates.add(cb.equal(root.get("id"), -1L));
                else predicates.add(root.get("copyId").in(copyIds));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<CopyApproval> page = copyApprovalRepository.findAll(spec, pageable);
        List<CopyApproval> entities = page.getContent();
        Map<Long, CopyLibrary> libraryMap = new java.util.HashMap<>();
        if (!entities.isEmpty()) {
            List<Long> copyIds = entities.stream().map(CopyApproval::getCopyId).distinct().collect(Collectors.toList());
            List<CopyLibrary> libraries = copyLibraryRepository.findAllById(copyIds);
            for (CopyLibrary lib : libraries) libraryMap.put(lib.getId(), lib);
        }
        Map<Long, CopyLibrary> finalMap = libraryMap;
        List<CopyApprovalVO> list = entities.stream().map(e -> toVO(e, finalMap.get(e.getCopyId()))).collect(Collectors.toList());
        return PageResultVO.of(page.getTotalElements(), list, vo.getPage(), vo.getRows());
    }

    @Override
    public CopyApprovalVO getById(Long id) {
        if (id == null || id <= 0) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "审批 ID 无效");
        CopyApproval entity = copyApprovalRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "审批记录不存在"));
        CopyLibrary lib = copyLibraryRepository.findByIdAndDeleted(entity.getCopyId(), 0).orElse(null);
        return toVO(entity, lib);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long save(CopyApprovalSaveVO vo) {
        CopyApproval entity;
        if (vo.getId() != null && vo.getId() > 0) {
            entity = copyApprovalRepository.findByIdAndDeleted(vo.getId(), 0)
                    .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "审批记录不存在"));
        } else {
            // 新建审批记录，校验文案存在
            copyLibraryRepository.findByIdAndDeleted(vo.getCopyId(), 0)
                    .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "文案不存在"));
            entity = new CopyApproval();
            entity.setCopyId(vo.getCopyId());
        }
        if (vo.getUserId() != null) entity.setUserId(vo.getUserId());
        if (vo.getApprovalStatus() != null) {
            entity.setApprovalStatus(vo.getApprovalStatus());
            // 审批完成时记录时间，并同步更新文案状态
            if (vo.getApprovalStatus() == 1 || vo.getApprovalStatus() == 0) {
                entity.setApprovalTime(new Timestamp(System.currentTimeMillis()));
                // 同步文案状态：通过=1，拒绝=0（待审核）
                int libraryStatus = vo.getApprovalStatus() == 1 ? 1 : 0;
                copyLibraryRepository.updateStatus(vo.getCopyId(), libraryStatus);
            }
        }
        if (vo.getComments() != null) entity.setComments(vo.getComments());
        entity = copyApprovalRepository.save(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (id == null || id <= 0) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "审批 ID 无效");
        CopyApproval entity = copyApprovalRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "审批记录不存在"));
        entity.setDeleted(1);
        copyApprovalRepository.save(entity);
    }

    private CopyApprovalVO toVO(CopyApproval e) {
        return toVO(e, null);
    }

    private CopyApprovalVO toVO(CopyApproval e, CopyLibrary lib) {
        CopyApprovalVO vo = new CopyApprovalVO();
        vo.setId(e.getId());
        vo.setCopyId(e.getCopyId());
        vo.setUserId(e.getUserId());
        vo.setApprovalStatus(e.getApprovalStatus());
        vo.setComments(e.getComments());
        vo.setApprovalTime(e.getApprovalTime());
        vo.setCreateTime(e.getCreateTime());
        vo.setUpdateTime(e.getUpdateTime());
        if (lib != null) {
            vo.setCopyTitle(lib.getTitle());
            vo.setCopyContent(lib.getContent());
            vo.setSubmitterId(lib.getUserId());
        }
        return vo;
    }
}
