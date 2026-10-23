import { act, renderHook } from '@testing-library/react';

import { useDebouncedValue } from '../use-debounced-value';

beforeEach(() => {
  vi.useFakeTimers();
});

afterEach(() => {
  vi.useRealTimers();
});

test('al inicio devuelve el valor de inmediato', () => {
  const { result } = renderHook(() => useDebouncedValue('a', 400));

  expect(result.current).toBe('a');
});

test('un cambio no se ve hasta que pasa la espera', () => {
  const { result, rerender } = renderHook(
    ({ valor }) => useDebouncedValue(valor, 400),
    { initialProps: { valor: 'a' } },
  );

  rerender({ valor: 'b' });
  act(() => {
    vi.advanceTimersByTime(399);
  });
  expect(result.current).toBe('a');

  act(() => {
    vi.advanceTimersByTime(1);
  });
  expect(result.current).toBe('b');
});

test('si el valor sigue cambiando, la espera se reinicia y solo llega el último', () => {
  const { result, rerender } = renderHook(
    ({ valor }) => useDebouncedValue(valor, 400),
    { initialProps: { valor: 'a' } },
  );

  rerender({ valor: 'b' });
  act(() => {
    vi.advanceTimersByTime(300);
  });
  rerender({ valor: 'c' });
  act(() => {
    vi.advanceTimersByTime(300);
  });
  // Pasaron 600 ms desde el primer cambio, pero solo 300 desde el último.
  expect(result.current).toBe('a');

  act(() => {
    vi.advanceTimersByTime(100);
  });
  expect(result.current).toBe('c');
});

test('la espera por defecto es de 400 ms', () => {
  const { result, rerender } = renderHook(
    ({ valor }) => useDebouncedValue(valor),
    { initialProps: { valor: 1 } },
  );

  rerender({ valor: 2 });
  act(() => {
    vi.advanceTimersByTime(399);
  });
  expect(result.current).toBe(1);
  act(() => {
    vi.advanceTimersByTime(1);
  });
  expect(result.current).toBe(2);
});
