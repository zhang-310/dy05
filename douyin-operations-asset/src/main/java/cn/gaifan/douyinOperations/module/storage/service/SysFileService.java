package cn.gaifan.douyinOperations.module.storage.service;

import cn.gaifan.douyinOperations.module.storage.vo.SysFileVO;

import java.util.List;

/**
 * 文件记录服务（数据库层面的文件管理）
 */
public interface SysFileService {

    SysFileVO getById(Long id);

    List<SysFileVO> listByOwner(Long ownerId);

    List<SysFileVO> listByOwnerAndModule(Long ownerId, String module);

    void delete(Long id);
}
