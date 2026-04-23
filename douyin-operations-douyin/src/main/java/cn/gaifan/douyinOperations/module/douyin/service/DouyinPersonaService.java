package cn.gaifan.douyinOperations.module.douyin.service;

import cn.gaifan.douyinOperations.module.douyin.entity.DyPersona;
import cn.gaifan.douyinOperations.module.douyin.vo.PersonaSaveVO;

import java.util.List;

/**
 * 人设管理服务
 */
public interface DouyinPersonaService {

    /**
     * 保存或更新人设
     */
    Long savePersona(PersonaSaveVO vo, Long userId);

    /**
     * 获取人设列表
     */
    List<DyPersona> listPersonas(Long userId, String personaType);

    /**
     * 获取人设详情
     */
    DyPersona getPersona(Long id, Long userId);

    /**
     * 删除人设
     */
    void deletePersona(Long id, Long userId);

    /**
     * 设置默认人设
     */
    void setDefaultPersona(Long id, Long userId);

    /**
     * 获取默认人设
     */
    DyPersona getDefaultPersona(Long userId);

    /**
     * 获取系统模板人设
     */
    List<DyPersona> getSystemTemplates();

    /**
     * 根据账号ID获取人设（1:1关系）
     */
    DyPersona getPersonaByAccountId(Long accountId, Long userId);
}
