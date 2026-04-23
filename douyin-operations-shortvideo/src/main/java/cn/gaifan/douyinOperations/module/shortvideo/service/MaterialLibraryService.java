package cn.gaifan.douyinOperations.module.shortvideo.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;

import java.util.Map;

/**
 * 素材库 Service
 */
public interface MaterialLibraryService {

    PageResultVO<Map<String, Object>> search(Integer page, Integer rows, String materialType, Long projectId, Long ownerId);

    void delete(Long id, Long ownerId);
}
