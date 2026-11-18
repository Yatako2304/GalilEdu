import { ArrowRight } from 'lucide-react';

import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import { formatFechaHoraLima } from '@/utils/format';

import { useCambiosDeSeccion } from '../api/matricula-detalle';

// HU-26 · historial de cambios de sección de la matrícula (más reciente primero). Solo aparece si hubo cambios.
export const HistorialCambiosSeccion = ({
  matriculaId,
}: {
  matriculaId: string;
}) => {
  const cambios = useCambiosDeSeccion(matriculaId);
  if (!cambios.data || cambios.data.length === 0) return null;

  return (
    <Card>
      <CardHeader>
        <CardTitle>Cambios de sección</CardTitle>
        <CardDescription>Del más reciente al más antiguo.</CardDescription>
      </CardHeader>
      <CardContent>
        <ul className="divide-y divide-stroke-tertiary">
          {cambios.data.map((c) => (
            <li key={c.id} className="space-y-1 py-3 first:pt-0 last:pb-0">
              <p className="flex items-center gap-2 text-label-1 text-content-primary">
                {c.origen}
                <ArrowRight className="size-4" aria-label="a" />
                {c.destino}
              </p>
              <p className="text-body-4 text-content-secondary">
                {c.motivo ?? 'Sin motivo registrado'}
                {c.observacion ? ` · ${c.observacion}` : ''}
              </p>
              <p className="text-caption-1 text-content-quartiary">
                {formatFechaHoraLima(c.realizadoEn)}
                {c.realizadoPor ? ` · ${c.realizadoPor}` : ''}
              </p>
            </li>
          ))}
        </ul>
      </CardContent>
    </Card>
  );
};
