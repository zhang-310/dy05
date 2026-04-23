package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvSceneCameraMapping;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvSceneCameraMappingRepository;
import cn.gaifan.douyinOperations.module.shortvideo.service.SceneCameraMappingAdminService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SceneCameraMappingAdminServiceImpl implements SceneCameraMappingAdminService {

    @Resource
    private SvSceneCameraMappingRepository repository;

    @Override
    public List<Map<String, Object>> listAll() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (SvSceneCameraMapping m : repository.findAllByOrderByConfidenceDesc()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", m.getId());
            row.put("sceneKeyword", m.getSceneKeyword());
            row.put("recommendedCamera", m.getRecommendedCamera());
            row.put("confidence", m.getConfidence());
            row.put("source", m.getSource());
            row.put("createTime", m.getCreateTime());
            out.add(row);
        }
        return out;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long save(Map<String, Object> body) {
        Long id = body != null && body.get("id") instanceof Number n ? n.longValue() : null;
        String kw = body != null && body.get("sceneKeyword") instanceof String s ? s.trim() : "";
        String cam = body != null && body.get("recommendedCamera") instanceof String s ? s.trim() : "";
        if (!StringUtils.hasText(kw) || !StringUtils.hasText(cam)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "sceneKeyword、recommendedCamera 必填");
        }
        BigDecimal conf = BigDecimal.valueOf(0.5);
        if (body.get("confidence") instanceof Number n) {
            conf = BigDecimal.valueOf(n.doubleValue());
        }
        String source = body != null && body.get("source") instanceof String s ? s.trim() : "ops";

        SvSceneCameraMapping e;
        if (id != null && id > 0) {
            e = repository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "记录不存在"));
        } else {
            e = new SvSceneCameraMapping();
            e.setCreateTime(new Timestamp(System.currentTimeMillis()));
        }
        e.setSceneKeyword(kw);
        e.setRecommendedCamera(cam);
        e.setConfidence(conf);
        e.setSource(source);
        repository.save(e);
        return e.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (id == null) {
            return;
        }
        if (!repository.existsById(id)) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "记录不存在");
        }
        repository.deleteById(id);
    }
}
