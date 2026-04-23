package cn.gaifan.douyinOperations.module.live.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveSessionTemplateSaveVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveSessionTemplateSearchVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveSessionTemplateVO;

public interface LiveSessionTemplateService {

    PageResultVO<LiveSessionTemplateVO> search(LiveSessionTemplateSearchVO vo, Long userId);

    LiveSessionTemplateVO getById(Long id, Long userId);

    long save(LiveSessionTemplateSaveVO vo, Long userId);

    void delete(Long id, Long userId);

    /** 场次保存前校验：模板存在且系统(0)或属主与 userId 一致 */
    void assertReadableByUser(Long templateId, Long userId);
}
