package com.galiledu.matricula.aplicacion;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.galiledu.pagos.aplicacion.PagoRegistrado;

/**
 * HU-31: Matrícula escucha cuando Pagos registra un pago (en línea, HU-41, o en caja). Se ejecuta después de que el
 * pago quedó
 * guardado, así un problema al matricular nunca deshace un pago ya cobrado.
 */
@Component
public class EscuchaPagoMatricula {
	private static final Logger LOG = LoggerFactory.getLogger(EscuchaPagoMatricula.class);

	private final ServicioConfirmacionPago confirmacion;

	public EscuchaPagoMatricula(ServicioConfirmacionPago confirmacion) {
		this.confirmacion = confirmacion;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void alAprobarsePago(PagoRegistrado evento) {
		for (UUID matriculaId : evento.matriculasPagadas()) {
			try {
				if (confirmacion.matricular(matriculaId).isPresent()) {
					confirmacion.generarPensiones(matriculaId, evento.usuarioId());
				}
			} catch (RuntimeException e) {
				LOG.error("No se completó la matrícula {} tras el pago {}", matriculaId,
						evento.referencia(), e);
			}
		}
	}
}
