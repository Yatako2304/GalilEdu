import { renderHook, act } from '@testing-library/react';

import {
  DURACION_NOTIFICACION,
  DURACION_SALIDA,
  useNotifications,
} from '../notifications-store';

beforeEach(() => {
  vi.useFakeTimers();
  useNotifications.setState({ notifications: [] });
});

afterEach(() => {
  vi.useRealTimers();
});

const titulos = () =>
  useNotifications.getState().notifications.map((n) => n.title);

test('se cierra con la X: primero se desvanece y luego se quita', () => {
  const { result } = renderHook(() => useNotifications());
  act(() => {
    result.current.addNotification({ type: 'info', title: 'Hola' });
  });
  const { id } = result.current.notifications[0];

  act(() => {
    result.current.dismissNotification(id);
  });
  expect(result.current.notifications[0].saliendo).toBe(true);

  act(() => {
    vi.advanceTimersByTime(DURACION_SALIDA);
  });
  expect(result.current.notifications).toHaveLength(0);
});

test('desaparece sola: 5 s el éxito y 7 s el error', () => {
  const { result } = renderHook(() => useNotifications());
  act(() => {
    result.current.addNotification({ type: 'success', title: 'Guardado' });
    result.current.addNotification({ type: 'error', title: 'Falló' });
  });

  act(() => {
    vi.advanceTimersByTime(DURACION_NOTIFICACION.success + DURACION_SALIDA);
  });
  expect(titulos()).toEqual(['Falló']);

  act(() => {
    vi.advanceTimersByTime(
      DURACION_NOTIFICACION.error - DURACION_NOTIFICACION.success,
    );
  });
  expect(result.current.notifications).toHaveLength(0);
});

test('no se acumulan: con una cuarta, la más antigua se va', () => {
  const { result } = renderHook(() => useNotifications());
  act(() => {
    ['1', '2', '3', '4'].forEach((title) =>
      result.current.addNotification({ type: 'info', title }),
    );
    vi.advanceTimersByTime(DURACION_SALIDA);
  });
  expect(titulos()).toEqual(['2', '3', '4']);
});
