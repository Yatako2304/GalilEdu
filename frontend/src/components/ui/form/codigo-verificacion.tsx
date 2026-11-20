import * as React from 'react';

import { cn } from '@/utils/cn';

import { Error } from './error';

export type CodigoVerificacionProps = {
  /** Los dígitos escritos hasta ahora (solo números, máximo `longitud`). */
  value: string;
  onChange: (valor: string) => void;
  longitud?: number;
  /** Mensaje de error debajo de las casillas; las pinta de rojo. */
  error?: string | null;
  disabled?: boolean;
  /** Nombre del grupo para lectores de pantalla. */
  label?: string;
};

// Figma > Autenticación > Código de verificación (HU-4): una casilla por dígito. Avanza sola al escribir, retrocede con
// Backspace y acepta pegar el código completo.
export const CodigoVerificacion = ({
  value,
  onChange,
  longitud = 6,
  error,
  disabled,
  label = 'Código de verificación',
}: CodigoVerificacionProps) => {
  const casillas = React.useRef<(HTMLInputElement | null)[]>([]);
  const digitos = Array.from({ length: longitud }, (_, i) => value[i] ?? '');

  const enfocar = (indice: number) => {
    const destino =
      casillas.current[Math.max(0, Math.min(longitud - 1, indice))];
    destino?.focus();
    destino?.select();
  };

  const escribir = (indice: number, texto: string) => {
    const nuevos = texto.replace(/\D/g, '');
    if (!nuevos) return;
    const resultado = [...digitos];
    nuevos
      .slice(0, longitud - indice)
      .split('')
      .forEach((d, k) => {
        resultado[indice + k] = d;
      });
    onChange(resultado.join('').slice(0, longitud));
    enfocar(indice + nuevos.length);
  };

  const alPresionar = (
    indice: number,
    evento: React.KeyboardEvent<HTMLInputElement>,
  ) => {
    if (evento.key === 'Backspace') {
      evento.preventDefault();
      const resultado = [...digitos];
      if (resultado[indice]) {
        resultado[indice] = '';
      } else if (indice > 0) {
        resultado[indice - 1] = '';
        enfocar(indice - 1);
      }
      onChange(resultado.join(''));
    } else if (evento.key === 'ArrowLeft') {
      evento.preventDefault();
      enfocar(indice - 1);
    } else if (evento.key === 'ArrowRight') {
      evento.preventDefault();
      enfocar(indice + 1);
    }
  };

  return (
    <div className="flex flex-col items-center gap-2">
      <div role="group" aria-label={label} className="flex gap-2">
        {digitos.map((digito, indice) => (
          <input
            // Las casillas no cambian de orden: el índice es una clave estable.
            key={indice}
            ref={(el) => {
              casillas.current[indice] = el;
            }}
            type="text"
            inputMode="numeric"
            autoComplete={indice === 0 ? 'one-time-code' : 'off'}
            maxLength={longitud}
            value={digito}
            disabled={disabled}
            aria-label={`Dígito ${indice + 1} de ${longitud}`}
            aria-invalid={!!error}
            onFocus={(e) => e.target.select()}
            onChange={(e) => escribir(indice, e.target.value)}
            onKeyDown={(e) => alPresionar(indice, e)}
            onPaste={(e) => {
              e.preventDefault();
              escribir(indice, e.clipboardData.getData('text'));
            }}
            className={cn(
              'h-12 w-10 rounded-lg border bg-white text-center text-body-2 text-content-primary transition-colors',
              'hover:border-slate-500 focus-visible:border-stroke-focus focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-deep-blue-100',
              'disabled:cursor-not-allowed disabled:bg-slate-200',
              error ? 'border-stroke-error' : 'border-stroke-primary',
            )}
          />
        ))}
      </div>
      <Error errorMessage={error} />
    </div>
  );
};
