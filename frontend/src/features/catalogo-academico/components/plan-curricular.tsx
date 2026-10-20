import {
  BookOpen,
  CircleCheck,
  CircleOff,
  CirclePlus,
  EllipsisVertical,
  Layers,
  Pencil,
  Search,
  Target,
} from 'lucide-react';
import * as React from 'react';

import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown';
import { Spinner } from '@/components/ui/spinner';
import { Table } from '@/components/ui/table';
import { Status, Tag } from '@/components/ui/tag';
import {
  AreaDetalle,
  AreaResumen,
  CompetenciaResumen,
  CursoResumen,
} from '@/types/api';
import { cn } from '@/utils/cn';

import {
  useArea,
  useAreas,
  useResumenPlanCurricular,
} from '../api/plan-curricular';

import { CambioEstado, DialogoCambiarEstado } from './dialogo-cambiar-estado';
import {
  DialogoArea,
  DialogoCompetencia,
  DialogoCurso,
} from './dialogos-plan-curricular';
import { textoGrados } from './esquemas';

type Modal =
  | { tipo: 'area'; area: AreaDetalle | null }
  | { tipo: 'competencia'; competencia: CompetenciaResumen | null }
  | { tipo: 'curso'; curso: CursoResumen | null };

const sinTildes = (texto: string) =>
  texto.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();

const plural = (n: number, singular: string, varios: string) =>
  `${n} ${n === 1 ? singular : varios}`;

// Figma > Personal Administrativo > "CI-14 · Competencias › Plan curricular: áreas, competencias y cursos (HU-52, 53, 54)"
export const PlanCurricular = () => {
  const resumen = useResumenPlanCurricular();
  const areas = useAreas();
  const [busqueda, setBusqueda] = React.useState('');
  const [elegida, setElegida] = React.useState<string | null>(null);
  const [modal, setModal] = React.useState<Modal | null>(null);
  const [cambio, setCambio] = React.useState<CambioEstado | null>(null);

  // Al cargar, se muestra la primera área (Figma: Matemática seleccionada).
  const idArea = elegida ?? areas.data?.[0]?.id ?? null;
  const area = useArea(idArea);

  const filtradas = (areas.data ?? []).filter((a) =>
    sinTildes(a.nombre).includes(sinTildes(busqueda.trim())),
  );
  const anio = resumen.data?.anioActivo;

  return (
    <>
      <ResumenTarjetas />

      <div className="grid gap-6 lg:grid-cols-[300px_1fr]">
        <div className="flex flex-col gap-3">
          <label className="relative">
            <span className="sr-only">Buscar área</span>
            <Search
              className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-content-tertiary"
              aria-hidden="true"
            />
            <input
              value={busqueda}
              onChange={(e) => setBusqueda(e.target.value)}
              placeholder="Buscar área"
              className="h-10 w-full rounded-lg border border-stroke-primary bg-white pl-9 pr-3 text-body-3 placeholder:text-content-placeholder focus-visible:border-stroke-focus focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-deep-blue-100"
            />
          </label>
          {areas.isLoading ? (
            <div className="flex h-40 items-center justify-center">
              <Spinner />
            </div>
          ) : (
            <ul aria-label="Áreas curriculares" className="flex flex-col gap-2">
              {filtradas.map((a) => (
                <li key={a.id}>
                  <FilaArea
                    area={a}
                    seleccionada={a.id === idArea}
                    onElegir={() => setElegida(a.id)}
                  />
                </li>
              ))}
              {filtradas.length === 0 && (
                <li className="py-6 text-center text-body-3 text-content-tertiary">
                  No hay áreas con ese nombre
                </li>
              )}
            </ul>
          )}
        </div>

        <div className="flex flex-col gap-6">
          <div className="flex justify-end">
            <Button
              icon={<CirclePlus className="size-4" />}
              onClick={() => setModal({ tipo: 'area', area: null })}
            >
              Nueva área
            </Button>
          </div>
          {area.isLoading || !area.data ? (
            idArea && (
              <div className="flex h-60 items-center justify-center">
                <Spinner />
              </div>
            )
          ) : (
            <DetalleArea
              area={area.data}
              anio={anio}
              onModal={setModal}
              onCambio={setCambio}
            />
          )}
        </div>
      </div>

      {modal?.tipo === 'area' && (
        <DialogoArea
          area={modal.area}
          onCerrar={() => setModal(null)}
          onGuardada={setElegida}
        />
      )}
      {modal?.tipo === 'competencia' && area.data && (
        <DialogoCompetencia
          area={area.data}
          competencia={modal.competencia}
          onCerrar={() => setModal(null)}
        />
      )}
      {modal?.tipo === 'curso' && area.data && (
        <DialogoCurso
          area={area.data}
          curso={modal.curso}
          onCerrar={() => setModal(null)}
        />
      )}
      <DialogoCambiarEstado cambio={cambio} onCerrar={() => setCambio(null)} />
    </>
  );
};

const ResumenTarjetas = () => {
  const resumen = useResumenPlanCurricular();
  const r = resumen.data;
  const tarjetas = [
    {
      Icono: Layers,
      titulo: 'Áreas activas',
      valor: r?.areasActivas,
      detalle: r && `de ${r.areasRegistradas} registradas`,
    },
    {
      Icono: Target,
      titulo: 'Competencias activas',
      valor: r?.competenciasActivas,
      detalle: r && `de ${r.competenciasRegistradas} registradas`,
    },
    {
      Icono: BookOpen,
      titulo: 'Cursos activos',
      valor: r?.cursosActivos,
      detalle: r && `de ${r.cursosRegistrados} registrados`,
    },
  ];
  return (
    <div className="grid gap-4 sm:grid-cols-3">
      {tarjetas.map(({ Icono, titulo, valor, detalle }) => (
        <Card key={titulo} className="px-5 py-4">
          <p className="flex items-center gap-2 text-body-4 text-content-tertiary">
            <Icono className="size-4" aria-hidden="true" />
            {titulo}
          </p>
          <p className="mt-1 text-h6 font-semibold text-content-primary">
            {valor ?? '—'}
          </p>
          <p className="text-body-4 text-content-quartiary">{detalle}</p>
        </Card>
      ))}
    </div>
  );
};

const FilaArea = ({
  area,
  seleccionada,
  onElegir,
}: {
  area: AreaResumen;
  seleccionada: boolean;
  onElegir: () => void;
}) => (
  <button
    type="button"
    onClick={onElegir}
    aria-current={seleccionada}
    className={cn(
      'flex w-full items-center justify-between gap-2 rounded-lg border px-4 py-3 text-left transition-colors',
      seleccionada
        ? 'border-stroke-accent-primary bg-surface-accent-light'
        : 'border-stroke-tertiary bg-white hover:bg-surface-secondary',
    )}
  >
    <span className="min-w-0">
      <span className="block truncate text-label-2 font-medium text-content-primary">
        {area.nombre}
      </span>
      <span className="block text-body-4 text-content-tertiary">
        {plural(area.competencias, 'competencia', 'competencias')} ·{' '}
        {plural(area.cursos, 'curso', 'cursos')}
      </span>
    </span>
    {area.estado === 'INACTIVA' && (
      <Tag color="neutral" size="sm">
        Inactiva
      </Tag>
    )}
  </button>
);

const DetalleArea = ({
  area,
  anio,
  onModal,
  onCambio,
}: {
  area: AreaDetalle;
  anio: number | null | undefined;
  onModal: (m: Modal) => void;
  onCambio: (c: CambioEstado) => void;
}) => {
  const activa = area.estado === 'ACTIVA';
  return (
    <>
      <Card className="flex flex-wrap items-start justify-between gap-4 p-5">
        <div className="min-w-0 flex-1">
          <div className="flex items-center gap-2">
            <h2 className="text-h6 font-semibold text-content-primary">
              {area.nombre}
            </h2>
            {!activa && (
              <Tag color="neutral" size="sm">
                Inactiva
              </Tag>
            )}
          </div>
          {area.descripcion && (
            <p className="mt-1 text-body-3 text-content-tertiary">
              {area.descripcion}
            </p>
          )}
        </div>
        <div className="flex gap-2">
          <Button
            size="sm"
            icon={<Pencil className="size-4" />}
            onClick={() => onModal({ tipo: 'area', area })}
          >
            Editar área
          </Button>
          <Button
            size="sm"
            variant={activa ? 'secondary' : 'primary-light'}
            icon={
              activa ? (
                <CircleOff className="size-4" />
              ) : (
                <CircleCheck className="size-4" />
              )
            }
            onClick={() =>
              onCambio({
                recurso: 'areas',
                id: area.id,
                nombre: area.nombre,
                accion: activa ? 'desactivar' : 'reactivar',
              })
            }
          >
            {activa ? 'Desactivar' : 'Reactivar'}
          </Button>
        </div>
      </Card>

      <Card className="p-5">
        <EncabezadoSeccion
          titulo="Competencias"
          descripcion="Lo que se evalúa en los estudiantes dentro del área."
          textoBoton="Agregar competencia"
          deshabilitado={!activa}
          onAgregar={() => onModal({ tipo: 'competencia', competencia: null })}
        />
        {area.competencias.length === 0 ? (
          <p className="py-6 text-center text-body-3 text-content-tertiary">
            El área aún no tiene competencias.
          </p>
        ) : (
          <ol className="mt-4 flex flex-col gap-3">
            {area.competencias.map((c, i) => (
              <FilaCompetencia
                key={c.id}
                numero={i + 1}
                competencia={c}
                onEditar={() =>
                  onModal({ tipo: 'competencia', competencia: c })
                }
                onCambio={onCambio}
              />
            ))}
          </ol>
        )}
      </Card>

      <Card className="p-5">
        <EncabezadoSeccion
          titulo="Cursos"
          descripcion="Asignaturas del área que se pueden dictar en los grados."
          textoBoton="Agregar curso"
          deshabilitado={!activa}
          onAgregar={() => onModal({ tipo: 'curso', curso: null })}
        />
        <div className="mt-4">
          <Table
            data={area.cursos}
            emptyMessage="El área aún no tiene cursos."
            columns={[
              {
                title: 'Curso',
                field: 'nombre',
                Cell: ({ entry }) => (
                  <span className="text-body-3 text-content-primary">
                    {entry.nombre}
                  </span>
                ),
              },
              {
                title: anio ? `Se dicta en ${anio}` : 'Se dicta en',
                field: 'grados',
                Cell: ({ entry }) => (
                  <span className="text-body-4 text-content-secondary">
                    {entry.estado === 'ACTIVO'
                      ? textoGrados(entry.grados)
                      : anio
                        ? `No se dicta en ${anio}`
                        : 'No se dicta'}
                  </span>
                ),
              },
              {
                title: 'Estado',
                field: 'estado',
                Cell: ({ entry }) => (
                  <Status active={entry.estado === 'ACTIVO'} />
                ),
              },
              {
                title: '',
                field: 'id',
                Cell: ({ entry }) => (
                  <AccionesCurso
                    curso={entry}
                    onEditar={() => onModal({ tipo: 'curso', curso: entry })}
                    onCambio={onCambio}
                  />
                ),
              },
            ]}
          />
        </div>
      </Card>
    </>
  );
};

const EncabezadoSeccion = ({
  titulo,
  descripcion,
  textoBoton,
  deshabilitado,
  onAgregar,
}: {
  titulo: string;
  descripcion: string;
  textoBoton: string;
  deshabilitado: boolean;
  onAgregar: () => void;
}) => (
  <div className="flex flex-wrap items-start justify-between gap-3">
    <div>
      <h3 className="text-label-1 font-semibold text-content-primary">
        {titulo}
      </h3>
      <p className="text-body-4 text-content-tertiary">{descripcion}</p>
    </div>
    <Button
      size="sm"
      variant="primary-light"
      icon={<CirclePlus className="size-4" />}
      disabled={deshabilitado}
      title={deshabilitado ? 'Reactiva el área para agregar' : undefined}
      onClick={onAgregar}
    >
      {textoBoton}
    </Button>
  </div>
);

const FilaCompetencia = ({
  numero,
  competencia,
  onEditar,
  onCambio,
}: {
  numero: number;
  competencia: CompetenciaResumen;
  onEditar: () => void;
  onCambio: (c: CambioEstado) => void;
}) => {
  const activa = competencia.estado === 'ACTIVA';
  return (
    <li className="flex items-start gap-3 rounded-lg border border-stroke-tertiary px-4 py-3">
      <span className="inline-flex size-6 shrink-0 items-center justify-center rounded-full bg-surface-accent-light text-caption-1 font-semibold text-content-accent-primary">
        {numero}
      </span>
      <div className="min-w-0 flex-1">
        <p
          className={cn(
            'text-label-2 font-medium',
            activa ? 'text-content-primary' : 'text-content-quartiary',
          )}
        >
          {competencia.nombre}
          {!activa && (
            <Tag color="neutral" size="sm" className="ml-2 align-middle">
              Inactiva
            </Tag>
          )}
        </p>
        {competencia.descripcion && (
          <p className="text-body-4 text-content-tertiary">
            {competencia.descripcion}
          </p>
        )}
      </div>
      <div className="flex gap-2">
        <button
          type="button"
          onClick={onEditar}
          className="inline-flex size-8 items-center justify-center rounded-lg border border-stroke-tertiary text-content-secondary hover:bg-surface-tertiary"
        >
          <Pencil className="size-4" aria-hidden="true" />
          <span className="sr-only">Editar {competencia.nombre}</span>
        </button>
        <button
          type="button"
          onClick={() =>
            onCambio({
              recurso: 'competencias',
              id: competencia.id,
              nombre: competencia.nombre,
              accion: activa ? 'desactivar' : 'reactivar',
            })
          }
          className={cn(
            'inline-flex size-8 items-center justify-center rounded-lg',
            activa
              ? 'bg-surface-error-light text-content-error'
              : 'bg-surface-success-light text-content-success',
          )}
        >
          {activa ? (
            <CircleOff className="size-4" aria-hidden="true" />
          ) : (
            <CircleCheck className="size-4" aria-hidden="true" />
          )}
          <span className="sr-only">
            {activa ? 'Desactivar' : 'Reactivar'} {competencia.nombre}
          </span>
        </button>
      </div>
    </li>
  );
};

const AccionesCurso = ({
  curso,
  onEditar,
  onCambio,
}: {
  curso: CursoResumen;
  onEditar: () => void;
  onCambio: (c: CambioEstado) => void;
}) => {
  const activo = curso.estado === 'ACTIVO';
  return (
    <DropdownMenu>
      <DropdownMenuTrigger className="inline-flex size-8 items-center justify-center rounded-lg text-content-tertiary hover:bg-surface-tertiary focus:outline-none focus-visible:ring-2 focus-visible:ring-stroke-focus">
        <EllipsisVertical className="size-4" aria-hidden="true" />
        <span className="sr-only">Acciones de {curso.nombre}</span>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-40">
        <DropdownMenuItem onClick={onEditar}>
          <Pencil className="mr-2 size-4" aria-hidden="true" /> Editar
        </DropdownMenuItem>
        <DropdownMenuItem
          onClick={() =>
            onCambio({
              recurso: 'cursos',
              id: curso.id,
              nombre: curso.nombre,
              accion: activo ? 'desactivar' : 'reactivar',
            })
          }
          className={
            activo ? 'text-content-error focus:text-content-error' : ''
          }
        >
          {activo ? (
            <CircleOff className="mr-2 size-4" aria-hidden="true" />
          ) : (
            <CircleCheck className="mr-2 size-4" aria-hidden="true" />
          )}
          {activo ? 'Desactivar' : 'Reactivar'}
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
};
