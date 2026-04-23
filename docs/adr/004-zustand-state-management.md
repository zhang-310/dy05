# ADR-004: 使用 Zustand 而非 Redux 进行状态管理

## 状态
已采纳

## 背景
React 状态管理方案:
1. Redux Toolkit -- 功能强大但样板代码多
2. MobX -- 响应式，但学习曲线陡
3. Zustand -- 轻量、简洁、TypeScript 友好
4. Jotai/Recoil -- 原子化状态

## 决策
使用 Zustand 5 进行全局状态管理。

## 后果
- 正面: 零样板代码、TypeScript 原生支持、体积小（~1KB）
- 负面: 生态不如 Redux 丰富
