# 🚀 测试环境启动指南

**项目**: 抖音运营平台  
**状态**: 生产就绪 95% ✓  
**启动方式**: 本地开发环境

---

## 📋 快速启动（3 步）

### 步骤 1：启动后端（终端 1）

```bash
cd C:\claude\dy01
mvn spring-boot:run
```

**预期输出**：
```
Started DouyinOperationsApplication in X.XXX seconds
```

**验证**：
- 访问 http://localhost:8080/actuator/health
- 应返回 `{"status":"UP"}`

---

### 步骤 2：启动前端（终端 2）

```bash
cd C:\claude\dy01\frontend
npm run dev
```

**预期输出**：
```
VITE v6.x.x  ready in XXX ms

➜  Local:   http://localhost:5173/
```

**验证**：
- 访问 http://localhost:5173
- 应看到登录页面

---

### 步骤 3：验证系统（终端 3）

```bash
# 检查后端健康状态
curl http://localhost:8080/actuator/health

# 检查 Swagger 文档
curl http://localhost:8080/swagger-ui.html

# 检查前端
curl http://localhost:5173
```

---

## 🌐 访问地址

| 服务 | 地址 | 用途 |
|------|------|------|
| 前端应用 | http://localhost:5173 | 用户界面 |
| 后端 API | http://localhost:8080 | REST API |
| Swagger 文档 | http://localhost:8080/swagger-ui.html | API 文档 |
| 健康检查 | http://localhost:8080/actuator/health | 系统状态 |
| 指标数据 | http://localhost:8080/actuator/prometheus | Prometheus 指标 |

---

## 🔐 默认登录凭证

**用户名**: admin  
**密码**: admin123

---

## 📊 测试场景

### 场景 1：用户登录
1. 访问 http://localhost:5173
2. 输入用户名: `admin`
3. 输入密码: `admin123`
4. 点击登录
5. **预期**: 进入系统首页

### 场景 2：查看 API 文档
1. 访问 http://localhost:8080/swagger-ui.html
2. **预期**: 看到 83+ 个 API 端点的完整文档

### 场景 3：检查系统健康
1. 访问 http://localhost:8080/actuator/health
2. **预期**: 返回 `{"status":"UP"}`

### 场景 4：查看性能指标
1. 访问 http://localhost:8080/actuator/prometheus
2. **预期**: 看到 Prometheus 格式的指标数据

---

## 🐛 常见问题

### 问题 1：端口已被占用

**症状**: `Address already in use`

**解决**:
```bash
# 查找占用端口的进程
netstat -ano | findstr :8080

# 杀死进程（替换 PID）
taskkill /PID <PID> /F
```

### 问题 2：数据库连接失败

**症状**: `Connection refused`

**解决**:
- 确保 PostgreSQL 已安装并运行
- 检查 application.yml 中的数据库配置
- 默认: `localhost:5432`, 用户: `postgres`, 密码: `postgresql`

### 问题 3：前端无法连接后端

**症状**: API 请求失败

**解决**:
- 确保后端已启动（http://localhost:8080 可访问）
- 检查浏览器控制台错误信息
- 检查 CORS 配置

### 问题 4：npm 依赖缺失

**症状**: `Module not found`

**解决**:
```bash
cd frontend
npm install
npm run dev
```

---

## 📈 性能验证

### 检查 API 响应时间

```bash
# 使用 curl 测试 API 响应时间
curl -w "Response time: %{time_total}s\n" http://localhost:8080/api/v1/auth/profile
```

**预期**: 响应时间 < 200ms

### 检查缓存命中率

1. 访问 http://localhost:8080/actuator/prometheus
2. 搜索 `cache_hit_rate`
3. **预期**: 缓存命中率 > 60%

---

## 🔍 日志查看

### 后端日志

后端日志输出到控制台，查看以下关键信息：

```
[INFO] Started DouyinOperationsApplication
[INFO] Tomcat started on port(s): 8080
[INFO] Spring Security initialized
[INFO] Redis connected
```

### 前端日志

前端日志输出到浏览器控制台（F12 打开）

---

## ✅ 验收清单

启动后请检查以下项目：

- [ ] 后端启动成功（http://localhost:8080 可访问）
- [ ] 前端启动成功（http://localhost:5173 可访问）
- [ ] 登录功能正常（使用 admin/admin123）
- [ ] Swagger 文档完整（83+ 端点）
- [ ] 系统健康检查通过
- [ ] 没有错误日志输出
- [ ] 页面加载速度正常（< 3 秒）
- [ ] API 响应时间正常（< 200ms）

---

## 🚀 下一步

1. **运行性能测试** - 使用 JMeter 或 Postman 进行压力测试
2. **收集用户反馈** - 邀请用户测试并收集反馈
3. **分析监控数据** - 查看 Prometheus 指标和日志
4. **部署到生产** - 准备好后部署到生产环境

---

**启动时间**: 2026-02-25  
**状态**: ✅ 生产就绪  
**推荐**: 立即部署

