import { Card } from '@/components/ui/card';
import { Checkbox } from '@/components/ui/form';
import { Spinner } from '@/components/ui/spinner';
import {
  TableBody,
  TableCell,
  TableElement,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { Tag } from '@/components/ui/tag';
import { formatDay } from '@/utils/format';

import { useCrearOrdenPago, useCuotasPorPagar } from '../api/pago-en-linea';

import { BarraPagoCuotas } from './barra-pago-cuotas';
import { etiquetaCuota, soles } from './formato';
import { useSeleccionCuotas } from './seleccion-cuotas';

// HU-41 · Cuotas por pagar con sus casillas y la barra de selección. Es la versión mínima para pagar mientras la
// pantalla de HU-40 (cronograma, saldo e historial) no existe; esa pantalla puede reemplazar esta tabla usando
// useSeleccionCuotas y BarraPagoCuotas, que es lo que aquí se arma.
export const PagoCuotas = () => {
  const consulta = useCuotasPorPagar();
  const seleccion = useSeleccionCuotas(consulta.data);
  const crear = useCrearOrdenPago();

  if (consulta.isLoading) {
    return (
      <div className="flex min-h-40 items-center justify-center">
        <Spinner />
      </div>
    );
  }
  const estudiantes = consulta.data?.estudiantes ?? [];
  const hayCuotas = estudiantes.some((e) => e.cuotas.length > 0);

  return (
    <Card className="space-y-4 p-6">
      <div>
        <h2 className="text-label-1 font-semibold text-content-primary">
          Pagar cuotas en línea
        </h2>
        <p className="text-body-3 text-content-tertiary">
          Marca las cuotas que quieres pagar. Las pensiones se pagan en orden:
          al marcar una, se marcan también las anteriores pendientes.
        </p>
      </div>

      <BarraPagoCuotas
        seleccionadas={seleccion.seleccionadas}
        isLoading={crear.isPending || crear.isSuccess}
        onPagar={() =>
          crear.mutate(seleccion.seleccionadas.map((c) => c.cuotaId))
        }
      />

      {!hayCuotas ? (
        <p className="py-6 text-center text-body-3 text-content-tertiary">
          No tienes cuotas pendientes.
        </p>
      ) : (
        estudiantes
          .filter((e) => e.cuotas.length > 0)
          .map((e) => (
            <section key={e.estudianteId} className="space-y-2">
              <h3 className="text-label-2 font-semibold text-content-primary">
                {e.nombre}
              </h3>
              <TableElement>
                <TableHeader>
                  <TableRow>
                    <TableHead className="w-12">
                      <span className="sr-only">Seleccionar</span>
                    </TableHead>
                    <TableHead>Concepto</TableHead>
                    <TableHead>Vencimiento</TableHead>
                    <TableHead className="text-right">Capital</TableHead>
                    <TableHead className="text-right">Recargo</TableHead>
                    <TableHead className="text-right">Total</TableHead>
                    <TableHead>Estado</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {e.cuotas.map((c) => {
                    const etiqueta = etiquetaCuota(c);
                    return (
                      <TableRow key={c.cuotaId}>
                        <TableCell>
                          <Checkbox
                            aria-label={`Pagar ${c.nombre} de ${e.nombre}`}
                            checked={seleccion.estaSeleccionada(c.cuotaId)}
                            disabled={!c.seleccionable}
                            onChange={() => seleccion.alternar(c.cuotaId)}
                          />
                        </TableCell>
                        <TableCell>{c.nombre}</TableCell>
                        <TableCell>{formatDay(c.fechaVencimiento)}</TableCell>
                        <TableCell className="text-right">
                          {soles(c.capital)}
                        </TableCell>
                        <TableCell className="text-right">
                          {c.recargo > 0 ? soles(c.recargo) : '—'}
                        </TableCell>
                        <TableCell className="text-right">
                          {soles(c.total)}
                        </TableCell>
                        <TableCell>
                          <Tag size="md" color={etiqueta.color}>
                            {etiqueta.texto}
                          </Tag>
                        </TableCell>
                      </TableRow>
                    );
                  })}
                </TableBody>
              </TableElement>
            </section>
          ))
      )}
    </Card>
  );
};
