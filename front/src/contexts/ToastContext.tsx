import { createContext, useContext, useCallback } from 'react'
import { SnackbarProvider, useSnackbar, VariantType } from 'notistack'

type ToastFn = (message: string, severity?: VariantType) => void

const ToastContext = createContext<ToastFn>(() => {})

function ToastInner({ children }: { children: React.ReactNode }) {
  const { enqueueSnackbar } = useSnackbar()
  const toast = useCallback<ToastFn>(
    (message, severity = 'default') => {
      enqueueSnackbar(message, { variant: severity })
    },
    [enqueueSnackbar]
  )
  return <ToastContext.Provider value={toast}>{children}</ToastContext.Provider>
}

export function ToastProvider({ children }: { children: React.ReactNode }) {
  return (
    <SnackbarProvider maxSnack={3} anchorOrigin={{ vertical: 'top', horizontal: 'right' }}>
      <ToastInner>{children}</ToastInner>
    </SnackbarProvider>
  )
}

export function useToast(): ToastFn {
  return useContext(ToastContext)
}
