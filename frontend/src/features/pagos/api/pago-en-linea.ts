import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { api } from '@/lib/api-client';
import { CuotasPorPagar, OrdenCreada, OrdenPagoDetalle } from '@/types/api';

// HU-41 · Pago en línea (backend: pagos/web/PagoEnLineaController.java).
const RUTA = '/pagos/pago-en-linea';

export const claveCuotasPorPagar = ['pagos', 'pago-en-linea', 'cuotas'];
const claveOrden = (codigo: string) => [
  'pagos',
  'pago-en-linea',
  'orden',
  codigo,
];

export const useCuotasPorPagar = () =>
  useQuery({
    queryKey: claveCuotasPorPagar,
    queryFn: (): Promise<CuotasPorPagar> => api.get(`${RUTA}/cuotas`),
  });

/** Crea la orden y lleva al apoderado a la página de pago de la pasarela. */
export const useCrearOrdenPago = () =>
  useMutation({
    mutationFn: (cuotaIds: string[]): Promise<OrdenCreada> =>
      api.post(`${RUTA}/ordenes`, { cuotaIds }),
    onSuccess: (orden) => {
      window.location.assign(orden.urlPago);
    },
  });

/** Resultado del pago. Mientras la pasarela no responde, vuelve a consultar cada 3 segundos. */
export const useOrdenPago = (codigo: string | null) =>
  useQuery({
    queryKey: claveOrden(codigo ?? ''),
    queryFn: (): Promise<OrdenPagoDetalle> =>
      api.get(`${RUTA}/ordenes/${codigo}`),
    enabled: Boolean(codigo),
    retry: false,
    refetchInterval: (consulta) =>
      consulta.state.data?.estado === 'EN_PROCESO' ? 3000 : false,
  });

const useResolverOrden = <T>(
  enviar: (dato: T) => Promise<OrdenPagoDetalle>,
) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: enviar,
    onSuccess: (orden) => {
      queryClient.setQueryData(claveOrden(orden.codigo), orden);
      queryClient.invalidateQueries({ queryKey: claveCuotasPorPagar });
    },
  });
};

/** El apoderado volvió de la pasarela sin pagar: se liberan sus cuotas. */
export const useCancelarOrdenPago = () =>
  useResolverOrden((codigo: string) =>
    api.post(`${RUTA}/ordenes/${codigo}/cancelar`),
  );

/** Solo con la pasarela simulada: aprobar o rechazar como lo haría la página de pago. */
export const useSimularPago = (codigo: string) =>
  useResolverOrden((aprobar: boolean) =>
    api.post(`${RUTA}/simulador/${codigo}`, { aprobar }),
  );
