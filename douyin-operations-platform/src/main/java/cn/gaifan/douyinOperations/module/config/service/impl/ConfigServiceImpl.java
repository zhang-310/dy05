package cn.gaifan.douyinOperations.module.config.service.impl;

import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.module.config.entity.ConfigVersionHistory;
import cn.gaifan.douyinOperations.module.config.entity.SysConfig;
import cn.gaifan.douyinOperations.module.config.repository.ConfigVersionHistoryRepository;
import cn.gaifan.douyinOperations.module.config.repository.SysConfigRepository;
import cn.gaifan.douyinOperations.module.config.service.ConfigService;
import cn.gaifan.douyinOperations.module.config.vo.ConfigSaveVO;
import cn.gaifan.douyinOperations.module.config.vo.ConfigSearchVO;
import cn.gaifan.douyinOperations.module.config.vo.ConfigVO;
import com.github.benmanes.caffeine.cache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ConfigServiceImpl implements ConfigService {

    private static final Logger log = LoggerFactory.getLogger(ConfigServiceImpl.class);
    private static final int MASK_LEN = 4;

    @Resource
    private SysConfigRepository sysConfigRepository;
    @Resource
    private ConfigVersionHistoryRepository configVersionHistoryRepository;
    @Resource(name = "stringRedisTemplate")
    private StringRedisTemplate redisTemplate;

    // P1-8: L1 Caffeine 本地缓存
    @Resource(name = "sysConfigCache")
    private Cache<String, Object> sysConfigCache;

    @Override
    public PageResultVO<ConfigVO> search(ConfigSearchVO vo) {
        ConfigSearchVO q = vo != null ? vo : new ConfigSearchVO();
        q.validateParams();
        Specification<SysConfig> spec = (root, query, cb) -> {
            List<Predicate> list = new ArrayList<>();
            list.add(cb.equal(root.get("deleted"), 0));
            if (q.getConfigKey() != null && !q.getConfigKey().trim().isEmpty()) {
                list.add(cb.like(root.get("configKey"), "%" + q.getConfigKey().trim() + "%"));
            }
            if (q.getConfigGroup() != null && !q.getConfigGroup().trim().isEmpty()) {
                list.add(cb.equal(root.get("configGroup"), q.getConfigGroup().trim()));
            }
            if (q.getKeyword() != null && !q.getKeyword().trim().isEmpty()) {
                String k = "%" + q.getKeyword().trim() + "%";
                list.add(cb.or(
                    cb.like(root.get("configKey"), k),
                    cb.like(root.get("configValue"), k),
                    cb.like(root.get("remark"), k)
                ));
            }
            return cb.and(list.toArray(new Predicate[0]));
        };
        Sort sort = Sort.by("desc".equalsIgnoreCase(q.getSortOrder()) ? Sort.Direction.DESC : Sort.Direction.ASC,
                q.getSortName() != null ? q.getSortName() : "id");
        Page<SysConfig> page = sysConfigRepository.findAll(spec, PageRequest.of(q.getPage(), q.getRows(), sort));
        List<ConfigVO> list = page.getContent().stream().map(this::toVO).collect(Collectors.toList());
        return PageResultVO.of(page.getTotalElements(), list, q.getPage(), q.getRows());
    }

    @Override
    @Cacheable(value = "config", key = "#key")
    public ConfigVO getByKey(String key) {
        if (key == null || key.trim().isEmpty()) return null;

        // P1-8: 先查 L1 Caffeine 缓存
        Object cached = sysConfigCache.getIfPresent(key.trim());
        if (cached != null) {
            return (ConfigVO) cached;
        }

        // L1 未命中，查数据库
        ConfigVO vo = sysConfigRepository.findByConfigKeyAndDeleted(key.trim(), 0).map(this::toVO).orElse(null);

        // P1-8: 写入 L1 缓存
        if (vo != null) {
            sysConfigCache.put(key.trim(), vo);
        }

        return vo;
    }

    /**
     * 原始配置值（不脱敏）。故意不加 @Cacheable：与 {@link #getByKey} 共用 Redis 缓存时，
     * 若 Redis 未启动，缓存切面会在查库前失败，导致定时任务等频繁 ERROR。
     * 读库成本低，可接受；变更后依赖 {@link #save} 的 @CacheEvict 刷新 getByKey 缓存。
     *
     * P1-6: 仅供后端内部服务层使用，不对外暴露 Controller 端点。
     * 调用方需自行确保不泄露敏感配置值。
     */
    @Override
    public String getRawValueByKey(String key) {
        if (key == null || key.trim().isEmpty()) return null;
        return sysConfigRepository.findByConfigKeyAndDeleted(key.trim(), 0)
                .map(SysConfig::getConfigValue).orElse(null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "config", key = "#vo.configKey")
    public long save(ConfigSaveVO vo, Long operatorId) {
        if (vo == null) throw new BusinessException(ErrorCode.CONFIG_SAVE_FAIL, "参数不能为空");
        SysConfig entity;
        String oldValue = null;
        String operationType = "CREATE";

        if (vo.getId() != null && vo.getId() > 0) {
            entity = sysConfigRepository.findById(vo.getId()).orElseThrow(
                () -> new BusinessException(ErrorCode.CONFIG_NOT_FOUND, "配置不存在"));
            oldValue = entity.getConfigValue();
            operationType = "UPDATE";
        } else {
            if (sysConfigRepository.findByConfigKeyAndDeleted(vo.getConfigKey().trim(), 0).isPresent()) {
                throw new BusinessException(ErrorCode.CONFIG_SAVE_FAIL, "配置键已存在");
            }
            entity = new SysConfig();
        }

        entity.setConfigKey(vo.getConfigKey().trim());
        entity.setConfigValue(vo.getConfigValue());
        entity.setValueType(vo.getValueType() != null ? vo.getValueType() : "string");
        entity.setIsSensitive(vo.getIsSensitive() != null ? vo.getIsSensitive() : 0);
        entity.setConfigGroup(vo.getConfigGroup() != null ? vo.getConfigGroup().trim() : null);
        entity.setRemark(vo.getRemark() != null ? vo.getRemark().trim() : null);
        sysConfigRepository.save(entity);

        // P1-4: 配置变更审计日志
        logConfigChange(operationType, entity, oldValue, operatorId);

        // P1-8: 清除 L1 缓存
        sysConfigCache.invalidate(vo.getConfigKey().trim());

        if (oldValue != null) {
            ConfigVersionHistory history = new ConfigVersionHistory();
            history.setConfigId(entity.getId());
            history.setConfigKey(entity.getConfigKey());
            history.setOldValue(oldValue);
            history.setNewValue(entity.getConfigValue());
            history.setOperatorId(operatorId);
            configVersionHistoryRepository.save(history);
        }
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "config", key = "#result")
    public void deleteById(Long id) {
        if (id == null) return;
        Optional<SysConfig> opt = sysConfigRepository.findById(id);
        if (!opt.isPresent()) return;
        SysConfig r = opt.get();
        String configKey = r.getConfigKey(); // P1-1: 精确失效缓存键
        r.setDeleted(1);
        sysConfigRepository.save(r);

        // P1-4: 配置删除审计日志
        logConfigChange("DELETE", r, r.getConfigValue(), null);

        // P1-8: 清除 L1 缓存
        sysConfigCache.invalidate(configKey);

        // P1-1: 手动删除缓存（因为方法返回void，@CacheEvict key无法使用#result）
        redisTemplate.delete("config::" + configKey);
    }

    /**
     * P1-4: 配置变更审计日志
     * 记录：操作类型、配置键、操作人、时间戳
     * 敏感配置变更发送告警
     */
    private void logConfigChange(String operationType, SysConfig config, String oldValue, Long operatorId) {
        String configKey = config.getConfigKey();
        String newValue = config.getConfigValue();
        boolean isSensitive = config.getIsSensitive() != null && config.getIsSensitive() == 1;

        // 构建审计日志
        StringBuilder auditLog = new StringBuilder();
        auditLog.append("配置变更审计: ")
                .append("操作=").append(operationType)
                .append(", 配置键=").append(configKey)
                .append(", 操作人ID=").append(operatorId != null ? operatorId : "SYSTEM")
                .append(", 敏感配置=").append(isSensitive);

        if ("UPDATE".equals(operationType)) {
            auditLog.append(", 旧值=").append(isSensitive ? maskSensitiveValue(oldValue) : oldValue)
                    .append(", 新值=").append(isSensitive ? maskSensitiveValue(newValue) : newValue);
        } else if ("DELETE".equals(operationType)) {
            auditLog.append(", 删除值=").append(isSensitive ? maskSensitiveValue(oldValue) : oldValue);
        } else {
            auditLog.append(", 新值=").append(isSensitive ? maskSensitiveValue(newValue) : newValue);
        }

        // 记录审计日志
        log.info(auditLog.toString());

        // P1-4: 敏感配置变更告警（包含 password/secret/key/token）
        if (isSensitive || containsSensitiveKeyword(configKey)) {
            log.warn("敏感配置变更告警: 配置键={}, 操作={}, 操作人ID={}", configKey, operationType, operatorId);
            // TODO: 集成告警系统（邮件/企业微信/钉钉）
        }
    }

    /**
     * P1-4: 检查配置键是否包含敏感关键词
     */
    private boolean containsSensitiveKeyword(String configKey) {
        if (configKey == null) return false;
        String lowerKey = configKey.toLowerCase();
        return lowerKey.contains("password") || lowerKey.contains("secret")
                || lowerKey.contains("key") || lowerKey.contains("token");
    }

    private ConfigVO toVO(SysConfig e) {
        ConfigVO vo = new ConfigVO();
        vo.setId(e.getId());
        vo.setConfigKey(e.getConfigKey());
        vo.setValueType(e.getValueType());
        vo.setIsSensitive(e.getIsSensitive());
        vo.setConfigGroup(e.getConfigGroup());
        vo.setRemark(e.getRemark());
        String val = e.getConfigValue();
        // P1-5: 敏感配置脱敏算法增强 - 根据长度动态脱敏
        if (e.getIsSensitive() != null && e.getIsSensitive() == 1 && val != null) {
            vo.setConfigValue(maskSensitiveValue(val));
        } else {
            vo.setConfigValue(val);
        }
        return vo;
    }

    /**
     * P1-5: 敏感配置脱敏算法增强 - 根据长度动态脱敏
     */
    private String maskSensitiveValue(String value) {
        if (value == null) return null;
        int len = value.length();

        if (len <= 8) {
            return "********";  // 短密钥完全隐藏
        } else if (len <= 16) {
            return value.substring(0, 2) + "******";  // 显示前2位
        } else if (len <= 32) {
            return value.substring(0, 4) + "****" + value.substring(len - 4);
        } else {
            return value.substring(0, 6) + "******" + value.substring(len - 4);
        }
    }
}
