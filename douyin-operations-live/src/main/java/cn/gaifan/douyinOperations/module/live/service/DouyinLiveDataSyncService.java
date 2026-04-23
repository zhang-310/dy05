package cn.gaifan.douyinOperations.module.live.service;

import cn.gaifan.douyinOperations.module.live.vo.LiveSessionDataVO;

/**
 * 抖音开放平台直播数据同步服务
 * <p>
 * 需在抖音开放平台申请「直播数据」能力，配置 client_key、client_secret 后使用。
 * 场次需关联 room_id（可从 live_url 解析或单独存储），且账号需已完成 OAuth 授权。
 * </p>
 *
 * @see cn.gaifan.douyinOperations.module.douyinapi.client.DouyinApiClient
 * @see docs/devops/DOUYIN-LIVE-API-SETUP.md
 */
public interface DouyinLiveDataSyncService {

    /**
     * 从抖音 API 同步直播汇总数据到 live_session_data
     *
     * @param sessionId 场次 ID
     * @param roomId    直播间 room_id（抖音返回）
     * @param accessToken 账号 access_token（从 OAuthToken 获取）
     * @return 同步后的场次数据，失败返回 null
     */
    LiveSessionDataVO syncSessionDataFromDouyin(Long sessionId, String roomId, String accessToken);

    /**
     * 从抖音 API 同步分产品数据到 live_product_data
     *
     * @param sessionId   场次 ID
     * @param roomId      直播间 room_id
     * @param accessToken 账号 access_token
     * @return 是否成功
     */
    boolean syncProductDataFromDouyin(Long sessionId, String roomId, String accessToken);
}
