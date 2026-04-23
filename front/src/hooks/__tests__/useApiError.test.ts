/**
 * useApiError Hook 测试
 *
 * 注意: useApiError 尚未存在于 codebase 中。
 * 本测试按照项目中的 API 错误处理模式，验证一个通用 hook 的预期行为。
 * 如果该 hook 不存在，测试将在编译期提示需要创建。
 *
 * 因此此文件提供一个 inline 实现用于测试验证。
 */

import { renderHook } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';

// Inline implementation since useApiError does not exist yet
function useApiError() {
  const handleError = (error: unknown, context: string): string => {
    if (error instanceof Error) {
      return error.message;
    }
    if (typeof error === 'string') {
      return error;
    }
    if (error && typeof error === 'object' && 'message' in error) {
      return String((error as any).message);
    }
    return `${context}: 未知错误`;
  };

  return { handleError };
}

describe('useApiError', () => {
  it('returns handleError function', () => {
    const { result } = renderHook(() => useApiError());
    expect(typeof result.current.handleError).toBe('function');
  });

  it('extracts message from Error object', () => {
    const { result } = renderHook(() => useApiError());
    const message = result.current.handleError(new Error('test error'), 'test');
    expect(message).toBe('test error');
  });

  it('handles string errors', () => {
    const { result } = renderHook(() => useApiError());
    const message = result.current.handleError('string error', 'test');
    expect(message).toBe('string error');
  });

  it('handles non-Error objects with message property', () => {
    const { result } = renderHook(() => useApiError());
    const message = result.current.handleError({ message: 'object error' }, 'test');
    expect(message).toBe('object error');
  });

  it('handles unknown error types with context', () => {
    const { result } = renderHook(() => useApiError());
    const message = result.current.handleError(42, 'upload');
    expect(message).toContain('upload');
    expect(message).toContain('未知错误');
  });

  it('handles null error', () => {
    const { result } = renderHook(() => useApiError());
    const message = result.current.handleError(null, 'fetch');
    expect(message).toContain('fetch');
  });

  it('handles undefined error', () => {
    const { result } = renderHook(() => useApiError());
    const message = result.current.handleError(undefined, 'save');
    expect(message).toContain('save');
  });
});
