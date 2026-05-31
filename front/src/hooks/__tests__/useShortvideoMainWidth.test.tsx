import { describe, expect, it, beforeEach } from 'vitest'
import { act, renderWithProviders, screen } from '@/test/utils'
import {
  normalizeShortvideoMainWidth,
  setShortvideoMainWidth,
  tokenToMaxWidth,
  useShortvideoMainWidth,
} from '../useShortvideoMainWidth'

function Probe() {
  const { maxWidth, selectValue, setWidth } = useShortvideoMainWidth()
  return (
    <div>
      <span data-testid="value">{selectValue}</span>
      <span data-testid="max">{String(maxWidth)}</span>
      <button type="button" onClick={() => setWidth('1680')}>set 1680</button>
      <button type="button" onClick={() => setWidth('full')}>set full</button>
    </div>
  )
}

describe('useShortvideoMainWidth', () => {
  beforeEach(() => {
    window.localStorage.clear()
  })

  it('normalizes invalid storage values to the default width', () => {
    expect(normalizeShortvideoMainWidth(null)).toBe('1440')
    expect(normalizeShortvideoMainWidth('bad')).toBe('1440')
    expect(tokenToMaxWidth('full')).toBe('full')
    expect(tokenToMaxWidth('1680')).toBe(1680)
  })

  it('syncs localStorage changes through the hook setter and custom event', () => {
    window.localStorage.setItem('shortvideo.mainContentMax', 'bad')
    renderWithProviders(<Probe />)

    expect(screen.getByTestId('value')).toHaveTextContent('1440')
    expect(screen.getByTestId('max')).toHaveTextContent('1440')

    act(() => {
      screen.getByText('set 1680').click()
    })
    expect(screen.getByTestId('value')).toHaveTextContent('1680')
    expect(screen.getByTestId('max')).toHaveTextContent('1680')

    act(() => {
      screen.getByText('set full').click()
    })
    expect(screen.getByTestId('value')).toHaveTextContent('full')
    expect(screen.getByTestId('max')).toHaveTextContent('full')
  })

  it('guards direct setter calls with normalized values', () => {
    act(() => {
      setShortvideoMainWidth('bad' as never)
    })

    expect(window.localStorage.getItem('shortvideo.mainContentMax')).toBe('1440')
  })
})
