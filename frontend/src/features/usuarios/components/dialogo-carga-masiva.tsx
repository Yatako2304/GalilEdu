import {
  Check,
  CircleAlert,
  CircleCheck,
  CloudUpload,
  Download,
  FileSpreadsheet,
  Info,
  MailCheck,
  Trash2,
  Upload,
} from 'lucide-react';
import * as React from 'react';

import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogTitle,
} from '@/components/ui/dialog';
import { ResultadoCargaMasiva } from '@/types/api';
import { cn } from '@/utils/cn';

import {
  descargarDetalle,
  descargarPlantilla,
  useCargarUsuarios,
  useValidarCarga,
  validarArchivoCarga,
} from '../api/carga-masiva';

const PASOS = ['Subir archivo', 'Validar datos', 'Resumen'];

const COLUMNAS = [
  'Tipo de documento',
  'N.° de documento',
  'Apellido paterno',
  'Apellido materno',
  'Nombres',
  'Fecha de nacimiento',
  'Nacionalidad',
  'Lugar de nacimiento',
  'Sexo',
  'Estado civil',
  'Grado de instrucción',
  'Celular',
  'Correo electrónico',
  'Teléfono de casa',
  'Profesión u ocupación',
  'Centro de trabajo',
  'Cargo',
  'Distrito',
  'Dirección',
  'Exalumno (Sí / No)',
  'Rol',
];

const Pasos = ({ actual }: { actual: number }) => (
  <ol className="grid grid-cols-3 border-t border-stroke-secondary">
    {PASOS.map((p, i) => (
      <li key={p} className="-mt-3 flex flex-col items-center gap-1">
        <span
          className={cn(
            'inline-flex size-6 items-center justify-center rounded-full border text-caption-1',
            i < actual &&
              'border-transparent bg-surface-accent-secondary text-white',
            i === actual &&
              'border-content-primary bg-white text-content-primary',
            i > actual &&
              'border-stroke-primary bg-white text-content-tertiary',
          )}
        >
          {i < actual ? (
            <Check className="size-3.5" aria-hidden="true" />
          ) : (
            i + 1
          )}
        </span>
        <span
          className={cn(
            'text-body-3',
            i === actual ? 'text-content-primary' : 'text-content-tertiary',
          )}
        >
          {p}
        </span>
      </li>
    ))}
  </ol>
);

const tamano = (bytes: number) =>
  bytes < 1024 * 1024
    ? `${Math.max(1, Math.round(bytes / 1024))} KB`
    : `${(bytes / 1024 / 1024).toFixed(2)} MB`;

// Figma > Administrador > modales "Carga masiva de usuarios" (pasos 1, 2 y 3) · HU-8
export const DialogoCargaMasiva = ({
  open,
  onCerrar,
}: {
  open: boolean;
  onCerrar: () => void;
}) => {
  const [paso, setPaso] = React.useState(0);
  const [archivo, setArchivo] = React.useState<File | null>(null);
  const [errorArchivo, setErrorArchivo] = React.useState<string | null>(null);
  const [arrastrando, setArrastrando] = React.useState(false);
  const [validacion, setValidacion] =
    React.useState<ResultadoCargaMasiva | null>(null);
  const [resultado, setResultado] = React.useState<ResultadoCargaMasiva | null>(
    null,
  );
  const entrada = React.useRef<HTMLInputElement>(null);
  const validar = useValidarCarga();
  const cargar = useCargarUsuarios();

  const reiniciar = () => {
    setPaso(0);
    setArchivo(null);
    setErrorArchivo(null);
    setValidacion(null);
    setResultado(null);
  };

  const cerrar = () => {
    reiniciar();
    onCerrar();
  };

  const elegir = (elegido: File | undefined) => {
    if (!elegido) return;
    const problema = validarArchivoCarga(elegido);
    setErrorArchivo(problema);
    setArchivo(problema ? null : elegido);
  };

  const continuar = () => {
    if (!archivo) return;
    if (paso === 0) {
      validar.mutate(archivo, {
        onSuccess: (r) => {
          setValidacion(r);
          setPaso(1);
        },
      });
    } else if (paso === 1) {
      cargar.mutate(archivo, {
        onSuccess: (r) => {
          setResultado(r);
          setPaso(2);
        },
      });
    }
  };

  return (
    <Dialog open={open} onOpenChange={(abierto) => !abierto && cerrar()}>
      <DialogContent className="flex max-h-[92vh] max-w-[800px] flex-col gap-0 overflow-hidden rounded-[12px] border-stroke-tertiary bg-white p-0">
        <div className="border-b border-stroke-tertiary px-6 py-5">
          <DialogTitle className="text-h6 font-semibold text-content-primary">
            Carga masiva de usuarios
          </DialogTitle>
          <DialogDescription className="text-body-3 text-content-secondary">
            Registra múltiples usuarios a la vez desde un archivo Excel o CSV
          </DialogDescription>
        </div>

        <div className="flex-1 space-y-6 overflow-y-auto px-6 pb-6 pt-10">
          <Pasos actual={paso} />

          {paso === 0 && (
            <>
              <div className="flex flex-wrap items-center justify-between gap-3 rounded-[12px] bg-surface-accent-light px-4 py-3">
                <p className="text-body-3 text-content-accent-primary">
                  Descarga la plantilla, complétala y súbela para registrar
                  usuarios en lote.
                </p>
                <Button
                  variant="primary-light"
                  size="sm"
                  icon={<Download className="size-4" />}
                  onClick={() => descargarPlantilla()}
                >
                  Descargar plantilla
                </Button>
              </div>

              <input
                ref={entrada}
                type="file"
                accept=".xlsx,.xls,.csv"
                className="sr-only"
                aria-label="Archivo de usuarios"
                onChange={(e) => {
                  elegir(e.target.files?.[0]);
                  e.target.value = '';
                }}
              />
              {archivo ? (
                <div className="flex items-center gap-3 rounded-[12px] border border-stroke-tertiary bg-surface-secondary px-4 py-3">
                  <FileSpreadsheet
                    className="size-8 shrink-0 text-content-success"
                    aria-hidden="true"
                  />
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-body-2 text-content-primary">
                      {archivo.name}
                    </p>
                    <p className="text-body-4 text-content-tertiary">
                      {tamano(archivo.size)}
                    </p>
                  </div>
                  <Button
                    variant="ghost"
                    size="sm"
                    icon={<Trash2 className="size-4" />}
                    onClick={() => setArchivo(null)}
                  >
                    Quitar
                  </Button>
                </div>
              ) : (
                <div
                  onDragOver={(e) => {
                    e.preventDefault();
                    setArrastrando(true);
                  }}
                  onDragLeave={() => setArrastrando(false)}
                  onDrop={(e) => {
                    e.preventDefault();
                    setArrastrando(false);
                    elegir(e.dataTransfer.files?.[0]);
                  }}
                  className={cn(
                    'flex flex-col items-center gap-2 rounded-[12px] border-2 border-dashed px-6 py-10 text-center transition-colors',
                    arrastrando
                      ? 'border-stroke-focus bg-surface-accent-light'
                      : 'border-stroke-primary bg-white',
                  )}
                >
                  <span className="inline-flex size-12 items-center justify-center rounded-full bg-surface-accent-light text-content-accent-secondary">
                    <CloudUpload className="size-6" aria-hidden="true" />
                  </span>
                  <p className="text-body-2 font-semibold text-content-primary">
                    Arrastra y suelta tu archivo aquí
                  </p>
                  <p className="text-body-4 text-content-tertiary">
                    XLSX, XLS o CSV (Máx. 10 MB)
                  </p>
                  <Button
                    variant="primary-light"
                    icon={<Upload className="size-4" />}
                    onClick={() => entrada.current?.click()}
                  >
                    Seleccionar archivo
                  </Button>
                </div>
              )}
              {errorArchivo && (
                <p className="text-body-4 text-content-error">{errorArchivo}</p>
              )}

              <div className="rounded-[12px] border border-stroke-tertiary bg-surface-secondary px-5 py-4">
                <p className="mb-3 flex items-center gap-2 text-label-2 font-medium text-content-primary">
                  <Info className="size-4" aria-hidden="true" /> Columnas
                  requeridas
                </p>
                <ol className="list-inside list-decimal gap-x-8 space-y-1 text-body-4 text-content-secondary sm:columns-2">
                  {COLUMNAS.map((c) => (
                    <li key={c}>{c}</li>
                  ))}
                </ol>
                <p className="mt-3 text-body-4 text-content-tertiary">
                  Obligatorias: tipo y N.° de documento, apellido paterno,
                  nombres, celular, correo y rol. Varios roles se separan con
                  coma (Docente, Tutor). Formatos aceptados: .xlsx, .xls o .csv
                  · Tamaño máximo: 10 MB
                </p>
              </div>
            </>
          )}

          {paso === 1 && validacion && (
            <>
              {validacion.errores.length > 0 ? (
                <div className="flex gap-3 rounded-[12px] border border-stroke-error-secondary bg-surface-error-light px-4 py-3">
                  <CircleAlert
                    className="mt-0.5 size-5 shrink-0 text-content-error"
                    aria-hidden="true"
                  />
                  <div>
                    <p className="text-body-2 text-content-primary">
                      Se encontraron {validacion.errores.length}{' '}
                      {validacion.errores.length === 1
                        ? 'registro'
                        : 'registros'}{' '}
                      con error
                    </p>
                    <p className="text-body-4 text-content-secondary">
                      {validacion.total} registros detectados ·{' '}
                      {validacion.validos} válidos · {validacion.errores.length}{' '}
                      con errores
                    </p>
                  </div>
                </div>
              ) : (
                <div className="flex gap-3 rounded-[12px] border border-stroke-success bg-surface-success-light px-4 py-3">
                  <CircleCheck
                    className="mt-0.5 size-5 shrink-0 text-content-success"
                    aria-hidden="true"
                  />
                  <div>
                    <p className="text-body-2 text-content-primary">
                      Todos los registros son válidos
                    </p>
                    <p className="text-body-4 text-content-secondary">
                      {validacion.total} registros detectados · listos para
                      crear
                    </p>
                  </div>
                </div>
              )}

              {validacion.errores.length > 0 && (
                <div className="max-h-72 overflow-y-auto rounded-[12px] border border-stroke-tertiary">
                  <table className="w-full text-body-3">
                    <thead className="sticky top-0 bg-surface-secondary">
                      <tr>
                        <th className="w-20 px-4 py-3 text-left font-semibold text-content-tertiary">
                          Fila
                        </th>
                        <th className="px-4 py-3 text-left font-semibold text-content-tertiary">
                          Error encontrado
                        </th>
                      </tr>
                    </thead>
                    <tbody>
                      {validacion.errores.map((e) => (
                        <tr
                          key={e.fila}
                          className="border-t border-stroke-tertiary"
                        >
                          <td className="px-4 py-3 text-content-secondary">
                            {e.fila}
                          </td>
                          <td className="px-4 py-3 text-content-secondary">
                            {e.mensaje}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}

              {validacion.errores.length > 0 && (
                <p className="text-body-4 text-content-tertiary">
                  {validacion.validos > 0
                    ? `Solo se registrarán los ${validacion.validos} registros válidos. Los ${validacion.errores.length} registros con error no se crearán: puedes corregirlos y subirlos en un nuevo archivo o registrarlos manualmente.`
                    : 'Ningún registro es válido. Corrige el archivo y vuelve a subirlo.'}
                </p>
              )}
            </>
          )}

          {paso === 2 && resultado && (
            <div className="flex flex-col items-center gap-4 text-center">
              <span className="inline-flex size-16 items-center justify-center rounded-full bg-surface-success-light text-content-success">
                <CircleCheck className="size-8" aria-hidden="true" />
              </span>
              <div>
                <p className="text-h5 font-semibold text-content-primary">
                  Carga masiva completada
                </p>
                <p className="text-body-3 text-content-secondary">
                  Se procesaron {resultado.total} registros del archivo{' '}
                  {resultado.nombreArchivo}.
                </p>
              </div>
              <div className="grid w-full grid-cols-1 gap-4 text-left sm:grid-cols-2">
                <div className="rounded-[12px] border border-stroke-success bg-surface-success-light p-4">
                  <p className="text-h4 font-semibold text-content-success">
                    {resultado.creados}
                  </p>
                  <p className="text-label-2 font-semibold text-content-primary">
                    Usuarios creados
                  </p>
                  <p className="text-body-4 text-content-secondary">
                    Ya pueden ingresar a GalilEdu
                  </p>
                </div>
                <div className="rounded-[12px] border border-stroke-error bg-surface-error-light p-4">
                  <p className="text-h4 font-semibold text-content-error">
                    {resultado.errores.length}
                  </p>
                  <p className="text-label-2 font-semibold text-content-primary">
                    Registros con error
                  </p>
                  <p className="text-body-4 text-content-secondary">
                    No se crearon · revisa el detalle
                  </p>
                </div>
              </div>
              <div className="flex w-full gap-3 rounded-[12px] bg-surface-accent-light px-4 py-3 text-left">
                <MailCheck
                  className="mt-0.5 size-4 shrink-0 text-content-accent-primary"
                  aria-hidden="true"
                />
                <div>
                  <p className="text-label-2 font-medium text-content-primary">
                    Credenciales enviadas
                  </p>
                  <p className="text-body-4 text-content-tertiary">
                    Cada usuario creado recibió sus credenciales de acceso en su
                    correo electrónico.
                  </p>
                </div>
              </div>
              {resultado.errores.length > 0 && (
                <div className="flex w-full flex-wrap items-center gap-3 rounded-[12px] border border-stroke-tertiary px-4 py-3 text-left">
                  <CircleAlert
                    className="size-5 shrink-0 text-content-warning"
                    aria-hidden="true"
                  />
                  <div className="min-w-0 flex-1">
                    <p className="text-label-2 font-medium text-content-primary">
                      Corrige los {resultado.errores.length} registros
                      pendientes
                    </p>
                    <p className="text-body-4 text-content-tertiary">
                      Descarga el detalle de los errores, corrígelos y vuelve a
                      subir el archivo.
                    </p>
                  </div>
                  <Button
                    variant="primary-light"
                    size="sm"
                    icon={<Download className="size-4" />}
                    onClick={() =>
                      descargarDetalle(
                        resultado.nombreArchivo,
                        resultado.errores,
                      )
                    }
                  >
                    Descargar detalle
                  </Button>
                </div>
              )}
            </div>
          )}
        </div>

        <div className="flex justify-end gap-3 border-t border-stroke-tertiary px-6 py-4">
          {paso < 2 ? (
            <>
              <Button variant="primary-light" onClick={cerrar}>
                Cancelar
              </Button>
              <Button
                onClick={continuar}
                disabled={
                  !archivo || (paso === 1 && (validacion?.validos ?? 0) === 0)
                }
                isLoading={validar.isPending || cargar.isPending}
              >
                Continuar
              </Button>
            </>
          ) : (
            <Button onClick={cerrar}>Finalizar</Button>
          )}
        </div>
      </DialogContent>
    </Dialog>
  );
};
