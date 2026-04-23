package cn.gaifan.douyinOperations.module.shortvideo.service;

import java.util.List;
import java.util.Map;

public interface OpsReportTemplateService {

    List<Map<String, Object>> list(Long ownerId);

    /** 单条（导出等）；无则 null */
    Map<String, Object> findOne(Long ownerId, Long id);

    long save(Long ownerId, Map<String, Object> body);

    void delete(Long ownerId, Long id);
}
