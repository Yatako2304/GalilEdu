import { Alert } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogTitle,
} from '@/components/ui/dialog';
import { Spinner } from '@/components/ui/spinner';
import {
  EstadoVisiblePeriodoMatricula,
  PeriodoMatriculaVigente,
} from '@/types/api';

import { usePeriodoMatricula } from '../../api/periodos-matricula';

import { rangoDeMatricula, siONo, soles, textoExtemporanea } from './formato';

type DialogoResumenConfiguracionProps = {
  abierto: boolean;
  onCerrar: () => void;
  periodo: Pick<
    PeriodoMatriculaVigente,
    | 'id'
    | 'nombre'
    | 'estado'
    | 'vacantesTotales'
    | 'bloquearPorMorosidad'
    | 'permitirExtemporanea'
  >;
};

const SUBTITULO: Record<EstadoVisiblePeriodoMatricula, string> = {
  ABIERTO: 'Periodo habilitado: la configuración es de solo lectura.',
  PROGRAMADO: 'Periodo programado: aún puedes editar su configuración.',
  CERRADO: 'Periodo cerrado: la configuración es de solo lectura.',
};

const FilaResumen = ({
  etiqueta,
  valor,
}: {
  etiqueta: string;
  valor: string;
}) => (
  <div className="flex items-center justify-between gap-4 py-1.5">
    <dt className="text-body-3 text-content-secondary">{etiqueta}</dt>
    <dd className="text-right text-label-2 font-medium text-content-primary">
      {valor}
    </dd>
  </div>
);

// Figma > pantalla 13 > "Modal - Configuración · Matrícula 2027": resumen del periodo con sus reglas. Las reglas
// (morosidad y matrícula extemporánea) no tienen campo en el formulario: solo se muestran aquí.
export const DialogoResumenConfiguracion = ({
  abierto,
  onCerrar,
  periodo,
}: DialogoResumenConfiguracionProps) => {
  const detalle = usePeriodoMatricula(abierto ? periodo.id : undefined);
  const datos = detalle.data;

  return (
    <Dialog open={abierto} onOpenChange={(open) => !open && onCerrar()}>
      <DialogContent className="flex max-h-[92vh] max-w-[560px] flex-col gap-0 overflow-hidden rounded-xl border-stroke-tertiary bg-white p-0">
        <div className="space-y-1 border-b border-stroke-tertiary px-6 py-5 pr-12">
          <DialogTitle className="text-h6 font-semibold text-content-primary">
            Configuración · {periodo.nombre}
          </DialogTitle>
          <DialogDescription className="text-body-4 text-content-secondary">
            {SUBTITULO[periodo.estado]}
          </DialogDescription>
        </div>

        <div className="space-y-4 overflow-y-auto p-6">
          {detalle.isLoading ? (
            <div className="flex h-40 items-center justify-center">
              <Spinner />
            </div>
          ) : !datos ? (
            <Alert type="error" title="No pudimos cargar la configuración.">
              Intenta de nuevo en unos minutos.
            </Alert>
          ) : (
            <>
              <dl className="divide-y divide-transparent rounded-xl bg-surface-secondary px-5 py-3">
                <FilaResumen
                  etiqueta="Fechas de matrícula regular"
                  valor={
                    datos.fechaInicio && datos.fechaCierre
                      ? rangoDeMatricula(datos.fechaInicio, datos.fechaCierre)
                      : '—'
                  }
                />
                <FilaResumen
                  etiqueta="Vacantes totales"
                  valor={String(periodo.vacantesTotales)}
                />
                <FilaResumen
                  etiqueta="Secciones"
                  valor={`${datos.cantidadSecciones} (${datos.resumenSecciones})`}
                />
                <FilaResumen
                  etiqueta="Monto de matrícula"
                  valor={
                    datos.montoMatricula === null
                      ? 'Sin tarifario confirmado'
                      : soles(datos.montoMatricula)
                  }
                />
                <FilaResumen
                  etiqueta="Documentos requeridos"
                  valor={String(datos.documentos.length)}
                />
                <FilaResumen
                  etiqueta="Bloquear por morosidad"
                  valor={siONo(periodo.bloquearPorMorosidad)}
                />
                <FilaResumen
                  etiqueta="Matrícula extemporánea"
                  valor={textoExtemporanea(periodo.permitirExtemporanea)}
                />
              </dl>
              {periodo.estado === 'ABIERTO' && (
                <Alert
                  type="info"
                  title="Una vez habilitado, el periodo no se puede editar; solo deshabilitar."
                />
              )}
            </>
          )}
        </div>

        <div className="flex justify-end border-t border-stroke-tertiary px-6 py-4">
          <Button type="button" variant="tertiary" onClick={onCerrar}>
            Cerrar
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
};
