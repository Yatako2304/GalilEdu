import { Alert } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { OrdenEnProceso } from '@/types/api';

import { soles } from './formato';

const HORA = new Intl.DateTimeFormat('es-PE', {
  timeStyle: 'short',
  timeZone: 'America/Lima',
});

type AvisoPagoEnProcesoProps = {
  orden: OrdenEnProceso;
  cancelando: boolean;
  onCancelar: () => void;
};

/**
 * HU-41: el apoderado cerró la página de la pasarela sin pagar. Sus cuotas siguen "En proceso" hasta que la orden
 * venza (CAHU41-8); aquí puede volver a la pasarela o cancelar el pago para liberarlas de inmediato.
 */
export const AvisoPagoEnProceso = ({
  orden,
  cancelando,
  onCancelar,
}: AvisoPagoEnProcesoProps) => (
  <Alert
    type="warning"
    title={`Tienes un pago en proceso por ${soles(orden.total)}`}
    action={
      <div className="flex flex-wrap gap-2 pr-3">
        <Button
          variant="primary-light"
          size="sm"
          isLoading={cancelando}
          onClick={onCancelar}
        >
          Cancelar pago
        </Button>
        <Button
          size="sm"
          disabled={cancelando}
          onClick={() => window.location.assign(orden.urlPago)}
        >
          Continuar pago
        </Button>
      </div>
    }
  >
    Orden {orden.codigo}. Complétalo antes de las{' '}
    {HORA.format(new Date(orden.expiraEn))} o cancélalo para volver a elegir las
    cuotas.
  </Alert>
);
