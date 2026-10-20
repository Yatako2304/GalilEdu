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
import { Table } from '@/components/ui/table';
import { Tag } from '@/components/ui/tag';
import { paths } from '@/config/paths';
import { AlumnoEstadoCuenta } from '@/types/api';
import { cn } from '@/utils/cn';

import { useEstadoCuenta, useRegistrarRetiro } from '../api/estado-cuenta';

import { DialogoRetiro } from './dialogo-retiro';
import { anios, filtrarAlumnos, soles } from './formato';

const Indicador = ({
  icono,
  valor,
  etiqueta,
}: {
  icono: React.ReactNode;
  valor: string;
  etiqueta: string;
}) => (
  <Card className="flex flex-col gap-3 p-5">
    <span className="inline-flex size-9 items-center justify-center rounded-lg bg-surface-accent-light text-content-accent-primary">
      {icono}
    </span>
    <p className="text-h4 font-semibold text-content-primary">{valor}</p>
    <p className="text-body-3 text-content-tertiary">{etiqueta}</p>
  </Card>
);

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

  const pestanas = [
    { texto: `Todos los alumnos (${e.alumnos.length})`, activa: true },
    { texto: 'Compromisos vigentes', activa: false },
    { texto: 'Cuotas vencidas', activa: false },
  ];

  return (
    <div className="flex flex-col gap-6">
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Indicador
          icono={<Landmark className="size-4" aria-hidden="true" />}
          valor={soles(e.comprometido)}
          etiqueta={`Comprometido ${e.anio}`}
        />
        <Indicador
          icono={<CircleCheck className="size-4" aria-hidden="true" />}
          valor={soles(e.cobrado)}
          etiqueta={`Cobrado ${e.anio} · ${e.porcentajeCobrado} %`}
        />
        <Indicador
          icono={<TriangleAlert className="size-4" aria-hidden="true" />}
          valor={soles(e.deudaVencida)}
          etiqueta="Deuda vencida"
        />
        <Indicador
          icono={<CalendarX className="size-4" aria-hidden="true" />}
          valor={`${e.cuotasVencidas}`}
          etiqueta={`Cuotas vencidas · ${e.alumnosConDeuda} alumno(s)`}
        />
      </div>

      <Card className="flex flex-col gap-4 p-6">
        <div className="flex flex-wrap gap-2 border-b border-stroke-tertiary pb-4">
          {pestanas.map((p) => (
            <button
              key={p.texto}
              type="button"
              disabled={!p.activa}
              title={p.activa ? undefined : 'Disponible en la iteración 2'}
              className={cn(
                'rounded-lg px-4 py-2.5 text-label-2',
                p.activa
                  ? 'bg-surface-accent-primary text-white'
                  : 'cursor-not-allowed text-content-placeholder',
              )}
            >
              {p.texto}
            </button>
          ))}
        </div>

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
