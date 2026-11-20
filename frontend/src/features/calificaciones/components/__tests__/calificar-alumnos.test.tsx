import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, expect, it, vi } from 'vitest';

import {
  CursoDocente,
  FilaCalificacion,
  ItemEvaluacion,
  PeriodoCursoDocente,
} from '@/types/api';

import { CalificarAlumnos } from '../calificar-alumnos';

// HU-59 y HU-60: la tabla solo registra celdas vacías; una nota guardada se cambia con «Modificar».

// Las filas deben ser el mismo arreglo en cada render: el componente las observa con useEffect.
const { guardar, filas } = vi.hoisted(() => {
  const fila = (
    matriculaId: string,
    estudiante: string,
    nota: number | null,
  ) => ({
    id: nota === null ? null : `cal-${matriculaId}`,
    matriculaId,
    estudianteId: `est-${matriculaId}`,
    estudiante,
    notaNumerica: nota,
    nivelLogro: null,
    evidencia: null,
    estado: nota === null ? 'PENDIENTE' : 'CALIFICADO',
    modificaciones: 0,
    noEvaluado: false,
    motivoNoEvaluado: null,
  });
  return {
    guardar: vi.fn(),
    filas: [
      fila('m-ana', 'Ana Lucía García López', 12),
      fila('m-diego', 'Diego José Quispe Mendoza', null),
    ],
  };
});

vi.mock('../../api/calificaciones', () => ({
  useCalificacionesItem: () => ({
    isLoading: false,
    data: filas as FilaCalificacion[],
  }),
  useGuardarCalificaciones: () => ({ mutateAsync: guardar, isPending: false }),
  useGuardarEvidencia: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useEliminarEvidencia: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useModificarCalificacion: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useHistorialCalificacion: () => ({ isLoading: false, data: [] }),
  descargarEvidencia: vi.fn(),
}));

const periodo: PeriodoCursoDocente = {
  id: 'periodo-4',
  numero: 4,
  nombre: 'IV Bimestre',
  fechaInicio: '2026-10-19',
  fechaFin: '2026-12-18',
  fechaLimiteNotas: '2026-12-23',
  estado: 'EN_CURSO',
  registroNotasAbierto: true,
  modificacionHabilitada: true,
  competencias: 1,
  items: 1,
};

const curso = {
  planCursoId: 'plan-1',
  seccionId: 'seccion-1',
  anioEscolarId: 'anio-1',
  anio: 2026,
  curso: 'Aritmética',
  area: 'Matemática',
  gradoCodigo: 'SEC-1',
  grado: '1.° de secundaria',
  seccion: 'A',
  escala: 'VIGESIMAL',
  reglaNota: 'PROMEDIO_PONDERADO',
  consolidacion: 'PROMEDIO_PERIODOS',
  periodos: [periodo],
} as CursoDocente;

const items: ItemEvaluacion[] = [
  {
    id: 'item-1',
    nombre: 'Examen bimestral',
    descripcion: 'Examen',
    tipo: 'EXAMEN',
    actualizadoEn: '2026-11-01T10:00:00Z',
    editable: true,
    competencias: [],
  },
];

beforeEach(() => {
  guardar.mockReset().mockResolvedValue([]);
});

it('una nota guardada queda bloqueada en la tabla y el lote solo envía las nuevas', async () => {
  const usuario = userEvent.setup();
  render(<CalificarAlumnos curso={curso} periodo={periodo} items={items} />);

  const ana = screen.getByLabelText('Calificación de Ana Lucía García López');
  const diego = screen.getByLabelText(
    'Calificación de Diego José Quispe Mendoza',
  );
  expect(ana).toHaveValue(12);
  expect(ana).toBeDisabled();
  expect(diego).toBeEnabled();
  expect(
    screen.getByRole('button', {
      name: 'Modificar calificación de Ana Lucía García López',
    }),
  ).toBeEnabled();

  const guardarBoton = screen.getByRole('button', {
    name: 'Guardar calificaciones',
  });
  expect(guardarBoton).toBeDisabled();

  await usuario.type(diego, '15');
  await usuario.click(guardarBoton);

  expect(guardar).toHaveBeenCalledWith({
    itemId: 'item-1',
    calificaciones: [
      { matriculaId: 'm-diego', notaNumerica: 15, nivelLogro: null },
    ],
  });
});
