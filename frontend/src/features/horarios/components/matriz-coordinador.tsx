import { ScheduleBlock } from '@/components/ui/schedule-block';

import { buscarCelda } from '../lib/borrador';
import { formatRango } from '../lib/formato';
import {
  CeldaDelHorario,
  DiaCoordinador,
  JornadaDelDia,
  NOMBRE_DEL_DIA,
} from '../types';

type MatrizCoordinadorProps = {
  jornada: JornadaDelDia;
  celdas: CeldaDelHorario[];
  /**
   * En edición las celdas se pueden abrir (las vacías muestran «+» y «Asignar»). En lectura las vacías son celdas
   * grises, sin «+», y nada es clicable.
   */
  editando: boolean;
  onElegirCelda: (dia: DiaCoordinador, numeroBloque: number) => void;
};

type Fila =
  | { tipo: 'bloque'; numero: number; inicio: string; fin: string }
  | { tipo: 'receso'; nombre: string; inicio: string; fin: string };

/** Los bloques en orden, con cada receso justo después del bloque que dice `despuesDeBloque`. */
const filasDeLaJornada = (jornada: JornadaDelDia): Fila[] => {
  const filas: Fila[] = [];
  let recesos = 0;
  [...jornada.bloques]
    .sort((a, b) => a.numero - b.numero)
    .forEach((bloque) => {
      filas.push({ tipo: 'bloque', ...bloque });
      jornada.recesos
        .filter((r) => r.despuesDeBloque === bloque.numero)
        .forEach((receso) => {
          recesos += 1;
          filas.push({
            tipo: 'receso',
            nombre: `Recreo ${recesos}`,
            inicio: receso.inicio,
            fin: receso.fin,
          });
        });
    });
  return filas;
};

const Hora = ({
  titulo,
  inicio,
  fin,
}: {
  titulo: string;
  inicio: string;
  fin: string;
}) => (
  <div role="rowheader" className="pr-2 pt-1">
    <p className="text-label-2 font-medium text-content-secondary">{titulo}</p>
    <p className="text-label-3 text-content-quartiary">
      {formatRango(inicio, fin)}
    </p>
  </div>
);

// Figma > HU-45 > "Matriz horaria": filas = bloques y recreos, columnas = días de la jornada (puede haber sábado).
// Es el mismo diseño que `MatrizHoraria` (P2), pero con los días y recesos del backend y con celdas que se pueden abrir;
// por eso es un componente aparte y no una variante de aquel.
export const MatrizCoordinador = ({
  jornada,
  celdas,
  editando,
  onElegirCelda,
}: MatrizCoordinadorProps) => {
  const dias = jornada.dias;

  return (
    <div className="overflow-x-auto">
      <div
        role="table"
        aria-label="Matriz horaria"
        className="grid min-w-[880px] gap-1"
        style={{
          gridTemplateColumns: `104px repeat(${dias.length}, minmax(150px, 1fr))`,
        }}
      >
        <div role="row" className="contents">
          {/* celda vacía: debe ocupar la 1.ª columna, por eso no es sr-only */}
          <div role="columnheader">
            <span className="sr-only">Bloque</span>
          </div>
          {dias.map((dia) => (
            <div
              key={dia}
              role="columnheader"
              className="pb-3 text-center text-label-1 font-medium text-content-secondary"
            >
              {NOMBRE_DEL_DIA[dia]}
            </div>
          ))}
        </div>

        {filasDeLaJornada(jornada).map((fila) =>
          fila.tipo === 'receso' ? (
            <div key={fila.nombre} role="row" className="contents">
              <Hora titulo={fila.nombre} inicio={fila.inicio} fin={fila.fin} />
              {dias.map((dia) => (
                <div
                  key={dia}
                  role="cell"
                  aria-label={`${NOMBRE_DEL_DIA[dia]}, ${fila.nombre}`}
                  className="self-start"
                >
                  <ScheduleBlock type="break" />
                </div>
              ))}
            </div>
          ) : (
            <div key={fila.numero} role="row" className="contents">
              <Hora
                titulo={`Bloque ${fila.numero}`}
                inicio={fila.inicio}
                fin={fila.fin}
              />
              {dias.map((dia) => {
                const celda = buscarCelda(celdas, dia, fila.numero);
                const abrir = editando
                  ? () => onElegirCelda(dia, fila.numero)
                  : undefined;
                return (
                  <div
                    key={dia}
                    role="cell"
                    aria-label={`${NOMBRE_DEL_DIA[dia]}, bloque ${fila.numero}`}
                    className="self-start"
                  >
                    {celda ? (
                      <ScheduleBlock
                        type="assigned"
                        courses={[
                          {
                            name: celda.curso,
                            teacher: celda.docente?.nombre,
                          },
                        ]}
                        room={celda.ambiente.codigo}
                        conflict={
                          celda.cruces.length > 0
                            ? celda.cruces.map((c) => c.mensaje).join(' ')
                            : undefined
                        }
                        onSelect={abrir}
                      />
                    ) : (
                      <ScheduleBlock type="empty" onAssign={abrir} />
                    )}
                  </div>
                );
              })}
            </div>
          ),
        )}
      </div>
    </div>
  );
};
