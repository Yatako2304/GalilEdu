import * as React from 'react';
import { UseFormRegisterReturn } from 'react-hook-form';

import { Card } from '@/components/ui/card';
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
}: {
  pregunta: string;
  registration: UseFormRegisterReturn;
}) => (
  <fieldset className="flex flex-col gap-2">
    <legend className="mb-1 text-body-4 text-content-secondary">
      {pregunta}
    </legend>
    <div className="flex gap-6">
      {(['SI', 'NO'] as const).map((v) => (
        <label
          key={v}
          className="flex items-center gap-2 text-body-3 text-content-primary"
        >
          <input
            type="radio"
            value={v}
            className="size-4 accent-deep-blue-900"
            {...registration}
          />
          {v === 'SI' ? 'Sí' : 'No'}
        </label>
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
}) => (
  <label className="flex items-center gap-2 text-body-3 text-content-secondary">
    <input
      type="checkbox"
      className="size-4 accent-deep-blue-900"
      disabled={disabled}
      {...registration}
    />
    {etiqueta}
  </label>
);
