package cn.gaifan.douyinOperations.module.messaging.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.messaging.entity.MsgPlatformConfig;
import cn.gaifan.douyinOperations.module.messaging.vo.MsgPlatformConfigSearchVO;
import cn.gaifan.douyinOperations.module.messaging.vo.MsgPlatformConfigSaveVO;
import cn.gaifan.douyinOperations.module.messaging.vo.MsgPlatformConfigVO;

/**
 * 企微/飞书接入配置服务
 */
public interface MessagingPlatformService {

    PageResultVO<MsgPlatformConfigVO> search(MsgPlatformConfigSearchVO vo);

    MsgPlatformConfigVO getById(Long id, Long userId);

    long save(MsgPlatformConfigSaveVO vo, Long userId);

    void delete(Long id, Long userId);

    MsgPlatformConfigVO getByPlatformAndToken(String platform, String callbackToken);

    /** 内部用：按平台+Token 获取完整配置实体（含 secret 等敏感字段，供 Webhook 验签与回复） */
    MsgPlatformConfig getConfigEntityByPlatformAndToken(String platform, String callbackToken);
}
