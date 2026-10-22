import { ExternalLink } from 'lucide-react';
import { Link } from 'react-router';

import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Spinner } from '@/components/ui/spinner';
import { Tag } from '@/components/ui/tag';
import { paths } from '@/config/paths';

import {
  useCuotaMatricula,
  useGenerarCuotaMatricula,
} from '../api/cuota-matricula';

import { soles } from './formato';

const fecha = (valor: string) =>
  new Intl.DateTimeFormat('es-PE').format(new Date(`${valor}T12:00:00`));

const etiquetas = {
  PENDIENTE: 'Pendiente de pago',
  EN_PROCESO: 'En proceso',
  PAGADA_PARCIAL: 'Pago parcial',
  PAGADA: 'Pagada',
  ANULADA: 'Anulada',
} as const;

export const PagoMatriculaCard = ({ matriculaId }: { matriculaId: string }) => {
  const consulta = useCuotaMatricula(matriculaId);
  const generar = useGenerarCuotaMatricula(matriculaId);
  const cuota = consulta.data;

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-label-1 font-semibold">
          Pago de matrícula
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        {consulta.isLoading ? (
          <div className="flex min-h-32 items-center justify-center">
            <Spinner />
          </div>
        ) : cuota ? (
          <>
            <dl className="space-y-2 rounded-[10px] bg-surface-secondary px-5 py-4 text-body-3">
              <Dato nombre="Concepto" valor={cuota.concepto} />
              <Dato nombre="Monto" valor={soles(cuota.monto)} />
              {cuota.montoDescuento > 0 && (
                <Dato
                  nombre="Descuento"
                  valor={`− ${soles(cuota.montoDescuento)}`}
                />
              )}
              <div className="flex items-start justify-between gap-4">
                <dt className="text-content-quartiary">Estado</dt>
                <dd>
                  <Tag
                    size="sm"
                    color={cuota.estado === 'PAGADA' ? 'success' : 'warning'}
                  >
                    {etiquetas[cuota.estado]}
                    {cuota.fechaPago ? ` · ${fecha(cuota.fechaPago)}` : ''}
                  </Tag>
                </dd>
              </div>
              <Dato
                nombre="Vencimiento"
                valor={fecha(cuota.fechaVencimiento)}
              />
              <Dato nombre="Responsable" valor={cuota.responsable} />
            </dl>
            <Button
              asChild
              variant="tertiary"
              size="sm"
              icon={<ExternalLink className="size-4" />}
            >
              <Link
                to={paths.app.administrativo.compromisoPago.getHref(
                  cuota.estudianteId,
                )}
              >
                Ver compromiso de pago
              </Link>
            </Button>
          </>
        ) : (
          <div className="space-y-4 text-body-3 text-content-quartiary">
            <p>La cuota de matrícula todavía no fue generada.</p>
            <Button
              onClick={() => generar.mutate()}
              isLoading={generar.isPending}
            >
              Generar cuota de matrícula
            </Button>
          </div>
        )}
      </CardContent>
    </Card>
  );
};

const Dato = ({ nombre, valor }: { nombre: string; valor: string }) => (
  <div className="flex items-start justify-between gap-4">
    <dt className="text-content-quartiary">{nombre}</dt>
    <dd className="text-right font-medium text-content-primary">{valor}</dd>
  </div>
);
