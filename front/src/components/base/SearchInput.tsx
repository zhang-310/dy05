import { useState, type KeyboardEvent } from 'react'
import { TextField, InputAdornment, IconButton } from '@mui/material'
import { alpha } from '@mui/material/styles'
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
 * - 背景：background.paper
 * - 边框：divider
 * - 焦点：primary 边框 + 阴影
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

  const handleKeyDown = (e: KeyboardEvent) => {
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
      data-testid="base-search-input"
      data-search-tone="neutral"
      inputProps={{ 'data-testid': 'base-search-input-field' }}
      sx={(theme) => ({
        '& .MuiOutlinedInput-root': {
          minHeight: '44px',
          padding: '12px 16px',
          borderRadius: '6px', // border-radius-md
          backgroundColor: theme.palette.background.paper,
          color: theme.palette.text.primary,
          transition: 'all 200ms cubic-bezier(0.4, 0, 0.2, 1)',
          '& .MuiOutlinedInput-notchedOutline': {
            borderColor: theme.palette.divider,
          },
          '&:hover': {
            backgroundColor: theme.palette.action.hover,
            '& .MuiOutlinedInput-notchedOutline': {
              borderColor: theme.palette.text.secondary,
            },
          },
          '&.Mui-focused': {
            backgroundColor: theme.palette.background.paper,
            boxShadow: `0 0 0 3px ${alpha(theme.palette.primary.main, 0.12)}`,
            '& .MuiOutlinedInput-notchedOutline': {
              borderColor: theme.palette.primary.main,
            },
          },
        },
        '& .MuiOutlinedInput-input': {
          fontSize: '14px', // font-size-base
          color: theme.palette.text.primary,
          padding: 0,
          '&::placeholder': {
            color: theme.palette.text.secondary,
            opacity: 0.7,
          },
        },
        '& .MuiInputBase-adornedStart': {
          paddingLeft: 0,
        },
        '& .MuiInputAdornment-positionStart': {
          marginRight: '8px', // spacing-sm
          color: theme.palette.text.secondary,
        },
        '& .MuiInputAdornment-positionEnd': {
          marginRight: 0,
        },
      })}
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
                color: 'text.secondary',
                '&:hover': {
                  color: 'text.primary',
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
