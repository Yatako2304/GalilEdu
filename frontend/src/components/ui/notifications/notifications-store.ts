import { nanoid } from 'nanoid';
import { create } from 'zustand';

export type Notification = {
  id: string;
  type: 'info' | 'warning' | 'success' | 'error';
  title: string;
  message?: string;
  /** Se está desvaneciendo; se quita de la lista al terminar la animación. */
  saliendo?: boolean;
};

/** Tiempo en pantalla antes de desvanecerse: los errores y avisos se dejan más para leerlos con calma. */
export const DURACION_NOTIFICACION: Record<Notification['type'], number> = {
  info: 5000,
  success: 5000,
  warning: 7000,
  error: 7000,
};

/** Lo que dura el desvanecido (`duration-300` en notification.tsx). */
export const DURACION_SALIDA = 300;

/** Como máximo se ven 3; si llega otra, la más antigua se va. */
const MAXIMO_VISIBLES = 3;

type NotificationsStore = {
  notifications: Notification[];
  addNotification: (notification: Omit<Notification, 'id'>) => void;
  dismissNotification: (id: string) => void;
};

export const useNotifications = create<NotificationsStore>((set, get) => ({
  notifications: [],
  addNotification: (notification) => {
    const nueva = { id: nanoid(), ...notification };
    const visibles = get().notifications.filter((n) => !n.saliendo);
    visibles
      .slice(0, Math.max(0, visibles.length - (MAXIMO_VISIBLES - 1)))
      .forEach((n) => get().dismissNotification(n.id));
    set((state) => ({ notifications: [...state.notifications, nueva] }));
    setTimeout(
      () => get().dismissNotification(nueva.id),
      DURACION_NOTIFICACION[nueva.type],
    );
  },
  // Primero se desvanece y luego se quita (también al cerrarla con la X).
  dismissNotification: (id) => {
    const actual = get().notifications.find((n) => n.id === id);
    if (!actual || actual.saliendo) return;
    set((state) => ({
      notifications: state.notifications.map((n) =>
        n.id === id ? { ...n, saliendo: true } : n,
      ),
    }));
    setTimeout(
      () =>
        set((state) => ({
          notifications: state.notifications.filter((n) => n.id !== id),
        })),
      DURACION_SALIDA,
    );
  },
}));
