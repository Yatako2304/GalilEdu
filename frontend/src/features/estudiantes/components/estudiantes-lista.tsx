import { Search } from 'lucide-react';
import * as React from 'react';

import { Card } from '@/components/ui/card';
import { Select } from '@/components/ui/form';
import { Link } from '@/components/ui/link';
import { Spinner } from '@/components/ui/spinner';
import { PaginationBar, Table } from '@/components/ui/table';
import { Tag } from '@/components/ui/tag';
import { EstadoEstudiante } from '@/types/api';

import { useEstudiantes } from '../api/estudiantes';

const ESTADOS: Record<
  EstadoEstudiante,
  { label: string; color: 'info' | 'success' | 'neutral' }
> = {
  INSCRITO: { label: 'Inscrito', color: 'info' },
  MATRICULADO: { label: 'Matriculado', color: 'success' },
  INACTIVO: { label: 'Inactivo', color: 'neutral' },
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
}: {
  hrefFicha: (id: string) => string;
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
            className="h-10 w-full rounded-lg border border-stroke-primary bg-white pl-9 pr-3 text-body-3 placeholder:text-content-placeholder focus-visible:border-stroke-focus focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-deep-blue-100"
          />
        </label>
        <Select
          aria-label="Filtrar por estado"
          options={[
            { label: 'Estado: Todos', value: '' },
            { label: 'Inscrito', value: 'INSCRITO' },
            { label: 'Matriculado', value: 'MATRICULADO' },
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
    </>
  );
};
