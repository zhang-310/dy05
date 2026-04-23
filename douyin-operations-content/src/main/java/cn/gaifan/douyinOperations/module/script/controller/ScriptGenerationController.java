package cn.gaifan.douyinOperations.module.script.controller;

import cn.gaifan.douyinOperations.common.annotation.CurrentUserId;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.script.service.ScriptGenerationService;
import cn.gaifan.douyinOperations.module.script.vo.ScriptGenerationRequestVO;
import cn.gaifan.douyinOperations.module.script.vo.ScriptGenerationVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/script")
public class ScriptGenerationController {

    @Autowired
    private ScriptGenerationService scriptGenerationService;

    @PostMapping("/generate")
    public RESTResult<ScriptGenerationVO> generateScript(
            @CurrentUserId Long userId,
            @Valid @RequestBody ScriptGenerationRequestVO request) {
        ScriptGenerationVO result = scriptGenerationService.generateScript(userId, request);
        return RESTResult.success("生成成功", result);
    }
}
