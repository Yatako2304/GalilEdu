import { Eye, MessageSquareWarning, Paperclip, Trash2 } from 'lucide-react';
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
import { DialogoConfirmacion } from '@/components/ui/dialog/dialogo-confirmacion';
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
  useEliminarDocumento,
  useObservarDocumento,
  useSubirDocumento,
  useValidarMatricula,
} from '../api/matricula-detalle';
import { descargarPlantillaDelPeriodo } from '../api/plantillas-matricula';
import { problemaDelArchivo, tamanoLegible } from '../archivos-matricula';

import { BotonCambiarSeccion } from './boton-cambiar-seccion';
import { BotonPlantilla } from './boton-plantilla';
import { DialogoObservarDocumento } from './dialogo-observar-documento';
import { HistorialCambiosSeccion } from './historial-cambios-seccion';

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

const DatosMatricula = ({ matricula }: { matricula: DetalleMatriculaDto }) => {
  const seccionGuardada = matricula.seccion
    ? `${matricula.seccion.etiqueta}${matricula.seccion.tutor ? ` · Tutor: ${matricula.seccion.tutor}` : ''}`
    : '—';
  const campos = [
    ['Estudiante', matricula.nombre],
    ['DNI', matricula.documento],
    ['Grado', matricula.grado],
    ['Sección', matricula.seccionAsignada ? seccionGuardada : 'Por asignar'],
    ...(matricula.seccionSeleccionada
      ? [['Sección reservada', seccionGuardada]]
      : []),
    ['Apoderado', matricula.apoderado ?? '—'],
    ['Contacto', matricula.contacto || '—'],
    ['Contacto de emergencia', matricula.contactoEmergencia ?? '—'],
    ['Información médica', matricula.informacionMedica],
    ...(matricula.extemporanea
      ? [
          ['Tipo de matrícula', 'Extemporánea'],
          [
            'Motivo de matrícula extemporánea',
            matricula.motivoExtemporanea ?? '—',
          ],
        ]
      : []),
  ];

  return (
    <Card>
      <CardHeader>
        <CardTitle>Datos del estudiante y apoderado</CardTitle>
      </CardHeader>
      <CardContent>
        <dl className="grid gap-x-8 gap-y-4 sm:grid-cols-2">
          {campos.map(([etiqueta, valor]) => (
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
};

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
  const eliminar = useEliminarDocumento(matricula.id);
  const observar = useObservarDocumento(matricula.id);
  const [observando, setObservando] = React.useState<{
    id: string;
    nombre: string;
  } | null>(null);
  const [eliminando, setEliminando] = React.useState<{
    id: string;
    nombre: string;
  } | null>(null);
  const { addNotification } = useNotifications();
  const puedeEliminarDocumentos =
    matricula.estado === 'SECCION_RESERVADA' ||
    matricula.estado === 'EN_VALIDACION';

  const confirmarObservacion = async (observacion: string) => {
    if (!observando) return;
    try {
      await observar.mutateAsync({ documentoId: observando.id, observacion });
      addNotification({
        type: 'success',
        title: 'Documento observado',
        message: `${observando.nombre} volvió al apoderado para su corrección.`,
      });
      setObservando(null);
    } catch {
      // El cliente HTTP muestra el mensaje específico del backend.
    }
  };

  const confirmarEliminacion = async () => {
    if (!eliminando) return;
    try {
      const actualizado = await eliminar.mutateAsync(eliminando.id);
      addNotification({
        type: 'success',
        title: 'Documento eliminado',
        message:
          actualizado.estado === 'SECCION_RESERVADA' &&
          matricula.estado === 'EN_VALIDACION'
            ? `${eliminando.nombre} fue eliminado y la matrícula volvió a sección reservada.`
            : `${eliminando.nombre} fue eliminado de la matrícula.`,
      });
      setEliminando(null);
    } catch {
      // El cliente HTTP muestra el mensaje específico del backend.
    }
  };

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
              ? `Puedes subir, reemplazar${puedeEliminarDocumentos ? ' o eliminar' : ''} documentos: PDF, JPG o PNG, máximo 10 MB. Al reemplazar, el documento vuelve a revisión.`
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
                  {documento.tienePlantilla &&
                    archivo.estado !== 'APROBADO' && (
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
                  {matricula.estado === 'EN_VALIDACION' &&
                    archivo.estado === 'EN_REVISION' && (
                      <Button
                        type="button"
                        variant="ghost"
                        size="sm"
                        icon={
                          <MessageSquareWarning
                            className="size-4"
                            aria-hidden="true"
                          />
                        }
                        aria-label={`Observar ${documento.nombre}`}
                        onClick={() =>
                          setObservando({
                            id: documento.id,
                            nombre: documento.nombre,
                          })
                        }
                      >
                        Observar
                      </Button>
                    )}
                  {matricula.puedeEditarDocumentos &&
                    archivo.estado !== 'APROBADO' && (
                      <>
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
                        {puedeEliminarDocumentos && (
                          <Button
                            type="button"
                            variant="error-light"
                            size="sm"
                            disabled={eliminar.isPending}
                            icon={
                              <Trash2 className="size-4" aria-hidden="true" />
                            }
                            aria-label={`Eliminar ${documento.nombre}`}
                            onClick={() =>
                              setEliminando({
                                id: documento.id,
                                nombre: documento.nombre,
                              })
                            }
                          >
                            Eliminar
                          </Button>
                        )}
                      </>
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
        {observando && (
          <DialogoObservarDocumento
            documento={observando.nombre}
            guardando={observar.isPending}
            alConfirmar={(o) => void confirmarObservacion(o)}
            onCerrar={() => setObservando(null)}
          />
        )}
        <DialogoConfirmacion
          open={Boolean(eliminando)}
          onOpenChange={(abierto) => !abierto && setEliminando(null)}
          tipo="eliminar"
          titulo="¿Eliminar documento?"
          descripcion={`Se eliminará ${eliminando?.nombre ?? 'este documento'} de la matrícula. Para continuar con la validación tendrás que volver a subirlo.`}
          textoConfirmar="Eliminar"
          cargando={eliminar.isPending}
          onConfirmar={() => void confirmarEliminacion()}
        />
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
        La cuota de matrícula se genera sola cuando apruebas la documentación y
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
  const validar = useValidarMatricula(matricula.id);
  const [validando, setValidando] = React.useState(false);
  // Un doble clic en "Aprobar" mandaba dos validaciones: la segunda fallaba porque la matrícula ya no estaba en validación.
  const validacionEnCurso = React.useRef(false);
  const config = configuracionEstado(matricula.estado, matricula.codigo);
  const enProceso =
    matricula.estado === 'SECCION_RESERVADA' ||
    matricula.estado === 'EN_VALIDACION';
  const volver = paths.app.administrativo.matriculasDelPeriodo.getHref(
    matricula.periodoId,
  );

  const confirmarValidacion = async () => {
    if (validacionEnCurso.current) return;
    validacionEnCurso.current = true;
    try {
      await validar.mutateAsync();
      addNotification({
        type: 'success',
        title: 'Documentación aprobada',
        message: `La matrícula de ${matricula.nombre} quedó pendiente de pago.`,
      });
    } catch {
      // El cliente HTTP muestra el mensaje específico del backend.
    } finally {
      validacionEnCurso.current = false;
    }
    setValidando(false);
  };

  return (
    <div className="flex flex-col gap-6">
      <Alert type={config.tipo} title={config.titulo}>
        {config.detalle}
      </Alert>

      <Card className="p-6">
        <Stepper
          steps={PASOS}
          current={matricula.pasoActual === 4 ? 5 : matricula.pasoActual}
        />
      </Card>

      <div className="grid items-start gap-6 xl:grid-cols-[minmax(0,1fr)_minmax(280px,420px)]">
        <div className="flex flex-col gap-6">
          <DatosMatricula matricula={matricula} />
          <DocumentosMatricula matricula={matricula} />
          <HistorialCambiosSeccion matriculaId={matricula.id} />
        </div>
        {enProceso ? (
          <PagoPendienteValidacion />
        ) : matricula.estado === 'PENDIENTE_PAGO' ||
          matricula.estado === 'MATRICULADO' ? (
          pagoMatricula
        ) : null}
      </div>

      <div className="flex flex-wrap items-center justify-between gap-3">
        <BotonCambiarSeccion matricula={matricula} />
        <div className="ml-auto flex gap-3">
          <Button asChild variant="tertiary">
            <Link to={volver}>{enProceso ? 'Cancelar' : 'Salir'}</Link>
          </Button>
          {enProceso && (
            <Button
              disabled={!matricula.puedeValidar}
              onClick={() => setValidando(true)}
            >
              Validar
            </Button>
          )}
        </div>
      </div>

      <DialogoConfirmacion
        open={validando}
        onOpenChange={setValidando}
        tipo="guardar"
        titulo="¿Aprobar documentación?"
        descripcion={`Se aprobarán los documentos de ${matricula.nombre} y la matrícula pasará a pendiente de pago.`}
        textoConfirmar="Aprobar"
        cargando={validar.isPending}
        onConfirmar={() => void confirmarValidacion()}
      />
    </div>
  );
};
