import { EstadoMatriculaApoderado } from '@/types/api';

import { htmlDeLaConstancia } from './constancia-matricula';

test('la constancia incluye alumno, código y escapa el HTML', () => {
  const html = htmlDeLaConstancia({
    estudiante: { id: 'e', nombre: 'Ana <b>', grado: '1.°' },
    periodo: { anio: 2027 },
    matricula: {
      grado: '1.°',
      seccion: 'A',
      codigo: 'MAT-2027-0001',
      confirmadoEn: null,
    },
    cuotaMatricula: null,
  } as unknown as EstadoMatriculaApoderado);
  expect(html).toContain('MAT-2027-0001');
  expect(html).toContain('Ana &lt;b&gt;');
});
