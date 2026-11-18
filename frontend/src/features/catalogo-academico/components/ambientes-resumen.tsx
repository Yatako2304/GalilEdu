import { Building2, DoorOpen, FlaskConical, Users } from 'lucide-react';

import { Card } from '@/components/ui/card';

import { useResumenAmbientes } from '../api/ambientes';

// Figma > Personal Administrativo > CI-26 · Ambientes físicos (nodo 857:10215): 4 tarjetas de resumen.
// Solo cuentan los ambientes activos y no dependen de los filtros de la lista.
export const AmbientesResumen = () => {
  const resumen = useResumenAmbientes();
  const r = resumen.data;
  const tarjetas = [
    {
      Icono: Building2,
      titulo: 'Ambientes activos',
      valor: r?.activos,
      detalle:
        r && `de ${r.total} ${r.total === 1 ? 'registrado' : 'registrados'}`,
    },
    {
      Icono: DoorOpen,
      titulo: 'Aulas regulares',
      valor: r?.aulasRegulares,
      detalle: 'para secciones',
    },
    {
      Icono: FlaskConical,
      titulo: 'Espacios especiales',
      valor: r?.espaciosEspeciales,
      detalle: 'laboratorios, talleres y otros',
    },
    {
      Icono: Users,
      titulo: 'Aforo total',
      valor: r?.aforoTotal,
      detalle: 'personas en ambientes activos',
    },
  ];
  return (
    <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      {tarjetas.map(({ Icono, titulo, valor, detalle }) => (
        // Figma CI-26 > "KPI": tarjeta compacta propia de esta pantalla, igual a la de CI-14 (no es el Stat Card).
        <Card key={titulo} className="flex flex-col gap-1.5 px-5 py-4">
          <p className="flex items-center gap-1.5 text-body-4 text-content-tertiary">
            <Icono
              className="size-[18px] text-icon-tertiary"
              aria-hidden="true"
            />
            {titulo}
          </p>
          <p className="text-h6 font-semibold text-content-primary">
            {valor ?? '—'}
          </p>
          <p className="text-body-4 text-content-tertiary">{detalle}</p>
        </Card>
      ))}
    </div>
  );
};
