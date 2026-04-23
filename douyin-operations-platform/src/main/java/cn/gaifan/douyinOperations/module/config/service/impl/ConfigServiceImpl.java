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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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

    private static final int MASK_LEN = 4;

    @Resource
    private SysConfigRepository sysConfigRepository;
    @Resource
    private ConfigVersionHistoryRepository configVersionHistoryRepository;

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
        return sysConfigRepository.findByConfigKeyAndDeleted(key.trim(), 0).map(this::toVO).orElse(null);
    }

    /**
     * 原始配置值（不脱敏）。故意不加 @Cacheable：与 {@link #getByKey} 共用 Redis 缓存时，
     * 若 Redis 未启动，缓存切面会在查库前失败，导致定时任务等频繁 ERROR。
     * 读库成本低，可接受；变更后依赖 {@link #save} 的 @CacheEvict 刷新 getByKey 缓存。
     */
    @Override
    public String getRawValueByKey(String key) {
        if (key == null || key.trim().isEmpty()) return null;
        return sysConfigRepository.findByConfigKeyAndDeleted(key.trim(), 0)
                .map(SysConfig::getConfigValue).orElse(null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "config", allEntries = true)
    public long save(ConfigSaveVO vo, Long operatorId) {
        if (vo == null) throw new BusinessException(ErrorCode.CONFIG_SAVE_FAIL, "参数不能为空");
        SysConfig entity;
        String oldValue = null;
        if (vo.getId() != null && vo.getId() > 0) {
            entity = sysConfigRepository.findById(vo.getId()).orElseThrow(
                () -> new BusinessException(ErrorCode.CONFIG_NOT_FOUND, "配置不存在"));
            oldValue = entity.getConfigValue();
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
    @CacheEvict(value = "config", allEntries = true)
    public void deleteById(Long id) {
        if (id == null) return;
        Optional<SysConfig> opt = sysConfigRepository.findById(id);
        if (!opt.isPresent()) return;
        SysConfig r = opt.get();
        r.setDeleted(1);
        sysConfigRepository.save(r);
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
        if (e.getIsSensitive() != null && e.getIsSensitive() == 1 && val != null && val.length() > MASK_LEN) {
            vo.setConfigValue("****" + val.substring(val.length() - MASK_LEN));
        } else {
            vo.setConfigValue(val);
        }
        return vo;
    }
}
