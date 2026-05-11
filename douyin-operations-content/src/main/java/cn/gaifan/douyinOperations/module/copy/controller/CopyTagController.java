package cn.gaifan.douyinOperations.module.copy.controller;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.copy.vo.CopyTagSearchVO;
import cn.gaifan.douyinOperations.module.copy.vo.CopyTagVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

/**
 * 文案标签 Controller
 */
@RestController
@RequestMapping("/api/v1/copy/tag")
public class CopyTagController {

    @PostMapping("/search")
    public RESTResult<PageResultVO<CopyTagVO>> search(@Valid @RequestBody CopyTagSearchVO vo) {
        vo.validateParams();
        return RESTResult.success(new PageResultVO<>(0L, Collections.emptyList(), vo.getPage(), vo.getRows()));
    }
}
