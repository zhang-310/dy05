package cn.gaifan.douyinOperations.module.script.service.impl;

import cn.gaifan.douyinOperations.module.script.entity.ComplianceWord;
import cn.gaifan.douyinOperations.module.script.repository.ComplianceWordRepository;
import cn.gaifan.douyinOperations.module.script.service.ComplianceWordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 合规词库服务：从 DB 加载，支持内存缓存与刷新
 */
@Service
public class ComplianceWordServiceImpl implements ComplianceWordService {

    private static final Logger log = LoggerFactory.getLogger(ComplianceWordServiceImpl.class);

    private static final String TYPE_ABSOLUTE = "absolute";
    private static final String TYPE_MEDICAL = "medical";

    @Autowired
    private ComplianceWordRepository repository;

    private volatile Map<String, String> absoluteReplacements = Collections.emptyMap();
    private volatile List<String> medicalViolations = Collections.emptyList();
    private volatile boolean loadedFromDb = false;

    @PostConstruct
    public void init() {
        refresh();
    }

    @Override
    public Map<String, String> getAbsoluteReplacements() {
        return absoluteReplacements;
    }

    @Override
    public List<String> getMedicalViolations() {
        return medicalViolations;
    }

    @Override
    public void refresh() {
        try {
            List<ComplianceWord> absolute = repository.findByWordTypeAndIsEnabled(TYPE_ABSOLUTE, 1);
            List<ComplianceWord> medical = repository.findByWordTypeAndIsEnabled(TYPE_MEDICAL, 1);

            Map<String, String> absMap = new LinkedHashMap<>();
            for (ComplianceWord w : absolute) {
                if (w.getWordValue() != null && !w.getWordValue().isBlank() && w.getReplacement() != null) {
                    absMap.put(w.getWordValue().trim(), w.getReplacement().trim());
                }
            }

            List<String> medList = medical.stream()
                    .map(ComplianceWord::getWordValue)
                    .filter(v -> v != null && !v.isBlank())
                    .map(String::trim)
                    .collect(Collectors.toList());

            this.absoluteReplacements = Collections.unmodifiableMap(absMap);
            this.medicalViolations = Collections.unmodifiableList(medList);
            this.loadedFromDb = true;
            log.info("合规词库已刷新: absolute={}, medical={}", absMap.size(), medList.size());
        } catch (Exception e) {
            log.warn("合规词库加载失败，将使用默认值: {}", e.getMessage());
            this.loadedFromDb = false;
        }
    }

    @Override
    public boolean isLoadedFromDb() {
        return loadedFromDb;
    }
}
