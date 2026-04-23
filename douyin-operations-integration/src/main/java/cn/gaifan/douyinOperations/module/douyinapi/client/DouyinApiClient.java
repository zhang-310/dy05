package cn.gaifan.douyinOperations.module.douyinapi.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 抖音开放平台 API 客户端
 * 文档: https://developer.open-douyin.com/docs/resource/zh-CN/dop/develop/openapi/
 * 使用全局 RestTemplate（带 API 调用日志拦截器）
 */
@Component
public class DouyinApiClient {

    private static final Logger log = LoggerFactory.getLogger(DouyinApiClient.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${douyin.api.client-key}")
    private String clientKey;

    @Value("${douyin.api.client-secret}")
    private String clientSecret;

    @Value("${douyin.api.base-url:https://open.douyin.com}")
    private String baseUrl;

    @Resource
    private RestTemplate restTemplate;

    // ─── OAuth 2.0 授权 ──────────────────────────────────────

    /**
     * 获取授权 URL
     */
    public String getAuthUrl(String redirectUri, String state) {
        return String.format("%s/platform/oauth/connect?client_key=%s&response_type=code&scope=user_info,video.list,live.room&redirect_uri=%s&state=%s",
                baseUrl, clientKey, redirectUri, state);
    }

    /**
     * 授权码换取 access_token
     */
    public AccessTokenResponse getAccessToken(String code) {
        try {
            String url = baseUrl + "/oauth/access_token/";
            Map<String, String> body = new HashMap<>();
            body.put("client_key", clientKey);
            body.put("client_secret", clientSecret);
            body.put("code", code);
            body.put("grant_type", "authorization_code");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                if (root.path("data").path("error_code").asInt() == 0) {
                    JsonNode data = root.path("data");
                    return new AccessTokenResponse(
                            data.path("access_token").asText(),
                            data.path("refresh_token").asText(),
                            data.path("expires_in").asLong(),
                            data.path("open_id").asText()
                    );
                }
                log.error("获取 access_token 失败: {}", response.getBody());
            }
        } catch (Exception e) {
            log.error("获取 access_token 异常", e);
        }
        return null;
    }

    /**
     * 刷新 access_token
     */
    public AccessTokenResponse refreshAccessToken(String refreshToken) {
        try {
            String url = baseUrl + "/oauth/refresh_token/";
            Map<String, String> body = new HashMap<>();
            body.put("client_key", clientKey);
            body.put("refresh_token", refreshToken);
            body.put("grant_type", "refresh_token");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                if (root.path("data").path("error_code").asInt() == 0) {
                    JsonNode data = root.path("data");
                    return new AccessTokenResponse(
                            data.path("access_token").asText(),
                            data.path("refresh_token").asText(),
                            data.path("expires_in").asLong(),
                            data.path("open_id").asText()
                    );
                }
            }
        } catch (Exception e) {
            log.error("刷新 access_token 异常", e);
        }
        return null;
    }

    // ─── 直播间 API ──────────────────────────────────────

    /**
     * 获取直播间信息
     */
    public LiveRoomInfo getLiveRoomInfo(String roomId, String accessToken) {
        try {
            String url = baseUrl + "/api/live/room/info/?room_id=" + roomId + "&access_token=" + accessToken;
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                if (root.path("data").path("error_code").asInt() == 0) {
                    JsonNode data = root.path("data").path("room");
                    return new LiveRoomInfo(
                            data.path("room_id").asText(),
                            data.path("title").asText(),
                            data.path("status").asInt(),
                            data.path("user_count").asInt(),
                            data.path("total_user").asInt()
                    );
                }
            }
        } catch (Exception e) {
            log.error("获取直播间信息失败: roomId={}", roomId, e);
        }
        return null;
    }

    /**
     * 获取直播数据
     */
    public LiveDataResponse getLiveData(String roomId, String accessToken) {
        try {
            String url = baseUrl + "/api/live/data/query/?room_id=" + roomId + "&access_token=" + accessToken;
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                if (root.path("data").path("error_code").asInt() == 0) {
                    JsonNode data = root.path("data");
                    return new LiveDataResponse(
                            data.path("user_count").asInt(),
                            data.path("total_user").asLong(),
                            data.path("like_count").asLong(),
                            data.path("comment_count").asInt(),
                            data.path("share_count").asInt(),
                            data.path("gift_count").asInt()
                    );
                }
            }
        } catch (Exception e) {
            log.error("获取直播数据失败: roomId={}", roomId, e);
        }
        return null;
    }

    /**
     * 获取直播商品列表
     */
    public ProductListResponse getProductList(String roomId, String accessToken) {
        try {
            String url = baseUrl + "/api/live/product/list/?room_id=" + roomId + "&access_token=" + accessToken;
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                if (root.path("data").path("error_code").asInt() == 0) {
                    JsonNode products = root.path("data").path("products");
                    java.util.List<ProductInfo> list = new java.util.ArrayList<>();
                    for (JsonNode p : products) {
                        list.add(new ProductInfo(
                                p.path("product_id").asText(),
                                p.path("name").asText(),
                                p.path("price").asDouble(),
                                p.path("sales").asInt(),
                                p.path("stock").asInt()
                        ));
                    }
                    return new ProductListResponse(list);
                }
            }
        } catch (Exception e) {
            log.error("获取商品列表失败: roomId={}", roomId, e);
        }
        return null;
    }

    // ─── 响应对象 ──────────────────────────────────────

    public record AccessTokenResponse(String accessToken, String refreshToken, long expiresIn, String openId) {}

    public record LiveRoomInfo(String roomId, String title, int status, int userCount, int totalUser) {}

    public record LiveDataResponse(int viewers, long totalViewers, long likes, int comments, int shares, int gifts) {}

    public record ProductInfo(String productId, String name, double price, int sales, int stock) {}

    public record ProductListResponse(java.util.List<ProductInfo> products) {}

    // ─── 视频数据 API (发布反馈闭环) ──────────────────────────────────────

    /**
     * 获取视频互动数据（播放、点赞、评论、分享）
     * 文档: https://developer.open-douyin.com/docs/resource/zh-CN/dop/develop/openapi/video-management/video-data
     * 需申请「视频互动数据」能力
     */
    public VideoDataResponse getVideoData(String itemId, String accessToken) {
        if (itemId == null || itemId.isBlank() || accessToken == null || accessToken.isBlank()) {
            return null;
        }
        return withRetry("getVideoData", () -> {
            try {
                String url = baseUrl + "/api/douyin/v1/video/data/?item_id=" + itemId + "&access_token=" + accessToken;
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<String> entity = new HttpEntity<>("{}", headers);

                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    JsonNode root = objectMapper.readTree(response.getBody());
                    JsonNode data = root.path("data");
                    if (data != null && !data.isMissingNode()) {
                        int errCode = data.path("error_code").asInt(0);
                        if (errCode == 0) {
                            JsonNode stat = data.path("statistics");
                            if (!stat.isMissingNode()) {
                                return new VideoDataResponse(
                                        stat.path("total_play").asLong(0),
                                        stat.path("total_like").asLong(0),
                                        stat.path("total_comment").asLong(0),
                                        stat.path("total_share").asLong(0),
                                        stat.path("avg_play_duration").asDouble(0)
                                );
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("getVideoData 解析失败: itemId={}, err={}", itemId, e.getMessage());
            }
            return null;
        });
    }

    /** 视频互动数据响应 */
    public record VideoDataResponse(long totalPlay, long totalLike, long totalComment, long totalShare, double avgPlayDuration) {}

    /**
     * 获取视频评论列表（发布反馈闭环 - 脚本学习）
     */
    public CommentListResponse getCommentList(String itemId, String accessToken, int cursor, int maxCount) {
        if (itemId == null || itemId.isBlank() || accessToken == null || accessToken.isBlank()) {
            return null;
        }
        try {
            String url = baseUrl + "/api/douyin/v1/video/comment/list/?item_id=" + itemId
                    + "&access_token=" + accessToken + "&cursor=" + cursor + "&count=" + maxCount;
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode data = root.path("data");
                if (!data.isMissingNode() && data.path("error_code").asInt(0) == 0) {
                    java.util.List<CommentInfo> comments = new java.util.ArrayList<>();
                    for (JsonNode c : data.path("comments")) {
                        comments.add(new CommentInfo(c.path("cid").asText(), c.path("text").asText(),
                                c.path("digg_count").asInt()));
                    }
                    return new CommentListResponse(comments, data.path("has_more").asBoolean(false),
                            data.path("cursor").asLong(0));
                }
            }
        } catch (Exception e) {
            log.debug("获取评论列表失败: itemId={}, err={}", itemId, e.getMessage());
        }
        return null;
    }

    public record CommentInfo(String cid, String content, int diggCount) {}

    public record CommentListResponse(java.util.List<CommentInfo> comments, boolean hasMore, long cursor) {}

    /**
     * 获取用户视频列表
     */
    public VideoListResponse getVideoList(String openId, String accessToken, long cursor, int count) {
        if (openId == null || openId.isBlank() || accessToken == null || accessToken.isBlank()) {
            return null;
        }
        try {
            String url = baseUrl + "/api/douyin/v1/video/list/?open_id=" + openId
                    + "&access_token=" + accessToken + "&cursor=" + cursor + "&count=" + count;
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode data = root.path("data");
                if (!data.isMissingNode() && data.path("error_code").asInt(0) == 0) {
                    java.util.List<VideoItem> items = new java.util.ArrayList<>();
                    for (JsonNode v : data.path("list")) {
                        items.add(new VideoItem(v.path("item_id").asText(),
                                v.path("title").asText(),
                                v.path("cover").path("url_list").isEmpty() ? "" : v.path("cover").path("url_list").get(0).asText(),
                                v.path("video").path("play_addr").path("url_list").isEmpty() ? "" : v.path("video").path("play_addr").path("url_list").get(0).asText(),
                                v.path("statistics").path("play_count").asLong(0),
                                v.path("statistics").path("digg_count").asLong(0),
                                v.path("statistics").path("comment_count").asLong(0),
                                v.path("statistics").path("share_count").asLong(0),
                                v.path("statistics").path("collect_count").asLong(0),
                                v.path("create_time").asLong(0)));
                    }
                    return new VideoListResponse(items, data.path("has_more").asBoolean(false), data.path("cursor").asLong(0));
                }
            }
        } catch (Exception e) {
            log.debug("获取视频列表失败: openId={}, err={}", openId, e.getMessage());
        }
        return null;
    }

    /** collectCount：视频被收藏数（statistics.collect_count，无则 0） */
    public record VideoItem(String itemId, String title, String coverUrl, String videoUrl,
                            long playCount, long likeCount, long commentCount, long shareCount, long collectCount,
                            long createTime) {
        public long diggCount() { return likeCount; }
        public int duration() { return 0; }
    }

    public record VideoListResponse(java.util.List<VideoItem> list, boolean hasMore, long cursor) {
        public java.util.List<VideoItem> videos() { return list; }
    }

    // ─── 重试工具 ──────────────────────────────────────

    private static final int MAX_RETRIES = 2;

    /** 带重试的 API 调用包装器，对瞬时网络错误自动重试 */
    private <T> T withRetry(String apiName, Supplier<T> action) {
        Exception lastEx = null;
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                return action.get();
            } catch (org.springframework.web.client.ResourceAccessException e) {
                lastEx = e;
                if (attempt < MAX_RETRIES) {
                    long waitMs = (long) (2000 * Math.pow(2, attempt));
                    log.warn("抖音 API [{}] 网络错误，{}ms 后重试 (attempt {}): {}", apiName, waitMs, attempt + 1, e.getMessage());
                    try { Thread.sleep(waitMs); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                }
            } catch (org.springframework.web.client.HttpServerErrorException e) {
                lastEx = e;
                if (attempt < MAX_RETRIES) {
                    long waitMs = (long) (3000 * Math.pow(2, attempt));
                    log.warn("抖音 API [{}] 5xx 错误，{}ms 后重试 (attempt {}): {}", apiName, waitMs, attempt + 1, e.getStatusCode());
                    try { Thread.sleep(waitMs); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                }
            }
        }
        log.error("抖音 API [{}] 重试耗尽", apiName, lastEx);
        return null;
    }
}
