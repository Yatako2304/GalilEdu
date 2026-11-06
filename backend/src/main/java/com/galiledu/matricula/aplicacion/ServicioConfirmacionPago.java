package com.galiledu.matricula.aplicacion;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.galiledu.compartido.web.ExcepcionNegocio;
import com.galiledu.matricula.infraestructura.ConsultasMatricula;
import com.galiledu.pagos.aplicacion.ServicioCronograma;

/**
 * HU-31 · Derivación a pagos: al confirmarse el pago de la cuota de matrícula, la matrícula queda MATRICULADO con su
 * código y se generan las pensiones del año (HU-37). Cada paso va en su propia transacción: el pago ya quedó registrado
 * y, si el cronograma falla (por ejemplo, ya existía), la matrícula sigue confirmada y el personal puede generarlo
 * con POST /pagos/matriculas/{id}/cronograma.
 */
@Service
public class ServicioConfirmacionPago {
	private static final Logger LOG = LoggerFactory.getLogger(ServicioConfirmacionPago.class);

	private final ConsultasMatricula consultas;
	private final ServicioCronograma cronograma;
	private final Clock reloj;

	public ServicioConfirmacionPago(ConsultasMatricula consultas, ServicioCronograma cronograma, Clock reloj) {
		this.consultas = consultas;
		this.cronograma = cronograma;
		this.reloj = reloj;
	}

	/** CAHU31-4 y CAHU31-6: solo una matrícula PENDIENTE_PAGO se confirma; un segundo aviso no hace nada. */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public Optional<String> matricular(UUID matriculaId) {
		Optional<String> codigo = consultas.matricularPorPago(matriculaId, OffsetDateTime.now(reloj));
		codigo.ifPresent(c -> LOG.info("Matrícula {} confirmada por pago con el código {}", matriculaId, c));
		return codigo;
	}

	/** Cronograma de pensiones del año (HU-37), en una transacción aparte de la confirmación. */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void generarPensiones(UUID matriculaId, UUID usuarioId) {
		try {
			cronograma.generarPensiones(matriculaId, usuarioId);
		} catch (ExcepcionNegocio e) {
			LOG.warn("No se generaron las pensiones de la matrícula {}: {}", matriculaId, e.getMessage());
			throw e;
		}
	}
}
