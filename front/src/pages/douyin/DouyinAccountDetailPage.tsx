import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  Box, Typography, Card, CardContent, Grid, Chip,
  Button, CircularProgress, Alert, Table, TableBody,
  TableCell, TableHead, TableRow,
} from '@mui/material'
import ArrowBackIcon from '@mui/icons-material/ArrowBack'
import { PageHeader } from '@/components/base'
import { douyinApi, type DyAccount, type DyAccountStats } from '@/api/douyin'

function formatNum(n: unknown): string {
  if (n == null || n === '') return '-'
  const num = Number(n)
  if (Number.isNaN(num)) return '-'
  if (num >= 10000) return (num / 10000).toFixed(1) + '万'
  return String(num)
}

export default function DouyinAccountDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [account, setAccount] = useState<DyAccount | null>(null)
  const [stats, setStats] = useState<DyAccountStats | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    const aid = id ? parseInt(id, 10) : NaN
    if (!id || Number.isNaN(aid)) {
      setError('无效的账号 ID')
      setLoading(false)
      return
    }
    setLoading(true)
    setError('')
    Promise.all([douyinApi.accountGet(aid), douyinApi.accountStats(aid)])
      .then(([acc, st]) => {
        setAccount(acc)
        setStats(st)
      })
      .catch((e: Error) => setError(e.message || '加载失败'))
      .finally(() => setLoading(false))
  }, [id])

  if (loading) return (
    <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
      <CircularProgress />
    </Box>
  )

  if (error) return (
    <Box sx={{ p: 3 }}>
      <Alert severity="error">{error}</Alert>
      <Button sx={{ mt: 2 }} startIcon={<ArrowBackIcon />} onClick={() => navigate(-1)}>返回</Button>
    </Box>
  )

  return (
    <Box sx={{ p: 3 }}>
      <PageHeader
        title={account?.accountName ?? '账号详情'}
        subtitle={account?.accountId ?? ''}
        actions={
          <Button startIcon={<ArrowBackIcon />} onClick={() => navigate(-1)}>返回列表</Button>
        }
      />

      <Grid container spacing={3} sx={{ mt: 1 }}>
        {account && (
          <Grid item xs={12} md={6}>
            <Card variant="outlined">
              <CardContent>
                <Typography variant="h6" gutterBottom>基本信息</Typography>
                {[
                  { label: '账号名称', value: account.accountName },
                  { label: '抖音号', value: account.accountId },
                  { label: '粉丝数', value: formatNum(account.fanCount) },
                  { label: '状态', value: (
                    <Chip
                      label={account.status === 1 ? '正常' : '停用'}
                      size="small"
                      color={account.status === 1 ? 'success' : 'error'}
                    />
                  )},
                  { label: '创建时间', value: account.createTime ?? '-' },
                ].map((row) => (
                  <Box key={row.label} sx={{ display: 'flex', py: 0.75, borderBottom: '1px solid', borderColor: 'divider' }}>
                    <Typography variant="body2" color="text.secondary" sx={{ width: 100, flexShrink: 0 }}>{row.label}</Typography>
                    <Typography variant="body2">{row.value}</Typography>
                  </Box>
                ))}
              </CardContent>
            </Card>
          </Grid>
        )}

        {stats && (
          <Grid item xs={12}>
            <Card variant="outlined">
              <CardContent>
                <Typography variant="h6" gutterBottom>数据统计</Typography>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>视频总数</TableCell>
                      <TableCell>总播放</TableCell>
                      <TableCell>总点赞</TableCell>
                      <TableCell>总分享</TableCell>
                      <TableCell>总评论</TableCell>
                      <TableCell>平均播放/条</TableCell>
                      <TableCell>平均点赞/条</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    <TableRow>
                      <TableCell>{formatNum(stats.totalVideos)}</TableCell>
                      <TableCell>{formatNum(stats.totalViews)}</TableCell>
                      <TableCell>{formatNum(stats.totalLikes)}</TableCell>
                      <TableCell>{formatNum(stats.totalShares)}</TableCell>
                      <TableCell>{formatNum(stats.totalComments)}</TableCell>
                      <TableCell>{stats.avgViewsPerVideo?.toFixed(0) ?? '-'}</TableCell>
                      <TableCell>{stats.avgLikesPerVideo?.toFixed(0) ?? '-'}</TableCell>
                    </TableRow>
                  </TableBody>
                </Table>
              </CardContent>
            </Card>
          </Grid>
        )}
      </Grid>
    </Box>
  )
}
