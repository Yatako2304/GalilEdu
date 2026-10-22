import {
  CalendarX,
  CircleCheck,
  EllipsisVertical,
  Eye,
  Landmark,
  LogOut,
  Search,
  TriangleAlert,
} from 'lucide-react';
import * as React from 'react';
import { useNavigate } from 'react-router';

import { Card } from '@/components/ui/card';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown';
import { useNotifications } from '@/components/ui/notifications';
import { Spinner } from '@/components/ui/spinner';
import { StatCard } from '@/components/ui/stat-card';
import { Table } from '@/components/ui/table';
import { Tabs } from '@/components/ui/tabs';
import { Tag } from '@/components/ui/tag';
import { paths } from '@/config/paths';
import { AlumnoEstadoCuenta } from '@/types/api';

import { useEstadoCuenta, useRegistrarRetiro } from '../api/estado-cuenta';

import { DialogoRetiro } from './dialogo-retiro';
import { anios, filtrarAlumnos, soles } from './formato';

type Fila = AlumnoEstadoCuenta & { id: string };

const AccionesFila = ({
  alumno,
  onVer,
  onRetiro,
}: {
  alumno: Fila;
  onVer: () => void;
  onRetiro: () => void;
}) => (
  <DropdownMenu>
    <DropdownMenuTrigger className="inline-flex size-8 items-center justify-center rounded-lg text-content-tertiary hover:bg-surface-tertiary focus:outline-none focus-visible:ring-2 focus-visible:ring-stroke-focus">
      <EllipsisVertical className="size-4" aria-hidden="true" />
      <span className="sr-only">Acciones de {alumno.nombre}</span>
    </DropdownMenuTrigger>
    <DropdownMenuContent align="end" className="w-52">
      <DropdownMenuItem onClick={onVer}>
        <Eye className="mr-2 size-4" aria-hidden="true" /> Ver cronograma
      </DropdownMenuItem>
      {alumno.estadoCompromiso === 'VIGENTE' && (
        <DropdownMenuItem onClick={onRetiro}>
          <LogOut className="mr-2 size-4" aria-hidden="true" /> Registrar retiro
        </DropdownMenuItem>
      )}
    </DropdownMenuContent>
  </DropdownMenu>
);

// Figma > Personal Administrativo > "P2 · Estado de cuenta — Todos los alumnos" (HU-37)
export const EstadoCuenta = () => {
  const navigate = useNavigate();
  const { addNotification } = useNotifications();
  const estado = useEstadoCuenta();
  const [texto, setTexto] = React.useState('');
  const [retiro, setRetiro] = React.useState<Fila | null>(null);
  const registrarRetiro = useRegistrarRetiro((r) => {
    setRetiro(null);
    addNotification({
      type: 'success',
      title: `Retiro registrado: ${r.cuotasAnuladas} cuota(s) anulada(s)`,
    });
  });

  if (estado.isLoading) {
    return (
      <div className="flex h-48 items-center justify-center">
        <Spinner size="lg" />
      </div>
    );
  }
  const e = estado.data;
  if (!e) return null;

  const filas: Fila[] = filtrarAlumnos(e.alumnos, texto).map((a) => ({
    ...a,
    id: a.compromisoId,
  }));
  const verCronograma = (a: Fila) =>
    navigate(paths.app.administrativo.compromisoPago.getHref(a.estudianteId));

  // Las otras dos vistas llegan con HU-38 a HU-42; por ahora se muestran deshabilitadas.
  const pestanas = [
    { value: 'todos', label: `Todos los alumnos (${e.alumnos.length})` },
    {
      value: 'vigentes',
      label: 'Compromisos vigentes',
      disabled: true,
      title: 'Disponible pronto',
    },
    {
      value: 'vencidas',
      label: 'Cuotas vencidas',
      disabled: true,
      title: 'Disponible pronto',
    },
  ];

  return (
    <div className="flex flex-col gap-6">
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard
          icon={<Landmark aria-hidden="true" />}
          value={soles(e.comprometido)}
          label={`Comprometido ${e.anio}`}
        />
        <StatCard
          icon={<CircleCheck aria-hidden="true" />}
          value={soles(e.cobrado)}
          label={`Cobrado ${e.anio} · ${e.porcentajeCobrado} %`}
        />
        <StatCard
          icon={<TriangleAlert aria-hidden="true" />}
          value={soles(e.deudaVencida)}
          label="Deuda vencida"
        />
        <StatCard
          icon={<CalendarX aria-hidden="true" />}
          value={`${e.cuotasVencidas}`}
          label={`Cuotas vencidas · ${e.alumnosConDeuda} alumno(s)`}
        />
      </div>

      <Card className="flex flex-col gap-4 p-6">
        <Tabs
          variant="pill"
          size="md"
          aria-label="Vista del estado de cuenta"
          className="border-b border-stroke-tertiary pb-4"
          items={pestanas}
          value="todos"
          onValueChange={() => {}}
        />

        <div className="relative w-full max-w-xs">
          <Search
            className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-content-placeholder"
            aria-hidden="true"
          />
          <input
            type="search"
            value={texto}
            onChange={(ev) => setTexto(ev.target.value)}
            placeholder="Buscar alumno por nombre o DNI"
            aria-label="Buscar alumno por nombre o DNI"
            className="h-10 w-full rounded-lg border border-stroke-primary bg-white pl-9 pr-3 text-body-3 placeholder:text-content-placeholder focus-visible:border-stroke-focus focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-deep-blue-100"
          />
        </div>

        <Table<Fila>
          data={filas}
          emptyMessage="No hay compromisos de pago para este año"
          columns={[
            {
              title: 'Alumno',
              field: 'nombre',
              Cell: ({ entry }) => (
                <button
                  type="button"
                  onClick={() => verCronograma(entry)}
                  className="text-left"
                >
                  <span className="block text-content-primary hover:underline">
                    {entry.nombre}
                  </span>
                  <span className="block text-body-4 text-content-tertiary">
                    DNI {entry.numeroDocumento} · Apoderado:{' '}
                    {entry.apoderado ?? '—'}
                  </span>
                </button>
              ),
            },
            {
              title: 'Sección actual',
              field: 'seccion',
              Cell: ({ entry }) => <span>{entry.seccion ?? '—'}</span>,
            },
            {
              title: 'Antigüedad',
              field: 'antiguedad',
              Cell: ({ entry }) => <span>{anios(entry.antiguedad)}</span>,
            },
            {
              title: 'Deuda total',
              field: 'deudaVencida',
              Cell: ({ entry }) => (
                <span className="flex items-center justify-end gap-2">
                  {entry.estadoCompromiso === 'ANULADO' && (
                    <Tag size="sm" color="neutral">
                      Anulado
                    </Tag>
                  )}
                  {soles(entry.deudaVencida)}
                </span>
              ),
            },
            {
              title: '',
              field: 'id',
              Cell: ({ entry }) => (
                <AccionesFila
                  alumno={entry}
                  onVer={() => verCronograma(entry)}
                  onRetiro={() => setRetiro(entry)}
                />
              ),
            },
          ]}
        />
      </Card>

      {retiro && (
        <DialogoRetiro
          open
          onOpenChange={(open) => !open && setRetiro(null)}
          alumno={retiro.nombre}
          cargando={registrarRetiro.isPending}
          onConfirmar={() =>
            registrarRetiro.mutate({
              compromisoId: retiro.compromisoId,
              fechaRetiro: new Date().toLocaleDateString('en-CA'),
            })
          }
        />
      )}
    </div>
  );
};
