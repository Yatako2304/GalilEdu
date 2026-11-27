import { zodResolver } from '@hookform/resolvers/zod';
import { LogOut } from 'lucide-react';
import { useForm } from 'react-hook-form';
import { z } from 'zod';

import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogTitle,
} from '@/components/ui/dialog';
import { Input, Textarea } from '@/components/ui/form';
import { IconBadge } from '@/components/ui/icon-badge';
import { useNotifications } from '@/components/ui/notifications';

import { useRetirarAlumno } from '../api/retiro';

/** Hoy en la zona del navegador, como "2026-11-27". */
const hoy = () => new Date().toLocaleDateString('en-CA');

const esquema = z.object({
  fechaRetiro: z
    .string()
    .min(1, 'Ingresa la fecha de retiro.')
    .refine(
      (fecha) => fecha <= hoy(),
      'La fecha de retiro no puede ser posterior a hoy.',
    ),
  motivo: z
    .string()
    .trim()
    .min(1, 'Ingresa el motivo del retiro.')
    .max(500, 'El motivo tiene como máximo 500 caracteres.'),
});

type Valores = z.infer<typeof esquema>;

/**
 * Retiro del alumno del colegio (fila 36 del plan de pruebas). Una sola acción, la misma desde Estudiantes, el
 * detalle de la matrícula y Pensiones: la matrícula pasa a Retirado (libera la vacante y sale de asistencia y del
 * registro de notas, que se siguen viendo en la libreta), se anulan las pensiones que vencen después y el estudiante
 * queda inactivo. Sigue el modal «Registrar retiro del alumno» de Pensiones en Figma, con fecha y motivo.
 */
export const DialogoRetiroAlumno = ({
  abierto,
  onOpenChange,
  estudianteId,
  estudiante,
}: {
  abierto: boolean;
  onOpenChange: (abierto: boolean) => void;
  estudianteId: string;
  estudiante: string;
}) => {
  const { addNotification } = useNotifications();
  const form = useForm<Valores>({
    resolver: zodResolver(esquema),
    defaultValues: { fechaRetiro: hoy(), motivo: '' },
  });

  const cerrar = () => {
    form.reset({ fechaRetiro: hoy(), motivo: '' });
    onOpenChange(false);
  };

  const retirar = useRetirarAlumno(estudianteId, (retiro) => {
    addNotification({
      type: 'success',
      title: 'Retiro registrado',
      message: `${estudiante} quedó retirado. Se anularon ${retiro.cuotasAnuladas} pensión(es) futura(s).`,
    });
    cerrar();
  });

  const enviar = form.handleSubmit((valores) =>
    retirar.mutate({
      fechaRetiro: valores.fechaRetiro,
      motivo: valores.motivo.trim(),
    }),
  );

  return (
    <Dialog open={abierto} onOpenChange={(open) => !open && cerrar()}>
      <DialogContent className="flex max-h-[92vh] max-w-lg flex-col gap-0 overflow-hidden rounded-xl border-stroke-tertiary bg-white p-0">
        <form onSubmit={enviar} noValidate className="flex min-h-0 flex-col">
          <div className="space-y-5 overflow-y-auto p-6">
            <div className="space-y-3">
              <IconBadge color="warning" size="lg" shape="square">
                <LogOut aria-hidden="true" />
              </IconBadge>
              <div className="space-y-1">
                <DialogTitle className="text-h6 font-semibold text-content-primary">
                  Registrar retiro del alumno
                </DialogTitle>
                <DialogDescription className="text-body-3 text-content-secondary">
                  {estudiante}: su matrícula quedará retirada y se liberará su
                  vacante. Se anularán las pensiones que vencen después de la
                  fecha de retiro; las vencidas se siguen debiendo. Sus notas
                  hasta el retiro se podrán ver, pero no modificar.
                </DialogDescription>
              </div>
            </div>

            <Input
              type="date"
              label="Fecha de retiro *"
              max={hoy()}
              registration={form.register('fechaRetiro')}
              error={form.formState.errors.fechaRetiro}
            />
            <Textarea
              label="Motivo del retiro *"
              maxLength={500}
              showCount
              registration={form.register('motivo')}
              error={form.formState.errors.motivo}
            />
          </div>

          <div className="flex justify-end gap-3 border-t border-stroke-tertiary px-6 py-4">
            <Button
              type="button"
              variant="tertiary"
              onClick={cerrar}
              disabled={retirar.isPending}
            >
              Cancelar
            </Button>
            <Button type="submit" isLoading={retirar.isPending}>
              Registrar retiro
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
};
