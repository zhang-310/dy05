/**
 * user store 测试
 */
import { describe, it, expect, beforeEach } from 'vitest'
import { useUserStore } from '../user'

describe('useUserStore', () => {
  beforeEach(() => {
    useUserStore.setState({
      token: null,
      userInfo: null,
    })
  })

  it('setToken updates token', () => {
    const { setToken } = useUserStore.getState()
    setToken('test-token-123')
    const s = useUserStore.getState()
    expect(s.token).toBe('test-token-123')
  })

  it('setUserInfo updates userInfo', () => {
    const { setUserInfo } = useUserStore.getState()
    setUserInfo({
      id: 1,
      username: 'test',
      nickname: 'Test User',
      roles: ['admin'],
    })
    const s = useUserStore.getState()
    expect(s.userInfo).toEqual({
      id: 1,
      username: 'test',
      nickname: 'Test User',
      roles: ['admin'],
    })
  })

  it('setUserInfo handles optional fields', () => {
    const { setUserInfo } = useUserStore.getState()
    setUserInfo({
      id: 2,
      username: 'u2',
      roles: ['user'],
    })
    const s = useUserStore.getState()
    expect(s.userInfo?.nickname).toBeUndefined()
  })

  it('logout resets state', () => {
    const { setToken, setUserInfo, logout } = useUserStore.getState()
    setToken('token')
    setUserInfo({ id: 1, username: 'x', roles: ['user'] })
    logout()
    const s = useUserStore.getState()
    expect(s.token).toBeNull()
    expect(s.userInfo).toBeNull()
  })
})
