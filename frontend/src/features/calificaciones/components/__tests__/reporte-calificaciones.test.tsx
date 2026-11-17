import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import {
  CursoDocente,
  ItemEvaluacion,
  NotasCompetencias,
  ReporteCalificaciones,
} from '@/types/api';

import { FiltrosReporte } from '../../api/calificaciones';
import { ReporteCalificacionesVista } from '../reporte-calificaciones';

const { consultar, exportar, notas } = vi.hoisted(() => ({
  consultar: vi.fn(),
  exportar: vi.fn(),
  notas: vi.fn(),
}));

vi.mock('../../api/calificaciones', () => ({
  useReporteCalificaciones: (filtros: FiltrosReporte | null) =>
    consultar(filtros),
  useNotasCompetencia: (seleccion: unknown, competenciaIds: string[]) =>
    notas(seleccion, competenciaIds),
  exportarReporte: exportar,
}));

const periodo = {
  id: 'periodo-4',
  numero: 4,
  nombre: 'IV Bimestre',
  fechaInicio: '2026-10-19',
  fechaFin: '2026-12-18',
  fechaLimiteNotas: '2026-12-23',
  estado: 'EN_CURSO' as const,
  registroNotasAbierto: true,
  competencias: 2,
  items: 2,
};
const curso: CursoDocente = {
  planCursoId: 'plan-1',
  seccionId: 'seccion-1',
  anioEscolarId: 'anio-1',
  anio: 2026,
  curso: 'Álgebra',
  area: 'Matemática',
  gradoCodigo: 'SEC-1',
  grado: '1.° de secundaria',
  seccion: 'A',
  escala: 'VIGESIMAL',
  reglaNota: 'PROMEDIO_PONDERADO',
  consolidacion: 'PROMEDIO_PERIODOS',
  periodos: [periodo],
};
const competencias = [
  { id: 'c1', nombre: 'Cantidad' },
  { id: 'c2', nombre: 'Regularidad' },
];
const items: ItemEvaluacion[] = [
  {
    id: 'i1',
    nombre: 'Práctica compartida',
    descripcion: 'Criterio',
    tipo: 'PRACTICA',
    actualizadoEn: '2026-11-01T10:00:00Z',
    editable: true,
    competencias: competencias.map((c) => ({
      competenciaId: c.id,
      competencia: c.nombre,
      pesoPorcentaje: 50,
    })),
  },
  {
    id: 'i2',
    nombre: 'Proyecto',
    descripcion: 'Criterio',
    tipo: 'PROYECTO',
    actualizadoEn: '2026-11-02T10:00:00Z',
    editable: true,
    competencias: [
      { competenciaId: 'c2', competencia: 'Regularidad', pesoPorcentaje: 50 },
    ],
  },
];
const reporte: ReporteCalificaciones = {
  titulo: 'Competencias seleccionadas',
  detalle: 'Consolidado de notas',
  columnas: [
    {
      clave: 'c1:i1',
      titulo: 'Práctica compartida',
      competenciaId: 'c1',
      competencia: 'Cantidad',
      promedio: false,
    },
    {
      clave: 'c1:resultado',
      titulo: 'Promedio final',
      competenciaId: 'c1',
      competencia: 'Cantidad',
      promedio: true,
    },
    {
      clave: 'c2:i1',
      titulo: 'Práctica compartida',
      competenciaId: 'c2',
      competencia: 'Regularidad',
      promedio: false,
    },
    {
      clave: 'c2:i2',
      titulo: 'Proyecto',
      competenciaId: 'c2',
      competencia: 'Regularidad',
      promedio: false,
    },
    {
      clave: 'c2:resultado',
      titulo: 'Promedio final',
      competenciaId: 'c2',
      competencia: 'Regularidad',
      promedio: true,
    },
    { clave: 'resultado_general', titulo: 'Promedio general', promedio: true },
  ],
  filas: [
    {
      matriculaId: 'm1',
      estudiante: 'Ana Pérez',
      valores: {
        'c1:i1': '17',
        'c1:resultado': '17',
        'c1:resultado_estado': 'Aprobado',
        'c2:i1': '17',
        'c2:i2': '9',
        'c2:resultado': '13',
        'c2:resultado_estado': 'Desaprobado',
        resultado_general: '14.33',
        resultado_general_estado: 'Aprobado',
      },
    },
  ],
};

const mostrar = () =>
  render(
    <ReporteCalificacionesVista
      seleccion={{
        planCursoId: curso.planCursoId,
        seccionId: curso.seccionId,
        periodoId: periodo.id,
      }}
      curso={curso}
      periodo={periodo}
      anios={[2026]}
      cursos={['Álgebra']}
      grados={[{ value: 'SEC-1', label: curso.grado }]}
      competencias={competencias}
      items={items}
      onAnioChange={vi.fn()}
      onCursoChange={vi.fn()}
      onGradoChange={vi.fn()}
      onPeriodoChange={vi.fn()}
    />,
  );

describe('Consolidado con varias competencias', () => {
  beforeEach(() => {
    exportar.mockReset();
    notas.mockReset().mockReturnValue({ isLoading: true, data: undefined });
    consultar
      .mockReset()
      .mockImplementation((filtros: FiltrosReporte | null) => ({
        isLoading: false,
        data: filtros
          ? {
              ...reporte,
              columnas: reporte.columnas.filter(
                (c) => filtros.incluirPromedio || !c.promedio,
              ),
            }
          : undefined,
      }));
  });

  it('selecciona varias competencias, agrupa sus notas y exporta los mismos filtros', async () => {
    const usuario = userEvent.setup();
    mostrar();
    expect(
      screen.getByText(/regla del año escolar: promedio ponderado/),
    ).toBeVisible();
    await usuario.click(screen.getByRole('combobox', { name: 'Competencias' }));
    expect(
      screen.getByRole('menuitemcheckbox', { name: 'Cantidad' }),
    ).toBeChecked();
    await usuario.click(
      screen.getByRole('menuitemcheckbox', { name: 'Regularidad' }),
    );
    await usuario.keyboard('{Escape}');
    await usuario.click(
      screen.getByRole('combobox', { name: 'Ítems de calificación' }),
    );
    expect(screen.getAllByRole('menuitemcheckbox')).toHaveLength(2);
    expect(
      screen.getByRole('menuitemcheckbox', { name: 'Proyecto' }),
    ).toBeChecked();
    await usuario.keyboard('{Escape}');
    await usuario.click(
      screen.getByRole('button', { name: 'Generar consolidado' }),
    );

    const tabla = screen.getByRole('table');
    expect(
      within(tabla).getByRole('columnheader', { name: 'Cantidad' }),
    ).toHaveAttribute('colspan', '2');
    expect(
      within(tabla).getByRole('columnheader', { name: 'Regularidad' }),
    ).toHaveAttribute('colspan', '3');
    expect(
      within(tabla).getAllByRole('columnheader', { name: 'Promedio final' }),
    ).toHaveLength(2);
    expect(
      within(tabla).getByRole('columnheader', { name: 'Promedio general' }),
    ).toBeVisible();
    expect(within(tabla).getByText('14,33/20')).toBeVisible();
    expect(within(tabla).getByText('Desaprobado')).toHaveClass(
      'text-content-error',
    );
    await usuario.click(screen.getByRole('button', { name: 'Descargar PDF' }));
    expect(exportar).toHaveBeenCalledWith({
      planCursoId: 'plan-1',
      seccionId: 'seccion-1',
      periodoId: 'periodo-4',
      tipo: 'COMPETENCIA',
      competenciaIds: 'c1,c2',
      itemIds: 'i1,i2',
      incluirPromedio: true,
    });
  });

  it('permite omitir los promedios y limpiar la selección', async () => {
    const usuario = userEvent.setup();
    mostrar();
    await usuario.click(
      screen.getByRole('switch', { name: 'Incluir promedio final' }),
    );
    await usuario.click(
      screen.getByRole('button', { name: 'Generar consolidado' }),
    );
    expect(consultar).toHaveBeenLastCalledWith(
      expect.objectContaining({ incluirPromedio: false, competenciaIds: 'c1' }),
    );
    expect(
      screen.queryByRole('columnheader', { name: /Promedio/ }),
    ).not.toBeInTheDocument();
    await usuario.click(screen.getByRole('button', { name: 'Limpiar' }));
    expect(screen.queryByRole('table')).not.toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: 'Generar consolidado' }),
    ).toBeDisabled();
  });

  it('descarta el reporte anterior al cambiar las competencias y ajusta los ítems disponibles', async () => {
    const usuario = userEvent.setup();
    mostrar();
    await usuario.click(
      screen.getByRole('button', { name: 'Generar consolidado' }),
    );
    expect(screen.getByRole('table')).toBeVisible();
    await usuario.click(screen.getByRole('combobox', { name: 'Competencias' }));
    await usuario.click(
      screen.getByRole('menuitemcheckbox', { name: 'Cantidad' }),
    );
    await usuario.keyboard('{Escape}');
    expect(screen.queryByRole('table')).not.toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: 'Generar consolidado' }),
    ).toBeDisabled();
    expect(
      screen.getByRole('combobox', { name: 'Ítems de calificación' }),
    ).toHaveAttribute('aria-disabled', 'true');
  });
});

describe('Nota de la competencia', () => {
  const notasCantidad: NotasCompetencias = {
    escala: 'VIGESIMAL',
    reglaNota: 'PROMEDIO_PONDERADO',
    consolidacion: 'PROMEDIO_PERIODOS',
    periodos: [
      { id: 'periodo-3', numero: 3, nombre: 'III Bimestre' },
      { id: 'periodo-4', numero: 4, nombre: 'IV Bimestre' },
    ],
    competencias: [
      {
        competenciaId: 'c1',
        competencia: 'Cantidad',
        usaPesos: true,
        pesosCompletos: true,
        cursos: [
          { planCursoId: 'plan-1', curso: 'Álgebra', pesoCurso: 40 },
          { planCursoId: 'plan-2', curso: 'Aritmética', pesoCurso: 60 },
        ],
      },
    ],
    filas: [
      {
        matriculaId: 'm1',
        estudiante: 'Ana Pérez',
        competencias: [
          {
            competenciaId: 'c1',
            periodos: [
              {
                periodoId: 'periodo-3',
                nota: '13.6',
                nivel: 'A',
                estado: 'Aprobado',
                parcial: false,
                aportes: [
                  { planCursoId: 'plan-1', nota: '16' },
                  { planCursoId: 'plan-2', nota: '12' },
                ],
              },
              {
                periodoId: 'periodo-4',
                nota: '15.6',
                nivel: 'AD',
                estado: 'Aprobado',
                parcial: true,
                aportes: [
                  { planCursoId: 'plan-1', nota: '18' },
                  { planCursoId: 'plan-2', nota: '14' },
                ],
              },
            ],
            notaFinal: {
              nota: '14.6',
              nivel: 'A',
              estado: 'Aprobado',
              parcial: true,
            },
          },
        ],
      },
    ],
  };

  beforeEach(() => {
    consultar.mockReset().mockReturnValue({ isLoading: false, data: reporte });
    notas
      .mockReset()
      .mockReturnValue({ isLoading: false, data: notasCantidad });
  });

  it('muestra la nota oficial por periodo y la final, con el aporte de cada curso', async () => {
    const usuario = userEvent.setup();
    mostrar();
    expect(
      screen.queryByText('Nota de la competencia'),
    ).not.toBeInTheDocument();
    await usuario.click(
      screen.getByRole('button', { name: 'Generar consolidado' }),
    );

    expect(notas).toHaveBeenLastCalledWith(
      { planCursoId: 'plan-1', seccionId: 'seccion-1' },
      ['c1'],
    );
    const tabla = within(
      screen.getByRole('table', { name: 'Nota de la competencia Cantidad' }),
    );
    expect(
      tabla.getByRole('columnheader', { name: 'III Bimestre' }),
    ).toBeVisible();
    expect(
      tabla.getByRole('columnheader', { name: 'Nota final (nivel de logro)' }),
    ).toBeVisible();
    expect(tabla.getByText('13,6/20')).toBeVisible();
    expect(tabla.getByText('· AD')).toBeVisible();
    // La nota final va en letra, con el número como referencia.
    expect(tabla.getByLabelText('Nivel de logro A')).toHaveTextContent('A');
    expect(tabla.getByText('14,6/20')).toBeVisible();
    expect(tabla.getAllByText(/Parcial/)).toHaveLength(2);
    expect(screen.getByText('Álgebra 40 % · Aritmética 60 %')).toBeVisible();
    expect(tabla.queryByText('Álgebra: 16/20')).not.toBeInTheDocument();

    await usuario.click(
      screen.getByRole('switch', { name: 'Ver el aporte de cada curso' }),
    );
    expect(tabla.getByText('Álgebra: 16/20')).toBeVisible();
    expect(tabla.getByText('Aritmética: 14/20')).toBeVisible();
  });

  it('avisa cuando los pesos de los cursos no suman 100 %', async () => {
    notas.mockReturnValue({
      isLoading: false,
      data: {
        ...notasCantidad,
        competencias: [
          { ...notasCantidad.competencias[0], pesosCompletos: false },
        ],
      },
    });
    const usuario = userEvent.setup();
    mostrar();
    await usuario.click(
      screen.getByRole('button', { name: 'Generar consolidado' }),
    );

    expect(
      screen.getByText('Los pesos de los cursos no suman 100 %.'),
    ).toBeVisible();
  });
});
