import { Consolidacion, Escala, ReglaNota } from '@/types/api';

export const ESCALAS_EVALUACION: { label: string; value: Escala }[] = [
  { label: 'Vigesimal (0–20)', value: 'VIGESIMAL' },
  { label: 'Cualitativa (AD–C)', value: 'CUALITATIVA' },
];

export const REGLAS_NOTA: Record<
  Escala,
  { label: string; value: ReglaNota }[]
> = {
  VIGESIMAL: [
    { label: 'Promedio simple', value: 'PROMEDIO_SIMPLE' },
    { label: 'Promedio ponderado', value: 'PROMEDIO_PONDERADO' },
  ],
  CUALITATIVA: [
    { label: 'Nivel más frecuente', value: 'NIVEL_MAS_FRECUENTE' },
    { label: 'Último nivel', value: 'ULTIMO_NIVEL' },
  ],
};

export const CONSOLIDACIONES_PERIODOS: {
  label: string;
  value: Consolidacion;
}[] = [
  { label: 'Promedio de periodos', value: 'PROMEDIO_PERIODOS' },
  { label: 'Último periodo', value: 'ULTIMO_PERIODO' },
];
