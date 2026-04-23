package cn.gaifan.douyinOperations.module.script.service;

import cn.gaifan.douyinOperations.module.script.vo.ScriptGenerationRequestVO;
import cn.gaifan.douyinOperations.module.script.vo.ScriptGenerationVO;

public interface ScriptGenerationService {
    ScriptGenerationVO generateScript(Long userId, ScriptGenerationRequestVO request);
}
