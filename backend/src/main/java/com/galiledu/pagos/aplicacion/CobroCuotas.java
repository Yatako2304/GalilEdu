package com.galiledu.pagos.aplicacion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.galiledu.compartido.web.ExcepcionNegocio;
import com.galiledu.pagos.aplicacion.DtosPagoEnLinea.CuotaPorPagar;
import com.galiledu.pagos.dominio.CompromisoPago;
import com.galiledu.pagos.dominio.Cuota;
import com.galiledu.pagos.dominio.EstadoCuota;
import com.galiledu.pagos.dominio.Pago;
import com.galiledu.pagos.infraestructura.CuotaRepositorio;

/**
 * Reglas para cobrar cuotas que comparten el pago en línea (HU-41) y el pago en caja: qué cuotas se pueden pagar, en
 * qué orden, cuánto recargo llevan a una fecha y cómo quedan después de pagarse.
 */
@Component
class CobroCuotas {
	private final CuotaRepositorio cuotas;

	CobroCuotas(CuotaRepositorio cuotas) {
		this.cuotas = cuotas;
	}

	/** Recargo por mora a la fecha con la misma regla de HU-39 (Cuota.recargo, Figma P11). */
	static BigDecimal recargo(Cuota q, LocalDate fecha) {
		CompromisoPago c = q.getCompromiso();
		return q.recargo(fecha, c.getDiasGracia(), c.getRecargoPorcentaje(), c.getTopeRecargoPorcentaje()).setScale(2,
				RoundingMode.HALF_UP);
	}

	/** Una cuota con saldo, su recargo a la fecha y las pensiones anteriores que deben pagarse con ella. */
	static CuotaPorPagar fila(Cuota q, List<Cuota> delEstudiante, LocalDate fecha) {
		BigDecimal recargo = recargo(q, fecha);
		List<UUID> requiere = pensionesAnterioresPagables(q, delEstudiante).stream().map(Cuota::getId).toList();
		return new CuotaPorPagar(q.getId(), q.getConceptoCodigo(), q.getNumero(), q.getNombre(),
				q.getCompromiso().getAnio(), q.getFechaVencimiento(), q.saldo(), recargo, q.saldo().add(recargo),
				q.estaVencida(fecha, q.getCompromiso().getDiasGracia()), q.getEstado(), q.esPagable(), requiere);
	}

	/** CAHU41-3: pensiones impagas del mismo estudiante que vencen antes; se pagan antes o junto con esta. */
	static List<Cuota> pensionesAnterioresPagables(Cuota q, List<Cuota> delEstudiante) {
		if (!q.esPension()) {
			return List.of();
		}
		return delEstudiante.stream()
				.filter(o -> o.esPension() && o.esPagable() && !o.getId().equals(q.getId())
						&& o.getFechaVencimiento().isBefore(q.getFechaVencimiento()))
				.toList();
	}

	/** Cada cuota elegida tiene saldo y no está en un pago en línea en proceso. */
	static void exigirPagables(Collection<Cuota> elegidas) {
		for (Cuota q : elegidas) {
			if (!q.esPagable()) {
				throw ExcepcionNegocio.conflicto(q.getEstado() == EstadoCuota.EN_PROCESO
						? "La cuota «" + q.getNombre() + "» ya tiene un pago en proceso."
						: "La cuota «" + q.getNombre() + "» ya no tiene saldo por pagar.");
			}
		}
	}

	/**
	 * CAHU41-3: las pensiones se pagan en orden de antigüedad. CAHU31-2: la matrícula se paga cuando el estudiante no
	 * tiene pensiones vencidas a la fecha (o se pagan junto con ella).
	 */
	void exigirOrdenDeAntiguedad(List<Cuota> elegidas, Set<UUID> ids, LocalDate fecha) {
		Set<UUID> estudiantes = elegidas.stream().map(q -> q.getCompromiso().getEstudianteId())
				.collect(Collectors.toSet());
		Map<UUID, List<Cuota>> todas = cuotas.deEstudiantes(estudiantes).stream()
				.collect(Collectors.groupingBy(q -> q.getCompromiso().getEstudianteId()));
		for (Cuota q : elegidas) {
			pensionesAnterioresPagables(q, todas.get(q.getCompromiso().getEstudianteId())).stream()
					.filter(anterior -> !ids.contains(anterior.getId()))
					.min(Comparator.comparing(Cuota::getFechaVencimiento))
					.ifPresent(anterior -> {
						throw ExcepcionNegocio.invalido("Primero paga la «" + anterior.getNombre() + "» de "
								+ q.getCompromiso().getEstudianteNombre() + ": las pensiones se pagan en orden.");
					});
			if (q.esMatricula()) {
				boolean debe = todas.get(q.getCompromiso().getEstudianteId()).stream()
						.anyMatch(o -> o.esPagable() && !ids.contains(o.getId())
								&& o.estaVencida(fecha, o.getCompromiso().getDiasGracia()));
				if (debe) {
					throw ExcepcionNegocio.invalido("Primero paga las pensiones vencidas de "
							+ q.getCompromiso().getEstudianteNombre() + " para pagar la matrícula.");
				}
			}
		}
	}

	/** Cuotas que se van pagando en un mismo pago (o en los pagos de una misma orden) para armar el evento. */
	static final class Liquidacion {
		private final List<UUID> pagadas = new ArrayList<>();
		private final List<UUID> matriculas = new ArrayList<>();

		/** Agrega la cuota a la constancia y le suma el capital; anota si quedó pagada por completo. */
		void cobrar(Pago pago, Cuota q, BigDecimal capital, BigDecimal recargo, LocalDate fecha) {
			pago.agregar(q.getId(), capital, recargo);
			q.registrarPago(capital, fecha);
			if (q.getEstado() == EstadoCuota.PAGADA) {
				pagadas.add(q.getId());
				if (q.esMatricula() && q.getCompromiso().getMatriculaId() != null) {
					matriculas.add(q.getCompromiso().getMatriculaId());
				}
			}
		}

		PagoRegistrado evento(String referencia, UUID usuarioId) {
			return new PagoRegistrado(referencia, usuarioId, List.copyOf(pagadas), List.copyOf(matriculas));
		}
	}
}
