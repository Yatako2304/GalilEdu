import { CirclePlus } from 'lucide-react';
import * as React from 'react';

import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Spinner } from '@/components/ui/spinner';
import { Table } from '@/components/ui/table';
import { Tag } from '@/components/ui/tag';
import { EstadoVersion, TablaConversionDetalle } from '@/types/api';
import { formatDay } from '@/utils/format';

import { useTablasConversion } from '../api/escala-calificacion';

import { BarraNiveles, InsigniaNivel } from './barra-niveles';
import { DialogoNuevaVersion } from './dialogo-nueva-version';

const ETIQUETA_ESTADO: Record<
  EstadoVersion,
  { texto: string; color: 'success' | 'info' | 'neutral' }
> = {
  VIGENTE: { texto: 'Vigente', color: 'success' },
  PROGRAMADA: { texto: 'Programada', color: 'info' },
  ANTERIOR: { texto: 'Anterior', color: 'neutral' },
};

/** "2025-03-01" → "01/03/2025" sin pasar por la zona horaria del navegador. */
const fecha = (iso: string) => formatDay(`${iso}T12:00:00`);

const rangosEnTexto = (t: TablaConversionDetalle) =>
  t.niveles.map((n) => `${n.nivel} ${n.notaDesde}–${n.notaHasta}`).join(' · ');

// Figma > Personal Administrativo > "CI-23 · Competencias › Escala de calificación — tabla de conversión vigente (HU-55)"
export const EscalaCalificacion = () => {
  const tablas = useTablasConversion();
  const [nuevaVersion, setNuevaVersion] = React.useState(false);

  if (tablas.isLoading) {
    return (
      <div className="flex h-60 items-center justify-center">
        <Spinner />
      </div>
    );
  }

  const versiones = tablas.data ?? [];
  const vigente = versiones.find((t) => t.estado === 'VIGENTE');
  const otras = versiones.filter((t) => t.estado !== 'VIGENTE');

  return (
    <>
      <Card className="p-5">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <div className="flex items-center gap-2">
              <h2 className="text-label-1 font-semibold text-content-primary">
                Tabla de conversión vigente
              </h2>
              {vigente && (
                <Tag color="success" size="sm">
                  Vigente
                </Tag>
              )}
            </div>
            <p className="text-body-4 text-content-tertiary">
              {vigente
                ? `Vigente desde el ${fecha(vigente.vigenteDesde)}. Se usa en los cursos con escala cualitativa.`
                : 'Aún no hay una tabla vigente. Registra la primera versión.'}
            </p>
          </div>
          <Button
            size="sm"
            icon={<CirclePlus className="size-4" />}
            onClick={() => setNuevaVersion(true)}
          >
            Nueva versión
          </Button>
        </div>

        {vigente && (
          <div className="mt-5 space-y-5">
            <BarraNiveles niveles={vigente.niveles} />
            <Table
              data={vigente.niveles.map((n) => ({ ...n, id: n.nivel }))}
              columns={[
                {
                  title: 'Nivel de logro',
                  field: 'nivel',
                  Cell: ({ entry }) => <InsigniaNivel nivel={entry.nivel} />,
                },
                { title: 'Desde', field: 'notaDesde' },
                { title: 'Hasta', field: 'notaHasta' },
                { title: 'Significado', field: 'significado' },
              ]}
            />
          </div>
        )}
      </Card>

      {otras.length > 0 && (
        <Card className="p-5">
          <h2 className="text-label-1 font-semibold text-content-primary">
            Otras versiones
          </h2>
          <p className="text-body-4 text-content-tertiary">
            Las programadas empiezan a regir en su fecha; las anteriores quedan
            como historial.
          </p>
          <ul className="mt-4 flex flex-col gap-2">
            {otras.map((t) => (
              <li
                key={t.id}
                className="flex flex-wrap items-center justify-between gap-2 rounded-xl border border-stroke-tertiary px-4 py-3"
              >
                <span className="text-body-3 text-content-primary">
                  Desde el {fecha(t.vigenteDesde)}
                  <span className="ml-3 text-body-4 text-content-tertiary">
                    {rangosEnTexto(t)}
                  </span>
                </span>
                <Tag color={ETIQUETA_ESTADO[t.estado].color} size="sm">
                  {ETIQUETA_ESTADO[t.estado].texto}
                </Tag>
              </li>
            ))}
          </ul>
        </Card>
      )}

      {nuevaVersion && (
        <DialogoNuevaVersion
          vigente={vigente}
          onCerrar={() => setNuevaVersion(false)}
        />
      )}
    </>
  );
};
