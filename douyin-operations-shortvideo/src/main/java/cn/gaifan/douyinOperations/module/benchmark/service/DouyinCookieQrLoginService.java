package cn.gaifan.douyinOperations.module.benchmark.service;

import cn.gaifan.douyinOperations.module.benchmark.vo.DouyinQrLoginPollResultVO;
import cn.gaifan.douyinOperations.module.benchmark.vo.DouyinQrLoginStartResultVO;

/**
 * 抖音 Web 扫码登录会话：服务端 Playwright 打开网页、截图二维码、轮询登录态并导出 Cookie。
 */
public interface DouyinCookieQrLoginService {

    DouyinQrLoginStartResultVO start(Long ownerId);

    DouyinQrLoginPollResultVO poll(String sessionId, Long ownerId);

    void cancel(String sessionId, Long ownerId);
}
