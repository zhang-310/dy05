/**
 * 通用 Mocks
 * 提供常用的 mock 函数和对象
 */

import { vi } from 'vitest'

/**
 * Mock React Router
 */
export const mockNavigate = vi.fn()
export const mockUseNavigate = () => mockNavigate
export const mockUseLocation = () => ({
  pathname: '/test',
  search: '',
  hash: '',
  state: null,
  key: 'default',
})
export const mockUseParams = () => ({})
export const mockUseSearchParams = () => [new URLSearchParams(), vi.fn()]

/**
 * Mock Axios
 */
export const mockAxios = {
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  delete: vi.fn(),
  patch: vi.fn(),
  request: vi.fn(),
  interceptors: {
    request: {
      use: vi.fn(),
      eject: vi.fn(),
    },
    response: {
      use: vi.fn(),
      eject: vi.fn(),
    },
  },
}

/**
 * Mock Notistack
 */
export const mockEnqueueSnackbar = vi.fn()
export const mockCloseSnackbar = vi.fn()
export const mockUseSnackbar = () => ({
  enqueueSnackbar: mockEnqueueSnackbar,
  closeSnackbar: mockCloseSnackbar,
})

/**
 * Mock TanStack Query
 */
export const mockUseQuery = vi.fn()
export const mockUseMutation = vi.fn()
export const mockQueryClient = {
  invalidateQueries: vi.fn(),
  setQueryData: vi.fn(),
  getQueryData: vi.fn(),
  removeQueries: vi.fn(),
  clear: vi.fn(),
}

/**
 * Mock Zustand Store
 */
export function createMockStore<T>(initialState: T) {
  let state = initialState

  return {
    getState: () => state,
    setState: (newState: Partial<T>) => {
      state = { ...state, ...newState }
    },
    subscribe: vi.fn(),
    destroy: vi.fn(),
  }
}

/**
 * Mock File
 */
export function createMockFile(
  name = 'test.txt',
  size = 1024,
  type = 'text/plain'
): File {
  const blob = new Blob(['test content'], { type })
  return new File([blob], name, { type })
}

/**
 * Mock Image
 */
export function createMockImage(
  name = 'test.jpg',
  size = 2048,
  type = 'image/jpeg'
): File {
  const blob = new Blob(['fake image content'], { type })
  return new File([blob], name, { type })
}

/**
 * Mock FileReader
 */
export class MockFileReader {
  result: string | ArrayBuffer | null = null
  error: Error | null = null
  onload: ((event: ProgressEvent<FileReader>) => void) | null = null
  onerror: ((event: ProgressEvent<FileReader>) => void) | null = null

  readAsDataURL(blob: Blob) {
    setTimeout(() => {
      this.result = 'data:text/plain;base64,dGVzdCBjb250ZW50'
      if (this.onload) {
        this.onload({ target: this } as ProgressEvent<FileReader>)
      }
    }, 0)
  }

  readAsText(blob: Blob) {
    setTimeout(() => {
      this.result = 'test content'
      if (this.onload) {
        this.onload({ target: this } as ProgressEvent<FileReader>)
      }
    }, 0)
  }
}

/**
 * Mock Clipboard API
 */
export const mockClipboard = {
  writeText: vi.fn().mockResolvedValue(undefined),
  readText: vi.fn().mockResolvedValue(''),
}

Object.defineProperty(navigator, 'clipboard', {
  value: mockClipboard,
  writable: true,
})

/**
 * Mock WebSocket
 */
export class MockWebSocket {
  url: string
  readyState = WebSocket.CONNECTING
  onopen: ((event: Event) => void) | null = null
  onclose: ((event: CloseEvent) => void) | null = null
  onmessage: ((event: MessageEvent) => void) | null = null
  onerror: ((event: Event) => void) | null = null

  constructor(url: string) {
    this.url = url
    setTimeout(() => {
      this.readyState = WebSocket.OPEN
      if (this.onopen) {
        this.onopen(new Event('open'))
      }
    }, 0)
  }

  send(data: string) {
    // Mock send
  }

  close() {
    this.readyState = WebSocket.CLOSED
    if (this.onclose) {
      this.onclose(new CloseEvent('close'))
    }
  }
}

/**
 * Mock EventSource (SSE)
 */
export class MockEventSource {
  url: string
  readyState = 0
  onopen: ((event: Event) => void) | null = null
  onmessage: ((event: MessageEvent) => void) | null = null
  onerror: ((event: Event) => void) | null = null

  constructor(url: string) {
    this.url = url
    setTimeout(() => {
      this.readyState = 1
      if (this.onopen) {
        this.onopen(new Event('open'))
      }
    }, 0)
  }

  close() {
    this.readyState = 2
  }

  addEventListener(type: string, listener: EventListener) {
    // Mock addEventListener
  }

  removeEventListener(type: string, listener: EventListener) {
    // Mock removeEventListener
  }

  dispatchEvent(event: Event): boolean {
    return true
  }
}

/**
 * Mock Date.now() for consistent timestamps
 */
export function mockDateNow(timestamp = 1704067200000) {
  vi.spyOn(Date, 'now').mockReturnValue(timestamp)
}

/**
 * Mock setTimeout/setInterval
 */
export function mockTimers() {
  vi.useFakeTimers()
}

export function restoreTimers() {
  vi.useRealTimers()
}

/**
 * Mock console methods
 */
export function mockConsole() {
  vi.spyOn(console, 'log').mockImplementation(() => {})
  vi.spyOn(console, 'error').mockImplementation(() => {})
  vi.spyOn(console, 'warn').mockImplementation(() => {})
  vi.spyOn(console, 'info').mockImplementation(() => {})
}

/**
 * Restore all mocks
 */
export function restoreAllMocks() {
  vi.restoreAllMocks()
  mockNavigate.mockClear()
  mockEnqueueSnackbar.mockClear()
  mockCloseSnackbar.mockClear()
  mockAxios.get.mockClear()
  mockAxios.post.mockClear()
  mockAxios.put.mockClear()
  mockAxios.delete.mockClear()
  mockAxios.patch.mockClear()
}
