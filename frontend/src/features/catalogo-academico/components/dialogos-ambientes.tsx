import { zodResolver } from '@hookform/resolvers/zod';
import { isAxiosError } from 'axios';
import { Building2 } from 'lucide-react';
import * as React from 'react';
import { useForm } from 'react-hook-form';

import { Alert } from '@/components/ui/alert';
import { Input, RadioCard, Textarea } from '@/components/ui/form';
import { useNotifications } from '@/components/ui/notifications';
import { AmbienteResumen, TipoAmbiente } from '@/types/api';

import { useCrearAmbiente, useEditarAmbiente } from '../api/ambientes';

import { DialogoCatalogo } from './dialogo-catalogo';
import {
  AmbienteFormulario,
  ambienteSchema,
  aSolicitudAmbiente,
  campoDelErrorDelBackend,
  textoRecursos,
} from './esquemas-ambientes';

type DialogoAmbienteProps = {
  /** null = ambiente nuevo. */
  ambiente: AmbienteResumen | null;
  onCerrar: () => void;
};

const TIPOS: { valor: TipoAmbiente; titulo: string; ayuda: string }[] = [
  {
    valor: 'AULA_REGULAR',
    titulo: 'Aula regular',
    ayuda: 'Aula de clases asignable a una sección',
  },
  {
    valor: 'ESPACIO_ESPECIAL',
    titulo: 'Espacio especial',
    ayuda: 'Laboratorio, taller, biblioteca, losa, auditorio…',
  },
];

const MENSAJE_ERROR_GENERAL =
  'No pudimos guardar el ambiente. Inténtalo de nuevo.';

const sinCambios = () => undefined;

// Figma > Personal Administrativo > CI-26 > "Modal - Nuevo ambiente físico" (nodo 857:11500).
// Diferencias con Figma: no hay campo «Código» (lo genera el backend) y el modo de edición no está dibujado: usa el
// mismo modal, con el tipo bloqueado.
export const DialogoAmbiente = ({
  ambiente,
  onCerrar,
}: DialogoAmbienteProps) => {
  const editando = ambiente !== null;
  const { addNotification } = useNotifications();
  // Un error que no es de ningún campo (por ejemplo, un 500) va en una alerta dentro del modal.
  const [errorGeneral, setErrorGeneral] = React.useState<string | null>(null);

  const form = useForm<AmbienteFormulario>({
    resolver: zodResolver(ambienteSchema),
    defaultValues: {
      // En edición el tipo no cambia: es el del ambiente y el campo va bloqueado.
      tipo: ambiente?.tipo ?? '',
      nombre: ambiente?.nombre ?? '',
      aforoMaximo: ambiente ? String(ambiente.aforoMaximo) : '',
      recursos: ambiente ? textoRecursos(ambiente.recursos) : '',
    },
  });

  const guardado = (titulo: string) => () => {
    addNotification({ type: 'success', title: titulo });
    onCerrar();
  };

  /** El 409 de nombre repetido va en el campo Nombre; el de aforo bajo la matrícula, en Aforo máximo. */
  const alFallar = (error: unknown) => {
    const respuesta = isAxiosError(error) ? error.response : undefined;
    const mensaje: string | undefined = respuesta?.data?.message;
    const campo = campoDelErrorDelBackend(respuesta?.status, mensaje);
    if (campo && mensaje) {
      form.setError(campo, { type: 'server', message: mensaje });
      return;
    }
    // Errores de validación del backend que se escaparon del formulario: cada uno en su campo.
    const errores: Record<string, string> = respuesta?.data?.errores ?? {};
    const deCampo = (['nombre', 'aforoMaximo', 'tipo'] as const).filter(
      (c) => errores[c],
    );
    if (respuesta?.status === 400 && deCampo.length > 0) {
      deCampo.forEach((c) =>
        form.setError(c, { type: 'server', message: errores[c] }),
      );
      return;
    }
    setErrorGeneral(mensaje ?? MENSAJE_ERROR_GENERAL);
  };

  const crear = useCrearAmbiente(guardado('Ambiente registrado.'), alFallar);
  const editar = useEditarAmbiente(guardado('Cambios guardados.'), alFallar);

  const enviar = form.handleSubmit((valores) => {
    setErrorGeneral(null);
    // El resolver ya entrega los valores normalizados (aforo numérico, recursos como lista).
    const datos = aSolicitudAmbiente(
      valores as unknown as Parameters<typeof aSolicitudAmbiente>[0],
    );
    if (ambiente) {
      editar.mutate({
        id: ambiente.id,
        data: {
          nombre: datos.nombre,
          aforoMaximo: datos.aforoMaximo,
          recursos: datos.recursos,
        },
      });
    } else {
      crear.mutate(datos);
    }
  });

  const errorTipo = form.formState.errors.tipo;

  return (
    <DialogoCatalogo
      open
      onCerrar={onCerrar}
      icono={<Building2 className="size-5" aria-hidden="true" />}
      titulo={editando ? 'Editar ambiente físico' : 'Nuevo ambiente físico'}
      descripcion="Completa los datos del aula o espacio."
      formId="form-ambiente"
      textoGuardar={editando ? 'Guardar cambios' : 'Registrar ambiente'}
      guardando={crear.isPending || editar.isPending}
      className="max-w-[600px]"
    >
      <form
        noValidate
        id="form-ambiente"
        className="space-y-4"
        onSubmit={enviar}
      >
        {errorGeneral && <Alert type="error" size="sm" title={errorGeneral} />}

        {/* Figma dibuja aquí el campo «Código» junto al nombre; ya no existe, así que el nombre ocupa todo el ancho. */}
        <Input
          label="Nombre *"
          registration={form.register('nombre')}
          error={form.formState.errors.nombre}
        />

        <fieldset>
          <legend className="mb-2 text-label-2 font-semibold text-content-primary">
            Tipo de ambiente *
          </legend>
          <div className="grid gap-2.5 sm:grid-cols-2">
            {TIPOS.map(({ valor, titulo, ayuda }) =>
              editando ? (
                // En edición el tipo no cambia: se ve el elegido y las dos opciones quedan bloqueadas.
                <RadioCard
                  key={valor}
                  name="tipo"
                  value={valor}
                  label={titulo}
                  helperText={ayuda}
                  checked={ambiente.tipo === valor}
                  disabled
                  onChange={sinCambios}
                />
              ) : (
                <RadioCard
                  key={valor}
                  value={valor}
                  label={titulo}
                  helperText={ayuda}
                  {...form.register('tipo')}
                />
              ),
            )}
          </div>
          {errorTipo?.message && (
            <p className="mt-1 text-body-4 text-content-error">
              {errorTipo.message}
            </p>
          )}
          {editando && (
            <p className="mt-1 text-body-4 text-content-quartiary">
              El tipo no se puede cambiar.
            </p>
          )}
        </fieldset>

        <Input
          label="Aforo máximo *"
          hint="Personas. Un aula regular admite como máximo 45."
          inputMode="numeric"
          registration={form.register('aforoMaximo')}
          error={form.formState.errors.aforoMaximo}
        />

        {/* Los recursos se escriben separados por comas y se envían como lista. */}
        <Textarea
          label="Recursos"
          hint="Separa cada recurso con una coma."
          registration={form.register('recursos')}
          error={form.formState.errors.recursos}
        />
      </form>
    </DialogoCatalogo>
  );
};
