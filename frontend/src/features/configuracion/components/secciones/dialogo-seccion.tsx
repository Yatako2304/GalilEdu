import { zodResolver } from '@hookform/resolvers/zod';
import { Users } from 'lucide-react';
import { useForm } from 'react-hook-form';

import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogTitle,
} from '@/components/ui/dialog';
import { Input, Select } from '@/components/ui/form';
import { IconBadge } from '@/components/ui/icon-badge';
import { useNotifications } from '@/components/ui/notifications';
import { GradoConSecciones } from '@/types/api';

import { useAnioEscolar } from '../../api/anios-escolares';
import { useCrearSeccion } from '../../api/mutaciones-seccion';

import {
  CAPACIDAD_MAXIMA,
  CAPACIDAD_MINIMA,
  ValoresSeccion,
  aSolicitudSeccion,
  normalizarNombreSeccion,
  nuevaSeccionSchema,
  valoresInicialesSeccion,
} from './esquemas-seccion';

type DialogoSeccionProps = {
  abierto: boolean;
  onCerrar: () => void;
  anioId: string;
  /** Los grados del año (los mismos de la lista): son las opciones del selector y dan el nombre del aviso. */
  grados: GradoConSecciones[];
  /** Si el modal se abre desde el botón "Agregar sección" de un grado, llega con ese grado ya elegido. */
  gradoCodigo?: string;
};

// Figma > Personal Administrativo > "Modal - Nueva sección" (HU-75). Con ventanas altas el selector de grado puede
// desplegarse sobre el borde del modal; por debajo de 560 px de alto vuelve a recortar y desplazar el cuerpo.
export const DialogoSeccion = ({
  abierto,
  onCerrar,
  ...datos
}: DialogoSeccionProps) => (
  <Dialog open={abierto} onOpenChange={(open) => !open && onCerrar()}>
    <DialogContent className="flex max-h-[92vh] max-w-[520px] flex-col gap-0 overflow-visible rounded-xl border-stroke-tertiary bg-white p-0 [@media(max-height:560px)]:overflow-hidden">
      <FormularioSeccion onCerrar={onCerrar} {...datos} />
    </DialogContent>
  </Dialog>
);

const FormularioSeccion = ({
  onCerrar,
  anioId,
  grados,
  gradoCodigo,
}: Omit<DialogoSeccionProps, 'abierto'>) => {
  const { addNotification } = useNotifications();
  // El año ya está en la caché de la página (la cabecera lo pidió): no es una petición nueva.
  const anio = useAnioEscolar(anioId);

  // Los errores salen en vivo, mientras se escribe, y no solo al enviar.
  const form = useForm<ValoresSeccion>({
    resolver: zodResolver(nuevaSeccionSchema),
    mode: 'onChange',
    defaultValues: valoresInicialesSeccion(gradoCodigo),
  });
  const { register, setValue, formState } = form;
  const errores = formState.errors;

  // Los errores del backend (por ejemplo, el nombre repetido en el grado) salen por el interceptor: no se duplican.
  const crear = useCrearSeccion(anioId, (creada) => {
    const grado = grados.find((g) => g.codigo === creada.gradoCodigo);
    addNotification({
      type: 'success',
      title: 'Sección creada',
      message: `${grado?.nombre ?? creada.gradoCodigo} · Sección ${creada.nombre}`,
    });
    onCerrar();
  });

  const enviar = form.handleSubmit((valores) =>
    crear.mutate(aSolicitudSeccion(valores)),
  );

  return (
    <form onSubmit={enviar} noValidate className="flex min-h-0 flex-1 flex-col">
      <div className="flex-1 space-y-5 p-6 [@media(max-height:560px)]:overflow-y-auto">
        <div className="space-y-3">
          <IconBadge color="accent" size="lg" aria-hidden="true">
            <Users />
          </IconBadge>
          <div className="space-y-2">
            <DialogTitle className="text-h6 font-semibold text-content-primary">
              Nueva sección
            </DialogTitle>
            <DialogDescription className="text-body-3 text-content-secondary">
              {anio.data ? `Año escolar ${anio.data.anio}` : 'Año escolar'}
            </DialogDescription>
          </div>
        </div>

        <Select
          label="Grado *"
          placeholder="Elige un grado"
          options={grados.map((g) => ({ label: g.nombre, value: g.codigo }))}
          registration={register('gradoCodigo')}
          error={errores.gradoCodigo}
        />

        <div className="grid gap-4 sm:grid-cols-2">
          <Input
            label="Nombre de la sección *"
            hint="Ej.: A, B, C"
            autoComplete="off"
            // El nombre se pasa a mayúsculas al recibir lo escrito; sin tope de largo en el campo para que el aviso
            // "hasta 5 caracteres" pueda verse en vivo.
            registration={register('nombre', {
              onChange: (evento) =>
                setValue(
                  'nombre',
                  normalizarNombreSeccion(evento.target.value),
                  {
                    shouldValidate: true,
                  },
                ),
            })}
            error={errores.nombre}
          />
          <Input
            label="Capacidad máxima *"
            hint={`Entre ${CAPACIDAD_MINIMA} y ${CAPACIDAD_MAXIMA} alumnos.`}
            inputMode="numeric"
            autoComplete="off"
            registration={register('capacidadMaxima')}
            error={errores.capacidadMaxima}
          />
        </div>
      </div>

      <div className="flex justify-end gap-3 border-t border-stroke-tertiary px-6 py-4">
        <Button type="button" variant="tertiary" onClick={onCerrar}>
          Cancelar
        </Button>
        <Button
          type="submit"
          isLoading={crear.isPending}
          disabled={crear.isPending}
        >
          Crear sección
        </Button>
      </div>
    </form>
  );
};
