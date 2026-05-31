import { Box, Card, CardContent, Chip, Grid, Stack, Typography } from '@mui/material'

export default function OfficialWebsitePage() {
  return (
    <Box sx={{ p: 3, maxWidth: 1200, mx: 'auto' }}>
      <Typography variant="h3" sx={{ mb: 1, textAlign: 'center' }}>
        抖音运营 SaaS 平台
      </Typography>
      <Typography variant="h6" sx={{ mb: 4, textAlign: 'center', color: 'text.secondary' }}>
        护肤彩妆直播 · 千万级 GMV · 一站式解决方案
      </Typography>

      <Grid container spacing={2}>
        <Grid item xs={12} sm={6} md={4}>
          <Card><CardContent>
            <Typography variant="h6">直播运营</Typography>
            <Typography variant="body2" color="text.secondary">场次管理 / 话术生成 / 实时中控 / GMV 看板</Typography>
          </CardContent></Card>
        </Grid>
        <Grid item xs={12} sm={6} md={4}>
          <Card><CardContent>
            <Typography variant="h6">短视频运营</Typography>
            <Typography variant="body2" color="text.secondary">脚本策划 / 素材管理 / AI 生成 / 爆款分析</Typography>
          </CardContent></Card>
        </Grid>
        <Grid item xs={12} sm={6} md={4}>
          <Card><CardContent>
            <Typography variant="h6">账号管理</Typography>
            <Typography variant="body2" color="text.secondary">多账号接入 / 数据同步 / 粉丝画像</Typography>
          </CardContent></Card>
        </Grid>
        <Grid item xs={12} sm={6} md={4}>
          <Card><CardContent>
            <Typography variant="h6">AI 智能</Typography>
            <Typography variant="body2" color="text.secondary">DeepSeek 驱动 / 话术生成 / 视频分析 / RAG 知识库</Typography>
          </CardContent></Card>
        </Grid>
        <Grid item xs={12} sm={6} md={4}>
          <Card><CardContent>
            <Typography variant="h6">商品管理</Typography>
            <Typography variant="body2" color="text.secondary">选品 / 话术关联 / 效果追踪 / 竞品分析</Typography>
          </CardContent></Card>
        </Grid>
        <Grid item xs={12} sm={6} md={4}>
          <Card><CardContent>
            <Typography variant="h6">数据看板</Typography>
            <Typography variant="body2" color="text.secondary">实时数据 / 趋势分析 / 经营复盘</Typography>
          </CardContent></Card>
        </Grid>
      </Grid>

      <Card sx={{ mt: 4 }}><CardContent>
        <Typography variant="h6">平台能力</Typography>
        <Stack direction="row" spacing={1} sx={{ mt: 1, flexWrap: 'wrap' }}>
          <Chip label="DeepSeek AI 已接入" color="success" />
          <Chip label="企微晨报推送" color="success" />
          <Chip label="微服务架构" />
          <Chip label="多租户隔离" />
        </Stack>
      </CardContent></Card>
    </Box>
  )
}
