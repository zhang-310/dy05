import { useMutation } from '@tanstack/react-query'
import { Alert, Box, Button, Card, CardContent, CircularProgress, Typography } from '@mui/material'
import { fetchDouyinOpsBrief } from '@/api/douyin-ops-commander'
import { useGaifanEntitlementGate } from '@/hooks/useGaifanEntitlementGate'
import { CREDITS_GOVERNANCE_PATH } from '@/utils/commercialError'
import { useNavigate } from 'react-router-dom'

export default function DouyinOpsCommanderPage() {
  const navigate = useNavigate()
  const gate = useGaifanEntitlementGate()
  const mutation = useMutation({
    mutationFn: fetchDouyinOpsBrief,
  })

  const err = mutation.error as (Error & { code?: number }) | null

  const handleGenerate = async () => {
    if (!(await gate('douyin-ops', 'douyin-ops.content-planning'))) return
    mutation.mutate()
  }

  return (
    <Box sx={{ p: 3, maxWidth: 960 }}>
      <Typography variant="h5" gutterBottom>
        抖音运营指挥官
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
        产品码 douyin-ops · 调用将消耗内容策划权益与积分（enforce 开启时）。
      </Typography>
      <Button variant="contained" onClick={handleGenerate} disabled={mutation.isPending}>
        {mutation.isPending ? '生成中…' : '生成运营简报'}
      </Button>
      {err && (
        <Alert
          severity="warning"
          sx={{ mt: 2 }}
          action={
            (err.code === 4420 || err.code === 4421) ? (
              <Button color="inherit" size="small" onClick={() => navigate(CREDITS_GOVERNANCE_PATH)}>
                积分治理
              </Button>
            ) : undefined
          }
        >
          {err.message}
        </Alert>
      )}
      {mutation.data && (
        <Card sx={{ mt: 2 }}>
          <CardContent>
            <Typography variant="subtitle1">{mutation.data.summary ?? '简报已生成'}</Typography>
            <pre style={{ whiteSpace: 'pre-wrap', fontSize: 12 }}>
              {JSON.stringify(mutation.data, null, 2)}
            </pre>
          </CardContent>
        </Card>
      )}
      {mutation.isPending && <CircularProgress sx={{ mt: 2 }} />}
    </Box>
  )
}
