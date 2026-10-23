import * as React from 'react';
import { useBlocker, useSearchParams } from 'react-router';

import { Alert } from '@/components/ui/alert';
import { Spinner } from '@/components/ui/spinner';
import { Tabs } from '@/components/ui/tabs';

import { useAniosEscolares } from '../../api/anios-escolares';
import { useTarifario } from '../../api/tarifarios';

import { DialogoCambiosSinConfirmar } from './dialogo-cambios-sin-confirmar';
import { anioPorDefecto } from './formato';
import { FormularioTarifario } from './formulario-tarifario';
import { SelectorAnio } from './selector-anio';

const PARAMETRO_ANIO = 'anioEscolarId';

// Figma > Personal Administrativo > "P19 · Tarifario y calendario" (HU-34). La pestaña "Catálogo de descuentos" es de
// HU-35 y todavía no está disponible.
//
// Lo escrito y no confirmado se pierde (no hay "Guardar"), así que se protege al salir de la pantalla (enlaces, atrás
// del navegador), al cerrar o recargar la pestaña y al cambiar de año en el selector.
export const ConfiguracionPensiones = () => {
  const [parametros, setParametros] = useSearchParams();
  const anios = useAniosEscolares();
  const [hayPendientes, setHayPendientes] = React.useState(false);
  const [anioPedido, setAnioPedido] = React.useState<string | null>(null);

  // Abre con el año de `?anioEscolarId=` y, si no viene, con el que se está preparando.
  const anio = anioPorDefecto(anios.data ?? [], parametros.get(PARAMETRO_ANIO));
  const tarifario = useTarifario({ anioEscolarId: anio?.id });

  const blocker = useBlocker(
    ({ currentLocation, nextLocation }) =>
      hayPendientes && currentLocation.pathname !== nextLocation.pathname,
  );

  React.useEffect(() => {
    if (!hayPendientes) return;
    const avisar = (e: BeforeUnloadEvent) => {
      e.preventDefault();
      e.returnValue = '';
    };
    window.addEventListener('beforeunload', avisar);
    return () => window.removeEventListener('beforeunload', avisar);
  }, [hayPendientes]);

  const cambiarAnio = (anioEscolarId: string) =>
    setParametros({ [PARAMETRO_ANIO]: anioEscolarId }, { replace: true });

  const elegirAnio = (anioEscolarId: string) => {
    if (anioEscolarId === anio?.id) return;
    if (hayPendientes) setAnioPedido(anioEscolarId);
    else cambiarAnio(anioEscolarId);
  };

  return (
    <div className="flex flex-col gap-6">
      <Tabs
        variant="pill"
        size="lg"
        aria-label="Configuración de pensiones"
        value="tarifario"
        onValueChange={() => undefined}
        items={[
          { value: 'tarifario', label: 'Tarifario y calendario' },
          {
            value: 'descuentos',
            label: 'Catálogo de descuentos',
            disabled: true,
            title: 'Disponible pronto',
          },
        ]}
      />

      {anios.isLoading ? (
        <div className="flex h-60 items-center justify-center">
          <Spinner />
        </div>
      ) : anios.isError ? (
        <Alert type="error" title="No pudimos cargar los años escolares.">
          Intenta de nuevo en unos minutos.
        </Alert>
      ) : !anio ? (
        <Alert
          type="info"
          title="Aún no hay años escolares para configurar su tarifario."
        />
      ) : (
        <>
          <SelectorAnio
            anios={anios.data ?? []}
            value={anio.id}
            onChange={elegirAnio}
            estado={tarifario.data?.estado ?? null}
          />
          {tarifario.isLoading ? (
            <div className="flex h-60 items-center justify-center">
              <Spinner />
            </div>
          ) : !tarifario.data ? (
            <Alert type="error" title="No pudimos cargar el tarifario del año.">
              Intenta de nuevo en unos minutos.
            </Alert>
          ) : (
            <FormularioTarifario
              // Al cambiar de año o recargarse el tarifario, el formulario vuelve a abrir con lo guardado.
              key={`${tarifario.data.anioEscolarId}-${tarifario.dataUpdatedAt}`}
              tarifario={tarifario.data}
              onPendientesChange={setHayPendientes}
            />
          )}
        </>
      )}

      <DialogoCambiosSinConfirmar
        abierto={blocker.state === 'blocked'}
        titulo="¿Salir sin confirmar?"
        textoSalir="Salir sin confirmar"
        onSeguir={() => blocker.reset?.()}
        onSalir={() => blocker.proceed?.()}
      />
      <DialogoCambiosSinConfirmar
        abierto={anioPedido !== null}
        titulo="¿Cambiar de año sin confirmar?"
        textoSalir="Cambiar de año"
        onSeguir={() => setAnioPedido(null)}
        onSalir={() => {
          if (anioPedido) cambiarAnio(anioPedido);
          setAnioPedido(null);
        }}
      />
    </div>
  );
};
