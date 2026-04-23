# Copy 模块（文案库管理）部署指南

## 模块概述

Copy 模块是一个完整的文案库管理系统，包含以下核心功能：

1. **文案库管理** - 创建、编辑、删除文案，支持分类、标签、评分等
2. **文案审核** - 管理员可以对文案进行审核（通过/拒绝），查看审核历史
3. **文案模板** - 创建包含变量占位符的模板，支持动态生成文案

## 后端部署步骤

### 1. Entity 和 Repository 部署

已生成的文件位置：
- `/src/main/java/cn/gaifan/douyinOperations/module/copy/entity/`
  - `CopyLibrary.java` - 文案库实体
  - `CopyApproval.java` - 审核记录实体
  - `CopyTemplate.java` - 模板实体
- `/src/main/java/cn/gaifan/douyinOperations/module/copy/repository/`
  - `CopyLibraryRepository.java`
  - `CopyApprovalRepository.java`
  - `CopyTemplateRepository.java`

### 2. Service 和 Controller 部署

已生成的文件位置：
- Service 接口：`/src/main/java/cn/gaifan/douyinOperations/module/copy/service/`
- Service 实现：`/src/main/java/cn/gaifan/douyinOperations/module/copy/service/impl/`
- Controller：`/src/main/java/cn/gaifan/douyinOperations/module/copy/controller/`

### 3. VO（值对象）部署

已生成的文件位置：
- `/src/main/java/cn/gaifan/douyinOperations/module/copy/vo/`
  - `CopyLibraryVO.java`, `CopyLibrarySearchVO.java`, `CopyLibrarySaveVO.java`
  - `CopyApprovalVO.java`, `CopyApprovalSearchVO.java`, `CopyApprovalSaveVO.java`
  - `CopyTemplateVO.java`, `CopyTemplateSearchVO.java`, `CopyTemplateSaveVO.java`

### 4. 数据库部署

执行以下 SQL 脚本（按顺序）：

```bash
# 1. 执行表结构脚本
psql -U postgres -d douyin_operations -f /sql/copy/schema.sql

# 2. 执行资源数据脚本（菜单、API、按钮资源）
psql -U postgres -d douyin_operations -f /sql/copy/resource-data.sql

# 3. 可选：执行示例数据脚本
psql -U postgres -d douyin_operations -f /sql/copy/sample-data.sql
```

## 前端部署步骤

### 1. API 层部署

文件位置：`/frontend/src/api/copy.ts`

包含所有后端 API 调用方法：
- `searchLibrary` - 查询文案库
- `saveLibrary` - 保存文案
- `deleteLibrary` - 删除文案
- `searchApprovals` - 查询待审核文案
- `approveItem` - 审核操作
- `searchTemplates` - 查询模板
- `generateFromTemplate` - 根据模板生成文案

### 2. 类型定义部署

文件位置：`/frontend/src/types/copy.ts`

包含所有 TypeScript 类型定义。

### 3. 路由部署

文件位置：`/frontend/src/router/modules/copy.ts`

需要在 `/frontend/src/router/index.ts` 中引入：

```typescript
import copyRoutes from './modules/copy'

// 在路由数组中添加
const routes = [
  // ... 其他路由
  ...copyRoutes,
]
```

### 4. 页面组件部署

文件位置：`/frontend/src/views/copy/`

- `LibraryManagement.vue` - 文案库管理页面
- `ApprovalManagement.vue` - 文案审核页面
- `TemplateManagement.vue` - 文案模板页面

## API 端点列表

### 文案库管理 (`/api/v1/copy/library`)

| 方法 | 端点 | 说明 |
|------|------|------|
| POST | `/search` | 分页查询文案库 |
| POST | `/get` | 获取文案详情 |
| POST | `/save` | 新增/编辑文案 |
| POST | `/delete` | 删除文案 |
| POST | `/increase-use-count` | 增加使用次数 |

### 文案审核 (`/api/v1/copy/approval`)

| 方法 | 端点 | 说明 |
|------|------|------|
| POST | `/search` | 查询待审核文案 |
| POST | `/get` | 获取审核详情 |
| POST | `/approve` | 审核操作（通过/拒绝） |
| POST | `/history` | 查询审核历史 |

### 文案模板 (`/api/v1/copy/template`)

| 方法 | 端点 | 说明 |
|------|------|------|
| POST | `/search` | 分页查询模板 |
| POST | `/get` | 获取模板详情 |
| POST | `/save` | 新增/编辑模板 |
| POST | `/delete` | 删除模板 |
| POST | `/generate` | 根据模板和变量生成文案 |

## 数据库表结构说明

### copy_library（文案库表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGSERIAL | 主键 |
| user_id | BIGINT | 用户ID |
| title | VARCHAR(256) | 文案标题 |
| content | TEXT | 文案内容 |
| category | VARCHAR(64) | 分类（商品描述/推广/营销/其他） |
| tags | VARCHAR(512) | 标签（逗号分隔） |
| word_count | INTEGER | 字数 |
| use_count | INTEGER | 使用次数 |
| rating | INTEGER | 评分（1-5） |
| status | INTEGER | 审核状态（0=待审核, 1=已审核） |
| deleted | INTEGER | 逻辑删除标记 |
| create_time | TIMESTAMP | 创建时间 |
| update_time | TIMESTAMP | 更新时间 |

### copy_approval（审核记录表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGSERIAL | 主键 |
| copy_id | BIGINT | 文案ID |
| user_id | BIGINT | 审核员ID |
| approval_status | INTEGER | 审核状态（0=拒绝, 1=通过, 2=待审核） |
| comments | TEXT | 审核意见 |
| approval_time | TIMESTAMP | 审核时间 |
| deleted | INTEGER | 逻辑删除标记 |
| create_time | TIMESTAMP | 创建时间 |
| update_time | TIMESTAMP | 更新时间 |

### copy_template（模板表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGSERIAL | 主键 |
| user_id | BIGINT | 用户ID |
| template_name | VARCHAR(256) | 模板名称 |
| template_content | TEXT | 模板内容（包含 {变量} 占位符） |
| category | VARCHAR(64) | 分类 |
| description | TEXT | 描述 |
| status | INTEGER | 状态（0=禁用, 1=有效） |
| deleted | INTEGER | 逻辑删除标记 |
| create_time | TIMESTAMP | 创建时间 |
| update_time | TIMESTAMP | 更新时间 |

## 权限配置

所有 copy 模块的功能都需要以下权限：
- **登录权限** - 必须是已登录用户
- **Admin 权限** - 必须具有 admin 角色

权限绑定已通过 `resource-data.sql` 脚本自动配置，admin 角色会自动获得所有 copy 模块的权限。

## 模板使用示例

### 创建模板

模板内容示例：
```
亲爱的 {用户名}，欢迎购买 {产品名}！
这款产品采用 {产品特点}，现在享受 {折扣} 折优惠。
立即下单，只需 {价格} 元，数量有限，先到先得！
```

### 生成文案

变量示例：
```json
{
  "用户名": "张三",
  "产品名": "夏季T恤",
  "产品特点": "100%纯棉",
  "折扣": "20",
  "价格": "89"
}
```

生成结果：
```
亲爱的 张三，欢迎购买 夏季T恤！
这款产品采用 100%纯棉，现在享受 20 折优惠。
立即下单，只需 89 元，数量有限，先到先得！
```

## 常见问题

### Q1：如何修改字段长度或类型？

如需修改表结构，请在 `schema.sql` 中进行修改，同时更新对应的 Entity 类。

### Q2：如何添加新的分类？

分类存储在 `category` 字段中，可以直接使用任意字符串。前端 UI 中已预定义了四种常用分类，可在页面中添加更多选项。

### Q3：如何实现文案分享功能？

可以在 `CopyLibraryController` 中添加一个分享接口，增加 `useCount` 计数。

### Q4：如何导出文案库？

可以在 `CopyLibraryService` 中添加 `export` 方法，支持导出为 CSV、Excel 等格式。

## 版本更新日志

### v1.0（2026-02-25）

- 初始版本发布
- 包含文案库、审核、模板三大功能模块
- 完整的后端和前端实现
- 详细的数据库脚本和权限配置

## 联系和支持

如有问题或需要技术支持，请联系开发团队。
