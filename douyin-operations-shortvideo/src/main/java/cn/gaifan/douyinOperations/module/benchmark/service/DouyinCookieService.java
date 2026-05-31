package cn.gaifan.douyinOperations.module.benchmark.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.benchmark.vo.DouyinCookieSearchVO;
import cn.gaifan.douyinOperations.module.benchmark.vo.DouyinCookieSaveVO;
import cn.gaifan.douyinOperations.module.benchmark.vo.DouyinCookieVO;

/**
 * 抖音Cookie管理服务
 */
public interface DouyinCookieService {

    record AvailableCookie(Long id, String cookieValue) {
    }

    /**
     * 分页查询Cookie
     */
    PageResultVO<DouyinCookieVO> search(DouyinCookieSearchVO searchVO, Long ownerId);

    /**
     * 根据ID获取Cookie
     */
    DouyinCookieVO getById(Long id, Long ownerId);

    /**
     * 保存Cookie（新增或更新）
     */
    DouyinCookieVO save(DouyinCookieSaveVO saveVO, Long ownerId);

    /**
     * 删除Cookie
     */
    void delete(Long id, Long ownerId);

    /**
     * 验证Cookie有效性
     */
    Boolean validate(Long id, Long ownerId);

    /**
     * 获取可用的Cookie
     * @param ownerId 用户ID
     * @param platform 平台
     * @return Cookie值，如果没有可用Cookie返回null
     */
    String getAvailableCookie(Long ownerId, String platform);

    /**
     * 获取指定 Cookie。多服务器采集时可让每个 worker 固定绑定一个抖音账号。
     */
    String getAvailableCookie(Long ownerId, String platform, Long preferredCookieId);

    /**
     * 获取可用 Cookie，并返回本次实际使用的 Cookie id。
     * 分布式采集 worker 需要用 id 精确标记失效账号，避免反复用同一个失效登录态重试。
     */
    AvailableCookie getAvailableCookieForUse(Long ownerId, String platform, Long preferredCookieId);

    /**
     * 将 Cookie 标记为不可用。
     */
    void markUnavailable(Long id, Long ownerId, String platform, String reason);

    /**
     * 记录Cookie使用
     */
    void recordUsage(Long id);
}
