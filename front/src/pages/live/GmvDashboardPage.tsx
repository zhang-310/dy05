import { Box, Card, CardContent, Grid2, Typography } from '@mui/material'
import GmvCounter from '../../components/GmvCounter'

export default function GmvDashboardPage() {
  return (
    <Box sx={{ p: 2 }}>
      <Typography variant="h5" sx={{ mb: 3 }}>GMV 看板</Typography>
      <Grid2 container spacing={2}>
        <Grid2 size={{ xs: 12, md: 4 }}>
          <Card><CardContent>
            <Typography color="text.secondary">今日 GMV</Typography>
            <GmvCounter value={0} fontSize={36} />
          </CardContent></Card>
        </Grid2>
        <Grid2 size={{ xs: 12, md: 4 }}>
          <Card><CardContent>
            <Typography color="text.secondary">昨日 GMV</Typography>
            <GmvCounter value={0} fontSize={36} />
          </CardContent></Card>
        </Grid2>
        <Grid2 size={{ xs: 12, md: 4 }}>
          <Card><CardContent>
            <Typography color="text.secondary">本月累计</Typography>
            <GmvCounter value={0} fontSize={36} />
          </CardContent></Card>
        </Grid2>
      </Grid2>
    </Box>
  )
}
