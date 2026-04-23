# agent 模块 API 文档

## 文件结构
```
controller/AgentController.java
controller/UserPreferenceController.java
entity/Agent.java
entity/AgentConversation.java
entity/AgentMessage.java
entity/AgentUserPreference.java
repository/AgentConversationRepository.java
repository/AgentMessageRepository.java
repository/AgentRepository.java
repository/AgentUserPreferenceRepository.java
service/AgentService.java
service/UserPreferenceService.java
service/impl/AgentServiceImpl.java
service/impl/UserPreferenceServiceImpl.java
skill/Skill.java
skill/SkillRegistry.java
skill/impl/ScriptIterateSkill.java
vo/AgentSaveVO.java
vo/AgentSearchVO.java
vo/AgentVO.java
```

## API 接口

### AgentController
```
@RequestMapping("/api/v1/agent")
@PostMapping("/list")
public RESTResult<PageResultVO<AgentVO>> list(HttpServletRequest request, @Valid @RequestBody AgentSearchVO searchVO) {
@PostMapping("/get")
public RESTResult<AgentVO> get(HttpServletRequest request, @RequestBody(required = false) Map<String, Object> body) {
@PostMapping("/save")
public RESTResult<Long> save(HttpServletRequest request, @Valid @RequestBody AgentSaveVO saveVO) {
@PostMapping("/delete")
public RESTResult<Void> delete(HttpServletRequest request, @RequestParam Long id) {
@PostMapping("/update-status")
public RESTResult<Void> updateStatus(HttpServletRequest request, @RequestParam Long id, @RequestParam Integer status) {
@PostMapping("/conversation/create")
public RESTResult<Long> createConversation(HttpServletRequest request, @RequestBody Map<String, Object> body) {
@PostMapping("/conversation/list")
public RESTResult<List<Map<String, Object>>> listConversations(HttpServletRequest request,
@PostMapping("/conversation/delete")
public RESTResult<Void> deleteConversation(HttpServletRequest request, @RequestParam Long id) {
@PostMapping("/message/send")
public RESTResult<Long> sendMessage(HttpServletRequest request, @RequestBody Map<String, Object> body) {
@PostMapping("/message/list")
public RESTResult<List<Map<String, Object>>> listMessages(HttpServletRequest request,
@PostMapping(value = "/chat-stream", produces = "text/event-stream;charset=UTF-8")
@GetMapping(value = "/sse-diagnostic", produces = "text/event-stream;charset=UTF-8")
```

### UserPreferenceController
```
@RequestMapping("/api/v1/agent")
@PostMapping("/preference/refine-suggestions")
public RESTResult<Map<String, Object>> getRefineSuggestions(@CurrentUserId Long userId,
```

## Entity 字段

### Agent
```
@Id
private Long id;
@Column(name = "user_id", nullable = false)
private Long userId;
@Column(name = "agent_name", nullable = false, length = 128)
private String agentName;
@Column(name = "description", length = 512)
private String description;
@Column(name = "agent_type", nullable = false)
private Integer agentType;
@Column(name = "system_prompt", columnDefinition = "TEXT")
private String systemPrompt;
@Column(name = "model_config", columnDefinition = "TEXT")
private String modelConfig;
@Column(name = "response_mode", nullable = false)
private Integer responseMode = 1;
@Column(name = "status", nullable = false)
private Integer status = 1;
@Column(name = "version", nullable = false)
private Integer version = 0;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### AgentConversation
```
@Id
private Long id;
@Column(name = "agent_id", nullable = false)
private Long agentId;
@Column(name = "user_id", nullable = false)
private Long userId;
@Column(name = "conversation_topic", nullable = false, length = 256)
private String conversationTopic;
@Column(name = "status", nullable = false)
private Integer status = 1;
@Column(name = "message_count", nullable = false)
private Integer messageCount = 0;
@Column(name = "last_message_time")
private Timestamp lastMessageTime;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### AgentMessage
```
@Id
private Long id;
@Column(name = "conversation_id", nullable = false)
private Long conversationId;
@Column(name = "sender_type", nullable = false)
private Integer senderType;
@Column(name = "content", nullable = false, columnDefinition = "TEXT")
private String content;
@Column(name = "tokens")
private Integer tokens = 0;
@Column(name = "create_time")
private Timestamp createTime;
```

### AgentUserPreference
```
@Id
private Long id;
@Column(name = "user_id", nullable = false)
private Long userId;
@Column(name = "pref_key", nullable = false, length = 128)
private String prefKey;
@Column(name = "pref_value", nullable = false, length = 512)
private String prefValue;
@Column(name = "usage_count", nullable = false)
private Integer usageCount = 1;
@Column(name = "last_used_at", nullable = false)
private Timestamp lastUsedAt;
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(name = "create_time", nullable = false)
private Timestamp createTime;
@Column(name = "update_time", nullable = false)
private Timestamp updateTime;
```

