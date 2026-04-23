package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvCategory;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvCategoryRepository;
import cn.gaifan.douyinOperations.module.shortvideo.vo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SvCategoryServiceImpl {

    @Resource
    private SvCategoryRepository svCategoryRepository;

    public List<SvCategoryVO> listByOwner(Long ownerId) {
        return svCategoryRepository.findByOwnerIdAndDeletedOrderBySortOrderAsc(ownerId, 0)
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    public SvCategoryVO getById(Long id, Long ownerId) {
        if (ownerId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        SvCategory entity = svCategoryRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "分类不存在"));
        if (!ownerId.equals(entity.getOwnerId()))
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限访问该分类");
        return toVO(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public long save(SvCategorySaveVO vo, Long ownerId) {
        if (ownerId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        SvCategory entity;
        if (vo.getId() != null && vo.getId() > 0) {
            entity = svCategoryRepository.findByIdAndDeleted(vo.getId(), 0)
                    .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "分类不存在"));
            if (!ownerId.equals(entity.getOwnerId()))
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权限修改该分类");
        } else {
            entity = new SvCategory();
            entity.setOwnerId(vo.getOwnerId() != null ? vo.getOwnerId() : ownerId);
        }
        entity.setName(vo.getName());
        if (vo.getDescription() != null) entity.setDescription(vo.getDescription());
        if (vo.getSortOrder() != null) entity.setSortOrder(vo.getSortOrder());
        return svCategoryRepository.save(entity).getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, Long ownerId) {
        if (ownerId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        SvCategory entity = svCategoryRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "分类不存在"));
        if (!ownerId.equals(entity.getOwnerId()))
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限删除该分类");
        entity.setDeleted(1);
        svCategoryRepository.save(entity);
    }

    private SvCategoryVO toVO(SvCategory e) {
        SvCategoryVO vo = new SvCategoryVO();
        vo.setId(e.getId());
        vo.setOwnerId(e.getOwnerId());
        vo.setName(e.getName());
        vo.setDescription(e.getDescription());
        vo.setSortOrder(e.getSortOrder());
        vo.setCreateTime(e.getCreateTime());
        vo.setUpdateTime(e.getUpdateTime());
        return vo;
    }
}
