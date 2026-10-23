import { diaYMesCorto, diasEntre } from './formato';

type BarraDuracionProps = { fechaInicio: string; fechaCierre: string };

// Figma > pantalla 13 > formulario > barra "01-dic ▬▬ 15-feb 76 días" bajo las fechas ("No definido" sin ellas).
// Los días son los que hay entre las dos fechas elegidas, como `diasDelPeriodo` del backend.
export const BarraDuracion = ({
  fechaInicio,
  fechaCierre,
}: BarraDuracionProps) => {
  const definida = !!fechaInicio && !!fechaCierre && fechaCierre > fechaInicio;
  const dias = definida ? diasEntre(fechaInicio, fechaCierre) : 0;
  const texto = definida
    ? `${dias} ${dias === 1 ? 'día' : 'días'}`
    : 'No definido';

  return (
    <div
      role="group"
      aria-label={`Duración del periodo: ${texto}`}
      className="flex items-center gap-3 rounded-lg bg-surface-secondary px-3 py-2.5 text-body-3 text-content-tertiary"
    >
      <span>{definida ? diaYMesCorto(fechaInicio) : '-'}</span>
      <span
        aria-hidden="true"
        className="h-2 flex-1 rounded-full bg-gradient-to-r from-deep-blue-900 via-vibrant-blue-500 to-turquoise-300"
      />
      <span>{definida ? diaYMesCorto(fechaCierre) : '-'}</span>
      <span className="text-label-2 font-semibold text-content-accent-secondary">
        {texto}
      </span>
    </div>
  );
};
