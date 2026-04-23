package cn.gaifan.douyinOperations.module.storage.service;

import cn.gaifan.douyinOperations.module.storage.vo.StorageFileVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 百度云 BOS 存储服务：列表、上传、删除、获取 CDN/公网 URL
 */
public interface BosStorageService {

    /**
     * 是否已配置 BOS（endpoint、bucket、AK/SK 均有效）
     */
    boolean isConfigured();

    /**
     * 列出指定前缀下的对象（含“目录”占位），返回可访问 URL
     *
     * @param prefix 前缀，如 "uploads/" 或 "" 表示根，null 视为 ""
     * @return 文件列表（含目录占位），未配置返回空列表
     */
    List<StorageFileVO> listObjects(String prefix);

    /**
     * 递归列出前缀下所有文件（不含目录占位，用于批量删除）
     */
    List<String> listAllObjectKeys(String prefix);

    /**
     * 上传文件到指定 key
     *
     * @param key  对象 key，如 "uploads/2025/01/xxx.jpg"
     * @param file 上传文件
     * @return 上传成功后的公网/CDN URL
     */
    String upload(String key, MultipartFile file);

    /**
     * 上传字节数组到指定 key（用于 Excel 嵌入图片等）
     *
     * @param key         对象 key
     * @param data        文件内容
     * @param contentType 如 image/png
     * @return 上传成功后的公网/CDN URL，未配置 BOS 返回 null
     */
    String uploadBytes(String key, byte[] data, String contentType);

    /**
     * 删除指定 key 的对象（校验归属，用户只能删除自己路径下的文件）
     *
     * @param key          对象 key
     * @param currentUserId 当前用户 ID
     */
    void deleteObject(String key, Long currentUserId);

    /**
     * 列出当前用户路径下的文件（prefix 强制为 {currentUserId}/，禁止跨用户列表）
     *
     * @param currentUserId 当前用户 ID
     * @param prefixSuffix  prefix 后缀，如 "2026-03-01/5001/keyframes"，空则列出用户根目录
     * @return 文件列表（含目录占位），未配置返回空列表
     */
    List<StorageFileVO> listUserFiles(Long currentUserId, String prefixSuffix);

    /**
     * 校验 key 是否属于当前用户（必须以 {currentUserId}/ 开头，禁止 .. 路径穿越）
     *
     * @param key          对象 key
     * @param currentUserId 当前用户 ID
     */
    void ensureKeyBelongsToUser(String key, Long currentUserId);

    /**
     * 获取对象的公网/CDN 访问 URL（校验归属后返回，用户只能获取自己路径下的文件 URL）
     *
     * @param key          对象 key
     * @param currentUserId 当前用户 ID
     * @return 可直接外链的 URL，未配置 CDN 时使用 BOS 默认域名
     */
    String getPublicUrlForUser(String key, Long currentUserId);

    /**
     * 获取对象的公网/CDN 访问 URL（不校验归属，仅内部使用，如上传成功后返回）
     *
     * @param key 对象 key
     * @return 可直接外链的 URL
     */
    String getPublicUrl(String key);

    /**
     * 获取对象内容为字节数组（内部使用，需先校验归属）
     */
    byte[] getObjectBytes(String key);

    /**
     * 获取对象内容为字节数组（校验归属后返回，用于参考图缓存等）
     */
    byte[] getObjectBytesForUser(String key, Long currentUserId);

    /**
     * 从 URL 拉取文件并上传到 BOS（回源拉取，避免下载-上传双倍流量）
     *
     * @param key       目标 BOS key
     * @param sourceUrl 源文件 URL（http/https）
     * @return 上传成功后的公网/CDN URL
     */
    String putObjectFromUrl(String key, String sourceUrl);
}
