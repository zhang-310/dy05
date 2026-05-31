import { describe, expect, it, vi, beforeEach } from 'vitest'
import { fireEvent, renderWithProviders, screen } from '@/test/utils'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import { ImageUploader } from '../ImageUploader'
import { ShotTimeline, buildTimeRanges, parseDurationFromTimeRange } from '../ShotTimeline'
import { VideoPlayer } from '../VideoPlayer'
import { VideoTimeline } from '../VideoTimeline'

describe('shortvideo shared components', () => {
  beforeEach(() => {
    window.localStorage.clear()
  })

  it('uses theme surfaces for uploader and shot timeline in dark mode', () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')

    renderWithProviders(
      <AppThemeProvider>
        <>
          <ImageUploader onFilesSelected={vi.fn()} />
          <ShotTimeline
            shots={[
              { shotNumber: 1, duration: 3 },
              { shotNumber: 2, duration: 5 },
            ]}
            totalDuration={8}
            onDurationChange={vi.fn()}
          />
        </>
      </AppThemeProvider>,
    )

    expect(screen.getByTestId('shortvideo-image-upload-zone')).toHaveStyle({
      backgroundColor: 'rgb(18, 18, 18)',
    })
    expect(screen.getByTestId('shortvideo-shot-timeline')).toHaveStyle({
      backgroundColor: 'rgb(18, 18, 18)',
    })
  })

  it('handles image file selection and timeline duration helpers', () => {
    const onFilesSelected = vi.fn().mockResolvedValue(undefined)
    const onDurationChange = vi.fn()

    renderWithProviders(
      <>
        <ImageUploader onFilesSelected={onFilesSelected} maxFiles={1} />
        <ShotTimeline
          shots={[{ shotNumber: 1, duration: 3 }]}
          totalDuration={3}
          onDurationChange={onDurationChange}
        />
      </>,
    )

    const file = new File(['img'], 'shot.png', { type: 'image/png' })
    fireEvent.change(document.querySelector('#image-uploader-input') as HTMLInputElement, {
      target: { files: [file] },
    })
    expect(onFilesSelected).toHaveBeenCalledWith([file])
    expect(parseDurationFromTimeRange('3-10s')).toBe(7)
    expect(parseDurationFromTimeRange('6s')).toBe(6)
    expect(parseDurationFromTimeRange()).toBe(5)
    expect(buildTimeRanges([3, 5, 2])).toEqual(['0-3s', '3-8s', '8-10s'])
  })

  it('marks video player and video timeline media overlays as explicit preview policy', () => {
    renderWithProviders(
      <AppThemeProvider>
        <>
          <VideoPlayer src="https://cdn.example.com/video.mp4" poster="https://cdn.example.com/poster.jpg" />
          <VideoTimeline
            clips={[{ url: 'https://cdn.example.com/clip-1.mp4', duration: 5, label: 'clip-1' }]}
            audioTrack={{ url: 'https://cdn.example.com/voice.mp3', label: 'voice' }}
            bgmUrl="https://cdn.example.com/bgm.mp3"
          />
        </>
      </AppThemeProvider>,
    )

    const player = screen.getByTestId('shortvideo-video-player')
    const controls = screen.getByTestId('shortvideo-video-player-controls')
    const timeline = screen.getByTestId('shortvideo-video-timeline')
    const clipLabel = screen.getByTestId('shortvideo-video-timeline-clip-label')

    expect(player).toHaveAttribute('data-media-tone', 'video-stage')
    expect(controls).toHaveAttribute('data-media-tone', 'control-overlay')
    expect(timeline).toHaveAttribute('data-media-tone', 'timeline-stage')
    expect(clipLabel).toHaveAttribute('data-media-tone', 'clip-label-overlay')

    expect(controls.outerHTML).not.toContain('rgba(0,0,0,0.8)')
    expect(clipLabel.outerHTML).not.toContain('rgba(0,0,0,0.7)')
  })
})
