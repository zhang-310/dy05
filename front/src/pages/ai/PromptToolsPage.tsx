import { useState, lazy, Suspense } from 'react'
import { Box, Tabs, Tab } from '@mui/material'
import { PageHeader, PageSkeleton } from '@/components/base'

const PromptLabPage = lazy(() => import('./PromptLabPage'))
const PromptTemplatePage = lazy(() => import('./PromptTemplatePage'))

export default function PromptToolsPage() {
  const [tab, setTab] = useState(0)

  return (
    <Box>
      <PageHeader
        title="Prompt 工具箱"
        breadcrumbs={[{ label: 'AI 中心' }, { label: 'Prompt 工具箱' }]}
      />
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
