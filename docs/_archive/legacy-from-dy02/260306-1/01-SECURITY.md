# 01 安全漏洞与加固方案

> 综合评分：35/100（F 级）
> 发现问题：18 项（P0: 5 / P1: 7 / P2: 6）

---

## P0 - 必须立即修复

### SEC-01: API 密钥已泄露到 Git 仓库

**严重性**: 致命
**文件**: `.env`
**内容**:
```
DEEPSEEK_API_KEY=sk-e9f50f1e990c4db8b0d4499d0b28145e
IFLYTEK_APP_ID=5d5b50ae
IFLYTEK_API_KEY=51dc343ae302e435f7c422bfc1c2584b
IFLYTEK_API_SECRET=d544008aadef685a4d70a1f62b62f51c
KLING_API_KEY=AByCQaBtQhCPpYNn88PaReKAFterCr3t
KLING_API_SECRET=AKKDMGrPbHLQLeapJKJMkCmHfDTM3eyf
```

**风险**: 攻击者可直接使用这些密钥调用 AI 服务，产生费用或泄露数据。

**修复方案**:
1. 立即轮换所有泄露的 API 密钥
2. 将 `.env` 添加到 `.gitignore`
3. 使用 `git filter-branch` 或 BFG Repo-Cleaner 从历史中删除
4. 仅保留 `.env.example`（不含真实值）

---

### SEC-02: 5 个 Controller 硬编码 userId = 1L

**严重性**: 致命
**影响**: 所有用户以同一身份操作，多租户数据隔离完全失效

**受影响 Controller**:

| Controller | 方法 | 行号 |
|-----------|------|------|
| ProductScriptVersionController | extractUserIdFromAuth() | 217-220 |
| EffectivenessScoreController | extractUserIdFromAuth() | 169-178 |
| ScriptOptimizationController | extractUserIdFromAuth() | 待查 |
| KnowledgeEvolutionController | extractUserIdFromAuth() | 待查 |
| LiveScriptNavigationController | extractUserIdFromAuth() | 待查 |

**问题代码**:
```java
private Long extractUserIdFromAuth(String auth) {
    return 1L;  // 所有请求都以 userId=1 执行
}
```

**正确做法**（参考 AbTestController）:
```java
Long userId = AuthTokenFilter.getUserId(request);
if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
```

---

### SEC-03: Service 层 getById 无 owner 校验

**严重性**: 高
**文件**: `LiveScriptServiceImpl.java`

**问题代码**:
```java
public LiveScriptVO getById(Long id) {
    LiveScript script = liveScriptRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "不存在"));
    return toLiveScriptVO(script);  // 未验证 userId
}
```

**修复**: 所有 getById 方法必须验证 `entity.getOwnerId().equals(currentUserId)`

---

### SEC-04: 生产环境 Token Secret 有默认值

**文件**: `application.yml:118`
```yaml
secret: ${APP_TOKEN_SECRET:dev-only-change-in-production-2025!@#$%}
```

**修复**: 删除默认值，强制通过环境变量注入

---

### SEC-05: Docker Compose 默认密码

**文件**: `docker/docker-compose.yml`
```yaml
RABBITMQ_DEFAULT_USER: ${RABBITMQ_USER:-guest}
RABBITMQ_DEFAULT_PASS: ${RABBITMQ_PASS:-guest}
```

**修复**: 移除默认值，强制通过 `.env` 配置

---

## P1 - 应尽快修复

### SEC-06: CORS 配置过于宽松
- `SecurityConfig.java:50` — `setAllowedHeaders(List.of("*"))` 与 `setAllowCredentials(true)` 组合
- **修复**: 明确列出允许的请求头

### SEC-07: 文件上传缺少 MIME Type 验证
- `UploadServiceImpl.java` — 无文件类型检查
- **修复**: 添加白名单 MIME Type 验证

### SEC-08: Nginx 无速率限制
- `docker/nginx/default.conf` — 无 `limit_req_zone`
- **修复**: 添加 `limit_req_zone $binary_remote_addr zone=api_limit:10m rate=100r/s`

### SEC-09: 生产 SSL/TLS 未启用
- `docker/nginx/gateway.conf` — HTTPS 配置已注释
- **修复**: 启用 SSL + Let's Encrypt 自动续期

### SEC-10: 缺少 CSP 头
- `docker/nginx/frontend.conf` — 无 Content-Security-Policy
- **修复**: 添加 CSP 头防止 XSS

### SEC-11: Actuator 端点网段过大
- `docker/nginx/gateway.conf:150` — `allow 172.28.0.0/16`（65536 个 IP）
- **修复**: 缩小到具体 IP

### SEC-12: 容器以 root 用户运行
- `docker/Dockerfile.backend` / `docker/Dockerfile.frontend`
- **修复**: 添加 `USER appuser`

---

## P2 - 可选优化

### SEC-13: 缺少镜像漏洞扫描（Trivy/Grype）
### SEC-14: 缺少密钥轮换机制
### SEC-15: Redis 未配置 TLS
### SEC-16: 缺少 WAF（Web Application Firewall）
### SEC-17: 基础镜像版本未固定（`maven:3.9` 未指定具体版本）
### SEC-18: 网络隔离不完整（所有容器同一 bridge 网络）
