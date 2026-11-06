import * as React from 'react';

import { CuotaPorPagar, CuotasPorPagar } from '@/types/api';

// HU-41 · Qué cuotas se pueden marcar para pagar. La tabla del cronograma (HU-40) usa este hook para sus casillas y
// la barra de selección (BarraPagoCuotas) para el total.

export type CuotaSeleccionable = CuotaPorPagar & { estudiante: string };

/**
 * Las pensiones se pagan en orden de antigüedad: al marcar una se marcan también las anteriores impagas del mismo
 * hijo (`requiere`), y al desmarcar una se desmarcan las que dependen de ella.
 */
export const alternarCuota = (
  seleccion: ReadonlySet<string>,
  cuota: Pick<CuotaPorPagar, 'cuotaId' | 'requiere' | 'seleccionable'>,
  cuotas: Pick<CuotaPorPagar, 'cuotaId' | 'requiere' | 'seleccionable'>[],
): Set<string> => {
  const nueva = new Set(seleccion);
  if (nueva.has(cuota.cuotaId)) {
    nueva.delete(cuota.cuotaId);
    cuotas
      .filter((c) => c.requiere.includes(cuota.cuotaId))
      .forEach((c) => nueva.delete(c.cuotaId));
    return nueva;
  }
  if (!cuota.seleccionable) return nueva;
  nueva.add(cuota.cuotaId);
  cuotas
    .filter((c) => c.seleccionable && cuota.requiere.includes(c.cuotaId))
    .forEach((c) => nueva.add(c.cuotaId));
  return nueva;
};

export const useSeleccionCuotas = (datos?: CuotasPorPagar) => {
  const cuotas = React.useMemo<CuotaSeleccionable[]>(
    () =>
      (datos?.estudiantes ?? []).flatMap((e) =>
        e.cuotas.map((c) => ({ ...c, estudiante: e.nombre })),
      ),
    [datos],
  );
  const [ids, setIds] = React.useState<Set<string>>(new Set());

  // Si una cuota deja de poder pagarse (por ejemplo, entró a otro pago), sale de la selección.
  React.useEffect(() => {
    setIds((actual) => {
      const vigentes = new Set(
        cuotas.filter((c) => c.seleccionable).map((c) => c.cuotaId),
      );
      const filtrada = new Set([...actual].filter((id) => vigentes.has(id)));
      return filtrada.size === actual.size ? actual : filtrada;
    });
  }, [cuotas]);

  const alternar = (cuotaId: string) => {
    const cuota = cuotas.find((c) => c.cuotaId === cuotaId);
    if (cuota) setIds((actual) => alternarCuota(actual, cuota, cuotas));
  };

  // "Seleccionar todas" de una tabla: marca las que se pueden pagar o desmarca todas las de la lista.
  const fijar = (cuotaIds: string[], marcar: boolean) =>
    setIds((actual) => {
      const nueva = new Set(actual);
      cuotas
        .filter(
          (c) => cuotaIds.includes(c.cuotaId) && (!marcar || c.seleccionable),
        )
        .forEach((c) =>
          marcar ? nueva.add(c.cuotaId) : nueva.delete(c.cuotaId),
        );
      return nueva;
    });

  const seleccionadas = cuotas.filter((c) => ids.has(c.cuotaId));
  return {
    cuotas,
    seleccionadas,
    estaSeleccionada: (cuotaId: string) => ids.has(cuotaId),
    alternar,
    fijar,
    limpiar: () => setIds(new Set()),
  };
};

/** "Pensión de agosto y Pensión de septiembre" */
export const unirNombres = (nombres: string[]) =>
  nombres.length <= 1
    ? (nombres[0] ?? '')
    : `${nombres.slice(0, -1).join(', ')} y ${nombres[nombres.length - 1]}`;
