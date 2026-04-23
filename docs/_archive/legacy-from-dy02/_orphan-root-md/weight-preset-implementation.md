# 权重预设模板功能实现文档

## 实现时间
2026-04-04

## 功能概述

在风格权重配置的基础上，新增**权重预设模板**功能，让用户可以快速选择常用的权重组合，无需手动调整滑块。

## 核心特性

### 1. 预设模板列表
提供 5 种常用权重组合模板：

#### ⚖️ 均衡融合
- **权重分配**: 所有风格平均分配
- **适用场景**: 多风格混合讲解，无主次之分
- **风格数量**: 2-10 个风格

#### 🎯 主次分明
- **权重分配**: 主风格 70%，次风格 30%
- **适用场景**: 突出主要风格，辅以次要风格
- **风格数量**: 2 个风格
- **示例**: 专业 70% + 温暖 30%（高端护肤品）

#### 👑 主导风格
- **权重分配**: 主风格 90%，其他风格平分 10%
- **适用场景**: 极端风格，几乎完全保持主风格
- **风格数量**: 2-5 个风格
- **示例**: 专业 90% + 热情 5% + 温暖 5%（医美类产品）

#### 📊 渐进过渡
- **权重分配**: 第一风格 50%，第二风格 30%，第三风格 20%
- **适用场景**: 分段讲解，逐步过渡
- **风格数量**: 3 个风格
- **示例**: 专业 50% + 热情 30% + 温暖 20%（开场→中段→结尾）

#### 🔺 金字塔式
- **权重分配**: 第一风格 60%，第二风格 30%，其他平分 10%
- **适用场景**: 多风格组合，主次分明
- **风格数量**: 3-5 个风格
- **示例**: 专业 60% + 热情 30% + 温暖 5% + 随意 5%

### 2. 智能适配
- 根据用户选中的风格数量，自动过滤显示适用的预设模板
- 不符合风格数量要求的模板自动隐藏
- 选择预设后，自动应用到当前选中的风格

### 3. 一键应用
- 下拉选择预设模板
- 自动计算并应用权重到各风格
- 应用后仍可手动微调

## 技术实现

### 核心文件

#### 1. 预设模板定义
**文件**: `front/src/constants/styleWeightPresets.ts`

```typescript
export interface StyleWeightPreset {
  id: string
  name: string
  description: string
  icon?: string
  weights: Record<string, number>
  minStyles: number  // 最少需要几个风格
  maxStyles: number  // 最多支持几个风格
}

export const STYLE_WEIGHT_PRESETS: StyleWeightPreset[] = [
  {
    id: 'balanced',
    name: '均衡融合',
    description: '所有风格平均分配，适合多风格混合讲解',
    icon: '⚖️',
    weights: {},  // 空对象表示平均分配
    minStyles: 2,
    maxStyles: 10,
  },
  // ... 其他预设
]
```

#### 2. 预设应用逻辑
```typescript
export function applyPresetToStyles(
  preset: StyleWeightPreset,
  selectedStyles: string[]
): Record<string, number> {
  const styleCount = selectedStyles.length

  // 检查风格数量是否符合预设要求
  if (styleCount < preset.minStyles || styleCount > preset.maxStyles) {
    return {}
  }

  // 根据预设 ID 应用不同的权重分配策略
  switch (preset.id) {
    case 'balanced':
      // 平均分配
      const weight = 1.0 / styleCount
      return selectedStyles.reduce((acc, style) => {
        acc[style] = weight
        return acc
      }, {} as Record<string, number>)

    case 'primary-secondary':
      // 70% + 30%
      return {
        [selectedStyles[0]]: 0.7,
        [selectedStyles[1]]: 0.3,
      }

    // ... 其他预设逻辑
  }
}
```

#### 3. 智能过滤
```typescript
export function getApplicablePresets(styleCount: number): StyleWeightPreset[] {
  return STYLE_WEIGHT_PRESETS.filter(
    (preset) => styleCount >= preset.minStyles && styleCount <= preset.maxStyles
  )
}
```

### UI 实现

#### 1. 预设选择器
**文件**: `front/src/components/product/script-manage/ScriptGeneratePanel.tsx`

```tsx
{/* Preset Templates Dropdown */}
{getApplicablePresets(genStyles.length).length > 0 && (
  <FormControl size="small" sx={{ minWidth: 120 }}>
    <Select
      value=""
      displayEmpty
      onChange={(e) => {
        const presetId = e.target.value
        if (presetId) {
          const preset = STYLE_WEIGHT_PRESETS.find((p) => p.id === presetId)
          if (preset) {
            const newWeights = applyPresetToStyles(preset, genStyles)
            setStyleWeights(newWeights)
          }
        }
      }}
      renderValue={() => (
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
          <AutoFixHighIcon sx={{ fontSize: 16 }} />
          <Typography variant="caption">快速预设</Typography>
        </Box>
      )}
    >
      {getApplicablePresets(genStyles.length).map((preset) => (
        <MenuItem key={preset.id} value={preset.id}>
          <Box>
            <Typography variant="body2" fontWeight={500}>
              {preset.icon} {preset.name}
            </Typography>
            <Typography variant="caption" color="text.secondary">
              {preset.description}
            </Typography>
          </Box>
        </MenuItem>
      ))}
    </Select>
  </FormControl>
)}
```

#### 2. UI 布局
- 预设选择器位于"风格权重配置"标题右侧
- 下拉菜单显示图标、名称和描述
- 选择后自动应用权重，下拉菜单恢复默认状态
- 应用后用户仍可通过滑块手动微调

## 使用流程

1. **开启融合模式** - 勾选"风格融合"开关
2. **选择风格** - 选择 2 个或以上风格
3. **选择预设** - 点击"快速预设"下拉菜单
4. **查看适用模板** - 系统自动过滤显示适用的预设
5. **应用预设** - 选择一个预设模板
6. **自动应用** - 权重自动应用到各风格
7. **微调（可选）** - 通过滑块手动微调权重

## 使用示例

### 示例 1: 高端护肤品讲解
- **选择风格**: 专业、温暖
- **选择预设**: 🎯 主次分明
- **应用结果**: 专业 70%，温暖 30%
- **效果**: 主要保持专业严谨，适度融入温暖亲和

### 示例 2: 混合讲解
- **选择风格**: 专业、热情、温暖
- **选择预设**: 📊 渐进过渡
- **应用结果**: 专业 50%，热情 30%，温暖 20%
- **效果**: 开场专业介绍 → 中段热情推荐 → 结尾温暖促成

### 示例 3: 医美类产品
- **选择风格**: 专业、热情、温暖
- **选择预设**: 👑 主导风格
- **应用结果**: 专业 90%，热情 5%，温暖 5%
- **效果**: 几乎完全专业风格，仅点缀少量其他元素

## 技术优势

1. **快速配置**: 一键应用常用权重组合，无需手动调整
2. **智能过滤**: 根据风格数量自动显示适用预设
3. **灵活微调**: 应用预设后仍可手动微调
4. **直观展示**: 图标 + 名称 + 描述，清晰易懂
5. **可扩展**: 易于添加新的预设模板

## 验证结果

### 编译验证
- ✅ 前端类型检查：`npm run type-check` 通过
- ✅ 后端编译：`mvn compile` 通过

### 功能验证
- ✅ 预设模板定义：5 种预设正确定义
- ✅ 智能过滤：根据风格数量正确过滤
- ✅ 权重应用：选择预设后正确应用权重
- ✅ UI 交互：下拉菜单正常工作
- ✅ 手动微调：应用预设后仍可手动调整

## 文件变更清单

### 新增文件
1. `front/src/constants/styleWeightPresets.ts` - 预设模板定义和应用逻辑

### 修改文件
1. `front/src/components/product/script-manage/ScriptGeneratePanel.tsx` - 新增预设选择器 UI

## 后续优化建议

### 短期
1. **自定义预设**: 允许用户保存自己的权重组合为预设
2. **预设收藏**: 标记常用预设，快速访问
3. **预设预览**: 鼠标悬停显示预设效果预览

### 中期
1. **智能推荐**: 根据商品类型和场景推荐最佳预设
2. **预设效果分析**: 统计各预设的使用效果和转化率
3. **预设分享**: 用户间分享自定义预设

### 长期
1. **AI 生成预设**: 根据历史数据自动生成最优预设
2. **动态预设**: 根据实时反馈动态调整预设权重
3. **行业预设库**: 提供不同行业的专业预设模板

## 总结

权重预设模板功能成功实现，为用户提供了快速配置权重的能力。核心亮点：

1. ✅ **5 种预设模板**: 覆盖常见使用场景
2. ✅ **智能过滤**: 根据风格数量自动显示适用预设
3. ✅ **一键应用**: 快速配置，无需手动调整
4. ✅ **灵活微调**: 应用后仍可手动调整
5. ✅ **代码质量**: 类型检查通过，功能完整

系统已具备生产环境部署条件，所有功能经过验证，代码质量良好。

---

**实现完成时间**: 2026-04-04 22:25
**总耗时**: 约 15 分钟
**代码质量**: ✅ 类型检查通过，✅ 编译通过，✅ 功能完整
