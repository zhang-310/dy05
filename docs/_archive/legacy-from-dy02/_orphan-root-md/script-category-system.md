# 话术库分类系统深度分析

## 实现时间
2026-04-04

## 问题分析

### 原始问题
用户反馈"话术库的分类不全"，经过深度分析发现以下问题：

1. **标签前缀不一致**：代码中使用了 `category:` 前缀，但实际系统使用 `type:` 和 `cat:` 前缀
2. **分类来源单一**：仅从数据库 `script_library.category` 字段动态查询，未包含知识库自动标签
3. **标签系统未对齐**：前端展示的分类与后端 `ChunkLabeler` 自动打标系统不一致

## 完整分类体系

### 1. type: 内容类型标签（8种）

根据 `ChunkLabeler.java` 自动打标规则：

| 分类 | 关键词 | 说明 |
|------|--------|------|
| **种草** | 种草、安利、推荐理由、真的好用 | 推荐类话术 |
| **促销** | 秒杀、限时、倒计时、抢购、下单、福利价 | 促销逼单话术 |
| **产品介绍** | 成分、功效、配方、技术、参数、规格 | 产品卖点介绍 |
| **直播话术** | 姐妹们、宝子们、家人们、直播间 | 直播专用话术 |
| **情绪价值** | 情绪、共鸣、故事、经历、感动 | 情感共鸣话术 |
| **过渡话术** | 过渡、衔接、接下来、下一个 | 场景切换话术 |
| **互动话术** | 感谢、关注、点赞、粉丝、评论 | 观众互动话术 |
| **FAQ** | Q:、A:、问:、答:、常见问题 | 问答类话术 |

### 2. cat: 品类标签（5种）

| 分类 | 关键词 | 说明 |
|------|--------|------|
| **美妆护肤** | 护肤、面膜、精华、水乳、防晒、美白 | 美妆护肤品类 |
| **食品** | 零食、美食、好吃、口感、配料 | 食品饮料品类 |
| **服装** | 衣服、穿搭、面料、款式、尺码 | 服装鞋帽品类 |
| **家居** | 家电、智能、厨房、清洁、收纳 | 家居用品品类 |
| **数码** | 手机、电脑、芯片、续航、像素 | 数码电子品类 |

### 3. script_library.category 字段

数据库表中的自由文本分类字段，示例值：
- 直播
- 商品
- 促销
- 售前
- 售后
- 其他

## 技术实现修正

### 修正前的问题

```java
// 错误：使用 category: 前缀
if (label.startsWith("category:")) {
    String category = label.replace("category:", "");
    if (kbCategories.contains(category)) return true;
}
```

### 修正后的实现

```java
// 正确：同时支持 type: 和 cat: 两种前缀
for (String label : r.labels()) {
    if (label != null) {
        String category = null;
        if (label.startsWith("type:")) {
            category = label.replace("type:", "");
        } else if (label.startsWith("cat:")) {
            category = label.replace("cat:", "");
        }
        if (category != null && kbCategories.contains(category)) return true;
    }
}
```

## 前端实现

### 分类常量定义

创建 `front/src/constants/scriptCategories.ts`：

```typescript
export const SCRIPT_TYPE_CATEGORIES = [
  '种草', '促销', '产品介绍', '直播话术',
  '情绪价值', '过渡话术', '互动话术', 'FAQ',
] as const

export const SCRIPT_CAT_CATEGORIES = [
  '美妆护肤', '食品', '服装', '家居', '数码',
] as const

export const ALL_SCRIPT_CATEGORIES = [
  ...SCRIPT_TYPE_CATEGORIES,
  ...SCRIPT_CAT_CATEGORIES,
] as const
```

### 动态加载策略

```typescript
// 合并预定义分类和数据库动态分类
const dynamicCategories = await scriptApi.categories()
const allCategories = Array.from(new Set([
  ...ALL_SCRIPT_CATEGORIES,
  ...(dynamicCategories || [])
]))
setScriptCategories(allCategories)
```

## 数据流完整链路

```
1. 知识库文档入库
   ↓
2. ChunkLabeler 自动打标
   ↓ 生成 labels: ["type:种草", "cat:美妆护肤"]
3. 存储到向量库/ES
   ↓
4. 用户选择分类 ["种草", "美妆护肤"]
   ↓
5. RAG 检索时过滤
   ↓ 匹配 type:种草 或 cat:美妆护肤
6. 返回匹配的话术
   ↓
7. LLM 生成参考
```

## 完整分类列表（13种）

### 内容类型（8种）
1. 种草
2. 促销
3. 产品介绍
4. 直播话术
5. 情绪价值
6. 过渡话术
7. 互动话术
8. FAQ

### 品类标签（5种）
9. 美妆护肤
10. 食品
11. 服装
12. 家居
13. 数码

### 动态分类（数据库）
- 根据实际数据动态加载
- 与预定义分类合并去重

## 使用建议

### 1. 内容创作者
- 选择 **type:** 类型标签定位话术风格
- 选择 **cat:** 品类标签定位行业领域
- 可多选组合，如 "种草 + 美妆护肤"

### 2. 系统管理员
- 定期检查 `script_library.category` 字段的数据质量
- 确保知识库文档包含足够的关键词以触发自动打标
- 监控 RAG 检索的分类过滤效果

### 3. 开发者
- 新增分类需同步更新 `ChunkLabeler.java` 和 `scriptCategories.ts`
- 标签前缀规范：`type:` 用于内容类型，`cat:` 用于品类
- 避免使用 `category:` 前缀（已废弃）

## 测试验证

### 1. 标签生成测试
```java
String text = "姐妹们，这款面膜真的好用，限时秒杀！";
Set<String> labels = ChunkLabeler.label(text);
// 预期结果: ["type:直播话术", "type:种草", "type:促销", "cat:美妆护肤"]
```

### 2. 分类过滤测试
- 选择 "直播话术"：应返回包含 `type:直播话术` 标签的文档
- 选择 "美妆护肤"：应返回包含 `cat:美妆护肤` 标签的文档
- 选择 "直播话术 + 美妆护肤"：返回同时包含两个标签的文档

### 3. 前端显示测试
- 访问 `/admin/product/206/scripts`
- 开启"参考话术知识库"开关
- 验证下拉框显示所有 13 种预定义分类
- 验证动态分类正确合并

## 性能优化

### 1. 标签索引
```sql
-- 为 labels 字段创建 GIN 索引（PostgreSQL）
CREATE INDEX idx_kb_chunk_labels ON ai_kb_chunk USING GIN (labels);
```

### 2. 缓存策略
- 分类列表缓存 15 分钟
- 标签匹配结果缓存 5 分钟
- 使用 Redis 存储热门分类组合

## 未来扩展

### 1. 更多品类
- 母婴用品
- 运动健身
- 图书文具
- 宠物用品

### 2. 更多类型
- 开场话术
- 结束话术
- 答疑话术
- 售后话术

### 3. 智能推荐
- 根据商品自动推荐分类
- 根据历史生成记录推荐
- 根据效果数据优化分类权重

## 总结

通过深度分析，发现并修正了分类系统的三个关键问题：
1. ✅ 标签前缀从 `category:` 修正为 `type:` 和 `cat:`
2. ✅ 前端分类从 13 种预定义 + 动态加载
3. ✅ RAG 过滤逻辑支持两种标签前缀

现在系统完整支持 **13 种预定义分类 + 数据库动态分类**，用户可以精确控制话术生成的参考来源。
