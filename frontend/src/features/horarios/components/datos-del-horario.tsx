import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input, Select } from '@/components/ui/form';

import { AnioParaHorarios, GradoParaHorarios } from '../types';

type DatosDelHorarioProps = {
  anios: AnioParaHorarios[];
  grados: GradoParaHorarios[];
  anioId: string;
  gradoCodigo: string;
  seccionId: string;
  /** null = la sección no tiene tutor; undefined = aún no hay sección elegida. */
  tutor: string | null | undefined;
  /** Deshabilita los selectores mientras se guarda. */
  bloqueado: boolean;
  onAnio: (anioId: string) => void;
  onGrado: (gradoCodigo: string) => void;
  onSeccion: (seccionId: string) => void;
};

// Figma > HU-45 > "Datos del Horario": año académico, grado, sección y tutor. En cascada: el grado se habilita con el
// año y la sección con el grado. El tutor es de solo lectura y sale de la sección (en Figma se dibuja como un select).
export const DatosDelHorario = ({
  anios,
  grados,
  anioId,
  gradoCodigo,
  seccionId,
  tutor,
  bloqueado,
  onAnio,
  onGrado,
  onSeccion,
}: DatosDelHorarioProps) => {
  const grado = grados.find((g) => g.codigo === gradoCodigo);
  const secciones = (grado?.secciones ?? []).filter(
    (s) => s.estado === 'ACTIVA',
  );

  return (
    <Card>
      <CardHeader>
        <CardTitle>Datos del Horario</CardTitle>
      </CardHeader>
      <CardContent className="grid grid-cols-1 gap-6 pb-6 sm:grid-cols-2 lg:grid-cols-4">
        <Select
          label="Año académico"
          disabled={bloqueado}
          value={anioId}
          onChange={(e) => onAnio(e.target.value)}
          options={anios.map((a) => ({ label: String(a.anio), value: a.id }))}
        />
        <Select
          label="Grado"
          placeholder="Elige un grado"
          disabled={bloqueado || !anioId}
          value={gradoCodigo}
          onChange={(e) => onGrado(e.target.value)}
          options={grados.map((g) => ({
            label: g.nombre,
            value: g.codigo,
            // Un grado sin secciones activas no tiene horario que armar.
            disabled: g.cantidadSeccionesActivas === 0,
          }))}
        />
        <Select
          label="Sección"
          placeholder="Elige una sección"
          disabled={bloqueado || !gradoCodigo}
          value={seccionId}
          onChange={(e) => onSeccion(e.target.value)}
          options={secciones.map((s) => ({ label: s.nombre, value: s.id }))}
        />
        <Input
          label="Tutor"
          readOnly
          disabled
          value={tutor === undefined ? '' : (tutor ?? 'Sin asignar')}
        />
      </CardContent>
    </Card>
  );
};
