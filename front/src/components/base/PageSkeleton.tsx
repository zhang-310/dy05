
import { Box, Skeleton, Stack } from '@mui/material'

export function PageSkeleton() {
  return (
    <Box sx={{ p: 2 }}>
      <Skeleton variant="text" width={200} height={32} sx={{ mb: 2 }} />
      <Stack direction="row" gap={2} sx={{ mb: 2 }}>
        {[1, 2, 3, 4].map((i) => (
          <Skeleton key={i} variant="rounded" width={120} height={36} />
        ))}
      </Stack>
      <Skeleton variant="rounded" width="100%" height={400} />
    </Box>
  )
}
