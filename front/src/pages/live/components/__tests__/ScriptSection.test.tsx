import { describe, it, expect, vi, beforeEach } from 'vitest'
import { screen, fireEvent } from '@testing-library/react'
import { renderWithProviders } from '@/test/utils'
import { ScriptSection, type ScriptSectionProps } from '../ScriptSection'

vi.mock('@/components/script/ScriptEditor', () => ({
  ScriptEditor: ({ value, onChange }: { value: string; onChange: (v: string) => void }) => (
    <textarea data-testid="script-editor" value={value} onChange={(e) => onChange(e.target.value)} />
  ),
}))
vi.mock('../ScriptCommentPopover', () => ({
  ScriptCommentPopover: () => <div data-testid="comment-popover" />,
}))

const mockScripts = [
  { id: 1, scriptContent: '大家好欢迎来到直播间', scriptType: 'opening', sequenceNo: 1, executed: 0, aiGenerated: 1, estimatedDurationSeconds: 30 },
  { id: 2, scriptContent: '今天给大家推荐一款', scriptType: 'product', sequenceNo: 2, executed: 1, aiGenerated: 1, estimatedDurationSeconds: 60 },
]

function makeProps(overrides?: Partial<ScriptSectionProps>): ScriptSectionProps {
  return {
    title: '开场区',
    scripts: mockScripts,
    expanded: true,
    onToggle: vi.fn(),
    editingId: null,
    editContent: '',
    violationResult: {},
    checkingId: null,
    saveLibLoading: false,
    onEdit: vi.fn(),
    onSaveEdit: vi.fn(),
    onCancelEdit: vi.fn(),
    onEditContentChange: vi.fn(),
    onCheckViolation: vi.fn(),
    onSaveToLibrary: vi.fn(),
    onMarkExecuted: vi.fn(),
    onDelete: vi.fn(),
    onRefineOpen: vi.fn(),
    ...overrides,
  }
}

describe('ScriptSection', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders collapsed state and toggles expand', () => {
    const onToggle = vi.fn()
    renderWithProviders(<ScriptSection {...makeProps({ expanded: false, onToggle })} />)

    expect(screen.getByText('开场区')).toBeInTheDocument()
    expect(screen.getByText('2 条')).toBeInTheDocument()

    fireEvent.click(screen.getByText('开场区'))
    expect(onToggle).toHaveBeenCalled()
  })

  it('renders scripts in expanded state', () => {
    renderWithProviders(<ScriptSection {...makeProps()} />)

    expect(screen.getByText(/大家好欢迎来到直播间/)).toBeInTheDocument()
    expect(screen.getByText(/今天给大家推荐一款/)).toBeInTheDocument()
  })

  it('enters edit mode on edit button click', () => {
    const onEdit = vi.fn()
    renderWithProviders(<ScriptSection {...makeProps({ onEdit })} />)

    const editBtns = screen.getAllByTestId('EditIcon')
    fireEvent.click(editBtns[0].closest('button')!)
    expect(onEdit).toHaveBeenCalledWith(1, '大家好欢迎来到直播间', mockScripts[0])
  })

  it('shows editor and cancel in edit mode', () => {
    renderWithProviders(
      <ScriptSection {...makeProps({ editingId: 1, editContent: '编辑内容' })} />
    )

    expect(screen.getByText('取消')).toBeInTheDocument()
    expect(screen.getByTestId('script-editor')).toBeInTheDocument()
    // Edit mode has a save button with CheckIcon
    expect(screen.getByTestId('CheckIcon')).toBeInTheDocument()
  })

  it('renders without crashing with delete handler', () => {
    const onDelete = vi.fn()
    const { container } = renderWithProviders(<ScriptSection {...makeProps({ onDelete })} />)
    expect(container).toBeInTheDocument()
  })
})
