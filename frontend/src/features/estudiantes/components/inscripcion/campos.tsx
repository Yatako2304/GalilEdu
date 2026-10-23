import * as React from 'react';
import { UseFormRegisterReturn } from 'react-hook-form';

import { Card } from '@/components/ui/card';
import { Checkbox, Radio } from '@/components/ui/form';
import { cn } from '@/utils/cn';

// Piezas pequeñas reutilizadas en los pasos de la inscripción.

export const SeccionPaso = ({
  titulo,
  descripcion,
  children,
  accion,
}: {
  titulo: string;
  descripcion?: string;
  children: React.ReactNode;
  accion?: React.ReactNode;
}) => (
  <Card className="p-6">
    <div className="mb-5 flex flex-wrap items-start justify-between gap-3">
      <div>
        <h2 className="text-h6 font-semibold text-content-primary">{titulo}</h2>
        {descripcion && (
          <p className="mt-1 text-body-3 text-content-tertiary">
            {descripcion}
          </p>
        )}
      </div>
      {accion}
    </div>
    <div className="flex flex-col gap-6">{children}</div>
  </Card>
);

export const Subtitulo = ({ children }: { children: React.ReactNode }) => (
  <h3 className="text-label-3 uppercase tracking-wide text-content-tertiary">
    {children}
  </h3>
);

export const Grilla = ({
  children,
  columnas = 3,
}: {
  children: React.ReactNode;
  columnas?: 2 | 3 | 4;
}) => (
  <div
    className={cn(
      'grid grid-cols-1 gap-4',
      columnas === 2 && 'md:grid-cols-2',
      columnas === 3 && 'md:grid-cols-3',
      columnas === 4 && 'md:grid-cols-2 lg:grid-cols-4',
    )}
  >
    {children}
  </div>
);

/** Pregunta Sí / No con radios (Figma: "¿Tiene alergias?  ○ Sí ○ No"). */
export const SiNo = ({
  pregunta,
  registration,
  orden = ['SI', 'NO'],
}: {
  pregunta: string;
  registration: UseFormRegisterReturn;
  /** Orden de los radios; Figma lo cambia según la pantalla (ej. "No, Sí" en discapacidad). */
  orden?: readonly ['SI', 'NO'] | readonly ['NO', 'SI'];
}) => (
  <fieldset className="flex flex-col gap-2">
    <legend className="mb-1 text-body-4 text-content-secondary">
      {pregunta}
    </legend>
    <div className="flex gap-6">
      {orden.map((v) => (
        <Radio
          key={v}
          value={v}
          label={v === 'SI' ? 'Sí' : 'No'}
          {...registration}
        />
      ))}
    </div>
  </fieldset>
);

export const Casilla = ({
  etiqueta,
  registration,
  disabled,
}: {
  etiqueta: string;
  registration: UseFormRegisterReturn;
  disabled?: boolean;
}) => <Checkbox label={etiqueta} disabled={disabled} {...registration} />;
