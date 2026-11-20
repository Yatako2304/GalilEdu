import { zodResolver } from '@hookform/resolvers/zod';
import { KeyRound, Pencil } from 'lucide-react';
import * as React from 'react';
import { useForm } from 'react-hook-form';
import { z } from 'zod';

import { DatosPersonaVista } from '@/components/personas/datos-persona-vista';
import { PersonaFields } from '@/components/personas/persona-fields';
import {
  PersonaFormValues,
  personaAFormulario,
  personaUsuarioSchema,
} from '@/components/personas/persona-schema';
import { Alert } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { Card, CardHeader, CardTitle } from '@/components/ui/card';
import { IconBadge } from '@/components/ui/icon-badge';
import { Link } from '@/components/ui/link';
import { useNotifications } from '@/components/ui/notifications';
import { Spinner } from '@/components/ui/spinner';
import { paths } from '@/config/paths';
import { ROLES } from '@/config/roles';
import { useActualizarMisDatos, useMisDatos, useUser } from '@/lib/auth';
import { DatosPersona } from '@/types/api';

type Valores = { persona: PersonaFormValues };

const esquema = z.object({ persona: personaUsuarioSchema });

/**
 * «Mis datos» (ampliación de HU-9: en Notion esa HU solo cubre la edición por el administrador; no está en Figma). Se
 * abre desde la tarjeta de usuario del menú lateral. Primero muestra los datos personales tal cual; recién con
 * «Editar datos» aparecen los campos (mismas validaciones que «Editar usuario», sin roles ni estado de la cuenta), así
 * nada cambia por accidente. Es igual para todos los roles (la familia del apoderado está en «Datos de la Familia»).
 * Al final, «Cambiar contraseña».
 */
export const MisDatos = () => {
  const misDatos = useMisDatos();
  const user = useUser();
  const tieneRolApoderado = !!user.data?.roles.includes(ROLES.APODERADO);

  if (misDatos.isPending) {
    return (
      <div className="flex justify-center py-16">
        <Spinner />
      </div>
    );
  }
  if (!misDatos.data) {
    return <Alert type="error" title="No se pudieron cargar tus datos." />;
  }

  return (
    <div className="space-y-6">
      <DatosPersonales
        datos={misDatos.data}
        tieneRolApoderado={tieneRolApoderado}
      />
      <TarjetaContrasena />
    </div>
  );
};

const DatosPersonales = ({
  datos,
  tieneRolApoderado,
}: {
  datos: DatosPersona;
  tieneRolApoderado: boolean;
}) => {
  const [editando, setEditando] = React.useState(false);

  if (editando) {
    return (
      <FormularioMisDatos
        datos={datos}
        tieneRolApoderado={tieneRolApoderado}
        onTerminar={() => setEditando(false)}
      />
    );
  }

  return (
    <Card>
      <CardHeader
        action={
          <Button
            variant="primary-light"
            icon={<Pencil className="size-4" aria-hidden="true" />}
            onClick={() => setEditando(true)}
          >
            Editar datos
          </Button>
        }
      >
        <CardTitle>Datos personales</CardTitle>
      </CardHeader>
      <div className="p-5">
        <DatosPersonaVista persona={datos} conExalumno={tieneRolApoderado} />
      </div>
    </Card>
  );
};

/** Se monta al pulsar «Editar datos»: siempre parte de los datos guardados. */
const FormularioMisDatos = ({
  datos,
  tieneRolApoderado,
  onTerminar,
}: {
  datos: DatosPersona;
  tieneRolApoderado: boolean;
  onTerminar: () => void;
}) => {
  const { addNotification } = useNotifications();

  const form = useForm<Valores>({
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    resolver: zodResolver(esquema) as any,
    mode: 'all',
    defaultValues: { persona: personaAFormulario(datos) },
  });

  const actualizar = useActualizarMisDatos(() => {
    addNotification({
      type: 'success',
      title: 'Tus datos se actualizaron correctamente.',
    });
    onTerminar();
  });

  const enviar = form.handleSubmit((valores) => {
    const datosValidos = personaUsuarioSchema.parse(
      valores.persona,
    ) as DatosPersona;
    actualizar.mutate(datosValidos);
  });

  return (
    <Card>
      <form noValidate onSubmit={enviar}>
        <CardHeader>
          <CardTitle>Editar datos personales</CardTitle>
        </CardHeader>
        <div className="p-5">
          <PersonaFields
            register={form.register}
            errors={form.formState.errors}
            prefix="persona"
            conPaisDeNacimiento
            sinExalumno={!tieneRolApoderado}
            tipoDocumento={form.watch('persona.tipoDocumento')}
            onTipoDocumentoChange={(filtrar) =>
              form.setValue(
                'persona.numeroDocumento',
                filtrar(form.getValues('persona.numeroDocumento')),
              )
            }
          />
        </div>
        <div className="flex justify-end gap-3 border-t border-stroke-tertiary px-5 py-4">
          <Button
            type="button"
            variant="primary-light"
            onClick={onTerminar}
            disabled={actualizar.isPending}
          >
            Cancelar
          </Button>
          <Button type="submit" isLoading={actualizar.isPending}>
            Guardar cambios
          </Button>
        </div>
      </form>
    </Card>
  );
};

const TarjetaContrasena = () => (
  <Card>
    <div className="flex flex-wrap items-center justify-between gap-4 px-5 py-4">
      <div className="flex items-center gap-4">
        <IconBadge shape="square">
          <KeyRound aria-hidden="true" />
        </IconBadge>
        <div>
          <p className="text-body-3 font-medium text-content-primary">
            Contraseña
          </p>
          <p className="text-body-4 text-content-tertiary">
            Cambia la contraseña con la que ingresas a GalilEdu.
          </p>
        </div>
      </div>
      <Button variant="primary-light" type="button" asChild>
        <Link to={paths.app.cambiarContrasena.getHref()}>
          Cambiar contraseña
        </Link>
      </Button>
    </div>
  </Card>
);
