package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.live.service.LiveScriptService;
import cn.gaifan.douyinOperations.module.live.vo.LiveScriptVO;
import cn.gaifan.douyinOperations.module.shortvideo.service.CrossModuleContentService;
import cn.gaifan.douyinOperations.module.shortvideo.service.ShortVideoAiService;
import cn.gaifan.douyinOperations.module.shortvideo.vo.AiScriptGenerateVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CrossModuleContentServiceImpl implements CrossModuleContentService {

    private static final Logger log = LoggerFactory.getLogger(CrossModuleContentServiceImpl.class);

    @Autowired(required = false)
    private LiveScriptService liveScriptService;

    @Autowired
    private ShortVideoAiService shortVideoAiService;

    @Override
    public String convertLiveScriptToVideoScript(Long liveScriptId, Long userId) {
        if (liveScriptService == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "直播话术模块未启用");
        }

        LiveScriptVO liveScript = liveScriptService.getById(liveScriptId, userId);
        if (liveScript == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "话术不存在或无权访问");
        }

        String scriptContent = liveScript.getScriptContent();
        if (scriptContent == null || scriptContent.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS, "话术内容为空，无法转换");
        }

        AiScriptGenerateVO vo = new AiScriptGenerateVO();
        vo.setCopyText(scriptContent);

        log.info("开始将直播话术 {} 转换为短视频脚本, userId={}", liveScriptId, userId);
        return shortVideoAiService.generateScript(vo, userId);
    }
}
