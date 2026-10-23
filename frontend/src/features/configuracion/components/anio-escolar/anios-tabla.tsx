import { EllipsisVertical, Eye } from 'lucide-react';
import { useNavigate } from 'react-router';

import { Card } from '@/components/ui/card';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown';
import { Table } from '@/components/ui/table';
import { paths } from '@/config/paths';
import { AnioEscolarResumen } from '@/types/api';

import { EstadoAnioTag } from './encabezado-anio';
import {
  aniosRegistrados,
  cantidadDePeriodos,
  nombreRegimen,
  rangoFechas,
} from './formato';

const AccionesFila = ({ anio }: { anio: AnioEscolarResumen }) => {
  const navigate = useNavigate();
  return (
    <DropdownMenu>
      <DropdownMenuTrigger className="inline-flex size-8 items-center justify-center rounded-lg text-content-tertiary hover:bg-surface-tertiary focus:outline-none focus-visible:ring-2 focus-visible:ring-stroke-focus">
        <EllipsisVertical className="size-4" aria-hidden="true" />
        <span className="sr-only">Acciones del año {anio.anio}</span>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-40">
        <DropdownMenuItem
          onClick={() =>
            navigate(
              paths.app.administrativo.anioEscolarDetalle.getHref(anio.id),
            )
          }
        >
          <Eye className="mr-2 size-4" aria-hidden="true" /> Ver detalle
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
};

// Figma > CI-01 · Año escolar > "Card - Años escolares": del año más reciente al más antiguo.
export const AniosTabla = ({ anios }: { anios: AnioEscolarResumen[] }) => {
  const ordenados = [...anios].sort((a, b) => b.anio - a.anio);

  return (
    <Card className="flex flex-col gap-4 p-6">
      <div>
        <h2 className="text-label-1 font-semibold text-content-primary">
          Años escolares
        </h2>
        <p className="text-body-4 text-content-tertiary">
          {aniosRegistrados(anios.length)}
        </p>
      </div>
      <Table
        data={ordenados}
        emptyMessage="Aún no hay años escolares registrados."
        columns={[
          {
            title: 'Año',
            field: 'anio',
            Cell: ({ entry }) => (
              <span className="text-label-2 font-semibold text-content-primary">
                {entry.anio}
              </span>
            ),
          },
          {
            title: 'Fechas',
            field: 'fechaInicio',
            Cell: ({ entry }) => (
              <span>{rangoFechas(entry.fechaInicio, entry.fechaFin)}</span>
            ),
          },
          {
            title: 'Régimen',
            field: 'regimen',
            Cell: ({ entry }) => <span>{nombreRegimen(entry.regimen)}</span>,
          },
          {
            title: 'Periodos',
            field: 'cantidadPeriodos',
            Cell: ({ entry }) => (
              <span>
                {cantidadDePeriodos(entry.regimen, entry.cantidadPeriodos)}
              </span>
            ),
          },
          { title: 'Secciones activas', field: 'seccionesActivas' },
          { title: 'Estructuras horarias', field: 'estructurasHorarias' },
          {
            title: 'Estado',
            field: 'estado',
            Cell: ({ entry }) => <EstadoAnioTag estado={entry.estado} />,
          },
          {
            title: '',
            field: 'id',
            Cell: ({ entry }) => <AccionesFila anio={entry} />,
          },
        ]}
      />
    </Card>
  );
};
