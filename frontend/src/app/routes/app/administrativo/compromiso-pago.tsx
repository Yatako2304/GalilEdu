import { CirclePlus, Percent } from 'lucide-react';
import * as React from 'react';
import { useParams } from 'react-router';

import { ContentLayout } from '@/components/layouts';
import { Button } from '@/components/ui/button';
import { Select } from '@/components/ui/form';
import {
  useCompromiso,
  useCompromisosEstudiante,
} from '@/features/pagos/api/compromisos';
import { CronogramaCuotas } from '@/features/pagos/components/cronograma-cuotas';

// Figma > Personal Administrativo > "P8 · Compromiso 2026 — Cronograma de cuotas" (HU-37)
const CompromisoPagoRoute = () => {
  const { estudianteId = '' } = useParams();
  const compromisos = useCompromisosEstudiante(estudianteId);
  const [elegido, setElegido] = React.useState<string>();
  const compromisoId = elegido ?? compromisos.data?.[0]?.id;
  const compromiso = useCompromiso(compromisoId);
  const e = compromiso.data?.estudiante;

  return (
    <ContentLayout
      title={e?.nombre ?? 'Compromiso de pago'}
      breadcrumbs={['Pensiones', 'Estado de cuenta', e?.nombre ?? '…']}
      subtitle={
        e
          ? `DNI ${e.numeroDocumento} · Apoderado: ${e.apoderado ?? '—'} · Sección actual: ${e.seccion ?? '—'}`
          : undefined
      }
      actions={
        <>
          <Select
            aria-label="Compromiso"
            wrapperClassName="w-48"
            value={compromisoId ?? ''}
            onChange={(ev) => setElegido(ev.target.value)}
            options={(compromisos.data ?? []).map((c) => ({
              label: `Compromiso ${c.anio}`,
              value: c.id,
            }))}
          />
          <Button
            variant="secondary"
            disabled
            icon={<CirclePlus className="size-4" />}
          >
            Cuota especial
          </Button>
          <Button
            variant="primary"
            disabled
            icon={<Percent className="size-4" />}
          >
            Añadir descuento
          </Button>
        </>
      }
    >
      <CronogramaCuotas
        compromiso={compromiso.data}
        isLoading={compromisos.isLoading || compromiso.isLoading}
      />
    </ContentLayout>
  );
};

export default CompromisoPagoRoute;
