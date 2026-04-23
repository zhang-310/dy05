import { useState, useCallback } from 'react'
import { Dialog, DialogTitle, DialogContent, DialogActions, Button } from '@mui/material'
import { useToast } from '@/contexts/ToastContext'
import { DataTablePage, type ColumnDef } from './DataTablePage'
import { FormDialog, type FormFieldDef } from '@/components/FormDialog'

interface CrudTablePageProps {
  title: string
  /** 副标题（与 toolbarVariant='live' 配合） */
  subtitle?: string
  /** 工具栏风格：live=直播场次风格 */
  toolbarVariant?: 'live' | 'default'
  fetchData: (params: { page?: number; rows?: number; keyword?: string }) => Promise<{ total: number; list: Record<string, unknown>[] }>
  columns: ColumnDef[]
  searchPlaceholder?: string
  showAdd?: boolean
  showEdit?: boolean
  showDelete?: boolean
  idKey?: string
  /** 表单字段（配置后启用新增/编辑弹窗） */
  formFields?: FormFieldDef[]
  /** 保存接口 */
  onSave?: (data: Record<string, unknown>) => Promise<unknown>
  /** 删除接口（返回后刷新列表） */
  onDelete?: (id: number) => Promise<void>
  /** 顶部操作按钮 */
  topActions?: React.ReactNode
}

export function CrudTablePage({
  title,
  subtitle,
  toolbarVariant = 'default',
  fetchData,
  columns,
  searchPlaceholder,
  showAdd = true,
  showEdit = true,
  showDelete = true,
  idKey = 'id',
  formFields,
  onSave,
  onDelete,
  topActions,
}: CrudTablePageProps) {
  const toast = useToast()
  const [dialogOpen, setDialogOpen] = useState(false)
  const [editingRow, setEditingRow] = useState<Record<string, unknown> | null>(null)
  const [deleteConfirm, setDeleteConfirm] = useState<Record<string, unknown> | null>(null)
  const [refreshKey, setRefreshKey] = useState(0)

  const refresh = useCallback(() => setRefreshKey((k) => k + 1), [])

  const handleAdd = () => {
    setEditingRow(null)
    setDialogOpen(true)
  }

  const handleEdit = (row: Record<string, unknown>) => {
    setEditingRow(row)
    setDialogOpen(true)
  }

  const handleSave = async (data: Record<string, unknown>) => {
    if (onSave) {
      await onSave(data)
      toast('保存成功', 'success')
      setDialogOpen(false)
      refresh()
    }
  }

  const handleDeleteClick = (row: Record<string, unknown>) => {
    setDeleteConfirm(row)
  }

  const handleDeleteConfirm = async () => {
    if (!onDelete || !deleteConfirm) return
    const id = Number(deleteConfirm[idKey])
    if (!id) return
    try {
      await onDelete(id)
      toast('删除成功', 'success')
      setDeleteConfirm(null)
      refresh()
    } catch (e) {
      toast(e instanceof Error ? e.message : '删除失败', 'error')
    }
  }

  const hasForm = formFields && formFields.length > 0 && onSave
  const hasDeleteApi = !!onDelete

  const handleBatchDelete = onDelete
    ? async (ids: unknown[]) => {
        for (const id of ids) {
          await onDelete(Number(id))
        }
      }
    : undefined

  return (
    <>
      <DataTablePage
        key={refreshKey}
        title={title}
        subtitle={subtitle}
        toolbarVariant={toolbarVariant}
        fetchData={fetchData}
        columns={columns}
        searchPlaceholder={searchPlaceholder}
        idKey={idKey}
        topActions={topActions}
        onAdd={showAdd && hasForm ? handleAdd : showAdd && !hasForm ? () => toast('请配置 formFields 和 onSave', 'info') : undefined}
        onEdit={showEdit && hasForm ? handleEdit : showEdit && !hasForm ? () => toast('请配置 formFields 和 onSave', 'info') : undefined}
        onDelete={showDelete && hasDeleteApi ? handleDeleteClick : showDelete && !hasDeleteApi ? () => toast('该资源不支持删除', 'info') : undefined}
        onBatchDelete={hasDeleteApi ? handleBatchDelete : undefined}
      />

      {hasForm && (
        <FormDialog
          open={dialogOpen}
          onClose={() => setDialogOpen(false)}
          title={editingRow ? `编辑${title}` : `新增${title}`}
          fields={formFields}
          initialValues={editingRow ?? {}}
          onSubmit={handleSave}
        />
      )}

      {deleteConfirm && (
        <Dialog open onClose={() => setDeleteConfirm(null)}>
          <DialogTitle>确认删除</DialogTitle>
          <DialogContent>确定要删除该记录吗？此操作不可恢复。</DialogContent>
          <DialogActions>
            <Button onClick={() => setDeleteConfirm(null)}>取消</Button>
            <Button variant="contained" color="error" onClick={handleDeleteConfirm}>
              删除
            </Button>
          </DialogActions>
        </Dialog>
      )}
    </>
  )
}
