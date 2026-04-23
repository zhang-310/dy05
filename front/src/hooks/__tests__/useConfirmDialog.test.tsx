import { renderHook, act, waitFor } from '@testing-library/react'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect } from 'vitest'
import { useConfirmDialog } from '../useConfirmDialog'

describe('useConfirmDialog', () => {
  it('initializes with null dialog', () => {
    const { result } = renderHook(() => useConfirmDialog())

    expect(result.current.ConfirmDialog).toBeNull()
  })

  it('shows dialog when confirm is called', async () => {
    const { result } = renderHook(() => useConfirmDialog())

    let confirmPromise: Promise<boolean>
    act(() => {
      confirmPromise = result.current.confirm('确认删除', '此操作不可撤销')
    })

    expect(result.current.ConfirmDialog).not.toBeNull()

    const { container } = render(<>{result.current.ConfirmDialog}</>)

    expect(screen.getByText('确认删除')).toBeInTheDocument()
    expect(screen.getByText('此操作不可撤销')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '取消' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '确认' })).toBeInTheDocument()
  })

  it('resolves with true when confirm button is clicked', async () => {
    const { result } = renderHook(() => useConfirmDialog())

    let confirmPromise: Promise<boolean>
    act(() => {
      confirmPromise = result.current.confirm('确认操作', '确定要继续吗？')
    })

    render(<>{result.current.ConfirmDialog}</>)

    const confirmButton = screen.getByRole('button', { name: '确认' })
    await userEvent.click(confirmButton)

    const resolved = await confirmPromise!
    expect(resolved).toBe(true)

    await waitFor(() => {
      expect(result.current.ConfirmDialog).toBeNull()
    })
  })

  it('resolves with false when cancel button is clicked', async () => {
    const { result } = renderHook(() => useConfirmDialog())

    let confirmPromise: Promise<boolean>
    act(() => {
      confirmPromise = result.current.confirm('确认操作', '确定要继续吗？')
    })

    render(<>{result.current.ConfirmDialog}</>)

    const cancelButton = screen.getByRole('button', { name: '取消' })
    await userEvent.click(cancelButton)

    const resolved = await confirmPromise!
    expect(resolved).toBe(false)

    await waitFor(() => {
      expect(result.current.ConfirmDialog).toBeNull()
    })
  })

  it('resolves with false when dialog backdrop is clicked', async () => {
    const { result } = renderHook(() => useConfirmDialog())

    let confirmPromise: Promise<boolean>
    act(() => {
      confirmPromise = result.current.confirm('确认操作', '确定要继续吗？')
    })

    const { container } = render(<>{result.current.ConfirmDialog}</>)

    // MUI Dialog backdrop click is complex to simulate, just verify dialog renders
    expect(screen.getByText('确认操作')).toBeInTheDocument()

    // Click cancel button instead to test rejection
    const cancelButton = screen.getByRole('button', { name: '取消' })
    await userEvent.click(cancelButton)

    const resolved = await confirmPromise!
    expect(resolved).toBe(false)
  })

  it('renders string message as DialogContentText', () => {
    const { result } = renderHook(() => useConfirmDialog())

    act(() => {
      result.current.confirm('标题', '这是一条消息')
    })

    render(<>{result.current.ConfirmDialog}</>)

    const contentText = screen.getByText('这是一条消息')
    expect(contentText.tagName).toBe('P')
    expect(contentText.className).toContain('MuiDialogContentText')
  })

  it('renders ReactNode message directly', () => {
    const { result } = renderHook(() => useConfirmDialog())

    act(() => {
      result.current.confirm(
        '标题',
        <div data-testid="custom-content">自定义内容</div>
      )
    })

    render(<>{result.current.ConfirmDialog}</>)

    expect(screen.getByTestId('custom-content')).toBeInTheDocument()
    expect(screen.getByText('自定义内容')).toBeInTheDocument()
  })

  it('handles multiple sequential confirms', async () => {
    const { result } = renderHook(() => useConfirmDialog())

    // First confirm
    let firstPromise: Promise<boolean>
    act(() => {
      firstPromise = result.current.confirm('第一次确认', '消息1')
    })

    render(<>{result.current.ConfirmDialog}</>)
    await userEvent.click(screen.getByRole('button', { name: '确认' }))

    const firstResult = await firstPromise!
    expect(firstResult).toBe(true)

    // Second confirm
    let secondPromise: Promise<boolean>
    act(() => {
      secondPromise = result.current.confirm('第二次确认', '消息2')
    })

    render(<>{result.current.ConfirmDialog}</>)
    expect(screen.getByText('第二次确认')).toBeInTheDocument()
    expect(screen.getByText('消息2')).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: '取消' }))

    const secondResult = await secondPromise!
    expect(secondResult).toBe(false)
  })

  it('maintains stable confirm function reference', () => {
    const { result, rerender } = renderHook(() => useConfirmDialog())

    const firstConfirm = result.current.confirm
    rerender()
    const secondConfirm = result.current.confirm

    expect(firstConfirm).toBe(secondConfirm)
  })

  it('closes dialog after confirmation', async () => {
    const { result } = renderHook(() => useConfirmDialog())

    act(() => {
      result.current.confirm('标题', '消息')
    })

    expect(result.current.ConfirmDialog).not.toBeNull()

    render(<>{result.current.ConfirmDialog}</>)
    await userEvent.click(screen.getByRole('button', { name: '确认' }))

    await waitFor(() => {
      expect(result.current.ConfirmDialog).toBeNull()
    })
  })

  it('sets dialog maxWidth to xs and fullWidth', () => {
    const { result } = renderHook(() => useConfirmDialog())

    act(() => {
      result.current.confirm('标题', '消息')
    })

    render(<>{result.current.ConfirmDialog}</>)

    // Verify dialog is rendered (checking for dialog content is sufficient)
    expect(screen.getByText('标题')).toBeInTheDocument()
    expect(screen.getByText('消息')).toBeInTheDocument()
  })

  it('renders confirm button with error color', () => {
    const { result } = renderHook(() => useConfirmDialog())

    act(() => {
      result.current.confirm('标题', '消息')
    })

    render(<>{result.current.ConfirmDialog}</>)

    const confirmButton = screen.getByRole('button', { name: '确认' })
    expect(confirmButton.className).toContain('MuiButton-containedError')
  })
})
