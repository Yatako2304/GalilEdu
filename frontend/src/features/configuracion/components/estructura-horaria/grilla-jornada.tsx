import { Coffee } from 'lucide-react';

import {
  TableBody,
  TableCell,
  TableElement,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { DiaLectivo, FilaJornadaResumen } from '@/types/api';

import { NOMBRE_DIA_LARGO, ordenarDias, textoRango } from './formato';

type GrillaJornadaProps = {
  diasLectivos: DiaLectivo[];
  /** La jornada de un día, tal como la devuelve el detalle: igual cada día lectivo. */
  jornada: FilaJornadaResumen[];
};

// Figma > CI-12 > "Grilla semanal": una columna "Bloque" (con el horario de cada bloque) y una por día lectivo, con
// las celdas vacías hasta que se generen los horarios de cada sección (HU-45). Los recesos ocupan toda la fila.
export const GrillaJornada = ({
  diasLectivos,
  jornada,
}: GrillaJornadaProps) => {
  const dias = ordenarDias(diasLectivos);
  return (
    <TableElement aria-label="Jornada semanal" className="table-fixed">
      <TableHeader>
        <TableRow>
          <TableHead className="w-[150px]">Bloque</TableHead>
          {dias.map((dia) => (
            <TableHead key={dia}>{NOMBRE_DIA_LARGO[dia]}</TableHead>
          ))}
        </TableRow>
      </TableHeader>
      <TableBody>
        {jornada.map((fila) =>
          fila.tipo === 'BLOQUE' ? (
            <TableRow key={`bloque-${fila.numero}`}>
              <TableCell>
                <p className="text-label-2 text-content-tertiary">
                  Bloque {fila.numero}
                </p>
                <p className="text-label-3 text-content-tertiary">
                  {textoRango(fila.inicio, fila.fin)}
                </p>
              </TableCell>
              {dias.map((dia) => (
                <TableCell
                  key={dia}
                  aria-label={`Bloque ${fila.numero}, ${NOMBRE_DIA_LARGO[dia]}: sin asignar`}
                  className="border-l border-stroke-tertiary bg-surface-secondary"
                />
              ))}
            </TableRow>
          ) : (
            <TableRow
              key={`receso-${fila.inicio}`}
              className="bg-surface-warning-light hover:bg-surface-warning-light"
            >
              <TableCell className="py-2 text-body-4 text-content-warning">
                {textoRango(fila.inicio, fila.fin)}
              </TableCell>
              <TableCell
                colSpan={Math.max(dias.length, 1)}
                className="py-2 text-content-warning"
              >
                <p className="flex items-center justify-center gap-1.5 text-label-2 font-medium">
                  <Coffee className="size-4" aria-hidden="true" />
                  Receso · {fila.duracionMin} min
                </p>
              </TableCell>
            </TableRow>
          ),
        )}
      </TableBody>
    </TableElement>
  );
};
