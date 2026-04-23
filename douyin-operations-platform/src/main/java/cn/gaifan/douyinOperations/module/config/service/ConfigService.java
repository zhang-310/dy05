package cn.gaifan.douyinOperations.module.config.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.config.vo.ConfigSaveVO;
import cn.gaifan.douyinOperations.module.config.vo.ConfigSearchVO;
import cn.gaifan.douyinOperations.module.config.vo.ConfigVO;

public interface ConfigService {

    PageResultVO<ConfigVO> search(ConfigSearchVO vo);

    ConfigVO getByKey(String key);

    /**
     * 按 key 获取配置原始值（不脱敏），仅后端内部使用（如 BOS 鉴权）
     */
    String getRawValueByKey(String key);

    /**
     * 保存配置。更新时会写入一条版本历史（sys_config_version_history）。
     * @param operatorId 操作人用户 ID，可为 null（历史记录中 operator_id 为空）
     */
    long save(ConfigSaveVO vo, Long operatorId);

    void deleteById(Long id);
}
