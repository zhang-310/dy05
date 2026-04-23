# CI/CD

## GitHub Actions

配置文件：`.github/workflows/ci-cd.yml`

### 流水线阶段

```
Push/PR → 后端测试（Maven + JaCoCo）→ 前端测试（tsc + Vitest + build）
       → 前端黄金路径 E2E（Playwright：e2e/golden-paths.spec.ts）
       → 代码质量检查（Sonar 等）→ 构建镜像 → 部署
```

黄金路径定义见 [对外验收口径与三层 100%](../quality/00-对外验收口径与三层100%.md)。

### 质量检查

配置文件：`.github/workflows/code-quality.yml`

- 代码风格检查
- 安全扫描
- 依赖漏洞检测

### 密钥配置

参考 `.github/SECRETS_SETUP.md` 配置 GitHub Secrets。

## 本地开发

### 后端

```bash
# 编译检查
mvn compile

# 运行（自动使用 dev profile）
mvn spring-boot:run

# 测试
mvn test

# 单个测试类
mvn test -Dtest=AuthUserServiceTest

# 打包
mvn package -DskipTests
```

### 前端

```bash
cd front

# 安装依赖
npm install

# 开发服务器（localhost:3000，/api 代理到 localhost:8080）
npm run dev

# 构建（先 tsc 再 vite build）
npm run build

# 类型检查
npm run type-check

# 测试
npm run test
npm run test:watch
npm run test:coverage

# E2E 测试（含路由全覆盖 + 黄金路径 GP-01/GP-02，见 docs/quality/00）
npm run test:e2e

# 全栈黄金路径（可选，需后端 + 测试账号）
# set E2E_FULL_STACK=1
# npm run test:e2e
```

### 后端 API 映射导出（契约清单辅助）

项目根目录 PowerShell：

```powershell
.\scripts\list-backend-mappings.ps1
# 输出 target/backend-mappings.txt
```

详见 [对外验收口径与三层 100%](../quality/00-对外验收口径与三层100%.md)。

## CODEOWNERS

配置文件：`.github/CODEOWNERS`，定义代码审核责任人。
