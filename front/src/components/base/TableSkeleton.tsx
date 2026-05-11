import { Box, Skeleton, Stack } from '@mui/material'

interface TableSkeletonProps {
  rows?: number
  columns?: number
}

export function TableSkeleton({ rows = 10, columns = 5 }: TableSkeletonProps) {
  return (
    <Box sx={{ width: '100%' }}>
      {/* Header */}
      <Stack direction="row" gap={2} sx={{ mb: 2, px: 2 }}>
        {Array.from({ length: columns }).map((_, i) => (
          <Skeleton key={`header-${i}`} variant="text" width={`${100 / columns}%`} height={40} />
        ))}
      </Stack>

      {/* Rows */}
      {Array.from({ length: rows }).map((_, rowIndex) => (
        <Stack key={`row-${rowIndex}`} direction="row" gap={2} sx={{ mb: 1, px: 2 }}>
          {Array.from({ length: columns }).map((_, colIndex) => (
            <Skeleton
              key={`cell-${rowIndex}-${colIndex}`}
              variant="text"
              width={`${100 / columns}%`}
              height={32}
            />
          ))}
        </Stack>
      ))}
    </Box>
  )
}
