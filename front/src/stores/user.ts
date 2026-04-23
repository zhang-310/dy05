import { create } from 'zustand'
import { persist } from 'zustand/middleware'

interface UserInfo {
  id: number
  username: string
  nickname?: string
  roles: string[]
}

interface UserState {
  token: string | null
  userInfo: UserInfo | null
  setToken: (token: string) => void
  setUserInfo: (info: UserInfo) => void
  logout: () => void
}

export const useUserStore = create<UserState>()(
  persist(
    (set) => ({
      token: null,
      userInfo: null,
      setToken: (token) => set({ token }),
      setUserInfo: (userInfo) => set({ userInfo }),
      logout: () => {
        localStorage.removeItem('token')
        set({ token: null, userInfo: null })
      },
    }),
    { name: 'dy02-user' }
  )
)
