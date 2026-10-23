import { cn } from '@/utils/cn';

import { Alert } from '../alert';

import type { Notification as DatosNotificacion } from './notifications-store';

export type NotificationProps = {
  notification: DatosNotificacion;
  onDismiss: (id: string) => void;
};

// Figma > Componentes > Toast: el mismo Alert con la X para cerrarlo. Aparece y se desvanece solo.
export const Notification = ({
  notification: { id, type, title, message, saliendo },
  onDismiss,
}: NotificationProps) => (
  <div className="flex w-full flex-col items-center space-y-4 sm:items-end">
    <Alert
      type={type}
      title={title}
      aria-label={title}
      onClose={() => onDismiss(id)}
      className={cn(
        'pointer-events-auto w-full max-w-[497px] shadow-card duration-300',
        saliendo
          ? 'animate-out fade-out fill-mode-forwards'
          : 'animate-in fade-in slide-in-from-top-2',
      )}
    >
      {message}
    </Alert>
  </div>
);
