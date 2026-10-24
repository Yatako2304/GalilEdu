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
import { useNavigate } from 'react-router';

import { Card } from '@/components/ui/card';
import { DialogoConfirmacion } from '@/components/ui/dialog/dialogo-confirmacion';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown';
import { Select } from '@/components/ui/form';
import { Link } from '@/components/ui/link';
import { Spinner } from '@/components/ui/spinner';
import { PaginationBar, Table } from '@/components/ui/table';
import { Tag } from '@/components/ui/tag';
import { EstadoEstudiante, EstudianteResumen } from '@/types/api';

import {
  AccionEstudiante,
  useCambiarEstadoEstudiante,
  useEstudiantes,
} from '../api/estudiantes';

// Textos de los modales de confirmación (mismo formato que Usuarios).
const TEXTOS: Record<
  AccionEstudiante,
  (nombre: string) => { titulo: string; descripcion: string }
> = {
  retirar: (nombre) => ({
    titulo: `¿Inactivar a ${nombre}?`,
    descripcion:
      'Quedará retirado y no se podrá inscribir de nuevo mientras siga inactivo. Puedes reactivarlo cuando quieras.',
  }),
  reactivar: (nombre) => ({
    titulo: `¿Reactivar a ${nombre}?`,
    descripcion: 'Volverá a figurar como activo en el colegio.',
  }),
  eliminar: (nombre) => ({
    titulo: `¿Eliminar a ${nombre}?`,
    descripcion:
      'Dejará de aparecer en el listado. Si se inscribe de nuevo con el mismo documento, sus datos se actualizarán con la nueva inscripción.',
  }),
};

const TIPO_DIALOGO = {
  retirar: 'desactivar',
  reactivar: 'reactivar',
  eliminar: 'eliminar',
} as const;

const ESTADOS: Record<
  EstadoEstudiante,
  { label: string; color: 'info' | 'success' | 'neutral' }
> = {
  INSCRITO: { label: 'Inscrito', color: 'info' },
  MATRICULADO: { label: 'Matriculado', color: 'success' },
  RETIRADO: { label: 'Retirado', color: 'neutral' },
};

export const EstadoEstudianteTag = ({
  estado,
}: {
  estado: EstadoEstudiante;
}) => (
  <Tag color={ESTADOS[estado].color} size="md" className="rounded-full">
    {ESTADOS[estado].label}
  </Tag>
);

// Figma > "Admin / Estudiantes | Listado de estudiantes"
export const EstudiantesLista = ({
  hrefFicha,
  hrefEditar,
}: {
  hrefFicha: (id: string) => string;
  hrefEditar: (id: string) => string;
}) => {
  const [texto, setTexto] = React.useState('');
  const [q, setQ] = React.useState('');
  const [estado, setEstado] = React.useState<EstadoEstudiante | ''>('');
  const [page, setPage] = React.useState(1);

  React.useEffect(() => {
    const t = setTimeout(() => {
      setQ(texto);
      setPage(1);
    }, 300);
    return () => clearTimeout(t);
  }, [texto]);

  const estudiantes = useEstudiantes({ q, estado, page });

  const [accion, setAccion] = React.useState<{
    estudiante: EstudianteResumen;
    tipo: AccionEstudiante;
  } | null>(null);
  const cambiarEstado = useCambiarEstadoEstudiante(() => setAccion(null));

  return (
    <>
      <div className="flex flex-wrap gap-3">
        <label className="relative w-full max-w-xs">
          <span className="sr-only">Buscar por nombre o documento</span>
          <Search
            className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-content-tertiary"
            aria-hidden="true"
          />
          <input
            value={texto}
            onChange={(e) => setTexto(e.target.value)}
            placeholder="Buscar por nombre o documento"
            className="h-10 w-full rounded-xl border border-stroke-primary bg-white pl-9 pr-3 text-body-3 placeholder:text-content-placeholder focus-visible:border-stroke-focus focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-deep-blue-100"
          />
        </label>
        <Select
          aria-label="Filtrar por estado"
          options={[
            { label: 'Estado: Todos', value: '' },
            { label: 'Inscrito', value: 'INSCRITO' },
            { label: 'Matriculado', value: 'MATRICULADO' },
            { label: 'Retirado', value: 'RETIRADO' },
          ]}
          value={estado}
          onChange={(e) => {
            setEstado(e.target.value as EstadoEstudiante | '');
            setPage(1);
          }}
          wrapperClassName="w-48"
          className="h-10"
        />
      </div>

      <Card className="p-6">
        {estudiantes.isLoading ? (
          <div className="flex h-80 items-center justify-center">
            <Spinner />
          </div>
        ) : (
          <>
            <Table
              data={estudiantes.data?.data ?? []}
              emptyMessage="Aún no hay estudiantes inscritos"
              columns={[
                {
                  title: 'Estudiante',
                  field: 'nombreCompleto',
                  Cell: ({ entry }) => (
                    <div>
                      <Link
                        to={hrefFicha(entry.id)}
                        className="text-body-3 text-content-primary"
                      >
                        {entry.nombreCompleto}
                      </Link>
                      <p className="text-body-4 text-content-tertiary">
                        {entry.documento}
                      </p>
                    </div>
                  ),
                },
                {
                  title: 'Grado',
                  field: 'grado',
                  Cell: ({ entry }) => (
                    <span>
                      {entry.grado}{' '}
                      <span className="text-content-quartiary">
                        ({entry.anioAcademico})
                      </span>
                    </span>
                  ),
                },
                {
                  title: 'Estado',
                  field: 'estado',
                  Cell: ({ entry }) => (
                    <EstadoEstudianteTag estado={entry.estado} />
                  ),
                },
                { title: 'Responsable', field: 'responsable' },
                {
                  title: '',
                  field: 'id',
                  Cell: ({ entry }) => (
                    <AccionesFila
                      estudiante={entry}
                      hrefFicha={hrefFicha}
                      hrefEditar={hrefEditar}
                      onAccion={(tipo) =>
                        setAccion({ estudiante: entry, tipo })
                      }
                    />
                  ),
                },
              ]}
            />
            {estudiantes.data && estudiantes.data.meta.total > 0 && (
              <PaginationBar
                {...estudiantes.data.meta}
                onPageChange={setPage}
              />
            )}
          </>
        )}
      </Card>

      {accion && (
        <DialogoConfirmacion
          open
          onOpenChange={(open) => !open && setAccion(null)}
          tipo={TIPO_DIALOGO[accion.tipo]}
          cargando={cambiarEstado.isPending}
          textoConfirmar={accion.tipo === 'eliminar' ? 'Eliminar' : 'Confirmar'}
          {...TEXTOS[accion.tipo](accion.estudiante.nombreCompleto)}
          onConfirmar={() =>
            cambiarEstado.mutate({
              id: accion.estudiante.id,
              accion: accion.tipo,
            })
          }
        />
      )}
    </>
  );
};

// Los tres puntos de cada fila (Figma): Ver, Editar, Inactivar o Reactivar, y Eliminar.
const AccionesFila = ({
  estudiante,
  hrefFicha,
  hrefEditar,
  onAccion,
}: {
  estudiante: EstudianteResumen;
  hrefFicha: (id: string) => string;
  hrefEditar: (id: string) => string;
  onAccion: (tipo: AccionEstudiante) => void;
}) => {
  const navigate = useNavigate();
  const retirado = estudiante.estado === 'RETIRADO';
  return (
    <DropdownMenu>
      <DropdownMenuTrigger className="inline-flex size-8 items-center justify-center rounded-xl text-content-tertiary hover:bg-surface-tertiary focus:outline-none focus-visible:ring-2 focus-visible:ring-stroke-focus">
        <EllipsisVertical className="size-4" aria-hidden="true" />
        <span className="sr-only">Acciones de {estudiante.nombreCompleto}</span>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-40">
        <DropdownMenuItem onClick={() => navigate(hrefFicha(estudiante.id))}>
          <Eye className="mr-2 size-4" aria-hidden="true" /> Ver
        </DropdownMenuItem>
        <DropdownMenuItem onClick={() => navigate(hrefEditar(estudiante.id))}>
          <Pencil className="mr-2 size-4" aria-hidden="true" /> Editar
        </DropdownMenuItem>
        <DropdownMenuItem
          onClick={() => onAccion(retirado ? 'reactivar' : 'retirar')}
        >
          {retirado ? (
            <UserCheck className="mr-2 size-4" aria-hidden="true" />
          ) : (
            <UserX className="mr-2 size-4" aria-hidden="true" />
          )}
          {retirado ? 'Reactivar' : 'Inactivar'}
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
