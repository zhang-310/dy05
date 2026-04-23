# 单元测试执行指南

## 快速开始

### 1. 运行所有测试
```bash
# 执行所有单元测试
mvn clean test

# 执行测试并显示详细信息
mvn clean test -X

# 使用新增依赖的快速运行
mvn test -q
```

### 2. 运行特定模块的测试
```bash
# Auth 模块测试
mvn test -Dtest=cn.gaifan.douyinOperations.module.auth.**

# Config 模块测试
mvn test -Dtest=cn.gaifan.douyinOperations.module.config.**

# Log 模块测试
mvn test -Dtest=cn.gaifan.douyinOperations.module.log.**
```

### 3. 运行特定的测试类
```bash
# 运行 AuthControllerTest
mvn test -Dtest=AuthControllerTest

# 运行 ConfigServiceTest
mvn test -Dtest=ConfigServiceTest

# 运行 LogControllerTest
mvn test -Dtest=LogControllerTest
```

### 4. 运行特定的测试方法
```bash
# 运行特定测试方法
mvn test -Dtest=AuthLoginServiceTest#testLogin_Success

# 使用正则表达式运行多个相似方法
mvn test -Dtest=AuthLoginServiceTest#testLogin*
```

### 5. 生成测试报告
```bash
# 生成 Surefire 测试报告（HTML 格式）
mvn clean test
# 报告位置: target/site/surefire-report.html

# 生成 JaCoCo 代码覆盖率报告
mvn clean test jacoco:report
# 报告位置: target/site/jacoco/index.html

# 同时生成所有报告
mvn clean test jacoco:report site
# 访问: target/site/index.html
```

## 测试框架详解

### BaseTest 基类使用

```java
@DisplayName("My Test Class")
public class MyControllerTest extends BaseTest {

    // mockMvc: 自动注入，用于测试 HTTP 请求
    // objectMapper: 自动初始化，用于 JSON 转换
    // webApplicationContext: 自动注入，Web 应用上下文

    @Test
    @DisplayName("Test description")
    void testSomething() throws Exception {
        // 使用 mockMvc 进行 HTTP 测试
        mockMvc.perform(get("/api/v1/test"))
            .andExpect(status().isOk());
    }
}
```

### Mock 和 Stub 模式

#### Service 层单元测试（使用 @Mock）
```java
@ExtendWith(MockitoExtension.class)
public class MyServiceTest {

    @Mock
    private MyRepository repository;

    private MyService service;

    @BeforeEach
    void setup() {
        service = new MyServiceImpl();
        // 使用反射注入 mock
        ReflectionTestUtils.setField(service, "repository", repository);
    }

    @Test
    void testServiceMethod() {
        // Arrange
        when(repository.findById(1L)).thenReturn(Optional.of(entity));

        // Act
        MyResult result = service.getById(1L);

        // Assert
        assertThat(result).isNotNull();
        verify(repository, times(1)).findById(1L);
    }
}
```

#### Controller 层集成测试（使用 @MockBean）
```java
@DisplayName("My Controller Test")
public class MyControllerTest extends BaseTest {

    @MockBean
    private MyService service;

    @Test
    void testEndpoint() throws Exception {
        // Arrange
        when(service.getData()).thenReturn(data);

        // Act & Assert
        mockMvc.perform(get("/api/v1/data"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(200));
    }
}
```

## 常见测试场景

### 1. REST API 测试
```java
@Test
@DisplayName("Should return user by ID")
void testGetUser() throws Exception {
    when(userService.getById(1L)).thenReturn(userVO);

    mockMvc.perform(get("/api/v1/users/1")
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(1))
        .andExpect(jsonPath("$.data.name").value("John"));
}
```

### 2. POST 请求测试
```java
@Test
@DisplayName("Should create user successfully")
void testCreateUser() throws Exception {
    CreateUserVO vo = new CreateUserVO();
    vo.setName("John");
    vo.setEmail("john@example.com");

    when(userService.create(any())).thenReturn(1L);

    mockMvc.perform(post("/api/v1/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(vo)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").value(1));
}
```

### 3. 异常处理测试
```java
@Test
@DisplayName("Should handle user not found exception")
void testUserNotFound() throws Exception {
    when(userService.getById(999L))
        .thenThrow(new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));

    mockMvc.perform(get("/api/v1/users/999"))
        .andExpect(status().isOk());
}
```

### 4. 验证参数测试
```java
@Test
@DisplayName("Should validate required parameters")
void testParameterValidation() throws Exception {
    // 不提供必填字段
    mockMvc.perform(post("/api/v1/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isBadRequest());
}
```

## 代码覆盖率

### 查看覆盖率报告
```bash
# 生成报告
mvn clean test jacoco:report

# 在浏览器中打开
# Windows: start target/site/jacoco/index.html
# Mac: open target/site/jacoco/index.html
# Linux: xdg-open target/site/jacoco/index.html
```

### 覆盖率指标解释
- **指令覆盖率** (Instruction Coverage): 代码行执行比例
- **分支覆盖率** (Branch Coverage): if/else 等分支覆盖
- **行覆盖率** (Line Coverage): 完整代码行覆盖
- **方法覆盖率** (Method Coverage): 方法执行比例

### 覆盖率目标
- 核心业务逻辑: ≥ 70%
- Service 层: ≥ 60%
- Controller 层: ≥ 50%
- 工具类: ≥ 80%

## 调试技巧

### 1. 在 IDE 中直接运行测试
```
右键点击测试类或方法 → Run 或 Debug
```

### 2. 启用详细日志
```bash
# 在测试配置中启用 DEBUG 级别
# application-test.properties
logging.level.cn.gaifan.douyinOperations=DEBUG
```

### 3. 查看测试执行过程
```bash
# 使用 -X 参数查看详细输出
mvn test -X

# 使用 -e 参数显示完整错误堆栈
mvn test -e
```

### 4. 断点调试
```java
@Test
void testWithBreakpoint() {
    // 在这里设置断点（Ctrl+Shift+B 或右键→Breakpoint）
    MyResult result = service.doSomething();
    // 单步执行查看执行过程
    assertThat(result).isNotNull();
}
```

## 性能优化

### 1. 并行执行测试
```bash
# 使用 Maven Surefire 的并行执行
mvn test -DparallelTestClasses
```

### 2. 跳过测试
```bash
# 构建时跳过测试
mvn clean install -DskipTests

# 编译但跳过测试执行
mvn clean compile
```

### 3. 选择性运行
```bash
# 运行包含特定关键字的测试
mvn test -Dtest=**Auth**

# 排除特定测试
mvn test -Dtest=!**IntegrationTest
```

## 故障排除

### 问题：测试因数据库连接失败
**解决**:
```bash
# 确保 H2 配置正确
# application-test.properties 已正确配置

# 检查依赖是否已安装
mvn dependency:resolve
```

### 问题：Mock 不生效
**解决**:
```java
// 确保使用了正确的注解
@MockBean  // 对于 Spring Bean
@Mock     // 对于普通对象

// 确保在 @BeforeEach 中初始化 MockitoAnnotations
@BeforeEach
void setup() {
    MockitoAnnotations.openMocks(this);
}
```

### 问题：测试超时
**解决**:
```java
// 添加超时限制
@Test
@Timeout(5)  // 5 秒超时
void testLongRunning() {
    // 测试代码
}
```

### 问题：测试间数据污染
**解决**:
```java
// 使用 @Transactional 确保事务回滚
@SpringBootTest
@Transactional  // 每个测试后自动回滚
public class MyTest extends BaseTest {
}
```

## 最佳实践检查清单

- [ ] 测试类名以 `Test` 或 `Tests` 结尾
- [ ] 测试方法使用 `@Test` 注解
- [ ] 测试方法名清晰表达测试意图
- [ ] 使用 `@DisplayName` 提供中文描述
- [ ] 严格遵循 AAA 模式
- [ ] 使用合适的断言库（AssertJ/Hamcrest）
- [ ] Mock 外部依赖
- [ ] 验证 Mock 调用次数（verify）
- [ ] 测试异常场景
- [ ] 避免测试间依赖
- [ ] 使用 @Transactional 确保隔离
- [ ] 覆盖边界情况
- [ ] 使用有意义的变量名
- [ ] 保持测试简洁
- [ ] 定期运行测试

## 相关文档
- [JUnit 5 文档](https://junit.org/junit5/)
- [Mockito 文档](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [AssertJ 文档](https://assertj.github.io/assertj-core/)
- [Spring Boot 测试文档](https://spring.io/guides/gs/testing-web/)
