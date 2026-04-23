package cn.gaifan.douyinOperations.module.shortvideo.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.*;

import java.util.List;

/**
 * 短视频脚本模板服务
 */
public interface SvScriptTemplateService {

    PageResultVO<SvScriptTemplateVO> search(SvScriptTemplateSearchVO vo, Long ownerId);

    SvScriptTemplateVO getById(Long id, Long ownerId);

    long save(SvScriptTemplateSaveVO vo, Long ownerId);

    void delete(Long id, Long ownerId);

    void incrementUseCount(Long id, Long ownerId);

    List<SvScriptTemplateVO> listByScene(String scene, Long ownerId);
}
