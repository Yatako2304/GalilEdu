import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { CalificacionesDocente } from '../calificaciones-docente';

const crearItem = vi.fn();
const { filasVacias } = vi.hoisted(() => ({ filasVacias: [] }));

vi.mock('../../api/calificaciones', () => ({
  useCursosDocente: () => ({
    isLoading: false,
    data: [
      {
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
        periodos: [
          {
            id: 'periodo-4',
            numero: 4,
            nombre: 'IV Bimestre',
            fechaInicio: '2026-10-19',
            fechaFin: '2026-12-18',
            fechaLimiteNotas: '2026-12-23',
            estado: 'EN_CURSO',
            registroNotasAbierto: true,
            competencias: 1,
            items: 0,
          },
        ],
      },
    ],
  }),
  useCompetenciasCurso: () => ({
    data: [{ id: 'competencia-1', nombre: 'Resuelve problemas de cantidad' }],
  }),
  useItemsEvaluacion: () => ({
    isLoading: false,
    data: [
      {
        id: 'item-1',
        nombre: 'Práctica 1',
        descripcion: 'Operaciones con números racionales',
        tipo: 'PRACTICA',
        actualizadoEn: '2026-11-04T12:00:00Z',
        editable: true,
        competencias: [
          {
            competenciaId: 'competencia-1',
            competencia: 'Resuelve problemas de cantidad',
            pesoPorcentaje: 50,
          },
        ],
      },
    ],
  }),
  useEliminarItem: () => ({
    mutateAsync: vi.fn(),
    isPending: false,
    isSuccess: false,
  }),
  useCrearItem: () => ({ mutateAsync: crearItem, isPending: false }),
  useEditarItem: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useCalificacionesItem: () => ({ data: filasVacias, isLoading: false }),
  useGuardarCalificaciones: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useGuardarEvidencia: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useEliminarEvidencia: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useReporteCalificaciones: (filtros: unknown) => ({
    isLoading: false,
    data: filtros
      ? {
          titulo: 'Resuelve problemas de cantidad',
          detalle: 'Consolidado de notas',
          columnas: [
            { clave: 'item-1', titulo: 'Práctica 1' },
            { clave: 'resultado', titulo: 'Promedio final' },
          ],
          filas: [
            {
              matriculaId: 'matricula-1',
              estudiante: 'Ana García',
              valores: {
                'item-1': '17',
                resultado: '17',
                resultado_estado: 'Aprobado',
              },
            },
          ],
        }
      : undefined,
  }),
  exportarReporte: vi.fn(),
}));

describe('Calificaciones del docente', () => {
  beforeEach(() => crearItem.mockReset().mockResolvedValue({}));

  it('agrupa configuración y calificación bajo el primer botón y separa el consolidado', async () => {
    render(<CalificacionesDocente />);

    expect((await screen.findAllByText('Álgebra')).length).toBeGreaterThan(0);
    expect(screen.getAllByText('1.° de secundaria').length).toBeGreaterThan(0);
    expect(screen.getByText('IV Bimestre')).toBeVisible();
    expect(screen.getByRole('tab', { name: 'Configurar ítems' })).toBeVisible();
    expect(
      screen.getByRole('tab', { name: 'Calificar alumnos' }),
    ).toBeVisible();
    const vistas = screen.getByRole('tablist', {
      name: 'Vistas de calificaciones',
    });
    expect(within(vistas).getAllByRole('tab')).toHaveLength(2);
    expect(
      within(vistas).getByRole('tab', { name: 'Configuración de ítems' }),
    ).toHaveAttribute('aria-selected', 'true');
    expect(
      within(vistas).getByRole('tab', { name: 'Consolidado de notas' }),
    ).toBeVisible();
    const secciones = screen.getByRole('tablist', {
      name: 'Secciones de calificaciones',
    });
    expect(within(secciones).getAllByRole('tab')).toHaveLength(2);
    expect(
      screen.queryByRole('heading', { name: 'Generar consolidado de notas' }),
    ).not.toBeInTheDocument();
    expect(
      screen.getByRole('tab', { name: 'Ítems por competencia' }),
    ).toBeVisible();
  });

  it('registra un ítem con criterio, tipo, competencia y peso', async () => {
    const usuario = userEvent.setup();
    render(<CalificacionesDocente />);

    await usuario.click(
      await screen.findByRole('button', {
        name: 'Agregar ítem de evaluación',
      }),
    );
    await usuario.type(
      screen.getByLabelText('Nombre del ítem de calificación *'),
      'Examen 4',
    );
    await usuario.type(
      screen.getByLabelText('Descripción *'),
      'Resuelve situaciones de cantidad.',
    );
    await usuario.click(screen.getByRole('button', { name: 'Agregar ítem' }));

    expect(crearItem).toHaveBeenCalledWith({
      seleccion: {
        planCursoId: 'plan-1',
        seccionId: 'seccion-1',
        periodoId: 'periodo-4',
      },
      datos: {
        nombre: 'Examen 4',
        descripcion: 'Resuelve situaciones de cantidad.',
        tipo: 'PRACTICA',
        competencias: [{ competenciaId: 'competencia-1', pesoPorcentaje: 50 }],
      },
    });
  });

  it('genera el consolidado desde su propio botón sin mostrar los controles de configuración', async () => {
    const usuario = userEvent.setup();
    render(<CalificacionesDocente />);

    await usuario.click(
      screen.getByRole('tab', { name: 'Consolidado de notas' }),
    );

    expect(
      screen.getByRole('heading', { name: 'Generar consolidado de notas' }),
    ).toBeVisible();
    expect(
      screen.queryByRole('heading', { name: 'Curso y sección' }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole('tab', { name: 'Configurar ítems' }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole('tab', { name: 'Calificar alumnos' }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole('button', { name: 'Agregar ítem de evaluación' }),
    ).not.toBeInTheDocument();
    await usuario.click(
      screen.getByRole('button', { name: 'Generar consolidado' }),
    );

    const tabla = await screen.findByRole('table', {
      name: 'Resuelve problemas de cantidad. Consolidado de notas',
    });
    expect(screen.getByText('Ana García')).toBeVisible();
    expect(
      within(tabla).getByRole('columnheader', { name: 'Promedio final' }),
    ).toBeVisible();
    expect(within(tabla).getByText('Aprobado')).toBeVisible();
    expect(within(tabla).getAllByText('17/20')).toHaveLength(2);
    expect(
      screen.getByRole('button', { name: 'Exportar consolidado' }),
    ).toBeEnabled();
  });

  it('permite volver a calificar alumnos después de abrir el consolidado', async () => {
    const usuario = userEvent.setup();
    render(<CalificacionesDocente />);

    await usuario.click(screen.getByRole('tab', { name: 'Calificar alumnos' }));
    await usuario.click(
      screen.getByRole('tab', { name: 'Consolidado de notas' }),
    );
    await usuario.click(
      screen.getByRole('tab', { name: 'Configuración de ítems' }),
    );

    expect(
      screen.getByRole('tab', { name: 'Calificar alumnos' }),
    ).toHaveAttribute('aria-selected', 'true');
    expect(
      screen.getByRole('heading', { name: 'Calificar alumnos' }),
    ).toBeVisible();
    expect(
      screen.getByRole('heading', { name: 'Curso y sección' }),
    ).toBeVisible();
    expect(
      screen.queryByRole('heading', { name: 'Generar consolidado de notas' }),
    ).not.toBeInTheDocument();
  });
});
