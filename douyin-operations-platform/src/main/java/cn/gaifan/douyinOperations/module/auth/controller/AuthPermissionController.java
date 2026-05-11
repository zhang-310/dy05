package cn.gaifan.douyinOperations.module.auth.controller;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.auth.vo.AuthPermissionSearchVO;
import cn.gaifan.douyinOperations.module.auth.vo.AuthPermissionVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

/**
 * 权限管理 Controller
 */
@RestController
@RequestMapping("/api/v1/auth/permission")
public class AuthPermissionController {

    @PostMapping("/search")
    public RESTResult<PageResultVO<AuthPermissionVO>> search(@Valid @RequestBody AuthPermissionSearchVO vo) {
        vo.validateParams();
        return RESTResult.success(new PageResultVO<>(0L, Collections.emptyList(), vo.getPage(), vo.getRows()));
    }
}
