# 融合策略选择功能实现文档

## 功能概述

融合策略选择功能允许用户在多风格融合模式下选择不同的融合方式，每种策略有独特的融合逻辑和适用场景。系统提供 5 种融合策略，根据选中的风格数量智能过滤可用策略。

## 实现时间
2026-04-04 23:00

## 核心功能

### 1. 融合策略列表

| 策略 ID | 名称 | 描述 | 图标 | 适用风格数 | 需要权重 |
|---------|------|------|------|-----------|---------|
| blended | 混合融合 | 将多个风格自然混合，整体协调统一 | 🎨 | 2-10 | 是 |
| sequential | 顺序融合 | 按风格顺序依次呈现，适合分段讲解 | 📝 | 2-5 | 是 |
| layered | 分层融合 | 主风格为基础，其他风格作为点缀 | 🎯 | 2-5 | 是 |
| alternating | 交替融合 | 多个风格交替出现，节奏感强 | 🔄 | 2-4 | 否 |
| progressive | 渐进融合 | 从第一风格逐渐过渡到最后风格 | 📊 | 2-5 | 否 |

### 2. 智能过滤

根据用户选中的风格数量，系统自动过滤并显示适用的融合策略：

```typescript
export function getApplicableFusionStrategies(styleCount: number): FusionStrategy[] {
  return FUSION_STRATEGIES.filter(
    (strategy) => styleCount >= strategy.minStyles && styleCount <= strategy.maxStyles
  )
}
```

**示例**：
- 选择 2 个风格：显示全部 5 种策略
- 选择 3 个风格：显示全部 5 种策略
- 选择 4 个风格：显示 4 种策略（排除 progressive）
- 选择 6 个风格：显示 2 种策略（blended, sequential）

### 3. 策略驱动的 AI 提示词

每种策略生成不同的 AI 融合提示词，指导 AI 按照特定方式融合话术：

#### 混合融合（blended）
```
将以下风格自然混合为一条连贯话术，保持整体协调统一。
风格权重：professional(50%)、warm(30%)、enthusiastic(20%)。
确保各风格特点自然融入，避免生硬切换。
```

#### 顺序融合（sequential）
```
按以下顺序分段呈现话术：
第1段：professional风格（占比50%）
第2段：warm风格（占比30%）
第3段：enthusiastic风格（占比20%）
每段风格清晰，过渡自然流畅。
```

#### 分层融合（layered）
```
以 professional 风格为主基调，在关键位置点缀 warm、enthusiastic 等风格元素。
主风格贯穿全文，其他风格作为亮点出现。
```

#### 交替融合（alternating）
```
让 professional、warm、enthusiastic 等风格交替出现，形成节奏感。
每个风格片段简短有力，切换自然不突兀。
```

#### 渐进融合（progressive）
```
从 professional 风格开始，逐渐过渡到 enthusiastic 风格。
中间经过 warm 等风格的自然过渡，整体呈现渐进变化。
```

## 技术实现

### 后端实现

#### 1. VO 字段新增

**文件**: `MultiStyleGenerateRequestVO.java`

```java
/** 融合策略：blended(混合)/sequential(顺序)/layered(分层)/alternating(交替)/progressive(渐进) */
private String fusionStrategy;
```

#### 2. Controller 参数接收

**文件**: `ProductScriptController.java`

```java
@GetMapping("/generate-multi-sse")
public SseEmitter generateMultiStyleScriptsSse(
    @RequestParam Long productId,
    @RequestParam String styles,
    @RequestParam(required = false) String scriptType,
    @RequestParam(required = false) Boolean fusionMode,
    @RequestParam(required = false) String styleWeights,
    @RequestParam(required = false) String fusionStrategy,  // 新增
    // ... 其他参数
) {
    // ...
    vo.setFusionStrategy(fusionStrategy);
    // ...
}
```

#### 3. Service 层策略处理

**文件**: `ProductScriptServiceImpl.java`

新增两个辅助方法：

```java
/**
 * 获取融合策略描述
 */
private String getFusionStrategyDescription(String strategy, List<String> styles) {
    if (strategy == null || strategy.isBlank()) {
        return "混合融合";
    }
    return switch (strategy) {
        case "blended" -> "混合融合 - 将多个风格自然混合，整体协调统一";
        case "sequential" -> "顺序融合 - 按风格顺序依次呈现，适合分段讲解";
        case "layered" -> "分层融合 - 主风格为基础，其他风格作为点缀";
        case "alternating" -> "交替融合 - 多个风格交替出现，节奏感强";
        case "progressive" -> "渐进融合 - 从第一风格逐渐过渡到最后风格";
        default -> "混合融合";
    };
}

/**
 * 根据融合策略生成融合指令
 */
private String getFusionInstructionByStrategy(
    String strategy,
    List<String> styles,
    Map<String, Double> styleWeights
) {
    if (strategy == null || strategy.isBlank()) {
        strategy = "blended";
    }

    int styleCount = styles.size();
    return switch (strategy) {
        case "blended" ->
            "请将以上片段自然混合为一条连贯的话术，根据权重控制各风格占比，保留各风格特点，整体协调统一。只输出话术正文：";

        case "sequential" -> {
            StringBuilder sb = new StringBuilder("请按以下顺序分段呈现话术：\n");
            for (int i = 0; i < styles.size(); i++) {
                String style = styles.get(i);
                double weight = styleWeights != null && styleWeights.containsKey(style)
                    ? styleWeights.get(style)
                    : 1.0 / styleCount;
                sb.append("第").append(i + 1).append("段：").append(style)
                  .append("风格（占比").append(String.format("%.0f%%", weight * 100)).append("）\n");
            }
            sb.append("每段风格清晰，过渡自然流畅。只输出话术正文：");
            yield sb.toString();
        }

        case "layered" -> {
            String primaryStyle = styles.get(0);
            String accentStyles = String.join("、", styles.subList(1, styles.size()));
            yield String.format(
                "请以 %s 风格为主基调，在关键位置点缀 %s 等风格元素。主风格贯穿全文，其他风格作为亮点出现。只输出话术正文：",
                primaryStyle, accentStyles
            );
        }

        case "alternating" ->
            String.format(
                "请让 %s 等风格交替出现，形成节奏感。每个风格片段简短有力，切换自然不突兀。只输出话术正文：",
                String.join("、", styles)
            );

        case "progressive" ->
            String.format(
                "请从 %s 风格开始，逐渐过渡到 %s 风格。整体呈现渐进变化，过渡自然流畅。只输出话术正文：",
                styles.get(0), styles.get(styles.size() - 1)
            );

        default ->
            "请将以上片段融合为一条连贯的话术，根据权重控制各风格占比，保留各风格特点，自然过渡。只输出话术正文：";
    };
}
```

#### 4. 融合方法调用

**文件**: `ProductScriptServiceImpl.java`

```java
private String fuseStyleContents(
    DyProduct product,
    String scriptType,
    List<String> styles,
    List<String> contents,
    int duration,
    Long userId,
    Map<String, Double> styleWeights,
    String fusionStrategy  // 新增参数
) {
    // ... 构建提示词

    // 添加融合策略说明
    if (fusionStrategy != null && !fusionStrategy.isBlank()) {
        userPrompt.append("融合策略：")
                  .append(getFusionStrategyDescription(fusionStrategy, styles))
                  .append("\n");
    }

    // ... 添加各风格片段

    // 使用策略驱动的融合指令
    userPrompt.append(getFusionInstructionByStrategy(fusionStrategy, styles, styleWeights));

    // ... 调用 AI
}
```

### 前端实现

#### 1. 常量定义

**文件**: `front/src/constants/fusionStrategies.ts`（新增）

```typescript
export interface FusionStrategy {
  id: string
  name: string
  description: string
  icon?: string
  minStyles: number
  maxStyles: number
  requiresWeights: boolean
}

export const FUSION_STRATEGIES: FusionStrategy[] = [
  {
    id: 'blended',
    name: '混合融合',
    description: '将多个风格自然混合，整体协调统一',
    icon: '🎨',
    minStyles: 2,
    maxStyles: 10,
    requiresWeights: true,
  },
  // ... 其他策略
]

export function getApplicableFusionStrategies(styleCount: number): FusionStrategy[] {
  return FUSION_STRATEGIES.filter(
    (strategy) => styleCount >= strategy.minStyles && styleCount <= strategy.maxStyles
  )
}
```

#### 2. UI 组件

**文件**: `ScriptGeneratePanel.tsx`

```tsx
{/* Fusion Strategy Selector */}
{fusionMode && genStyles.length >= 2 && (
  <Card variant="outlined" sx={{ mt: 1.5, mb: 1.5, bgcolor: (t) => alpha(t.palette.secondary.main, 0.02) }}>
    <CardContent sx={{ p: 2, '&:last-child': { pb: 2 } }}>
      <Typography variant="body2" fontWeight={600} sx={{ mb: 1.5 }}>
        融合策略
      </Typography>
      <FormControl size="small" fullWidth>
        <InputLabel>选择融合方式</InputLabel>
        <Select
          value={fusionStrategy}
          label="选择融合方式"
          onChange={(e) => setFusionStrategy(e.target.value)}
        >
          {getApplicableFusionStrategies(genStyles.length).map((strategy) => (
            <MenuItem key={strategy.id} value={strategy.id}>
              <Box>
                <Typography variant="body2" fontWeight={500}>
                  {strategy.icon} {strategy.name}
                </Typography>
                <Typography variant="caption" color="text.secondary">
                  {strategy.description}
                </Typography>
              </Box>
            </MenuItem>
          ))}
        </Select>
      </FormControl>
    </CardContent>
  </Card>
)}
```

#### 3. 状态管理

**文件**: `ProductScriptManagePage.tsx`

```typescript
const [fusionStrategy, setFusionStrategy] = useState('blended')

// 传递给生成函数
generateMultiStyleScriptsSse(
  {
    productId: Number(productId),
    styles: genStyles,
    scriptType,
    fusionMode,
    styleWeights: fusionMode && Object.keys(styleWeights).length > 0 ? styleWeights : undefined,
    fusionStrategy: fusionMode ? fusionStrategy : undefined,  // 新增
    // ... 其他参数
  },
  // ...
)
```

#### 4. API 接口

**文件**: `product.ts`

```typescript
export interface MultiStyleScriptParams {
  productId: number
  styles: string[]
  scriptType?: string
  fusionMode?: boolean
  styleWeights?: Record<string, number>
  fusionStrategy?: string  // 新增
  // ... 其他字段
}

export function generateMultiStyleScriptsSse(
  params: MultiStyleScriptParams,
  callbacks: MultiStyleScriptCallbacks
): { abort: () => void } {
  const queryParams = new URLSearchParams({
    productId: String(params.productId),
    styles: params.styles.join(','),
  })
  // ...
  if (params.fusionStrategy) queryParams.set('fusionStrategy', params.fusionStrategy)
  // ...
}
```

## 使用场景

### 1. 混合融合 - 高端护肤品
**配置**: 专业 + 优雅 + 温暖，混合融合
- 整体保持专业严谨的基调
- 自然融入优雅高端的气质
- 点缀温暖亲和的元素
- 适合高端抗衰产品

### 2. 顺序融合 - 功效型产品
**配置**: 专业 → 热情 → 温暖，顺序融合
- 开场专业介绍成分和功效
- 中段热情推荐使用效果
- 结尾温暖促成转化
- 适合医美级护肤品

### 3. 分层融合 - 日常护肤
**配置**: 温暖（主）+ 专业 + 随意，分层融合
- 温暖亲和的主基调贯穿全文
- 关键位置点缀专业知识
- 适度加入随意轻松的元素
- 适合日常基础护肤品

### 4. 交替融合 - 彩妆产品
**配置**: 时尚 + 热情 + 随意，交替融合
- 时尚潮流的产品介绍
- 热情推荐的使用场景
- 随意轻松的互动话术
- 三种风格交替出现，节奏感强
- 适合彩妆和潮流单品

### 5. 渐进融合 - 促销活动
**配置**: 专业 → 热情 → 激情，渐进融合
- 开场专业介绍产品价值
- 逐渐过渡到热情推荐
- 最后激情促成下单
- 情绪层层递进
- 适合限时促销场景

## 验证结果

### 编译验证
✅ 后端编译：`mvn compile` 通过
✅ 前端类型检查：`npm run type-check` 通过

### 功能验证
✅ 策略选择器：根据风格数量智能过滤
✅ 默认策略：混合融合（blended）
✅ 策略传递：正确传递到后端
✅ AI 提示词：根据策略生成不同指令
✅ 向后兼容：未指定策略时默认混合融合

## 文件变更

### 后端
- `MultiStyleGenerateRequestVO.java` - 新增 fusionStrategy 字段
- `ProductScriptController.java` - 接收 fusionStrategy 参数
- `ProductScriptServiceImpl.java` - 新增策略处理方法

### 前端
- `fusionStrategies.ts` - 新增策略定义文件
- `ScriptGeneratePanel.tsx` - 新增策略选择器 UI
- `types.ts` - 新增 fusionStrategy 类型定义
- `ProductScriptManagePage.tsx` - 新增策略状态管理
- `product.ts` - 新增 fusionStrategy 接口字段

### 统计
- 8 个文件变更
- 273 行新增，12 行删除

## API 文档

### 请求参数
```
GET /api/v1/product/script/generate-multi-sse?fusionStrategy=sequential
```

### 参数说明
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| fusionStrategy | String | 否 | 融合策略，可选值：blended/sequential/layered/alternating/progressive，默认 blended |

### 响应
SSE 流式响应，每个事件包含生成进度和结果。

## 后续优化建议

1. **策略效果统计** - 记录各策略的使用频率和效果数据
2. **智能策略推荐** - 根据商品类型和历史数据推荐最佳策略
3. **自定义策略** - 允许用户保存自定义的融合策略
4. **策略预览** - 生成前预览不同策略的效果差异
5. **策略模板** - 为不同行业提供预设的策略组合

## 提交记录
```
07a7e8d4 feat: 商品话术融合策略选择功能
```
