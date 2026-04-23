import { useState } from 'react'
import { Box, Tab, Tabs } from '@mui/material'
import { PageHeader } from '@/components/base'
import CopyLibraryPage from './CopyLibraryPage'
import CopyApprovalPage from './CopyApprovalPage'
import CopyTemplatePage from './CopyTemplatePage'

export default function CopyPage() {
  const [tab, setTab] = useState(0)

  return (
    <Box sx={{ p: 3 }}>
      <PageHeader title="文案管理" subtitle="文案库、审批与模板" />
      <Tabs value={tab} onChange={(_, v) => setTab(v)} sx={{ mb: 2 }}>
        <Tab label="文案库" />
        <Tab label="文案审批" />
        <Tab label="文案模板" />
      </Tabs>
      {tab === 0 && <CopyLibraryPage />}
      {tab === 1 && <CopyApprovalPage />}
      {tab === 2 && <CopyTemplatePage />}
    </Box>
  )
}
