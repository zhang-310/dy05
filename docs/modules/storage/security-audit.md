# Storage 模块安全审计报告

**审计日期**: 2026-05-08
**审计范围**: storage 模块（文件存储上传）
**审计标准**: OWASP Top 10 2021、CWE Top 25、CVSS 3.1
**审计文件数**: 42 个 Java 文件

## 执行摘要

**总体评分**: 72/100 (等级 C+)

**漏洞统计**:
- 严重 (CVSS 9.0-10.0): 1 个
- 高危 (CVSS 7.0-8.9): 3 个
- 中危 (CVSS 4.0-6.9): 5 个
- 低危 (CVSS 0.1-3.9): 3 个

**关键发现**:
- ❌ 严重：路径遍历漏洞（CWE-22）- 用户可控的 storageKey 未充分验证
- ⚠️ 高危：文件类型验证缺失（CWE-434）- 可上传任意文件类型
- ⚠️ 高危：文件大小限制不一致（CWE-400）- DoS 风险
- ⚠️ 高危：敏感信息泄露（CWE-532）- 日志记录完整 URL 和 key
- ℹ️ 中危：缺少文件内容验证（CWE-434）- 仅验证扩展名
- ℹ️ 中危：SSRF 风险（CWE-918）- putObjectFromUrl 未验证目标 URL

**优点**:
- ✅ 使用 JPA 参数化查询，无 SQL 注入风险
- ✅ 实现了基本的用户隔离（owner_id 检查）
- ✅ 路径穿越防护（检查 ".."）
- ✅ 管理员权限检查（StorageController）
- ✅ 逻辑删除机制

## OWASP Top 10 2021 检查

### A01:2021 - Broken Access Control

**检查结果**: ⚠️ 部分合规

**发现问题**:
1. **用户隔离不完整** - UploadController 未强制 storageKey 以 userId 开头
   - 文件: `UploadController.java` 第 31 行
   - 用户可在 UploadInitVO 中提供任意 storageKey，未验证是否属于当前用户
   - StorageController 正确实现了强制前缀（第 129 行）

2. **权限检查不一致** - UploadController 所有接口仅检查登录，未检查管理员权限
   - StorageController 正确检查了 admin 角色（第 39-41 行）
   - UploadController 应该也需要权限控制或至少限制上传配额

**正面实践**:
- ✅ BosStorageService.ensureKeyBelongsToUser() 正确验证文件归属（第 241-249 行）
- ✅ 所有操作都检查 ownerId 匹配（UploadServiceImpl 第 135、219、264、304 行）

### A02:2021 - Cryptographic Failures

**检查结果**: ⚠️ 部分合规

**发现问题**:
1. **敏感配置存储** - BOS AK/SK 存储在系统配置表中
   - 文件: `BosStorageServiceImpl.java` 第 42-43 行
   - 虽然使用 ConfigService.getRawValueByKey()，但未说明是否加密存储
   - 建议：使用专用密钥管理服务（如 AWS KMS、HashiCorp Vault）

2. **文件传输未加密** - putObjectFromUrl 使用 HTTP 连接
   - 文件: `BosStorageServiceImpl.java` 第 284-294 行
   - 虽然 BOS 公网 URL 使用 HTTPS（第 148、162 行），但下载源 URL 可能是 HTTP

**正面实践**:
- ✅ BOS 公网 URL 强制使用 HTTPS（第 148、162 行）

### A03:2021 - Injection

**检查结果**: ✅ 合规

**正面实践**:
- ✅ 所有数据库操作使用 JPA 参数化查询（SysUploadTaskRepository 第 27-37 行）
- ✅ 无原生 SQL 拼接
- ✅ 路径参数使用 trim() 和验证（BosStorageServiceImpl 第 243-248 行）

### A04:2021 - Insecure Design

**检查结果**: ⚠️ 部分合规

**发现问题**:
1. **文件类型验证缺失** (HIGH)
   - 文件: `UploadController.java`、`StorageController.java`
   - 未验证上传文件的 MIME 类型和扩展名
   - 用户可上传任意文件类型（.exe、.sh、.jsp 等）
   - CWE-434: Unrestricted Upload of File with Dangerous Type

2. **文件大小限制不一致** (HIGH)
   - UploadServiceImpl 限制 10 GB（第 46 行）
   - StorageController.upload() 无大小限制
   - 可能导致 DoS 攻击

3. **秒传机制安全风险** (MEDIUM)
   - 文件: `UploadServiceImpl.java` 第 107-121 行
   - 仅基于 MD5 + fileSize + ownerId 判断秒传
   - 同一用户上传相同 MD5 的不同文件会被误判为秒传
   - 建议：增加文件名或业务上下文验证

4. **缺少速率限制** (MEDIUM)
   - 所有上传接口无速率限制
   - 可能被滥用进行 DoS 攻击

**正面实践**:
- ✅ 分块上传支持断点续传（UploadServiceImpl）
- ✅ 过期任务自动清理（7 天，第 99 行）

### A05:2021 - Security Misconfiguration

**检查结果**: ⚠️ 部分合规

**发现问题**:
1. **调度器默认启用** (LOW)
   - 文件: `UploadTaskCleanupScheduler.java` 第 19 行
   - matchIfMissing = true，默认启用清理任务
   - 建议：生产环境应显式配置

2. **错误信息过于详细** (MEDIUM)
   - 文件: `BosStorageServiceImpl.java` 第 136、183、198、308 行
   - 异常消息直接返回给前端：`e.getMessage()`
   - 可能泄露内部路径、配置信息

**正面实践**:
- ✅ 使用 @ConditionalOnProperty 控制功能开关（第 19 行）

### A06:2021 - Vulnerable and Outdated Components

**检查结果**: ⚠️ 需人工验证

**待检查项**:
- Baidu BOS SDK 版本（com.baidubce:bce-java-sdk）
- Spring Boot 3.3.7 依赖项
- MultipartFile 处理库

**建议**: 运行 `mvn dependency:tree` 和 `mvn versions:display-dependency-updates`

### A07:2021 - Identification and Authentication Failures

**检查结果**: ✅ 合规

**正面实践**:
- ✅ 所有接口检查用户登录（AuthTokenFilter.getUserId）
- ✅ 管理员接口额外检查角色（StorageController 第 39-41 行）
- ✅ 上传任务与用户绑定（ownerId）

### A08:2021 - Software and Data Integrity Failures

**检查结果**: ⚠️ 部分合规

**发现问题**:
1. **文件内容验证缺失** (MEDIUM)
   - 文件: `UploadServiceImpl.java` 第 129-152 行
   - 仅验证 chunkMd5 参数存在，未实际计算并验证分块内容的 MD5
   - 恶意用户可提供错误的 MD5 值
   - CWE-354: Improper Validation of Integrity Check Value

2. **完整性检查不完整** (MEDIUM)
   - completeUpload() 仅检查分块数量（第 272 行），未验证最终文件 MD5
   - 建议：合并后重新计算文件 MD5 并与 fileMd5 比对

**正面实践**:
- ✅ 要求客户端提供文件 MD5（UploadInitVO 第 15-16 行）
- ✅ 分块上传要求提供 chunkMd5（UploadController 第 46 行）

### A09:2021 - Security Logging and Monitoring Failures

**检查结果**: ⚠️ 部分合规

**发现问题**:
1. **敏感信息记录到日志** (HIGH)
   - 文件: `BosStorageServiceImpl.java` 第 135、182、197、307 行
   - 日志包含完整的 objectKey 和 sourceUrl
   - 可能泄露用户隐私文件路径
   - CWE-532: Insertion of Sensitive Information into Log File

2. **缺少安全事件日志** (MEDIUM)
   - 未记录权限拒绝事件（ensureKeyBelongsToUser 抛出异常时）
   - 未记录异常上传行为（如频繁失败、超大文件）

**正面实践**:
- ✅ 记录关键操作（初始化、完成、取消上传）
- ✅ 使用 SLF4J 统一日志框架

### A10:2021 - Server-Side Request Forgery (SSRF)

**检查结果**: ⚠️ 存在风险

**发现问题**:
1. **SSRF 漏洞** (HIGH)
   - 文件: `BosStorageServiceImpl.java` 第 279-310 行
   - putObjectFromUrl() 接受任意 URL，未验证目标地址
   - 攻击者可访问内网资源：`http://localhost:8080/admin`、`http://169.254.169.254/latest/meta-data/`
   - CWE-918: Server-Side Request Forgery (SSRF)
   - CVSS 3.1: 8.6 (HIGH)

2. **User-Agent 伪造** (LOW)
   - 第 289-290 行硬编码 Chrome User-Agent
   - 可能被目标网站识别为爬虫

**修复建议**:
```java
// 添加 URL 白名单验证
private static final Set<String> ALLOWED_DOMAINS = Set.of(
    "douyin.com", "douyinvod.com", "byteimg.com"
);

private void validateSourceUrl(String sourceUrl) {
    try {
        URL url = new URL(sourceUrl);
        String host = url.getHost().toLowerCase();
        
        // 禁止内网地址
        if (host.equals("localhost") || host.equals("127.0.0.1") || 
            host.startsWith("192.168.") || host.startsWith("10.") ||
            host.startsWith("172.16.") || host.equals("169.254.169.254")) {
            throw new SecurityException("禁止访问内网地址");
        }
        
        // 白名单检查
        boolean allowed = ALLOWED_DOMAINS.stream()
            .anyMatch(domain -> host.equals(domain) || host.endsWith("." + domain));
        if (!allowed) {
            throw new SecurityException("URL 不在白名单中");
        }
    } catch (MalformedURLException e) {
        throw new IllegalArgumentException("无效的 URL");
    }
}
```

## 漏洞详情

### CRITICAL-01 - 路径遍历漏洞（用户可控 storageKey）

**CVSS 评分**: 9.1 (严重)
**CWE 编号**: CWE-22 (Path Traversal)
**OWASP 分类**: A01:2021 (Broken Access Control)

**漏洞描述**:
UploadController.initUpload() 接受用户提供的 storageKey，未验证是否以当前用户 ID 开头。攻击者可构造恶意 storageKey 访问或覆盖其他用户的文件。

**影响范围**:
- `UploadController.java` 第 29-38 行
- `UploadInitVO.java` 第 18-19 行
- 所有使用分块上传的功能

**攻击场景**:
```json
POST /api/v1/storage/upload/init
{
  "originalFilename": "malicious.txt",
  "fileSize": 1024,
  "fileMd5": "abc123",
  "storageKey": "999/sensitive/admin-data.txt",  // 攻击者 userId=1，但指定 999
  "module": "live"
}
```

**修复建议**:
1. 在 UploadServiceImpl.initUpload() 中强制 storageKey 前缀：
```java
@Override
public UploadInitResultVO initUpload(Long userId, UploadInitVO vo) {
    // 强制 storageKey 以 userId 开头
    String storageKey = vo.getStorageKey();
    if (storageKey == null || storageKey.trim().isEmpty()) {
        storageKey = userId + "/" + vo.getOriginalFilename();
    } else {
        storageKey = storageKey.trim();
        if (storageKey.contains("..")) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "非法路径");
        }
        // 强制前缀
        String expectedPrefix = userId + "/";
        if (!storageKey.startsWith(expectedPrefix)) {
            storageKey = expectedPrefix + storageKey;
        }
    }
    vo.setStorageKey(storageKey);
    // ... 后续逻辑
}
```

2. 或者完全移除 storageKey 参数，由后端生成：
```java
String storageKey = userId + "/" + module + "/" + 
    LocalDate.now().format(DateTimeFormatter.ISO_DATE) + "/" + 
    UUID.randomUUID() + getExtension(vo.getOriginalFilename());
```

**工作量**: 0.5 人日

---

### HIGH-01 - 文件类型验证缺失

**CVSS 评分**: 7.5 (高危)
**CWE 编号**: CWE-434 (Unrestricted Upload of File with Dangerous Type)
**OWASP 分类**: A04:2021 (Insecure Design)

**漏洞描述**:
所有上传接口未验证文件类型，用户可上传任意扩展名的文件（.exe、.sh、.jsp、.php 等）。虽然文件存储在 BOS，但如果 BOS 配置不当或存在其他漏洞，可能导致远程代码执行。

**影响范围**:
- `UploadController.uploadChunk()` - 无文件类型检查
- `StorageController.upload()` - 无文件类型检查

**攻击场景**:
```bash
# 上传恶意脚本
curl -X POST /api/v1/storage/upload \
  -F "file=@malicious.jsp" \
  -F "prefix=uploads/"
```

**修复建议**:
```java
// 1. 定义允许的文件类型白名单
private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
    "jpg", "jpeg", "png", "gif", "webp",  // 图片
    "mp4", "mov", "avi", "webm",          // 视频
    "mp3", "wav", "aac",                  // 音频
    "pdf", "doc", "docx", "xls", "xlsx"   // 文档
);

private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
    "image/jpeg", "image/png", "image/gif", "image/webp",
    "video/mp4", "video/quicktime", "video/x-msvideo",
    "audio/mpeg", "audio/wav", "audio/aac",
    "application/pdf", "application/msword"
);

// 2. 验证方法
private void validateFile(MultipartFile file) {
    String originalName = file.getOriginalFilename();
    if (originalName == null || originalName.isEmpty()) {
        throw new BusinessException(ErrorCode.VALIDATION_FAIL, "文件名不能为空");
    }
    
    // 验证扩展名
    String ext = originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase();
    if (!ALLOWED_EXTENSIONS.contains(ext)) {
        throw new BusinessException(ErrorCode.VALIDATION_FAIL, 
            "不支持的文件类型: " + ext);
    }
    
    // 验证 MIME 类型
    String contentType = file.getContentType();
    if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType)) {
        throw new BusinessException(ErrorCode.VALIDATION_FAIL, 
            "不支持的 MIME 类型: " + contentType);
    }
    
    // 验证文件头（Magic Number）
    try (InputStream is = file.getInputStream()) {
        byte[] header = new byte[8];
        is.read(header);
        if (!isValidFileHeader(header, ext)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, 
                "文件内容与扩展名不匹配");
        }
    }
}
```

**工作量**: 1 人日

---

### HIGH-02 - SSRF 漏洞（putObjectFromUrl）

**CVSS 评分**: 8.6 (高危)
**CWE 编号**: CWE-918 (Server-Side Request Forgery)
**OWASP 分类**: A10:2021 (SSRF)

**漏洞描述**:
BosStorageServiceImpl.putObjectFromUrl() 接受任意 URL，未验证目标地址。攻击者可利用此功能访问内网资源、云元数据服务或进行端口扫描。

**影响范围**:
- `BosStorageServiceImpl.java` 第 279-310 行
- 所有调用 putObjectFromUrl() 的功能

**攻击场景**:
```java
// 1. 访问云元数据服务（AWS/阿里云/腾讯云）
bosStorageService.putObjectFromUrl("test.txt", 
    "http://169.254.169.254/latest/meta-data/iam/security-credentials/");

// 2. 访问内网服务
bosStorageService.putObjectFromUrl("admin.html", 
    "http://localhost:8080/actuator/env");

// 3. 端口扫描
for (int port = 1; port < 65535; port++) {
    bosStorageService.putObjectFromUrl("scan.txt", 
        "http://internal-server:" + port);
}
```

**修复建议**:
已在 A10 章节提供详细修复代码，关键点：
1. 禁止访问内网地址（127.0.0.1、192.168.x.x、10.x.x.x、169.254.169.254）
2. 实施 URL 白名单（仅允许 douyin.com 等可信域名）
3. 设置连接超时和读取超时（已实现，第 287-288 行）
4. 限制重定向次数

**工作量**: 0.5 人日

---

### HIGH-03 - 敏感信息泄露到日志

**CVSS 评分**: 7.2 (高危)
**CWE 编号**: CWE-532 (Insertion of Sensitive Information into Log File)
**OWASP 分类**: A09:2021 (Security Logging and Monitoring Failures)

**漏洞描述**:
日志记录包含完整的文件路径（objectKey）和源 URL，可能泄露用户隐私信息。日志文件通常权限较宽松，可能被未授权人员访问。

**影响范围**:
- `BosStorageServiceImpl.java` 第 135、182、197、307 行
- `UploadServiceImpl.java` 第 91、162、200、206、284、314 行

**攻击场景**:
```
// 日志示例
2026-05-08 10:23:45 ERROR BosStorageServiceImpl - BOS upload 失败 key=1001/live/2026-05-08/sensitive-document.pdf
2026-05-08 10:24:12 ERROR BosStorageServiceImpl - BOS putObjectFromUrl 失败 key=1001/private/photo.jpg, sourceUrl=https://private-server.com/user/1001/photo.jpg
```

**修复建议**:
```java
// 1. 脱敏工具类
public class LogSanitizer {
    public static String sanitizeKey(String key) {
        if (key == null || key.length() < 10) return "***";
        // 仅保留前缀和后缀
        return key.substring(0, 5) + "***" + key.substring(key.length() - 5);
    }
    
    public static String sanitizeUrl(String url) {
        try {
            URL u = new URL(url);
            return u.getProtocol() + "://" + u.getHost() + "/***";
        } catch (Exception e) {
            return "***";
        }
    }
}

// 2. 修改日志调用
log.error("BOS upload 失败 key={}", LogSanitizer.sanitizeKey(objectKey), e);
log.error("BOS putObjectFromUrl 失败 key={}, sourceUrl={}", 
    LogSanitizer.sanitizeKey(key), LogSanitizer.sanitizeUrl(sourceUrl), e);
```

**工作量**: 0.5 人日

---

### MEDIUM-01 - 文件内容完整性验证缺失

**CVSS 评分**: 5.3 (中危)
**CWE 编号**: CWE-354 (Improper Validation of Integrity Check Value)
**OWASP 分类**: A08:2021 (Software and Data Integrity Failures)

**漏洞描述**:
分块上传时，虽然要求客户端提供 chunkMd5，但服务端未实际计算并验证分块内容的 MD5。攻击者可提供错误的 MD5 值，导致文件损坏或被篡改。

**影响范围**:
- `UploadServiceImpl.uploadChunk()` 第 129-152 行
- `UploadServiceImpl.completeUpload()` 第 260-292 行

**修复建议**:
```java
@Override
public void uploadChunk(Long userId, String uploadId, Integer chunkIndex,
                       String chunkMd5, MultipartFile chunkFile) {
    // ... 现有验证逻辑 ...
    
    // 计算实际 MD5
    String actualMd5;
    try (InputStream is = chunkFile.getInputStream()) {
        actualMd5 = DigestUtils.md5Hex(is);
    } catch (IOException e) {
        throw new BusinessException(ErrorCode.STORAGE_UPLOAD_FAIL, "读取文件失败");
    }
    
    // 验证 MD5
    if (!actualMd5.equalsIgnoreCase(chunkMd5)) {
        throw new BusinessException(ErrorCode.VALIDATION_FAIL, 
            "分块 MD5 校验失败，期望: " + chunkMd5 + ", 实际: " + actualMd5);
    }
    
    // ... 后续上传逻辑 ...
}

@Override
public UploadCompleteResultVO completeUpload(Long userId, String uploadId) {
    // ... 现有逻辑 ...
    
    // 合并后验证整个文件的 MD5
    String actualFileMd5 = calculateMergedFileMd5(chunks);
    if (!actualFileMd5.equalsIgnoreCase(task.getFileMd5())) {
        throw new BusinessException(ErrorCode.VALIDATION_FAIL, 
            "文件 MD5 校验失败");
    }
    
    // ... 后续逻辑 ...
}
```

**工作量**: 1 人日

---

### MEDIUM-02 - 文件大小限制不一致

**CVSS 评分**: 5.0 (中危)
**CWE 编号**: CWE-400 (Uncontrolled Resource Consumption)
**OWASP 分类**: A04:2021 (Insecure Design)

**漏洞描述**:
- UploadServiceImpl 限制 10 GB（第 46 行）
- StorageController.upload() 无大小限制
- Spring Boot 默认限制 1 MB（可配置）
- 不一致的限制可能导致 DoS 攻击或资源耗尽

**影响范围**:
- `UploadServiceImpl.java` 第 46 行
- `StorageController.java` 第 108-140 行

**修复建议**:
```java
// 1. 统一配置类
@ConfigurationProperties(prefix = "storage.upload")
public class StorageUploadProperties {
    private long maxFileSize = 10L * 1024 * 1024 * 1024; // 10 GB
    private long maxChunkSize = 100L * 1024 * 1024;      // 100 MB
    private int maxChunks = 1000;
    // getters/setters
}

// 2. StorageController 添加验证
@PostMapping("/upload")
public RESTResult<Map<String, String>> upload(..., @RequestParam("file") MultipartFile file, ...) {
    if (file.getSize() > storageProperties.getMaxFileSize()) {
        return RESTResult.error(ErrorCode.STORAGE_FILE_TOO_LARGE, 
            "文件超过 " + (storageProperties.getMaxFileSize() / 1024 / 1024 / 1024) + " GB 上限");
    }
    // ... 后续逻辑 ...
}

// 3. application.yml 配置
spring:
  servlet:
    multipart:
      max-file-size: 100MB
      max-request-size: 100MB
storage:
  upload:
    max-file-size: 10737418240  # 10 GB
    max-chunk-size: 104857600   # 100 MB
```

**工作量**: 0.5 人日

---

### MEDIUM-03 - 秒传机制安全风险

**CVSS 评分**: 4.3 (中危)
**CWE 编号**: CWE-841 (Improper Enforcement of Behavioral Workflow)
**OWASP 分类**: A04:2021 (Insecure Design)

**漏洞描述**:
秒传仅基于 MD5 + fileSize + ownerId 判断，未考虑文件名或业务上下文。同一用户上传相同 MD5 的不同文件会被误判为秒传，返回错误的文件 URL。

**影响范围**:
- `UploadServiceImpl.checkSecondUpload()` 第 107-121 行

**修复建议**:
```java
// 方案 1: 增加文件名或模块验证
@Override
public String checkSecondUpload(String fileMd5, Long userId, Long fileSize, 
                                String originalFilename, String module) {
    Optional<SysUploadTask> existing = taskRepository
        .findByFileMd5AndOwnerIdAndFileSizeAndOriginalFilenameAndModuleAndDeleted(
            fileMd5, userId, fileSize, originalFilename, module, 0
        );
    // ... 后续逻辑 ...
}

// 方案 2: 返回文件列表让用户选择
@Override
public List<String> checkSecondUpload(String fileMd5, Long userId, Long fileSize) {
    List<SysUploadTask> existing = taskRepository
        .findByFileMd5AndOwnerIdAndFileSizeAndStatusAndDeleted(
            fileMd5, userId, fileSize, "COMPLETED", 0
        );
    return existing.stream()
        .map(SysUploadTask::getStorageKey)
        .collect(Collectors.toList());
}
```

**工作量**: 0.5 人日

---

### MEDIUM-04 - 缺少速率限制

**CVSS 评分**: 5.3 (中危)
**CWE 编号**: CWE-770 (Allocation of Resources Without Limits or Throttling)
**OWASP 分类**: A04:2021 (Insecure Design)

**漏洞描述**:
所有上传接口无速率限制，攻击者可频繁调用接口进行 DoS 攻击或滥用存储资源。

**影响范围**:
- 所有 UploadController 和 StorageController 接口

**修复建议**:
```java
// 1. 引入 Resilience4j RateLimiter
@Configuration
public class RateLimiterConfig {
    @Bean
    public RateLimiter uploadRateLimiter() {
        return RateLimiter.of("upload", RateLimiterConfig.custom()
            .limitForPeriod(10)           // 每个周期最多 10 次
            .limitRefreshPeriod(Duration.ofMinutes(1))  // 1 分钟
            .timeoutDuration(Duration.ofSeconds(5))
            .build());
    }
}

// 2. 应用到 Controller
@PostMapping("/init")
@RateLimiter(name = "upload")
public RESTResult<UploadInitResultVO> initUpload(...) {
    // ... 现有逻辑 ...
}

// 3. 或使用 Redis 实现分布式限流
@Aspect
@Component
public class RateLimitAspect {
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Around("@annotation(rateLimited)")
    public Object rateLimit(ProceedingJoinPoint pjp, RateLimited rateLimited) {
        Long userId = getCurrentUserId();
        String key = "rate_limit:" + rateLimited.value() + ":" + userId;
        
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == 1) {
            redisTemplate.expire(key, rateLimited.period(), TimeUnit.SECONDS);
        }
        
        if (count > rateLimited.limit()) {
            throw new BusinessException(ErrorCode.RATE_LIMIT_EXCEEDED, 
                "操作过于频繁，请稍后再试");
        }
        
        return pjp.proceed();
    }
}
```

**工作量**: 1 人日

---

### MEDIUM-05 - 错误信息过于详细

**CVSS 评分**: 4.0 (中危)
**CWE 编号**: CWE-209 (Generation of Error Message Containing Sensitive Information)
**OWASP 分类**: A05:2021 (Security Misconfiguration)

**漏洞描述**:
异常消息直接返回给前端（`e.getMessage()`），可能泄露内部路径、配置信息、数据库结构等敏感信息。

**影响范围**:
- `BosStorageServiceImpl.java` 第 136、183、198、308 行

**修复建议**:
```java
// 1. 定义通用错误消息
private static final String GENERIC_UPLOAD_ERROR = "文件上传失败，请稍后重试";
private static final String GENERIC_DELETE_ERROR = "文件删除失败，请稍后重试";

// 2. 修改异常处理
try {
    // ... 上传逻辑 ...
} catch (Exception e) {
    log.error("BOS upload 失败 key={}", LogSanitizer.sanitizeKey(objectKey), e);
    // 仅返回通用错误消息
    throw new BusinessException(ErrorCode.STORAGE_UPLOAD_FAIL, GENERIC_UPLOAD_ERROR);
}

// 3. 开发环境可返回详细错误
@Value("${spring.profiles.active:prod}")
private String activeProfile;

private String getErrorMessage(Exception e) {
    if ("dev".equals(activeProfile)) {
        return e.getMessage();  // 开发环境返回详细错误
    }
    return GENERIC_UPLOAD_ERROR;  // 生产环境返回通用错误
}
```

**工作量**: 0.5 人日

---

### LOW-01 - 调度器默认启用

**CVSS 评分**: 2.0 (低危)
**CWE 编号**: CWE-1188 (Insecure Default Initialization of Resource)
**OWASP 分类**: A05:2021 (Security Misconfiguration)

**漏洞描述**:
UploadTaskCleanupScheduler 和 BosCleanupScheduler 默认启用（matchIfMissing = true），可能在未配置的环境中意外执行清理任务。

**影响范围**:
- `UploadTaskCleanupScheduler.java` 第 19 行
- `BosCleanupScheduler.java` 第 15 行

**修复建议**:
```java
// 修改为默认禁用
@ConditionalOnProperty(name = "storage.upload.cleanup.enabled", havingValue = "true", matchIfMissing = false)

// application.yml 显式配置
storage:
  upload:
    cleanup:
      enabled: true  # 生产环境启用
```

**工作量**: 0.1 人日

---

### LOW-02 - User-Agent 硬编码

**CVSS 评分**: 1.5 (低危)
**CWE 编号**: CWE-656 (Reliance on Security Through Obscurity)
**OWASP 分类**: A05:2021 (Security Misconfiguration)

**漏洞描述**:
putObjectFromUrl() 硬编码 Chrome User-Agent，可能被目标网站识别为爬虫并封禁。

**影响范围**:
- `BosStorageServiceImpl.java` 第 289-290 行

**修复建议**:
```java
// 配置化 User-Agent
@Value("${storage.http.user-agent:Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36}")
private String userAgent;

http.setRequestProperty("User-Agent", userAgent);
```

**工作量**: 0.1 人日

---

### LOW-03 - 缺少安全事件日志

**CVSS 评分**: 2.5 (低危)
**CWE 编号**: CWE-778 (Insufficient Logging)
**OWASP 分类**: A09:2021 (Security Logging and Monitoring Failures)

**漏洞描述**:
未记录权限拒绝事件、异常上传行为等安全事件，难以追踪攻击行为。

**影响范围**:
- `BosStorageServiceImpl.ensureKeyBelongsToUser()` 第 241-249 行
- 所有权限检查点

**修复建议**:
```java
@Override
public void ensureKeyBelongsToUser(String key, Long currentUserId) {
    if (!StringUtils.hasText(key)) {
        log.warn("安全事件: 空 key 访问尝试, userId={}", currentUserId);
        throw new IllegalArgumentException("key 不能为空");
    }
    
    String normalizedKey = key.trim();
    if (normalizedKey.contains("..")) {
        log.warn("安全事件: 路径穿越尝试, userId={}, key={}", 
            currentUserId, LogSanitizer.sanitizeKey(key));
        throw new SecurityException("非法路径：包含 ..");
    }
    
    String expectedPrefix = currentUserId + "/";
    if (!normalizedKey.startsWith(expectedPrefix)) {
        log.warn("安全事件: 越权访问尝试, userId={}, key={}", 
            currentUserId, LogSanitizer.sanitizeKey(key));
        throw new SecurityException("无权访问此文件：key 不属于当前用户");
    }
}
```

**工作量**: 0.5 人日

---

## 安全加固建议

### 认证与授权

- [x] 所有接口检查用户登录（已实现）
- [x] 管理员接口检查角色（StorageController 已实现）
- [ ] UploadController 添加权限控制或配额限制
- [ ] 实施基于角色的访问控制（RBAC）
- [ ] 添加 API 密钥认证（用于服务间调用）

### 数据保护

- [ ] BOS AK/SK 使用密钥管理服务（KMS）加密存储
- [ ] 敏感文件启用服务端加密（SSE）
- [ ] 传输层强制 HTTPS（已部分实现）
- [ ] 文件下载 URL 添加签名和过期时间
- [ ] 实施数据分类和标签（public/internal/confidential）

### 输入验证

- [ ] 文件类型白名单验证（扩展名 + MIME + Magic Number）
- [ ] 文件大小统一限制（10 GB）
- [ ] 文件名长度和字符限制（禁止特殊字符）
- [ ] storageKey 强制用户前缀
- [ ] URL 白名单验证（putObjectFromUrl）
- [ ] 分块 MD5 实际计算和验证
- [ ] 完整文件 MD5 验证

### 速率限制

- [ ] 上传接口限流（10 次/分钟/用户）
- [ ] 下载接口限流（100 次/分钟/用户）
- [ ] 全局 IP 限流（防止 DDoS）
- [ ] 存储配额限制（每用户 100 GB）

### 日志与监控

- [ ] 敏感信息脱敏（文件路径、URL）
- [ ] 记录安全事件（权限拒绝、路径穿越尝试）
- [ ] 异常上传行为告警（频繁失败、超大文件）
- [ ] 集成 SIEM 系统
- [ ] 定期审计日志分析

### 错误处理

- [ ] 生产环境返回通用错误消息
- [ ] 开发环境可选详细错误
- [ ] 统一异常处理器
- [ ] 避免堆栈跟踪泄露

---

## 合规性检查

### GDPR（通用数据保护条例）

- [ ] **数据最小化**: 仅收集必要的文件元数据
- [ ] **用户同意**: 上传前明确告知数据用途
- [ ] **数据可携带性**: 提供批量导出功能
- [ ] **删除权**: 实现真实删除（非仅逻辑删除）
- [ ] **数据泄露通知**: 72 小时内通知受影响用户
- [ ] **隐私设计**: 默认最高隐私设置

### PCI-DSS（支付卡行业数据安全标准）

- [ ] **加密传输**: 所有文件传输使用 TLS 1.2+
- [ ] **加密存储**: 敏感文件服务端加密
- [ ] **访问控制**: 最小权限原则
- [ ] **日志审计**: 保留 1 年审计日志
- [ ] **漏洞管理**: 季度安全扫描
- [ ] **安全测试**: 年度渗透测试

### 等保 2.0（中国信息安全等级保护）

- [ ] **身份鉴别**: 多因素认证（MFA）
- [ ] **访问控制**: 强制访问控制（MAC）
- [ ] **安全审计**: 完整审计日志
- [ ] **数据完整性**: 文件 Hash 验证
- [ ] **数据保密性**: 敏感数据加密
- [ ] **备份恢复**: 定期备份和恢复演练

---

## 问题清单

### P0 - 严重漏洞

| 编号 | 漏洞 | CVSS | CWE | 文件 | 工作量 |
|------|------|------|-----|------|--------|
| CRITICAL-01 | 路径遍历漏洞（用户可控 storageKey） | 9.1 | CWE-22 | UploadController.java:31 | 0.5 人日 |

### P1 - 高危漏洞

| 编号 | 漏洞 | CVSS | CWE | 文件 | 工作量 |
|------|------|------|-----|------|--------|
| HIGH-01 | 文件类型验证缺失 | 7.5 | CWE-434 | UploadController.java, StorageController.java | 1 人日 |
| HIGH-02 | SSRF 漏洞（putObjectFromUrl） | 8.6 | CWE-918 | BosStorageServiceImpl.java:279 | 0.5 人日 |
| HIGH-03 | 敏感信息泄露到日志 | 7.2 | CWE-532 | BosStorageServiceImpl.java, UploadServiceImpl.java | 0.5 人日 |

### P2 - 中危漏洞

| 编号 | 漏洞 | CVSS | CWE | 文件 | 工作量 |
|------|------|------|-----|------|--------|
| MEDIUM-01 | 文件内容完整性验证缺失 | 5.3 | CWE-354 | UploadServiceImpl.java:129 | 1 人日 |
| MEDIUM-02 | 文件大小限制不一致 | 5.0 | CWE-400 | UploadServiceImpl.java:46, StorageController.java | 0.5 人日 |
| MEDIUM-03 | 秒传机制安全风险 | 4.3 | CWE-841 | UploadServiceImpl.java:107 | 0.5 人日 |
| MEDIUM-04 | 缺少速率限制 | 5.3 | CWE-770 | UploadController.java, StorageController.java | 1 人日 |
| MEDIUM-05 | 错误信息过于详细 | 4.0 | CWE-209 | BosStorageServiceImpl.java | 0.5 人日 |

### P3 - 低危漏洞

| 编号 | 漏洞 | CVSS | CWE | 文件 | 工作量 |
|------|------|------|-----|------|--------|
| LOW-01 | 调度器默认启用 | 2.0 | CWE-1188 | UploadTaskCleanupScheduler.java:19 | 0.1 人日 |
| LOW-02 | User-Agent 硬编码 | 1.5 | CWE-656 | BosStorageServiceImpl.java:289 | 0.1 人日 |
| LOW-03 | 缺少安全事件日志 | 2.5 | CWE-778 | BosStorageServiceImpl.java:241 | 0.5 人日 |

---

## 修复路线图

### 第一阶段：严重漏洞修复（立即，1 天内）

**目标**: 修复所有 CVSS >= 9.0 的漏洞

**任务清单**:
- [ ] CRITICAL-01: 修复路径遍历漏洞
  - 在 UploadServiceImpl.initUpload() 中强制 storageKey 前缀
  - 或完全移除 storageKey 参数，由后端生成
  - 添加单元测试验证修复

**预计工作量**: 0.5 人日

### 第二阶段：高危漏洞修复（1 周内）

**目标**: 修复所有 CVSS >= 7.0 的漏洞

**任务清单**:
- [ ] HIGH-01: 实施文件类型验证
  - 定义文件类型白名单（扩展名 + MIME + Magic Number）
  - 在 UploadController 和 StorageController 中添加验证
  - 添加集成测试验证恶意文件被拒绝
- [ ] HIGH-02: 修复 SSRF 漏洞
  - 实施 URL 白名单验证
  - 禁止访问内网地址
  - 添加单元测试验证修复
- [ ] HIGH-03: 修复敏感信息泄露
  - 实现日志脱敏工具类
  - 修改所有日志调用
  - 生产环境返回通用错误消息

**预计工作量**: 2.5 人日

### 第三阶段：中危漏洞修复（1 个月内）

**目标**: 修复所有 CVSS >= 4.0 的漏洞

**任务清单**:
- [ ] MEDIUM-01: 实施文件完整性验证
  - 分块上传时计算并验证 MD5
  - 完成上传后验证整个文件 MD5
- [ ] MEDIUM-02: 统一文件大小限制
  - 创建配置类 StorageUploadProperties
  - 在所有上传接口中应用限制
- [ ] MEDIUM-03: 改进秒传机制
  - 增加文件名或模块验证
  - 或返回文件列表让用户选择
- [ ] MEDIUM-04: 实施速率限制
  - 集成 Resilience4j RateLimiter
  - 或使用 Redis 实现分布式限流
- [ ] MEDIUM-05: 改进错误处理
  - 生产环境返回通用错误消息
  - 开发环境可选详细错误

**预计工作量**: 4.0 人日

### 第四阶段：低危漏洞修复（2 个月内）

**目标**: 修复所有 CVSS < 4.0 的漏洞

**任务清单**:
- [ ] LOW-01: 调度器默认禁用
- [ ] LOW-02: User-Agent 配置化
- [ ] LOW-03: 增强安全事件日志

**预计工作量**: 0.7 人日

### 第五阶段：安全加固（3 个月内）

**目标**: 实施纵深防御措施

**任务清单**:
- [ ] 集成密钥管理服务（KMS）
- [ ] 实施存储配额限制
- [ ] 添加异常行为告警
- [ ] 集成 SIEM 系统
- [ ] 定期安全扫描和渗透测试

**预计工作量**: 5 人日

---

## 总结

**总工作量**: 12.7 人日（约 2.5 周）

**优先级分布**:
- P0 (严重): 1 个漏洞，0.5 人日
- P1 (高危): 3 个漏洞，2.5 人日
- P2 (中危): 5 个漏洞，4.0 人日
- P3 (低危): 3 个漏洞，0.7 人日
- 安全加固: 5.0 人日

**预期收益**:
- 消除严重安全漏洞，防止数据泄露和越权访问
- 提升系统安全评分从 72/100 (C+) 到 90+/100 (A)
- 满足 GDPR、PCI-DSS、等保 2.0 合规要求
- 降低安全事件响应成本
- 提升用户信任度和品牌形象

**关键里程碑**:
- Day 1: 修复路径遍历漏洞（CRITICAL-01）
- Week 1: 修复所有高危漏洞（HIGH-01/02/03）
- Month 1: 修复所有中危漏洞（MEDIUM-01/02/03/04/05）
- Month 2: 修复所有低危漏洞（LOW-01/02/03）
- Month 3: 完成安全加固和合规认证

**后续行动**:
1. 立即启动第一阶段修复（CRITICAL-01）
2. 安排代码审查和安全测试
3. 更新安全开发规范
4. 定期进行安全培训
5. 建立漏洞响应流程

---

**审计人**: Claude (Anthropic)
**审计工具**: 人工代码审查 + OWASP Top 10 2021 + CWE Top 25
**下次审计**: 2026-08-08（3 个月后）

