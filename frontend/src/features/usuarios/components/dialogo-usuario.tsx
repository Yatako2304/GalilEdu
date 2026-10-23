import { zodResolver } from '@hookform/resolvers/zod';
import { Lock, UserCheck } from 'lucide-react';
import * as React from 'react';
import { useForm } from 'react-hook-form';
import { z } from 'zod';

import { PersonaFields } from '@/components/personas/persona-fields';
import {
  PersonaFormValues,
  personaAFormulario,
  personaUsuarioSchema,
  personaVacia,
} from '@/components/personas/persona-schema';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogTitle,
} from '@/components/ui/dialog';
import { Switch } from '@/components/ui/form';
import { Spinner } from '@/components/ui/spinner';
import { ROLES } from '@/config/roles';
import { DatosPersona, UsuarioCreado, UsuarioDetalle } from '@/types/api';
import { cn } from '@/utils/cn';
import { formatFecha } from '@/utils/format';

import { useRolesAsignables, useUsuario } from '../api/get-usuario';
import {
  AccionEstado,
  useActualizarUsuario,
  useCambiarEstadoUsuario,
  useCrearUsuario,
} from '../api/mutaciones';
import { verificarDocumento } from '../api/verificar-documento';

import { DialogoConfirmacion } from './dialogo-confirmacion';
import { EstudiantesACargo, SeccionesTutor } from './vinculos-usuario';

/**
 * Roles que no se asignan desde aquí: Apoderado se asigna al inscribir a un estudiante y Tutor al
 * designarlo tutor de una sección (CAHU6-33 y 34). Se muestran, pero solo de vista.
 */
const ROLES_SOLO_LECTURA: ROLES[] = [ROLES.APODERADO, ROLES.TUTOR];

const MOTIVO_SOLO_LECTURA: Partial<Record<ROLES, string>> = {
  [ROLES.APODERADO]: 'Se asigna al inscribir a un estudiante',
  [ROLES.TUTOR]: 'Se asigna al designarlo tutor de una sección',
};

const esquema = z.object({
  persona: personaUsuarioSchema,
  roles: z.array(z.nativeEnum(ROLES)).min(1, 'Asigna al menos un rol'),
});

type Valores = { persona: PersonaFormValues; roles: ROLES[] };

export type ModoDialogo =
  | { modo: 'crear' }
  | { modo: 'ver'; id: string }
  | { modo: 'editar'; id: string };

type DialogoUsuarioProps = {
  estado: ModoDialogo | null;
  onCerrar: () => void;
  onCreado: (u: UsuarioCreado) => void;
};

const nombreCorto = (u: UsuarioDetalle) =>
  `${u.persona.nombres.split(' ')[0]} ${u.persona.apellidoPaterno}`;

// Figma > Administrador > Usuarios > modales "Nuevo Usuario" y "Editar usuario" (HU-6, HU-9).
export const DialogoUsuario = ({
  estado,
  onCerrar,
  onCreado,
}: DialogoUsuarioProps) => {
  const id = estado && estado.modo !== 'crear' ? estado.id : null;
  const detalle = useUsuario(id);
  const cargando = !!id && detalle.isLoading;

  return (
    <Dialog open={!!estado} onOpenChange={(open) => !open && onCerrar()}>
      <DialogContent className="flex max-h-[92vh] max-w-[800px] flex-col gap-0 overflow-hidden rounded-xl border-stroke-tertiary bg-white p-0">
        {estado &&
          (cargando ? (
            <div className="flex h-60 items-center justify-center">
              <DialogTitle className="sr-only">Cargando usuario</DialogTitle>
              <Spinner />
            </div>
          ) : (
            <FormularioUsuario
              key={id ?? 'nuevo'}
              modo={estado.modo}
              usuario={detalle.data}
              onCerrar={onCerrar}
              onCreado={onCreado}
            />
          ))}
      </DialogContent>
    </Dialog>
  );
};

const FormularioUsuario = ({
  modo,
  usuario,
  onCerrar,
  onCreado,
}: {
  modo: ModoDialogo['modo'];
  usuario?: UsuarioDetalle;
  onCerrar: () => void;
  onCreado: (u: UsuarioCreado) => void;
}) => {
  const soloLectura = modo === 'ver';
  const roles = useRolesAsignables();
  const [confirmar, setConfirmar] = React.useState<
    { tipo: 'guardar'; datos: Valores } | { tipo: AccionEstado } | null
  >(null);

  const form = useForm<Valores>({
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    resolver: zodResolver(esquema) as any,
    defaultValues: usuario
      ? { persona: personaAFormulario(usuario.persona), roles: usuario.roles }
      : // Como en la inscripción: ningún campo trae valor por defecto (CAHU6-15).
        {
          persona: {
            ...personaVacia(),
            tipoDocumento: '' as unknown as PersonaFormValues['tipoDocumento'],
            nacionalidad: '',
          },
          roles: [],
        },
  });
  const rolesElegidos = form.watch('roles');

  const crear = useCrearUsuario((u) => {
    onCerrar();
    onCreado(u);
  });
  const actualizar = useActualizarUsuario(() => {
    setConfirmar(null);
    onCerrar();
  });
  const cambiarEstado = useCambiarEstadoUsuario(() => setConfirmar(null));

  const [verificando, setVerificando] = React.useState(false);

  /**
   * El documento repetido se marca debajo del campo junto con los demás errores, no como notificación
   * al guardar. Si el documento no cambió al editar, no se consulta.
   */
  const documentoRepetido = () => {
    const { tipoDocumento, numeroDocumento } = form.getValues('persona');
    if (usuario && numeroDocumento.trim() === usuario.persona.numeroDocumento) {
      return Promise.resolve(null);
    }
    return verificarDocumento(tipoDocumento, numeroDocumento, modo === 'crear');
  };

  const enviar = async (evento?: React.BaseSyntheticEvent) => {
    evento?.preventDefault();
    setVerificando(true);
    const repetido = documentoRepetido();
    let valores: Valores | null = null;
    await form.handleSubmit((v) => {
      valores = v;
    })(evento);
    const error = await repetido;
    setVerificando(false);
    if (error) {
      form.setError(
        'persona.numeroDocumento',
        { type: 'repetido', message: error },
        { shouldFocus: !!valores },
      );
      return;
    }
    if (!valores) return;
    if (modo === 'editar') {
      setConfirmar({ tipo: 'guardar', datos: valores });
      return;
    }
    const datos = esquema.parse(valores);
    crear.mutate({
      persona: datos.persona as DatosPersona,
      roles: datos.roles,
    });
  };

  const alternarRol = (rol: ROLES) => {
    if (ROLES_SOLO_LECTURA.includes(rol)) return;
    const actuales = form.getValues('roles');
    form.setValue(
      'roles',
      actuales.includes(rol)
        ? actuales.filter((r) => r !== rol)
        : [...actuales, rol],
      { shouldValidate: form.formState.isSubmitted },
    );
  };

  const activo = usuario?.estado === 'ACTIVO';
  const nombre = usuario ? nombreCorto(usuario) : '';

  return (
    <>
      <div className="border-b border-stroke-tertiary px-6 py-5">
        <DialogTitle className="text-h6 font-semibold text-content-primary">
          {modo === 'crear'
            ? 'Nuevo Usuario'
            : modo === 'editar'
              ? 'Editar usuario'
              : 'Detalle del usuario'}
        </DialogTitle>
        <DialogDescription className="text-body-3 text-content-secondary">
          {usuario
            ? `Código de usuario: ${usuario.username} · Registrado el ${formatFecha(usuario.creadoEn)}`
            : 'Completa los datos para crear una cuenta en Galiledu'}
        </DialogDescription>
      </div>

      <form
        noValidate
        id="form-usuario"
        onSubmit={enviar}
        className="flex-1 space-y-6 overflow-y-auto p-6"
      >
        {usuario && (
          <div className="flex items-center justify-between gap-4 rounded-xl border border-stroke-tertiary bg-surface-secondary px-4 py-3">
            <div>
              <p className="text-label-2 font-medium text-content-primary">
                {activo ? 'Cuenta activa' : 'Cuenta inactiva'}
              </p>
              <p className="text-body-4 text-content-tertiary">
                Usuario {usuario.username} ·{' '}
                {activo
                  ? 'Puede iniciar sesión en GalilEdu'
                  : 'No puede iniciar sesión en GalilEdu'}
              </p>
            </div>
            <Switch
              checked={activo}
              disabled={soloLectura}
              onCheckedChange={(v) =>
                setConfirmar({ tipo: v ? 'reactivar' : 'desactivar' })
              }
              className="data-[state=checked]:bg-surface-accent-primary data-[state=unchecked]:bg-slate-300"
              aria-label="Cuenta activa"
            />
          </div>
        )}

        <section className="space-y-4">
          <h3 className="text-label-3 uppercase tracking-wide text-content-tertiary">
            Datos personales
          </h3>
          <PersonaFields
            register={form.register}
            errors={form.formState.errors}
            prefix="persona"
            disabled={soloLectura}
            conPaisDeNacimiento
            tipoDocumento={form.watch('persona.tipoDocumento')}
            onTipoDocumentoChange={(filtrar) =>
              form.setValue(
                'persona.numeroDocumento',
                filtrar(form.getValues('persona.numeroDocumento')),
              )
            }
          />
        </section>

        <section className="space-y-3 border-t border-stroke-tertiary pt-5">
          <h3 className="text-label-3 uppercase tracking-wide text-content-tertiary">
            Asignación de rol
          </h3>
          <div className="flex flex-wrap gap-2">
            {(roles.data ?? []).map((rol) => {
              const elegido = rolesElegidos.includes(rol.codigo);
              const bloqueado = ROLES_SOLO_LECTURA.includes(rol.codigo);
              return (
                <button
                  key={rol.codigo}
                  type="button"
                  disabled={soloLectura || bloqueado}
                  aria-pressed={elegido}
                  title={MOTIVO_SOLO_LECTURA[rol.codigo]}
                  onClick={() => alternarRol(rol.codigo)}
                  className={cn(
                    'inline-flex items-center gap-1 rounded-full border px-3 py-1 text-body-4 transition-colors disabled:cursor-default',
                    elegido
                      ? 'border-transparent bg-deep-blue-800 text-white'
                      : 'border-stroke-accent-primary bg-white text-content-accent-secondary',
                    !elegido && !bloqueado && 'hover:bg-surface-accent-light',
                    bloqueado &&
                      !soloLectura &&
                      'cursor-not-allowed opacity-60',
                  )}
                >
                  {bloqueado && <Lock className="size-3" aria-hidden="true" />}
                  {rol.nombre}
                </button>
              );
            })}
          </div>
          {form.formState.errors.roles && (
            <p className="text-body-4 text-content-error">
              {form.formState.errors.roles.message}
            </p>
          )}
        </section>

        {usuario?.roles.includes(ROLES.APODERADO) && (
          <EstudiantesACargo estudiantes={usuario.estudiantesACargo} />
        )}
        {usuario?.roles.includes(ROLES.TUTOR) && (
          <SeccionesTutor secciones={usuario.tutorDe} />
        )}
      </form>

      <div className="flex items-center justify-between gap-3 border-t border-stroke-tertiary px-6 py-4">
        <div>
          {usuario && !activo && !soloLectura && (
            <Button
              variant="tertiary"
              icon={<UserCheck className="size-4" />}
              onClick={() => setConfirmar({ tipo: 'reactivar' })}
            >
              Activar usuario
            </Button>
          )}
        </div>
        <div className="flex gap-3">
          <Button variant="primary-light" onClick={onCerrar}>
            {soloLectura ? 'Cerrar' : 'Cancelar'}
          </Button>
          {!soloLectura && (
            <Button
              type="submit"
              form="form-usuario"
              isLoading={crear.isPending || verificando}
            >
              Guardar
            </Button>
          )}
        </div>
      </div>

      {usuario && confirmar && (
        <DialogoConfirmacion
          open
          onOpenChange={(open) => !open && setConfirmar(null)}
          tipo={confirmar.tipo}
          cargando={actualizar.isPending || cambiarEstado.isPending}
          textoConfirmar={
            confirmar.tipo === 'guardar' ? 'Guardar' : 'Confirmar'
          }
          {...TEXTOS[confirmar.tipo](nombre)}
          onConfirmar={() => {
            if (confirmar.tipo === 'guardar') {
              const datos = esquema.parse(confirmar.datos);
              actualizar.mutate({
                id: usuario.id,
                data: {
                  persona: datos.persona as DatosPersona,
                  roles: datos.roles,
                },
              });
            } else {
              cambiarEstado.mutate({ id: usuario.id, accion: confirmar.tipo });
            }
          }}
        />
      )}
    </>
  );
};

export const TEXTOS: Record<
  'guardar' | AccionEstado,
  (nombre: string) => { titulo: string; descripcion: string }
> = {
  guardar: (n) => ({
    titulo: `¿Guardar los cambios de ${n}?`,
    descripcion:
      'Se actualizarán los datos del usuario. Si modificaste sus roles, se habilitarán o deshabilitarán automáticamente las funciones correspondientes.',
  }),
  desactivar: (n) => ({
    titulo: `¿Desactivar a ${n}?`,
    descripcion:
      'Al desactivarlo, no podrá iniciar sesión en Galiledu. Su información y el historial de auditoría asociado a su cuenta se conservarán, y podrás reactivarlo cuando lo necesites.',
  }),
  reactivar: (n) => ({
    titulo: `¿Reactivar a ${n}?`,
    descripcion:
      'El usuario podrá volver a iniciar sesión de inmediato, con el rol y los permisos que tenía asignados antes de ser desactivado.',
  }),
  eliminar: (n) => ({
    titulo: `¿Eliminar a ${n}?`,
    descripcion:
      'Al eliminarlo, dejará de aparecer en la lista de usuarios y no podrá iniciar sesión en Galiledu.',
  }),
};
