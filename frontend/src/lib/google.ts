// HU-3: ventana de inicio de sesión de Google (Google Identity Services, flujo de código en ventana emergente).
// El código que devuelve Google se envía al backend (POST /auth/google), que lo canjea y valida la cuenta.
// https://developers.google.com/identity/oauth2/web/guides/use-code-model

const SCRIPT_GOOGLE = 'https://accounts.google.com/gsi/client';

type RespuestaCodigo = { code?: string; error?: string };
type ErrorVentana = { type?: string };
type ClienteCodigo = { requestCode: () => void };

declare global {
  interface Window {
    google?: {
      accounts?: {
        oauth2?: {
          initCodeClient: (config: {
            client_id: string;
            scope: string;
            ux_mode: 'popup';
            callback: (respuesta: RespuestaCodigo) => void;
            error_callback?: (error: ErrorVentana) => void;
          }) => ClienteCodigo;
        };
      };
    };
  }
}

/** El usuario cerró la ventana de Google sin elegir cuenta: no es un error que haya que mostrar. */
export class GoogleCancelado extends Error {}

let carga: Promise<void> | null = null;

/** Carga una sola vez el script de Google. */
export const cargarGoogle = (): Promise<void> => {
  if (window.google?.accounts?.oauth2) return Promise.resolve();
  if (!carga) {
    carga = new Promise<void>((resolve, reject) => {
      const script = document.createElement('script');
      script.src = SCRIPT_GOOGLE;
      script.async = true;
      script.defer = true;
      script.onload = () => resolve();
      script.onerror = () => {
        carga = null;
        script.remove();
        reject(new Error('No se pudo cargar Google'));
      };
      document.head.appendChild(script);
    });
  }
  return carga;
};

/**
 * Abre la ventana de Google y devuelve el código de autorización. Debe llamarse directamente desde el clic del
 * botón (el navegador bloquea las ventanas emergentes que no vienen de un clic).
 */
export const pedirCodigoGoogle = (idCliente: string): Promise<string> =>
  new Promise((resolve, reject) => {
    const oauth2 = window.google?.accounts?.oauth2;
    if (!oauth2) {
      reject(new Error('Google no está cargado'));
      return;
    }
    oauth2
      .initCodeClient({
        client_id: idCliente,
        scope: 'openid email profile',
        ux_mode: 'popup',
        callback: (respuesta) => {
          if (respuesta.code) resolve(respuesta.code);
          else if (respuesta.error === 'access_denied')
            reject(new GoogleCancelado());
          else reject(new Error(respuesta.error ?? 'Sin código de Google'));
        },
        error_callback: (error) => {
          if (error.type === 'popup_closed') reject(new GoogleCancelado());
          else reject(new Error(error.type ?? 'Error de Google'));
        },
      })
      .requestCode();
  });
