import { Eye, Paperclip, RefreshCw } from 'lucide-react';
import * as React from 'react';
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
import { useNotifications } from '@/components/ui/notifications';
import { Stepper } from '@/components/ui/stepper';
import { Tag } from '@/components/ui/tag';
import { paths } from '@/config/paths';
import {
  ArchivoGuardado,
  DetalleMatricula as DetalleMatriculaDto,
  EstadoMatricula,
} from '@/types/api';

import {
  abrirDocumentoDeMatricula,
  useCambiarSeccion,
  useSeccionesDeMatricula,
  useSubirDocumento,
} from '../api/matricula-detalle';
import { descargarPlantillaDelPeriodo } from '../api/plantillas-matricula';
import { problemaDelArchivo, tamanoLegible } from '../archivos-matricula';

import { BotonPlantilla } from './boton-plantilla';
import { DialogoCambiarSeccion } from './dialogo-cambiar-seccion';

const PASOS = [
  { label: 'Datos y documentos' },
  { label: 'Validación' },
  { label: 'Pago de matrícula' },
  { label: 'Matriculado' },
];

const configuracionEstado = (
  estado: EstadoMatricula,
  codigo: string | null,
): {
  tipo: AlertType;
  etiqueta: string;
  color: 'success' | 'warning' | 'info' | 'error';
  titulo: string;
  detalle: string;
} => {
  switch (estado) {
    case 'MATRICULADO':
      return {
        tipo: 'success',
        etiqueta: 'Matriculado',
        color: 'success',
        titulo: `Matrícula registrada con el código ${codigo}.`,
        detalle:
          'Se generó el cronograma de pensiones y se notificó al apoderado.',
      };
    case 'PENDIENTE_PAGO':
      return {
        tipo: 'warning',
        etiqueta: 'Pendiente de pago',
        color: 'warning',
        titulo: 'Documentación validada y sin deuda pendiente.',
        detalle:
          'La cuota de matrícula se consulta o genera desde la tarjeta de pago.',
      };
    case 'EN_VALIDACION':
      return {
        tipo: 'warning',
        etiqueta: 'En validación',
        color: 'info',
        titulo: 'Documentos en revisión',
        detalle: 'Revisa y valida cada uno para continuar con la matrícula.',
      };
    case 'RETIRADO':
      return {
        tipo: 'error',
        etiqueta: 'Retirado',
        color: 'error',
        titulo: 'El estudiante fue retirado.',
        detalle: 'Esta matrícula ya no admite cambios.',
      };
    case 'ANULADA':
      return {
        tipo: 'error',
        etiqueta: 'Anulada',
        color: 'error',
        titulo: 'La matrícula fue anulada.',
        detalle: 'Esta matrícula ya no admite cambios.',
      };
    default:
      return {
        tipo: 'warning',
        etiqueta: 'Sección reservada',
        color: 'info',
        titulo: 'Matrícula pendiente de validación',
        detalle: 'Revisa y valida cada uno para continuar con la matrícula.',
      };
  }
};

export const etiquetaDetalleMatricula = (
  estado: EstadoMatricula,
  codigo: string | null = null,
) => {
  const config = configuracionEstado(estado, codigo);
  return (
    <Tag color={config.color} size="md">
      {config.etiqueta}
    </Tag>
  );
};

const DatosMatricula = ({ matricula }: { matricula: DetalleMatriculaDto }) => (
  <Card>
    <CardHeader>
      <CardTitle>Datos del estudiante y apoderado</CardTitle>
    </CardHeader>
    <CardContent>
      <dl className="grid gap-x-8 gap-y-4 sm:grid-cols-2">
        {[
          ['Estudiante', matricula.nombre],
          ['DNI', matricula.documento],
          ['Grado', matricula.grado],
          [
            'Sección',
            matricula.seccion
              ? `${matricula.seccion.etiqueta}${matricula.seccion.tutor ? ` · Tutor: ${matricula.seccion.tutor}` : ''}`
              : '—',
          ],
          ['Apoderado', matricula.apoderado ?? '—'],
          ['Contacto', matricula.contacto || '—'],
          ['Contacto de emergencia', matricula.contactoEmergencia ?? '—'],
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

const ESTADO_ARCHIVO: Record<
  ArchivoGuardado['estado'],
  { etiqueta: string; color: 'info' | 'success' | 'error' }
> = {
  EN_REVISION: { etiqueta: 'En revisión', color: 'info' },
  APROBADO: { etiqueta: 'Aprobado', color: 'success' },
  OBSERVADO: { etiqueta: 'Observado', color: 'error' },
};

const DocumentosMatricula = ({
  matricula,
}: {
  matricula: DetalleMatriculaDto;
}) => {
  const entrada = React.useRef<HTMLInputElement>(null);
  const [elegido, setElegido] = React.useState<string | null>(null);
  const [error, setError] = React.useState<string | null>(null);
  const subir = useSubirDocumento(matricula.id);
  const { addNotification } = useNotifications();

  const pedir = (documentoId: string) => {
    setElegido(documentoId);
    entrada.current?.click();
  };

  const recibir = async (archivo: File | undefined) => {
    if (!archivo || !elegido) return;
    const problema = problemaDelArchivo(archivo);
    setError(problema);
    if (problema) return;
    try {
      await subir.mutateAsync({ documentoId: elegido, archivo });
      addNotification({
        type: 'success',
        title: 'Documento guardado',
        message: `${archivo.name} quedó en revisión.`,
      });
    } catch {
      // El cliente HTTP muestra el mensaje específico del backend.
    }
  };

  return (
    <Card>
      <CardHeader>
        <div>
          <CardTitle>Documentos de matrícula</CardTitle>
          <CardDescription>
            {matricula.puedeEditarDocumentos
              ? 'Puedes subir o reemplazar documentos: PDF, JPG o PNG, máximo 10 MB. Al reemplazar, el documento vuelve a revisión.'
              : 'Esta matrícula ya no admite cambios en sus documentos.'}
          </CardDescription>
        </div>
      </CardHeader>
      <CardContent className="space-y-3">
        <input
          ref={entrada}
          type="file"
          accept="application/pdf,image/jpeg,image/png"
          className="sr-only"
          aria-label="Archivo del documento"
          onChange={(e) => {
            void recibir(e.target.files?.[0]);
            e.target.value = '';
          }}
        />
        {matricula.documentos.map((documento) => {
          const archivo = documento.archivo;
          if (!archivo) {
            return (
              <FileItem
                key={documento.id}
                name={documento.nombre}
                meta={documento.descripcion ?? undefined}
                actions={
                  <>
                    <Tag size="sm" color="warning">
                      Pendiente
                    </Tag>
                    {documento.tienePlantilla && (
                      <BotonPlantilla
                        documento={documento.nombre}
                        alDescargar={() =>
                          descargarPlantillaDelPeriodo(
                            matricula.periodoId,
                            documento.id,
                            documento.nombre,
                          )
                        }
                      />
                    )}
                    {matricula.puedeEditarDocumentos && (
                      <Button
                        type="button"
                        variant="primary-light"
                        size="sm"
                        disabled={subir.isPending}
                        icon={
                          <Paperclip className="size-4" aria-hidden="true" />
                        }
                        aria-label={`Subir ${documento.nombre}`}
                        onClick={() => pedir(documento.id)}
                      >
                        Subir
                      </Button>
                    )}
                  </>
                }
              />
            );
          }
          const estado = ESTADO_ARCHIVO[archivo.estado];
          return (
            <FileItem
              key={documento.id}
              name={archivo.nombre}
              mimeType={archivo.tipoMime}
              meta={
                <>
                  {documento.nombre} · {tamanoLegible(archivo.tamanoBytes)}
                  {archivo.observacion && ` · ${archivo.observacion}`}
                </>
              }
              actions={
                <>
                  <Tag size="sm" color={estado.color}>
                    {estado.etiqueta}
                  </Tag>
                  {documento.tienePlantilla && (
                    <BotonPlantilla
                      documento={documento.nombre}
                      alDescargar={() =>
                        descargarPlantillaDelPeriodo(
                          matricula.periodoId,
                          documento.id,
                          documento.nombre,
                        )
                      }
                    />
                  )}
                  <Button
                    type="button"
                    size="icon"
                    variant="tertiary"
                    aria-label={`Ver ${documento.nombre}`}
                    onClick={() =>
                      abrirDocumentoDeMatricula(matricula.id, documento.id)
                    }
                  >
                    <Eye className="size-4" aria-hidden="true" />
                  </Button>
                  {matricula.puedeEditarDocumentos && (
                    <Button
                      type="button"
                      variant="ghost"
                      size="sm"
                      disabled={subir.isPending}
                      aria-label={`Reemplazar ${documento.nombre}`}
                      onClick={() => pedir(documento.id)}
                    >
                      Reemplazar
                    </Button>
                  )}
                </>
              }
            />
          );
        })}
        {error && (
          <p role="alert" className="text-body-4 text-content-error">
            {error}
          </p>
        )}
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

// Figma > Personal Administrativo > A5, B6 y B7 · "Detalle de matrícula". HU-24.
export const DetalleMatricula = ({
  matricula,
  pagoMatricula,
}: {
  matricula: DetalleMatriculaDto;
  /** Tarjeta de pago (HU-36/37) que arma la ruta: un feature no importa de otro. */
  pagoMatricula?: React.ReactNode;
}) => {
  const { addNotification } = useNotifications();
  const [cambiando, setCambiando] = React.useState(false);
  const secciones = useSeccionesDeMatricula(matricula.id, cambiando);
  const cambiarSeccion = useCambiarSeccion(matricula.id);
  const config = configuracionEstado(matricula.estado, matricula.codigo);
  const enProceso =
    matricula.estado === 'SECCION_RESERVADA' ||
    matricula.estado === 'EN_VALIDACION';
  const volver = paths.app.administrativo.matriculasDelPeriodo.getHref(
    matricula.periodoId,
  );

  const confirmarCambio = async (seccionId: string) => {
    try {
      const nuevo = await cambiarSeccion.mutateAsync(seccionId);
      addNotification({
        type: 'success',
        title: 'Sección cambiada',
        message: `${matricula.nombre} pasó a ${nuevo.seccion?.etiqueta}.`,
      });
      setCambiando(false);
    } catch {
      // El cliente HTTP muestra el mensaje específico del backend.
    }
  };

  return (
    <div className="flex flex-col gap-6">
      <Alert type={config.tipo} title={config.titulo}>
        {config.detalle}
      </Alert>

      <Card className="p-6">
        <Stepper steps={PASOS} current={matricula.pasoActual} />
      </Card>

      <div className="grid items-start gap-6 xl:grid-cols-[minmax(0,1fr)_minmax(280px,420px)]">
        <div className="flex flex-col gap-6">
          <DatosMatricula matricula={matricula} />
          <DocumentosMatricula matricula={matricula} />
        </div>
        {enProceso ? (
          <PagoPendienteValidacion />
        ) : matricula.estado === 'PENDIENTE_PAGO' ||
          matricula.estado === 'MATRICULADO' ? (
          pagoMatricula
        ) : null}
      </div>

      <div className="flex flex-wrap items-center justify-between gap-3">
        <Button
          variant="secondary"
          disabled={!matricula.puedeCambiarSeccion}
          onClick={() => setCambiando(true)}
          icon={<RefreshCw className="size-4" aria-hidden="true" />}
        >
          Cambiar sección
        </Button>
        <div className="ml-auto flex gap-3">
          <Button asChild variant="tertiary">
            <Link to={volver}>{enProceso ? 'Cancelar' : 'Salir'}</Link>
          </Button>
          {enProceso && (
            // La validación de documentos es HU-32.
            <Button disabled>Validar</Button>
          )}
        </div>
      </div>

      {cambiando && (
        <DialogoCambiarSeccion
          secciones={secciones.data}
          cargando={secciones.isLoading}
          guardando={cambiarSeccion.isPending}
          onCerrar={() => setCambiando(false)}
          alConfirmar={confirmarCambio}
        />
      )}
    </div>
  );
};
