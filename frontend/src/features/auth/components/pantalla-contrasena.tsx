import { AxiosError } from 'axios';
import { Eye, EyeOff, X } from 'lucide-react';
import * as React from 'react';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/form';
import { Error } from '@/components/ui/form/error';
import { Link } from '@/components/ui/link';
import { REGLA_CONTRASENA, cumpleReglaContrasena } from '@/lib/contrasena';

// Piezas compartidas por las pantallas de contraseña de Figma > 🌟 Autenticación: Restablecer contraseña (HU-4) y
// Cambiar contraseña (HU-5). Tarjeta centrada sobre fondo blanco, con la X arriba a la izquierda.

export const mensajeDe = (error: unknown) => {
  if (error instanceof AxiosError) {
    if (!error.response) {
      return 'No se pudo conectar con el servidor. Inténtalo nuevamente.';
    }
    const mensaje = (error.response.data as { message?: string })?.message;
    if (mensaje) return mensaje;
  }
  return 'Ocurrió un error inesperado. Inténtalo nuevamente.';
};

export const comoError = (mensaje: string | null) =>
  mensaje ? { type: 'server', message: mensaje } : undefined;

/** Página completa con la X para salir y la tarjeta del paso centrada. */
export const PantallaContrasena = ({
  cerrarHref,
  cerrarEtiqueta,
  children,
}: {
  cerrarHref: string;
  cerrarEtiqueta: string;
  children: React.ReactNode;
}) => (
  <div className="relative flex min-h-screen items-center justify-center bg-white px-4 py-16">
    <Link
      to={cerrarHref}
      aria-label={cerrarEtiqueta}
      className="absolute left-6 top-6 text-content-secondary hover:text-content-primary"
    >
      <X className="size-5" aria-hidden="true" />
    </Link>
    {children}
  </div>
);

export const Tarjeta = ({
  titulo,
  descripcion,
  children,
}: {
  titulo: string;
  descripcion: string;
  children: React.ReactNode;
}) => (
  <div className="w-full max-w-[480px] rounded-xl border border-stroke-tertiary bg-white p-8 shadow-sm">
    <h1 className="text-center text-h6 font-semibold text-content-primary">
      {titulo}
    </h1>
    <p className="mt-6 text-center text-body-4 text-content-primary">
      {descripcion}
    </p>
    <div className="mt-6">{children}</div>
  </div>
);

/** Botón del ojo para mostrar u ocultar una contraseña (como en el login). */
export const VerContrasena = ({
  visible,
  onToggle,
  campo,
}: {
  visible: boolean;
  onToggle: () => void;
  campo: string;
}) => (
  <button
    type="button"
    onClick={onToggle}
    className="text-content-tertiary hover:text-content-primary"
  >
    {visible ? (
      <EyeOff className="size-4" aria-hidden="true" />
    ) : (
      <Eye className="size-4" aria-hidden="true" />
    )}
    <span className="sr-only">
      {visible ? `Ocultar ${campo}` : `Mostrar ${campo}`}
    </span>
  </button>
);

/** Campo de contraseña con el ojo de Figma. */
export const CampoContrasena = ({
  label,
  value,
  onChange,
  error,
  hint,
  autoComplete,
}: {
  label: string;
  value: string;
  onChange: (valor: string) => void;
  error?: string | null;
  hint?: string;
  autoComplete: string;
}) => {
  const [visible, setVisible] = React.useState(false);
  return (
    <Input
      type={visible ? 'text' : 'password'}
      label={label}
      autoComplete={autoComplete}
      value={value}
      onChange={(e) => onChange(e.target.value)}
      hint={hint}
      error={comoError(error ?? null)}
      endAdornment={
        <VerContrasena
          visible={visible}
          onToggle={() => setVisible((v) => !v)}
          campo={label.toLowerCase()}
        />
      }
    />
  );
};

/**
 * Paso «Nueva contraseña» (igual en HU-4 y HU-5): la regla de Figma pasa de ayuda a error si no se cumple, y la
 * repetición debe coincidir. `error` es el mensaje del servidor; `debajo` va bajo ese mensaje (por ejemplo, un enlace
 * para volver al paso anterior).
 */
export const NuevaContrasenaForm = ({
  textoBoton,
  cargando,
  error,
  onCambio,
  onGuardar,
  debajo,
}: {
  textoBoton: string;
  cargando: boolean;
  error: string | null;
  /** Se llama al escribir, para borrar el error del servidor. */
  onCambio: () => void;
  onGuardar: (contrasena: string) => void;
  debajo?: React.ReactNode;
}) => {
  const [contrasena, setContrasena] = React.useState('');
  const [repetir, setRepetir] = React.useState('');
  const invalida = contrasena !== '' && !cumpleReglaContrasena(contrasena);
  const noCoinciden = repetir !== '' && repetir !== contrasena;
  const lista = cumpleReglaContrasena(contrasena) && repetir === contrasena;

  return (
    <form
      onSubmit={(evento) => {
        evento.preventDefault();
        onGuardar(contrasena);
      }}
      className="space-y-6"
    >
      <CampoContrasena
        label="Contraseña"
        autoComplete="new-password"
        value={contrasena}
        onChange={(valor) => {
          setContrasena(valor);
          onCambio();
        }}
        hint={REGLA_CONTRASENA}
        error={invalida ? REGLA_CONTRASENA : null}
      />
      <CampoContrasena
        label="Repetir contraseña"
        autoComplete="new-password"
        value={repetir}
        onChange={(valor) => {
          setRepetir(valor);
          onCambio();
        }}
        error={noCoinciden ? 'Las contraseñas no coinciden.' : null}
      />
      <div className="space-y-2">
        <Button
          type="submit"
          variant="secondary"
          className="w-full"
          isLoading={cargando}
          disabled={!lista || cargando}
        >
          {textoBoton}
        </Button>
        <Error errorMessage={error} />
        {error && debajo}
      </div>
    </form>
  );
};
