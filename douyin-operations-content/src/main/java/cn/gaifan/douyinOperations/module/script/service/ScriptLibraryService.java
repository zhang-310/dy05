package cn.gaifan.douyinOperations.module.script.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.script.vo.*;

import java.util.List;

public interface ScriptLibraryService {
    PageResultVO<ScriptVO> search(ScriptSearchVO vo);
    ScriptVO getById(Long id);
    long save(ScriptSaveVO vo);
    void delete(Long id);
    void incrementUseCount(Long id);

    /**
     * 获取所有话术分类列表
     * @return 分类列表
     */
    List<String> listCategories();
}
