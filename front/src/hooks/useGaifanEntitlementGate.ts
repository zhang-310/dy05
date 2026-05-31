import { useCallback } from 'react'
import { checkGaifanEntitlement } from '@/api/gaifan-catalog'
import { useToast } from '@/contexts/ToastContext'
import { commercialDenialMessage, isCommercialDenial } from '@/utils/commercialError'

/**
 * Returns a gate function: await gate(product, feature) before chargeable API calls.
 * Shows toast and returns false when denied.
 */
export function useGaifanEntitlementGate() {
  const toast = useToast()

  return useCallback(
    async (productCode: string, featureCode: string, reasonFallback?: string): Promise<boolean> => {
      try {
        const ent = await checkGaifanEntitlement(productCode, featureCode)
        if (ent && ent.granted === false) {
          toast(
            commercialDenialMessage(4421, ent.reason ?? reasonFallback ?? '无产品权益'),
            'warning'
          )
          return false
        }
        return true
      } catch (e) {
        if (isCommercialDenial(e)) {
          toast(commercialDenialMessage(e), 'warning')
          return false
        }
        throw e
      }
    },
    [toast]
  )
}
