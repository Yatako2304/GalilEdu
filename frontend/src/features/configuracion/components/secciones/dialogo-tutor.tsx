import { zodResolver } from '@hookform/resolvers/zod';
import { UserCog } from 'lucide-react';
import { useForm } from 'react-hook-form';

import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogTitle,
} from '@/components/ui/dialog';
import { Select } from '@/components/ui/form';
import { IconBadge } from '@/components/ui/icon-badge';
import { useNotifications } from '@/components/ui/notifications';
import { Spinner } from '@/components/ui/spinner';
import { DocenteElegible, SeccionResumen } from '@/types/api';

import { useAsignarTutores } from '../../api/mutaciones-seccion';
import { useDocentesElegibles } from '../../api/secciones';

import {
  SIN_COTUTOR,
  ValoresTutores,
  aSolicitudTutores,
  tutoresSchema,
  valoresInicialesTutores,
} from './esquemas-seccion';

type DialogoTutorProps = {
  anioId: string;
  seccion: SeccionResumen;
  /** "4.° B": el grado y la sección, como en la lista. */
  etiqueta: string;
  onCerrar: () => void;
};

// Figma > Personal Administrativo > "Modal - Asignar tutor · 4.° B" (HU-75). Se monta solo mientras está abierto.
// No se dibujan "Vigente desde" ni "Historial de tutoría": el MVP no guarda el historial de tutores (V9 queda fuera).
export const DialogoTutor = ({
  anioId,
  seccion,
  etiqueta,
  onCerrar,
}: DialogoTutorProps) => {
  const docentes = useDocentesElegibles(anioId);

  return (
    <Dialog open onOpenChange={(open) => !open && onCerrar()}>
      <DialogContent className="flex max-h-[92vh] max-w-[540px] flex-col gap-0 overflow-visible rounded-xl border-stroke-tertiary bg-white p-0 [@media(max-height:560px)]:overflow-hidden">
        {docentes.data ? (
          <FormularioTutor
            seccion={seccion}
            etiqueta={etiqueta}
            docentes={docentes.data}
            onCerrar={onCerrar}
          />
        ) : (
          <>
            <DialogTitle className="sr-only">
              Asignar tutor · {etiqueta}
            </DialogTitle>
            <DialogDescription className="sr-only">
              Cargando los docentes
            </DialogDescription>
            <div className="flex h-60 items-center justify-center">
              <Spinner />
            </div>
          </>
        )}
      </DialogContent>
    </Dialog>
  );
};

type FormularioTutorProps = Omit<DialogoTutorProps, 'anioId'> & {
  docentes: DocenteElegible[];
};

const FormularioTutor = ({
  seccion,
  etiqueta,
  docentes,
  onCerrar,
}: FormularioTutorProps) => {
  const { addNotification } = useNotifications();
  // Los docentes llegan antes de montar el formulario: así el <select> ya tiene sus opciones al recibir el valor inicial.
  const form = useForm<ValoresTutores>({
    resolver: zodResolver(tutoresSchema),
    defaultValues: valoresInicialesTutores(seccion),
  });
  const { register, watch, formState } = form;
  const tutorElegido = watch('tutorId');

  const asignar = useAsignarTutores((guardada) => {
    addNotification({
      type: 'success',
      title: 'Tutor asignado',
      message: `${etiqueta} · ${guardada.tutor ?? ''}`,
    });
    onCerrar();
  });

  const enviar = form.handleSubmit((valores) =>
    asignar.mutate({ id: seccion.id, data: aSolicitudTutores(valores) }),
  );

  // Un docente es tutor de una sola sección por año: quien ya lo es de otra sección no se puede elegir, salvo el tutor
  // de esta misma. El cotutor no tiene esa regla, pero no puede ser el tutor elegido.
  const opcionesTutor = docentes.map((d) => ({
    label: d.nombre,
    value: d.id,
    disabled: d.esTutorEsteAnio && d.id !== seccion.tutorId,
  }));
  const opcionesCotutor = [
    { label: 'Sin cotutor', value: SIN_COTUTOR },
    ...docentes.map((d) => ({
      label: d.nombre,
      value: d.id,
      disabled: d.id === tutorElegido,
    })),
  ];

  return (
    <form onSubmit={enviar} noValidate className="flex min-h-0 flex-1 flex-col">
      <div className="flex-1 space-y-5 p-8 [@media(max-height:560px)]:overflow-y-auto">
        <div className="space-y-3">
          <IconBadge color="accent" size="lg" aria-hidden="true">
            <UserCog />
          </IconBadge>
          <div className="space-y-2">
            <DialogTitle className="text-h6 font-semibold text-content-primary">
              Asignar tutor · {etiqueta}
            </DialogTitle>
            <DialogDescription className="text-body-3 text-content-tertiary">
              Un docente solo puede ser tutor de una sección por año. El cotutor
              es opcional.
            </DialogDescription>
          </div>
        </div>

        <Select
          label="Tutor *"
          placeholder="Elige un tutor"
          options={opcionesTutor}
          registration={register('tutorId')}
          error={formState.errors.tutorId}
        />
        <Select
          label="Cotutor (opcional)"
          options={opcionesCotutor}
          registration={register('cotutorId')}
          error={formState.errors.cotutorId}
        />
      </div>

      <div className="flex justify-end gap-3 border-t border-stroke-tertiary px-8 py-4">
        <Button type="button" variant="tertiary" onClick={onCerrar}>
          Cancelar
        </Button>
        <Button
          type="submit"
          isLoading={asignar.isPending}
          disabled={asignar.isPending}
        >
          Guardar
        </Button>
      </div>
    </form>
  );
};
