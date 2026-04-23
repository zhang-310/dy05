package cn.gaifan.douyinOperations.module.benchmark.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.benchmark.vo.DouyinCookieSearchVO;
import cn.gaifan.douyinOperations.module.benchmark.vo.DouyinCookieSaveVO;
import cn.gaifan.douyinOperations.module.benchmark.vo.DouyinCookieVO;

/**
 * 抖音Cookie管理服务
 */
public interface DouyinCookieService {

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
     * 记录Cookie使用
     */
    void recordUsage(Long id);
}
