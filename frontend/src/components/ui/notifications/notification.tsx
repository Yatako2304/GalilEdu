import { Info, CircleAlert, CircleX, CircleCheck, X } from 'lucide-react';

// Figma > Componentes > Toast
const icons = {
  info: <Info className="size-5 text-turquoise-700" aria-hidden="true" />,
  success: (
    <CircleCheck className="size-5 text-growth-green-600" aria-hidden="true" />
  ),
  warning: <CircleAlert className="size-5 text-amber-600" aria-hidden="true" />,
  error: <CircleX className="size-5 text-red-600" aria-hidden="true" />,
};

export type NotificationProps = {
  notification: {
    id: string;
    type: keyof typeof icons;
    title: string;
    message?: string;
  };
  onDismiss: (id: string) => void;
};

export const Notification = ({
  notification: { id, type, title, message },
  onDismiss,
}: NotificationProps) => {
  return (
    <div className="flex w-full flex-col items-center space-y-4 sm:items-end">
      <div className="pointer-events-auto w-full max-w-sm overflow-hidden rounded-[12px] border border-stroke-tertiary bg-white shadow-lg">
        <div className="p-4" role="alert" aria-label={title}>
          <div className="flex items-start">
            <div className="shrink-0">{icons[type]}</div>
            <div className="ml-3 w-0 flex-1">
              <p className="text-label-2 font-semibold text-content-primary">
                {title}
              </p>
              {message && (
                <p className="mt-1 text-body-3 text-content-quartiary">
                  {message}
                </p>
              )}
            </div>
            <div className="ml-4 flex shrink-0">
              <button
                className="inline-flex rounded-md text-content-placeholder hover:text-content-tertiary focus:outline-none focus:ring-2 focus:ring-stroke-focus"
                onClick={() => {
                  onDismiss(id);
                }}
              >
                <span className="sr-only">Cerrar</span>
                <X className="size-4" aria-hidden="true" />
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
