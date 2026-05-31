package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.shortvideo.service.AccountCollectWorkerGatewayService;
import cn.gaifan.douyinOperations.module.shortvideo.vo.AccountCollectWorkerClaimVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.AccountCollectWorkerFailVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.AccountCollectWorkerHeartbeatVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.AccountCollectWorkerSubmitResultVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.AccountCollectWorkerSubmitVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.AccountCollectWorkerTaskVO;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/short-video/account-collect/worker")
public class AccountCollectWorkerGatewayController {

    @Resource
    private AccountCollectWorkerGatewayService workerGatewayService;

    @Value("${app.shortvideo.account-collect.remote-worker.token:}")
    private String collectorToken;

    @PostMapping("/claim")
    public RESTResult<AccountCollectWorkerTaskVO> claim(@RequestBody AccountCollectWorkerClaimVO vo,
                                                       HttpServletRequest request) {
        requireCollectorToken(request);
        AccountCollectWorkerTaskVO task = workerGatewayService.claim(vo);
        return RESTResult.success(task);
    }

    @PostMapping("/heartbeat")
    public RESTResult<Boolean> heartbeat(@RequestBody AccountCollectWorkerHeartbeatVO vo,
                                         HttpServletRequest request) {
        requireCollectorToken(request);
        return RESTResult.success(workerGatewayService.heartbeat(vo));
    }

    @PostMapping("/submit")
    public RESTResult<AccountCollectWorkerSubmitResultVO> submit(@RequestBody AccountCollectWorkerSubmitVO vo,
                                                                HttpServletRequest request) {
        requireCollectorToken(request);
        return RESTResult.success(workerGatewayService.submit(vo));
    }

    @PostMapping("/fail")
    public RESTResult<AccountCollectWorkerSubmitResultVO> fail(@RequestBody AccountCollectWorkerFailVO vo,
                                                              HttpServletRequest request) {
        requireCollectorToken(request);
        return RESTResult.success(workerGatewayService.fail(vo));
    }

    private void requireCollectorToken(HttpServletRequest request) {
        if (!StringUtils.hasText(collectorToken)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "主服务器未配置采集节点 Token");
        }
        String provided = request.getHeader("X-Collector-Token");
        if (!collectorToken.equals(provided)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "采集节点 Token 无效");
        }
    }
}
