import { useEffect, useState } from 'react'
import { Box, Typography, Paper } from '@mui/material'

export default function OpenApiDeveloperPage() {
  const [overview, setOverview] = useState<Record<string, unknown> | null>(null)

  useEffect(() => {
    fetch('/api/openapi/overview?tenantId=demo-tenant')
      .then((r) => r.json())
      .then((body) => setOverview(body?.data ?? null))
      .catch(() => {})
  }, [])

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h5" gutterBottom>OpenAPI 开发者</Typography>
      <Paper sx={{ p: 2 }}>
        <pre>{JSON.stringify(overview, null, 2)}</pre>
      </Paper>
    </Box>
  )
}
