import { describe, expect, it, vi } from 'vitest'
import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '@/test/utils'
import ShoppingCartFloat from './index'
import FloatButton from './FloatButton'

describe('ShoppingCartFloat', () => {
  it('opens the drawer and shows cart totals', async () => {
    const onCheckout = vi.fn()
    const user = userEvent.setup()

    renderWithProviders(
      <ShoppingCartFloat
        items={[
          { id: 'sku-1', name: '补水精华', price: 99, quantity: 2 },
          { id: 'sku-2', name: '修护面霜', price: 129, quantity: 1 },
        ]}
        onCheckout={onCheckout}
      />,
    )

    await user.click(screen.getByRole('button', { name: '打开购物车' }))

    expect(screen.getByText('购物车')).toBeInTheDocument()
    expect(screen.getByText('补水精华')).toBeInTheDocument()
    expect(screen.getByText('¥327.00')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: '前往支付' }))
    expect(onCheckout).toHaveBeenCalledTimes(1)
  })

  it('shows a real empty state when no cart items exist', async () => {
    const user = userEvent.setup()
    renderWithProviders(<ShoppingCartFloat items={[]} />)

    await user.click(screen.getByRole('button', { name: '打开购物车' }))

    expect(screen.getByText('购物车为空')).toBeInTheDocument()
  })

  it('uses theme tones for the floating button and drawer surfaces', async () => {
    const user = userEvent.setup()
    renderWithProviders(<ShoppingCartFloat items={[{ id: 'sku-1', name: '补水精华', price: 99, quantity: 1 }]} />)

    const button = screen.getByTestId('shopping-cart-float-button')
    expect(button).toHaveAttribute('data-cart-tone', 'primary')
    expect(button.outerHTML).not.toContain('#00D084')
    expect(button.outerHTML).not.toContain('#0F172A')
    expect(button.outerHTML).not.toContain('#1E293B')

    await user.click(screen.getByRole('button', { name: '打开购物车' }))
    const drawer = await screen.findByTestId('shopping-cart-drawer-surface')
    expect(drawer).toHaveAttribute('data-cart-tone', 'surface')
    expect(drawer.outerHTML).not.toContain('#1E293B')
    expect(drawer.outerHTML).not.toContain('#334155')
    expect(drawer.outerHTML).not.toContain('#F1F5F9')
    expect(drawer.outerHTML).not.toContain('#475569')
    expect(drawer.outerHTML).not.toContain('#00D084')
  })
})

describe('FloatButton', () => {
  it('caps the badge count and preserves aria state', () => {
    renderWithProviders(<FloatButton count={128} aria-label="打开购物车" aria-expanded={true} />)

    expect(screen.getByText('99+')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '打开购物车' })).toHaveAttribute('aria-expanded', 'true')
  })
})
