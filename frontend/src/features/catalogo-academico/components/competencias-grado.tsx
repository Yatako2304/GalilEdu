import { Pencil, Percent } from 'lucide-react';
import * as React from 'react';

import { Alert } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Input } from '@/components/ui/form';
import { Spinner } from '@/components/ui/spinner';
import { Table } from '@/components/ui/table';
import { Tag } from '@/components/ui/tag';
import { PesosCompetencia, PlanGrado, ReglaNota } from '@/types/api';

import {
  useGuardarPesosCompetencia,
  usePesosCompetencias,
} from '../api/plan-evaluacion';

import { DialogoCatalogo } from './dialogo-catalogo';

const NOMBRE_REGLA: Record<ReglaNota, string> = {
  PROMEDIO_SIMPLE: 'promedio simple',
  PROMEDIO_PONDERADO: 'promedio ponderado',
  NIVEL_MAS_FRECUENTE: 'nivel más frecuente',
  ULTIMO_NIVEL: 'último nivel',
};

/** "40" o "33,33": como se escribe un porcentaje en pantalla. */
export const porcentaje = (valor: number) =>
  `${Number(valor.toFixed(2)).toString().replace('.', ',')} %`;

type FilaCompetencia = PesosCompetencia & { id: string };

type Props = { plan: PlanGrado; editable: boolean };

/**
 * Vista "Competencias del grado" (debajo de los cursos del plan). La nota oficial es de la competencia: una por
 * periodo y una final. Cuando varios cursos evalúan la misma competencia, aquí se define cuánto aporta cada uno.
 * Los pesos se usan con promedio ponderado (ponderan las notas) y en la escala cualitativa (pesan el voto de cada
 * curso); con promedio simple los cursos aportan por igual.
 */
export const CompetenciasGrado = ({ plan, editable }: Props) => {
  const pesos = usePesosCompetencias({
    anioEscolarId: plan.anioEscolarId,
    grado: plan.gradoCodigo,
  });
  const [editando, setEditando] = React.useState<PesosCompetencia | null>(null);
  const usaPesos = plan.reglaNota !== 'PROMEDIO_SIMPLE';
  const cualitativa =
    plan.reglaNota === 'NIVEL_MAS_FRECUENTE' ||
    plan.reglaNota === 'ULTIMO_NIVEL';
  const filas: FilaCompetencia[] = (pesos.data ?? []).map((p) => ({
    ...p,
    id: p.competenciaId,
  }));

  return (
    <Card className="p-5">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h3 className="text-label-1 font-semibold text-content-primary">
            Competencias del grado
          </h3>
          <p className="mt-1 max-w-3xl text-body-4 text-content-tertiary">
            La nota oficial es de la competencia: una por periodo y una final.
            Si varios cursos evalúan la misma competencia, cada uno aporta una
            parte de esa nota.
          </p>
        </div>
        {usaPesos && plan.pesosIncompletos > 0 && (
          <Tag color="warning" size="sm">
            {plan.pesosIncompletos === 1
              ? '1 competencia con pesos incompletos'
              : `${plan.pesosIncompletos} competencias con pesos incompletos`}
          </Tag>
        )}
      </div>

      {!usaPesos && (
        <Alert
          className="mt-4"
          type="info"
          size="sm"
          title={`El año usa ${NOMBRE_REGLA[plan.reglaNota]}: los cursos de una competencia aportan por igual.`}
        >
          Los pesos por curso se usan con promedio ponderado y en la escala de
          letras.
        </Alert>
      )}
      {cualitativa && (
        <Alert
          className="mt-4"
          type="info"
          size="sm"
          title="El año usa escala de letras: cada curso vota por su nivel con su peso."
        >
          Gana el nivel con más peso acumulado; si empatan, el nivel mayor.
        </Alert>
      )}

      <div className="mt-4">
        {pesos.isLoading ? (
          <div className="flex h-24 items-center justify-center">
            <Spinner />
          </div>
        ) : filas.length === 0 ? (
          <p className="text-body-3 text-content-tertiary">
            Ningún curso del grado evalúa competencias todavía. Configura las
            competencias de cada curso.
          </p>
        ) : (
          <Table
            data={filas}
            columns={[
              {
                title: 'Competencia',
                field: 'competencia',
                Cell: ({ entry }) => (
                  <div>
                    <p className="text-body-3 text-content-primary">
                      {entry.competencia}
                    </p>
                    <p className="text-body-4 text-content-tertiary">
                      {entry.area}
                    </p>
                  </div>
                ),
              },
              {
                title: 'Cursos que la evalúan',
                field: 'cursos',
                Cell: ({ entry }) => (
                  <ul className="flex flex-wrap gap-2">
                    {entry.cursos.map((c) => (
                      <li key={c.planCursoId}>
                        <Tag color="neutral" size="sm">
                          {usaPesos
                            ? `${c.curso} · ${c.pesoCurso == null ? 'sin peso' : porcentaje(c.pesoCurso)}`
                            : c.curso}
                        </Tag>
                      </li>
                    ))}
                  </ul>
                ),
              },
              {
                title: 'Estado',
                field: 'completo',
                Cell: ({ entry }) =>
                  entry.cursos.length === 1 ? (
                    <Tag color="info" size="sm">
                      Un solo curso
                    </Tag>
                  ) : !usaPesos ? (
                    <Tag color="info" size="sm">
                      Aportan por igual
                    </Tag>
                  ) : entry.completo ? (
                    <Tag color="success" size="sm">
                      Suma 100 %
                    </Tag>
                  ) : (
                    <Tag color="warning" size="sm">
                      Suma {porcentaje(entry.total)}
                    </Tag>
                  ),
              },
              {
                title: '',
                field: 'id',
                Cell: ({ entry }) =>
                  editable && usaPesos && entry.cursos.length > 1 ? (
                    <Button
                      size="sm"
                      variant="primary-light"
                      icon={<Pencil className="size-4" />}
                      onClick={() => setEditando(entry)}
                    >
                      Ajustar pesos
                    </Button>
                  ) : (
                    <span className="sr-only">Sin acciones</span>
                  ),
              },
            ]}
          />
        )}
      </div>

      {editando && (
        <DialogoPesosCompetencia
          plan={plan}
          competencia={editando}
          onCerrar={() => setEditando(null)}
        />
      )}
    </Card>
  );
};

type DialogoProps = {
  plan: PlanGrado;
  competencia: PesosCompetencia;
  onCerrar: () => void;
};

/** Reparte 100 % en partes iguales; el primero recibe el resto del redondeo (33,34 + 33,33 + 33,33). */
const partesIguales = (cantidad: number) => {
  const parte = Math.floor(10000 / cantidad) / 100;
  const primera = Math.round((100 - parte * (cantidad - 1)) * 100) / 100;
  return Array.from({ length: cantidad }, (_, i) =>
    i === 0 ? primera : parte,
  );
};

const aTexto = (valor: number | null) =>
  valor == null ? '' : String(valor).replace('.', ',');

const aNumero = (texto: string) => {
  const limpio = texto.trim().replace(',', '.');
  return limpio === '' ? NaN : Number(limpio);
};

// Modal "Pesos de <competencia>": el peso de todos los cursos que la evalúan, que deben sumar 100 %.
export const DialogoPesosCompetencia = ({
  plan,
  competencia,
  onCerrar,
}: DialogoProps) => {
  const [valores, setValores] = React.useState<string[]>(
    competencia.cursos.map((c) => aTexto(c.pesoCurso)),
  );
  const [enviado, setEnviado] = React.useState(false);
  const guardar = useGuardarPesosCompetencia(onCerrar);

  const numeros = valores.map(aNumero);
  const errorDe = (n: number) =>
    Number.isNaN(n)
      ? 'Ingresa el peso del curso.'
      : n < 0.01 || n > 100
        ? 'El peso va de 0,01 a 100.'
        : Math.round(n * 100) !== n * 100
          ? 'Usa hasta 2 decimales.'
          : undefined;
  const errores = numeros.map(errorDe);
  const total =
    Math.round(
      numeros.reduce((suma, n) => suma + (Number.isNaN(n) ? 0 : n), 0) * 100,
    ) / 100;
  const sumaCorrecta = total === 100;

  return (
    <DialogoCatalogo
      open
      onCerrar={onCerrar}
      icono={<Percent className="size-5" aria-hidden="true" />}
      titulo={`Pesos de «${competencia.competencia}»`}
      descripcion={`${plan.grado} · ${plan.anio}. ${
        plan.reglaNota === 'NIVEL_MAS_FRECUENTE' ||
        plan.reglaNota === 'ULTIMO_NIVEL'
          ? 'Cuánto pesa el nivel de cada curso al decidir el nivel de la competencia'
          : 'Cuánto aporta cada curso a la nota de la competencia'
      } en cada periodo. Deben sumar 100 %.`}
      formId="form-pesos-competencia"
      textoGuardar="Guardar"
      guardando={guardar.isPending}
      className="max-w-[560px]"
    >
      <form
        noValidate
        id="form-pesos-competencia"
        className="space-y-4"
        onSubmit={(e) => {
          e.preventDefault();
          setEnviado(true);
          if (errores.some(Boolean) || !sumaCorrecta) return;
          guardar.mutate({
            competenciaId: competencia.competenciaId,
            data: {
              anioEscolarId: plan.anioEscolarId,
              gradoCodigo: plan.gradoCodigo,
              pesos: competencia.cursos.map((c, i) => ({
                planCursoId: c.planCursoId,
                pesoCurso: numeros[i],
              })),
            },
          });
        }}
      >
        {competencia.cursos.map((c, i) => (
          <Input
            key={c.planCursoId}
            label={`${c.curso} *`}
            inputMode="decimal"
            value={valores[i]}
            endAdornment="%"
            error={
              enviado && errores[i]
                ? { type: 'validate', message: errores[i] }
                : undefined
            }
            onChange={(e) =>
              setValores((actuales) =>
                actuales.map((v, j) => (j === i ? e.target.value : v)),
              )
            }
          />
        ))}

        <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl bg-surface-secondary px-4 py-3">
          <span className="text-body-3 text-content-secondary">
            Total:{' '}
            <strong
              className={
                sumaCorrecta ? 'text-content-success' : 'text-content-error'
              }
            >
              {porcentaje(total)}
            </strong>
          </span>
          <Button
            type="button"
            size="sm"
            variant="secondary"
            onClick={() =>
              setValores(partesIguales(competencia.cursos.length).map(aTexto))
            }
          >
            Repartir en partes iguales
          </Button>
        </div>

        {enviado && !sumaCorrecta && (
          <Alert type="error" size="sm" title="Los pesos deben sumar 100 %." />
        )}
      </form>
    </DialogoCatalogo>
  );
};
