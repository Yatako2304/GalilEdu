import { Check, ChevronDown, WalletCards } from 'lucide-react';
import * as React from 'react';

import { Avatar } from '@/components/ui/avatar';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown';
import { Checkbox, Select } from '@/components/ui/form';
import { Spinner } from '@/components/ui/spinner';
import {
  TableBody,
  TableCell,
  TableElement,
  TableFooter,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { Tabs } from '@/components/ui/tabs';
import { Tag } from '@/components/ui/tag';
import { CompromisoDetalle, HijoConDeuda } from '@/types/api';
import { formatDay } from '@/utils/format';

import {
  useCompromisoApoderado,
  useCompromisosApoderado,
  useHijosConDeuda,
  usePagosApoderado,
} from '../api/pensiones-apoderado';

import { ResumenCompromiso } from './cronograma-cuotas';
import { etiquetaCuota, soles } from './formato';
import { TablaPagos } from './tabla-pagos';

type Pestana = 'cronograma' | 'pagos';

const nombreCompleto = (hijo: HijoConDeuda) =>
  `${hijo.nombres} ${hijo.apellidos}`.trim();

const detalleAcademico = (hijo: HijoConDeuda) =>
  [hijo.grado ?? 'Grado por asignar', hijo.seccion].filter(Boolean).join(' · ');

const SelectorEstudiante = ({
  hijos,
  elegido,
  onElegir,
}: {
  hijos: HijoConDeuda[];
  elegido?: HijoConDeuda;
  onElegir: (id: string) => void;
}) => (
  <Card className="flex min-h-[76px] items-center px-4 py-1">
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <button
          type="button"
          className="flex min-w-0 items-center gap-3 rounded-xl text-left outline-none focus-visible:ring-2 focus-visible:ring-stroke-focus"
          aria-label="Seleccionar estudiante"
        >
          <Avatar name={elegido ? nombreCompleto(elegido) : ''} />
          <span className="min-w-0">
            <span className="block truncate text-label-2 font-medium text-content-primary">
              {elegido ? nombreCompleto(elegido) : 'Selecciona un estudiante'}
            </span>
            <span className="block truncate text-body-4 text-content-tertiary">
              {elegido ? detalleAcademico(elegido) : 'Sin datos académicos'}
            </span>
          </span>
          <ChevronDown
            className="ml-3 size-4 shrink-0 text-content-tertiary"
            aria-hidden="true"
          />
        </button>
      </DropdownMenuTrigger>
      <DropdownMenuContent
        align="start"
        className="w-[min(360px,calc(100vw-32px))] rounded-xl border-stroke-tertiary bg-white p-2"
      >
        <DropdownMenuLabel className="px-3 py-2 text-caption-1 font-medium uppercase tracking-wide text-content-quartiary">
          Estudiantes a tu cargo ({hijos.length})
        </DropdownMenuLabel>
        <DropdownMenuSeparator className="bg-stroke-tertiary" />
        {hijos.map((hijo) => {
          const seleccionado = hijo.estudianteId === elegido?.estudianteId;
          return (
            <DropdownMenuItem
              key={hijo.estudianteId}
              onSelect={() => onElegir(hijo.estudianteId)}
              className="gap-3 rounded-xl p-3 focus:bg-surface-secondary"
            >
              <Avatar name={nombreCompleto(hijo)} size="sm" />
              <span className="min-w-0 flex-1">
                <span className="block truncate text-body-3 font-medium text-content-primary">
                  {nombreCompleto(hijo)}
                </span>
                <span className="block truncate text-body-4 text-content-quartiary">
                  {detalleAcademico(hijo)}
                </span>
              </span>
              {seleccionado && (
                <Check
                  className="size-4 text-content-accent-primary"
                  aria-label="Seleccionado"
                />
              )}
            </DropdownMenuItem>
          );
        })}
      </DropdownMenuContent>
    </DropdownMenu>
  </Card>
);

const TablaCuotasApoderado = ({
  compromiso,
}: {
  compromiso: CompromisoDetalle;
}) => (
  <TableElement className="whitespace-nowrap">
    <TableHeader>
      <TableRow>
        <TableHead className="w-12">
          <Checkbox disabled aria-label="Seleccionar todas las cuotas" />
        </TableHead>
        <TableHead>Cuota</TableHead>
        <TableHead>Vencimiento</TableHead>
        <TableHead className="text-right">Monto</TableHead>
        <TableHead>Estado</TableHead>
        <TableHead className="text-right">Recargo</TableHead>
      </TableRow>
    </TableHeader>
    <TableBody>
      {compromiso.cuotas.map((cuota) => {
        const etiqueta = etiquetaCuota(cuota);
        return (
          <TableRow key={cuota.id}>
            <TableCell>
              <Checkbox disabled aria-label={`Seleccionar ${cuota.nombre}`} />
            </TableCell>
            <TableCell>
              <p className="text-content-primary">{cuota.nombre}</p>
              <p className="text-body-4 text-content-tertiary">
                {cuota.numero
                  ? `Cuota ${cuota.numero} de ${compromiso.resumen.pensionesTotal}`
                  : 'Cuota de matrícula'}
              </p>
            </TableCell>
            <TableCell>{formatDay(cuota.fechaVencimiento)}</TableCell>
            <TableCell className="text-right font-semibold text-content-primary">
              {soles(cuota.montoFinal)}
            </TableCell>
            <TableCell>
              <span className="flex flex-wrap items-center gap-2">
                <Tag size="sm" color={etiqueta.color}>
                  {etiqueta.texto}
                </Tag>
                {cuota.vencida && (
                  <span className="text-body-4 text-content-tertiary">
                    Vencida hace {cuota.diasAtraso} días
                  </span>
                )}
                {cuota.fechaPago && (
                  <span className="text-body-4 text-content-tertiary">
                    {formatDay(cuota.fechaPago)}
                  </span>
                )}
              </span>
            </TableCell>
            <TableCell className="text-right text-content-error">
              {cuota.recargo > 0 ? `+ ${soles(cuota.recargo)}` : '—'}
            </TableCell>
          </TableRow>
        );
      })}
    </TableBody>
    <TableFooter>
      <TableRow>
        <TableCell />
        <TableCell colSpan={2} className="font-semibold text-content-primary">
          Total anual comprometido
        </TableCell>
        <TableCell className="text-right font-semibold text-content-primary">
          {soles(compromiso.resumen.totalAnual)}
        </TableCell>
        <TableCell />
        <TableCell className="text-right font-semibold text-content-primary">
          {soles(
            compromiso.cuotas.reduce(
              (total, cuota) => total + cuota.recargo,
              0,
            ),
          )}
        </TableCell>
      </TableRow>
    </TableFooter>
  </TableElement>
);

// Figma > Apoderado > Pensiones (HU-40). Las acciones de pago permanecen deshabilitadas hasta HU-41.
export const PensionesApoderado = () => {
  const hijos = useHijosConDeuda();
  const [estudianteId, setEstudianteId] = React.useState('');
  const [compromisoId, setCompromisoId] = React.useState('');
  const [pestana, setPestana] = React.useState<Pestana>('cronograma');
  const compromisos = useCompromisosApoderado(estudianteId);
  const compromiso = useCompromisoApoderado(compromisoId);
  const pagos = usePagosApoderado(
    pestana === 'pagos' ? compromisoId : undefined,
  );

  React.useEffect(() => {
    if (!estudianteId && hijos.data?.length)
      setEstudianteId(hijos.data[0].estudianteId);
  }, [estudianteId, hijos.data]);

  React.useEffect(() => {
    const primero = compromisos.data?.[0]?.id ?? '';
    setCompromisoId((actual) =>
      compromisos.data?.some((item) => item.id === actual) ? actual : primero,
    );
  }, [compromisos.data]);

  const elegirEstudiante = (id: string) => {
    setEstudianteId(id);
    setCompromisoId('');
    setPestana('cronograma');
  };

  if (hijos.isLoading) {
    return (
      <div className="flex h-60 items-center justify-center">
        <Spinner size="lg" />
      </div>
    );
  }

  if (!hijos.data?.length) {
    return (
      <Card className="p-8 text-center text-body-3 text-content-tertiary">
        No hay estudiantes asociados a tu cuenta.
      </Card>
    );
  }

  const elegido = hijos.data.find((hijo) => hijo.estudianteId === estudianteId);

  return (
    <div className="flex flex-col gap-5">
      <div className="flex justify-end">
        <Select
          aria-label="Compromiso"
          options={(compromisos.data ?? []).map((item) => ({
            label: `Compromiso ${item.anio}`,
            value: item.id,
          }))}
          placeholder={compromisos.isLoading ? 'Cargando…' : 'Sin compromisos'}
          value={compromisoId}
          disabled={compromisos.isLoading || !compromisos.data?.length}
          onChange={(evento) => setCompromisoId(evento.target.value)}
          wrapperClassName="w-52"
        />
      </div>

      <SelectorEstudiante
        hijos={hijos.data}
        elegido={elegido}
        onElegir={elegirEstudiante}
      />

      {!compromisos.isLoading && !compromisos.data?.length ? (
        <Card className="p-8 text-center text-body-3 text-content-tertiary">
          Aún no tiene compromiso de pago.
        </Card>
      ) : compromiso.isLoading || !compromiso.data ? (
        <div className="flex h-60 items-center justify-center">
          <Spinner size="lg" />
        </div>
      ) : (
        <>
          <ResumenCompromiso compromiso={compromiso.data} />

          <Card className="flex flex-col gap-4 p-6">
            <Tabs
              variant="pill"
              size="md"
              aria-label="Detalle de pensiones"
              className="border-b border-stroke-tertiary pb-4"
              items={[
                { value: 'cronograma', label: 'Cronograma de cuotas' },
                { value: 'pagos', label: 'Pagos' },
              ]}
              value={pestana}
              onValueChange={setPestana}
            />

            {pestana === 'cronograma' ? (
              <>
                <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-stroke-focus bg-surface-accent-light px-4 py-3">
                  <div>
                    <p className="text-label-2 font-medium text-content-primary">
                      Selecciona las cuotas que deseas pagar
                    </p>
                    <p className="text-body-4 text-content-tertiary">
                      El pago en línea estará disponible próximamente.
                    </p>
                  </div>
                  <Button
                    disabled
                    icon={<WalletCards className="size-4" />}
                    title="Disponible en HU-41"
                  >
                    Pagar seleccionadas
                  </Button>
                </div>
                <TablaCuotasApoderado compromiso={compromiso.data} />
              </>
            ) : (
              <TablaPagos pagos={pagos.data} isLoading={pagos.isLoading} />
            )}
          </Card>
        </>
      )}
    </div>
  );
};
