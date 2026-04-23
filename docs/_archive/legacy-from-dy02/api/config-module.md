# config 模块 API 文档

## 文件结构
```
controller/ConfigController.java
controller/package-info.java
dto/ConfigDTO.java
entity/ConfigVersionHistory.java
entity/SysConfig.java
entity/SysConfigGroup.java
entity/SysIndustry.java
entity/package-info.java
package-info.java
repository/ConfigVersionHistoryRepository.java
repository/SysConfigGroupRepository.java
repository/SysConfigRepository.java
repository/SysIndustryRepository.java
repository/package-info.java
service/ConfigService.java
service/impl/ConfigServiceImpl.java
service/impl/package-info.java
service/package-info.java
util/package-info.java
vo/ConfigSaveVO.java
vo/ConfigSearchVO.java
vo/ConfigVO.java
vo/package-info.java
```

## API 接口

### ConfigController
```
@RequestMapping("/api/v1/config")
@PostMapping("/list")
public RESTResult<PageResultVO<ConfigVO>> list(HttpServletRequest request, @io.swagger.v3.oas.annotations.parameters.RequestBody(
@PostMapping("/get")
public RESTResult<ConfigVO> get(HttpServletRequest request,
@PostMapping("/save")
public RESTResult<Long> save(HttpServletRequest request, @Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(
@PostMapping("/delete")
public RESTResult<Void> delete(HttpServletRequest request, @io.swagger.v3.oas.annotations.parameters.RequestBody(
```

### package-info
```
```

## Entity 字段

### ConfigVersionHistory
```
@Id
private Long id;
@Column(name = "config_id", nullable = false)
private Long configId;
@Column(name = "config_key", nullable = false, length = 128)
private String configKey;
@Column(name = "old_value", columnDefinition = "TEXT")
private String oldValue;
@Column(name = "new_value", columnDefinition = "TEXT")
private String newValue;
@Column(name = "operator_id")
private Long operatorId;
@Column(name = "create_time")
private Timestamp createTime;
```

### SysConfig
```
@Id
private Long id;
@Column(name = "config_key", nullable = false, length = 128)
private String configKey;
@Column(name = "config_value", columnDefinition = "TEXT")
private String configValue;
@Column(name = "value_type", nullable = false, length = 16)
private String valueType = "string";
@Column(name = "is_sensitive", nullable = false)
private Integer isSensitive = 0;
@Column(name = "config_group", length = 64)
private String configGroup;
@Column(name = "remark", length = 256)
private String remark;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### SysConfigGroup
```
@Id
private Long id;
@Column(name = "parent_id")
private Long parentId = 0L;
@Column(name = "group_code", nullable = false, length = 64)
private String groupCode;
@Column(name = "group_name", nullable = false, length = 64)
private String groupName;
@Column(name = "icon", length = 64)
private String icon;
@Column(name = "description", length = 256)
private String description;
@Column(name = "sort_order")
private Integer sortOrder = 0;
@Column(name = "is_system", nullable = false)
private Integer isSystem = 0;
@Column(name = "status", nullable = false)
private Integer status = 1;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### SysIndustry
```
@Id
private Long id;
@Column(name = "parent_id")
private Long parentId = 0L;
@Column(name = "industry_name", nullable = false, length = 64)
private String industryName;
@Column(name = "industry_code", nullable = false, length = 32)
private String industryCode;
@Column(name = "icon", length = 128)
private String icon;
@Column(name = "sort_order")
private Integer sortOrder = 0;
@Column(name = "status", nullable = false)
private Integer status = 1;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### package-info
```
```

