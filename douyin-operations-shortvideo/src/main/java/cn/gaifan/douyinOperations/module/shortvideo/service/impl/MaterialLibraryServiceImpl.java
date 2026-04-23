package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvMaterial;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvMaterialRepository;
import cn.gaifan.douyinOperations.module.shortvideo.service.MaterialLibraryService;
import jakarta.annotation.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MaterialLibraryServiceImpl implements MaterialLibraryService {

    @Resource
    private SvMaterialRepository materialRepository;

    @Override
    public PageResultVO<Map<String, Object>> search(Integer page, Integer rows, String materialType, Long projectId, Long ownerId) {
        if (ownerId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        int p = page != null && page >= 0 ? page : 0;
        int r = rows != null && rows > 0 ? Math.min(rows, 100) : 20;
        Specification<SvMaterial> spec = (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            preds.add(cb.equal(root.get("ownerId"), ownerId));
            preds.add(cb.equal(root.get("deleted"), 0));
            if (StringUtils.hasText(materialType)) preds.add(cb.equal(root.get("materialType"), materialType.trim()));
            if (projectId != null) preds.add(cb.equal(root.get("projectId"), projectId));
            return cb.and(preds.toArray(new Predicate[0]));
        };
        Sort sort = "video".equalsIgnoreCase(materialType != null ? materialType.trim() : "")
                && projectId != null
                ? Sort.by(Sort.Order.asc("shotId").with(Sort.NullHandling.NULLS_LAST))
                : Sort.by(Sort.Direction.DESC, "createTime");
        Page<SvMaterial> pg = materialRepository.findAll(spec, PageRequest.of(p, r, sort));
        List<Map<String, Object>> list = pg.getContent().stream().map(this::toMap).toList();
        return PageResultVO.of(pg.getTotalElements(), list, p, r);
    }

    @Override
    public void delete(Long id, Long ownerId) {
        if (ownerId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        SvMaterial m = materialRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "素材不存在"));
        if (!m.getOwnerId().equals(ownerId)) throw new BusinessException(ErrorCode.FORBIDDEN, "无权限删除");
        m.setDeleted(1);
        materialRepository.save(m);
    }

    private Map<String, Object> toMap(SvMaterial e) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", e.getId());
        m.put("materialType", e.getMaterialType());
        m.put("url", e.getUrl());
        m.put("shotId", e.getShotId());
        m.put("projectId", e.getProjectId());
        m.put("duration", e.getDuration());
        m.put("formatType", e.getFormatType());
        m.put("generationType", e.getGenerationType());
        m.put("createTime", e.getCreateTime());
        return m;
    }
}
