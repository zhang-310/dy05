import { useState } from 'react'
import { TextField, InputAdornment, IconButton } from '@mui/material'
import { Search as SearchIcon, Clear as ClearIcon } from '@mui/icons-material'

interface SearchInputProps {
  placeholder?: string
  onSearch: (query: string) => void
  onClear?: () => void
  defaultValue?: string
}

/**
 * SearchInput - 搜索输入框组件
 * 应用设计系统规范：
 * - 高度：44px
 * - 圆角：6px (border-radius-md)
 * - 背景：color-surface-dark
 * - 边框：color-surface-light
 * - 焦点：绿色边框 + 阴影
 */
export function SearchInput({
  placeholder = '搜索...',
  onSearch,
  onClear,
  defaultValue = '',
}: SearchInputProps) {
  const [value, setValue] = useState(defaultValue)

  const handleSearch = () => {
    onSearch(value)
  }

  const handleClear = () => {
    setValue('')
    onClear?.()
  }

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      handleSearch()
    }
  }

  return (
    <TextField
      value={value}
      onChange={(e) => setValue(e.target.value)}
      onKeyDown={handleKeyDown}
      placeholder={placeholder}
      fullWidth
      sx={{
        '& .MuiOutlinedInput-root': {
          minHeight: '44px',
          padding: '12px 16px',
          borderRadius: '6px', // border-radius-md
          backgroundColor: '#0F172A', // color-surface-dark
          color: '#F1F5F9', // color-text-primary
          border: '1px solid #334155', // color-surface-light
          transition: 'all 200ms cubic-bezier(0.4, 0, 0.2, 1)',
          '&:hover': {
            borderColor: '#475569', // color-text-secondary
          },
          '&.Mui-focused': {
            borderColor: '#00D084', // color-primary
            boxShadow: '0 0 0 3px rgba(0, 208, 132, 0.1)',
          },
        },
        '& .MuiOutlinedInput-input': {
          fontSize: '14px', // font-size-base
          color: '#F1F5F9', // color-text-primary
          padding: 0,
          '&::placeholder': {
            color: '#475569', // color-text-secondary
            opacity: 0.7,
          },
        },
        '& .MuiInputBase-adornedStart': {
          paddingLeft: 0,
        },
        '& .MuiInputAdornment-positionStart': {
          marginRight: '8px', // spacing-sm
          color: '#475569', // color-text-secondary
        },
        '& .MuiInputAdornment-positionEnd': {
          marginRight: 0,
        },
      }}
      InputProps={{
        startAdornment: (
          <InputAdornment position="start">
            <SearchIcon sx={{ fontSize: '20px' }} />
          </InputAdornment>
        ),
        endAdornment: value ? (
          <InputAdornment position="end">
            <IconButton
              size="small"
              onClick={handleClear}
              aria-label="清除"
              sx={{
                padding: '4px',
                color: '#475569', // color-text-secondary
                '&:hover': {
                  color: '#F1F5F9', // color-text-primary
                },
              }}
            >
              <ClearIcon fontSize="small" />
            </IconButton>
          </InputAdornment>
        ) : undefined,
      }}
    />
  )
}
