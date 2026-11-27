import { zodResolver } from '@hookform/resolvers/zod';
import { Banknote } from 'lucide-react';
import * as React from 'react';
import { useForm } from 'react-hook-form';
import { z } from 'zod';

import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogTitle,
} from '@/components/ui/dialog';
import { Checkbox, Input, Select, Textarea } from '@/components/ui/form';
import { IconBadge } from '@/components/ui/icon-badge';
import { useNotifications } from '@/components/ui/notifications';
import { Spinner } from '@/components/ui/spinner';
import { CuotaPorPagar, MedioPagoCaja } from '@/types/api';

import {
  useCuotasPorPagarCompromiso,
  useRegistrarPagoCaja,
} from '../api/pago-en-caja';

import { soles } from './formato';
import { alternarCuota } from './seleccion-cuotas';

const MEDIOS: { value: MedioPagoCaja; label: string }[] = [
  { value: 'EFECTIVO', label: 'Efectivo' },
  { value: 'TRANSFERENCIA', label: 'Transferencia' },
  { value: 'DEPOSITO', label: 'Depósito' },
  { value: 'YAPE_PLIN', label: 'Yape/Plin' },
  { value: 'TARJETA_POS', label: 'Tarjeta (POS)' },
];

/** Hoy en la zona del navegador, como "2026-11-27". */
const hoy = () => new Date().toLocaleDateString('en-CA');

const esquema = z
  .object({
    fechaPago: z.string().min(1, 'Ingresa la fecha de pago.'),
    medioPago: z.enum([
      'EFECTIVO',
      'TRANSFERENCIA',
      'DEPOSITO',
      'YAPE_PLIN',
      'TARJETA_POS',
    ]),
    numeroOperacion: z
      .string()
      .trim()
      .max(30, 'El número de operación tiene como máximo 30 caracteres.')
      .regex(
        /^[A-Za-z0-9-]*$/,
        'El número de operación solo lleva letras, números y guiones.',
      ),
    observacion: z
      .string()
      .trim()
      .max(300, 'La observación tiene como máximo 300 caracteres.'),
  })
  .superRefine((valores, contexto) => {
    if (valores.fechaPago > hoy()) {
      contexto.addIssue({
        code: z.ZodIssueCode.custom,
        path: ['fechaPago'],
        message: 'La fecha de pago no puede ser posterior a hoy.',
      });
    }
    if (valores.medioPago !== 'EFECTIVO' && !valores.numeroOperacion) {
      contexto.addIssue({
        code: z.ZodIssueCode.custom,
        path: ['numeroOperacion'],
        message: 'Ingresa el número de operación.',
      });
    }
  });

type Valores = z.infer<typeof esquema>;

const detalleCuota = (cuota: CuotaPorPagar) => {
  if (cuota.estado === 'EN_PROCESO')
    return 'Tiene un pago en línea en proceso.';
  // El monto de la pensión va a la derecha de la fila; aquí solo el recargo, si lo hay.
  return cuota.recargo > 0
    ? `+ ${soles(cuota.recargo)} de recargo por mora`
    : undefined;
};

/**
 * Pago en caja (fila 29 del plan de pruebas): el apoderado pagó en el colegio y el Personal administrativo lo registra.
 * Se pagan cuotas completas; las pensiones en orden (al marcar una se marcan las anteriores) y con el recargo a la
 * fecha del pago. No hay frame en Figma: sigue el modal «Añadir descuento» de P8.
 */
export const DialogoRegistrarPago = ({
  abierto,
  onOpenChange,
  compromisoId,
  estudiante,
}: {
  abierto: boolean;
  onOpenChange: (abierto: boolean) => void;
  compromisoId: string;
  estudiante?: string;
}) => {
  const { addNotification } = useNotifications();
  const form = useForm<Valores>({
    resolver: zodResolver(esquema),
    defaultValues: {
      fechaPago: hoy(),
      medioPago: 'EFECTIVO',
      numeroOperacion: '',
      observacion: '',
    },
  });
  const fechaPago = form.watch('fechaPago');
  const medioPago = form.watch('medioPago');
  const fechaValida = !!fechaPago && fechaPago <= hoy();
  const cuotas = useCuotasPorPagarCompromiso(
    compromisoId,
    fechaPago,
    abierto && fechaValida,
  );
  const lista = React.useMemo(() => cuotas.data ?? [], [cuotas.data]);
  const [ids, setIds] = React.useState<Set<string>>(new Set());
  const [sinCuotas, setSinCuotas] = React.useState(false);

  // Si al cambiar la fecha una cuota deja de poder pagarse, sale de la selección.
  React.useEffect(() => {
    setIds((actual) => {
      const vigentes = new Set(
        lista.filter((c) => c.seleccionable).map((c) => c.cuotaId),
      );
      const filtrada = new Set([...actual].filter((id) => vigentes.has(id)));
      return filtrada.size === actual.size ? actual : filtrada;
    });
  }, [lista]);

  React.useEffect(() => {
    if (medioPago === 'EFECTIVO') form.setValue('numeroOperacion', '');
  }, [form, medioPago]);

  const elegidas = lista.filter((c) => ids.has(c.cuotaId));
  const capital = elegidas.reduce((total, c) => total + c.capital, 0);
  const recargo = elegidas.reduce((total, c) => total + c.recargo, 0);

  const cerrar = () => {
    form.reset({
      fechaPago: hoy(),
      medioPago: 'EFECTIVO',
      numeroOperacion: '',
      observacion: '',
    });
    setIds(new Set());
    setSinCuotas(false);
    onOpenChange(false);
  };

  const registrar = useRegistrarPagoCaja(compromisoId, (pago) => {
    addNotification({
      type: 'success',
      title: 'Pago registrado',
      message: `Constancia ${pago.numeroConstancia} por ${soles(pago.total)}.`,
    });
    cerrar();
  });

  const enviar = form.handleSubmit((valores) => {
    if (!elegidas.length) {
      setSinCuotas(true);
      return;
    }
    registrar.mutate({
      cuotaIds: elegidas.map((c) => c.cuotaId),
      fechaPago: valores.fechaPago,
      medioPago: valores.medioPago,
      numeroOperacion:
        valores.medioPago === 'EFECTIVO' ? null : valores.numeroOperacion,
      observacion: valores.observacion || null,
    });
  });

  const alternar = (cuota: CuotaPorPagar) => {
    setSinCuotas(false);
    setIds((actual) => alternarCuota(actual, cuota, lista));
  };

  return (
    <Dialog open={abierto} onOpenChange={(open) => !open && cerrar()}>
      <DialogContent className="flex max-h-[92vh] max-w-screen-sm flex-col gap-0 overflow-hidden rounded-xl border-stroke-tertiary bg-white p-0">
        <form onSubmit={enviar} noValidate className="flex min-h-0 flex-col">
          <div className="space-y-5 overflow-y-auto p-6">
            <div className="space-y-3">
              <IconBadge color="accent" size="lg" shape="square">
                <Banknote aria-hidden="true" />
              </IconBadge>
              <div className="space-y-1">
                <DialogTitle className="text-h6 font-semibold text-content-primary">
                  Registrar pago
                </DialogTitle>
                <DialogDescription className="text-body-3 text-content-secondary">
                  {estudiante ? `${estudiante}: m` : 'M'}arca las cuotas que se
                  pagaron en el colegio. Se emitirá su constancia de pago.
                </DialogDescription>
              </div>
            </div>

            <Input
              type="date"
              label="Fecha de pago *"
              max={hoy()}
              registration={form.register('fechaPago')}
              error={form.formState.errors.fechaPago}
            />

            <fieldset className="space-y-3">
              <legend className="text-label-1 font-semibold text-content-primary">
                Cuotas pagadas *
              </legend>
              <p className="text-body-4 text-content-tertiary">
                Las pensiones se pagan en orden: al marcar una se marcan las
                anteriores. El recargo por mora se calcula a la fecha de pago.
              </p>
              {cuotas.isLoading ? (
                <div className="flex h-24 items-center justify-center">
                  <Spinner />
                </div>
              ) : lista.length ? (
                <ul className="divide-y divide-stroke-tertiary rounded-lg border border-stroke-tertiary">
                  {lista.map((cuota) => (
                    <li
                      key={cuota.cuotaId}
                      className="flex items-start justify-between gap-4 px-4 py-3"
                    >
                      <Checkbox
                        label={cuota.nombre}
                        helperText={detalleCuota(cuota)}
                        checked={ids.has(cuota.cuotaId)}
                        disabled={!cuota.seleccionable}
                        onChange={() => alternar(cuota)}
                      />
                      <span className="shrink-0 text-body-3 font-medium text-content-primary">
                        {soles(cuota.capital)}
                      </span>
                    </li>
                  ))}
                </ul>
              ) : (
                <p className="text-body-3 text-content-tertiary">
                  El compromiso no tiene cuotas por pagar.
                </p>
              )}
              {sinCuotas && (
                <p className="text-label-3 text-content-error">
                  Selecciona al menos una cuota.
                </p>
              )}
            </fieldset>

            <div className="grid gap-4 sm:grid-cols-2">
              <Select
                label="Medio de pago *"
                options={MEDIOS}
                registration={form.register('medioPago')}
                error={form.formState.errors.medioPago}
              />
              <Input
                label={
                  medioPago === 'EFECTIVO'
                    ? 'N.º de operación'
                    : 'N.º de operación *'
                }
                disabled={medioPago === 'EFECTIVO'}
                maxLength={30}
                registration={form.register('numeroOperacion')}
                error={form.formState.errors.numeroOperacion}
              />
            </div>

            <Textarea
              label="Observación"
              maxLength={300}
              showCount
              registration={form.register('observacion')}
              error={form.formState.errors.observacion}
            />

            <div className="flex items-center justify-between rounded-lg bg-surface-secondary px-4 py-3">
              <p className="text-body-3 text-content-secondary">
                {elegidas.length
                  ? `${elegidas.length} cuota(s)${recargo > 0 ? ` · incluye ${soles(recargo)} de recargo` : ''}`
                  : 'Ninguna cuota seleccionada'}
              </p>
              <p className="text-h6 font-semibold text-content-primary">
                {soles(capital + recargo)}
              </p>
            </div>
          </div>

          <div className="flex justify-end gap-3 border-t border-stroke-tertiary px-6 py-4">
            <Button
              type="button"
              variant="tertiary"
              onClick={cerrar}
              disabled={registrar.isPending}
            >
              Cancelar
            </Button>
            <Button type="submit" isLoading={registrar.isPending}>
              Registrar pago
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
};
