package com.galiledu.matricula.aplicacion;

import jakarta.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.galiledu.matricula.infraestructura.EscriturasMatricula;
import com.galiledu.pagos.aplicacion.PagoEnLineaAprobado;

/**
 * HU-31: cuando la pasarela confirma el pago de la cuota de matrícula, el estudiante queda MATRICULADO y el apoderado
 * ya ve su sección. Corre en la misma transacción que el pago, así que o se confirman las dos cosas o ninguna. Luego
 * publica {@link MatriculaConfirmada} para que Pagos genere las pensiones.
 */
@Component
public class ConfirmacionMatriculaPorPago {
	private static final Logger LOG = LoggerFactory.getLogger(ConfirmacionMatriculaPorPago.class);

	private final EscriturasMatricula escrituras;
	private final ApplicationEventPublisher eventos;
	private final EntityManager em;

	public ConfirmacionMatriculaPorPago(EscriturasMatricula escrituras, ApplicationEventPublisher eventos,
			EntityManager em) {
		this.escrituras = escrituras;
		this.eventos = eventos;
		this.em = em;
	}

	@EventListener
	@Transactional
	public void alAprobarseElPago(PagoEnLineaAprobado pago) {
		// Pagos deja las cuotas pagadas en memoria (JPA): se escriben antes de consultarlas con SQL.
		em.flush();
		pago.cuotaIds().forEach(cuotaId -> escrituras.matricularPorCuotaPagada(cuotaId).ifPresent(m -> {
			LOG.info("Matrícula {} confirmada por el pago de la orden {}", m.codigo(), pago.codigoOrden());
			eventos.publishEvent(new MatriculaConfirmada(m.id(), pago.usuarioId()));
		}));
	}
}
