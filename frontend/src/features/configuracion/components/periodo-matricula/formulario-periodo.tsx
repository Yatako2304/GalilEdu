import { zodResolver } from '@hookform/resolvers/zod';
import { ChevronLeft } from 'lucide-react';
import * as React from 'react';
import { useForm } from 'react-hook-form';
import { Link, useNavigate } from 'react-router';

import { Alert } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { useNotifications } from '@/components/ui/notifications';
import { Spinner } from '@/components/ui/spinner';
import { paths } from '@/config/paths';
import { FormularioPeriodoMatricula } from '@/types/api';

import {
  useCrearPeriodoMatricula,
  useEditarPeriodoMatricula,
  useRefrescarPeriodosMatricula,
} from '../../api/mutaciones-periodo-matricula';
import {
  useAniosDisponiblesMatricula,
  useFormularioNuevoPeriodo,
} from '../../api/periodos-matricula';
import {
  emparejarPlantillas,
  quitarPlantilla,
  subirPlantillas,
} from '../../api/plantillas';

import { DocumentosRequeridos } from './documentos-requeridos';
import {
  DocumentoBorrador,
  SeccionParaValidar,
  ValoresPeriodoMatricula,
  crearPeriodoMatriculaSchema,
  documentosDePeriodo,
  solicitudDePeriodo,
  totalDeVacantes,
  valoresDePeriodo,
} from './esquemas';
import { SeccionFechas } from './seccion-fechas';
import { TablaVacantes } from './tabla-vacantes';
import { TarjetaMonto } from './tarjeta-monto';
import { TarjetasResumen } from './tarjetas-resumen';

const VALORES_VACIOS: ValoresPeriodoMatricula = {
  anioEscolarId: '',
  fechaInicio: '',
  horaInicio: '',
  fechaCierre: '',
  horaCierre: '',
  vacantes: [],
};

type FormularioPeriodoProps = {
  /** Un periodo que ya existe: sin él, el formulario es el de un periodo nuevo. */
  periodo?: FormularioPeriodoMatricula;
  /** Muestra el periodo sin poder cambiarlo (Figma: la página "Ver"). */
  soloLectura?: boolean;
};

const TITULO_FORMULARIO = {
  nuevo: 'Nuevo periodo de matrícula',
  editar: 'Editar periodo de matrícula',
  ver: 'Periodo de matrícula',
};

// Figma > Personal Administrativo > pantalla 13 > "Nuevo periodo de matrícula" (HU-23); editar y ver son el mismo
// formulario con el periodo cargado.
//
// "Guardar configuración" guarda el periodo con sus fechas, vacantes y documentos de una vez; recién con los ids de
// documento que devuelve se suben las plantillas elegidas (y se quitan las que se marcaron). Si alguna falla, el
// periodo ya está guardado y se puede volver a intentar desde su edición (un periodo programado se puede editar).
export const FormularioPeriodo = ({
  periodo,
  soloLectura = false,
}: FormularioPeriodoProps) => {
  const modo = !periodo ? 'nuevo' : soloLectura ? 'ver' : 'editar';
  const navigate = useNavigate();
  const { addNotification } = useNotifications();
  const crear = useCrearPeriodoMatricula();
  const editar = useEditarPeriodoMatricula();
  const refrescar = useRefrescarPeriodosMatricula();
  const [documentos, setDocumentos] = React.useState<DocumentoBorrador[]>(() =>
    periodo ? documentosDePeriodo(periodo) : [],
  );
  const [guardando, setGuardando] = React.useState(false);

  // Un periodo que ya existe no cambia de año: solo el de nuevo pide los años que aún no tienen periodo.
  const anios = useAniosDisponiblesMatricula(modo === 'nuevo');
  // El año más cercano primero: es el que se está por habilitar.
  const aniosOrdenados = periodo
    ? [{ id: periodo.anioEscolarId, anio: periodo.anio }]
    : [...(anios.data ?? [])].sort((a, b) => a.anio - b.anio);

  // Las reglas de las vacantes dependen de las secciones del año elegido, que llegan después.
  const seccionesParaValidar = React.useRef<SeccionParaValidar[]>([]);
  const form = useForm<ValoresPeriodoMatricula>({
    resolver: ((valores: unknown, contexto: unknown, opciones: unknown) =>
      zodResolver(crearPeriodoMatriculaSchema(seccionesParaValidar.current))(
        valores as never,
        contexto as never,
        opciones as never,
      )) as never,
    defaultValues: periodo ? valoresDePeriodo(periodo) : VALORES_VACIOS,
  });
  const { watch, setValue, getValues, clearErrors } = form;

  // Abre con el primer año disponible ya elegido.
  const primerAnio = modo === 'nuevo' ? aniosOrdenados[0]?.id : undefined;
  React.useEffect(() => {
    if (primerAnio && !getValues('anioEscolarId')) {
      setValue('anioEscolarId', primerAnio);
    }
  }, [primerAnio, getValues, setValue]);

  const anioElegido = watch('anioEscolarId');
  const nuevo = useFormularioNuevoPeriodo(
    modo === 'nuevo' ? anioElegido || undefined : undefined,
  );
  const datos = periodo ?? nuevo.data;
  seccionesParaValidar.current = (datos?.secciones ?? []).map((s) => ({
    seccionId: s.seccionId,
    etiqueta: s.etiqueta,
    alumnosOcupanVacante: s.alumnosOcupanVacante,
  }));

  // Al llegar las secciones de un año, sus capacidades pasan a ser las vacantes del formulario. Solo al cambiar de
  // año: una recarga en segundo plano no debe pisar lo que se está escribiendo. Un periodo que ya existe ya trae las
  // suyas.
  const anioCargado = modo === 'nuevo' ? datos?.anioEscolarId : undefined;
  React.useEffect(() => {
    if (modo !== 'nuevo' || !datos) return;
    setValue(
      'vacantes',
      datos.secciones.map((s) => ({
        seccionId: s.seccionId,
        capacidad: String(s.capacidad),
      })),
    );
    clearErrors('vacantes');
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [anioCargado]);

  const sinAnios =
    modo === 'nuevo' && anios.isSuccess && anios.data.length === 0;
  const puedeGuardar =
    !guardando &&
    (modo === 'editar' || (!sinAnios && !!datos && datos.tarifarioConfirmado));

  const guardar = form.handleSubmit(async (valores) => {
    setGuardando(true);
    try {
      let guardado: FormularioPeriodoMatricula;
      try {
        const solicitud = solicitudDePeriodo(valores, documentos);
        guardado = periodo
          ? await editar.mutateAsync({
              id: periodo.id as string,
              data: { ...solicitud, anioEscolarId: null },
            })
          : await crear.mutateAsync(solicitud);
      } catch {
        return; // el motivo ya salió como notificación
      }
      const periodoId = guardado.id as string;
      const pendientes = emparejarPlantillas(
        documentos
          .filter((d) => d.archivo)
          .map((d) => ({ nombre: d.nombre, archivo: d.archivo as File })),
        guardado.documentos,
      );
      const { fallidas } = await subirPlantillas(periodoId, pendientes);

      // Las plantillas que se marcaron para quitar (solo al editar). Una que se cambió por otra ya se reemplazó.
      const aQuitar = documentos.filter((d) => d.id && d.quitarPlantilla);
      for (const documento of aQuitar) {
        try {
          await quitarPlantilla(periodoId, documento.id as string);
        } catch {
          fallidas.push(documento.nombre);
        }
      }
      if (pendientes.length > 0 || aQuitar.length > 0) await refrescar();

      if (fallidas.length > 0) {
        addNotification({
          type: 'warning',
          title: 'Periodo guardado, pero faltan plantillas',
          message:
            modo === 'nuevo'
              ? `No se pudo subir la plantilla de: ${fallidas.join(', ')}. Vuelve a subirla desde "Editar".`
              : `No se pudo actualizar la plantilla de: ${fallidas.join(', ')}. Vuelve a intentarlo.`,
        });
        // Un periodo nuevo pasa a su edición para reintentar; uno que ya se editaba se recarga con lo que quedó.
        if (modo === 'nuevo') {
          navigate(
            paths.app.administrativo.periodoMatriculaEditar.getHref(periodoId),
          );
        }
        return;
      }
      addNotification({
        type: 'success',
        title:
          modo === 'nuevo'
            ? 'Periodo de matrícula guardado'
            : 'Periodo de matrícula actualizado',
        message: guardado.titulo,
      });
      navigate(paths.app.administrativo.matricula.getHref());
    } finally {
      setGuardando(false);
    }
  });

  if (modo === 'nuevo' && anios.isLoading) {
    return (
      <div className="flex h-60 items-center justify-center">
        <Spinner />
      </div>
    );
  }

  const vacantes = watch('vacantes');
  const bloqueado = sinAnios || soloLectura;

  return (
    <form
      noValidate
      onSubmit={guardar}
      aria-label={TITULO_FORMULARIO[modo]}
      className="flex flex-col gap-6"
    >
      {sinAnios && (
        <Alert
          type="warning"
          title="No hay años escolares disponibles para un nuevo periodo de matrícula."
        />
      )}
      {nuevo.isError && (
        <Alert type="error" title="No pudimos cargar las secciones del año.">
          Intenta de nuevo en unos minutos.
        </Alert>
      )}
      {soloLectura && periodo && !periodo.editable && (
        <Alert
          type="info"
          title="Una vez habilitado, el periodo no se puede editar; solo deshabilitar."
        />
      )}

      <TarjetasResumen
        fechaInicio={watch('fechaInicio')}
        fechaCierre={watch('fechaCierre')}
        vacantesTotales={datos ? totalDeVacantes(vacantes) : null}
        monto={datos?.montoMatricula ?? null}
        documentos={documentos.length}
      />

      <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_minmax(0,420px)]">
        <div className="flex flex-col gap-6">
          <SeccionFechas
            form={form}
            anios={aniosOrdenados.map((a) => ({
              label: String(a.anio),
              value: a.id,
            }))}
            anioFijo={modo !== 'nuevo'}
            deshabilitado={bloqueado}
          />
          <TablaVacantes
            form={form}
            secciones={datos ? datos.secciones : anioElegido ? null : []}
            total={totalDeVacantes(vacantes)}
            mensajeVacio={
              anioElegido
                ? 'Este año aún no tiene secciones activas.'
                : 'Elige el año académico para ver sus secciones.'
            }
            deshabilitado={bloqueado}
          />
        </div>
        <div className="flex flex-col gap-6">
          <TarjetaMonto
            anio={datos?.anio ?? null}
            anioEscolarId={datos?.anioEscolarId ?? null}
            monto={datos?.montoMatricula ?? null}
            tarifarioConfirmado={datos?.tarifarioConfirmado ?? true}
          />
          <DocumentosRequeridos
            documentos={documentos}
            onChange={setDocumentos}
            deshabilitado={sinAnios}
            periodoId={periodo?.id ?? undefined}
            soloLectura={soloLectura}
          />
        </div>
      </div>

      {soloLectura ? (
        <div className="flex justify-end gap-3">
          <Button asChild variant="tertiary">
            <Link to={paths.app.administrativo.matricula.getHref()}>
              <ChevronLeft className="size-4" aria-hidden="true" />
              Volver a los periodos de matrícula
            </Link>
          </Button>
          {periodo?.editable && (
            <Button asChild>
              <Link
                to={paths.app.administrativo.periodoMatriculaEditar.getHref(
                  periodo.id as string,
                )}
              >
                Editar
              </Link>
            </Button>
          )}
        </div>
      ) : (
        <div className="flex justify-end gap-3">
          <Button
            type="button"
            variant="tertiary"
            onClick={() =>
              navigate(paths.app.administrativo.matricula.getHref())
            }
          >
            Cancelar
          </Button>
          <Button type="submit" isLoading={guardando} disabled={!puedeGuardar}>
            Guardar configuración
          </Button>
        </div>
      )}
    </form>
  );
};
