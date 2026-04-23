# ADR-004：Zustand 状态管理

## 状态

已采纳

## 上下文

前端需要全局状态管理（用户登录态、UI 状态、业务编辑状态等）。React 生态有 Redux、MobX、Zustand、Jotai 等方案。

## 决策

使用 Zustand 5 作为全局状态管理方案。

## 理由

- 极简 API，几乎零样板代码
- 不需要 Provider 包装组件树
- TypeScript 支持优秀
- 体积小（<1KB），性能好
- 支持中间件（persist、devtools）

## 实现模式

```typescript
export const useUserStore = create<UserState>((set) => ({
  user: null,
  token: null,
  setUser: (user) => set({ user }),
  logout: () => {
    set({ user: null, token: null })
    localStorage.removeItem('token')
  },
}))
```

## Store 划分

| Store | 职责 |
|-------|------|
| useUserStore | 用户信息、Token |
| useUIStore | 侧边栏、主题 |
| useLiveEditStore | 直播话术编辑 |
| useLiveGenStore | 话术生成流程 |
| useLiveProductStore | 直播商品 |
| useIndustryBrainStore | 行业大脑 |

## 后果

- 正面：开发效率高，代码简洁
- 正面：按业务域拆分 Store，职责清晰
- 负面：缺少 Redux DevTools 的时间旅行调试（Zustand devtools 中间件可部分弥补）
