package cn.gaifan.douyinOperations.module.log.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.log.vo.SystemLogSearchVO;
import cn.gaifan.douyinOperations.module.log.vo.SystemLogVO;

/**
 * 系统日志：分页查询与落库记录
 */
public interface SystemLogService {

    PageResultVO<SystemLogVO> search(SystemLogSearchVO vo);

    /**
     * 记录一条系统日志（异常不抛出避免影响主流程）
     */
    void save(String module, String eventType, String summary, String detail, int status);
}
