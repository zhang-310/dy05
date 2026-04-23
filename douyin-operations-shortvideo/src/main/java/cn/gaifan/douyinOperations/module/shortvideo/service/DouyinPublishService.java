package cn.gaifan.douyinOperations.module.shortvideo.service;

/**
 * 抖音开放平台视频发布服务。
 * 封装 OAuth 鉴权 + 视频上传 + 创建抖音视频。
 */
public interface DouyinPublishService {

    /** 检查是否已配置抖音开放平台凭据 */
    boolean isConfigured();

    /** 检查指定用户是否已完成 OAuth 授权 */
    boolean isUserAuthorized(Long userId);

    /**
     * 发布视频到抖音。
     *
     * @param videoUrl  视频文件 URL（BOS CDN 或本地路径）
     * @param title     视频标题
     * @param userId    操作用户 ID
     * @return 发布结果
     */
    PublishResult publish(String videoUrl, String title, Long userId);

    record PublishResult(boolean success, String itemId, String error) {
        public static PublishResult fail(String error) {
            return new PublishResult(false, null, error);
        }

        public static PublishResult ok(String itemId) {
            return new PublishResult(true, itemId, null);
        }
    }
}
