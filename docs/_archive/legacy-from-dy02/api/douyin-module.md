# douyin 模块 API 文档

## 文件结构
```
config/DouyinLearningScheduler.java
controller/DouyinAccountController.java
controller/DouyinPersonaController.java
controller/DouyinVideoController.java
controller/FanProfileController.java
entity/DouyinAccount.java
entity/DouyinVideo.java
entity/DyFanProfile.java
entity/DyFanProfileStats.java
entity/DyPersona.java
package-info.java
repository/DouyinAccountRepository.java
repository/DouyinVideoRepository.java
repository/DyFanProfileRepository.java
repository/DyFanProfileStatsRepository.java
repository/DyPersonaRepository.java
service/DouyinAccountService.java
service/DouyinPersonaService.java
service/DouyinScriptLearningService.java
service/DouyinVideoService.java
service/FanProfileService.java
service/impl/DouyinAccountServiceImpl.java
service/impl/DouyinPersonaServiceImpl.java
service/impl/DouyinScriptLearningServiceImpl.java
service/impl/DouyinVideoServiceImpl.java
service/impl/FanProfileServiceImpl.java
vo/DouyinAccountSaveVO.java
vo/DouyinAccountSearchVO.java
vo/DouyinAccountStatisticsVO.java
vo/DouyinAccountVO.java
vo/DouyinVideoSaveVO.java
vo/DouyinVideoSearchVO.java
vo/DouyinVideoVO.java
vo/FanProfileVO.java
vo/PersonaSaveVO.java
```

## API 接口

### DouyinAccountController
```
@RequestMapping("/api/v1/douyin/account")
@PostMapping("/search")
public RESTResult<PageResultVO<DouyinAccountVO>> search(HttpServletRequest request, @io.swagger.v3.oas.annotations.parameters.RequestBody(
@PostMapping("/get")
public RESTResult<DouyinAccountVO> get(HttpServletRequest request, @Parameter(
@PostMapping("/save")
public RESTResult<Long> save(HttpServletRequest request, @Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(
@PostMapping("/delete")
public RESTResult<Void> delete(HttpServletRequest request, @Parameter(
@PostMapping("/statistics")
public RESTResult<DouyinAccountStatisticsVO> statistics(HttpServletRequest request, @Parameter(
```

### DouyinPersonaController
```
@RequestMapping("/api/v1/douyin/persona")
@PostMapping("/save")
public RESTResult<Long> save(@RequestBody PersonaSaveVO vo, HttpServletRequest request) {
@PostMapping("/list")
public RESTResult<List<DyPersona>> list(@RequestParam(required = false) String personaType,
@PostMapping("/get")
public RESTResult<DyPersona> get(@RequestParam Long id, HttpServletRequest request) {
@PostMapping("/delete")
public RESTResult<Void> delete(@RequestParam Long id, HttpServletRequest request) {
@PostMapping("/set-default")
public RESTResult<Void> setDefault(@RequestParam Long id, HttpServletRequest request) {
@PostMapping("/get-default")
public RESTResult<DyPersona> getDefault(HttpServletRequest request) {
@PostMapping("/templates")
public RESTResult<List<DyPersona>> templates(HttpServletRequest request) {
```

### DouyinVideoController
```
@RequestMapping("/api/v1/douyin/video")
@PostMapping("/search")
public RESTResult<PageResultVO<DouyinVideoVO>> search(HttpServletRequest request, @io.swagger.v3.oas.annotations.parameters.RequestBody(
@PostMapping("/get")
public RESTResult<DouyinVideoVO> get(HttpServletRequest request, @Parameter(
@PostMapping("/save")
public RESTResult<Long> save(HttpServletRequest request, @Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(
@PostMapping("/sync")
public RESTResult<Void> sync(HttpServletRequest request, @Parameter(
```

### FanProfileController
```
@RequestMapping("/api/v1/douyin/fan-profile")
@PostMapping("/get")
public RESTResult<FanProfileVO> getFanProfile(HttpServletRequest request,
@PostMapping("/stats")
public RESTResult<List<DyFanProfileStats>> getStats(HttpServletRequest request,
@PostMapping("/sync/{accountId}")
public RESTResult<Void> manualSync(
```

## Entity 字段

### DouyinAccount
```
@Id
private Long id;
@Column(name = "user_id", nullable = false)
private Long userId;
@Column(name = "account_name", nullable = false, length = 128)
private String accountName;
@Column(name = "account_id", nullable = false, length = 128)
private String accountId;
@Column(name = "follow_count", nullable = false)
private Long followCount = 0L;
@Column(name = "fan_count", nullable = false)
private Long fanCount = 0L;
@Column(name = "video_count", nullable = false)
private Long videoCount = 0L;
@Column(name = "total_likes", nullable = false)
private Long totalLikes = 0L;
@Column(name = "description", length = 512)
private String description;
@Column(name = "status", nullable = false)
private Integer status = 0;
@Column(name = "bind_time")
private Timestamp bindTime;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### DouyinVideo
```
@Id
private Long id;
@Column(name = "account_id", nullable = false)
private Long accountId;
@Column(name = "video_id", nullable = false, length = 128)
private String videoId;
@Column(name = "title", nullable = false, length = 256)
private String title;
@Column(name = "description", length = 1024)
private String description;
@Column(name = "view_count", nullable = false)
private Long viewCount = 0L;
@Column(name = "like_count", nullable = false)
private Long likeCount = 0L;
@Column(name = "share_count", nullable = false)
private Long shareCount = 0L;
@Column(name = "comment_count", nullable = false)
private Long commentCount = 0L;
@Column(name = "download_count", nullable = false)
private Long downloadCount = 0L;
@Column(name = "video_type", length = 32)
private String videoType;
@Column(name = "publish_time")
private Timestamp publishTime;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### DyFanProfile
```
@Id
private Long id;
@Column(name = "account_id", nullable = false)
private Long accountId;
@Column(name = "age_range", length = 32)
private String ageRange;
@Column(name = "gender", length = 16)
private String gender;
@Column(name = "province", length = 64)
private String province;
@Column(name = "city", length = 64)
private String city;
@Column(name = "interest_tags", columnDefinition = "TEXT")
private String interestTags;
@Column(name = "active_time", length = 32)
private String activeTime;
@Column(name = "device_type", length = 32)
private String deviceType;
@Column(name = "fan_count")
private Long fanCount = 0L;
@Column(name = "sync_time")
private Timestamp syncTime;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### DyFanProfileStats
```
@Id
private Long id;
@Column(name = "account_id", nullable = false)
private Long accountId;
@Column(name = "stat_type", nullable = false, length = 32)
private String statType;
@Column(name = "stat_key", nullable = false, length = 64)
private String statKey;
@Column(name = "stat_value", length = 128)
private String statValue;
@Column(name = "count")
private Long count = 0L;
@Column(name = "percentage", precision = 5, scale = 2)
private BigDecimal percentage;
@Column(name = "sync_time")
private Timestamp syncTime;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### DyPersona
```
@Id
private Long id;
@Column(name = "owner_id", nullable = false)
private Long ownerId;
@Column(name = "account_id")
private Long accountId;
@Column(name = "persona_name", nullable = false, length = 128)
private String personaName;
@Column(name = "persona_type", length = 32)
@Column(name = "description", columnDefinition = "TEXT")
private String description;
@Column(name = "tone", length = 64)
@Column(name = "target_audience", length = 256)
private String targetAudience;
@Column(name = "content_style", columnDefinition = "TEXT")
private String contentStyle;
@Column(name = "keywords", length = 512)
private String keywords;
@Column(name = "is_default", nullable = false)
private Integer isDefault = 0;
@Column(name = "status", nullable = false)
private Integer status = 1;
@Column(name = "interaction_style", length = 64)
private String interactionStyle;
@Column(name = "language_style", length = 64)
private String languageStyle;
@Column(name = "content_ratio", length = 256)
private String contentRatio;
@Column(name = "local_flavor", length = 64)
private String localFlavor;
@Column(name = "live_style", length = 64)
private String liveStyle;
@Column(name = "persona_traits", length = 512)
private String personaTraits;
@Column(name = "ip_type", length = 32)
private String ipType;
@Column(name = "age_range", length = 32)
private String ageRange;
@Column(name = "positioning_tags", length = 512)
private String positioningTags;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

