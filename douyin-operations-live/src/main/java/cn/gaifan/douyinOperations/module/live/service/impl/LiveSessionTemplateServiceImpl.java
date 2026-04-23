package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.live.entity.LiveSessionTemplate;
import cn.gaifan.douyinOperations.module.live.repository.LiveSessionTemplateRepository;
import cn.gaifan.douyinOperations.module.live.service.LiveSessionTemplateService;
import cn.gaifan.douyinOperations.module.live.util.LiveSessionSlotBlueprintParser;
import cn.gaifan.douyinOperations.module.live.vo.LiveSessionTemplateSaveVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveSessionTemplateSearchVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveSessionTemplateVO;
import jakarta.annotation.Resource;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class LiveSessionTemplateServiceImpl implements LiveSessionTemplateService {

    private static final List<String> SORTABLE = List.of("id", "updateTime", "createTime", "name", "code");

    @Resource
    private LiveSessionTemplateRepository templateRepository;

    @Override
    public PageResultVO<LiveSessionTemplateVO> search(LiveSessionTemplateSearchVO vo, Long userId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        vo.validateParams();
        String sortName = vo.getSortName() != null && SORTABLE.contains(vo.getSortName()) ? vo.getSortName() : "updateTime";
        String ord = "asc".equalsIgnoreCase(vo.getSortOrder()) ? "asc" : "desc";
        var pageable = PageRequest.of(vo.getPage(), vo.getRows(),
                Sort.by("desc".equals(ord) ? Sort.Direction.DESC : Sort.Direction.ASC, sortName));

        Specification<LiveSessionTemplate> spec = (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.equal(root.get("deleted"), 0));
            ps.add(root.get("ownerId").in(List.of(0L, userId)));
            if (vo.getKeyword() != null && !vo.getKeyword().isBlank()) {
                String kw = "%" + vo.getKeyword().trim() + "%";
                ps.add(cb.or(
                        cb.like(root.get("name"), kw),
                        cb.like(root.get("code"), kw),
                        cb.like(cb.coalesce(root.get("description"), ""), kw)));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };

        Page<LiveSessionTemplate> page = templateRepository.findAll(spec, pageable);
        List<LiveSessionTemplateVO> list = page.getContent().stream().map(this::toVo).toList();
        return PageResultVO.of(page.getTotalElements(), list, vo.getPage(), vo.getRows());
    }

    @Override
    public LiveSessionTemplateVO getById(Long id, Long userId) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "模板 ID 无效");
        }
        LiveSessionTemplate t = templateRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "模板不存在"));
        if (!Long.valueOf(0L).equals(t.getOwnerId()) && !userId.equals(t.getOwnerId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权查看该模板");
        }
        return toVo(t);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long save(LiveSessionTemplateSaveVO vo, Long userId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        LiveSessionSlotBlueprintParser.parse(vo.getStructureJson());

        LiveSessionTemplate entity;
        if (vo.getId() != null && vo.getId() > 0) {
            entity = templateRepository.findByIdAndDeleted(vo.getId(), 0)
                    .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "模板不存在"));
            if (Long.valueOf(0L).equals(entity.getOwnerId())) {
                throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED, "系统预置模板不可修改");
            }
            if (!userId.equals(entity.getOwnerId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权修改该模板");
            }
        } else {
            entity = new LiveSessionTemplate();
            entity.setOwnerId(userId);
        }

        String code = vo.getCode().trim();
        Long ownerIdForUniq = entity.getOwnerId();
        Long entityIdForUniq = entity.getId();
        templateRepository.findByOwnerIdAndCodeAndDeleted(ownerIdForUniq, code, 0).ifPresent(other -> {
            if (entityIdForUniq == null || !other.getId().equals(entityIdForUniq)) {
                throw new BusinessException(ErrorCode.VALIDATION_FAIL, "同租户下模板编码已存在: " + code);
            }
        });

        entity.setName(vo.getName().trim());
        entity.setCode(code);
        entity.setDescription(vo.getDescription());
        entity.setStructureJson(vo.getStructureJson().trim());
        entity = templateRepository.save(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, Long userId) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "模板 ID 无效");
        }
        LiveSessionTemplate entity = templateRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "模板不存在"));
        if (Long.valueOf(0L).equals(entity.getOwnerId())) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED, "系统预置模板不可删除");
        }
        if (!userId.equals(entity.getOwnerId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权删除该模板");
        }
        entity.setDeleted(1);
        templateRepository.save(entity);
    }

    @Override
    public void assertReadableByUser(Long templateId, Long userId) {
        if (templateId == null || templateId <= 0) {
            return;
        }
        LiveSessionTemplate t = templateRepository.findByIdAndDeleted(templateId, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "场次模板不存在"));
        if (!Long.valueOf(0L).equals(t.getOwnerId()) && !userId.equals(t.getOwnerId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权使用该场次模板");
        }
    }

    private LiveSessionTemplateVO toVo(LiveSessionTemplate t) {
        LiveSessionTemplateVO vo = new LiveSessionTemplateVO();
        vo.setId(t.getId());
        vo.setOwnerId(t.getOwnerId());
        vo.setName(t.getName());
        vo.setCode(t.getCode());
        vo.setDescription(t.getDescription());
        vo.setStructureJson(t.getStructureJson());
        vo.setCreateTime(t.getCreateTime());
        vo.setUpdateTime(t.getUpdateTime());
        return vo;
    }
}
