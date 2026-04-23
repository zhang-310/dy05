# 前端 `src/pages` 大文件扫描（自动生成参考）

运行：

```bash
cd frontend-react && node scripts/count-large-pages.mjs 800
```

## 最近一次输出（阈值 ≥800 行）

| 行数 | 文件 |
|------|------|
| 1065 | `frontend-react/src/pages/live/LiveScriptBuilderPage.tsx` |
| 846 | `frontend-react/src/pages/crud/ProductPage.tsx` |

- 扫描范围：`frontend-react/src/pages/**/*.tsx`
- 本次扫描合计：**220** 个 `.tsx` 文件

> 随代码变化重新运行脚本更新本表；第四轮文档中「1754 行 ViralLibrary」等旧数据已不适用。
