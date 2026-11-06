import * as React from 'react';
import { useBlocker } from 'react-router';

import { Alert } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { useNotifications } from '@/components/ui/notifications';
import { Spinner } from '@/components/ui/spinner';

import { useGuardarHorario } from '../api/guardar-horario';
import { useHorarioSeccion, useOpcionesHorario } from '../api/horario-seccion';
import { useAniosParaHorarios, useGradosParaHorarios } from '../api/selectores';
import {
  aSolicitudHorario,
  conCelda,
  hayCambios as difiere,
  sinCelda,
} from '../lib/borrador';
import { esConflictoDeVersion, mensajeDeError } from '../lib/formato';
import { CeldaDelHorario, DiaCoordinador } from '../types';

import { BarraAcciones } from './barra-acciones';
import { DatosDelHorario } from './datos-del-horario';
import { DialogoAsignarBloque } from './dialogo-asignar-bloque';
import { DialogoCambiosSinGuardar } from './dialogo-cambios-sin-guardar';
import { MatrizCoordinador } from './matriz-coordinador';

type Cambio =
  | { campo: 'anio'; valor: string }
  | { campo: 'grado'; valor: string }
  | { campo: 'seccion'; valor: string };

type ErrorDeGuardado = { mensaje: string; conflictoDeVersion: boolean };

const TEXTO_SALIR: Record<Cambio['campo'], string> = {
  anio: 'Cambiar de año',
  grado: 'Cambiar de grado',
  seccion: 'Cambiar de sección',
};

// Figma > Coordinador > "HU-45: Asignación de docente y curso a un bloque de horario" (HU-45).
// Modo lectura por defecto; «Editar horario» abre un borrador en el navegador que solo se envía al backend (un PUT con
// la matriz completa y la versión del GET) al pulsar «Guardar cambios».
export const AsignacionHorarios = () => {
  const { addNotification } = useNotifications();

  // ---------- Selectores en cascada ----------
  const aniosQuery = useAniosParaHorarios();
  const anios = React.useMemo(() => aniosQuery.data ?? [], [aniosQuery.data]);
  // El año no viene elegido: el grado y la sección se habilitan cuando el usuario lo elige.
  const [anioId, setAnioId] = React.useState('');
  const [gradoCodigo, setGradoCodigo] = React.useState('');
  const [seccionId, setSeccionId] = React.useState('');
  const gradosQuery = useGradosParaHorarios(anioId || undefined);

  // ---------- Horario y borrador ----------
  const horarioQuery = useHorarioSeccion(seccionId || undefined);
  const horario = seccionId ? horarioQuery.data : undefined;
  const [editando, setEditando] = React.useState(false);
  const [borrador, setBorrador] = React.useState<CeldaDelHorario[]>([]);
  const [celdaAbierta, setCeldaAbierta] = React.useState<{
    dia: DiaCoordinador;
    numeroBloque: number;
  } | null>(null);
  const [errorDeGuardado, setErrorDeGuardado] =
    React.useState<ErrorDeGuardado | null>(null);
  const [cambioPendiente, setCambioPendiente] = React.useState<Cambio | null>(
    null,
  );
  const opcionesQuery = useOpcionesHorario(seccionId || undefined, editando);

  const hayCambios = editando && !!horario && difiere(horario.celdas, borrador);

  const salirDeLaEdicion = () => {
    setEditando(false);
    setBorrador([]);
    setCeldaAbierta(null);
    setErrorDeGuardado(null);
  };

  const guardar = useGuardarHorario(seccionId, () => {
    salirDeLaEdicion();
    addNotification({
      type: 'success',
      title: 'Horario guardado',
      message: 'Los cambios del horario se guardaron.',
    });
  });

  // ---------- Avisos al salir con cambios sin guardar ----------
  const blocker = useBlocker(
    ({ currentLocation, nextLocation }) =>
      hayCambios && currentLocation.pathname !== nextLocation.pathname,
  );
  React.useEffect(() => {
    if (!hayCambios) return;
    const avisar = (e: BeforeUnloadEvent) => {
      e.preventDefault();
      e.returnValue = '';
    };
    window.addEventListener('beforeunload', avisar);
    return () => window.removeEventListener('beforeunload', avisar);
  }, [hayCambios]);

  // ---------- Cambios de año, grado y sección ----------
  const aplicar = ({ campo, valor }: Cambio) => {
    salirDeLaEdicion();
    if (campo === 'anio') {
      setAnioId(valor);
      setGradoCodigo('');
      setSeccionId('');
    } else if (campo === 'grado') {
      setGradoCodigo(valor);
      setSeccionId('');
    } else {
      setSeccionId(valor);
    }
  };
  const elegir = (cambio: Cambio) => {
    const actual =
      cambio.campo === 'anio'
        ? anioId
        : cambio.campo === 'grado'
          ? gradoCodigo
          : seccionId;
    if (cambio.valor === actual) return;
    if (hayCambios) setCambioPendiente(cambio);
    else aplicar(cambio);
  };

  // ---------- Edición ----------
  const editar = () => {
    if (!horario) return;
    setBorrador(horario.celdas.map((c) => ({ ...c })));
    setErrorDeGuardado(null);
    setEditando(true);
  };

  const enviar = () => {
    if (!horario) return;
    setErrorDeGuardado(null);
    guardar.mutate(aSolicitudHorario(borrador, horario.version), {
      onError: (error) =>
        setErrorDeGuardado({
          mensaje: mensajeDeError(error),
          conflictoDeVersion: esConflictoDeVersion(error),
        }),
    });
  };

  // 409 de versión: se vuelve a leer el horario y el borrador pasa a ser lo que hay guardado; se sigue en edición.
  const recargar = async () => {
    const [{ data: nuevo }] = await Promise.all([
      horarioQuery.refetch(),
      opcionesQuery.refetch(),
    ]);
    if (nuevo) setBorrador(nuevo.celdas.map((c) => ({ ...c })));
    setErrorDeGuardado(null);
  };

  // ---------- Dibujo ----------
  const bloqueAbierto = celdaAbierta
    ? horario?.jornada.bloques.find(
        (b) => b.numero === celdaAbierta.numeroBloque,
      )
    : undefined;

  const estadoVacio = (() => {
    if (!seccionId) {
      return 'Elige un año, un grado y una sección para ver o armar su horario.';
    }
    if (horarioQuery.isError) return mensajeDeError(horarioQuery.error);
    return null;
  })();

  const crucesGuardados = (horario?.celdas ?? []).flatMap((c) => c.cruces);

  if (aniosQuery.isLoading) {
    return (
      <div className="flex h-48 w-full items-center justify-center">
        <Spinner size="lg" />
      </div>
    );
  }

  return (
    <>
      <DatosDelHorario
        anios={anios}
        grados={gradosQuery.data ?? []}
        anioId={anioId}
        gradoCodigo={gradoCodigo}
        seccionId={seccionId}
        tutor={horario ? (horario.seccion.tutor?.nombre ?? null) : undefined}
        bloqueado={guardar.isPending}
        onAnio={(valor) => elegir({ campo: 'anio', valor })}
        onGrado={(valor) => elegir({ campo: 'grado', valor })}
        onSeccion={(valor) => elegir({ campo: 'seccion', valor })}
      />

      <Card>
        <CardHeader>
          <CardTitle size="body-1">Matriz horaria</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4 p-6">
          {errorDeGuardado && (
            <Alert
              type="error"
              title={errorDeGuardado.mensaje}
              action={
                errorDeGuardado.conflictoDeVersion ? (
                  <Button size="sm" variant="primary-light" onClick={recargar}>
                    Recargar
                  </Button>
                ) : undefined
              }
            />
          )}
          {!editando && crucesGuardados.length > 0 && (
            <Alert type="warning" title="Este horario tiene cruces">
              {crucesGuardados.map((c) => (
                <span
                  key={`${c.dia}-${c.numeroBloque}-${c.tipo}`}
                  className="block"
                >
                  {c.mensaje}
                </span>
              ))}
            </Alert>
          )}
          {horarioQuery.isLoading && seccionId ? (
            <div className="flex h-48 items-center justify-center">
              <Spinner size="lg" />
            </div>
          ) : estadoVacio ? (
            <p className="py-10 text-center text-body-3 text-content-quartiary">
              {estadoVacio}
            </p>
          ) : (
            horario && (
              <MatrizCoordinador
                jornada={horario.jornada}
                celdas={editando ? borrador : horario.celdas}
                editando={editando}
                onElegirCelda={(dia, numeroBloque) =>
                  setCeldaAbierta({ dia, numeroBloque })
                }
              />
            )
          )}
        </CardContent>
      </Card>

      {horario && (
        <BarraAcciones
          editando={editando}
          editable={horario.editable}
          hayCambios={hayCambios}
          guardando={guardar.isPending}
          onEditar={editar}
          onCancelar={salirDeLaEdicion}
          onGuardar={enviar}
        />
      )}

      {editando && horario && celdaAbierta && bloqueAbierto && (
        <DialogoAsignarBloque
          key={`${celdaAbierta.dia}-${celdaAbierta.numeroBloque}`}
          seccionId={seccionId}
          dia={celdaAbierta.dia}
          bloque={bloqueAbierto}
          opciones={opcionesQuery.data}
          guardadas={horario.celdas}
          borrador={borrador}
          onAsignar={(celda) => {
            setBorrador((actual) => conCelda(actual, celda));
            setCeldaAbierta(null);
          }}
          onQuitar={() => {
            setBorrador((actual) =>
              sinCelda(actual, celdaAbierta.dia, celdaAbierta.numeroBloque),
            );
            setCeldaAbierta(null);
          }}
          onCerrar={() => setCeldaAbierta(null)}
        />
      )}

      <DialogoCambiosSinGuardar
        abierto={cambioPendiente !== null}
        titulo="¿Descartar los cambios del horario?"
        textoSalir={cambioPendiente ? TEXTO_SALIR[cambioPendiente.campo] : ''}
        onSeguir={() => setCambioPendiente(null)}
        onSalir={() => {
          if (cambioPendiente) aplicar(cambioPendiente);
          setCambioPendiente(null);
        }}
      />
      <DialogoCambiosSinGuardar
        abierto={blocker.state === 'blocked'}
        titulo="¿Salir sin guardar?"
        textoSalir="Salir sin guardar"
        onSeguir={() => blocker.reset?.()}
        onSalir={() => blocker.proceed?.()}
      />
    </>
  );
};
