# 调试步骤

## 1. 打开浏览器开发者工具

按 **F12** 打开开发者工具

## 2. 切换到 Network（网络）标签

## 3. 刷新页面

按 **F5** 刷新页面

## 4. 找到 API 请求

在 Network 列表中找到 `/api/v1/short-video/account-collect/list` 请求

## 5. 查看响应数据

点击该请求，查看 Response（响应）标签，检查返回的数据中是否包含 `svAccountId` 字段

## 6. 截图或复制响应数据

请将响应数据复制给我，或者截图发给我

---

或者，我可以直接用 curl 测试 API：
