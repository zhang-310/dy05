package cn.gaifan.douyinOperations.module.copy.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.copy.vo.*;

public interface CopyLibraryService {
    PageResultVO<CopyLibraryVO> search(CopyLibrarySearchVO vo);
    CopyLibraryVO getById(Long id);
    long save(CopyLibrarySaveVO vo);
    void delete(Long id);
    void updateStatus(Long id, Integer status);
    void incrementUseCount(Long id);
}
