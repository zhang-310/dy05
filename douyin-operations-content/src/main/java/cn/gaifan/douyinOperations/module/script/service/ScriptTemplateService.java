package cn.gaifan.douyinOperations.module.script.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.script.vo.*;

import java.util.List;

/**
 * 话术模板服务
 */
public interface ScriptTemplateService {

    PageResultVO<ScriptTemplateVO> search(ScriptTemplateSearchVO vo);

    ScriptTemplateVO getById(Long id);

    long save(ScriptTemplateSaveVO vo);

    void delete(Long id);

    void incrementUseCount(Long id);

    List<ScriptTemplateVO> listByScene(String scene);

    /** 管理端：保存系统模板（仅限 templateType=system） */
    long saveSystemTemplate(ScriptTemplateSaveVO vo);

    /** 管理端：删除系统模板（仅限 templateType=system） */
    void deleteSystemTemplate(Long id);
}
