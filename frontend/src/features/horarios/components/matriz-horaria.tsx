import { DiaSemana, HorarioSeccion } from '@/types/api';

import { BloqueHorario } from './bloque-horario';

const DIAS: { key: DiaSemana; label: string }[] = [
  { key: 'LUNES', label: 'Lunes' },
  { key: 'MARTES', label: 'Martes' },
  { key: 'MIERCOLES', label: 'Miércoles' },
  { key: 'JUEVES', label: 'Jueves' },
  { key: 'VIERNES', label: 'Viernes' },
];

/** "13:30" -> "1:30pm" (formato de Figma). */
export const formatHora = (hora: string) => {
  const [h, m] = hora.split(':').map(Number);
  const sufijo = h >= 12 ? 'pm' : 'am';
  const h12 = h % 12 === 0 ? 12 : h % 12;
  return `${h12}:${String(m).padStart(2, '0')}${sufijo}`;
};

type MatrizHorariaProps = {
  horario: Pick<HorarioSeccion, 'bloques' | 'sesiones'>;
};

// Figma: "Matriz horaria" (filas = bloques/recreos, columnas = días)
export const MatrizHoraria = ({ horario }: MatrizHorariaProps) => {
  const bloques = [...horario.bloques].sort((a, b) => a.orden - b.orden);

  return (
    <div className="overflow-x-auto">
      <div
        role="table"
        aria-label="Matriz horaria"
        className="grid min-w-[880px] grid-cols-[104px_repeat(5,minmax(150px,1fr))] gap-1"
      >
        <div role="row" className="contents">
          {/* celda vacía: debe ocupar la 1.ª columna, por eso no es sr-only */}
          <div role="columnheader">
            <span className="sr-only">Bloque</span>
          </div>
          {DIAS.map((dia) => (
            <div
              key={dia.key}
              role="columnheader"
              className="pb-3 text-center text-body-3 text-content-primary"
            >
              {dia.label}
            </div>
          ))}
        </div>

        {bloques.map((bloque) => (
          <div key={bloque.id} role="row" className="contents">
            <div role="rowheader" className="pr-2 pt-1">
              <p className="text-body-4 text-content-secondary">
                {bloque.nombre}
              </p>
              <p className="text-body-4 text-content-quartiary">
                {formatHora(bloque.horaInicio)} - {formatHora(bloque.horaFin)}
              </p>
            </div>
            {DIAS.map((dia) => (
              <div key={dia.key} role="cell" className="self-start">
                {bloque.tipo === 'RECREO' ? (
                  <BloqueHorario tipo="RECREO" />
                ) : (
                  <BloqueHorario
                    tipo="CLASE"
                    sesion={horario.sesiones.find(
                      (s) => s.bloqueId === bloque.id && s.dia === dia.key,
                    )}
                  />
                )}
              </div>
            ))}
          </div>
        ))}
      </div>
    </div>
  );
};
