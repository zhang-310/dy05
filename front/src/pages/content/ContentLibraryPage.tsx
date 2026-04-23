import { useState } from 'react'
import { Box, Tab, Tabs } from '@mui/material'
import { PageHeader } from '@/components/base'
import ScriptListPage from '@/pages/script/ScriptListPage'
import ScriptTemplatePage from '@/pages/script/ScriptTemplatePage'
import CopyLibraryPage from '@/pages/copy/CopyLibraryPage'
import ViolationWordPage from '@/pages/script/ViolationWordPage'
import ViolationCheckPage from '@/pages/script/ViolationCheckPage'
import SlangDictPage from '@/pages/slangdict/SlangDictPage'

export default function ContentLibraryPage() {
  const [tab, setTab] = useState(0)

  return (
    <Box sx={{ p: 3 }}>
      <PageHeader title="内容库" subtitle="话术、文案、违规词与梗库" />
      <Tabs value={tab} onChange={(_, v) => setTab(v)} sx={{ mb: 2 }} variant="scrollable" scrollButtons="auto">
        <Tab label="话术库" />
        <Tab label="话术模板" />
        <Tab label="文案库" />
        <Tab label="违规词库" />
        <Tab label="合规检测" />
        <Tab label="梗库" />
      </Tabs>
      {tab === 0 && <ScriptListPage />}
      {tab === 1 && <ScriptTemplatePage />}
      {tab === 2 && <CopyLibraryPage />}
      {tab === 3 && <ViolationWordPage />}
      {tab === 4 && <ViolationCheckPage />}
      {tab === 5 && <SlangDictPage />}
    </Box>
  )
}
