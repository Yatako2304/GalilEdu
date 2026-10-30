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
import { Spinner } from '@/components/ui/spinner';
import { StatCard } from '@/components/ui/stat-card';
import { PaginationBar, Table } from '@/components/ui/table';
import { SelectableTag } from '@/components/ui/tag';
import { paths } from '@/config/paths';
import { useMatriculasDelPeriodo } from '@/features/matricula/api/matriculas-administrativo';
import { EstadoMatricula, MatriculaAdministrativa } from '@/types/api';

import { seccionMatricula } from './presentacion-matricula';

const ESTADOS: Record<
  EstadoMatricula,
  {
    etiqueta: string;
    color: 'info' | 'warning' | 'success' | 'error';
  }
> = {
  SECCION_RESERVADA: { etiqueta: 'En proceso', color: 'info' },
  EN_VALIDACION: { etiqueta: 'En validación', color: 'info' },
  PENDIENTE_PAGO: { etiqueta: 'Pendiente de pago', color: 'warning' },
  MATRICULADO: { etiqueta: 'Matriculado', color: 'success' },
  RETIRADO: { etiqueta: 'Retirado', color: 'error' },
  ANULADA: { etiqueta: 'Anulada', color: 'error' },
};

const AccionesMatricula = ({
  matricula,
  periodoId,
}: {
  matricula: MatriculaAdministrativa;
  periodoId: string;
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
              `${paths.app.administrativo.matriculaDetalle.getHref(matricula.id)}?periodoId=${encodeURIComponent(periodoId)}`,
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

// Figma > Personal Administrativo > A3 · Matrículas del periodo.
export const MatriculasPeriodo = ({
  periodoId,
  cierre,
}: {
  periodoId: string;
  cierre?: string | null;
}) => {
  const consulta = useMatriculasDelPeriodo(periodoId);
  const [busqueda, setBusqueda] = React.useState('');
  const [estado, setEstado] = React.useState('TODOS');
  const texto = busqueda.trim().toLocaleLowerCase('es-PE');
  const todas = consulta.data ?? [];
  const matriculas = todas.filter(
    (matricula) =>
      (estado === 'TODOS' || matricula.estado === estado) &&
      (!texto ||
        `${matricula.nombre} ${matricula.numeroDocumento} ${matricula.codigo ?? ''}`
          .toLocaleLowerCase('es-PE')
          .includes(texto)),
  );
  const matriculados = todas.filter(
    (matricula) => matricula.estado === 'MATRICULADO',
  ).length;
  const enProceso = todas.filter((matricula) =>
    ['SECCION_RESERVADA', 'EN_VALIDACION', 'PENDIENTE_PAGO'].includes(
      matricula.estado,
    ),
  ).length;

  if (consulta.isLoading) {
    return (
      <div className="flex min-h-64 items-center justify-center">
        <Spinner size="lg" />
      </div>
    );
  }

  if (consulta.isError) {
    return (
      <Alert type="error" title="No pudimos cargar las matrículas del periodo.">
        Intenta nuevamente en unos minutos.
      </Alert>
    );
  }

  return (
    <div className="flex flex-col gap-6">
      {cierre && (
        <Alert type="info" title={`El periodo está en curso hasta ${cierre}.`}>
          La matrícula extemporánea se habilita cuando el periodo cierre.
        </Alert>
      )}

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard
          icon={<Users aria-hidden="true" />}
          value={todas.length}
          label="Estudiantes en el periodo"
        />
        <StatCard
          icon={<CircleCheck aria-hidden="true" />}
          iconColor="success"
          value={matriculados}
          label="Matriculados"
        />
        <StatCard
          icon={<RefreshCw aria-hidden="true" />}
          value={enProceso}
          label="En proceso"
        />
        <StatCard
          icon={<CircleAlert aria-hidden="true" />}
          iconColor="warning"
          value={enProceso}
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
            aria-label="Filtrar por estado"
            wrapperClassName="w-56"
            value={estado}
            onChange={(event) => setEstado(event.target.value)}
            options={[
              { label: 'Estado: Todos', value: 'TODOS' },
              { label: 'En proceso', value: 'SECCION_RESERVADA' },
              { label: 'En validación', value: 'EN_VALIDACION' },
              { label: 'Pendiente de pago', value: 'PENDIENTE_PAGO' },
              { label: 'Matriculado', value: 'MATRICULADO' },
              { label: 'Retirado', value: 'RETIRADO' },
              { label: 'Anulada', value: 'ANULADA' },
            ]}
          />
        </div>

        <Table<MatriculaAdministrativa>
          data={matriculas}
          emptyMessage="No hay matrículas que coincidan con los filtros"
          columns={[
            {
              title: 'Estudiante',
              field: 'nombre',
              Cell: ({ entry }) => (
                <div>
                  <p className="text-content-primary">{entry.nombre}</p>
                  <p className="text-body-4 text-content-tertiary">
                    DNI {entry.numeroDocumento}
                    {entry.codigo ? ` · ${entry.codigo}` : ''}
                  </p>
                </div>
              ),
            },
            { title: 'Grado', field: 'grado' },
            {
              title: 'Sección',
              field: 'seccion',
              Cell: ({ entry }) => (
                <span>{seccionMatricula(entry.estado, entry.seccion)}</span>
              ),
            },
            {
              title: 'Estado',
              field: 'estado',
              Cell: ({ entry }) => (
                <SelectableTag
                  readOnly
                  shape="circle"
                  size="md"
                  color={ESTADOS[entry.estado].color}
                >
                  {ESTADOS[entry.estado].etiqueta}
                </SelectableTag>
              ),
            },
            {
              title: '',
              field: 'id',
              Cell: ({ entry }) => (
                <AccionesMatricula matricula={entry} periodoId={periodoId} />
              ),
            },
          ]}
        />
        <PaginationBar
          page={1}
          size={Math.max(matriculas.length, 1)}
          total={matriculas.length}
          totalPages={1}
          onPageChange={() => {}}
        />
      </Card>
    </div>
  );
};
