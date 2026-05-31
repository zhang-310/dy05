package cn.gaifan.douyinOperations.module.ai.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 讯飞语音合成 WebSocket 客户端
 * 文档：https://www.xfyun.cn/doc/tts/online_tts/API.html
 */
public class IflytekTtsClient extends WebSocketClient {

    private static final Logger log = LoggerFactory.getLogger(IflytekTtsClient.class);
    private static final String HOST = "tts-api.xfyun.cn";
    private static final String PATH = "/v2/tts";

    private final AtomicReference<byte[]> audioResult = new AtomicReference<>();
    private final CountDownLatch doneLatch = new CountDownLatch(1);
    private volatile String errorMessage;

    private final String appId;

    public IflytekTtsClient(String appId, String apiKey, String apiSecret) throws Exception {
        super(new URI(buildAuthUrl(apiKey, apiSecret)));
        this.appId = appId;
    }

    /**
     * 构建带鉴权的 WebSocket URL
     */
    static String buildAuthUrl(String apiKey, String apiSecret) throws Exception {
        String url = "wss://" + HOST + PATH;
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        String date = sdf.format(new Date());
        String signatureOrigin = "host: " + HOST + "\ndate: " + date + "\nGET " + PATH + " HTTP/1.1";
        String signature = hmacSha256Base64(signatureOrigin, apiSecret);
        String authorizationOrigin = String.format(
                "api_key=\"%s\", algorithm=\"hmac-sha256\", headers=\"host date request-line\", signature=\"%s\"",
                apiKey, signature);
        String authorization = Base64.getEncoder().encodeToString(authorizationOrigin.getBytes(StandardCharsets.UTF_8));
        return url + "?authorization=" + java.net.URLEncoder.encode(authorization, StandardCharsets.UTF_8)
                + "&date=" + java.net.URLEncoder.encode(date, StandardCharsets.UTF_8)
                + "&host=" + java.net.URLEncoder.encode(HOST, StandardCharsets.UTF_8);
    }

    private static String hmacSha256Base64(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }

    /**
     * 发送合成请求并等待结果
     *
     * @param text   文本（UTF-8）
     * @param vcn    发音人，如 xiaoyan
     * @param speed  语速 0-100，默认 50
     * @param pitch  音高 0-100，默认 50
     * @return 音频字节（mp3），失败返回 null
     */
    public byte[] synthesize(String text, String vcn, int speed, int pitch) {
        try {
            connectBlocking(15, TimeUnit.SECONDS);
            if (!isOpen()) {
                errorMessage = "WebSocket 连接失败，请检查网络或讯飞控制台 IP 白名单";
                log.warn("讯飞 TTS: {}", errorMessage);
                return null;
            }
            String textBase64 = Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
            JSONObject req = new JSONObject();
            req.put("common", new JSONObject().fluentPut("app_id", appId));
            req.put("business", new JSONObject()
                    .fluentPut("aue", "lame")
                    .fluentPut("sfl", 1)
                    .fluentPut("vcn", vcn != null ? vcn : "xiaoyan")
                    .fluentPut("speed", Math.max(0, Math.min(100, speed)))
                    .fluentPut("pitch", Math.max(0, Math.min(100, pitch)))
                    .fluentPut("tte", "UTF8"));
            req.put("data", new JSONObject()
                    .fluentPut("text", textBase64)
                    .fluentPut("status", 2));
            send(req.toJSONString());
            send("{\"data\":{\"status\":2}}"); // 结束标识
            boolean ok = doneLatch.await(60, TimeUnit.SECONDS);
            closeBlocking();
            return ok ? audioResult.get() : null;
        } catch (Exception e) {
            if (errorMessage == null) errorMessage = e.getMessage();
            log.error("讯飞 TTS 合成失败", e);
            return null;
        }
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        log.debug("讯飞 TTS WebSocket 已连接");
    }

    @Override
    public void onMessage(String message) {
        try {
            JSONObject json = JSON.parseObject(message);
            int code = json.getIntValue("code");
            if (code != 0) {
                errorMessage = json.getString("message");
                log.warn("讯飞 TTS 返回错误: code={}, message={}", code, errorMessage);
                doneLatch.countDown();
                return;
            }
            JSONObject data = json.getJSONObject("data");
            if (data == null) {
                doneLatch.countDown();
                return;
            }
            String audio = data.getString("audio");
            if (audio != null && !audio.isEmpty()) {
                byte[] decoded = Base64.getDecoder().decode(audio);
                byte[] existing = audioResult.get();
                if (existing == null) {
                    audioResult.set(decoded);
                } else {
                    byte[] merged = new byte[existing.length + decoded.length];
                    System.arraycopy(existing, 0, merged, 0, existing.length);
                    System.arraycopy(decoded, 0, merged, existing.length, decoded.length);
                    audioResult.set(merged);
                }
            }
            if (data.getIntValue("status") == 2) {
                doneLatch.countDown();
            }
        } catch (Exception e) {
            log.warn("解析讯飞 TTS 响应失败: {}", e.getMessage());
            doneLatch.countDown();
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        if (doneLatch.getCount() > 0) {
            doneLatch.countDown();
        }
    }

    @Override
    public void onError(Exception ex) {
        errorMessage = ex.getMessage();
        log.warn("讯飞 TTS WebSocket 错误: {}", errorMessage);
        if (doneLatch.getCount() > 0) {
            doneLatch.countDown();
        }
    }

    /** 获取最后一次错误信息（合成失败时） */
    public String getErrorMessage() {
        return errorMessage;
    }
}
