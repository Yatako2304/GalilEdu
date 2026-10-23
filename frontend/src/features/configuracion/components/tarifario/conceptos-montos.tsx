import { UseFormReturn } from 'react-hook-form';

import { Card } from '@/components/ui/card';
import { Input } from '@/components/ui/form';
import { Table } from '@/components/ui/table';
import { TarifarioVista } from '@/types/api';

import { ValoresTarifario } from './esquemas';
import { fechaDeDia, resumenDeCuotas, vencimientoDePension } from './formato';

type CampoMonto = 'montoCuotaIngreso' | 'montoMatricula' | 'montoPension';

type Fila = {
  id: string;
  concepto: string;
  detalle: string;
  aplicaA: string;
  vencimiento: string;
  campo: CampoMonto;
  /** Los montos no tienen etiqueta visible en Figma: este es su nombre accesible, con la moneda. */
  nombreAccesible: string;
  form: UseFormReturn<ValoresTarifario>;
  deshabilitado: boolean;
};

// Las celdas van fuera del componente: si se crearan en cada render, la tabla las volvería a montar con cada tecla y el
// campo perdería el foco.
const CeldaConcepto = ({ entry }: { entry: Fila }) => (
  <div>
    <p className="text-label-1 text-content-primary">{entry.concepto}</p>
    <p className="text-caption-1 text-content-tertiary">{entry.detalle}</p>
  </div>
);

const CeldaMonto = ({ entry }: { entry: Fila }) => (
  <Input
    type="number"
    inputMode="decimal"
    step="0.01"
    min={0}
    startAdornment="S/"
    aria-label={entry.nombreAccesible}
    className="h-10 w-32"
    disabled={entry.deshabilitado}
    registration={entry.form.register(entry.campo)}
    error={entry.form.formState.errors[entry.campo]}
  />
);

type ConceptosMontosProps = {
  form: UseFormReturn<ValoresTarifario>;
  tarifario: TarifarioVista;
  deshabilitado: boolean;
};

// Figma > P19 > tarjeta "Conceptos y montos": los tres montos son editables; el vencimiento de la matrícula y el de la
// pensión se calculan en el backend a partir de las fechas del año.
export const ConceptosMontos = ({
  form,
  tarifario,
  deshabilitado,
}: ConceptosMontosProps) => {
  const { cuotas } = tarifario.calendario;
  const filas: Fila[] = [
    {
      id: 'cuota-ingreso',
      concepto: 'Cuota de ingreso',
      detalle: 'Pago único al incorporarse',
      aplicaA: 'Solo alumnos nuevos',
      vencimiento: 'Antes de la matrícula',
      campo: 'montoCuotaIngreso',
      nombreAccesible: 'Monto de la cuota de ingreso, en soles',
      form,
      deshabilitado,
    },
    {
      id: 'matricula',
      concepto: `Matrícula ${tarifario.anio}`,
      detalle: 'Cuota de matrícula',
      aplicaA: 'Todos los alumnos',
      vencimiento: fechaDeDia(tarifario.conceptos.vencimientoMatricula),
      campo: 'montoMatricula',
      nombreAccesible: 'Monto de la matrícula, en soles',
      form,
      deshabilitado,
    },
    {
      id: 'pension',
      concepto: 'Pensión mensual',
      detalle: resumenDeCuotas(cuotas),
      aplicaA: 'Todos los alumnos',
      vencimiento: vencimientoDePension(cuotas),
      campo: 'montoPension',
      nombreAccesible: 'Monto de la pensión mensual, en soles',
      form,
      deshabilitado,
    },
  ];

  return (
    <Card className="flex flex-col gap-4 p-6">
      <h2 className="text-label-1 font-semibold text-content-primary">
        Conceptos y montos
      </h2>
      <Table
        data={filas}
        columns={[
          { title: 'Concepto', field: 'concepto', Cell: CeldaConcepto },
          { title: 'Aplica a', field: 'aplicaA' },
          { title: 'Monto', field: 'campo', Cell: CeldaMonto },
          { title: 'Vencimiento', field: 'vencimiento' },
        ]}
      />
    </Card>
  );
};
