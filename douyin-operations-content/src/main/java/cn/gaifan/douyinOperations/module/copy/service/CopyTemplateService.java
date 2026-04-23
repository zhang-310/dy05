package cn.gaifan.douyinOperations.module.copy.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.copy.vo.*;

public interface CopyTemplateService {
    PageResultVO<CopyTemplateVO> search(CopyTemplateSearchVO vo);
    CopyTemplateVO getById(Long id);
    long save(CopyTemplateSaveVO vo);
    void delete(Long id);
    void updateStatus(Long id, Integer status);
}
