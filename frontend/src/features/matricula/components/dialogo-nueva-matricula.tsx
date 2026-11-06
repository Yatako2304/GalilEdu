import { Eye, Paperclip, RefreshCw, Search, UserRound } from 'lucide-react';
import * as React from 'react';

import { Alert } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogTitle,
} from '@/components/ui/dialog';
import { FileItem } from '@/components/ui/files';
import { Input } from '@/components/ui/form';
import { useNotifications } from '@/components/ui/notifications';
import { Stepper } from '@/components/ui/stepper';
import { Tag } from '@/components/ui/tag';
import { useDebouncedValue } from '@/hooks/use-debounced-value';
import { PreparacionMatricula, SeccionOpcion } from '@/types/api';

import {
  useBuscarEstudiantes,
  useCrearMatricula,
  usePreparacionMatricula,
  useSeccionesDelAlumno,
} from '../api/matriculas-periodo';
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

const DatosDelEstudiante = ({
  datos,
  seccion,
}: {
  datos: PreparacionMatricula;
  seccion: { etiqueta: string; tutor: string | null } | null;
}) => (
  <Card>
    <CardHeader>
      <CardTitle>Datos del estudiante y apoderado</CardTitle>
    </CardHeader>
    <CardContent>
      <dl className="grid gap-x-8 gap-y-4 sm:grid-cols-2">
        {[
          ['Estudiante', datos.nombre],
          ['DNI', datos.documento],
          ['Grado', datos.grado],
          [
            'Sección',
            seccion
              ? `${seccion.etiqueta}${seccion.tutor ? ` · Tutor: ${seccion.tutor}` : ''}`
              : 'Sin vacantes',
          ],
          ['Apoderado', datos.apoderado ?? '—'],
          ['Contacto', datos.contacto || '—'],
          ['Contacto de emergencia', datos.contactoEmergencia ?? '—'],
          ['Información médica', datos.informacionMedica],
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

const DocumentosDeLaMatricula = ({
  periodoId,
  datos,
  archivos,
  alElegir,
}: {
  periodoId: string;
  datos: PreparacionMatricula;
  archivos: Record<string, File>;
  alElegir: (documentoId: string, archivo: File) => void;
}) => {
  const entrada = React.useRef<HTMLInputElement>(null);
  const [elegido, setElegido] = React.useState<string | null>(null);
  const [error, setError] = React.useState<string | null>(null);

  const pedir = (documentoId: string) => {
    setElegido(documentoId);
    entrada.current?.click();
  };

  const recibir = (archivo: File | undefined) => {
    if (!archivo || !elegido) return;
    const problema = problemaDelArchivo(archivo);
    setError(problema);
    if (!problema) alElegir(elegido, archivo);
  };

  const plantilla = (documento: PreparacionMatricula['documentos'][number]) =>
    documento.tienePlantilla ? (
      <BotonPlantilla
        documento={documento.nombre}
        alDescargar={() =>
          descargarPlantillaDelPeriodo(
            periodoId,
            documento.id,
            documento.nombre,
          )
        }
      />
    ) : null;

  const ver = (archivo: File) => {
    const url = URL.createObjectURL(archivo);
    window.open(url, '_blank', 'noopener');
    // El navegador ya abrió la pestaña: se libera el enlace temporal.
    setTimeout(() => URL.revokeObjectURL(url), 60_000);
  };

  return (
    <Card>
      <CardHeader>
        <div>
          <CardTitle>Documentos de matrícula</CardTitle>
          <p className="mt-1 text-body-4 text-content-tertiary">
            Puedes subirlos ahora o después: el apoderado también puede hacerlo.
            PDF, JPG o PNG, máximo 10 MB.
          </p>
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
            recibir(e.target.files?.[0]);
            e.target.value = '';
          }}
        />
        {datos.documentos.map((documento) => {
          const archivo = archivos[documento.id];
          return archivo ? (
            <FileItem
              key={documento.id}
              name={archivo.name}
              mimeType={archivo.type}
              meta={`${documento.nombre} · ${tamanoLegible(archivo.size)} · se guardará con la matrícula`}
              actions={
                <>
                  {plantilla(documento)}
                  <Button
                    type="button"
                    size="icon"
                    variant="tertiary"
                    aria-label={`Ver ${documento.nombre}`}
                    onClick={() => ver(archivo)}
                  >
                    <Eye className="size-4" aria-hidden="true" />
                  </Button>
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    onClick={() => pedir(documento.id)}
                  >
                    Reemplazar
                  </Button>
                </>
              }
            />
          ) : (
            <FileItem
              key={documento.id}
              name={documento.nombre}
              meta={documento.descripcion ?? undefined}
              actions={
                <>
                  <Tag size="sm" color="warning">
                    Pendiente
                  </Tag>
                  {plantilla(documento)}
                  <Button
                    type="button"
                    variant="primary-light"
                    size="sm"
                    icon={<Paperclip className="size-4" aria-hidden="true" />}
                    aria-label={`Subir ${documento.nombre}`}
                    onClick={() => pedir(documento.id)}
                  >
                    Subir
                  </Button>
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

/** Buscador por nombre o DNI: lista a quienes están inscritos para el año del periodo y no tienen matrícula. */
const BuscadorDeEstudiantes = ({
  periodoId,
  alElegir,
}: {
  periodoId: string;
  alElegir: (estudianteId: string) => void;
}) => {
  const [texto, setTexto] = React.useState('');
  const busqueda = useDebouncedValue(texto.trim());
  const { data: resultados = [], isLoading } = useBuscarEstudiantes(
    periodoId,
    busqueda,
  );

  return (
    <div className="space-y-3">
      <Input
        label="Estudiante"
        aria-label="Buscar estudiante por nombre o DNI"
        placeholder="Busca por nombre o DNI"
        value={texto}
        onChange={(e) => setTexto(e.target.value)}
        startAdornment={<Search className="size-4" aria-hidden="true" />}
      />
      <ul className="max-h-60 space-y-1 overflow-y-auto" aria-live="polite">
        {resultados.map((e) => (
          <li key={e.id}>
            <button
              type="button"
              onClick={() => alElegir(e.id)}
              className="flex w-full items-center gap-3 rounded-lg px-3 py-2 text-left hover:bg-surface-primary-hover focus:outline-none focus-visible:ring-2 focus-visible:ring-stroke-focus"
            >
              <UserRound
                className="size-4 text-icon-accent-primary"
                aria-hidden="true"
              />
              <span className="flex-1">
                <span className="block text-label-1 text-content-primary">
                  {e.nombre}
                </span>
                <span className="block text-body-4 text-content-tertiary">
                  DNI {e.documento} · {e.grado}
                </span>
              </span>
            </button>
          </li>
        ))}
      </ul>
      {!isLoading && resultados.length === 0 && (
        <p className="text-body-4 text-content-tertiary">
          {busqueda
            ? 'No hay estudiantes sin matrícula que coincidan.'
            : 'No hay estudiantes inscritos para este año que estén sin matrícula.'}
        </p>
      )}
    </div>
  );
};

// Figma > Personal Administrativo > A6 · "Nueva matrícula" (paso 1: datos y documentos). HU-24, HU-72, HU-29.
export const DialogoNuevaMatricula = ({
  periodoId,
  onCerrar,
}: {
  periodoId: string;
  onCerrar: () => void;
}) => {
  const { addNotification } = useNotifications();
  const [estudianteId, setEstudianteId] = React.useState<string>();
  const [archivos, setArchivos] = React.useState<Record<string, File>>({});
  const { data: datos, isLoading } = usePreparacionMatricula(
    periodoId,
    estudianteId,
  );
  const crear = useCrearMatricula(periodoId);
  const [cambiando, setCambiando] = React.useState(false);
  const [elegida, setElegida] = React.useState<SeccionOpcion>();
  const secciones = useSeccionesDelAlumno(periodoId, estudianteId, cambiando);
  // La sección que se mostrará y se enviará: la elegida o, si no, la que propone el servidor.
  const seccion = elegida ?? datos?.seccion ?? null;

  const elegirEstudiante = (id?: string) => {
    setEstudianteId(id);
    setArchivos({});
    setElegida(undefined);
  };

  const todosCargados =
    !!datos &&
    datos.documentos.length > 0 &&
    datos.documentos.every((d) => archivos[d.id]);

  // Evita un segundo envío (doble clic) antes de que el botón se deshabilite.
  const enviando = React.useRef(false);

  const guardar = async () => {
    if (!datos || !seccion || enviando.current) return;
    enviando.current = true;
    try {
      const creada = await crear.mutateAsync({
        estudianteId: datos.estudianteId,
        seccionId: elegida?.id,
        archivos,
      });
      addNotification({
        type: creada.seccionReasignada ? 'warning' : 'success',
        title: 'Matrícula registrada',
        message: creada.seccionReasignada
          ? `La sección elegida se llenó: ${datos.nombre} quedó en ${creada.seccion}.`
          : `${datos.nombre} quedó con la sección ${creada.seccion} reservada.`,
      });
      onCerrar();
    } catch {
      // El cliente HTTP muestra el mensaje específico del backend.
      enviando.current = false;
    }
  };

  return (
    <Dialog open onOpenChange={(abierto) => !abierto && onCerrar()}>
      <DialogContent className="flex max-h-[92vh] max-w-[880px] flex-col gap-0 overflow-hidden rounded-xl border-stroke-tertiary bg-white p-0">
        <div className="flex-1 space-y-5 overflow-y-auto p-6">
          <div>
            <DialogDescription className="mb-2 text-body-4 text-content-quartiary">
              Paso 1 · Datos y documentos
            </DialogDescription>
            <DialogTitle className="text-h6 font-semibold text-content-primary">
              Nueva matrícula
            </DialogTitle>
          </div>

          <Stepper steps={PASOS} current={todosCargados ? 2 : 1} />

          {!estudianteId ? (
            <BuscadorDeEstudiantes
              periodoId={periodoId}
              alElegir={elegirEstudiante}
            />
          ) : isLoading || !datos ? (
            <p className="text-body-3 text-content-tertiary">
              Cargando datos del estudiante…
            </p>
          ) : (
            <>
              <div className="flex items-center justify-between gap-3">
                <Tag color={datos.tipo === 'NUEVO' ? 'info' : 'success'}>
                  {datos.tipo === 'NUEVO' ? 'Estudiante nuevo' : 'Continuador'}
                </Tag>
                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  onClick={() => elegirEstudiante(undefined)}
                >
                  Elegir otro estudiante
                </Button>
              </div>
              {!datos.seccion && (
                <Alert
                  type="error"
                  title={`No hay vacantes en ninguna sección de ${datos.grado}.`}
                >
                  No se puede matricular hasta que se libere un cupo o se amplíe
                  la capacidad de una sección.
                </Alert>
              )}
              <DatosDelEstudiante datos={datos} seccion={seccion} />
              <DocumentosDeLaMatricula
                periodoId={periodoId}
                datos={datos}
                archivos={archivos}
                alElegir={(documentoId, archivo) =>
                  setArchivos((actual) => ({
                    ...actual,
                    [documentoId]: archivo,
                  }))
                }
              />
              <Card>
                <CardHeader>
                  <CardTitle>Pago de matrícula</CardTitle>
                </CardHeader>
                <CardContent>
                  <p className="text-body-3 text-content-tertiary">
                    La cuota se genera cuando la documentación esté validada y
                    no exista deuda pendiente.
                  </p>
                </CardContent>
              </Card>
            </>
          )}
        </div>

        <div className="flex flex-wrap items-center justify-between gap-3 border-t border-stroke-tertiary p-4">
          <Button
            type="button"
            variant="secondary"
            disabled={!seccion}
            onClick={() => setCambiando(true)}
            icon={<RefreshCw className="size-4" aria-hidden="true" />}
          >
            Cambiar sección
          </Button>
          <div className="ml-auto flex gap-3">
            <Button type="button" variant="tertiary" onClick={onCerrar}>
              Cancelar
            </Button>
            <Button
              type="button"
              disabled={!seccion || crear.isPending}
              onClick={guardar}
            >
              Guardar
            </Button>
          </div>
        </div>
      </DialogContent>
      {cambiando && (
        <DialogoCambiarSeccion
          secciones={
            secciones.data && {
              ...secciones.data,
              seleccionadaId: seccion?.id ?? null,
            }
          }
          cargando={secciones.isLoading}
          guardando={false}
          onCerrar={() => setCambiando(false)}
          alConfirmar={(id) => {
            setElegida(secciones.data?.secciones.find((s) => s.id === id));
            setCambiando(false);
          }}
        />
      )}
    </Dialog>
  );
};
