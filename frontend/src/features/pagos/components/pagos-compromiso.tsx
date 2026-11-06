import { usePagosCompromiso } from '../api/pagos';

import { TablaPagos } from './tabla-pagos';

export const PagosCompromiso = ({ compromisoId }: { compromisoId: string }) => {
  const pagos = usePagosCompromiso(compromisoId);
  return <TablaPagos pagos={pagos.data} isLoading={pagos.isLoading} />;
};
