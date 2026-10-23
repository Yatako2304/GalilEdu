import {
  Copy,
  EllipsisVertical,
  ListPlus,
  Plus,
  SlidersHorizontal,
  Trash2,
  UserPlus,
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
import { Select } from '@/components/ui/form';
import { IconBadge } from '@/components/ui/icon-badge';
import { Spinner } from '@/components/ui/spinner';
import { Table } from '@/components/ui/table';
import { Tag } from '@/components/ui/tag';
import { GRADOS_POR_NIVEL } from '@/config/catalogos';
import { EstadoAnioEscolar, PlanCursoResumen, PlanGrado } from '@/types/api';
import { cn } from '@/utils/cn';

import { useAniosEscolares, usePlanGrado } from '../api/plan-evaluacion';

import { DialogoCursoPlan, ModoCursoPlan } from './dialogo-curso-plan';
import {
  DialogoCopiarPlan,
  DialogoDocentes,
  DialogoQuitarCurso,
} from './dialogos-plan-evaluacion';

const ESTADO_ANIO: Record<
  EstadoAnioEscolar,
  { texto: string; color: 'success' | 'info' | 'neutral' }
> = {
  ACTIVO: { texto: 'Activo', color: 'success' },
  PLANIFICADO: { texto: 'Planificado', color: 'info' },
  CERRADO: { texto: 'Cerrado', color: 'neutral' },
};

const ESCALA_TEXTO: Record<NonNullable<PlanCursoResumen['escala']>, string> = {
  VIGESIMAL: 'Vigesimal',
  CUALITATIVA: 'Cualitativa',
  MIXTA: 'Mixta',
};

type Nivel = 'PRIMARIA' | 'SECUNDARIA';

type Modal =
  | { tipo: 'curso'; modo: ModoCursoPlan }
  | { tipo: 'docentes'; curso: PlanCursoResumen }
  | { tipo: 'quitar'; curso: PlanCursoResumen }
  | { tipo: 'copiar' };

const plural = (n: number, uno: string, varios: string) =>
  `${n} ${n === 1 ? uno : varios}`;

// Figma > Personal Administrativo > "CI-17 · Competencias › Plan de evaluación — 1.° de secundaria 2026 (HU-56)"
// y "CI-20 · Plan de evaluación — 3.° de secundaria 2027 sin cursos (estado vacío)"
export const PlanEvaluacion = () => {
  const anios = useAniosEscolares();
  const [anioId, setAnioId] = React.useState('');
  const [nivel, setNivel] = React.useState<Nivel>('SECUNDARIA');
  const [grado, setGrado] = React.useState('SEC-1');
  const [modal, setModal] = React.useState<Modal | null>(null);

  // Por defecto, el año activo (o el más reciente).
  const anioElegido =
    anioId ||
    anios.data?.find((a) => a.estado === 'ACTIVO')?.id ||
    anios.data?.[0]?.id ||
    '';
  const anio = anios.data?.find((a) => a.id === anioElegido);
  const plan = usePlanGrado(
    anioElegido ? { anioEscolarId: anioElegido, grado } : null,
  );
  const editable = anio?.estado !== 'CERRADO';

  const cambiarNivel = (n: Nivel) => {
    setNivel(n);
    setGrado(GRADOS_POR_NIVEL[n][0].value as string);
  };

  if (anios.isLoading) {
    return (
      <div className="flex h-60 items-center justify-center">
        <Spinner />
      </div>
    );
  }
  if (!anios.data?.length) {
    return (
      <Card className="p-8 text-center text-body-3 text-content-tertiary">
        Aún no hay años escolares. Crea uno en Año escolar.
      </Card>
    );
  }

  return (
    <>
      <Card className="flex flex-wrap items-center gap-3 px-4 py-3">
        <Select
          aria-label="Año escolar"
          wrapperClassName="w-44"
          className="h-9 text-body-4"
          options={anios.data.map((a) => ({
            label: `Año escolar ${a.anio}`,
            value: a.id,
          }))}
          value={anioElegido}
          onChange={(e) => setAnioId(e.target.value)}
        />
        {anio && (
          <Tag color={ESTADO_ANIO[anio.estado].color} size="sm">
            {ESTADO_ANIO[anio.estado].texto}
          </Tag>
        )}
        <div className="flex" role="group" aria-label="Nivel">
          {(['PRIMARIA', 'SECUNDARIA'] as Nivel[]).map((n) => (
            <Chip key={n} activo={nivel === n} onClick={() => cambiarNivel(n)}>
              {n === 'PRIMARIA' ? 'Primaria' : 'Secundaria'}
            </Chip>
          ))}
        </div>
        <div className="flex flex-wrap gap-2" role="group" aria-label="Grado">
          {GRADOS_POR_NIVEL[nivel].map((g) => (
            <Chip
              key={g.value}
              activo={grado === g.value}
              onClick={() => setGrado(g.value as string)}
            >
              {g.label}
            </Chip>
          ))}
        </div>
      </Card>

      {plan.isLoading || !plan.data ? (
        <div className="flex h-60 items-center justify-center">
          <Spinner />
        </div>
      ) : (
        <ContenidoPlan
          plan={plan.data}
          editable={editable}
          onModal={setModal}
        />
      )}

      {plan.data && modal?.tipo === 'curso' && (
        <DialogoCursoPlan
          plan={plan.data}
          modo={modal.modo}
          onCerrar={() => setModal(null)}
        />
      )}
      {plan.data && modal?.tipo === 'docentes' && (
        <DialogoDocentes
          plan={plan.data}
          curso={modal.curso}
          onCerrar={() => setModal(null)}
        />
      )}
      {plan.data && modal?.tipo === 'quitar' && (
        <DialogoQuitarCurso
          plan={plan.data}
          curso={modal.curso}
          onCerrar={() => setModal(null)}
        />
      )}
      {plan.data && modal?.tipo === 'copiar' && (
        <DialogoCopiarPlan plan={plan.data} onCerrar={() => setModal(null)} />
      )}
    </>
  );
};

const Chip = ({
  activo,
  onClick,
  children,
}: {
  activo: boolean;
  onClick: () => void;
  children: React.ReactNode;
}) => (
  <button
    type="button"
    aria-pressed={activo}
    onClick={onClick}
    className={cn(
      'rounded-lg border px-3 py-1 text-body-4 transition-colors',
      activo
        ? 'border-transparent bg-deep-blue-800 text-white'
        : 'border-stroke-accent-primary bg-white text-content-accent-secondary hover:bg-surface-accent-light',
    )}
  >
    {children}
  </button>
);

const ContenidoPlan = ({
  plan,
  editable,
  onModal,
}: {
  plan: PlanGrado;
  editable: boolean;
  onModal: (m: Modal) => void;
}) => {
  const vacio = plan.cursos.length === 0;
  const tarjetas = [
    {
      titulo: 'Cursos que se dictan',
      valor: plan.cursos.length,
      detalle: `en ${plan.grado}`,
    },
    {
      titulo: 'Competencias evaluadas',
      valor: plan.competenciasEvaluadas,
      detalle: 'sin repetir',
    },
    {
      titulo: 'Configuración',
      valor: `${plan.cursosConfigurados} / ${plan.cursos.length}`,
      detalle: 'cursos configurados',
    },
    {
      titulo: 'Docentes asignados',
      valor: `${plan.docentesAsignados} / ${plan.docentesRequeridos}`,
      detalle: `${plural(plan.secciones.length, 'sección', 'secciones')} × ${plural(plan.cursos.length, 'curso', 'cursos')}`,
    },
  ];

  return (
    <>
      {!vacio && (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {tarjetas.map((t) => (
            <Card key={t.titulo} className="px-5 py-4">
              <p className="text-body-4 text-content-tertiary">{t.titulo}</p>
              <p className="mt-1 text-h6 font-semibold text-content-primary">
                {t.valor}
              </p>
              <p className="text-body-4 text-content-quartiary">{t.detalle}</p>
            </Card>
          ))}
        </div>
      )}

      <div className="flex flex-wrap items-center justify-between gap-3">
        <h2 className="text-h6 font-semibold text-content-primary">
          {plan.grado} · {plan.anio}
        </h2>
        {editable && (
          <div className="flex gap-2">
            {vacio && (
              <Button
                size="sm"
                variant="secondary"
                icon={<Copy className="size-4" />}
                onClick={() => onModal({ tipo: 'copiar' })}
              >
                Copiar plan
              </Button>
            )}
            <Button
              size="sm"
              icon={<Plus className="size-4" />}
              onClick={() =>
                onModal({ tipo: 'curso', modo: { tipo: 'agregar' } })
              }
            >
              Agregar curso
            </Button>
          </div>
        )}
      </div>

      {vacio ? (
        <Card className="flex flex-col items-center gap-3 px-6 py-12 text-center">
          <IconBadge size="lg">
            <ListPlus aria-hidden="true" />
          </IconBadge>
          <p className="text-label-1 font-semibold text-content-primary">
            {plan.grado} aún no tiene cursos en {plan.anio}
          </p>
          <p className="text-body-3 text-content-tertiary">
            Agrega los cursos uno por uno o copia el plan de otro grado o de
            otro año.
          </p>
          {editable && (
            <div className="mt-2 flex gap-2">
              <Button
                size="sm"
                variant="secondary"
                icon={<Copy className="size-4" />}
                onClick={() => onModal({ tipo: 'copiar' })}
              >
                Copiar plan de otro grado
              </Button>
              <Button
                size="sm"
                icon={<Plus className="size-4" />}
                onClick={() =>
                  onModal({ tipo: 'curso', modo: { tipo: 'agregar' } })
                }
              >
                Agregar curso
              </Button>
            </div>
          )}
        </Card>
      ) : (
        <Card className="p-5">
          <Table
            data={plan.cursos}
            columns={[
              {
                title: 'Curso',
                field: 'curso',
                Cell: ({ entry }) => (
                  <div>
                    <p className="text-body-3 text-content-primary">
                      {entry.curso}
                    </p>
                    <p className="text-body-4 text-content-tertiary">
                      {entry.area}
                    </p>
                  </div>
                ),
              },
              {
                title: 'Horas sem.',
                field: 'horasSemanales',
                Cell: ({ entry }) => <span>{entry.horasSemanales} h</span>,
              },
              {
                title: 'Competencias',
                field: 'competencias',
                Cell: ({ entry }) => (
                  <span>
                    {plural(entry.competencias, 'competencia', 'competencias')}
                  </span>
                ),
              },
              {
                title: 'Escala',
                field: 'escala',
                Cell: ({ entry }) => (
                  <span>{entry.escala ? ESCALA_TEXTO[entry.escala] : '—'}</span>
                ),
              },
              {
                title: 'Docentes',
                field: 'docentesAsignados',
                Cell: ({ entry }) => (
                  <span>
                    {entry.docentesAsignados} / {plan.secciones.length}
                  </span>
                ),
              },
              {
                title: 'Estado',
                field: 'id',
                Cell: ({ entry }) =>
                  entry.competencias > 0 ? (
                    <Tag color="success" size="sm">
                      Configurado
                    </Tag>
                  ) : (
                    <Tag color="warning" size="sm">
                      Sin configurar
                    </Tag>
                  ),
              },
              {
                title: '',
                field: 'cursoId',
                Cell: ({ entry }) =>
                  editable ? (
                    <AccionesCursoPlan curso={entry} onModal={onModal} />
                  ) : (
                    <span className="sr-only">Año cerrado</span>
                  ),
              },
            ]}
          />
        </Card>
      )}
    </>
  );
};

const AccionesCursoPlan = ({
  curso,
  onModal,
}: {
  curso: PlanCursoResumen;
  onModal: (m: Modal) => void;
}) => (
  <DropdownMenu>
    <DropdownMenuTrigger className="inline-flex size-8 items-center justify-center rounded-xl text-content-tertiary hover:bg-surface-tertiary focus:outline-none focus-visible:ring-2 focus-visible:ring-stroke-focus">
      <EllipsisVertical className="size-4" aria-hidden="true" />
      <span className="sr-only">Acciones de {curso.curso}</span>
    </DropdownMenuTrigger>
    <DropdownMenuContent align="end" className="w-56">
      <DropdownMenuItem
        onClick={() =>
          onModal({
            tipo: 'curso',
            modo: { tipo: 'configurar', planCursoId: curso.id },
          })
        }
      >
        <SlidersHorizontal className="mr-2 size-4" aria-hidden="true" />
        Configurar competencias
      </DropdownMenuItem>
      <DropdownMenuItem onClick={() => onModal({ tipo: 'docentes', curso })}>
        <UserPlus className="mr-2 size-4" aria-hidden="true" /> Asignar docentes
      </DropdownMenuItem>
      <DropdownMenuItem
        onClick={() => onModal({ tipo: 'quitar', curso })}
        className="text-content-error focus:text-content-error"
      >
        <Trash2 className="mr-2 size-4" aria-hidden="true" /> Quitar del plan
      </DropdownMenuItem>
    </DropdownMenuContent>
  </DropdownMenu>
);
