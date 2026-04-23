# dy02 设计系统规范提取

源文件：`DESIGN.md`
提取日期：2026-04-12

## 色彩系统映射

| 设计规范名 | CSS 变量 | 十六进制 | 用途 | RGB值 |
|----------|---------|---------|------|-------|
| 霓虹绿 | --color-primary | #00D084 | 主色、CTA 按钮、成功 | rgb(0, 208, 132) |
| 霓虹绿 Hover | --color-primary-dark | #00B36A | 按钮悬停 | rgb(0, 179, 106) |
| 霓虹绿 Active | --color-primary-darker | #00984D | 按钮按下 | rgb(0, 152, 77) |
| 深灰黑 | --color-surface-dark | #0F172A | 页面背景 | rgb(15, 23, 42) |
| 深灰 | --color-surface | #1E293B | 卡片、表面、对话框 | rgb(30, 41, 59) |
| 表面浅色 | --color-surface-light | #334155 | 边框、分割线、禁用背景 | rgb(51, 65, 85) |
| 中灰 | --color-text-secondary | #475569 | 次级文本、禁用文本 | rgb(71, 85, 105) |
| 浅灰 | --color-divider | #E2E8F0 | 轻盈分割线 | rgb(226, 232, 240) |
| 浅白 | --color-text-primary | #F1F5F9 | 主文本、亮色文字 | rgb(241, 245, 249) |
| 金色 | --color-accent | #FFD700 | 强调、奢侈品标记 | rgb(255, 215, 0) |
| 红色 | --color-error | #EF4444 | 错误、警告、库存预警 | rgb(239, 68, 68) |
| 琥珀色 | --color-warning | #fbbf24 | 警告状态 | rgb(251, 191, 36) |
| 绿色 | --color-success | #34C759 | 成功状态 | rgb(52, 199, 89) |
| 蓝色 | --color-info | #3B82F6 | 信息、链接、次级操作 | rgb(59, 130, 246) |

## 间距系统

基础单位：**8px**

| 变量 | 像素 | 使用场景 | 示例 |
|------|------|---------|------|
| --spacing-xs | 4px | 微小间距 | 图标之间、行内间距 |
| --spacing-sm | 8px | 小间距 | 元素内部间距 |
| --spacing-md | 16px | 中间距 | 组件之间间距 |
| --spacing-lg | 24px | 大间距 | 模块之间间距 |
| --spacing-xl | 32px | 超大间距 | 部分间距 |
| --spacing-2xl | 48px | 版心内边距 | 容器内边距 |

## 内边距规范

| 组件 | 内边距 | 高度 | 说明 |
|------|--------|------|------|
| 按钮 | 12px 16px | 44px | 最小可交互区 |
| 输入框 | 12px 16px | 44px | 同按钮 |
| 卡片 | 16px | — | 内容包裹 |
| 对话框 | 24px | — | 大空间内容 |
| 列表项 | 12px 16px | — | 紧凑排列 |
| 标签/徽章 | 4px 12px | 24px | 小元素 |

## 圆角规范

| 变量 | 像素 | 用途 | 应用组件 |
|------|------|------|----------|
| --border-radius-none | 0 | 直角 | 特殊场景 |
| --border-radius-sm | 4px | 轻微圆角 | 小卡片 |
| --border-radius-md | 6px | 中等圆角 | 输入框 |
| --border-radius-lg | 8px | 标准圆角 | 按钮、卡片 |
| --border-radius-xl | 12px | 大圆角 | 模态框、卡片 |
| --border-radius-full | 50% | 完全圆形 | 头像、小徽章 |

## 阴影规范

| 等级 | CSS变量 | 数值 | 用途 | 应用场景 |
|------|---------|------|------|----------|
| Elevation 1 | --shadow-elevation-1 | 0 4px 12px rgba(0,0,0,0.3) | 轻微提升 | 卡片、弹出菜单 |
| Elevation 2 | --shadow-elevation-2 | 0 8px 24px rgba(0,0,0,0.35) | 中等提升 | 浮动面板、悬浮按钮 |
| Elevation 3 | --shadow-elevation-3 | 0 12px 32px rgba(0,0,0,0.4) | 强提升 | 模态框、购物车浮窗 |

## 排版规范

### 字号阶梯

| 用途 | 字号 | 行高 | 字间距 | 字重 | CSS 变量 |
|------|------|------|--------|------|----------|
| 大标题 | 32px | 1.2 | -0.5px | 700 | --font-size-2xl |
| 标题 | 24px | 1.3 | -0.3px | 600 | --font-size-xl |
| 小标题 | 18px | 1.4 | 0 | 600 | --font-size-lg |
| 正文 | 14px | 1.6 | 0.2px | 400 | --font-size-base |
| 小文本 | 12px | 1.5 | 0 | 500 | --font-size-sm |
| 超小文本 | 11px | 1.4 | 0 | 400 | --font-size-xs |

### 字重规范

| 用途 | 字重 | CSS变量 | 示例 |
|------|------|---------|------|
| 正文 | 400 | --font-weight-normal | 商品描述、评论内容 |
| 重点 | 500 | --font-weight-medium | 次级标题、标签 |
| 标题 | 600-700 | --font-weight-semibold/bold | 页面标题、卡片标题 |
| 浅色 | 300 | --font-weight-light | 副标题、辅助信息 |

### 字体族

```css
font-family: 'Inter', 'Helvetica Neue', -apple-system, BlinkMacSystemFont, sans-serif;
```

## 动画规范

### 动画时间

| 场景 | CSS变量 | 时间 | 用途 | 示例 |
|------|---------|------|------|------|
| 快速反馈 | --transition-fast | 200ms | 加入购物车、点赞、按钮 | Button Hover |
| 标准动画 | --transition-base | 300ms | 页面转换、对话框 | Modal 弹出 |
| 缓慢动画 | --transition-slow | 500ms | 加载、长操作、骨架屏 | Loading 动画 |

### 缓动函数

| 函数 | CSS变量 | Bezier | 用途 |
|------|---------|---------|------|
| easeInOut | --ease-in-out | cubic-bezier(0.4, 0, 0.2, 1) | 标准过渡 |
| easeOut | --ease-out | cubic-bezier(0, 0, 0.2, 1) | 出现动画 |
| easeIn | --ease-in | cubic-bezier(0.4, 0, 1, 1) | 消失动画 |

### 微交互示例

**添加购物车动画**：
1. 按钮变绿色 (100ms)
2. 文字变为 "已加入" (100ms)
3. 购物车数字 +1，上升动画 (300ms)
4. Toast 提示出现 (200ms)

**购物车浮球**：
- 固定位置：右下角 (bottom: 20px, right: 20px)
- 大小：48px 圆形
- Hover 放大 1.1x (200ms)
- 点击 → 抽屉从右滑入 (300ms)

## 按钮规范

### 尺寸

- 高度：44px（最小可交互区域）
- 圆角：8px（--border-radius-lg）
- 内边距：12px 16px
- 字体：600 weight, 14px (--font-size-base)

### 按钮状态

| 状态 | 背景色 | 文字色 | 投影 | 变换 |
|------|--------|--------|------|------|
| 默认 | var(--color-primary) | #0F172A | 无 | 无 |
| Hover | var(--color-primary-dark) | #0F172A | 0 8px 16px rgba(0,208,132,0.3) | translateY(-1px) |
| Active | var(--color-primary-darker) | #0F172A | 0 4px 8px rgba(0,208,132,0.2) | translateY(0) |
| Disabled | var(--color-neutral-disabled) | #94a3b8 | 无 | 无，opacity 0.6 |

### 按钮类型

- **主按钮（CTA）**：绿色填充 (#00D084)
- **次按钮**：金色填充 (#FFD700)
- **文字按钮**：绿色文字，无填充
- **危险按钮**：红色填充 (#EF4444)

## 卡片规范

- 圆角：12px (--border-radius-xl)
- 边框：1px #334155
- 背景：#1E293B (--color-surface)
- 投影：0 4px 12px rgba(0, 0, 0, 0.3)
- 内边距：16px (--spacing-md)
- Hover：边框变为 #00D084，投影增强

## 对话框规范

- 圆角：12px (--border-radius-xl)
- 背景：#1E293B (--color-surface)
- 边框：1px #334155
- 遮罩：rgba(15, 23, 42, 0.7)（深半透明）
- 内边距：24px (--spacing-lg)
- 标题权重：600 (--font-weight-semibold)
- 动画：300ms 缓入缓出

## 输入框规范

- 圆角：6px (--border-radius-md)
- 高度：44px
- 边框：1px #334155
- 背景：#0F172A (--color-surface-dark)
- 文字色：#F1F5F9 (--color-text-primary)
- 焦点边框：2px #00D084 + 阴影
- 内边距：12px 16px (12px 16px)

## 表格（DataGrid）规范

- 行高：48px
- 边框：1px #334155
- 表头背景：#1E293B (--color-surface)
- 表头文字权重：600
- Hover 行背景：rgba(0, 208, 132, 0.05)（轻绿）
- 排序图标：#00D084 (--color-primary)

## 响应式布局断点

| 设备 | 宽度 | 栅格 | 内边距 | 说明 |
|------|------|------|--------|------|
| 移动 | 320px-640px | 4 列 | 12px | 手机垂直 |
| 平板 | 641px-1024px | 8 列 | 16px | 平板、手机横 |
| 桌面 | 1025px-1440px | 12 列 | 24px | 标准桌面 |
| 超宽屏 | 1441px+ | 12 列 | 32px | 大屏显示器 |

## 组件升级检查清单

### 样式一致性

- [ ] 所有颜色都使用 CSS 变量
- [ ] 间距只使用 8px 的倍数
- [ ] 圆角只使用定义的值（0, 4px, 6px, 8px, 12px, 50%）
- [ ] 阴影使用规范值（elevation-1/2/3）
- [ ] 按钮高度 44px
- [ ] 输入框高度 44px
- [ ] 内边距遵循规范
- [ ] 动画时间使用定义值（200/300/500ms）

### 交互一致性

- [ ] 所有按钮都有 Hover、Active、Disabled 状态
- [ ] 焦点态使用绿色边框
- [ ] 禁用态处理正确（opacity 0.6）
- [ ] Hover 动画使用 200ms
- [ ] 模态框动画使用 300ms

### 对比度检查

- [ ] 对比度 ≥ 4.5:1（WCAG AA）
- [ ] 主文本 vs 背景：#F1F5F9 vs #0F172A ✓
- [ ] 次文本 vs 背景：#475569 vs #0F172A ✓
- [ ] 边框 vs 背景：#334155 vs #1E293B ✓

### 无障碍检查

- [ ] 最小触摸区 44×44px
- [ ] 键盘导航支持
- [ ] 焦点指示清晰可见
- [ ] 色盲友好（不仅依赖颜色）

## 参考资源

- 设计系统文件：`DESIGN.md`
- CSS 变量文件：`src/styles/design-system.css`
- MUI 主题配置：`src/theme/index.ts`
- 组件升级模板：`src/components/_templates/ComponentUpgrade.md`
