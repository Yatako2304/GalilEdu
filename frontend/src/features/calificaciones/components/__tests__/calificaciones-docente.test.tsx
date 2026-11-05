import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { CalificacionesDocente } from '../calificaciones-docente';

const crearItem = vi.fn();

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
            pesoPorcentaje: 100,
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
  useReporteCalificaciones: (filtros: unknown) => ({
    isLoading: false,
    data: filtros
      ? {
          titulo: 'Práctica 1',
          detalle: 'Calificaciones por ítem',
          columnas: [
            { clave: 'nota', titulo: 'Calificación' },
            { clave: 'evidencia', titulo: 'Evidencia' },
            { clave: 'estado', titulo: 'Estado' },
          ],
          filas: [
            {
              matriculaId: 'matricula-1',
              estudiante: 'Ana García',
              valores: {
                nota: '17',
                evidencia: 'practica.pdf',
                estado: 'Calificado',
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

  it('muestra el curso asignado, el periodo y los tres recorridos de las HU', async () => {
    render(<CalificacionesDocente />);

    expect((await screen.findAllByText('Álgebra')).length).toBeGreaterThan(0);
    expect(screen.getAllByText('1.° de secundaria').length).toBeGreaterThan(0);
    expect(screen.getByText('IV Bimestre')).toBeVisible();
    expect(screen.getByRole('tab', { name: 'Configurar ítems' })).toBeVisible();
    expect(
      screen.getByRole('tab', { name: 'Calificar alumnos' }),
    ).toBeVisible();
    expect(
      screen.getByRole('tab', { name: 'Ver calificaciones' }),
    ).toBeVisible();
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
        competencias: [{ competenciaId: 'competencia-1', pesoPorcentaje: 100 }],
      },
    });
  });

  it('mantiene alineadas las columnas en cada tipo de reporte', async () => {
    const usuario = userEvent.setup();
    render(<CalificacionesDocente />);

    await usuario.click(
      screen.getByRole('tab', { name: 'Ver calificaciones' }),
    );

    const tabla = await screen.findByRole('table', {
      name: 'Práctica 1. Calificaciones por ítem',
    });
    expect(tabla).toHaveStyle({ minWidth: '800px' });
    expect(screen.getByText('Ana García')).toBeVisible();
    expect(screen.getByText('Calificado')).toBeVisible();

    await usuario.click(screen.getByRole('tab', { name: 'Por competencia' }));
    expect(tabla).toHaveStyle({ minWidth: '800px' });

    await usuario.click(screen.getByRole('tab', { name: 'Por periodo' }));
    expect(tabla).toHaveStyle({ minWidth: '800px' });
  });
});
