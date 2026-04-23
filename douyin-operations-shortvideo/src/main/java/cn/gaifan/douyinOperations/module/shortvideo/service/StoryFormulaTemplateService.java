package cn.gaifan.douyinOperations.module.shortvideo.service;

import java.util.List;
import java.util.Map;

public interface StoryFormulaTemplateService {

    List<Map<String, Object>> listUserTemplatesAsMaps(Long ownerId);

    long save(Long ownerId, Map<String, Object> body);

    void delete(Long ownerId, Long id);
}
