package cn.gaifan.douyinOperations.module.storage.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.config.service.ConfigService;
import cn.gaifan.douyinOperations.module.storage.service.BosStorageService;
import cn.gaifan.douyinOperations.module.storage.vo.StorageFileVO;
import com.baidubce.auth.DefaultBceCredentials;
import com.baidubce.services.bos.BosClient;
import com.baidubce.services.bos.BosClientConfiguration;
import com.baidubce.services.bos.model.BosObjectSummary;
import com.baidubce.services.bos.model.ListObjectsRequest;
import com.baidubce.services.bos.model.ListObjectsResponse;
import com.baidubce.services.bos.model.ObjectMetadata;
import com.baidubce.services.bos.model.PutObjectRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

/**
 * 百度云 BOS 存储服务实现：从系统配置读取 endpoint/bucket/AK/SK/CDN 域名
 * 配置项（在「系统配置」中维护）：storage.bos.endpoint、storage.bos.bucket、
 * storage.bos.accessKey、storage.bos.secretKey、storage.bos.cdnDomain（可选）、storage.bos.region（可选，如 bj）
 */
@Slf4j
@Service
public class BosStorageServiceImpl implements BosStorageService {

    private static final String CONFIG_ENDPOINT = "storage.bos.endpoint";
    private static final String CONFIG_BUCKET = "storage.bos.bucket";
    private static final String CONFIG_ACCESS_KEY = "storage.bos.accessKey";
    private static final String CONFIG_SECRET_KEY = "storage.bos.secretKey";
    private static final String CONFIG_CDN_DOMAIN = "storage.bos.cdnDomain";
    private static final String CONFIG_REGION = "storage.bos.region";

    @Resource
    private ConfigService configService;

    /** 读取配置原始值（含敏感项），仅用于 BOS 等后端鉴权 */
    private String getConfigValue(String key) {
        String raw = configService.getRawValueByKey(key);
        return StringUtils.hasText(raw) ? raw.trim() : null;
    }

    @Override
    public boolean isConfigured() {
        String endpoint = getConfigValue(CONFIG_ENDPOINT);
        String bucket = getConfigValue(CONFIG_BUCKET);
        String ak = getConfigValue(CONFIG_ACCESS_KEY);
        String sk = getConfigValue(CONFIG_SECRET_KEY);
        return StringUtils.hasText(endpoint) && StringUtils.hasText(bucket) && StringUtils.hasText(ak) && StringUtils.hasText(sk);
    }

    private BosClient createClient() {
        String endpoint = getConfigValue(CONFIG_ENDPOINT);
        String ak = getConfigValue(CONFIG_ACCESS_KEY);
        String sk = getConfigValue(CONFIG_SECRET_KEY);
        if (!StringUtils.hasText(endpoint) || !StringUtils.hasText(ak) || !StringUtils.hasText(sk)) {
            throw new IllegalStateException("BOS 未配置或配置不完整，请在「系统配置」中设置 storage.bos.*");
        }
        BosClientConfiguration config = new BosClientConfiguration();
        config.setCredentials(new DefaultBceCredentials(ak, sk));
        config.setEndpoint(endpoint.trim());
        return new BosClient(config);
    }

    private String getBucket() {
        String bucket = getConfigValue(CONFIG_BUCKET);
        if (!StringUtils.hasText(bucket)) throw new IllegalStateException("BOS bucket 未配置");
        return bucket.trim();
    }

    @Override
    public List<StorageFileVO> listObjects(String prefix) {
        if (!isConfigured()) return new ArrayList<>();
        String normalizedPrefix = StringUtils.hasText(prefix) ? prefix.trim() : "";
        if (!normalizedPrefix.isEmpty() && !normalizedPrefix.endsWith("/")) {
            normalizedPrefix += "/";
        }
        List<StorageFileVO> list = new ArrayList<>();
        BosClient client = null;
        try {
            client = createClient();
            String bucket = getBucket();
            ListObjectsRequest req = new ListObjectsRequest(bucket).withPrefix(normalizedPrefix).withDelimiter("/").withMaxKeys(1000);
            ListObjectsResponse resp = client.listObjects(req);
            List<String> commonPrefixes = resp.getCommonPrefixes();
            if (commonPrefixes != null) {
                for (String commonPrefix : commonPrefixes) {
                    list.add(new StorageFileVO(commonPrefix, null, null, null, true));
                }
            }
            for (BosObjectSummary sum : resp.getContents()) {
                String key = sum.getKey();
                if (key.equals(normalizedPrefix)) continue;
                String url = getPublicUrl(key);
                list.add(new StorageFileVO(key, sum.getSize(), sum.getLastModified(), url, false));
            }
        } catch (Exception e) {
            log.warn("BOS listObjects 失败 prefix={}", normalizedPrefix, e);
        }
        return list;
    }

    @Override
    public String upload(String key, MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("文件为空");
        String objectKey = StringUtils.hasText(key) ? key.trim() : file.getOriginalFilename();
        if (!StringUtils.hasText(objectKey)) objectKey = "upload-" + System.currentTimeMillis();
        BosClient client = null;
        try {
            client = createClient();
            String bucket = getBucket();
            ObjectMetadata meta = new ObjectMetadata();
            meta.setContentLength(file.getSize());
            String contentType = file.getContentType();
            if (StringUtils.hasText(contentType)) meta.setContentType(contentType);
            try (InputStream in = file.getInputStream()) {
                PutObjectRequest putReq = new PutObjectRequest(bucket, objectKey, in, meta);
                client.putObject(putReq);
            }
            return getPublicUrl(objectKey);
        } catch (Exception e) {
            log.error("BOS upload 失败 key={}", objectKey, e);
            throw new BusinessException(ErrorCode.STORAGE_UPLOAD_FAIL, "上传失败: " + (e.getMessage() != null ? e.getMessage() : "未知错误"));
        }
    }

    @Override
    @Cacheable(value = "storage:url", key = "#key", unless = "#result == null")
    public String getPublicUrl(String key) {
        if (!StringUtils.hasText(key)) return null;
        String cdnDomain = getConfigValue(CONFIG_CDN_DOMAIN);
        if (StringUtils.hasText(cdnDomain)) {
            String domain = cdnDomain.trim().replaceFirst("^https?://", "");
            String k = key.startsWith("/") ? key.substring(1) : key;
            return "https://" + domain + "/" + k;
        }
        String bucket = getConfigValue(CONFIG_BUCKET);
        String region = getConfigValue(CONFIG_REGION);
        if (!StringUtils.hasText(region)) {
            String endpoint = getConfigValue(CONFIG_ENDPOINT);
            if (StringUtils.hasText(endpoint)) {
                int i = endpoint.indexOf("bos.") + 4;
                int j = endpoint.indexOf(".baidubce.com");
                if (i > 3 && j > i) region = endpoint.substring(i, j);
            }
        }
        if (!StringUtils.hasText(region)) region = "bj";
        String k = key.startsWith("/") ? key.substring(1) : key;
        return "https://" + bucket.trim() + "." + region + ".bcebos.com/" + k;
    }

    @Override
    public String uploadBytes(String key, byte[] data, String contentType) {
        if (data == null || data.length == 0) throw new IllegalArgumentException("数据为空");
        String objectKey = StringUtils.hasText(key) ? key.trim() : "upload-" + System.currentTimeMillis();
        BosClient client = null;
        try {
            client = createClient();
            String bucket = getBucket();
            ObjectMetadata meta = new ObjectMetadata();
            meta.setContentLength(data.length);
            if (StringUtils.hasText(contentType)) meta.setContentType(contentType);
            try (InputStream in = new ByteArrayInputStream(data)) {
                PutObjectRequest putReq = new PutObjectRequest(bucket, objectKey, in, meta);
                client.putObject(putReq);
            }
            return getPublicUrl(objectKey);
        } catch (Exception e) {
            log.error("BOS uploadBytes 失败 key={}", objectKey, e);
            throw new BusinessException(ErrorCode.STORAGE_UPLOAD_FAIL, "上传失败: " + (e.getMessage() != null ? e.getMessage() : "未知错误"));
        }
    }

    @Override
    public void deleteObject(String key, Long currentUserId) {
        if (!StringUtils.hasText(key)) return;
        ensureKeyBelongsToUser(key, currentUserId);
        if (!isConfigured()) throw new IllegalStateException("BOS 未配置");
        BosClient client = null;
        try {
            client = createClient();
            client.deleteObject(getBucket(), key.trim());
        } catch (Exception e) {
            log.error("BOS deleteObject 失败 key={}", key, e);
            throw new BusinessException(ErrorCode.STORAGE_DELETE_FAIL, "删除失败: " + (e.getMessage() != null ? e.getMessage() : "未知错误"));
        }
    }

    @Override
    public List<String> listAllObjectKeys(String prefix) {
        if (!isConfigured()) return new ArrayList<>();
        String normalizedPrefix = StringUtils.hasText(prefix) ? prefix.trim() : "";
        if (!normalizedPrefix.isEmpty() && !normalizedPrefix.endsWith("/")) {
            normalizedPrefix += "/";
        }
        List<String> keys = new ArrayList<>();
        BosClient client = null;
        try {
            client = createClient();
            String bucket = getBucket();
            String marker = "";
            while (true) {
                ListObjectsRequest req = new ListObjectsRequest(bucket).withPrefix(normalizedPrefix).withMarker(marker).withMaxKeys(1000);
                ListObjectsResponse resp = client.listObjects(req);
                for (BosObjectSummary sum : resp.getContents()) {
                    keys.add(sum.getKey());
                }
                if (!resp.isTruncated()) break;
                marker = resp.getNextMarker();
            }
        } catch (Exception e) {
            log.warn("BOS listAllObjectKeys 失败 prefix={}", normalizedPrefix, e);
        }
        return keys;
    }

    @Override
    public List<StorageFileVO> listUserFiles(Long currentUserId, String prefixSuffix) {
        String userPrefix = currentUserId + "/";
        if (StringUtils.hasText(prefixSuffix)) {
            userPrefix += prefixSuffix.trim();
            if (!userPrefix.endsWith("/")) userPrefix += "/";
        }
        return listObjects(userPrefix);
    }

    @Override
    public void ensureKeyBelongsToUser(String key, Long currentUserId) {
        if (!StringUtils.hasText(key)) throw new IllegalArgumentException("key 不能为空");
        String normalizedKey = key.trim();
        if (normalizedKey.contains("..")) throw new SecurityException("非法路径：包含 ..");
        String expectedPrefix = currentUserId + "/";
        if (!normalizedKey.startsWith(expectedPrefix)) {
            throw new SecurityException("无权访问此文件：key 不属于当前用户");
        }
    }

    @Override
    public String getPublicUrlForUser(String key, Long currentUserId) {
        ensureKeyBelongsToUser(key, currentUserId);
        return getPublicUrl(key);
    }

    @Override
    public byte[] getObjectBytes(String key) {
        if (!StringUtils.hasText(key)) return new byte[0];
        if (!isConfigured()) return new byte[0];
        BosClient client = null;
        try {
            client = createClient();
            byte[] data = client.getObject(getBucket(), key.trim()).getObjectContent().readAllBytes();
            return data;
        } catch (Exception e) {
            log.warn("BOS getObjectBytes 失败 key={}", key, e);
            return new byte[0];
        }
    }

    @Override
    public byte[] getObjectBytesForUser(String key, Long currentUserId) {
        ensureKeyBelongsToUser(key, currentUserId);
        return getObjectBytes(key);
    }

    @Override
    public String putObjectFromUrl(String key, String sourceUrl) {
        if (!StringUtils.hasText(key)) throw new IllegalArgumentException("key 不能为空");
        if (!StringUtils.hasText(sourceUrl)) throw new IllegalArgumentException("sourceUrl 不能为空");

        // P1-2: SSRF 防护 - 仅允许 HTTP/HTTPS 协议
        String urlLower = sourceUrl.toLowerCase().trim();
        if (!urlLower.startsWith("http://") && !urlLower.startsWith("https://")) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "仅支持 HTTP/HTTPS 协议");
        }

        // P1-2: SSRF 防护 - 禁止访问内网地址
        try {
            URL url = new URL(sourceUrl);
            String host = url.getHost().toLowerCase();

            // 禁止 localhost 和 127.0.0.1
            if (host.equals("localhost") || host.equals("127.0.0.1") || host.startsWith("127.")) {
                throw new BusinessException(ErrorCode.VALIDATION_FAIL, "禁止访问本地地址");
            }

            // 禁止内网 IP 段
            if (host.startsWith("10.") || host.startsWith("192.168.") ||
                host.startsWith("172.16.") || host.startsWith("172.17.") ||
                host.startsWith("172.18.") || host.startsWith("172.19.") ||
                host.startsWith("172.20.") || host.startsWith("172.21.") ||
                host.startsWith("172.22.") || host.startsWith("172.23.") ||
                host.startsWith("172.24.") || host.startsWith("172.25.") ||
                host.startsWith("172.26.") || host.startsWith("172.27.") ||
                host.startsWith("172.28.") || host.startsWith("172.29.") ||
                host.startsWith("172.30.") || host.startsWith("172.31.")) {
                throw new BusinessException(ErrorCode.VALIDATION_FAIL, "禁止访问内网地址");
            }

            // 禁止 169.254.x.x (链路本地地址)
            if (host.startsWith("169.254.")) {
                throw new BusinessException(ErrorCode.VALIDATION_FAIL, "禁止访问链路本地地址");
            }

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "URL 格式错误");
        }

        BosClient client = null;
        try {
            URL url = new URL(sourceUrl);
            URLConnection conn = url.openConnection();
            if (conn instanceof HttpURLConnection http) {
                http.setConnectTimeout(25_000);
                http.setReadTimeout(120_000);
                http.setRequestProperty("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36");
                if (sourceUrl.contains("douyin")) {
                    http.setRequestProperty("Referer", "https://www.douyin.com/");
                }
            }
            byte[] data = conn.getInputStream().readAllBytes();
            String objectKey = key.trim();
            client = createClient();
            String bucket = getBucket();
            ObjectMetadata meta = new ObjectMetadata();
            meta.setContentLength(data.length);
            try (InputStream in = new ByteArrayInputStream(data)) {
                PutObjectRequest putReq = new PutObjectRequest(bucket, objectKey, in, meta);
                client.putObject(putReq);
            }
            return getPublicUrl(objectKey);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            // P1-3: 敏感信息日志脱敏 - 不记录完整 URL
            String maskedUrl = sourceUrl.length() > 50 ? sourceUrl.substring(0, 50) + "..." : sourceUrl;
            log.error("BOS putObjectFromUrl 失败 key={}, sourceUrl={}", key, maskedUrl, e);
            throw new BusinessException(ErrorCode.STORAGE_UPLOAD_FAIL, "从 URL 上传失败: " + (e.getMessage() != null ? e.getMessage() : "未知错误"));
        }
    }
}
