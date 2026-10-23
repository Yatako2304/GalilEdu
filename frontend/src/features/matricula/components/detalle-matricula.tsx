import { Eye, RefreshCw } from 'lucide-react';
import { ReactNode } from 'react';
import { Link } from 'react-router';

import { Alert, AlertType } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import { FileItem } from '@/components/ui/files';
import { Stepper } from '@/components/ui/stepper';
import { Tag } from '@/components/ui/tag';
import { paths } from '@/config/paths';

import { MatriculaFlujo } from './datos-flujo-matricula';

const PASOS = [
  { label: 'Datos y documentos' },
  { label: 'Validación' },
  { label: 'Pago de matrícula' },
  { label: 'Matriculado' },
];

const documentos = [
  ['Ficha de matrícula firmada', 'ficha_matricula.pdf'],
  ['DNI del estudiante', 'dni_estudiante.pdf'],
  ['DNI del apoderado', 'dni_apoderado.pdf'],
  ['Declaración jurada del apoderado', 'declaracion_jurada.pdf'],
] as const;

const configuracionEstado = (matricula: MatriculaFlujo) => {
  if (matricula.estado === 'MATRICULADO')
    return {
      paso: 4,
      tipo: 'success' as AlertType,
      etiqueta: 'Matriculado',
      color: 'success' as const,
      titulo: `Matrícula registrada con el código ${matricula.codigo}.`,
      detalle:
        'Se generó el cronograma de pensiones y se notificó al apoderado.',
    };
  if (matricula.estado === 'PENDIENTE_PAGO')
    return {
      paso: 3,
      tipo: 'warning' as AlertType,
      etiqueta: 'Pendiente de pago',
      color: 'warning' as const,
      titulo: 'Documentación validada y sin deuda pendiente.',
      detalle:
        'La cuota de matrícula se consulta o genera desde la tarjeta de pago.',
    };
  return {
    paso: 2,
    tipo: 'warning' as AlertType,
    etiqueta: 'Sección reservada',
    color: 'info' as const,
    titulo: 'Matrícula pendiente de validación',
    detalle: 'Revisa y valida cada uno para continuar con la matrícula.',
  };
};

const DatosMatricula = ({ matricula }: { matricula: MatriculaFlujo }) => (
  <Card>
    <CardHeader>
      <CardTitle>Datos del estudiante y apoderado</CardTitle>
    </CardHeader>
    <CardContent>
      <dl className="grid gap-x-8 gap-y-4 sm:grid-cols-2">
        {[
          ['Estudiante', matricula.nombre],
          ['DNI', matricula.dni],
          ['Grado', matricula.grado],
          ['Sección', `${matricula.seccion} · Tutor: ${matricula.tutor}`],
          ['Apoderado', matricula.apoderado],
          ['Contacto', matricula.contacto],
          ['Contacto de emergencia', matricula.emergencia],
          ['Información médica', matricula.informacionMedica],
        ].map(([etiqueta, valor]) => (
          <div key={etiqueta}>
            <dt className="text-caption-1 text-content-quartiary">
              {etiqueta}
            </dt>
            <dd className="mt-1 text-body-3 font-medium text-content-primary">
              {valor}
            </dd>
          </div>
        ))}
      </dl>
    </CardContent>
  </Card>
);

const DocumentosMatricula = ({ matricula }: { matricula: MatriculaFlujo }) => {
  const aprobados = matricula.estado !== 'SECCION_RESERVADA';
  return (
    <Card>
      <CardHeader>
        <div>
          <CardTitle>Documentos de matrícula</CardTitle>
          <CardDescription>
            {aprobados
              ? 'Documentación validada.'
              : 'Puedes subir, reemplazar o eliminar documentos para mantener el expediente completo.'}
          </CardDescription>
        </div>
      </CardHeader>
      <CardContent className="space-y-3">
        {documentos.map(([nombre, archivo]) => (
          <FileItem
            key={archivo}
            name={nombre}
            meta={archivo}
            actions={
              <>
                {aprobados && (
                  <Tag size="sm" color="success">
                    Aprobado
                  </Tag>
                )}
                <Button
                  type="button"
                  size="icon"
                  variant="tertiary"
                  disabled
                  aria-label={`Ver ${nombre}`}
                >
                  <Eye className="size-4" aria-hidden="true" />
                </Button>
              </>
            }
          />
        ))}
      </CardContent>
    </Card>
  );
};

const PagoPendienteValidacion = () => (
  <Card>
    <CardHeader>
      <CardTitle>Pago de matrícula</CardTitle>
    </CardHeader>
    <CardContent>
      <p className="text-body-3 text-content-tertiary">
        La cuota de matrícula se genera cuando la documentación está validada y
        no existe deuda pendiente.
      </p>
    </CardContent>
  </Card>
);

// Figma > Personal Administrativo > A5, B6 y B7.
export const DetalleMatricula = ({
  matricula,
  periodoId,
  pagoMatricula,
}: {
  matricula: MatriculaFlujo;
  periodoId: string;
  pagoMatricula?: ReactNode;
}) => {
  const estado = configuracionEstado(matricula);
  const volver =
    paths.app.administrativo.matriculasDelPeriodo.getHref(periodoId);

  return (
    <div className="flex flex-col gap-6">
      <Alert type={estado.tipo} title={estado.titulo}>
        {estado.detalle}
      </Alert>

      <Card className="p-6">
        <Stepper steps={PASOS} current={estado.paso} />
      </Card>

      <div className="grid items-start gap-6 lg:grid-cols-[minmax(0,1fr)_minmax(280px,420px)]">
        <div className="flex flex-col gap-6">
          <DatosMatricula matricula={matricula} />
          <DocumentosMatricula matricula={matricula} />
        </div>
        {matricula.estado === 'SECCION_RESERVADA' ? (
          <PagoPendienteValidacion />
        ) : pagoMatricula ? (
          pagoMatricula
        ) : (
          <Card>
            <CardHeader>
              <CardTitle>Pago de matrícula</CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-body-3 text-content-tertiary">
                Esta matrícula es parte de la maqueta y no consulta la API.
              </p>
            </CardContent>
          </Card>
        )}
      </div>

      <div className="flex flex-wrap items-center justify-between gap-3">
        <Button
          variant="secondary"
          disabled
          icon={<RefreshCw className="size-4" aria-hidden="true" />}
        >
          Cambiar sección
        </Button>
        <div className="ml-auto flex gap-3">
          <Button asChild variant="tertiary">
            <Link to={volver}>
              {matricula.estado === 'SECCION_RESERVADA' ? 'Cancelar' : 'Salir'}
            </Link>
          </Button>
          {matricula.estado === 'SECCION_RESERVADA' && (
            <Button disabled>Validar</Button>
          )}
        </div>
      </div>
    </div>
  );
};

export const etiquetaDetalleMatricula = (matricula: MatriculaFlujo) => {
  const estado = configuracionEstado(matricula);
  return (
    <Tag color={estado.color} size="md">
      {estado.etiqueta}
    </Tag>
  );
};
