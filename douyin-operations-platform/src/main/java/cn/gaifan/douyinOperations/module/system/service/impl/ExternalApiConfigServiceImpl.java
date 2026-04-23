package cn.gaifan.douyinOperations.module.system.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.service.ApiKeyEncryptionService;
import cn.gaifan.douyinOperations.module.system.entity.ExternalApiCallLog;
import cn.gaifan.douyinOperations.module.system.entity.ExternalApiConfig;
import cn.gaifan.douyinOperations.module.system.repository.ExternalApiCallLogRepository;
import cn.gaifan.douyinOperations.module.system.repository.ExternalApiConfigRepository;
import cn.gaifan.douyinOperations.module.system.service.ExternalApiConfigService;
import cn.gaifan.douyinOperations.module.system.vo.ExternalApiConfigSaveVO;
import cn.gaifan.douyinOperations.module.system.vo.ExternalApiConfigSearchVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import jakarta.persistence.criteria.Predicate;
import java.sql.Timestamp;
import java.util.*;

/**
 * 外部 API 配置管理服务实现
 */
@Service
public class ExternalApiConfigServiceImpl implements ExternalApiConfigService {

    private static final Logger log = LoggerFactory.getLogger(ExternalApiConfigServiceImpl.class);

    private static final Set<String> SORTABLE_FIELDS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("id", "providerCode", "category", "priority",
                    "healthStatus", "avgLatencyMs", "successRatePct", "createTime", "updateTime")));

    @Resource
    private ExternalApiConfigRepository configRepository;

    @Resource
    private ExternalApiCallLogRepository callLogRepository;

    @Resource
    private ApiKeyEncryptionService apiKeyEncryptionService;

    @Override
    public PageResultVO<ExternalApiConfig> search(ExternalApiConfigSearchVO searchVO) {
        searchVO.validateParams();
        String sortName = SORTABLE_FIELDS.contains(searchVO.getSortName()) ? searchVO.getSortName() : "id";
        Pageable pageable = PageRequest.of(searchVO.getPage(), searchVO.getRows(),
                Sort.by("desc".equalsIgnoreCase(searchVO.getSortOrder()) ? Sort.Direction.DESC : Sort.Direction.ASC, sortName));

        Specification<ExternalApiConfig> spec = buildSpecification(searchVO);
        Page<ExternalApiConfig> page = configRepository.findAll(spec, pageable);

        // 脱敏：列表不返回密钥明文
        List<ExternalApiConfig> masked = page.getContent().stream()
                .map(this::maskSensitiveFields)
                .toList();

        return PageResultVO.of(page.getTotalElements(), masked, searchVO.getPage(), searchVO.getRows());
    }

    @Override
    public ExternalApiConfig getByProviderCode(String code) {
        ExternalApiConfig config = configRepository.findByProviderCodeAndDeleted(code, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "供应商配置不存在: " + code));
        return maskSensitiveFields(config);
    }

    @Override
    @Transactional
    public ExternalApiConfig save(ExternalApiConfigSaveVO saveVO) {
        ExternalApiConfig entity;
        if (saveVO.getId() != null) {
            // 更新
            entity = configRepository.findById(saveVO.getId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "配置不存在"));
        } else {
            // 新增：检查 providerCode 唯一性
            Optional<ExternalApiConfig> existing = configRepository.findByProviderCodeAndDeleted(saveVO.getProviderCode(), 0);
            if (existing.isPresent()) {
                throw new BusinessException(ErrorCode.DATA_ALREADY_EXISTS, "供应商编码已存在: " + saveVO.getProviderCode());
            }
            entity = new ExternalApiConfig();
        }

        entity.setProviderCode(saveVO.getProviderCode());
        entity.setProviderName(saveVO.getProviderName());
        entity.setCategory(saveVO.getCategory());
        entity.setBaseUrl(saveVO.getBaseUrl());
        entity.setIsEnabled(saveVO.getIsEnabled());
        entity.setPriority(saveVO.getPriority());
        entity.setRateLimitPerMin(saveVO.getRateLimitPerMin());
        entity.setDailyQuota(saveVO.getDailyQuota());
        entity.setMonthlyQuota(saveVO.getMonthlyQuota());
        entity.setExtraConfig(saveVO.getExtraConfig());

        if (saveVO.getApiKey() != null && !saveVO.getApiKey().isBlank()) {
            entity.setApiKeyEncrypted(apiKeyEncryptionService.encryptForStorage(saveVO.getApiKey()));
        }
        if (saveVO.getApiSecret() != null && !saveVO.getApiSecret().isBlank()) {
            entity.setApiSecretEncrypted(apiKeyEncryptionService.encryptForStorage(saveVO.getApiSecret()));
        }

        entity = configRepository.save(entity);
        log.info("外部API配置已保存: providerCode={}, id={}", entity.getProviderCode(), entity.getId());
        return maskSensitiveFields(entity);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ExternalApiConfig entity = configRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "配置不存在"));
        entity.setDeleted(1);
        configRepository.save(entity);
        log.info("外部API配置已删除: providerCode={}, id={}", entity.getProviderCode(), id);
    }

    @Override
    public List<ExternalApiConfig> getEnabledByCategory(String category) {
        List<ExternalApiConfig> list = configRepository.findByCategoryAndIsEnabledAndDeleted(category, true, 0);
        list.sort(Comparator.comparingInt(c -> c.getPriority() != null ? c.getPriority() : Integer.MAX_VALUE));
        return list.stream().map(this::maskSensitiveFields).toList();
    }

    @Override
    @Transactional
    public void updateHealthStatus(String providerCode, String status, Integer latencyMs, Float successRate) {
        ExternalApiConfig config = configRepository.findByProviderCodeAndDeleted(providerCode, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "供应商配置不存在: " + providerCode));
        config.setHealthStatus(status);
        config.setAvgLatencyMs(latencyMs);
        config.setSuccessRatePct(successRate);
        config.setLastHealthCheck(new Timestamp(System.currentTimeMillis()));
        configRepository.save(config);
    }

    @Override
    @Transactional
    public void logApiCall(String providerCode, String endpoint, String method,
                           int responseStatus, int latencyMs, String error,
                           String callerModule, Long userId) {
        ExternalApiCallLog logEntry = new ExternalApiCallLog();
        logEntry.setProviderCode(providerCode);
        logEntry.setEndpoint(endpoint);
        logEntry.setMethod(method);
        logEntry.setResponseStatus(responseStatus);
        logEntry.setLatencyMs(latencyMs);
        logEntry.setErrorMessage(error);
        logEntry.setCallerModule(callerModule);
        logEntry.setCallerUserId(userId);
        callLogRepository.save(logEntry);
    }

    @Override
    public String getDecryptedApiKey(String providerCode) {
        ExternalApiConfig config = configRepository.findByProviderCodeAndDeleted(providerCode, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "供应商配置不存在: " + providerCode));
        return apiKeyEncryptionService.decryptForUse(config.getApiKeyEncrypted());
    }

    @Override
    public List<ExternalApiConfig> getAllEnabled() {
        return configRepository.findByIsEnabledAndDeleted(true, 0);
    }

    // ==================== 私有方法 ====================

    /**
     * 构建 Specification 动态查询条件
     */
    private Specification<ExternalApiConfig> buildSpecification(ExternalApiConfigSearchVO searchVO) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (searchVO.getCategory() != null && !searchVO.getCategory().isBlank()) {
                predicates.add(cb.equal(root.get("category"), searchVO.getCategory()));
            }
            if (searchVO.getHealthStatus() != null && !searchVO.getHealthStatus().isBlank()) {
                predicates.add(cb.equal(root.get("healthStatus"), searchVO.getHealthStatus()));
            }
            if (searchVO.getIsEnabled() != null) {
                predicates.add(cb.equal(root.get("isEnabled"), searchVO.getIsEnabled()));
            }
            if (searchVO.getKeyword() != null && !searchVO.getKeyword().isBlank()) {
                String like = "%" + searchVO.getKeyword().trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("providerCode"), like),
                        cb.like(root.get("providerName"), like)
                ));
            }

            return predicates.isEmpty() ? null : cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * 脱敏：apiKeyEncrypted / apiSecretEncrypted 只显示末尾4位
     */
    private ExternalApiConfig maskSensitiveFields(ExternalApiConfig config) {
        if (config.getApiKeyEncrypted() != null) {
            config.setApiKeyEncrypted(maskValue(config.getApiKeyEncrypted()));
        }
        if (config.getApiSecretEncrypted() != null) {
            config.setApiSecretEncrypted(maskValue(config.getApiSecretEncrypted()));
        }
        return config;
    }

    private String maskValue(String value) {
        if (value == null || value.length() <= 4) {
            return "****";
        }
        return "****" + value.substring(value.length() - 4);
    }
}
