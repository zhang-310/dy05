package cn.gaifan.douyinOperations.module.shortvideo.service;

import java.util.List;
import java.util.Map;

/** H-1：全局运镜场景关键词映射运营 CRUD（仅管理员写） */
public interface SceneCameraMappingAdminService {

    List<Map<String, Object>> listAll();

    long save(Map<String, Object> body);

    void delete(Long id);
}
