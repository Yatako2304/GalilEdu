import {
  ArrowRight,
  CalendarDays,
  GraduationCap,
  ListChecks,
} from 'lucide-react';
import { Link } from 'react-router';

import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { paths } from '@/config/paths';
import { EstadoMatriculaApoderado } from '@/types/api';

import { fechaPeriodo } from './consulta-matricula';
import { seccionMatricula } from './presentacion-matricula';

export const ResumenMatricula = ({
  estado,
}: {
  estado?: EstadoMatriculaApoderado;
}) => {
  if (!estado) return null;

  return (
    <Card>
      <CardHeader>
        <div>
          <CardTitle>Matrícula {estado.periodo.anio}</CardTitle>
          <p className="mt-1 text-body-3 text-content-quartiary">
            {estado.periodo.abierto
              ? 'El periodo está abierto'
              : 'El periodo está cerrado'}
          </p>
        </div>
      </CardHeader>
      <CardContent className="space-y-4">
        <dl className="space-y-3">
          <div className="flex items-start gap-3">
            <GraduationCap
              className="mt-0.5 size-5 shrink-0 text-content-accent-secondary"
              aria-hidden="true"
            />
            <div>
              <dt className="text-caption-1 text-content-quartiary">Grado</dt>
              <dd className="text-body-3 text-content-primary">
                {estado.matricula?.grado ?? estado.estudiante.grado} ·{' '}
                {seccionMatricula(
                  estado.matricula?.estado,
                  estado.matricula?.seccion,
                )}
              </dd>
            </div>
          </div>
          <div className="flex items-start gap-3">
            <CalendarDays
              className="mt-0.5 size-5 shrink-0 text-content-accent-secondary"
              aria-hidden="true"
            />
            <div>
              <dt className="text-caption-1 text-content-quartiary">
                Fecha límite
              </dt>
              <dd className="text-body-3 text-content-primary">
                {fechaPeriodo(estado.periodo.cierre)}
              </dd>
            </div>
          </div>
          <div className="flex items-start gap-3">
            <ListChecks
              className="mt-0.5 size-5 shrink-0 text-content-accent-secondary"
              aria-hidden="true"
            />
            <div>
              <dt className="text-caption-1 text-content-quartiary">Pasos</dt>
              <dd className="text-body-3 text-content-primary">
                Estado → Documentos → Pago
              </dd>
            </div>
          </div>
        </dl>
        <Button
          asChild
          className="w-full"
          icon={<ArrowRight className="size-4" />}
          iconPosition="right"
        >
          <Link to={paths.app.apoderado.matricula.getHref()}>
            Consultar matrícula
          </Link>
        </Button>
      </CardContent>
    </Card>
  );
};
