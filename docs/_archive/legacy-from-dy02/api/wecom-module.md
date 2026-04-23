# wecom 模块 API 文档

## 文件结构
```
controller/WecomController.java
entity/WcMessageLog.java
entity/WcPushRule.java
entity/WcRobotConfig.java
repository/WcMessageLogRepository.java
repository/WcPushRuleRepository.java
repository/WcRobotConfigRepository.java
service/NotificationTriggerService.java
service/WecomService.java
service/impl/NotificationTriggerServiceImpl.java
service/impl/WecomServiceImpl.java
vo/WcMessageLogSearchVO.java
vo/WcMessageLogVO.java
vo/WcPushRuleSaveVO.java
vo/WcPushRuleVO.java
vo/WcRobotConfigSaveVO.java
vo/WcRobotConfigVO.java
vo/WcRobotSearchVO.java
vo/WcSendMessageVO.java
```

## API 接口

### WecomController
```
@RequestMapping("/api/v1/wecom")
@PostMapping("/robot/list")
public RESTResult<PageResultVO<WcRobotConfigVO>> robotList(HttpServletRequest request,
@PostMapping("/robot/get")
public RESTResult<WcRobotConfigVO> robotGet(HttpServletRequest request, @RequestParam Long id) {
@PostMapping("/robot/save")
public RESTResult<Long> robotSave(HttpServletRequest request, @Valid @RequestBody WcRobotConfigSaveVO vo) {
@PostMapping("/robot/delete")
public RESTResult<Void> robotDelete(HttpServletRequest request, @RequestParam Long id) {
@PostMapping("/robot/update-status")
public RESTResult<Void> robotStatus(HttpServletRequest request,
@PostMapping("/rule/list")
public RESTResult<List<WcPushRuleVO>> ruleList(HttpServletRequest request) {
@PostMapping("/rule/get")
public RESTResult<WcPushRuleVO> ruleGet(HttpServletRequest request, @RequestParam Long id) {
@PostMapping("/rule/save")
public RESTResult<Long> ruleSave(HttpServletRequest request, @Valid @RequestBody WcPushRuleSaveVO vo) {
@PostMapping("/rule/delete")
public RESTResult<Void> ruleDelete(HttpServletRequest request, @RequestParam Long id) {
@PostMapping("/rule/update-status")
public RESTResult<Void> ruleStatus(HttpServletRequest request,
@PostMapping("/log/list")
public RESTResult<PageResultVO<WcMessageLogVO>> logList(HttpServletRequest request,
@PostMapping("/push")
public RESTResult<Void> push(HttpServletRequest request, @Valid @RequestBody WcSendMessageVO vo) {
```

## Entity 字段

### WcMessageLog
```
@Id
private Long id;
@Column(name = "owner_id", nullable = false)
private Long ownerId;
@Column(name = "robot_id", nullable = false)
private Long robotId;
@Column(name = "rule_id")
private Long ruleId;
@Column(name = "message_type", nullable = false, length = 16)
private String messageType = "text";
@Column(name = "message_content", nullable = false, columnDefinition = "TEXT")
private String messageContent;
@Column(name = "status", nullable = false)
private Integer status = 0;
@Column(name = "error_message", length = 512)
private String errorMessage;
@Column(name = "send_time", nullable = false)
private Timestamp sendTime;
@Column(name = "create_time", nullable = false)
private Timestamp createTime;
```

### WcPushRule
```
@Id
private Long id;
@Column(name = "owner_id", nullable = false)
private Long ownerId;
@Column(name = "robot_id", nullable = false)
private Long robotId;
@Column(name = "rule_name", nullable = false, length = 128)
private String ruleName;
@Column(name = "trigger_type", nullable = false, length = 16)
private String triggerType;
@Column(name = "trigger_config", nullable = false, columnDefinition = "TEXT")
private String triggerConfig;
@Column(name = "message_template", nullable = false, columnDefinition = "TEXT")
private String messageTemplate;
@Column(name = "status", nullable = false)
private Integer status = 1;
@Column(name = "last_trigger_time")
private Timestamp lastTriggerTime;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time", nullable = false)
private Timestamp createTime;
@Column(name = "update_time", nullable = false)
private Timestamp updateTime;
```

### WcRobotConfig
```
@Id
private Long id;
@Column(name = "owner_id", nullable = false)
private Long ownerId;
@Column(name = "robot_name", nullable = false, length = 128)
private String robotName;
@Column(name = "webhook_url", nullable = false, length = 512)
private String webhookUrl;
@Column(name = "robot_type", nullable = false, length = 32)
private String robotType = "custom";
@Column(name = "status", nullable = false)
private Integer status = 1;
@Column(name = "description", length = 256)
private String description;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time", nullable = false)
private Timestamp createTime;
@Column(name = "update_time", nullable = false)
private Timestamp updateTime;
```

