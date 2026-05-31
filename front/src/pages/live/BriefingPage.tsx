import { Box, Card, CardContent, Chip, Stack, Typography } from '@mui/material'

export default function BriefingPage() {
  return (
    <Box sx={{ p: 2 }}>
      <Typography variant="h5" sx={{ mb: 2 }}>📊 每日运营晨报</Typography>
      <Stack spacing={2}>
        <Card><CardContent>
          <Typography variant="h6">今日场次: <Chip label="0" color="primary" size="small" /></Typography>
          <Typography color="text.secondary">昨日 GMV: -- 万</Typography>
        </CardContent></Card>
        <Card><CardContent>
          <Typography variant="h6">待审核: <Chip label="0" color="warning" size="small" /></Typography>
          <Typography color="text.secondary">话术审核 + 商品审核</Typography>
        </CardContent></Card>
        <Card><CardContent>
          <Typography variant="h6">企微推送: <Chip label="已配置" color="success" size="small" /></Typography>
          <Typography color="text.secondary">每日 07:30 自动推送</Typography>
        </CardContent></Card>
        <Card><CardContent>
          <Typography variant="h6">AI 用量: <Chip label="正常" color="success" size="small" /></Typography>
          <Typography color="text.secondary">DeepSeek Provider: 已连接</Typography>
        </CardContent></Card>
      </Stack>
    </Box>
  )
}
