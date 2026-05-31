import { describe, it, expect, vi, beforeEach } from 'vitest'
import { fireEvent, renderWithProviders, screen } from '@/test/utils'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import UploadDialog from '../UploadDialog'

const uploadHookState = vi.hoisted(() => ({
  task: null as null | {
    uploadId: string
    filename: string
    fileSize: number
    totalChunks: number
    uploadedChunks: number
    uploadedBytes: number
    progressPercent: number
    status: string
    error?: string
  },
  isUploading: false,
  progressEmitted: false,
  startUpload: vi.fn(),
  pauseUpload: vi.fn(),
  resumeUpload: vi.fn(),
  abortUpload: vi.fn(),
}))

vi.mock('@/hooks/useChunkedUpload', () => ({
  useChunkedUpload: vi.fn((options?: { onProgress?: (stats: any) => void }) => {
    if (uploadHookState.isUploading && !uploadHookState.progressEmitted) {
      uploadHookState.progressEmitted = true
      options?.onProgress?.({
        totalSize: 8 * 1024 * 1024,
        uploadedSize: 4 * 1024 * 1024,
        totalChunks: 4,
        uploadedChunks: 2,
        failedChunks: 1,
        progressPercent: 50,
        uploadSpeed: 1024 * 1024,
        remainingTime: 4,
      })
    }

    return uploadHookState
  }),
}))

describe('UploadDialog', () => {
  beforeEach(() => {
    window.localStorage.clear()
    uploadHookState.task = null
    uploadHookState.isUploading = false
    uploadHookState.progressEmitted = false
    vi.clearAllMocks()
  })

  it('uses theme-aware dropzone and selected-file surfaces in dark mode', () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')

    renderWithProviders(
      <AppThemeProvider>
        <UploadDialog open onClose={vi.fn()} />
      </AppThemeProvider>,
    )

    expect(screen.getByTestId('upload-dialog-dropzone-surface')).toHaveAttribute('data-dropzone-color', '#e3f2fd')
    expect(screen.getByTestId('upload-dialog-dropzone-surface')).not.toHaveStyle({ border: '2px dashed #ccc' })
    expect(screen.getByTestId('upload-dialog-icon-surface')).not.toHaveStyle({ color: '#999' })

    const file = new File(['hello'], 'script.md', { type: 'text/markdown' })
    fireEvent.change(document.querySelector('input[type="file"]') as HTMLInputElement, {
      target: { files: [file] },
    })

    expect(screen.getByTestId('upload-dialog-selected-file-surface')).not.toHaveStyle({ backgroundColor: 'rgb(245, 245, 245)' })
    expect(screen.getByText('文件名：script.md')).toBeInTheDocument()

    const serialized = document.body.innerHTML
    for (const legacy of ['#ccc', '#1976d2', '#f5f5f5', '#999']) {
      expect(serialized).not.toContain(legacy)
    }
  })

  it('keeps upload progress actions and warning state while using themed surfaces', () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')
    uploadHookState.isUploading = true
    uploadHookState.task = {
      uploadId: 'upload-1',
      filename: 'video.mp4',
      fileSize: 8 * 1024 * 1024,
      totalChunks: 4,
      uploadedChunks: 2,
      uploadedBytes: 4 * 1024 * 1024,
      progressPercent: 50,
      status: 'UPLOADING',
    }

    renderWithProviders(
      <AppThemeProvider>
        <UploadDialog open onClose={vi.fn()} />
      </AppThemeProvider>,
    )

    expect(screen.getByTestId('upload-dialog-progress-info-surface')).not.toHaveStyle({ backgroundColor: 'rgb(245, 245, 245)' })
    expect(screen.getByTestId('upload-dialog-progress-surface')).toBeInTheDocument()
    expect(screen.getByText('2 / 4')).toBeInTheDocument()
    expect(screen.getByText('1 个分块上传失败，已自动重试')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '暂停' }))
    expect(uploadHookState.pauseUpload).toHaveBeenCalled()

    const serialized = document.body.innerHTML
    for (const legacy of ['#ccc', '#1976d2', '#f5f5f5', '#999']) {
      expect(serialized).not.toContain(legacy)
    }
  })
})
