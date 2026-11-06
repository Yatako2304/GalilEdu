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
// año y la sección con el grado; ninguno trae un valor puesto (sin placeholders: la etiqueta va dentro del campo y sube
// al elegir). El tutor es de solo lectura y sale de la sección (en Figma se dibuja como un select).
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
  // Solo los grados que tienen secciones para elegir: el backend devuelve los 11 (los de primaria, vacíos).
  const gradosConSecciones = grados.filter(
    (g) => g.cantidadSeccionesActivas > 0,
  );
  const grado = gradosConSecciones.find((g) => g.codigo === gradoCodigo);
  const secciones = (grado?.secciones ?? []).filter(
    (s) => s.estado === 'ACTIVA',
  );

  return (
    <Card>
      <CardHeader>
        <CardTitle size="body-1">Datos del Horario</CardTitle>
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
          disabled={bloqueado || gradosConSecciones.length === 0}
          value={gradoCodigo}
          onChange={(e) => onGrado(e.target.value)}
          options={gradosConSecciones.map((g) => ({
            label: g.nombre,
            value: g.codigo,
          }))}
        />
        <Select
          label="Sección"
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
