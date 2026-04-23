# storage 模块 API 文档

## 文件结构
```
config/BosCleanupScheduler.java
config/UploadTaskCleanupScheduler.java
controller/StorageController.java
controller/UploadController.java
controller/package-info.java
entity/BosFileMetadata.java
entity/ChunkStatus.java
entity/SysFile.java
entity/SysUploadChunk.java
entity/SysUploadTask.java
entity/UploadStatus.java
entity/package-info.java
package-info.java
repository/BosFileMetadataRepository.java
repository/SysFileRepository.java
repository/SysUploadChunkRepository.java
repository/SysUploadTaskRepository.java
repository/package-info.java
service/BosCleanupService.java
service/BosFileMetadataService.java
service/BosReferenceImageCacheService.java
service/BosStorageService.java
service/SysFileService.java
service/UploadService.java
service/impl/BosCleanupServiceImpl.java
service/impl/BosFileMetadataServiceImpl.java
service/impl/BosReferenceImageCacheServiceImpl.java
service/impl/BosStorageServiceImpl.java
service/impl/SysFileServiceImpl.java
service/impl/UploadServiceImpl.java
service/impl/package-info.java
service/package-info.java
util/package-info.java
vo/StorageFileVO.java
vo/SysFileVO.java
vo/UploadChunkVO.java
vo/UploadCompleteResultVO.java
vo/UploadCompleteVO.java
vo/UploadInitResultVO.java
vo/UploadInitVO.java
vo/UploadProgressVO.java
vo/package-info.java
```

## API 接口

### StorageController
```
@RequestMapping("/api/v1/storage")
@PostMapping("/configured")
public RESTResult<Boolean> configured(HttpServletRequest request,
@PostMapping("/list")
public RESTResult<List<StorageFileVO>> list(HttpServletRequest request,
@PostMapping("/upload")
public RESTResult<Map<String, String>> upload(HttpServletRequest request,
@PostMapping("/delete")
public RESTResult<Void> delete(HttpServletRequest request, @io.swagger.v3.oas.annotations.parameters.RequestBody(
@PostMapping("/url")
public RESTResult<Map<String, String>> getUrl(HttpServletRequest request,
```

### UploadController
```
@RequestMapping("/api/v1/storage/upload")
@PostMapping("/init")
public RESTResult<UploadInitResultVO> initUpload(
@PostMapping("/chunk")
public RESTResult<Void> uploadChunk(
@GetMapping("/chunks")
public RESTResult<List<Integer>> getUploadedChunks(
@GetMapping("/progress")
public RESTResult<UploadProgressVO> getProgress(
@PostMapping("/complete")
public RESTResult<UploadCompleteResultVO> completeUpload(
@PostMapping("/cancel")
public RESTResult<Void> cancelUpload(
```

### package-info
```
```

## Entity 字段

### BosFileMetadata
```
@Id
private Long id;
@Column(name = "bos_key", nullable = false, length = 500)
private String bosKey;
@Column(name = "user_id", nullable = false)
private Long userId;
@Column(name = "task_id")
private Long taskId;
@Column(name = "category", length = 50)
private String category;
@Column(name = "file_size")
private Long fileSize = 0L;
@Column(name = "storage_cost_monthly", precision = 10, scale = 4)
private BigDecimal storageCostMonthly = BigDecimal.ZERO;
@Column(name = "usage_count")
private Integer usageCount = 0;
@Column(name = "shot_id")
private Long shotId;
@Column(name = "source_url", length = 500)
private String sourceUrl;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### ChunkStatus
```
```

### SysFile
```
@Id
private Long id;
@Column(name = "owner_id")
private Long ownerId;
@Column(name = "original_name", nullable = false, length = 256)
private String originalName;
@Column(name = "storage_name", nullable = false, length = 256)
private String storageName;
@Column(name = "storage_path", nullable = false, length = 512)
private String storagePath;
@Column(name = "file_url", nullable = false, length = 512)
private String fileUrl;
@Column(name = "file_type", length = 32)
private String fileType;
@Column(name = "file_ext", length = 16)
private String fileExt;
@Column(name = "file_size")
private Long fileSize = 0L;
@Column(name = "module", length = 64)
private String module;
@Column(name = "provider", length = 32)
private String provider = "local";
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
```

### SysUploadChunk
```
@Id
private Long id;
@Column(name = "task_id", nullable = false)
private Long taskId;
@Column(name = "chunk_index", nullable = false)
private Integer chunkIndex;
@Column(name = "chunk_size", nullable = false)
private Long chunkSize;
@Column(name = "chunk_md5", nullable = false, length = 64)
private String chunkMd5;
@Column(name = "bos_etag", length = 64)
private String bosEtag;
@Column(name = "bos_part_number")
private Integer bosPartNumber;
@Column(name = "status", nullable = false, length = 32)
private String status = "PENDING";
@Column(name = "retry_count")
private Integer retryCount = 0;
@Column(name = "uploaded_at")
private Timestamp uploadedAt;
@Column(name = "created_at", nullable = false)
private Timestamp createdAt;
```

### SysUploadTask
```
@Id
private Long id;
@Column(name = "owner_id", nullable = false)
private Long ownerId;
@Column(name = "upload_id", nullable = false, unique = true, length = 64)
private String uploadId;
@Column(name = "bos_upload_id", length = 256)
private String bosUploadId;
@Column(name = "original_filename", nullable = false, length = 256)
private String originalFilename;
@Column(name = "storage_key", nullable = false, length = 512)
private String storageKey;
@Column(name = "file_size", nullable = false)
private Long fileSize;
@Column(name = "file_md5", length = 64)
private String fileMd5;
@Column(name = "chunk_size")
private Integer chunkSize = 5242880;
@Column(name = "total_chunks", nullable = false)
private Integer totalChunks;
@Column(name = "uploaded_chunks")
private Integer uploadedChunks = 0;
@Column(name = "uploaded_bytes")
private Long uploadedBytes = 0L;
@Column(name = "status", nullable = false, length = 32)
private String status = "PENDING";
@Column(name = "failure_reason", length = 512)
private String failureReason;
@Column(name = "module", length = 64)
private String module;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "created_at", nullable = false)
private Timestamp createdAt;
@Column(name = "updated_at", nullable = false)
private Timestamp updatedAt;
@Column(name = "completed_at")
private Timestamp completedAt;
@Column(name = "expire_at", nullable = false)
private Timestamp expireAt;
```

### UploadStatus
```
```

### package-info
```
```

