package com.galiledu.matricula.aplicacion;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.galiledu.pagos.aplicacion.PagoEnLineaAprobado;

/**
 * HU-31: Matrícula escucha cuando Pagos confirma un pago en línea (HU-41). Se ejecuta después de que el pago quedó
 * guardado, así un problema al matricular nunca deshace un pago ya cobrado por la pasarela.
 */
@Component
public class EscuchaPagoMatricula {
	private static final Logger LOG = LoggerFactory.getLogger(EscuchaPagoMatricula.class);

	private final ServicioConfirmacionPago confirmacion;

	public EscuchaPagoMatricula(ServicioConfirmacionPago confirmacion) {
		this.confirmacion = confirmacion;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void alAprobarsePago(PagoEnLineaAprobado evento) {
		for (UUID matriculaId : evento.matriculasPagadas()) {
			try {
				if (confirmacion.matricular(matriculaId).isPresent()) {
					confirmacion.generarPensiones(matriculaId, evento.usuarioId());
				}
			} catch (RuntimeException e) {
				LOG.error("No se completó la matrícula {} tras el pago de la orden {}", matriculaId,
						evento.codigoOrden(), e);
			}
		}
	}
}
