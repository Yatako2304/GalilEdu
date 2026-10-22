import { Alert } from '../alert';

export type NotificationProps = {
  notification: {
    id: string;
    type: 'info' | 'success' | 'warning' | 'error';
    title: string;
    message?: string;
  };
  onDismiss: (id: string) => void;
};

// Figma > Componentes > Toast: el mismo Alert con la X para cerrarlo.
export const Notification = ({
  notification: { id, type, title, message },
  onDismiss,
}: NotificationProps) => (
  <div className="flex w-full flex-col items-center space-y-4 sm:items-end">
    <Alert
      type={type}
      title={title}
      aria-label={title}
      onClose={() => onDismiss(id)}
      className="pointer-events-auto w-full max-w-[497px] shadow-card"
    >
      {message}
    </Alert>
  </div>
);
