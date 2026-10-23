import { CirclePlus } from 'lucide-react';

import { Tag } from '@/components/ui/tag';
import { EstructuraHorariaResumen } from '@/types/api';
import { cn } from '@/utils/cn';

import { BotonSeccion } from '../secciones/boton-seccion';

import {
  MOTIVO_ANIO_NO_MODIFICABLE,
  textoCantidadDias,
  textoJornada,
  textoRegistradas,
} from './formato';

type ListaEstructurasProps = {
  estructuras: EstructuraHorariaResumen[];
  seleccionadaId: string | undefined;
  onElegir: (id: string) => void;
  /** Con el año ACTIVO o CERRADO no se puede crear: "Nueva" queda bloqueado con el motivo. */
  anioModificable: boolean;
  onNueva?: () => void;
};

// Figma > CI-12 > "Lista de estructuras": "Estructuras del año", "2 registradas", el botón "Nueva" y una tarjeta por
// estructura (la elegida va resaltada). La marca "Principal" no está en Figma: se agregó para ver cuál se usa en cada nivel.
export const ListaEstructuras = ({
  estructuras,
  seleccionadaId,
  onElegir,
  anioModificable,
  onNueva,
}: ListaEstructurasProps) => (
  <section aria-label="Estructuras del año" className="flex flex-col gap-2.5">
    <div className="flex items-center justify-between gap-3">
      <div>
        <h2 className="text-label-1 font-semibold text-content-primary">
          Estructuras del año
        </h2>
        <p className="text-body-4 text-content-tertiary">
          {textoRegistradas(estructuras.length)}
        </p>
      </div>
      <BotonSeccion
        bloqueado={!anioModificable}
        motivo={MOTIVO_ANIO_NO_MODIFICABLE}
        size="sm"
        icon={<CirclePlus className="size-4" aria-hidden="true" />}
        onClick={onNueva}
      >
        Nueva
      </BotonSeccion>
    </div>

    {estructuras.length === 0 ? (
      <p className="text-body-4 text-content-tertiary">
        Este año aún no tiene estructuras horarias.
      </p>
    ) : (
      <ul className="flex flex-col gap-2.5">
        {estructuras.map((e) => {
          const seleccionada = e.id === seleccionadaId;
          return (
            <li key={e.id}>
              <button
                type="button"
                aria-pressed={seleccionada}
                onClick={() => onElegir(e.id)}
                className={cn(
                  'flex w-full flex-col gap-[3px] rounded-[10px] border px-3.5 py-3 text-left outline-none transition-colors focus-visible:ring-2 focus-visible:ring-stroke-focus',
                  seleccionada
                    ? 'border-stroke-accent-secondary bg-surface-accent-light'
                    : 'border-stroke-tertiary bg-white hover:bg-surface-secondary',
                )}
              >
                <span className="flex items-start justify-between gap-2">
                  <span className="flex items-center gap-2">
                    <span className="text-label-2 font-semibold text-content-primary">
                      {e.codigo}
                    </span>
                    {e.esPrincipal && (
                      <Tag color="primary" size="sm">
                        Principal
                      </Tag>
                    )}
                  </span>
                  <span className="text-body-4 text-content-tertiary">
                    {textoCantidadDias(e.diasLectivos.length)}
                  </span>
                </span>
                {e.descripcion && (
                  <span className="text-body-4 text-content-tertiary">
                    {e.descripcion}
                  </span>
                )}
                <span className="text-body-4 text-content-tertiary">
                  {textoJornada(e)}
                </span>
              </button>
            </li>
          );
        })}
      </ul>
    )}
  </section>
);
