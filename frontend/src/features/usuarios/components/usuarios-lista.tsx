import {
  EllipsisVertical,
  Eye,
  Pencil,
  Search,
  Trash2,
  UserCheck,
  UserX,
} from 'lucide-react';
import * as React from 'react';

import { Card } from '@/components/ui/card';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown';
import { Select } from '@/components/ui/form';
import { Spinner } from '@/components/ui/spinner';
import { PaginationBar, Table } from '@/components/ui/table';
import { Status } from '@/components/ui/tag';
import { ROLES } from '@/config/roles';
import { EstadoUsuario, UsuarioResumen } from '@/types/api';
import { formatDay } from '@/utils/format';

import { useUsuarios } from '../api/get-usuarios';
import { AccionEstado, useCambiarEstadoUsuario } from '../api/mutaciones';

import { DialogoConfirmacion } from './dialogo-confirmacion';
import { ModoDialogo, TEXTOS } from './dialogo-usuario';
import { NOMBRE_ROL, RolesCelda } from './rol-tag';

const OPCIONES_ROL = [
  { label: 'Rol: Todos', value: '' },
  ...Object.values(ROLES).map((r) => ({ label: NOMBRE_ROL[r], value: r })),
];

const OPCIONES_ESTADO = [
  { label: 'Estado: Todos', value: '' },
  { label: 'Activo', value: 'ACTIVO' },
  { label: 'Inactivo', value: 'INACTIVO' },
];

/** Espera a que el usuario deje de escribir antes de buscar. */
const useDebounce = (valor: string, ms = 300) => {
  const [actual, setActual] = React.useState(valor);
  React.useEffect(() => {
    const t = setTimeout(() => setActual(valor), ms);
    return () => clearTimeout(t);
  }, [valor, ms]);
  return actual;
};

type UsuariosListaProps = {
  onAbrir: (estado: ModoDialogo) => void;
  /** Filtro inicial (ej. "Revisar cuentas" desde el inicio abre los inactivos). */
  estadoInicial?: EstadoUsuario | '';
};

// Figma > Administrador > "Admin / Usuarios | HU-6 | HU-7 | HU-8 - grilla"
export const UsuariosLista = ({
  onAbrir,
  estadoInicial = '',
}: UsuariosListaProps) => {
  const [texto, setTexto] = React.useState('');
  const [rol, setRol] = React.useState<ROLES | ''>('');
  const [estado, setEstado] = React.useState<EstadoUsuario | ''>(estadoInicial);
  const [page, setPage] = React.useState(1);
  const q = useDebounce(texto);

  React.useEffect(() => setPage(1), [q, rol, estado]);

  const usuarios = useUsuarios({ filtros: { q, rol, estado, page } });

  const [accion, setAccion] = React.useState<{
    usuario: UsuarioResumen;
    tipo: AccionEstado;
  } | null>(null);
  const cambiarEstado = useCambiarEstadoUsuario(() => setAccion(null));

  return (
    <>
      <div className="flex flex-wrap gap-3">
        <label className="relative w-full max-w-xs">
          <span className="sr-only">Buscar por nombre o correo</span>
          <Search
            className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-content-tertiary"
            aria-hidden="true"
          />
          <input
            value={texto}
            onChange={(e) => setTexto(e.target.value)}
            placeholder="Buscar por nombre o correo"
            className="h-10 w-full rounded-xl border border-stroke-primary bg-white pl-9 pr-3 text-body-3 placeholder:text-content-placeholder focus-visible:border-stroke-focus focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-deep-blue-100"
          />
        </label>
        <Select
          aria-label="Filtrar por rol"
          options={OPCIONES_ROL}
          value={rol}
          onChange={(e) => setRol(e.target.value as ROLES | '')}
          wrapperClassName="w-48"
          className="h-10"
        />
        <Select
          aria-label="Filtrar por estado"
          options={OPCIONES_ESTADO}
          value={estado}
          onChange={(e) => setEstado(e.target.value as EstadoUsuario | '')}
          wrapperClassName="w-48"
          className="h-10"
        />
      </div>

      <Card className="p-6">
        {usuarios.isLoading ? (
          <div className="flex h-80 items-center justify-center">
            <Spinner />
          </div>
        ) : (
          <>
            <Table
              data={usuarios.data?.data ?? []}
              emptyMessage="No se encontraron usuarios con esos filtros"
              columns={[
                {
                  title: 'Usuario',
                  field: 'nombre',
                  Cell: ({ entry }) => (
                    <div>
                      <p className="text-body-3 text-content-primary">
                        {entry.nombre}
                      </p>
                      <p className="text-body-4 text-content-tertiary">
                        {entry.correo}
                      </p>
                    </div>
                  ),
                },
                {
                  title: 'Rol',
                  field: 'roles',
                  Cell: ({ entry }) => <RolesCelda roles={entry.roles} />,
                },
                {
                  title: 'Estado',
                  field: 'estado',
                  Cell: ({ entry }) => (
                    <Status active={entry.estado === 'ACTIVO'} />
                  ),
                },
                {
                  title: 'Último acceso',
                  field: 'ultimoAcceso',
                  Cell: ({ entry }) => (
                    <span>
                      {entry.ultimoAcceso
                        ? formatDay(entry.ultimoAcceso)
                        : 'Sin accesos'}
                    </span>
                  ),
                },
                {
                  title: '',
                  field: 'id',
                  Cell: ({ entry }) => (
                    <AccionesFila
                      usuario={entry}
                      onAbrir={onAbrir}
                      onAccion={(tipo) => setAccion({ usuario: entry, tipo })}
                    />
                  ),
                },
              ]}
            />
            {usuarios.data && usuarios.data.meta.total > 0 && (
              <PaginationBar {...usuarios.data.meta} onPageChange={setPage} />
            )}
          </>
        )}
      </Card>

      {accion && (
        <DialogoConfirmacion
          open
          onOpenChange={(open) => !open && setAccion(null)}
          tipo={accion.tipo}
          cargando={cambiarEstado.isPending}
          textoConfirmar={accion.tipo === 'eliminar' ? 'Eliminar' : 'Confirmar'}
          {...TEXTOS[accion.tipo](accion.usuario.nombre)}
          onConfirmar={() =>
            cambiarEstado.mutate({ id: accion.usuario.id, accion: accion.tipo })
          }
        />
      )}
    </>
  );
};

const AccionesFila = ({
  usuario,
  onAbrir,
  onAccion,
}: {
  usuario: UsuarioResumen;
  onAbrir: (estado: ModoDialogo) => void;
  onAccion: (tipo: AccionEstado) => void;
}) => {
  const activo = usuario.estado === 'ACTIVO';
  return (
    <DropdownMenu>
      <DropdownMenuTrigger className="inline-flex size-8 items-center justify-center rounded-xl text-content-tertiary hover:bg-surface-tertiary focus:outline-none focus-visible:ring-2 focus-visible:ring-stroke-focus">
        <EllipsisVertical className="size-4" aria-hidden="true" />
        <span className="sr-only">Acciones de {usuario.nombre}</span>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-40">
        <DropdownMenuItem
          onClick={() => onAbrir({ modo: 'ver', id: usuario.id })}
        >
          <Eye className="mr-2 size-4" aria-hidden="true" /> Ver
        </DropdownMenuItem>
        <DropdownMenuItem
          onClick={() => onAbrir({ modo: 'editar', id: usuario.id })}
        >
          <Pencil className="mr-2 size-4" aria-hidden="true" /> Editar
        </DropdownMenuItem>
        <DropdownMenuItem
          onClick={() => onAccion(activo ? 'desactivar' : 'reactivar')}
        >
          {activo ? (
            <UserX className="mr-2 size-4" aria-hidden="true" />
          ) : (
            <UserCheck className="mr-2 size-4" aria-hidden="true" />
          )}
          {activo ? 'Inactivar' : 'Reactivar'}
        </DropdownMenuItem>
        <DropdownMenuItem
          onClick={() => onAccion('eliminar')}
          className="text-content-error focus:text-content-error"
        >
          <Trash2 className="mr-2 size-4" aria-hidden="true" /> Eliminar
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
};
