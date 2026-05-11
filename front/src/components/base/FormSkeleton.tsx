import { Box, Card, CardContent, Skeleton, Stack } from '@mui/material'

interface FormSkeletonProps {
  fields?: number
  hasActions?: boolean
}

export function FormSkeleton({ fields = 6, hasActions = true }: FormSkeletonProps) {
  return (
    <Card>
      <CardContent>
        <Stack spacing={3}>
          {/* Form Title */}
          <Skeleton variant="text" width="40%" height={32} />

          {/* Form Fields */}
          {Array.from({ length: fields }).map((_, i) => (
            <Box key={`field-${i}`}>
              <Skeleton variant="text" width="30%" height={20} sx={{ mb: 1 }} />
              <Skeleton variant="rounded" width="100%" height={56} />
            </Box>
          ))}

          {/* Action Buttons */}
          {hasActions && (
            <Stack direction="row" gap={2} justifyContent="flex-end" sx={{ mt: 2 }}>
              <Skeleton variant="rounded" width={100} height={40} />
              <Skeleton variant="rounded" width={100} height={40} />
            </Stack>
          )}
        </Stack>
      </CardContent>
    </Card>
  )
}
