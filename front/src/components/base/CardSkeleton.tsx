import { Box, Card, CardContent, Skeleton, Stack } from '@mui/material'

interface CardSkeletonProps {
  count?: number
  variant?: 'default' | 'compact' | 'detailed'
}

export function CardSkeleton({ count = 3, variant = 'default' }: CardSkeletonProps) {
  const renderCard = () => {
    switch (variant) {
      case 'compact':
        return (
          <Card>
            <CardContent>
              <Skeleton variant="text" width="60%" height={24} sx={{ mb: 1 }} />
              <Skeleton variant="text" width="40%" height={20} />
            </CardContent>
          </Card>
        )

      case 'detailed':
        return (
          <Card>
            <CardContent>
              <Stack direction="row" justifyContent="space-between" sx={{ mb: 2 }}>
                <Skeleton variant="text" width="50%" height={28} />
                <Skeleton variant="circular" width={40} height={40} />
              </Stack>
              <Skeleton variant="text" width="100%" height={20} sx={{ mb: 1 }} />
              <Skeleton variant="text" width="80%" height={20} sx={{ mb: 2 }} />
              <Stack direction="row" gap={1}>
                <Skeleton variant="rounded" width={80} height={32} />
                <Skeleton variant="rounded" width={80} height={32} />
              </Stack>
            </CardContent>
          </Card>
        )

      default:
        return (
          <Card>
            <CardContent>
              <Skeleton variant="text" width="70%" height={24} sx={{ mb: 1 }} />
              <Skeleton variant="text" width="100%" height={20} sx={{ mb: 1 }} />
              <Skeleton variant="text" width="90%" height={20} />
            </CardContent>
          </Card>
        )
    }
  }

  return (
    <Box sx={{ display: 'grid', gap: 2, gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))' }}>
      {Array.from({ length: count }).map((_, i) => (
        <Box key={`card-skeleton-${i}`}>{renderCard()}</Box>
      ))}
    </Box>
  )
}
