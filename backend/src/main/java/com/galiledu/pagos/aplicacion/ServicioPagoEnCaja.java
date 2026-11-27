package com.galiledu.pagos.aplicacion;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galiledu.compartido.web.ExcepcionNegocio;
import com.galiledu.pagos.aplicacion.DtosPagoEnCaja.PagoCajaRegistrado;
import com.galiledu.pagos.aplicacion.DtosPagoEnCaja.SolicitudPagoCaja;
import com.galiledu.pagos.aplicacion.DtosPagoEnLinea.CuotaPorPagar;
import com.galiledu.pagos.dominio.CompromisoPago;
import com.galiledu.pagos.dominio.Cuota;
import com.galiledu.pagos.dominio.EstadoCompromiso;
import com.galiledu.pagos.dominio.EstadoCuota;
import com.galiledu.pagos.dominio.MedioPagoCaja;
import com.galiledu.pagos.dominio.Pago;
import com.galiledu.pagos.infraestructura.CompromisoPagoRepositorio;
import com.galiledu.pagos.infraestructura.ConsultasPagos;
import com.galiledu.pagos.infraestructura.CuotaRepositorio;
import com.galiledu.pagos.infraestructura.PagoRepositorio;

/**
 * Pago en caja (fila 29 del plan de pruebas, «Registrar pago de cuota»): el apoderado paga en el colegio y el Personal
 * administrativo lo registra desde el cronograma del compromiso (P8). Sigue las reglas del pago en línea (HU-41):
 * cuotas completas, pensiones en orden y recargo por mora calculado a la fecha del pago; emite la misma constancia.
 */
@Service
public class ServicioPagoEnCaja {
	private final CompromisoPagoRepositorio compromisos;
	private final CuotaRepositorio cuotas;
	private final PagoRepositorio pagos;
	private final ConsultasPagos consultas;
	private final CobroCuotas cobro;
	private final ApplicationEventPublisher eventos;
	private final Clock reloj;

	public ServicioPagoEnCaja(CompromisoPagoRepositorio compromisos, CuotaRepositorio cuotas, PagoRepositorio pagos,
			ConsultasPagos consultas, CobroCuotas cobro, ApplicationEventPublisher eventos, Clock reloj) {
		this.compromisos = compromisos;
		this.cuotas = cuotas;
		this.pagos = pagos;
		this.consultas = consultas;
		this.cobro = cobro;
		this.eventos = eventos;
		this.reloj = reloj;
	}

	/** Cuotas del compromiso con saldo, con el recargo que tendrían si se pagan en {@code fecha} (hoy si es null). */
	@Transactional(readOnly = true)
	public List<CuotaPorPagar> cuotasPorPagar(UUID compromisoId, LocalDate fecha) {
		CompromisoPago compromiso = compromisos.findById(compromisoId)
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("No existe el compromiso de pago."));
		LocalDate dia = fecha != null ? fecha : LocalDate.now(reloj);
		exigirFecha(dia);
		if (compromiso.getEstado() != EstadoCompromiso.VIGENTE) {
			return List.of();
		}
		List<Cuota> suyas = cuotas.deEstudiantes(List.of(compromiso.getEstudianteId())).stream()
				.filter(q -> q.getCompromiso().getId().equals(compromisoId))
				.filter(q -> q.estaActiva() && q.getEstado() != EstadoCuota.PAGADA)
				.sorted(Comparator.comparing(Cuota::getFechaVencimiento))
				.toList();
		return suyas.stream().map(q -> CobroCuotas.fila(q, suyas, dia)).toList();
	}

	/** Registra el pago de las cuotas elegidas (completas) y emite su constancia. */
	@Transactional
	public PagoCajaRegistrado registrar(UUID usuarioId, UUID compromisoId, SolicitudPagoCaja solicitud) {
		if (!compromisos.existsById(compromisoId)) {
			throw ExcepcionNegocio.noEncontrado("No existe el compromiso de pago.");
		}
		LocalDate fecha = solicitud.fechaPago();
		exigirFecha(fecha);
		MedioPagoCaja medio = solicitud.medioPago();
		String operacion = solicitud.numeroOperacion() == null ? "" : solicitud.numeroOperacion().trim();
		if (medio.requiereOperacion() && operacion.isEmpty()) {
			throw ExcepcionNegocio.invalido("Ingresa el número de operación del pago con " + medio.etiqueta() + ".");
		}
		String referencia = medio.referencia(medio.requiereOperacion() ? operacion : null);
		if (medio.requiereOperacion()
				&& pagos.existsByOrdenPagoIdIsNullAndRegistradoPorIsNotNullAndReferencia(referencia)) {
			throw ExcepcionNegocio.conflicto(
					"Ya hay un pago registrado con la operación " + operacion + " (" + medio.etiqueta() + ").");
		}

		Set<UUID> ids = new LinkedHashSet<>(solicitud.cuotaIds());
		List<Cuota> elegidas = cuotas.bloquear(ids);
		if (elegidas.size() != ids.size()
				|| elegidas.stream().anyMatch(q -> !q.getCompromiso().getId().equals(compromisoId))) {
			throw ExcepcionNegocio.noEncontrado("Alguna de las cuotas no existe o no es de este compromiso.");
		}
		CompromisoPago compromiso = elegidas.get(0).getCompromiso();
		if (compromiso.getEstado() != EstadoCompromiso.VIGENTE) {
			throw ExcepcionNegocio.conflicto("El compromiso no está vigente: no se pueden registrar pagos.");
		}
		CobroCuotas.exigirPagables(elegidas);
		cobro.exigirOrdenDeAntiguedad(elegidas, ids, fecha);

		LocalDate hoy = LocalDate.now(reloj);
		OffsetDateTime fechaHora = fecha.atTime(LocalTime.now(reloj)).atZone(reloj.getZone()).toOffsetDateTime();
		String nota = solicitud.observacion() == null || solicitud.observacion().isBlank()
				? null
				: solicitud.observacion().trim();
		Pago pago = Pago.enCaja(compromiso.getEstudianteId(), consultas.siguienteConstancia(hoy.getYear()), fechaHora,
				referencia, nota, usuarioId);
		CobroCuotas.Liquidacion liquidacion = new CobroCuotas.Liquidacion();
		BigDecimal capital = BigDecimal.ZERO;
		BigDecimal recargo = BigDecimal.ZERO;
		for (Cuota q : elegidas.stream().sorted(Comparator.comparing(Cuota::getFechaVencimiento)).toList()) {
			BigDecimal saldo = q.saldo();
			BigDecimal mora = CobroCuotas.recargo(q, fecha);
			liquidacion.cobrar(pago, q, saldo, mora, fecha);
			capital = capital.add(saldo);
			recargo = recargo.add(mora);
		}
		pagos.save(pago);
		eventos.publishEvent(liquidacion.evento(pago.getNumeroConstancia(), usuarioId));
		return new PagoCajaRegistrado(pago.getId(), pago.getNumeroConstancia(), capital, recargo,
				pago.getMontoTotal());
	}

	private void exigirFecha(LocalDate fecha) {
		if (fecha.isAfter(LocalDate.now(reloj))) {
			throw ExcepcionNegocio.invalido("La fecha de pago no puede ser posterior a hoy.");
		}
	}
}
