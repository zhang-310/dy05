package cn.gaifan.douyinOperations.module.shortvideo.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvContentCalendarSaveVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvContentCalendarSearchVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvContentCalendarVO;

import java.util.List;

public interface SvContentCalendarService {

    PageResultVO<SvContentCalendarVO> list(SvContentCalendarSearchVO searchVO, Long userId);

    SvContentCalendarVO get(Long id, Long userId);

    Long save(SvContentCalendarSaveVO saveVO, Long userId);

    void delete(Long id, Long userId);

    List<SvContentCalendarVO> listByDateRange(String from, String to, Long personaId, Long userId);

    int autoGenerate(Long personaId, String from, String to, Long userId);
}
