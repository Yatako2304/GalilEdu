import { Download } from 'lucide-react';
import * as React from 'react';

import { ContentLayout } from '@/components/layouts';
import { Alert } from '@/components/ui/alert';
import { Avatar } from '@/components/ui/avatar';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Select } from '@/components/ui/form';
import { Spinner } from '@/components/ui/spinner';
import {
  TableBody,
  TableCell,
  TableElement,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { Tabs } from '@/components/ui/tabs';
import { paths } from '@/config/paths';
import {
  ReporteAsistencia as Reporte,
  TipoReporteAsistencia,
} from '@/types/api';

import {
  exportarReporteAsistencia,
  FiltroReporte,
  useReporteAsistencia,
  useSeccionesAsistencia,
} from '../api/asistencia';

const MIGAS = [
  { label: 'Asistencia', to: paths.app.tutor.asistencia.getHref() },
  'Reporte',
];

/** "2026-09-14" → "14/09/2026". */
const fecha = (iso: string) => iso.split('-').reverse().join('/');

/**
 * HU-68 · RF-146: reporte de inasistencias y tardanzas de la sección, por mes o por periodo (el plan de pruebas lo pide
 * por bimestre), ordenado por apellidos y exportable a PDF. Figma no tiene esta pantalla: se armó con los componentes
 * de la de asistencia (ver docs/modulos/asistencia.md).
 */
export const ReporteAsistencia = () => {
  const secciones = useSeccionesAsistencia();
  const seccion = secciones.data?.[0];
  const [filtro, setFiltro] = React.useState<FiltroReporte>({});
  const reporte = useReporteAsistencia(seccion?.id, filtro);

  if (secciones.isLoading || (seccion && !reporte.data)) {
    return (
      <ContentLayout title="Reporte de asistencia" breadcrumbs={MIGAS}>
        <div className="flex h-48 items-center justify-center">
          <Spinner size="lg" />
        </div>
      </ContentLayout>
    );
  }
  if (!seccion || !reporte.data) {
    return (
      <ContentLayout title="Reporte de asistencia" breadcrumbs={MIGAS}>
        <Alert type="default" title="No tienes una sección a cargo.">
          El reporte es del tutor o cotutor de una sección del año escolar
          activo.
        </Alert>
      </ContentLayout>
    );
  }
  return (
    <PantallaReporte
      reporte={reporte.data}
      cargando={reporte.isFetching}
      onFiltro={setFiltro}
    />
  );
};

const PantallaReporte = ({
  reporte,
  cargando,
  onFiltro,
}: {
  reporte: Reporte;
  cargando: boolean;
  onFiltro: (filtro: FiltroReporte) => void;
}) => {
  const [exportando, setExportando] = React.useState(false);
  const { meses, periodos } = reporte.opciones;
  const nombrePeriodo = periodos[0]?.etiqueta
    .toLowerCase()
    .includes('trimestre')
    ? 'trimestre'
    : 'bimestre';
  const opciones = reporte.tipo === 'MES' ? meses : periodos;

  const cambiarTipo = (tipo: TipoReporteAsistencia) => {
    const primera = (tipo === 'MES' ? meses : periodos).at(
      tipo === 'MES' ? 0 : -1,
    );
    if (primera) onFiltro({ tipo, valor: primera.valor });
  };

  const exportar = async () => {
    setExportando(true);
    try {
      await exportarReporteAsistencia(
        reporte.seccion.id,
        { tipo: reporte.tipo, valor: reporte.valor },
        `reporte-asistencia-${reporte.valor}.pdf`,
      );
    } catch {
      // El cliente de la API ya muestra el error como notificación.
    } finally {
      setExportando(false);
    }
  };

  return (
    <ContentLayout
      title="Reporte de asistencia"
      breadcrumbs={MIGAS}
      subtitle={`${reporte.seccion.grado} · Sección ${reporte.seccion.nombre} · Año escolar ${reporte.seccion.anio}`}
      actions={
        <Button
          variant="primary"
          icon={<Download className="size-4" aria-hidden="true" />}
          onClick={exportar}
          isLoading={exportando}
        >
          Exportar PDF
        </Button>
      }
    >
      <div className="flex flex-col gap-5">
        <Card className="flex flex-wrap items-end justify-between gap-4 p-4">
          <div className="flex flex-wrap items-end gap-4">
            <Tabs
              variant="pill"
              aria-label="Tipo de reporte"
              items={[
                { value: 'MES' as const, label: 'Por mes' },
                {
                  value: 'PERIODO' as const,
                  label: `Por ${nombrePeriodo}`,
                  disabled: periodos.length === 0,
                },
              ]}
              value={reporte.tipo}
              onValueChange={cambiarTipo}
            />
            <Select
              label={reporte.tipo === 'MES' ? 'Mes' : 'Periodo'}
              options={opciones.map((o) => ({
                value: o.valor,
                label: o.etiqueta,
              }))}
              value={reporte.valor}
              onChange={(evento) =>
                onFiltro({ tipo: reporte.tipo, valor: evento.target.value })
              }
              wrapperClassName="w-56"
            />
            {cargando && <Spinner size="sm" />}
          </div>
          <p className="text-body-4 text-content-tertiary">
            Del {fecha(reporte.desde)} al {fecha(reporte.hasta)} ·{' '}
            {reporte.diasRegistrados === 1
              ? '1 día con asistencia registrada'
              : `${reporte.diasRegistrados} días con asistencia registrada`}
          </p>
        </Card>

        {reporte.diasRegistrados === 0 && (
          <Alert
            type="default"
            title={`No hay asistencia registrada en ${reporte.etiqueta.toLowerCase()}.`}
          >
            Los totales aparecen en cero hasta que se registre algún día.
          </Alert>
        )}

        <Card className="grid grid-cols-2 divide-stroke-tertiary sm:grid-cols-4 sm:divide-x">
          <Total
            color="bg-surface-error"
            etiqueta="Faltas"
            valor={reporte.totales.faltas}
          />
          <Total
            color="bg-surface-success"
            etiqueta="Faltas justificadas"
            valor={reporte.totales.faltasJustificadas}
          />
          <Total
            color="bg-surface-warning"
            etiqueta="Tardanzas"
            valor={reporte.totales.tardanzas}
          />
          <Total
            color="bg-surface-success"
            etiqueta="Tardanzas justificadas"
            valor={reporte.totales.tardanzasJustificadas}
          />
        </Card>

        <Card className="overflow-hidden p-0">
          {reporte.filas.length === 0 ? (
            <p className="py-10 text-center text-body-3 text-content-tertiary">
              La sección aún no tiene estudiantes matriculados.
            </p>
          ) : (
            <TableElement>
              <TableHeader>
                <TableRow>
                  <TableHead className="w-16">N.°</TableHead>
                  <TableHead>Estudiante</TableHead>
                  <TableHead className="text-right">Faltas</TableHead>
                  <TableHead className="text-right">Justificadas</TableHead>
                  <TableHead className="text-right">Tardanzas</TableHead>
                  <TableHead className="text-right">Justificadas</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {reporte.filas.map((fila, indice) => (
                  <TableRow key={fila.matriculaId}>
                    <TableCell className="text-content-tertiary">
                      {indice + 1}
                    </TableCell>
                    <TableCell>
                      <div className="flex items-center gap-3">
                        <Avatar
                          name={`${fila.nombres} ${fila.apellidos}`}
                          size="sm"
                        />
                        <div className="flex flex-col">
                          <span className="text-body-3 text-content-primary">
                            {fila.apellidos}, {fila.nombres}
                          </span>
                          <span className="text-caption-1 text-content-quartiary">
                            Cód. {fila.documento}
                          </span>
                        </div>
                      </div>
                    </TableCell>
                    <TableCell className="text-right">{fila.faltas}</TableCell>
                    <TableCell className="text-right">
                      {fila.faltasJustificadas}
                    </TableCell>
                    <TableCell className="text-right">
                      {fila.tardanzas}
                    </TableCell>
                    <TableCell className="text-right">
                      {fila.tardanzasJustificadas}
                    </TableCell>
                  </TableRow>
                ))}
                <TableRow className="bg-surface-secondary font-semibold">
                  <TableCell />
                  <TableCell>Total de la sección</TableCell>
                  <TableCell className="text-right">
                    {reporte.totales.faltas}
                  </TableCell>
                  <TableCell className="text-right">
                    {reporte.totales.faltasJustificadas}
                  </TableCell>
                  <TableCell className="text-right">
                    {reporte.totales.tardanzas}
                  </TableCell>
                  <TableCell className="text-right">
                    {reporte.totales.tardanzasJustificadas}
                  </TableCell>
                </TableRow>
              </TableBody>
            </TableElement>
          )}
        </Card>
      </div>
    </ContentLayout>
  );
};

const Total = ({
  color,
  etiqueta,
  valor,
}: {
  color: string;
  etiqueta: string;
  valor: number;
}) => (
  <div className="flex flex-col gap-1 px-5 py-4">
    <span className="inline-flex items-center gap-2 text-caption-1 text-content-tertiary">
      <span className={`size-2 rounded-full ${color}`} aria-hidden="true" />
      {etiqueta}
    </span>
    <span className="text-h6 font-semibold text-content-primary">{valor}</span>
  </div>
);
