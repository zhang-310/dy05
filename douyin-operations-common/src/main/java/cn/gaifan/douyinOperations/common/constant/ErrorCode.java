package cn.gaifan.douyinOperations.common.constant;

/**
 * 错误码常量，与 docs/04-错误码注册表.md 一致
 */
public final class ErrorCode {

    private ErrorCode() {}

    // ==================== 全局码 ====================

    /** 成功 */
    public static final int SUCCESS = 200;

    // ==================== 1000 段：参数校验与系统 ====================

    /** 参数校验失败 */
    public static final int VALIDATION_FAIL = 1001;
    /** 参数无效（同 VALIDATION_FAIL，兼容旧代码） */
    public static final int INVALID_PARAMS = 1001;
    /** 系统繁忙/服务器错误 */
    public static final int SYSTEM_BUSY = 1002;
    /** 内部错误（同 SYSTEM_BUSY，兼容旧代码） */
    public static final int INTERNAL_ERROR = 1002;
    /** 请求频率超限 */
    public static final int RATE_LIMIT = 1003;
    /** 请求过于频繁（同 RATE_LIMIT，兼容旧代码） */
    public static final int TOO_MANY_REQUESTS = 1003;
    /** 重复提交 */
    public static final int DUPLICATE_SUBMIT = 1004;
    /** 数据不存在（通用） */
    public static final int DATA_NOT_FOUND = 1005;
    /** 未找到（同 DATA_NOT_FOUND，兼容旧代码） */
    public static final int NOT_FOUND = 1005;
    /** 数据已存在（通用唯一冲突） */
    public static final int DATA_ALREADY_EXISTS = 1006;
    /** 操作不允许（通用业务拒绝） */
    public static final int OPERATION_NOT_ALLOWED = 1007;

    // ==================== 2000 段：认证与鉴权（auth） ====================

    /** 未登录或会话过期 */
    public static final int UNAUTHORIZED = 2001;
    /** 无权限访问该资源 */
    public static final int FORBIDDEN = 2002;
    /** 无权限（同 FORBIDDEN，兼容旧代码） */
    public static final int PERMISSION_DENIED = 2002;
    /** Token 无效或已篡改 */
    public static final int TOKEN_INVALID = 2003;
    /** 需要图形验证码（登录失败次数过多） */
    public static final int CAPTCHA_REQUIRED = 2004;
    /** 用户名或密码错误 */
    public static final int LOGIN_FAILED = 2005;
    /** 账号已禁用 */
    public static final int USER_DISABLED = 2006;
    /** 用户不存在 */
    public static final int USER_NOT_FOUND = 2007;
    /** 角色不存在 */
    public static final int ROLE_NOT_FOUND = 2008;
    /** 资源不存在 */
    public static final int RESOURCE_NOT_FOUND = 2009;
    /** 原密码不正确 */
    public static final int PASSWORD_MISMATCH = 2010;
    /** 用户名已存在 */
    public static final int USERNAME_EXISTS = 2011;
    /** 该第三方账号已绑定其他用户 */
    public static final int OAUTH_ALREADY_BOUND = 2012;
    /** 图形验证码错误或已过期 */
    public static final int CAPTCHA_INVALID = 2013;
    /** 短信验证码错误或已过期 */
    public static final int VERIFY_CODE_INVALID = 2014;

    // ==================== 3100：抖音账号 + 人设 + 产品（douyin） ====================

    /** 抖音账号不存在 */
    public static final int ACCOUNT_NOT_FOUND = 3101;
    /** OAuth2 授权失败 */
    public static final int OAUTH_FAILED = 3102;
    /** Token 刷新失败 */
    public static final int TOKEN_REFRESH_FAILED = 3103;
    /** 人设不存在 */
    public static final int PERSONA_NOT_FOUND = 3104;
    /** 产品不存在 */
    public static final int PRODUCT_NOT_FOUND = 3105;
    /** 产品话术不存在 */
    public static final int PRODUCT_SCRIPT_NOT_FOUND = 3106;
    /** 抖音账号已绑定其他用户 */
    public static final int ACCOUNT_ALREADY_BOUND = 3107;
    /** 抖音账号授权已过期 */
    public static final int ACCOUNT_TOKEN_EXPIRED = 3108;
    /** 绑定账号数量超出上限 */
    public static final int ACCOUNT_BIND_LIMIT = 3109;
    /** 人设删除被拒绝（存在关联数据） */
    public static final int PERSONA_DELETE_DENIED = 3110;
    /** 无权操作该人设 */
    public static final int PERSONA_FORBIDDEN = 3111;
    /** 无权操作该产品 */
    public static final int PRODUCT_FORBIDDEN = 3112;
    /** 抖音开放平台 API 调用失败 */
    public static final int DOUYIN_API_CALL_FAILED = 3113;
    /** 产品话术模板不存在 */
    public static final int PRODUCT_TEMPLATE_NOT_FOUND = 3114;
    /** 库存更新冲突（乐观锁版本冲突，请刷新后重试） */
    public static final int INVENTORY_CONFLICT = 3115;
    /** 产品话术 AI 生成失败 */
    public static final int PRODUCT_SCRIPT_GENERATE_FAIL = 3141;
    /** 产品话术风格参数非法 */
    public static final int PRODUCT_SCRIPT_STYLE_INVALID = 3142;
    /** 产品话术类型参数非法 */
    public static final int PRODUCT_SCRIPT_TYPE_INVALID = 3143;
    /** 产品话术合规检测未通过 */
    public static final int PRODUCT_SCRIPT_COMPLIANCE_FAIL = 3144;
    /** 产品话术生成频率超限 */
    public static final int PRODUCT_SCRIPT_RATE_LIMITED = 3145;
    /** 产品话术批量生成数量超限 */
    public static final int PRODUCT_SCRIPT_BATCH_TOO_LARGE = 3146;
    /** 产品话术生成任务不存在 */
    public static final int PRODUCT_SCRIPT_TASK_NOT_FOUND = 3147;
    /** 内容违规（通用合规检测失败） */
    public static final int COMPLIANCE_VIOLATION = 3148;

    // ==================== 3200：短视频（shortvideo） ====================

    /** 视频不存在 */
    public static final int VIDEO_NOT_FOUND = 3201;
    /** 视频同步失败 */
    public static final int SYNC_FAILED = 3202;
    /** 策划方案不存在 */
    public static final int PLAN_NOT_FOUND = 3203;
    /** 爆款视频不存在 */
    public static final int HOT_VIDEO_NOT_FOUND = 3204;
    /** 脚本模板不存在 */
    public static final int SCRIPT_TEMPLATE_NOT_FOUND = 3205;
    /** AI 视频制作失败 */
    public static final int VIDEO_PRODUCE_FAILED = 3206;
    /** 项目不存在 */
    public static final int PROJECT_NOT_FOUND = 3207;
    /** 短视频脚本不存在 */
    public static final int SV_SCRIPT_NOT_FOUND = 3208;
    /** 分镜不存在 */
    public static final int SHOT_LIST_NOT_FOUND = 3209;
    /** 工作流任务不存在 */
    public static final int WORKFLOW_TASK_NOT_FOUND = 3210;
    /** 工作流执行失败 */
    public static final int WORKFLOW_EXECUTION_FAILED = 3211;
    /** 内容审核失败 */
    public static final int CONTENT_AUDIT_FAILED = 3250;
    /** 内容违规 */
    public static final int CONTENT_VIOLATION = 3251;
    /** 审核服务不可用 */
    public static final int AUDIT_SERVICE_UNAVAILABLE = 3252;

    // ==================== 3300：直播（live） ====================

    /** 直播场次不存在 */
    public static final int SESSION_NOT_FOUND = 3301;
    /** 直播场次状态不允许该操作 */
    public static final int SESSION_STATUS_INVALID = 3302;
    /** 场次关联产品不存在 */
    public static final int SESSION_PRODUCT_NOT_FOUND = 3303;
    /** 直播话术不存在 */
    public static final int SESSION_SCRIPT_NOT_FOUND = 3304;
    /** 非法状态转换（如 live→preparing、ended→live） */
    public static final int LIVE_STATUS_TRANSITION_INVALID = 3305;
    /** 开播前置条件不满足（产品/人设/话术/合规） */
    public static final int LIVE_READINESS_NOT_MET = 3306;
    /** 产品不属于同一账号 */
    public static final int LIVE_PRODUCT_ACCOUNT_MISMATCH = 3307;
    /** 人设未激活或不属于当前用户 */
    public static final int LIVE_PERSONA_INVALID = 3308;
    /** 话术生成失败 */
    public static final int LIVE_SCRIPT_GENERATE_FAIL = 3309;
    /** 违规检测服务不可用 */
    public static final int LIVE_VIOLATION_SERVICE_UNAVAILABLE = 3310;
    /** 数据同步失败 */
    public static final int LIVE_DATA_SYNC_FAIL = 3311;
    /** AI 分析服务不可用 */
    public static final int LIVE_AI_ANALYSIS_FAIL = 3312;
    /** 重复添加产品 */
    public static final int LIVE_PRODUCT_DUPLICATE = 3313;
    /** 没有话术段落 */
    public static final int LIVE_NO_SCRIPTS_FOUND = 3314;
    /** 话术段落不存在 */
    public static final int LIVE_SCRIPT_NOT_FOUND = 3315;
    /** 已在最后一个话术段落 */
    public static final int LIVE_SCRIPT_ALREADY_LAST = 3316;
    /** 已在第一个话术段落 */
    public static final int LIVE_SCRIPT_ALREADY_FIRST = 3317;
    /** 无效的话术段落序号 */
    public static final int LIVE_INVALID_SLOT_INDEX = 3318;
    /** 无访问权限 */
    public static final int LIVE_ACCESS_DENIED = 3319;
    /** 没有当前话术段落 */
    public static final int LIVE_NO_CURRENT_SCRIPT = 3320;
    /** 话术已在审核中 */
    public static final int LIVE_APPROVAL_ALREADY_PENDING = 3321;
    /** 话术已通过审核 */
    public static final int LIVE_APPROVAL_ALREADY_APPROVED = 3322;
    /** 审核记录不存在 */
    public static final int LIVE_APPROVAL_NOT_FOUND = 3323;
    /** 审核已完成，不可重复操作 */
    public static final int LIVE_APPROVAL_ALREADY_REVIEWED = 3324;
    /** 评论不存在 */
    public static final int LIVE_COMMENT_NOT_FOUND = 3325;
    /** 无权限操作评论 */
    public static final int LIVE_COMMENT_FORBIDDEN = 3326;
    /** 生成任务不存在 */
    public static final int LIVE_GENERATION_TASK_NOT_FOUND = 3327;
    /** 系统错误 */
    public static final int SYSTEM_ERROR = 1002;

    // ==================== 3400：话术 + 违规词（script） ====================

    /** 话术不存在 */
    public static final int SCRIPT_NOT_FOUND = 3401;
    /** 无权操作该话术 */
    public static final int SCRIPT_FORBIDDEN = 3402;
    /** 违规词不存在 */
    public static final int VIOLATION_WORD_NOT_FOUND = 3403;
    /** 违规词已存在 */
    public static final int VIOLATION_WORD_EXISTS = 3404;
    /** 话术模板不存在 */
    public static final int TEMPLATE_NOT_FOUND = 3405;
    /** 无权操作该话术模板 */
    public static final int TEMPLATE_FORBIDDEN = 3406;
    /** 系统模板不允许修改或删除 */
    public static final int TEMPLATE_SYSTEM_FORBIDDEN = 3407;
    /** 违规词 CSV 格式错误 */
    public static final int CSV_FORMAT_ERROR = 3408;
    /** 检测文本不能为空 */
    public static final int CHECK_TEXT_EMPTY = 3409;
    /** scope 参数无效（仅支持 all/live/video） */
    public static final int CHECK_SCOPE_INVALID = 3410;
    /** 违规词导入失败 */
    public static final int VIOLATION_IMPORT_FAIL = 3411;

    // ==================== 3600：系统配置 + 行业分类（config） ====================

    /** 配置项不存在 */
    public static final int CONFIG_NOT_FOUND = 3601;
    /** 配置保存失败 */
    public static final int CONFIG_SAVE_FAIL = 3602;
    /** 配置分组不存在 */
    public static final int CONFIG_GROUP_NOT_FOUND = 3603;
    /** 系统预设分组不允许删除 */
    public static final int CONFIG_GROUP_SYSTEM_NODELETE = 3604;
    /** 配置键已存在 */
    public static final int CONFIG_KEY_EXISTS = 3605;
    /** 行业分类不存在 */
    public static final int INDUSTRY_NOT_FOUND = 3606;
    /** 行业编码已存在 */
    public static final int INDUSTRY_CODE_EXISTS = 3607;
    /** 配置值类型校验失败 */
    public static final int CONFIG_VALUE_TYPE_INVALID = 3608;
    /** 行业分类正在被使用，不允许删除 */
    public static final int INDUSTRY_IN_USE = 3609;

    // ==================== 3500：文案库 + 审批 + 模板（copy） ====================

    /** 文案不存在 */
    public static final int COPY_LIBRARY_NOT_FOUND = 3501;
    /** 无权操作该文案 */
    public static final int COPY_LIBRARY_FORBIDDEN = 3502;
    /** 审批记录不存在 */
    public static final int COPY_APPROVAL_NOT_FOUND = 3503;
    /** 审批状态不允许该操作 */
    public static final int COPY_APPROVAL_STATUS_INVALID = 3504;
    /** 文案模板不存在 */
    public static final int COPY_TEMPLATE_NOT_FOUND = 3505;
    /** 无权操作该文案模板 */
    public static final int COPY_TEMPLATE_FORBIDDEN = 3506;
    /** 系统模板不允许修改或删除 */
    public static final int COPY_TEMPLATE_SYSTEM_FORBIDDEN = 3507;

    // ==================== 3700：文件存储（storage） ====================

    /** 存储服务未配置 */
    public static final int STORAGE_NOT_CONFIGURED = 3701;
    /** 上传失败 */
    public static final int STORAGE_UPLOAD_FAIL = 3702;
    /** 删除失败 */
    public static final int STORAGE_DELETE_FAIL = 3703;
    /** 文件不存在 */
    public static final int STORAGE_FILE_NOT_FOUND = 3704;
    /** 文件超过大小限制 */
    public static final int STORAGE_FILE_TOO_LARGE = 3705;
    /** 文件类型不允许 */
    public static final int STORAGE_FILE_TYPE_NOT_ALLOWED = 3706;
    /** 无权操作该路径下的文件（用户只能管理自己路径下的素材） */
    public static final int STORAGE_PATH_ACCESS_DENIED = 3707;
    /** 上传任务不存在 */
    public static final int UPLOAD_TASK_NOT_FOUND = 3708;
    /** 分块 MD5 校验失败 */
    public static final int UPLOAD_CHUNK_MD5_MISMATCH = 3709;
    /** 分块序号超出范围 */
    public static final int UPLOAD_INVALID_CHUNK_INDEX = 3710;
    /** 分块不完整，不能完成 */
    public static final int UPLOAD_CHUNKS_INCOMPLETE = 3711;
    /** 上传状态无效 */
    public static final int UPLOAD_STATUS_INVALID = 3712;

    // ==================== 3800：系统监控（system） ====================

    /** API 调用日志不存在 */
    public static final int SYSTEM_LOG_NOT_FOUND = 3801;
    /** API 调用日志导出失败 */
    public static final int SYSTEM_LOG_EXPORT_FAIL = 3802;
    /** 同步日志不存在 */
    public static final int SYSTEM_SYNC_LOG_NOT_FOUND = 3803;
    /** 系统健康检查失败 */
    public static final int SYSTEM_HEALTH_CHECK_FAIL = 3804;

    // ==================== 3900：企业微信（wecom） ====================

    /** 企业微信未配置 */
    public static final int WECOM_NOT_CONFIGURED = 3901;
    /** 企业微信消息发送失败 */
    public static final int WECOM_SEND_FAIL = 3902;
    /** 企业微信鉴权失败 */
    public static final int WECOM_AUTH_FAIL = 3903;

    // ==================== 4000 段：AI 能力（ai） ====================

    /** AI 调用额度已用完 */
    public static final int AI_QUOTA_EXCEEDED = 4001;
    /** AI 模型不可用（所有回退层均失败） */
    public static final int AI_MODEL_UNAVAILABLE = 4002;
    /** Prompt 模板不存在 */
    public static final int AI_PROMPT_NOT_FOUND = 4003;
    /** AI 生成失败（通用） */
    public static final int AI_GENERATE_FAIL = 4004;
    /** 知识库索引失败 */
    public static final int AI_KNOWLEDGE_INDEX_FAIL = 4005;
    /** 知识库检索失败 */
    public static final int AI_KNOWLEDGE_SEARCH_FAIL = 4006;
    /** 向量数据库不可用（已废弃，用 4050 AI_MILVUS_UNAVAILABLE 替代） */
    public static final int AI_VECTORDB_UNAVAILABLE = 4007;
    /** 进化任务执行失败 */
    public static final int AI_EVOLVE_TASK_FAIL = 4008;
    /** 进化报告质量低于入库阈值 */
    public static final int AI_EVOLVE_QUALITY_LOW = 4009;
    /** 模型配置不存在 */
    public static final int AI_MODEL_CONFIG_NOT_FOUND = 4010;
    /** 任务未配置对应模型 */
    public static final int AI_TASK_MODEL_NOT_CONFIGURED = 4011;
    /** AI 多媒体生成失败（图像/视频/TTS） */
    public static final int AI_MEDIA_GENERATE_FAIL = 4012;
    /** 进化主题与现有主题重复 */
    public static final int AI_TOPIC_DUPLICATE = 4013;

    // ---------- AI 基础设施 ----------

    /** Milvus 向量数据库不可用 */
    public static final int AI_MILVUS_UNAVAILABLE = 4050;
    /** Milvus 向量写入失败 */
    public static final int AI_MILVUS_WRITE_FAILED = 4051;
    /** Milvus 向量检索失败 */
    public static final int AI_MILVUS_SEARCH_FAILED = 4052;
    /** Elasticsearch 不可用 */
    public static final int AI_ES_UNAVAILABLE = 4053;
    /** Elasticsearch 索引写入失败 */
    public static final int AI_ES_INDEX_FAILED = 4054;
    /** Elasticsearch 检索失败 */
    public static final int AI_ES_SEARCH_FAILED = 4055;
    /** Redis 缓存不可用 */
    public static final int AI_REDIS_UNAVAILABLE = 4056;
    /** Redis 缓存操作失败 */
    public static final int AI_REDIS_CACHE_FAILED = 4057;
    /** RabbitMQ 消息队列不可用 */
    public static final int AI_RABBITMQ_UNAVAILABLE = 4058;
    /** RabbitMQ 消息发布失败 */
    public static final int AI_RABBITMQ_PUBLISH_FAILED = 4059;
    /** BGE-M3 嵌入生成失败 */
    public static final int AI_EMBED_FAILED = 4060;
    /** BGE-reranker 重排序失败 */
    public static final int AI_RERANK_FAILED = 4061;
    /** Milvus + ES 双写不一致 */
    public static final int AI_DUAL_WRITE_INCONSISTENT = 4062;
    /** 死信队列消息数超阈值 */
    public static final int AI_DLQ_THRESHOLD = 4063;

    /** 文档已加密（PDF/DOC/DOCX），需先解除密码 */
    public static final int AI_DOC_ENCRYPTED = 4064;
    /** PDF 疑似扫描件（图片型），无法提取文字 */
    public static final int AI_DOC_SCAN_ONLY = 4065;
    /** 文件类型伪装（扩展名与 magic bytes 不符） */
    public static final int AI_DOC_FAKE_TYPE = 4066;
    /** 文档解析失败（POI/PDFBox 异常） */
    public static final int AI_DOC_PARSE_FAIL = 4067;
    /** 文档超过大小限制 */
    public static final int AI_DOC_TOO_LARGE = 4068;
    /** 目录层级超限 */
    public static final int AI_IMPORT_DIR_DEPTH = 4069;
    /** 文件数量超限 */
    public static final int AI_IMPORT_FILE_LIMIT = 4070;

    // ==================== 4100 段：AI 智能体（agent） ====================

    /** 智能体不存在 */
    public static final int AGENT_NOT_FOUND = 4101;
    /** 无权操作该智能体 */
    public static final int AGENT_FORBIDDEN = 4102;
    /** 智能体任务不存在 */
    public static final int AGENT_TASK_NOT_FOUND = 4103;
    /** 智能体任务执行失败 */
    public static final int AGENT_TASK_FAIL = 4104;
    /** 智能体配置无效 */
    public static final int AGENT_CONFIG_INVALID = 4105;

    // ==================== 4200 段：A/B 测试（abtest） ====================

    /** 实验不存在或已删除 */
    public static final int AB_EXPERIMENT_NOT_FOUND = 4201;
    /** 无权访问或操作该实验 */
    public static final int AB_EXPERIMENT_FORBIDDEN = 4202;
    /** 当前实验状态不允许该操作 */
    public static final int AB_EXPERIMENT_STATUS_INVALID = 4203;
    /** 变体不存在或已删除 */
    public static final int AB_VARIANT_NOT_FOUND = 4204;
    /** 变体数量不合法（必须恰好 2 个） */
    public static final int AB_VARIANT_COUNT_INVALID = 4205;
    /** 实验已在运行中，不可重复启动 */
    public static final int AB_EXPERIMENT_ALREADY_RUNNING = 4206;

    // ==================== 4300 段：归因分析（attribution） ====================

    /** 归因记录不存在 */
    public static final int ATTR_NOT_FOUND = 4301;
    /** 无权操作该归因记录 */
    public static final int ATTR_FORBIDDEN = 4302;

    // ==================== 4400 段：支付与订单（payment） ====================

    /** 订单不存在 */
    public static final int ORDER_NOT_FOUND = 4401;
    /** 无权操作该订单 */
    public static final int ORDER_FORBIDDEN = 4402;
    /** 订单状态不允许该操作 */
    public static final int ORDER_STATUS_INVALID = 4403;
    /** 订单金额不匹配 */
    public static final int ORDER_AMOUNT_MISMATCH = 4404;
    /** 订单已存在（幂等校验） */
    public static final int ORDER_ALREADY_EXISTS = 4405;
    /** 支付失败 */
    public static final int PAYMENT_FAILED = 4406;
    /** 支付超时 */
    public static final int PAYMENT_TIMEOUT = 4407;
    /** 支付渠道不可用 */
    public static final int PAYMENT_CHANNEL_UNAVAILABLE = 4408;
    /** 支付回调验签失败 */
    public static final int PAYMENT_CALLBACK_VERIFY_FAIL = 4409;
    /** 支付交易不存在 */
    public static final int PAYMENT_TRANSACTION_NOT_FOUND = 4410;
    /** 退款不存在 */
    public static final int REFUND_NOT_FOUND = 4411;
    /** 退款状态不允许该操作 */
    public static final int REFUND_STATUS_INVALID = 4412;
    /** 退款金额超过订单金额 */
    public static final int REFUND_AMOUNT_EXCEED = 4413;
    /** 退款失败 */
    public static final int REFUND_FAILED = 4414;
    /** 结算记录不存在 */
    public static final int SETTLEMENT_NOT_FOUND = 4415;
    /** 结算金额不匹配 */
    public static final int SETTLEMENT_AMOUNT_MISMATCH = 4416;

    // ==================== 4500 段：监控与告警（monitoring） ====================

    /** 指标数据不存在 */
    public static final int METRIC_NOT_FOUND = 4501;
    /** 指标收集失败 */
    public static final int METRIC_COLLECTION_FAIL = 4502;
    /** 告警规则不存在 */
    public static final int ALERT_RULE_NOT_FOUND = 4503;
    /** 无权操作该告警规则 */
    public static final int ALERT_RULE_FORBIDDEN = 4504;
    /** 告警规则参数无效 */
    public static final int ALERT_RULE_INVALID = 4505;
    /** 告警规则已存在 */
    public static final int ALERT_RULE_EXISTS = 4506;
    /** 告警执行失败 */
    public static final int ALERT_EXECUTION_FAIL = 4507;
    /** 告警通知发送失败 */
    public static final int ALERT_NOTIFICATION_FAIL = 4508;
    /** 告警记录不存在 */
    public static final int ALERT_RECORD_NOT_FOUND = 4509;
    /** 链路追踪数据不可用 */
    public static final int TRACE_DATA_UNAVAILABLE = 4510;
    /** 日志聚合失败 */
    public static final int LOG_AGGREGATION_FAIL = 4511;
    /** 仪表板数据聚合失败 */
    public static final int DASHBOARD_DATA_AGGREGATION_FAIL = 4512;
    /** Prometheus 服务不可用 */
    public static final int PROMETHEUS_UNAVAILABLE = 4513;

    // ==================== 5000 段：日志（log） ====================

    /** 无权限查询日志 */
    public static final int LOG_QUERY_FORBIDDEN = 5001;
    /** 日志导出失败 */
    public static final int LOG_EXPORT_FAIL = 5002;

    // ==================== 5100 段：短信（sms） ====================

    /** 短信服务未配置 */
    public static final int SMS_NOT_CONFIGURED = 5101;
    /** 短信发送失败 */
    public static final int SMS_SEND_FAIL = 5102;
    /** 短信提供商不可用 */
    public static final int SMS_PROVIDER_UNAVAILABLE = 5103;
    /** 短信模板不存在 */
    public static final int SMS_TEMPLATE_NOT_FOUND = 5104;
    /** 验证码无效或已过期 */
    public static final int SMS_CODE_INVALID = 5105;
    /** 验证码已过期 */
    public static final int SMS_CODE_EXPIRED = 5106;
    /** 验证码尝试次数超限 */
    public static final int SMS_CODE_ATTEMPT_LIMIT = 5107;
    /** 手机号格式不合法 */
    public static final int SMS_PHONE_INVALID = 5108;
    /** 短信发送频率超限（防止滥用） */
    public static final int SMS_SEND_RATE_LIMIT = 5109;
    /** 日发送配额已用完 */
    public static final int SMS_DAILY_QUOTA_EXCEEDED = 5110;
}
