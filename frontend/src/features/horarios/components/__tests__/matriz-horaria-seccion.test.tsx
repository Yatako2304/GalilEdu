import { horarioSeccion5A } from '@/testing/mocks/fixtures/horarios';
import { rtlRender, screen, within } from '@/testing/test-utils';
import { HorarioSeccion } from '@/types/api';

import { MatrizHoraria } from '../matriz-horaria';

// HU-49: `CursoSesion.seccion` es opcional; con ella la celda muestra la sección (Figma > Vista = Profesor) y sin ella
// queda como siempre.

const horario = (
  cursos: HorarioSeccion['sesiones'][number]['cursos'],
): Pick<HorarioSeccion, 'bloques' | 'sesiones'> => ({
  bloques: [
    {
      id: 'b1',
      nombre: 'Bloque 1',
      tipo: 'CLASE',
      horaInicio: '08:00',
      horaFin: '08:45',
      orden: 0,
    },
  ],
  sesiones: [{ dia: 'LUNES', bloqueId: 'b1', tipo: 'ASIGNADO', cursos }],
});

test('con sección, la celda la muestra junto al aula', () => {
  rtlRender(
    <MatrizHoraria
      horario={horario([
        { curso: 'Álgebra', docente: '', aula: 'A-401', seccion: '1.° A' },
      ])}
    />,
  );

  const celda = screen.getAllByRole('cell')[0];
  expect(within(celda).getByText('Álgebra')).toBeVisible();
  expect(within(celda).getByText('A-401')).toBeVisible();
  expect(within(celda).getByText('1.° A')).toBeVisible();
});

test('con varios cursos, las secciones se unen con « · »', () => {
  rtlRender(
    <MatrizHoraria
      horario={horario([
        { curso: 'Álgebra', docente: '', aula: 'A-401', seccion: '1.° A' },
        { curso: 'Geometría', docente: '', aula: 'A-402', seccion: '1.° B' },
      ])}
    />,
  );

  const celda = screen.getAllByRole('cell')[0];
  expect(within(celda).getByText('A-401 · A-402')).toBeVisible();
  expect(within(celda).getByText('1.° A · 1.° B')).toBeVisible();
});

test('sin sección, la celda queda igual que antes', () => {
  rtlRender(
    <MatrizHoraria
      horario={horario([
        { curso: 'Álgebra', docente: 'Prof. Ana', aula: 'A-401' },
      ])}
    />,
  );

  const celda = screen.getAllByRole('cell')[0];
  expect(within(celda).getByText('Prof. Ana')).toBeVisible();
  const pie = within(celda).getByText('A-401').closest('p')!;
  // Solo el aula: no se dibuja ninguna sección.
  expect(pie.querySelectorAll('span')).toHaveLength(1);
});

test('una sesión sin sección en el horario de siempre no dibuja ninguna', () => {
  rtlRender(<MatrizHoraria horario={horarioSeccion5A} />);

  const tabla = screen.getByRole('table', { name: /matriz horaria/i });
  expect(within(tabla).queryByText(/^\d\.° [A-Z]$/)).not.toBeInTheDocument();
});
