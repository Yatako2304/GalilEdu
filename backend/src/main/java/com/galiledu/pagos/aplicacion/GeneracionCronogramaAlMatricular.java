package com.galiledu.pagos.aplicacion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import com.galiledu.matricula.aplicacion.MatriculaConfirmada;

/**
 * HU-31 / HU-37: al quedar matriculado el estudiante se genera su cronograma de pensiones. Corre después de confirmar
 * el pago y en su propia transacción: si falla (por ejemplo, tarifario sin confirmar) el pago y la matrícula quedan
 * firmes y el cronograma se genera luego con {@code POST /pagos/matriculas/{id}/cronograma}.
 */
@Component
public class GeneracionCronogramaAlMatricular {
	private static final Logger LOG = LoggerFactory.getLogger(GeneracionCronogramaAlMatricular.class);

	private final ServicioCronograma cronograma;
	private final TransactionTemplate nueva;

	public GeneracionCronogramaAlMatricular(ServicioCronograma cronograma, PlatformTransactionManager transacciones) {
		this.cronograma = cronograma;
		this.nueva = new TransactionTemplate(transacciones);
		this.nueva.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void alMatricularse(MatriculaConfirmada evento) {
		try {
			nueva.executeWithoutResult(
					t -> cronograma.generarPensiones(evento.matriculaId(), evento.usuarioId()));
		} catch (RuntimeException e) {
			LOG.warn("No se generó el cronograma de la matrícula {}: {}", evento.matriculaId(), e.getMessage());
		}
	}
}
