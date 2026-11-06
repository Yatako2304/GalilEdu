import {
  CircleAlert,
  CircleCheck,
  EllipsisVertical,
  Eye,
  RefreshCw,
  Search,
  Users,
} from 'lucide-react';
import * as React from 'react';
import { useNavigate } from 'react-router';

import { Alert } from '@/components/ui/alert';
import { Card } from '@/components/ui/card';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown';
import { Input, Select } from '@/components/ui/form';
import { StatCard } from '@/components/ui/stat-card';
import { PaginationBar, Table } from '@/components/ui/table';
import { SelectableTag } from '@/components/ui/tag';
import { GRADOS_POR_NIVEL, nombreGrado } from '@/config/catalogos';
import { paths } from '@/config/paths';
import { useDebouncedValue } from '@/hooks/use-debounced-value';
import { EstadoMatricula, FilaMatriculaPeriodo } from '@/types/api';
import { formatFechaLima } from '@/utils/format';

import {
  OrdenMatriculas,
  useMatriculasDelPeriodo,
  usePeriodoMatriculas,
} from '../api/matriculas-periodo';

const ESTADOS: Partial<
  Record<
    EstadoMatricula,
    { etiqueta: string; color: 'info' | 'warning' | 'success' }
  >
> = {
  SECCION_RESERVADA: { etiqueta: 'Sección reservada', color: 'info' },
  EN_VALIDACION: { etiqueta: 'En validación', color: 'info' },
  PENDIENTE_PAGO: { etiqueta: 'Pendiente de pago', color: 'warning' },
  MATRICULADO: { etiqueta: 'Matriculado', color: 'success' },
};

const OPCIONES_GRADO = [
  { label: 'Grado: Todos', value: '' },
  ...Object.values(GRADOS_POR_NIVEL)
    .flat()
    .map(({ value }) => ({
      label: nombreGrado(String(value)),
      value: String(value),
    })),
];

const OPCIONES_ESTADO = [
  { label: 'Estado: Todos', value: '' },
  ...Object.entries(ESTADOS).map(([value, e]) => ({
    label: e.etiqueta,
    value,
  })),
];

/** Columna de la tabla ⇄ parámetro `orden` del backend. */
const COLUMNA_POR_ORDEN: Record<OrdenMatriculas, keyof FilaMatriculaPeriodo> = {
  estudiante: 'nombre',
  grado: 'grado',
  seccion: 'seccion',
  estado: 'estado',
};

const ordenDeColumna = (campo: keyof FilaMatriculaPeriodo) =>
  (Object.keys(COLUMNA_POR_ORDEN) as OrdenMatriculas[]).find(
    (orden) => COLUMNA_POR_ORDEN[orden] === campo,
  );

const AccionesMatricula = ({
  matricula,
}: {
  matricula: FilaMatriculaPeriodo;
}) => {
  const navigate = useNavigate();
  return (
    <DropdownMenu>
      <DropdownMenuTrigger className="inline-flex size-8 items-center justify-center rounded-xl text-content-tertiary hover:bg-surface-tertiary focus:outline-none focus-visible:ring-2 focus-visible:ring-stroke-focus">
        <EllipsisVertical className="size-4" aria-hidden="true" />
        <span className="sr-only">Acciones de {matricula.nombre}</span>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-52">
        <DropdownMenuItem
          onClick={() =>
            navigate(
              paths.app.administrativo.matriculaDetalle.getHref(matricula.id),
            )
          }
        >
          <Eye className="mr-2 size-4" aria-hidden="true" /> Ver detalle
        </DropdownMenuItem>
        <DropdownMenuItem disabled>
          Completar matrícula
          <span className="ml-auto text-label-3 text-content-quartiary">
            Disponible pronto
          </span>
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
};

/** Aviso del periodo (Figma A3: abierto = información; cerrado = advertencia). */
const AvisoPeriodo = ({ periodoId }: { periodoId: string }) => {
  const { data: periodo } = usePeriodoMatriculas(periodoId);
  if (!periodo) return null;
  if (periodo.estado === 'CERRADO') {
    return (
      <Alert type="warning" title="El periodo de matrícula está cerrado.">
        Ya no se pueden asignar secciones ni validar matrículas regulares: solo
        registrar matrículas extemporáneas y cambiar de sección a los
        matriculados.
      </Alert>
    );
  }
  if (periodo.estado === 'PROGRAMADO') {
    return (
      <Alert
        type="info"
        title={`El periodo regular inicia el ${formatFechaLima(periodo.inicio)}.`}
      >
        Hasta entonces no se pueden asignar secciones.
      </Alert>
    );
  }
  return (
    <Alert
      type="info"
      title={`El periodo regular está en curso hasta el ${formatFechaLima(periodo.cierre)}.`}
    >
      La matrícula extemporánea se habilita cuando el periodo cierre.
    </Alert>
  );
};

// Figma > Personal Administrativo > A3 · Matrículas del periodo (HU-24, HU-71).
export const MatriculasPeriodo = ({ periodoId }: { periodoId: string }) => {
  const [busqueda, setBusqueda] = React.useState('');
  const [grado, setGrado] = React.useState('');
  const [estado, setEstado] = React.useState('');
  const [orden, setOrden] = React.useState<OrdenMatriculas>('estudiante');
  const [direccion, setDireccion] = React.useState<'asc' | 'desc'>('asc');
  const [page, setPage] = React.useState(1);
  const texto = useDebouncedValue(busqueda.trim());

  // Cualquier cambio de filtro u orden vuelve a la primera página.
  React.useEffect(() => setPage(1), [texto, grado, estado, orden, direccion]);

  const { data: periodo } = usePeriodoMatriculas(periodoId);
  const { data, isError, isLoading } = useMatriculasDelPeriodo(periodoId, {
    texto,
    grado,
    estado,
    orden,
    direccion,
    page,
  });
  const indicadores = periodo?.indicadores;
  const hayFiltros = Boolean(texto || grado || estado);

  if (isError) {
    return (
      <Alert type="error" title="No pudimos cargar las matrículas del periodo.">
        Intenta de nuevo en unos minutos.
      </Alert>
    );
  }

  return (
    <div className="flex flex-col gap-6">
      <AvisoPeriodo periodoId={periodoId} />

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard
          icon={<Users aria-hidden="true" />}
          value={indicadores?.estudiantes ?? 0}
          label="Estudiantes en el periodo"
        />
        <StatCard
          icon={<CircleCheck aria-hidden="true" />}
          iconColor="success"
          value={indicadores?.matriculados ?? 0}
          label="Matriculados"
        />
        <StatCard
          icon={<RefreshCw aria-hidden="true" />}
          value={indicadores?.enProceso ?? 0}
          label="En proceso"
        />
        <StatCard
          icon={<CircleAlert aria-hidden="true" />}
          iconColor="warning"
          value={indicadores?.requierenAccion ?? 0}
          label="Requieren tu acción"
        />
      </div>

      <Card className="p-6">
        <div className="mb-5 flex flex-wrap gap-3">
          <Input
            aria-label="Buscar por nombre, DNI o código"
            placeholder="Buscar por nombre, DNI o código"
            value={busqueda}
            onChange={(event) => setBusqueda(event.target.value)}
            startAdornment={<Search className="size-4" aria-hidden="true" />}
            wrapperClassName="w-full max-w-sm"
          />
          <Select
            aria-label="Filtrar por grado"
            wrapperClassName="w-56"
            value={grado}
            onChange={(event) => setGrado(event.target.value)}
            options={OPCIONES_GRADO}
          />
          <Select
            aria-label="Filtrar por estado"
            wrapperClassName="w-56"
            value={estado}
            onChange={(event) => setEstado(event.target.value)}
            options={OPCIONES_ESTADO}
          />
        </div>

        <Table<FilaMatriculaPeriodo>
          data={isLoading ? [] : (data?.data ?? [])}
          emptyMessage={
            isLoading
              ? 'Cargando matrículas…'
              : hayFiltros
                ? 'No hay matrículas que coincidan con los filtros'
                : 'Aún no hay estudiantes con matrícula en este periodo'
          }
          sort={{
            field: COLUMNA_POR_ORDEN[orden],
            direction: direccion,
            onSort: (campo) => {
              const nuevo = ordenDeColumna(campo);
              if (!nuevo) return;
              if (nuevo === orden) {
                setDireccion(direccion === 'asc' ? 'desc' : 'asc');
              } else {
                setOrden(nuevo);
                setDireccion('asc');
              }
            },
          }}
          columns={[
            {
              title: 'Estudiante',
              field: 'nombre',
              sortable: true,
              Cell: ({ entry }) => (
                <div>
                  <p className="text-content-primary">{entry.nombre}</p>
                  <p className="text-body-4 text-content-tertiary">
                    DNI {entry.documento}
                    {entry.codigo ? ` · ${entry.codigo}` : ''}
                  </p>
                </div>
              ),
            },
            { title: 'Grado', field: 'grado', sortable: true },
            {
              title: 'Sección',
              field: 'seccion',
              sortable: true,
              Cell: ({ entry }) => <span>{entry.seccion ?? '—'}</span>,
            },
            {
              title: 'Estado',
              field: 'estado',
              sortable: true,
              Cell: ({ entry }) => {
                const e = ESTADOS[entry.estado];
                return (
                  <SelectableTag
                    readOnly
                    shape="circle"
                    size="md"
                    color={e?.color ?? 'info'}
                  >
                    {e?.etiqueta ?? entry.estado}
                  </SelectableTag>
                );
              },
            },
            {
              title: '',
              field: 'id',
              Cell: ({ entry }) => <AccionesMatricula matricula={entry} />,
            },
          ]}
        />
        <PaginationBar
          page={data?.meta.page ?? 1}
          size={data?.meta.size ?? 10}
          total={data?.meta.total ?? 0}
          totalPages={data?.meta.totalPages ?? 1}
          onPageChange={setPage}
        />
      </Card>
    </div>
  );
};
