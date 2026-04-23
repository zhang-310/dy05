# copy 模块 API 文档

## 文件结构
```
controller/CopyAiController.java
controller/CopyApprovalController.java
controller/CopyLibraryController.java
controller/CopyTemplateController.java
entity/CopyApproval.java
entity/CopyLibrary.java
entity/CopyTemplate.java
repository/CopyApprovalRepository.java
repository/CopyLibraryRepository.java
repository/CopyTemplateRepository.java
service/CopyApprovalService.java
service/CopyLibraryService.java
service/CopyTemplateService.java
service/impl/CopyApprovalServiceImpl.java
service/impl/CopyLibraryServiceImpl.java
service/impl/CopyTemplateServiceImpl.java
vo/CopyApprovalSaveVO.java
vo/CopyApprovalSearchVO.java
vo/CopyApprovalVO.java
vo/CopyLibrarySaveVO.java
vo/CopyLibrarySearchVO.java
vo/CopyLibraryVO.java
vo/CopyTemplateSaveVO.java
vo/CopyTemplateSearchVO.java
vo/CopyTemplateVO.java
```

## API 接口

### CopyAiController
```
@RequestMapping("/api/v1/copy/ai")
@PostMapping("/generate")
public RESTResult<String> generate(@RequestBody(required = false) java.util.Map<String, Object> body, HttpServletRequest request) {
```

### CopyApprovalController
```
@RequestMapping("/api/v1/copy/approval")
@PostMapping("/search")
public RESTResult<PageResultVO<CopyApprovalVO>> search(HttpServletRequest request,
@PostMapping("/get")
public RESTResult<CopyApprovalVO> get(HttpServletRequest request,
@PostMapping("/save")
public RESTResult<Long> save(HttpServletRequest request, @Valid @RequestBody CopyApprovalSaveVO vo) {
@PostMapping("/delete")
public RESTResult<Void> delete(HttpServletRequest request,
```

### CopyLibraryController
```
@RequestMapping("/api/v1/copy/library")
@PostMapping("/search")
public RESTResult<PageResultVO<CopyLibraryVO>> search(HttpServletRequest request,
@PostMapping("/get")
public RESTResult<CopyLibraryVO> get(HttpServletRequest request,
@PostMapping("/save")
public RESTResult<Long> save(HttpServletRequest request, @Valid @RequestBody CopyLibrarySaveVO vo) {
@PostMapping("/delete")
public RESTResult<Void> delete(HttpServletRequest request,
@PostMapping("/update-status")
public RESTResult<Void> updateStatus(HttpServletRequest request,
@PostMapping("/increment-use-count")
public RESTResult<Void> incrementUseCount(HttpServletRequest request, @RequestParam Long id) {
```

### CopyTemplateController
```
@RequestMapping("/api/v1/copy/template")
@PostMapping("/search")
public RESTResult<PageResultVO<CopyTemplateVO>> search(HttpServletRequest request,
@PostMapping("/get")
public RESTResult<CopyTemplateVO> get(HttpServletRequest request,
@PostMapping("/save")
public RESTResult<Long> save(HttpServletRequest request, @Valid @RequestBody CopyTemplateSaveVO vo) {
@PostMapping("/delete")
public RESTResult<Void> delete(HttpServletRequest request,
@PostMapping("/update-status")
public RESTResult<Void> updateStatus(HttpServletRequest request,
```

## Entity 字段

### CopyApproval
```
@Id
private Long id;
@Column(name = "copy_id", nullable = false)
private Long copyId;
@Column(name = "user_id")
private Long userId;
@Column(name = "approval_status", nullable = false)
private Integer approvalStatus = 2;
@Column(name = "comments", columnDefinition = "TEXT")
private String comments;
@Column(name = "approval_time")
private Timestamp approvalTime;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### CopyLibrary
```
@Id
private Long id;
@Column(name = "user_id", nullable = false)
private Long userId;
@Column(name = "title", nullable = false, length = 256)
private String title;
@Column(name = "content", nullable = false, columnDefinition = "TEXT")
private String content;
@Column(name = "category", length = 64)
private String category;
@Column(name = "tags", length = 512)
private String tags;
@Column(name = "word_count")
private Integer wordCount = 0;
@Column(name = "use_count", nullable = false)
private Integer useCount = 0;
@Column(name = "rating")
private Integer rating;
@Column(name = "status", nullable = false)
private Integer status = 0;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### CopyTemplate
```
@Id
private Long id;
@Column(name = "user_id", nullable = false)
private Long userId;
@Column(name = "template_name", nullable = false, length = 256)
private String templateName;
@Column(name = "template_content", nullable = false, columnDefinition = "TEXT")
private String templateContent;
@Column(name = "category", length = 64)
private String category;
@Column(name = "description", columnDefinition = "TEXT")
private String description;
@Column(name = "status", nullable = false)
private Integer status = 1;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

