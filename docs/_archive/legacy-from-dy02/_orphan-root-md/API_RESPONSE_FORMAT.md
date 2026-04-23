# API 响应体格式统一方案

## 概述

本项目使用统一的响应体格式 `RESTResult<T>` 来封装所有 API 响应。

## 响应体类型

### 1. RESTResult<T>（推荐使用）

**位置**: `cn.gaifan.douyinOperations.common.vo.RESTResult`

**字段说明**:
```java
{
  "status": 0,              // 状态码：0=成功，非0=失败
  "message": "操作成功",     // 响应消息
  "data": {},               // 响应数据（泛型）
  "timestamp": "...",       // 时间戳
  "error": null,            // 错误信息（失败时）
  "valid": true,            // 验证结果
  "duration": 123,          // 执行耗时（毫秒）
  "traceId": "..."          // 追踪 ID
}
```

**使用示例**:
```java
// 成功响应（无数据）
return RESTResult.success();

// 成功响应（带数据）
return RESTResult.getSuccess(data);

// 失败响应
return RESTResult.error(ErrorCode.VALIDATION_FAIL, "参数错误");

// 新增成功
return RESTResult.addSuccess(id);

// 更新成功
return RESTResult.updateSuccess(null);

// 删除成功
return RESTResult.deleteSuccess(null);
```

### 2. Result<T>（向后兼容）

**位置**: `cn.gaifan.douyinOperations.common.vo.Result`

**说明**: Result 类继承自 RESTResult，提供向后兼容支持。

**标记**: `@Deprecated` - 建议新代码使用 RESTResult

**使用场景**:
- KnowledgeBaseController
- ShortVideoAiController
- 其他历史遗留代码

**迁移建议**: 逐步将 Result 替换为 RESTResult

## 当前使用情况

### 使用 RESTResult 的 Controller（推荐）

- AuthController
- AuthResourceController
- AuthRoleController
- DouyinController
- PersonaController
- ScriptController
- LiveSessionController
- LiveAiController
- ProductController
- ProductScriptController
- FanProfileController
- EvolutionController
- 其他大部分 Controller

### 使用 Result 的 Controller（需迁移）

- KnowledgeBaseController
- ShortVideoAiController

## 统一方案

### 阶段 1：创建兼容层（已完成）

创建 `Result<T>` 类继承 `RESTResult<T>`，确保现有代码不受影响。

### 阶段 2：逐步迁移（建议）

1. 新增 API 统一使用 `RESTResult`
2. 修改现有 API 时，将 `Result` 替换为 `RESTResult`
3. 在 IDE 中标记 `Result` 为 `@Deprecated`

### 阶段 3：完全统一（可选）

1. 更新所有使用 `Result` 的 Controller
2. 更新前端 API 调用代码
3. 移除 `Result` 类（保留一个版本周期）

## 错误码规范

使用 `ErrorCode` 枚举定义标准错误码：

```java
public enum ErrorCode {
    SUCCESS(0, "操作成功"),
    VALIDATION_FAIL(400, "参数验证失败"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "无权限"),
    DATA_NOT_FOUND(404, "数据不存在"),
    DATA_ALREADY_EXISTS(409, "数据已存在"),
    INTERNAL_ERROR(500, "服务器内部错误");
}
```

## 最佳实践

### 1. Controller 层

```java
@RestController
@RequestMapping("/api/v1/example")
public class ExampleController {

    @PostMapping("/create")
    public RESTResult<Long> create(@RequestBody ExampleVO vo) {
        Long id = exampleService.create(vo);
        return RESTResult.addSuccess(id);
    }

    @GetMapping("/{id}")
    public RESTResult<ExampleVO> get(@PathVariable Long id) {
        ExampleVO data = exampleService.getById(id);
        return RESTResult.getSuccess(data);
    }

    @PostMapping("/update")
    public RESTResult<Void> update(@RequestBody ExampleVO vo) {
        exampleService.update(vo);
        return RESTResult.updateSuccess(null);
    }

    @PostMapping("/delete")
    public RESTResult<Void> delete(@RequestParam Long id) {
        exampleService.delete(id);
        return RESTResult.deleteSuccess(null);
    }
}
```

### 2. 异常处理

使用全局异常处理器统一处理异常：

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public RESTResult<Void> handleBusinessException(BusinessException e) {
        return RESTResult.error(e.getErrorCode(), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public RESTResult<Void> handleException(Exception e) {
        return RESTResult.error(ErrorCode.INTERNAL_ERROR, e.getMessage());
    }
}
```

### 3. 分页响应

使用 `PageResultVO<T>` 封装分页数据：

```java
@PostMapping("/list")
public RESTResult<PageResultVO<ExampleVO>> list(@RequestBody SearchVO vo) {
    PageResultVO<ExampleVO> page = exampleService.search(vo);
    return RESTResult.getSuccess(page);
}
```

## 前端对接

### 响应处理

```typescript
interface ApiResponse<T> {
  status: number;
  message: string;
  data: T;
  timestamp: string;
  error?: string;
  valid: boolean;
  duration?: number;
  traceId?: string;
}

// Axios 拦截器
axios.interceptors.response.use(
  (response) => {
    const res = response.data as ApiResponse<any>;
    if (res.status === 0) {
      return res.data;
    } else {
      ElMessage.error(res.message || res.error);
      return Promise.reject(new Error(res.message));
    }
  },
  (error) => {
    ElMessage.error(error.message);
    return Promise.reject(error);
  }
);
```

## 迁移检查清单

- [x] 创建 Result 兼容类
- [ ] 更新 KnowledgeBaseController 使用 RESTResult
- [ ] 更新 ShortVideoAiController 使用 RESTResult
- [ ] 更新前端 API 调用（如需要）
- [ ] 添加 @Deprecated 注解到 Result 类
- [ ] 文档更新完成

## 注意事项

1. **向后兼容**: Result 类继承 RESTResult，确保现有代码正常运行
2. **渐进式迁移**: 不强制一次性修改所有代码
3. **统一标准**: 新代码统一使用 RESTResult
4. **错误处理**: 使用 ErrorCode 枚举，避免硬编码
5. **追踪 ID**: 使用 MDC 设置 traceId，便于问题排查

## 相关文档

- [RESTResult 源码](../src/main/java/cn/gaifan/douyinOperations/common/vo/RESTResult.java)
- [Result 源码](../src/main/java/cn/gaifan/douyinOperations/common/vo/Result.java)
- [ErrorCode 枚举](../src/main/java/cn/gaifan/douyinOperations/common/constant/ErrorCode.java)
