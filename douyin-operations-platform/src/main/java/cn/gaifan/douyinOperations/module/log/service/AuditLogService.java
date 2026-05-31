package cn.gaifan.douyinOperations.module.log.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.log.vo.LogAuditSearchVO;
import cn.gaifan.douyinOperations.module.log.vo.LogAuditVO;

/**
 * 审计日志：分页查询 audit_log。
 */
public interface AuditLogService {

    PageResultVO<LogAuditVO> search(LogAuditSearchVO vo);
}
