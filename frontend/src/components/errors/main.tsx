import { Button } from '../ui/button';

export const MainErrorFallback = () => {
  return (
    <div
      className="flex h-screen w-screen flex-col items-center justify-center gap-4"
      role="alert"
    >
      <h2 className="text-h6 font-semibold text-content-error">
        Ocurrió un error inesperado
      </h2>
      <Button onClick={() => window.location.assign(window.location.origin)}>
        Recargar
      </Button>
    </div>
  );
};
