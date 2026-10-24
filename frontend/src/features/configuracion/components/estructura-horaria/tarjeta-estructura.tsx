import { Pencil, Trash2 } from 'lucide-react';

import { Alert } from '@/components/ui/alert';
import { Card } from '@/components/ui/card';
import { Spinner } from '@/components/ui/spinner';
import { Tag } from '@/components/ui/tag';
import { DetalleEstructuraHoraria } from '@/types/api';

import { useDetalleEstructura } from '../../api/estructuras-horarias';
import { BotonSeccion } from '../secciones/boton-seccion';

import {
  MOTIVO_ANIO_NO_MODIFICABLE,
  textoBloquesPorDia,
  textoDias,
  textoRecesos,
} from './formato';
import { GrillaJornada } from './grilla-jornada';

type TarjetaEstructuraProps = {
  estructuraId: string;
  onEditar?: (estructura: DetalleEstructuraHoraria) => void;
  onEliminar?: (estructura: DetalleEstructuraHoraria) => void;
};

// Figma > CI-12 > "Card - EH-PRI-26": código y descripción, los seis datos y la "Jornada semanal".
export const TarjetaEstructura = ({
  estructuraId,
  onEditar,
  onEliminar,
}: TarjetaEstructuraProps) => {
  const detalle = useDetalleEstructura(estructuraId);
  const e = detalle.data;

  if (!e) {
    return detalle.isError ? (
      <Alert type="error" title="No pudimos cargar la estructura horaria.">
        Vuelve a intentarlo en unos minutos.
      </Alert>
    ) : (
      <Card className="flex h-60 items-center justify-center">
        <Spinner />
      </Card>
    );
  }

  const datos = [
    { titulo: 'Días lectivos', valor: textoDias(e.diasLectivos) },
    { titulo: 'Hora de entrada', valor: e.horaEntrada },
    { titulo: 'Hora de salida', valor: e.horaSalida },
    { titulo: 'Bloques por día', valor: textoBloquesPorDia(e) },
    { titulo: 'Recesos', valor: textoRecesos(e.recesos) },
    { titulo: 'Bloques / semana', valor: String(e.bloquesPorSemana) },
  ];

  return (
    <Card className="flex flex-col gap-4 p-6">
      <div className="flex items-start justify-between gap-4">
        <div className="space-y-1">
          <h2 className="text-label-1 font-semibold text-content-primary">
            {e.codigo}
          </h2>
          {e.descripcion && (
            <p className="text-body-4 text-content-tertiary">{e.descripcion}</p>
          )}
        </div>
        <div className="flex items-center gap-2">
          {e.esPrincipal && <Tag color="primary">Principal</Tag>}
          {/* Editar y Eliminar no están en Figma. Con el año no modificable quedan bloqueados, con el motivo. */}
          <BotonSeccion
            bloqueado={!e.anioModificable}
            motivo={MOTIVO_ANIO_NO_MODIFICABLE}
            size="sm"
            variant="primary-light"
            icon={<Pencil className="size-3.5" aria-hidden="true" />}
            onClick={() => onEditar?.(e)}
          >
            Editar
          </BotonSeccion>
          <BotonSeccion
            bloqueado={!e.anioModificable}
            motivo={MOTIVO_ANIO_NO_MODIFICABLE}
            size="sm"
            variant="error-light"
            icon={<Trash2 className="size-3.5" aria-hidden="true" />}
            onClick={() => onEliminar?.(e)}
          >
            Eliminar
          </BotonSeccion>
        </div>
      </div>

      <dl className="grid grid-cols-2 gap-3 sm:grid-cols-3 2xl:grid-cols-6">
        {datos.map(({ titulo, valor }) => (
          <div
            key={titulo}
            className="space-y-0.5 rounded-[10px] bg-surface-secondary px-3.5 py-2.5"
          >
            <dt className="text-body-4 text-content-tertiary">{titulo}</dt>
            <dd className="text-label-1 font-semibold text-content-primary">
              {valor}
            </dd>
          </div>
        ))}
      </dl>

      <h3 className="text-label-2 font-semibold text-content-primary">
        Jornada semanal
      </h3>
      <div className="overflow-hidden rounded-[10px] border border-stroke-tertiary">
        <GrillaJornada diasLectivos={e.diasLectivos} jornada={e.jornada} />
      </div>
      <p className="text-body-4 text-content-tertiary">
        Los bloques se llenarán con cursos y docentes al generar los horarios de
        cada sección.
      </p>
    </Card>
  );
};
