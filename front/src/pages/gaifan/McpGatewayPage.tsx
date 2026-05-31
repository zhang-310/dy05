import { useEffect, useState } from 'react'
import { Box, Button, MenuItem, Paper, Stack, TextField, Typography, Alert, Table, TableBody, TableCell, TableHead, TableRow } from '@mui/material'
import { checkGaifanEntitlement } from '@/api/gaifan-catalog'
import { useToast } from '@/contexts/ToastContext'
import { commercialDenialMessage, isCommercialDenial } from '@/utils/commercialError'

type ToolRow = { toolCode: string; name: string; requiredProductCode: string; requiredFeatureCode: string }

export default function McpGatewayPage() {
  const toast = useToast()
  const [tools, setTools] = useState<ToolRow[]>([])
  const [toolCode, setToolCode] = useState('video.analyze')
  const [inputJson, setInputJson] = useState('{"viralVideoId":"1","userId":"1"}')
  const [result, setResult] = useState<unknown>(null)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    fetch('/api/mcp/tools')
      .then((r) => r.json())
      .then((body) => {
        const list = (body?.data ?? []) as ToolRow[]
        setTools(list)
        if (list.length > 0 && !list.some((t) => t.toolCode === toolCode)) {
          setToolCode(list[0].toolCode)
        }
      })
      .catch(() => {})
  }, [])

  const invoke = async () => {
    setLoading(true)
    const tool = tools.find((t) => t.toolCode === toolCode)
    const productCode = tool?.requiredProductCode ?? 'video-insight'
    const featureCode = tool?.requiredFeatureCode ?? 'ai.mcp-tool'
    try {
      const ent = await checkGaifanEntitlement(productCode, featureCode)
      if (ent && ent.granted === false) {
        toast(commercialDenialMessage({ code: 4421, message: ent.reason ?? '无 MCP 工具权益' }), 'warning')
        setLoading(false)
        return
      }
    } catch (e) {
      if (isCommercialDenial(e)) {
        toast(commercialDenialMessage(e), 'warning')
        setLoading(false)
        return
      }
    }
    let args: Record<string, string> = {}
    try {
      const parsed = JSON.parse(inputJson) as Record<string, string>
      args = Object.fromEntries(Object.entries(parsed).map(([k, v]) => [k, String(v)]))
    } catch {
      setResult({ error: 'arguments 必须是合法 JSON' })
      setLoading(false)
      return
    }
    fetch('/api/mcp/invoke', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        tenantId: 'demo-tenant',
        userId: 'demo-user',
        toolCode,
        channel: 'MCP',
        arguments: args,
        traceId: `ui-${Date.now()}`,
      }),
    })
      .then((r) => r.json())
      .then((body) => setResult(body?.data ?? body))
      .catch((err) => setResult({ error: String(err) }))
      .finally(() => setLoading(false))
  }

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h5" gutterBottom>MCP 网关</Typography>
      <Alert severity="info" sx={{ mb: 2 }}>
        enforce 开启时，无权益返回 HTTP 402（code 4421），积分不足返回 402（code 4420）。请先在{' '}
        <a href="/admin/gaifan/credits">积分治理</a> 确认余额与套餐。
      </Alert>
      <Paper sx={{ p: 2, mb: 2 }}>
        <Stack spacing={2} direction={{ xs: 'column', md: 'row' }}>
          <TextField select label="工具" value={toolCode} onChange={(e) => setToolCode(e.target.value)} sx={{ minWidth: 220 }}>
            {tools.map((t) => (
              <MenuItem key={t.toolCode} value={t.toolCode}>{t.toolCode}</MenuItem>
            ))}
          </TextField>
          <TextField label="arguments JSON" fullWidth multiline minRows={2} value={inputJson} onChange={(e) => setInputJson(e.target.value)} />
          <Button variant="contained" onClick={invoke} disabled={loading}>{loading ? '调用中…' : '沙箱 invoke'}</Button>
        </Stack>
      </Paper>
      {tools.length > 0 && (
        <Paper sx={{ p: 2, mb: 2 }}>
          <Typography variant="subtitle2" gutterBottom>已注册工具（productCode / featureCode）</Typography>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>toolCode</TableCell>
                <TableCell>product</TableCell>
                <TableCell>feature</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {tools.map((t) => (
                <TableRow key={t.toolCode}>
                  <TableCell>{t.toolCode}</TableCell>
                  <TableCell>{t.requiredProductCode}</TableCell>
                  <TableCell>{t.requiredFeatureCode}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </Paper>
      )}
      <Paper sx={{ p: 2 }}>
        <Typography variant="subtitle2" gutterBottom>调用结果</Typography>
        <pre style={{ margin: 0, overflow: 'auto' }}>{JSON.stringify(result, null, 2)}</pre>
      </Paper>
    </Box>
  )
}
