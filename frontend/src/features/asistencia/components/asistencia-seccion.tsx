import { CalendarDays, Check, Clock, FileText, Pencil } from 'lucide-react';
import * as React from 'react';

import { ContentLayout } from '@/components/layouts';
import { Alert } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Calendar } from '@/components/ui/date-picker';
import { useNotifications } from '@/components/ui/notifications';
import { ProgressBar } from '@/components/ui/progress-bar';
import { Spinner } from '@/components/ui/spinner';
import { PaginationBar } from '@/components/ui/table';
import { Tag } from '@/components/ui/tag';
import {
  AsistenciaDelDia,
  EstadoAsistencia,
  FilaAsistencia,
} from '@/types/api';

import {
  descargarSustento,
  useAsistenciaDelDia,
  useGuardarAsistencia,
  useJustificar,
  useSeccionesAsistencia,
} from '../api/asistencia';
import {
  Borrador,
  borradorDe,
  cambios as calcularCambios,
  contar,
  ETIQUETA_MOTIVO,
  fechaLarga,
  hoyEnLima,
  horaLima,
  nombreCompleto,
  tardanzaSinHora,
} from '../lib/asistencia';

import { DialogoJustificar } from './dialogo-justificar';
import {
  DialogoConfirmarCorreccion,
  DialogoGuardarAsistencia,
} from './dialogos-asistencia';
import { TablaAsistencia } from './tabla-asistencia';

const POR_PAGINA = 12;

type Modo = 'consulta' | 'registro' | 'correccion';

// Figma > Tutor > "HU65 · Marcar asistencia de la sección tutorada" (AS-01 a AS-06).
export const AsistenciaSeccion = () => {
  const secciones = useSeccionesAsistencia();
  const seccion = secciones.data?.[0];
  const [fecha, setFecha] = React.useState(hoyEnLima);
  const dia = useAsistenciaDelDia(seccion?.id, fecha);

  if (secciones.isLoading || (seccion && dia.isLoading)) {
    return (
      <ContentLayout
        title="Asistencia de la sección"
        breadcrumbs={['Asistencia']}
      >
        <div className="flex h-48 items-center justify-center">
          <Spinner size="lg" />
        </div>
      </ContentLayout>
    );
  }
  if (!seccion) {
    return (
      <ContentLayout
        title="Asistencia de la sección"
        breadcrumbs={['Asistencia']}
      >
        <Alert type="default" title="No tienes una sección a cargo.">
          La asistencia la registra el tutor o cotutor de una sección del año
          escolar activo.
        </Alert>
      </ContentLayout>
    );
  }
  if (!dia.data) return null;
  return (
    <PantallaAsistencia
      key={dia.data.fecha}
      dia={dia.data}
      onCambiarFecha={setFecha}
    />
  );
};

const PantallaAsistencia = ({
  dia,
  onCambiarFecha,
}: {
  dia: AsistenciaDelDia;
  onCambiarFecha: (fecha: string) => void;
}) => {
  const { addNotification } = useNotifications();
  const guardar = useGuardarAsistencia(dia.seccion.id, dia.fecha);
  const justificar = useJustificar(dia.seccion.id, dia.fecha);
  const [justificando, setJustificando] = React.useState<FilaAsistencia | null>(
    null,
  );
  const sinRegistro = dia.registro === null;
  const [modo, setModo] = React.useState<Modo>(
    sinRegistro && dia.editable ? 'registro' : 'consulta',
  );
  const [borrador, setBorrador] = React.useState<Borrador>(() =>
    borradorDe(dia.estudiantes),
  );
  const [mostrarErrores, setMostrarErrores] = React.useState(false);
  const [pagina, setPagina] = React.useState(1);
  const [dialogo, setDialogo] = React.useState<'guardar' | 'corregir' | null>(
    null,
  );

  // Si el día cambió en el servidor (otro tutor guardó o corrigió), se vuelve a lo guardado.
  React.useEffect(() => {
    setBorrador(borradorDe(dia.estudiantes));
    if (dia.registro !== null && modo === 'registro') setModo('consulta');
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [dia]);

  const editando = modo !== 'consulta';
  const conteo = editando ? contar(dia.estudiantes, borrador) : dia.resumen;
  const pendientes = calcularCambios(dia.estudiantes, borrador);
  // Cambiar de día descartaría lo marcado: se bloquea mientras haya algo sin guardar.
  const hayCambiosSinGuardar =
    modo === 'correccion'
      ? pendientes.length > 0
      : modo === 'registro' &&
        dia.estudiantes.some((f) => borrador[f.matriculaId]?.estado);
  const totalPaginas = Math.max(
    1,
    Math.ceil(dia.estudiantes.length / POR_PAGINA),
  );
  const desde = (pagina - 1) * POR_PAGINA;
  const filasPagina = dia.estudiantes.slice(desde, desde + POR_PAGINA);
  const contexto = `${dia.seccion.grado} ${dia.seccion.nombre} · ${fechaLarga(dia.fecha).toLowerCase()}`;

  const marcar = (matriculaId: string, estado: EstadoAsistencia) =>
    setBorrador((actual) => ({
      ...actual,
      [matriculaId]: { estado, hora: actual[matriculaId]?.hora ?? '' },
    }));
  const cambiarHora = (matriculaId: string, hora: string) =>
    setBorrador((actual) => ({
      ...actual,
      [matriculaId]: { estado: 'TARDANZA', hora },
    }));
  const marcarTodosPresentes = () =>
    setBorrador((actual) =>
      Object.fromEntries(
        dia.estudiantes.map((f) => [
          f.matriculaId,
          actual[f.matriculaId]?.estado
            ? actual[f.matriculaId]
            : { estado: 'PRESENTE' as const, hora: '' },
        ]),
      ),
    );

  const sinMarcar = dia.estudiantes.filter(
    (f) => !borrador[f.matriculaId]?.estado,
  );
  const sinHora = dia.estudiantes.filter((f) =>
    tardanzaSinHora(borrador[f.matriculaId]),
  );

  const intentarGuardar = () => {
    if (sinMarcar.length > 0 || sinHora.length > 0) {
      setMostrarErrores(true);
      return;
    }
    setDialogo(modo === 'registro' ? 'guardar' : 'corregir');
  };

  const confirmar = () => {
    guardar.mutate(
      {
        version: dia.registro?.version ?? null,
        asistencias: dia.estudiantes.map((f) => {
          const marca = borrador[f.matriculaId];
          return {
            matriculaId: f.matriculaId,
            estado: marca.estado as EstadoAsistencia,
            horaLlegada: marca.estado === 'TARDANZA' ? marca.hora : null,
          };
        }),
      },
      {
        onSuccess: (nuevo) => {
          addNotification(
            modo === 'registro'
              ? {
                  type: 'success',
                  title: 'Asistencia registrada',
                  message: `Se guardó la asistencia de ${nuevo.resumen.total} estudiantes.`,
                }
              : {
                  type: 'success',
                  title: 'Asistencia corregida',
                  message:
                    pendientes.length === 1
                      ? 'Se guardó 1 cambio.'
                      : `Se guardaron ${pendientes.length} cambios.`,
                },
          );
          setDialogo(null);
          setMostrarErrores(false);
          setModo('consulta');
        },
        onError: () => setDialogo(null),
      },
    );
  };

  const descartar = () => {
    setBorrador(borradorDe(dia.estudiantes));
    setMostrarErrores(false);
    setModo('consulta');
  };

  return (
    <ContentLayout
      title="Asistencia de la sección"
      breadcrumbs={['Asistencia']}
      subtitle={`${dia.seccion.grado} · Sección ${dia.seccion.nombre} · ${dia.estudiantes.length} estudiantes`}
    >
      <div className="flex flex-col gap-5">
        <Card className="flex flex-wrap items-center justify-between gap-3 p-4">
          <div className="flex flex-wrap items-center gap-3">
            <SelectorFecha
              fecha={dia.fecha}
              hoy={dia.hoy}
              onChange={onCambiarFecha}
              disabled={hayCambiosSinGuardar}
            />
            <EstadoDelDia dia={dia} modo={modo} />
          </div>
          <div className="flex flex-wrap gap-3">
            {modo === 'consulta' && dia.editable && dia.registro && (
              <Button
                variant="secondary"
                icon={<Pencil className="size-4" aria-hidden="true" />}
                onClick={() => setModo('correccion')}
              >
                Corregir asistencia
              </Button>
            )}
            <Button
              variant="secondary"
              icon={<FileText className="size-4" aria-hidden="true" />}
              disabled
              title="Disponible pronto"
            >
              Reporte mensual
            </Button>
          </div>
        </Card>

        <AvisoDelDia
          dia={dia}
          modo={modo}
          errores={mostrarErrores}
          sinMarcar={sinMarcar.map(nombreCompleto)}
          tardanzasSinHora={sinHora.length}
        />

        <Card className="grid grid-cols-2 divide-stroke-tertiary sm:grid-cols-5 sm:divide-x">
          <Dato
            color="bg-surface-success"
            etiqueta="Presentes"
            valor={conteo.presentes}
          />
          <Dato
            color="bg-surface-warning"
            etiqueta="Tardanzas"
            valor={conteo.tardanzas}
          />
          <Dato
            color="bg-surface-error"
            etiqueta="Faltas"
            valor={conteo.faltas}
          />
          <Dato
            color="bg-slate-400"
            etiqueta="Sin marcar"
            valor={conteo.sinMarcar}
            alerta={editando && conteo.sinMarcar > 0}
          />
          <div className="col-span-2 flex flex-col justify-center gap-2 px-5 py-4 sm:col-span-1">
            <span className="text-caption-1 text-content-tertiary">
              {conteo.total - conteo.sinMarcar} de {conteo.total} estudiantes
              marcados
            </span>
            <ProgressBar
              value={
                conteo.total
                  ? ((conteo.total - conteo.sinMarcar) / conteo.total) * 100
                  : 0
              }
              label="Estudiantes marcados"
              showLabel={false}
            />
          </div>
        </Card>

        <Card className="overflow-hidden p-0">
          {dia.estudiantes.length === 0 ? (
            <p className="py-10 text-center text-body-3 text-content-tertiary">
              La sección aún no tiene estudiantes matriculados.
            </p>
          ) : (
            <>
              <TablaAsistencia
                filas={filasPagina}
                desde={desde + 1}
                borrador={editando ? borrador : null}
                mostrarErrores={mostrarErrores}
                cambiadas={
                  modo === 'correccion'
                    ? new Set(pendientes.map((c) => c.fila.matriculaId))
                    : undefined
                }
                onMarcar={marcar}
                onHora={cambiarHora}
                onJustificar={
                  modo === 'consulta' && dia.registro
                    ? setJustificando
                    : undefined
                }
                onDescargarSustento={(fila) =>
                  descargarSustento(
                    dia.seccion.id,
                    dia.fecha,
                    fila.matriculaId,
                    fila.justificacion?.archivoNombre ?? 'sustento',
                  )
                }
              />
              <div className="flex justify-end px-4 py-3">
                <PaginationBar
                  page={pagina}
                  size={POR_PAGINA}
                  total={dia.estudiantes.length}
                  totalPages={totalPaginas}
                  onPageChange={setPagina}
                />
              </div>
              {editando && (
                <div className="flex flex-wrap items-center justify-between gap-3 border-t border-stroke-tertiary bg-surface-secondary px-5 py-4">
                  <span className="inline-flex items-center gap-2 text-caption-1 text-content-tertiary">
                    <Clock className="size-4" aria-hidden="true" />
                    {modo === 'registro'
                      ? 'Puedes corregir la asistencia solo durante el día de hoy.'
                      : pendientes.length === 1
                        ? '1 cambio sin guardar'
                        : `${pendientes.length} cambios sin guardar`}
                  </span>
                  <div className="flex flex-wrap gap-3">
                    {modo === 'registro' ? (
                      <Button
                        variant="tertiary"
                        icon={<Check className="size-4" aria-hidden="true" />}
                        onClick={marcarTodosPresentes}
                      >
                        Marcar todos como presentes
                      </Button>
                    ) : (
                      <Button variant="tertiary" onClick={descartar}>
                        Descartar cambios
                      </Button>
                    )}
                    <Button
                      variant="primary"
                      onClick={intentarGuardar}
                      disabled={
                        modo === 'registro'
                          ? conteo.sinMarcar === conteo.total
                          : pendientes.length === 0
                      }
                    >
                      {modo === 'registro'
                        ? 'Guardar asistencia'
                        : 'Guardar cambios'}
                    </Button>
                  </div>
                </div>
              )}
            </>
          )}
        </Card>
      </div>

      <DialogoGuardarAsistencia
        open={dialogo === 'guardar'}
        onOpenChange={(abierto) => !abierto && setDialogo(null)}
        contexto={contexto}
        resumen={conteo}
        onConfirmar={confirmar}
        cargando={guardar.isPending}
      />
      <DialogoJustificar
        fila={justificando}
        seccion={`${dia.seccion.grado} ${dia.seccion.nombre}`}
        fecha={dia.fecha}
        onOpenChange={(abierto) => !abierto && setJustificando(null)}
        cargando={justificar.isPending}
        onGuardar={(solicitud) =>
          justificar.mutate(solicitud, {
            onSuccess: () => {
              addNotification({
                type: 'success',
                title: 'Justificación registrada',
                message: `${justificando ? nombreCompleto(justificando) : ''}: ${ETIQUETA_MOTIVO[solicitud.motivo]}.`,
              });
              setJustificando(null);
            },
          })
        }
      />
      <DialogoConfirmarCorreccion
        open={dialogo === 'corregir'}
        onOpenChange={(abierto) => !abierto && setDialogo(null)}
        contexto={contexto}
        cambios={pendientes}
        onConfirmar={confirmar}
        cargando={guardar.isPending}
      />
    </ContentLayout>
  );
};

const EstadoDelDia = ({ dia, modo }: { dia: AsistenciaDelDia; modo: Modo }) => {
  const esHoy = dia.fecha === dia.hoy;
  if (!dia.registro) {
    return (
      <>
        <Tag size="sm" color={dia.editable ? 'warning' : 'neutral'}>
          {dia.editable ? 'Pendiente' : 'Sin registro'}
        </Tag>
        {esHoy && (
          <span className="text-body-4 text-content-tertiary">Hoy</span>
        )}
      </>
    );
  }
  const hora = horaLima(dia.registro.registradoEn);
  if (modo === 'correccion') {
    return (
      <>
        <Tag size="sm" color="info">
          En corrección
        </Tag>
        <span className="text-body-4 text-content-tertiary">
          Registrada hoy a las {hora}
        </span>
      </>
    );
  }
  return (
    <>
      <Tag size="sm" color="success">
        Registrada
      </Tag>
      <span className="text-body-4 text-content-tertiary">
        {esHoy ? 'Hoy' : fechaLarga(dia.fecha)} a las {hora}
        {dia.registro.registradoPor ? ` · ${dia.registro.registradoPor}` : ''}
        {dia.registro.corregidoEn
          ? ` · corregida a las ${horaLima(dia.registro.corregidoEn)}`
          : ''}
      </span>
    </>
  );
};

const AvisoDelDia = ({
  dia,
  modo,
  errores,
  sinMarcar,
  tardanzasSinHora,
}: {
  dia: AsistenciaDelDia;
  modo: Modo;
  errores: boolean;
  sinMarcar: string[];
  tardanzasSinHora: number;
}) => {
  if (!dia.editable && !dia.registro && !dia.motivoNoEscolar) {
    return (
      <Alert type="default" title="No se registró la asistencia de este día.">
        Solo se registra la asistencia del día de hoy.
      </Alert>
    );
  }
  if (dia.motivoNoEscolar) {
    return (
      <Alert type="default" title="No se registra asistencia en esta fecha.">
        {dia.motivoNoEscolar}
      </Alert>
    );
  }
  if (errores && sinMarcar.length > 0) {
    return (
      <Alert
        type="error"
        title={
          sinMarcar.length === 1
            ? 'Falta 1 estudiante por marcar.'
            : `Faltan ${sinMarcar.length} estudiantes por marcar.`
        }
      >
        Marca su asistencia para poder guardar: {sinMarcar.join(', ')}.
      </Alert>
    );
  }
  if (errores && tardanzasSinHora > 0) {
    return (
      <Alert type="error" title="Indica la hora de llegada de cada tardanza.">
        Escribe la hora en la columna «Hora de llegada».
      </Alert>
    );
  }
  if (modo === 'registro') {
    return (
      <Alert type="info" title="Aún no registras la asistencia de hoy.">
        Marca a cada estudiante como presente, tardanza o falta. Si casi todos
        asistieron, usa “Marcar todos como presentes” y luego cambia solo las
        excepciones.
      </Alert>
    );
  }
  if (modo === 'correccion') {
    return (
      <Alert
        type="warning"
        title="Estás corrigiendo la asistencia que ya guardaste hoy."
      >
        Los cambios quedarán en el historial con la hora. Mañana ya no podrás
        modificarla.
      </Alert>
    );
  }
  return null;
};

const Dato = ({
  color,
  etiqueta,
  valor,
  alerta,
}: {
  color: string;
  etiqueta: string;
  valor: number;
  alerta?: boolean;
}) => (
  <div className="flex flex-col gap-1 px-5 py-4">
    <span className="inline-flex items-center gap-2 text-caption-1 text-content-tertiary">
      <span className={`size-2 rounded-full ${color}`} aria-hidden="true" />
      {etiqueta}
    </span>
    <span
      className={
        alerta
          ? 'text-h6 font-semibold text-content-error'
          : 'text-h6 font-semibold text-content-primary'
      }
    >
      {valor}
    </span>
  </div>
);

/**
 * Botón de la fecha (AS-01) con el calendario desplegable: elige un día de hoy hacia atrás para consultarlo o
 * justificar sus faltas y tardanzas (HU-67).
 */
const SelectorFecha = ({
  fecha,
  hoy,
  onChange,
  disabled,
}: {
  fecha: string;
  hoy: string;
  onChange: (fecha: string) => void;
  disabled: boolean;
}) => {
  const [abierto, setAbierto] = React.useState(false);
  const contenedor = React.useRef<HTMLDivElement>(null);

  React.useEffect(() => {
    if (!abierto) return;
    const alClickFuera = (evento: MouseEvent) => {
      if (!contenedor.current?.contains(evento.target as Node))
        setAbierto(false);
    };
    const alTeclear = (evento: KeyboardEvent) => {
      if (evento.key === 'Escape') setAbierto(false);
    };
    document.addEventListener('mousedown', alClickFuera);
    document.addEventListener('keydown', alTeclear);
    return () => {
      document.removeEventListener('mousedown', alClickFuera);
      document.removeEventListener('keydown', alTeclear);
    };
  }, [abierto]);

  return (
    <div ref={contenedor} className="relative">
      <Button
        variant="primary"
        icon={<CalendarDays className="size-4" aria-hidden="true" />}
        aria-haspopup="dialog"
        aria-expanded={abierto}
        aria-label={`Fecha: ${fechaLarga(fecha)}. Elegir otro día`}
        title={
          disabled
            ? 'Guarda o descarta los cambios para ver otro día'
            : undefined
        }
        disabled={disabled}
        onClick={() => setAbierto(!abierto)}
      >
        {fechaLarga(fecha)}
      </Button>
      {abierto && (
        <div className="absolute left-0 top-full z-20 mt-2">
          <Calendar
            value={fecha}
            max={hoy}
            onChange={(nueva) => {
              setAbierto(false);
              if (nueva !== fecha) onChange(nueva);
            }}
          />
        </div>
      )}
    </div>
  );
};
