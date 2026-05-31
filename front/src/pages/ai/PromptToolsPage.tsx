import { useState, lazy, Suspense } from 'react'
import { Alert, AlertTitle, Box, Tabs, Tab } from '@mui/material'
import { PageHeader, PageSkeleton } from '@/components/base'

const PromptLabPage = lazy(() => import('./PromptLabPage'))
const PromptTemplatePage = lazy(() => import('./PromptTemplatePage'))

export default function PromptToolsPage() {
  const [tab, setTab] = useState(0)

  return (
    <Box
      data-testid="prompt-tools-page"
      data-ready-endpoints="lazy:PromptLabPage,lazy:PromptTemplatePage"
      data-child-ready-endpoints="/ai/prompt-template/list,/ai/prompt-template/save,/ai/prompt-template/delete,/ai/prompt-template/extract-variables,/ai/prompt-template/test-render,/ai/prompt-template/record-usage"
      data-unsupported-endpoints="/ai/prompt-tools/mock,/ai/prompt-tools/static-template,/ai/prompt-tools/local-render"
      data-wrapper-only="true"
      data-no-page-api-request="true"
    >
      <PageHeader
        title="Prompt 工具箱"
        subtitle="复用 Prompt 实验室和 Prompt 模板真实契约，不在外层生成占位模板或伪造渲染结果。"
        breadcrumbs={[{ label: 'AI 中心' }, { label: 'Prompt 工具箱' }]}
      />
      <Alert
        severity="info"
        data-testid="prompt-tools-boundary-contract"
        data-wrapper-only="true"
        data-no-page-api-request="true"
        sx={{ mb: 2 }}
      >
        <AlertTitle>工具箱边界</AlertTitle>
        实验室和模板页继续调用 <code>/ai/prompt-template/list|save|delete|get-active|extract-variables|test-render|record-usage</code>；
        外层只负责 Tab 组织和懒加载，错误、预览和启用状态均由子页面按真实接口展示。
      </Alert>
      <Tabs value={tab} onChange={(_, v) => setTab(v)} sx={{ mb: 2, borderBottom: 1, borderColor: 'divider' }}>
        <Tab label="Prompt 实验室" />
        <Tab label="Prompt 模板" />
      </Tabs>
      <Suspense fallback={<PageSkeleton />}>
        {tab === 0 && <PromptLabPage />}
        {tab === 1 && <PromptTemplatePage />}
      </Suspense>
    </Box>
  )
}
