package cn.gaifan.douyinOperations.module.copy.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.copy.vo.*;

public interface CopyApprovalService {
    PageResultVO<CopyApprovalVO> search(CopyApprovalSearchVO vo);
    CopyApprovalVO getById(Long id);
    long save(CopyApprovalSaveVO vo);
    void delete(Long id);
}
