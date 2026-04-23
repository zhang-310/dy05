package cn.gaifan.douyinOperations.module.storage.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.storage.entity.SysFile;
import cn.gaifan.douyinOperations.module.storage.repository.SysFileRepository;
import cn.gaifan.douyinOperations.module.storage.service.SysFileService;
import cn.gaifan.douyinOperations.module.storage.vo.SysFileVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SysFileServiceImpl implements SysFileService {

    @Resource
    private SysFileRepository sysFileRepository;

    @Override
    public SysFileVO getById(Long id) {
        if (id == null || id <= 0) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "文件 ID 无效");
        return toVO(sysFileRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "文件不存在")));
    }

    @Override
    public List<SysFileVO> listByOwner(Long ownerId) {
        return sysFileRepository.findByOwnerIdAndDeleted(ownerId, 0)
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public List<SysFileVO> listByOwnerAndModule(Long ownerId, String module) {
        return sysFileRepository.findByOwnerIdAndModuleAndDeleted(ownerId, module, 0)
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (id == null || id <= 0) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "文件 ID 无效");
        SysFile entity = sysFileRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "文件不存在"));
        entity.setDeleted(1);
        sysFileRepository.save(entity);
    }

    private SysFileVO toVO(SysFile e) {
        SysFileVO vo = new SysFileVO();
        vo.setId(e.getId());
        vo.setOwnerId(e.getOwnerId());
        vo.setOriginalName(e.getOriginalName());
        vo.setStorageName(e.getStorageName());
        vo.setStoragePath(e.getStoragePath());
        vo.setFileUrl(e.getFileUrl());
        vo.setFileType(e.getFileType());
        vo.setFileExt(e.getFileExt());
        vo.setFileSize(e.getFileSize());
        vo.setModule(e.getModule());
        vo.setProvider(e.getProvider());
        vo.setCreateTime(e.getCreateTime());
        return vo;
    }
}
