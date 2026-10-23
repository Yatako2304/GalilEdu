import { zodResolver } from '@hookform/resolvers/zod';
import { Pencil } from 'lucide-react';
import { useForm } from 'react-hook-form';

import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogTitle,
} from '@/components/ui/dialog';
import { Input } from '@/components/ui/form';
import { IconBadge } from '@/components/ui/icon-badge';
import { useNotifications } from '@/components/ui/notifications';
import { SeccionResumen } from '@/types/api';

import { useCambiarCapacidad } from '../../api/mutaciones-seccion';

import {
  CAPACIDAD_MAXIMA,
  CAPACIDAD_MINIMA,
  ValoresCapacidad,
  aSolicitudCapacidad,
  editarSeccionSchema,
} from './esquemas-seccion';

type DialogoEditarSeccionProps = {
  seccion: SeccionResumen;
  /** "4.° B": el grado y la sección, como en la lista. */
  etiqueta: string;
  onCerrar: () => void;
};

// "Modal - Editar sección" (HU-75). No está dibujado en Figma: sigue el modal "Nueva sección". El backend solo permite
// cambiar la capacidad; el nombre y el grado no se editan. Se monta solo mientras está abierto.
export const DialogoEditarSeccion = ({
  seccion,
  etiqueta,
  onCerrar,
}: DialogoEditarSeccionProps) => {
  const { addNotification } = useNotifications();
  const form = useForm<ValoresCapacidad>({
    resolver: zodResolver(editarSeccionSchema),
    mode: 'onChange',
    defaultValues: { capacidadMaxima: String(seccion.capacidadMaxima) },
  });

  // El error del backend (capacidad menor que los alumnos matriculados) sale por el interceptor.
  const cambiar = useCambiarCapacidad((guardada) => {
    addNotification({
      type: 'success',
      title: 'Sección actualizada',
      message: `${etiqueta} · Capacidad máxima ${guardada.capacidadMaxima}`,
    });
    onCerrar();
  });

  const enviar = form.handleSubmit((valores) =>
    cambiar.mutate({ id: seccion.id, data: aSolicitudCapacidad(valores) }),
  );

  return (
    <Dialog open onOpenChange={(open) => !open && onCerrar()}>
      <DialogContent className="flex max-h-[92vh] max-w-[520px] flex-col gap-0 overflow-hidden rounded-xl border-stroke-tertiary bg-white p-0">
        <form
          onSubmit={enviar}
          noValidate
          className="flex min-h-0 flex-1 flex-col"
        >
          <div className="flex-1 space-y-5 overflow-y-auto p-6">
            <div className="space-y-3">
              <IconBadge color="accent" size="lg" aria-hidden="true">
                <Pencil />
              </IconBadge>
              <div className="space-y-2">
                <DialogTitle className="text-h6 font-semibold text-content-primary">
                  Editar sección · {etiqueta}
                </DialogTitle>
                <DialogDescription className="text-body-3 text-content-secondary">
                  Solo se puede cambiar la capacidad. Hay{' '}
                  {seccion.alumnosOcupanVacante} alumnos que ocupan vacante.
                </DialogDescription>
              </div>
            </div>

            <Input
              label="Capacidad máxima *"
              hint={`Entre ${CAPACIDAD_MINIMA} y ${CAPACIDAD_MAXIMA} alumnos.`}
              inputMode="numeric"
              autoComplete="off"
              registration={form.register('capacidadMaxima')}
              error={form.formState.errors.capacidadMaxima}
            />
          </div>

          <div className="flex justify-end gap-3 border-t border-stroke-tertiary px-6 py-4">
            <Button type="button" variant="tertiary" onClick={onCerrar}>
              Cancelar
            </Button>
            <Button
              type="submit"
              isLoading={cambiar.isPending}
              disabled={cambiar.isPending}
            >
              Guardar
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
};
