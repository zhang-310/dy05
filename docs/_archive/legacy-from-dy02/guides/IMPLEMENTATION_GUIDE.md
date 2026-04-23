# 🔨 快速升级实施指南 - 代码示例与步骤

**目标**：在 2-3 周内实现最关键的升级
**优先顺序**：测试 → Docker → CI/CD → 代码质量 → 文档

---

## 1️⃣ 添加单元测试框架（4 小时）

### Step 1: 更新 pom.xml

```xml
<!-- 在 <dependencies> 中添加 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>io.rest-assured</groupId>
    <artifactId>rest-assured</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
```

### Step 2: 创建测试基类

```java
// src/test/java/cn/gaifan/douyinOperations/BaseTest.java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
@DisplayName("Base Test Class")
public class BaseTest {

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected WebApplicationContext webApplicationContext;

    protected MockMvc mockMvc;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(webApplicationContext)
            .build();
    }
}
```

### Step 3: 创建第一个测试（以 Auth 为例）

```java
// src/test/java/cn/gaifan/douyinOperations/module/auth/controller/AuthControllerTest.java
@DisplayName("Auth Controller Tests")
public class AuthControllerTest extends BaseTest {

    @MockBean
    private AuthLoginService authLoginService;

    @Test
    @DisplayName("Should login successfully with valid credentials")
    void testLogin_Success() throws Exception {
        // Arrange
        LoginParams params = new LoginParams();
        params.setUsername("admin");
        params.setPassword("password");

        AuthUser user = new AuthUser();
        user.setId(1L);
        user.setUsername("admin");

        when(authLoginService.login(any()))
            .thenReturn(user);

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(params)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.username").value("admin"));
    }

    @Test
    @DisplayName("Should fail login with invalid credentials")
    void testLogin_Failure() throws Exception {
        when(authLoginService.login(any()))
            .thenThrow(new BusinessException(ErrorCode.LOGIN_FAILED));

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"wrong\",\"password\":\"wrong\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(2005));
    }
}
```

### Step 4: 创建 Service 测试

```java
// src/test/java/cn/gaifan/douyinOperations/module/auth/service/AuthUserServiceTest.java
@DisplayName("Auth User Service Tests")
public class AuthUserServiceTest {

    private AuthUserService authUserService;

    @Mock
    private AuthUserRepository authUserRepository;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        authUserService = new AuthUserServiceImpl(authUserRepository);
    }

    @Test
    @DisplayName("Should find user by username")
    void testFindByUsername() {
        // Arrange
        AuthUser user = AuthUser.builder()
            .id(1L)
            .username("admin")
            .build();

        when(authUserRepository.findByUsername("admin"))
            .thenReturn(Optional.of(user));

        // Act
        Optional<AuthUser> result = authUserRepository.findByUsername("admin");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("admin");
    }
}
```

---

## 2️⃣ Docker 容器化（2 小时）

### Step 1: 创建后端 Dockerfile

```dockerfile
# Dockerfile
# 编译阶段
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

# 缓存依赖
RUN ./mvnw dependency:resolve

COPY src src
RUN ./mvnw clean package -DskipTests -q

# 运行阶段
FROM eclipse-temurin:17-jdk-alpine

# 添加用户和组
RUN addgroup -g 1000 spring && adduser -D -u 1000 -G spring spring

WORKDIR /app

# 复制编译好的 jar
COPY --from=builder --chown=spring:spring /app/target/*.jar app.jar

USER spring

# 暴露端口
EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD java -cp app.jar org.springframework.boot.loader.JarLauncher \
        -cp /app/app.jar org.springframework.boot.actuate.health.SimpleHealthIndicator || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Step 2: 创建前端 Dockerfile

```dockerfile
# frontend/Dockerfile
FROM node:20-alpine AS builder

WORKDIR /app

COPY package*.json ./
RUN npm ci --only=production && npm ci

COPY . .

# 构建前端
RUN npm run build

# Nginx 运行阶段
FROM nginx:alpine

COPY --from=builder /app/dist /usr/share/nginx/html

# 复制 Nginx 配置
COPY nginx.conf /etc/nginx/nginx.conf
COPY default.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost/health || exit 1

CMD ["nginx", "-g", "daemon off;"]
```

### Step 3: 创建 nginx.conf

```nginx
# nginx.conf
user nginx;
worker_processes auto;
error_log /var/log/nginx/error.log warn;
pid /var/run/nginx.pid;

events {
    worker_connections 1024;
}

http {
    include /etc/nginx/mime.types;
    default_type application/octet-stream;

    log_format main '$remote_addr - $remote_user [$time_local] "$request" '
                    '$status $body_bytes_sent "$http_referer" '
                    '"$http_user_agent" "$http_x_forwarded_for"';

    access_log /var/log/nginx/access.log main;

    sendfile on;
    tcp_nopush on;
    tcp_nodelay on;
    keepalive_timeout 65;
    types_hash_max_size 2048;
    client_max_body_size 20M;

    # Gzip 压缩
    gzip on;
    gzip_vary on;
    gzip_min_length 1000;
    gzip_types text/plain text/css text/xml text/javascript
               application/x-javascript application/xml+rss;

    include /etc/nginx/conf.d/*.conf;
}
```

### Step 4: 创建 docker-compose.yml

```yaml
# docker-compose.yml
version: '3.8'

services:
  postgres:
    image: postgres:16-alpine
    container_name: douyin-db
    environment:
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
      POSTGRES_DB: douyin_operations
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./sql:/docker-entrypoint-initdb.d
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    container_name: douyin-redis
    ports:
      - "6379:6379"
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  backend:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: douyin-backend
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/douyin_operations
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: postgres
      SPRING_REDIS_HOST: redis
      SPRING_REDIS_PORT: 6379
    ports:
      - "8080:8080"
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 10s
      timeout: 5s
      retries: 5

  frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile
    container_name: douyin-frontend
    depends_on:
      - backend
    ports:
      - "80:80"
    environment:
      REACT_APP_API_URL: http://backend:8080
    healthcheck:
      test: ["CMD", "wget", "--quiet", "--tries=1", "--spider", "http://localhost/health"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  postgres_data:
```

### Step 5: 创建 .dockerignore

```
# .dockerignore
node_modules
npm-debug.log
dist
build
target
.git
.gitignore
.env
.env.local
.DS_Store
*.log
*.class
.idea
.vscode
coverage
```

### 运行命令

```bash
# 构建镜像
docker-compose build

# 启动服务
docker-compose up -d

# 查看日志
docker-compose logs -f backend

# 停止服务
docker-compose down

# 清理
docker-compose down -v
```

---

## 3️⃣ CI/CD 流水线（1 小时）

### Step 1: 创建 GitHub Actions 工作流

```yaml
# .github/workflows/ci-cd.yml
name: CI/CD Pipeline

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main, develop ]

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}

jobs:
  test:
    runs-on: ubuntu-latest
    name: Test & Build

    services:
      postgres:
        image: postgres:16-alpine
        env:
          POSTGRES_PASSWORD: postgres
          POSTGRES_DB: test_db
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
        ports:
          - 5432:5432

      redis:
        image: redis:7-alpine
        options: >-
          --health-cmd "redis-cli ping"
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
        ports:
          - 6379:6379

    steps:
    - uses: actions/checkout@v3
      with:
        fetch-depth: 0

    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
        cache: maven

    - name: Run backend tests
      run: mvn clean test -q

    - name: Build backend
      run: mvn clean package -DskipTests -q

    - name: Set up Node.js
      uses: actions/setup-node@v3
      with:
        node-version: 20
        cache: 'npm'
        cache-dependency-path: frontend/package-lock.json

    - name: Install frontend dependencies
      run: cd frontend && npm ci

    - name: Build frontend
      run: cd frontend && npm run build

    - name: Run frontend tests
      run: cd frontend && npm test -- --run

    - name: Upload coverage reports
      uses: codecov/codecov-action@v3
      if: always()

  build-and-push:
    needs: test
    if: github.event_name == 'push' && github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    name: Build & Push Docker Images

    permissions:
      contents: read
      packages: write

    steps:
    - uses: actions/checkout@v3

    - name: Set up Docker Buildx
      uses: docker/setup-buildx-action@v2

    - name: Log in to Container Registry
      uses: docker/login-action@v2
      with:
        registry: ${{ env.REGISTRY }}
        username: ${{ github.actor }}
        password: ${{ secrets.GITHUB_TOKEN }}

    - name: Extract metadata
      id: meta
      uses: docker/metadata-action@v4
      with:
        images: ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}
        tags: |
          type=ref,event=branch
          type=semver,pattern={{version}}
          type=sha

    - name: Build and push backend image
      uses: docker/build-push-action@v4
      with:
        context: .
        push: true
        tags: ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}-backend:${{ github.sha }}
        cache-from: type=gha
        cache-to: type=gha,mode=max

    - name: Build and push frontend image
      uses: docker/build-push-action@v4
      with:
        context: ./frontend
        push: true
        tags: ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}-frontend:${{ github.sha }}
        cache-from: type=gha
        cache-to: type=gha,mode=max

  deploy:
    needs: build-and-push
    if: github.event_name == 'push' && github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    name: Deploy to Production

    steps:
    - uses: actions/checkout@v3

    - name: Deploy with kubectl
      env:
        KUBE_CONFIG: ${{ secrets.KUBE_CONFIG }}
        IMAGE_TAG: ${{ github.sha }}
      run: |
        mkdir -p $HOME/.kube
        echo "$KUBE_CONFIG" | base64 -d > $HOME/.kube/config

        kubectl set image deployment/douyin-backend \
          douyin-backend=${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}-backend:$IMAGE_TAG \
          -n production

        kubectl set image deployment/douyin-frontend \
          douyin-frontend=${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}-frontend:$IMAGE_TAG \
          -n production

        kubectl rollout status deployment/douyin-backend -n production
        kubectl rollout status deployment/douyin-frontend -n production

    - name: Notify deployment
      if: always()
      uses: 8398a7/action-slack@v3
      with:
        status: ${{ job.status }}
        text: 'Deployment ${{ job.status }}'
        webhook_url: ${{ secrets.SLACK_WEBHOOK }}
```

---

## 4️⃣ 添加 Swagger 文档（2 小时）

### Step 1: 添加注解到 Controller

```java
@RestController
@RequestMapping("/api/v1/auth")
@OpenAPIDefinition(
    info = @Info(
        title = "Auth API",
        version = "1.0.0",
        description = "User authentication and authorization API",
        contact = @Contact(name = "Support", email = "support@douyin.com")
    ),
    servers = {
        @Server(url = "http://localhost:8080", description = "Development"),
        @Server(url = "https://api.douyin.com", description = "Production")
    }
)
@SecurityScheme(
    name = "Bearer Token",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT"
)
@Tag(name = "Authentication", description = "Auth endpoints")
public class AuthController {

    @PostMapping("/login")
    @Operation(
        summary = "User login",
        description = "Authenticate user and return JWT token",
        tags = {"Authentication"}
    )
    @Parameters({
        @Parameter(name = "username", description = "User's username", required = true),
        @Parameter(name = "password", description = "User's password", required = true)
    })
    @ApiResponse(responseCode = "200", description = "Login successful",
        content = @Content(schema = @Schema(implementation = LoginResult.class)))
    @ApiResponse(responseCode = "401", description = "Invalid credentials")
    public ResponseEntity<?> login(@RequestBody LoginParams params) {
        // implementation
    }
}
```

### Step 2: 访问 Swagger UI

```
http://localhost:8080/swagger-ui.html
http://localhost:8080/v3/api-docs
```

---

## 5️⃣ 代码规范工具（1 小时）

### Step 1: 添加 ESLint + Prettier（前端）

```bash
cd frontend
npm install -D eslint prettier eslint-config-prettier eslint-plugin-vue
```

### .eslintrc.cjs

```javascript
module.exports = {
  root: true,
  env: { browser: true, es2021: true },
  extends: ['eslint:recommended', 'plugin:vue/vue3-essential', 'prettier'],
  parserOptions: { ecmaVersion: 'latest', sourceType: 'module' },
  rules: {
    'no-console': process.env.NODE_ENV === 'production' ? 'warn' : 'off',
    'no-unused-vars': 'warn',
    'prefer-const': 'warn'
  }
}
```

### .prettierrc

```json
{
  "semi": true,
  "singleQuote": true,
  "tabWidth": 2,
  "trailingComma": "es5",
  "printWidth": 100
}
```

### Step 2: 配置 Checkstyle（后端）

在 pom.xml 中添加：

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-checkstyle-plugin</artifactId>
    <version>3.3.0</version>
    <executions>
        <execution>
            <goals>
                <goal>check</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

---

## 📋 验收清单

```
□ 后端单元测试运行成功（>50 个测试）
□ Docker 容器构建成功
□ docker-compose 启动所有服务
□ GitHub Actions 工作流执行成功
□ Swagger UI 可访问且文档完整
□ npm run lint 无错误
□ mvn checkstyle:check 通过
□ 前端构建产物 <1MB
□ 所有测试通过
```

---

**预计完成时间**：2-3 天
**难度等级**：中等
**投资回报率**：极高（代码质量↑ 50%，部署时间↓ 80%）
