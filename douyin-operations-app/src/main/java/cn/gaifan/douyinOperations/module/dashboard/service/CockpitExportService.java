package cn.gaifan.douyinOperations.module.dashboard.service;

import cn.gaifan.douyinOperations.module.dashboard.vo.CockpitExportRequestVO;
import cn.gaifan.douyinOperations.module.dashboard.vo.CockpitExportResultVO;
import cn.gaifan.douyinOperations.module.dashboard.vo.CockpitPreviewVO;
import cn.gaifan.douyinOperations.module.dashboard.vo.CockpitSessionRowVO;

import java.util.List;

public interface CockpitExportService {

    List<CockpitSessionRowVO> listCockpitSessions(CockpitExportRequestVO request, List<Long> visibleUserIds);

    CockpitExportResultVO exportSessions(CockpitExportRequestVO request, List<Long> visibleUserIds);

    CockpitPreviewVO previewSessions(CockpitExportRequestVO request, List<Long> visibleUserIds);
}
