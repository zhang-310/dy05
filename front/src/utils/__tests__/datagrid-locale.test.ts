import { describe, it, expect } from 'vitest'
import { dataGridLocale } from '../datagrid-locale'

describe('dataGridLocale', () => {
  it('has noRowsLabel in Chinese', () => {
    expect(dataGridLocale.noRowsLabel).toBe('没有数据。')
  })

  it('has noResultsOverlayLabel', () => {
    expect(dataGridLocale.noResultsOverlayLabel).toBe('未找到数据。')
  })

  it('has footerRowSelected function', () => {
    expect(typeof dataGridLocale.footerRowSelected).toBe('function')
    const fn = dataGridLocale.footerRowSelected as (count: number) => string
    expect(fn(3)).toContain('3')
  })

  it('has MuiTablePagination translations', () => {
    expect(dataGridLocale.MuiTablePagination).toBeDefined()
  })

  it('has toolbar translations', () => {
    expect(dataGridLocale.toolbarColumns).toBeDefined()
    expect(dataGridLocale.toolbarExport).toBeDefined()
  })

  it('has column menu translations', () => {
    expect(dataGridLocale.columnMenuSortAsc).toBeDefined()
    expect(dataGridLocale.columnMenuSortDesc).toBeDefined()
    expect(dataGridLocale.columnMenuFilter).toBeDefined()
  })
})
