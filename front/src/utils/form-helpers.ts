import { useCallback, useState } from 'react'

/** 必填校验，通过返回 null */
export function required(value: string | null | undefined, message = '必填'): string | null {
  if (value == null || String(value).trim() === '') return message
  return null
}

export function maxLength(max: number, message?: string): (value: string) => string | null {
  const msg = message ?? `最多 ${max} 个字符`
  return (value: string) => (value.length > max ? msg : null)
}

export function numberRange(min: number, max: number, message?: string): (value: string) => string | null {
  const msg = message ?? `请输入 ${min}–${max} 之间的数字`
  return (value: string) => {
    const n = Number(value)
    if (!Number.isFinite(n) || n < min || n > max) return msg
    return null
  }
}

type FieldErrors<T extends Record<string, unknown>> = Partial<Record<keyof T, string>>

/** 轻量表单状态 hook（不引入 react-hook-form） */
export function useFormFields<T extends Record<string, unknown>>(initial: T) {
  const [values, setValues] = useState<T>(() => ({ ...initial }))
  const [errors, setErrors] = useState<FieldErrors<T>>({})
  const [dirty, setDirty] = useState(false)

  const setField = useCallback(<K extends keyof T>(key: K, value: T[K]) => {
    setDirty(true)
    setValues((prev) => ({ ...prev, [key]: value }))
    setErrors((prev) => {
      const next = { ...prev }
      delete next[key]
      return next
    })
  }, [])

  const validate = useCallback(
    (rules: Partial<{ [K in keyof T]: (v: T[K]) => string | null }>) => {
      const next: FieldErrors<T> = {}
      for (const key of Object.keys(rules) as (keyof T)[]) {
        const rule = rules[key]
        if (!rule) continue
        const err = rule(values[key])
        if (err) next[key] = err
      }
      setErrors(next)
      return Object.keys(next).length === 0
    },
    [values],
  )

  const reset = useCallback(() => {
    setValues({ ...initial })
    setErrors({})
    setDirty(false)
  }, [initial])

  return { values, errors, setField, validate, reset, isDirty: dirty }
}
