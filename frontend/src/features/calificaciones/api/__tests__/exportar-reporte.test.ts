import { afterEach, expect, it, vi } from 'vitest';

import { api } from '@/lib/api-client';

import { exportarReporte } from '../calificaciones';

vi.mock('@/lib/api-client', () => ({ api: { get: vi.fn() } }));

afterEach(() => {
  vi.restoreAllMocks();
  vi.unstubAllGlobals();
});

it('descarga únicamente el PDF con los filtros del reporte generado', async () => {
  const archivo = new Blob(['%PDF-1.7'], { type: 'application/pdf' });
  vi.mocked(api.get).mockResolvedValue(archivo);
  const crearUrl = vi.fn().mockReturnValue('blob:reporte-pdf');
  const revocarUrl = vi.fn();
  vi.stubGlobal(
    'URL',
    class extends URL {
      static createObjectURL = crearUrl;
      static revokeObjectURL = revocarUrl;
    },
  );
  const pulsar = vi
    .spyOn(HTMLAnchorElement.prototype, 'click')
    .mockImplementation(function (this: HTMLAnchorElement) {
      expect(this.download).toBe('consolidado-notas.pdf');
      expect(this.href).toBe('blob:reporte-pdf');
    });
  const filtros = {
    tipo: 'COMPETENCIA' as const,
    planCursoId: 'plan-1',
    seccionId: 'seccion-1',
    periodoId: 'periodo-4',
    competenciaIds: 'c1,c2',
    itemIds: 'i1,i2',
    incluirPromedio: true,
  };

  await exportarReporte(filtros);

  expect(api.get).toHaveBeenCalledWith(
    '/calificaciones/docente/reportes/exportar-pdf',
    {
      params: filtros,
      responseType: 'blob',
    },
  );
  expect(crearUrl).toHaveBeenCalledWith(archivo);
  expect(pulsar).toHaveBeenCalledOnce();
  expect(revocarUrl).toHaveBeenCalledWith('blob:reporte-pdf');
});
