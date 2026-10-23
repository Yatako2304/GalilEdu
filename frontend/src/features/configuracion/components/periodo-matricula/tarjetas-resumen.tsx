import { Banknote, CalendarDays, FileText, Users } from 'lucide-react';

import { StatCard } from '@/components/ui/stat-card';

import { rangoCorto, soles } from './formato';

type TarjetasResumenProps = {
  fechaInicio: string;
  fechaCierre: string;
  /** null mientras no hay secciones que sumar (aún no se elige el año). */
  vacantesTotales: number | null;
  /** null si el año no tiene tarifario confirmado o aún no se elige. */
  monto: number | null;
  documentos: number;
};

// Figma > pantalla 13 > formulario > los cuatro indicadores de arriba. "-" mientras no hay dato.
export const TarjetasResumen = ({
  fechaInicio,
  fechaCierre,
  vacantesTotales,
  monto,
  documentos,
}: TarjetasResumenProps) => (
  <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
    <StatCard
      icon={<CalendarDays />}
      iconColor="accent"
      value={
        fechaInicio && fechaCierre ? rangoCorto(fechaInicio, fechaCierre) : '-'
      }
      label="Periodo de matrícula"
    />
    <StatCard
      icon={<Users />}
      iconColor="turquoise"
      value={vacantesTotales ?? '-'}
      label="Vacantes totales"
    />
    <StatCard
      icon={<Banknote />}
      iconColor="warning"
      value={monto === null ? '-' : soles(monto)}
      label="Monto de matrícula"
    />
    <StatCard
      icon={<FileText />}
      iconColor="success"
      value={documentos > 0 ? documentos : '-'}
      label="Documentos requeridos"
    />
  </div>
);
