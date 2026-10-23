import { Link } from 'react-router';

import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Spinner } from '@/components/ui/spinner';
import { Tag } from '@/components/ui/tag';
import { paths } from '@/config/paths';
import { AnioEscolarDetalle, CodigoRequisito } from '@/types/api';

import {
  anioActivo,
  useAniosEscolares,
  useRequisitosAnio,
} from '../../api/anios-escolares';

import { PestanaAnio } from './pestanas-anio';
import { BotonRevisar, RequisitoCard } from './requisito-card';

/**
 * A dónde lleva el "Revisar" de cada requisito: una pestaña del mismo año, o (el plan de evaluación) la pantalla
 * de Competencias, donde se configura.
 */
export const destinoDeRevision = (
  codigo: CodigoRequisito,
): { pestana: PestanaAnio } | { ruta: string } => {
  switch (codigo) {
    case 'PERIODOS':
      return { pestana: 'periodos' };
    case 'SECCIONES':
    case 'TUTORES':
      return { pestana: 'secciones' };
    case 'ESTRUCTURA_HORARIA':
      return { pestana: 'estructura-horaria' };
    case 'PLAN_EVALUACION':
      return { ruta: paths.app.administrativo.competencias.getHref() };
  }
};

type RequisitosAnioProps = {
  anio: Pick<AnioEscolarDetalle, 'id'>;
  onRevisar: (pestana: PestanaAnio) => void;
};

// Figma > CI-06 > "Card - Requisitos": "Antes de activar el año". Solo se muestra con el año PLANIFICADO.
export const RequisitosAnio = ({ anio, onRevisar }: RequisitosAnioProps) => {
  const requisitos = useRequisitosAnio(anio.id, true);
  const anios = useAniosEscolares();
  const otroActivo = anios.data ? anioActivo(anios.data) : undefined;

  if (requisitos.isLoading) {
    return (
      <Card className="flex h-40 items-center justify-center">
        <Spinner />
      </Card>
    );
  }
  // Si falla, el aviso ya salió como notificación; la pantalla sigue sirviendo sin esta tarjeta.
  if (!requisitos.data) return null;

  return (
    <Card className="flex flex-col gap-4 p-6">
      <div className="flex items-start gap-4">
        <div className="flex min-w-0 flex-1 flex-col gap-1">
          <h2 className="text-label-1 font-semibold text-content-primary">
            Antes de activar el año
          </h2>
          <p className="text-body-4 text-content-tertiary">
            Los tres primeros requisitos son obligatorios. Los demás se pueden
            completar después de activarlo.
          </p>
        </div>
        {otroActivo && (
          <Tag color="warning" size="md">
            El año {otroActivo.anio} sigue activo
          </Tag>
        )}
      </div>

      <ul className="grid gap-3 md:grid-cols-2 xl:grid-cols-3">
        {[...requisitos.data.obligatorios, ...requisitos.data.recomendados].map(
          (requisito) => {
            const destino = destinoDeRevision(requisito.codigo);
            return (
              <RequisitoCard
                key={requisito.codigo}
                requisito={requisito}
                revisar={
                  'pestana' in destino ? (
                    <BotonRevisar
                      aria-label={`Revisar ${requisito.texto}`}
                      onClick={() => onRevisar(destino.pestana)}
                    />
                  ) : (
                    <Button
                      asChild
                      variant="link"
                      size="sm"
                      className="h-auto p-0"
                    >
                      <Link
                        to={destino.ruta}
                        aria-label={`Revisar ${requisito.texto}`}
                      >
                        Revisar
                      </Link>
                    </Button>
                  )
                }
              />
            );
          },
        )}
      </ul>
    </Card>
  );
};
