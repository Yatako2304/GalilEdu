package com.galiledu.pagos.aplicacion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galiledu.compartido.web.ExcepcionNegocio;
import com.galiledu.pagos.aplicacion.DtosPagoEnLinea.ConstanciaEmitida;
import com.galiledu.pagos.aplicacion.DtosPagoEnLinea.CuotaPorPagar;
import com.galiledu.pagos.aplicacion.DtosPagoEnLinea.CuotasPorPagar;
import com.galiledu.pagos.aplicacion.DtosPagoEnLinea.EstudianteConCuotas;
import com.galiledu.pagos.aplicacion.DtosPagoEnLinea.LineaOrden;
import com.galiledu.pagos.aplicacion.DtosPagoEnLinea.OrdenCreada;
import com.galiledu.pagos.aplicacion.DtosPagoEnLinea.OrdenDetalle;
import com.galiledu.pagos.aplicacion.PasarelaPagos.Cobro;
import com.galiledu.pagos.aplicacion.PasarelaPagos.Concepto;
import com.galiledu.pagos.aplicacion.PasarelaPagos.Resultado;
import com.galiledu.pagos.aplicacion.PasarelaPagos.Sesion;
import com.galiledu.pagos.dominio.CompromisoPago;
import com.galiledu.pagos.dominio.Cuota;
import com.galiledu.pagos.dominio.EstadoCompromiso;
import com.galiledu.pagos.dominio.EstadoCuota;
import com.galiledu.pagos.dominio.EstadoOrdenPago;
import com.galiledu.pagos.dominio.OrdenPago;
import com.galiledu.pagos.dominio.Pago;
import com.galiledu.pagos.infraestructura.ConsultasPagos;
import com.galiledu.pagos.infraestructura.ConsultasPagos.EstudianteACargo;
import com.galiledu.pagos.infraestructura.CuotaRepositorio;
import com.galiledu.pagos.infraestructura.OrdenPagoRepositorio;
import com.galiledu.pagos.infraestructura.PagoRepositorio;

/**
 * HU-41 · Pago en línea de cuotas pendientes (secciones 3.7 y 3.8 del documento de Pagos).
 * <ol>
 * <li>El apoderado elige cuotas de sus hijos; las pensiones se pagan en orden de antigüedad.</li>
 * <li>Se crea la orden con capital y recargo, sus cuotas quedan EN_PROCESO y se redirige a la pasarela.</li>
 * <li>La pasarela confirma (notificación o consulta directa): se registra un pago con constancia por estudiante.</li>
 * <li>Si rechaza, el apoderado cancela o pasan 30 minutos, las cuotas vuelven a su estado anterior.</li>
 * </ol>
 */
@Service
public class ServicioPagoEnLinea {
	private static final Logger LOG = LoggerFactory.getLogger(ServicioPagoEnLinea.class);
	static final String RUTA_RETORNO = "/app/apoderado/pensiones/pago";

	private final CuotaRepositorio cuotas;
	private final OrdenPagoRepositorio ordenes;
	private final PagoRepositorio pagos;
	private final ConsultasPagos consultas;
	private final PasarelaPagos pasarela;
	private final ApplicationEventPublisher eventos;
	private final Clock reloj;
	private final int minutosExpiracion;
	private final String urlFrontend;

	public ServicioPagoEnLinea(CuotaRepositorio cuotas, OrdenPagoRepositorio ordenes, PagoRepositorio pagos,
			ConsultasPagos consultas, PasarelaPagos pasarela, ApplicationEventPublisher eventos, Clock reloj,
			@Value("${galiledu.pagos.minutos-expiracion:30}") int minutosExpiracion,
			@Value("${galiledu.pagos.url-frontend}") String urlFrontend) {
		this.cuotas = cuotas;
		this.ordenes = ordenes;
		this.pagos = pagos;
		this.consultas = consultas;
		this.pasarela = pasarela;
		this.eventos = eventos;
		this.reloj = reloj;
		this.minutosExpiracion = minutosExpiracion;
		this.urlFrontend = urlFrontend.replaceAll("/+$", "");
	}

	/** CAHU41-1: cuotas con saldo de cada hijo, con capital y recargo por separado. */
	@Transactional(readOnly = true)
	public CuotasPorPagar cuotasPorPagar(UUID usuarioId) {
		List<EstudianteACargo> hijos = consultas.estudiantesDelUsuario(usuarioId);
		if (hijos.isEmpty()) {
			return new CuotasPorPagar(List.of());
		}
		LocalDate hoy = LocalDate.now(reloj);
		Map<UUID, List<Cuota>> porEstudiante = cuotas.deEstudiantes(hijos.stream().map(EstudianteACargo::id).toList())
				.stream()
				.filter(q -> q.estaActiva() && q.getEstado() != EstadoCuota.PAGADA)
				.collect(Collectors.groupingBy(q -> q.getCompromiso().getEstudianteId()));
		List<EstudianteConCuotas> resultado = new ArrayList<>();
		for (EstudianteACargo hijo : hijos) {
			List<Cuota> suyas = porEstudiante.getOrDefault(hijo.id(), List.of()).stream()
					.sorted(Comparator.comparing(Cuota::getFechaVencimiento))
					.toList();
			List<CuotaPorPagar> filas = suyas.stream().map(q -> fila(q, suyas, hoy)).toList();
			resultado.add(new EstudianteConCuotas(hijo.id(), hijo.nombre(), filas));
		}
		return new CuotasPorPagar(resultado);
	}

	private CuotaPorPagar fila(Cuota q, List<Cuota> delEstudiante, LocalDate hoy) {
		BigDecimal recargo = recargo(q, hoy);
		List<UUID> requiere = pensionesAnterioresPagables(q, delEstudiante).stream().map(Cuota::getId).toList();
		return new CuotaPorPagar(q.getId(), q.getConceptoCodigo(), q.getNumero(), q.getNombre(),
				q.getCompromiso().getAnio(), q.getFechaVencimiento(), q.saldo(), recargo, q.saldo().add(recargo),
				q.estaVencida(hoy, q.getCompromiso().getDiasGracia()), q.getEstado(), q.esPagable(), requiere);
	}

	/**
	 * Recargo por mora con la misma regla de HU-39 (Cuota.recargo, Figma P11), para que la orden cobre lo que se
	 * muestra.
	 */
	private static BigDecimal recargo(Cuota q, LocalDate hoy) {
		CompromisoPago c = q.getCompromiso();
		return q.recargo(hoy, c.getDiasGracia(), c.getRecargoPorcentaje(), c.getTopeRecargoPorcentaje()).setScale(2,
				RoundingMode.HALF_UP);
	}

	/** CAHU41-3: pensiones impagas del mismo hijo que vencen antes; se pagan antes o junto con esta. */
	private static List<Cuota> pensionesAnterioresPagables(Cuota q, List<Cuota> delEstudiante) {
		if (!q.esPension()) {
			return List.of();
		}
		return delEstudiante.stream()
				.filter(o -> o.esPension() && o.esPagable() && !o.getId().equals(q.getId())
						&& o.getFechaVencimiento().isBefore(q.getFechaVencimiento()))
				.toList();
	}

	/** CAHU41-5: crea la orden, pone sus cuotas EN_PROCESO y abre la sesión en la pasarela. */
	@Transactional
	public OrdenCreada crearOrden(UUID usuarioId, List<UUID> cuotaIds) {
		Set<UUID> ids = new LinkedHashSet<>(cuotaIds);
		Set<UUID> hijos = consultas.estudiantesDelUsuario(usuarioId).stream()
				.map(EstudianteACargo::id)
				.collect(Collectors.toSet());
		List<Cuota> elegidas = cuotas.bloquear(ids);
		// CAHU41-13: una cuota de otra familia se trata como inexistente.
		if (elegidas.size() != ids.size() || elegidas.stream().anyMatch(q -> !hijos.contains(
				q.getCompromiso().getEstudianteId()) || q.getCompromiso().getEstado() != EstadoCompromiso.VIGENTE)) {
			throw ExcepcionNegocio.noEncontrado("Alguna de las cuotas no existe o no es de un estudiante a tu cargo.");
		}
		for (Cuota q : elegidas) {
			if (!q.esPagable()) {
				throw ExcepcionNegocio.conflicto(q.getEstado() == EstadoCuota.EN_PROCESO
						? "La cuota «" + q.getNombre() + "» ya tiene un pago en proceso."
						: "La cuota «" + q.getNombre() + "» ya no tiene saldo por pagar.");
			}
		}
		exigirOrdenDeAntiguedad(elegidas, ids);

		OffsetDateTime ahora = OffsetDateTime.now(reloj);
		LocalDate hoy = ahora.toLocalDate();
		OrdenPago orden = OrdenPago.nueva(consultas.siguienteCodigoOrden(hoy.getYear()), usuarioId, ahora,
				ahora.plusMinutes(minutosExpiracion));
		List<Concepto> conceptos = new ArrayList<>();
		for (Cuota q : elegidas.stream().sorted(Comparator.comparing(Cuota::getFechaVencimiento)).toList()) {
			BigDecimal recargo = recargo(q, hoy);
			conceptos.add(new Concepto(descripcion(q, recargo), q.saldo().add(recargo)));
			orden.agregar(q, recargo);
		}
		ordenes.saveAndFlush(orden);

		String retorno = urlFrontend + RUTA_RETORNO + "?orden=" + orden.getCodigo();
		Sesion sesion;
		try {
			sesion = pasarela.crearSesion(new Cobro(orden.getCodigo(), orden.getMontoTotal(), conceptos,
					orden.getExpiraEn(), retorno, retorno + "&cancelado=1"));
		} catch (RuntimeException e) {
			LOG.error("No se pudo crear la sesión de pago de la orden {}", orden.getCodigo(), e);
			throw ExcepcionNegocio.conflicto(
					"No pudimos conectar con la pasarela de pagos. Intenta de nuevo en unos minutos.");
		}
		orden.asignarSesion(sesion.referencia(), sesion.url());
		return new OrdenCreada(orden.getCodigo(), orden.getMontoTotal(), orden.getExpiraEn(), sesion.url());
	}

	private void exigirOrdenDeAntiguedad(List<Cuota> elegidas, Set<UUID> ids) {
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
		}
	}

	private static String descripcion(Cuota q, BigDecimal recargo) {
		String texto = q.getNombre() + " · " + q.getCompromiso().getEstudianteNombre();
		return recargo.signum() > 0 ? texto + " (incluye S/ " + recargo + " de recargo)" : texto;
	}

	/** CAHU41-6/7/9: resultado del pago para la pantalla de regreso; si sigue en proceso, consulta a la pasarela. */
	@Transactional
	public OrdenDetalle consultar(UUID usuarioId, String codigo) {
		OrdenPago orden = propia(usuarioId, codigo);
		if (orden.estaEnProceso()) {
			resolver(orden);
		}
		return detalle(orden);
	}

	/** El apoderado volvió con "cancelar": se cierra la sesión y las cuotas quedan libres de inmediato. */
	@Transactional
	public OrdenDetalle cancelar(UUID usuarioId, String codigo) {
		propia(usuarioId, codigo);
		OrdenPago orden = ordenes.bloquearPorCodigo(codigo).orElseThrow();
		if (orden.estaEnProceso()) {
			Resultado resultado = consultarPasarela(orden);
			if (resultado.estado() == PasarelaPagos.Estado.APROBADO) {
				aprobar(orden, resultado.montoConfirmado());
			} else {
				cancelarEnPasarela(orden);
				cerrarSinPago(orden, EstadoOrdenPago.RECHAZADA, "Cancelaste el pago.");
			}
		}
		return detalle(orden);
	}

	/** Notificación de la pasarela (webhook): se vuelve a consultar el estado real antes de registrar nada. */
	@Transactional
	public void notificar(String referencia) {
		ordenes.bloquearPorReferencia(referencia)
				.filter(OrdenPago::estaEnProceso)
				.ifPresent(this::resolver);
	}

	/** Tarea periódica: resuelve las órdenes cuyo plazo ya pasó (CAHU41-8). */
	@Transactional
	public void resolverSiVencio(String codigo) {
		ordenes.bloquearPorCodigo(codigo)
				.filter(OrdenPago::estaEnProceso)
				.ifPresent(this::resolver);
	}

	@Transactional(readOnly = true)
	public List<String> ordenesVencidas() {
		return ordenes.vencidas(OffsetDateTime.now(reloj));
	}

	private void resolver(OrdenPago orden) {
		Resultado resultado = consultarPasarela(orden);
		switch (resultado.estado()) {
			case APROBADO -> aprobar(orden, resultado.montoConfirmado());
			case RECHAZADO -> cerrarSinPago(orden, EstadoOrdenPago.RECHAZADA,
					resultado.motivo() != null ? resultado.motivo() : "La pasarela rechazó el pago.");
			case EXPIRADO -> cerrarSinPago(orden, EstadoOrdenPago.EXPIRADA,
					"La orden venció sin que se completara el pago.");
			case PENDIENTE -> {
				if (OffsetDateTime.now(reloj).isAfter(orden.getExpiraEn())) {
					cancelarEnPasarela(orden);
					cerrarSinPago(orden, EstadoOrdenPago.EXPIRADA, "La orden venció sin que se completara el pago.");
				}
			}
		}
	}

	private Resultado consultarPasarela(OrdenPago orden) {
		if (orden.getReferenciaPasarela() == null) {
			return Resultado.pendiente();
		}
		try {
			return pasarela.consultar(orden.getReferenciaPasarela());
		} catch (RuntimeException e) {
			LOG.warn("No se pudo consultar la orden {} en la pasarela", orden.getCodigo(), e);
			return Resultado.pendiente();
		}
	}

	private void cancelarEnPasarela(OrdenPago orden) {
		if (orden.getReferenciaPasarela() == null) {
			return;
		}
		try {
			pasarela.cancelar(orden.getReferenciaPasarela());
		} catch (RuntimeException e) {
			LOG.warn("No se pudo cerrar la sesión de la orden {} en la pasarela", orden.getCodigo(), e);
		}
	}

	/**
	 * CAHU41-6/10/11: un pago con constancia por estudiante; las cuotas suman su capital. Si el monto confirmado no
	 * coincide con la orden, los pagos quedan OBSERVADOS para revisión.
	 */
	private void aprobar(OrdenPago orden, BigDecimal montoConfirmado) {
		OffsetDateTime ahora = OffsetDateTime.now(reloj);
		Map<UUID, Cuota> porId = cuotas
				.findAllById(orden.getLineas().stream().map(OrdenPago.Linea::getCuotaId).toList())
				.stream().collect(Collectors.toMap(Cuota::getId, Function.identity()));
		Map<UUID, Pago> porEstudiante = new LinkedHashMap<>();
		List<UUID> pagadas = new ArrayList<>();
		for (OrdenPago.Linea linea : orden.getLineas()) {
			Cuota q = porId.get(linea.getCuotaId());
			Pago pago = porEstudiante.computeIfAbsent(q.getCompromiso().getEstudianteId(),
					est -> Pago.enLinea(orden.getId(), est, consultas.siguienteConstancia(ahora.getYear()), ahora,
							orden.getReferenciaPasarela()));
			pago.agregar(q.getId(), linea.getCapital(), linea.getRecargo());
			q.registrarPago(linea.getCapital(), ahora.toLocalDate());
			if (q.getEstado() == EstadoCuota.PAGADA) {
				pagadas.add(q.getId());
			}
		}
		if (montoConfirmado != null && montoConfirmado.compareTo(orden.getMontoTotal()) != 0) {
			String motivo = "La pasarela confirmó S/ %s y la orden %s era por S/ %s."
					.formatted(montoConfirmado, orden.getCodigo(), orden.getMontoTotal());
			porEstudiante.values().forEach(p -> p.observar(motivo));
			LOG.warn(motivo);
		}
		pagos.saveAll(porEstudiante.values());
		orden.aprobar(montoConfirmado, ahora);
		eventos.publishEvent(new PagoEnLineaAprobado(orden.getId(), orden.getCodigo(), orden.getUsuarioId(), pagadas));
	}

	private void cerrarSinPago(OrdenPago orden, EstadoOrdenPago estado, String motivo) {
		Map<UUID, Cuota> porId = cuotas
				.findAllById(orden.getLineas().stream().map(OrdenPago.Linea::getCuotaId).toList())
				.stream().collect(Collectors.toMap(Cuota::getId, Function.identity()));
		orden.getLineas().forEach(l -> porId.get(l.getCuotaId()).restaurar(l.getEstadoAnterior()));
		orden.cerrarSinPago(estado, motivo, OffsetDateTime.now(reloj));
	}

	private OrdenPago propia(UUID usuarioId, String codigo) {
		return ordenes.findByCodigo(codigo)
				.filter(o -> o.getUsuarioId().equals(usuarioId))
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("No existe la orden de pago " + codigo + "."));
	}

	private OrdenDetalle detalle(OrdenPago orden) {
		Map<UUID, Cuota> porId = cuotas
				.findAllById(orden.getLineas().stream().map(OrdenPago.Linea::getCuotaId).toList())
				.stream().collect(Collectors.toMap(Cuota::getId, Function.identity()));
		Map<UUID, String> nombres = porId.values().stream().collect(Collectors.toMap(
				q -> q.getCompromiso().getEstudianteId(), q -> q.getCompromiso().getEstudianteNombre(), (a, b) -> a));
		List<LineaOrden> lineas = orden.getLineas().stream()
				.map(l -> {
					Cuota q = porId.get(l.getCuotaId());
					return new LineaOrden(l.getCuotaId(), q.getCompromiso().getEstudianteNombre(), q.getNombre(),
							l.getCapital(), l.getRecargo());
				})
				.toList();
		List<ConstanciaEmitida> constancias = pagos.findByOrdenPagoId(orden.getId()).stream()
				.map(p -> new ConstanciaEmitida(p.getNumeroConstancia(), nombres.get(p.getEstudianteId()),
						p.getMontoTotal(), p.getEstado()))
				.toList();
		return new OrdenDetalle(orden.getCodigo(), orden.getEstado(), orden.getMontoTotal(),
				orden.getMontoConfirmado(), orden.getMotivoRechazo(), orden.getCreadoEn(), orden.getExpiraEn(),
				orden.getUrlPago(), lineas, constancias);
	}
}
