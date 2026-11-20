package com.galiledu.pagos.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galiledu.compartido.web.ExcepcionNegocio;
import com.galiledu.pagos.aplicacion.DtosDescuentos.BeneficiarioHermanos;
import com.galiledu.pagos.aplicacion.DtosDescuentos.DescuentoActual;
import com.galiledu.pagos.aplicacion.DtosDescuentos.DescuentoAplicable;
import com.galiledu.pagos.aplicacion.DtosDescuentos.DescuentoAplicadoVista;
import com.galiledu.pagos.aplicacion.DtosDescuentos.DescuentoDelCompromiso;
import com.galiledu.pagos.aplicacion.DtosDescuentos.DescuentosAplicables;
import com.galiledu.pagos.aplicacion.DtosDescuentos.ResultadoDescuento;
import com.galiledu.pagos.aplicacion.DtosDescuentos.SolicitudAplicarDescuento;
import com.galiledu.pagos.dominio.CompromisoPago;
import com.galiledu.pagos.dominio.Cuota;
import com.galiledu.pagos.dominio.Descuento;
import com.galiledu.pagos.dominio.DescuentoAplicado;
import com.galiledu.pagos.dominio.EstadoCompromiso;
import com.galiledu.pagos.dominio.EstadoDescuento;
import com.galiledu.pagos.dominio.EstadoDescuentoAplicado;
import com.galiledu.pagos.dominio.TipoDescuento;
import com.galiledu.pagos.infraestructura.CompromisoPagoRepositorio;
import com.galiledu.pagos.infraestructura.ConsultasPagos;
import com.galiledu.pagos.infraestructura.ConsultasPagos.HermanoMatriculado;
import com.galiledu.pagos.infraestructura.DescuentoAplicadoRepositorio;
import com.galiledu.pagos.infraestructura.DescuentoRepositorio;

/**
 * HU-38 · Figma "P10 · Descuento aplicado": aplicar un descuento del catálogo al compromiso (RF-86 a RF-89) y revocarlo
 * (RF-90). El descuento es del estudiante (uno ACTIVO a la vez, RF-87) y rebaja las cuotas PENDIENTES sin pagos de
 * los conceptos que cubre y que vencen dentro de su vigencia.
 */
@Service
public class ServicioDescuentosAplicados {
	private static final ZoneId LIMA = ZoneId.of("America/Lima");

	private final CompromisoPagoRepositorio compromisos;
	private final DescuentoRepositorio catalogo;
	private final DescuentoAplicadoRepositorio aplicados;
	private final ConsultasPagos consultas;

	public ServicioDescuentosAplicados(CompromisoPagoRepositorio compromisos, DescuentoRepositorio catalogo,
			DescuentoAplicadoRepositorio aplicados, ConsultasPagos consultas) {
		this.compromisos = compromisos;
		this.catalogo = catalogo;
		this.aplicados = aplicados;
		this.consultas = consultas;
	}

	/** RF-86: descuentos ACTIVOS del catálogo con el porcentaje que le tocaría al estudiante. */
	@Transactional(readOnly = true)
	public DescuentosAplicables aplicables(UUID compromisoId) {
		CompromisoPago c = buscarCompromiso(compromisoId);
		List<HermanoMatriculado> familia = consultas.hermanosMatriculados(c.getEstudianteId(), c.getAnioEscolarId());
		int hermanos = familia.size();
		List<BeneficiarioHermanos> beneficiarios = menores(familia).stream()
				.map(h -> new BeneficiarioHermanos(h.estudianteId(), h.nombre()))
				.toList();
		boolean esMenor = esMenor(familia, c.getEstudianteId());
		List<DescuentoAplicable> lista = catalogo.findByEstadoOrderByNombreAsc(EstadoDescuento.ACTIVO).stream()
				.map(d -> {
					Optional<BigDecimal> porcentaje = d.porcentajePara(hermanos);
					boolean porHermanos = d.getTipo() == TipoDescuento.POR_HERMANOS;
					String motivo = porcentaje.isEmpty()
							? sinTramo(hermanos)
							: porHermanos && !esMenor ? noEsElMenor(familia) : null;
					return new DescuentoAplicable(d.getId(), d.getNombre(), d.getTipo(), d.getAplicacion(),
							porcentaje.orElse(null), d.getConceptos().stream().sorted().toList(), d.getCondicion(),
							motivo == null, motivo, porHermanos ? hermanos : null,
							porHermanos ? beneficiarios : List.of());
				})
				.toList();
		DescuentoActual actual = aplicados
				.findByEstudianteIdAndEstado(c.getEstudianteId(), EstadoDescuentoAplicado.ACTIVO)
				.map(a -> new DescuentoActual(a.getId(), a.getNombre(), a.getPorcentaje()))
				.orElse(null);
		return new DescuentosAplicables(hermanos, actual, lista);
	}

	/** Pestaña "Descuento" de P10: el descuento vigente del estudiante y su historial. */
	@Transactional(readOnly = true)
	public DescuentoDelCompromiso delCompromiso(UUID compromisoId) {
		CompromisoPago c = buscarCompromiso(compromisoId);
		List<DescuentoAplicado> historial = aplicados.findByEstudianteIdOrderByAplicadoEnDesc(c.getEstudianteId());
		Optional<DescuentoAplicado> activo = historial.stream().filter(DescuentoAplicado::estaActivo).findFirst();
		int cuotas = activo.map(a -> (int) c.getCuotas().stream()
				.filter(q -> q.estaActiva() && a.getId().equals(q.getDescuentoAplicadoId())).count())
				.orElse(0);
		return new DescuentoDelCompromiso(activo.map(this::vista).orElse(null), cuotas,
				historial.stream().map(this::vista).toList());
	}

	@Transactional
	public ResultadoDescuento aplicar(UUID compromisoId, SolicitudAplicarDescuento s, UUID usuarioId) {
		CompromisoPago c = buscarCompromiso(compromisoId);
		if (c.getEstado() != EstadoCompromiso.VIGENTE) {
			throw ExcepcionNegocio.conflicto("Solo se aplican descuentos a un compromiso vigente.");
		}
		Descuento d = catalogo.findById(s.descuentoId())
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("No existe el descuento."));
		if (!d.estaActivo()) {
			throw ExcepcionNegocio.conflicto("El descuento \"" + d.getNombre() + "\" está inactivo.");
		}
		List<HermanoMatriculado> familia = consultas.hermanosMatriculados(c.getEstudianteId(), c.getAnioEscolarId());
		int hermanos = familia.size();
		BigDecimal porcentaje = d.porcentajePara(hermanos)
				.orElseThrow(() -> ExcepcionNegocio.conflicto(sinTramo(hermanos)));
		// Corrección del PO: el descuento por hermanos solo se aplica al hermano menor (a ambos si son gemelos).
		if (d.getTipo() == TipoDescuento.POR_HERMANOS && !esMenor(familia, c.getEstudianteId())) {
			throw ExcepcionNegocio.conflicto(noEsElMenor(familia));
		}

		LocalDate desde = s.vigenciaDesde() != null ? s.vigenciaDesde() : LocalDate.now(LIMA);
		boolean hastaFinEscolaridad = hastaFinEscolaridad(s);
		if (s.vigenciaHasta() != null && s.vigenciaHasta().isBefore(desde)) {
			throw ExcepcionNegocio.invalido("La vigencia termina antes de empezar.");
		}

		// RF-87: un solo descuento por estudiante; el reemplazo exige confirmación.
		Optional<DescuentoAplicado> anterior = aplicados.findByEstudianteIdAndEstado(c.getEstudianteId(),
				EstadoDescuentoAplicado.ACTIVO);
		if (anterior.isPresent()) {
			DescuentoAplicado a = anterior.get();
			if (!s.reemplazar()) {
				throw ExcepcionNegocio.conflicto("El estudiante ya tiene el descuento \"" + a.getNombre() + "\" ("
						+ porcentajeTexto(a.getPorcentaje()) + "). El nuevo beneficio lo reemplazará: confirma para "
						+ "continuar.");
			}
			restituir(a);
			a.reemplazar();
			// Se guarda antes de insertar el nuevo: la BD admite un solo ACTIVO por estudiante.
			aplicados.saveAndFlush(a);
		}

		DescuentoAplicado nuevo = aplicados.saveAndFlush(DescuentoAplicado.nuevo(c.getEstudianteId(), d, porcentaje,
				desde, s.vigenciaHasta(), hastaFinEscolaridad, s.motivo().trim(), usuarioId));
		// RF-88: el porcentaje se aplica sobre el monto original de las cuotas pendientes de los conceptos cubiertos.
		int recalculadas = 0;
		for (CompromisoPago compromiso : compromisos.findByEstudianteId(c.getEstudianteId())) {
			if (compromiso.getEstado() != EstadoCompromiso.VIGENTE) {
				continue;
			}
			for (Cuota q : compromiso.getCuotas()) {
				if (d.aplicaA(q.getConceptoCodigo()) && nuevo.vigenteEn(q.getFechaVencimiento())
						&& q.aplicarDescuento(porcentaje, nuevo.getId())) {
					recalculadas++;
				}
			}
		}
		compromisos.flush();
		return new ResultadoDescuento(vista(nuevo), recalculadas);
	}

	/** RF-90: restituye el monto original de las cuotas pendientes; las pagadas no cambian. */
	@Transactional
	public ResultadoDescuento revocar(UUID descuentoAplicadoId, String motivo, UUID usuarioId) {
		DescuentoAplicado a = aplicados.findById(descuentoAplicadoId)
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("No existe el descuento aplicado."));
		if (!a.estaActivo()) {
			throw ExcepcionNegocio.conflicto("El descuento ya no está vigente.");
		}
		int restituidas = restituir(a);
		a.revocar(motivo.trim(), usuarioId);
		aplicados.saveAndFlush(a);
		compromisos.flush();
		return new ResultadoDescuento(vista(a), restituidas);
	}

	private int restituir(DescuentoAplicado a) {
		int restituidas = 0;
		for (CompromisoPago compromiso : compromisos.findByEstudianteId(a.getEstudianteId())) {
			for (Cuota q : compromiso.getCuotas()) {
				if (q.quitarDescuento(a.getId())) {
					restituidas++;
				}
			}
		}
		return restituidas;
	}

	/**
	 * Corrección del PO: "hasta que termine el colegio" no lleva fecha de fin; con una fecha de fin, la fecha es
	 * obligatoria. Sin el indicador (frontend anterior) se deduce de la fecha: vacía = sin fin.
	 */
	private static boolean hastaFinEscolaridad(SolicitudAplicarDescuento s) {
		if (s.hastaFinEscolaridad() == null) {
			return s.vigenciaHasta() == null;
		}
		if (s.hastaFinEscolaridad() && s.vigenciaHasta() != null) {
			throw ExcepcionNegocio.invalido(
					"Si el descuento rige hasta que el estudiante termine el colegio, no indiques la fecha de fin.");
		}
		if (!s.hastaFinEscolaridad() && s.vigenciaHasta() == null) {
			throw ExcepcionNegocio.invalido(
					"Indica hasta cuándo rige el descuento o marca «Hasta que termine el colegio».");
		}
		return s.hastaFinEscolaridad();
	}

	private CompromisoPago buscarCompromiso(UUID id) {
		return compromisos.findById(id)
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("No existe el compromiso de pago."));
	}

	/**
	 * Los hermanos de menor edad: los de fecha de nacimiento más reciente (la lista ya viene del menor al mayor). Si
	 * dos comparten la fecha (gemelos), lo son ambos. Si nadie tiene la fecha registrada, no se puede distinguir y lo
	 * son todos.
	 */
	private static List<HermanoMatriculado> menores(List<HermanoMatriculado> familia) {
		if (familia.isEmpty()) {
			return List.of();
		}
		LocalDate menor = familia.getFirst().fechaNacimiento();
		return familia.stream().filter(h -> Objects.equals(h.fechaNacimiento(), menor)).toList();
	}

	private static boolean esMenor(List<HermanoMatriculado> familia, UUID estudianteId) {
		return menores(familia).stream().anyMatch(h -> h.estudianteId().equals(estudianteId));
	}

	private static String noEsElMenor(List<HermanoMatriculado> familia) {
		List<String> nombres = menores(familia).stream().map(HermanoMatriculado::nombre).toList();
		return nombres.size() == 1
				? "El descuento por hermanos corresponde al hermano menor: " + nombres.getFirst() + "."
				: "El descuento por hermanos corresponde a los hermanos menores: " + String.join(" y ", nombres) + ".";
	}

	private static String sinTramo(int hermanos) {
		return "El estudiante tiene " + hermanos + " hermano(s) matriculado(s) contándolo a él: ningún tramo del "
				+ "descuento le corresponde.";
	}

	private static String porcentajeTexto(BigDecimal porcentaje) {
		return porcentaje.stripTrailingZeros().toPlainString() + " %";
	}

	private DescuentoAplicadoVista vista(DescuentoAplicado a) {
		return new DescuentoAplicadoVista(a.getId(), a.getDescuentoId(), a.getNombre(), a.getPorcentaje(),
				a.getVigenciaDesde(), a.getVigenciaHasta(), a.isHastaFinEscolaridad(), a.getMotivo(), a.getEstado(),
				consultas.nombreDeUsuario(a.getAplicadoPor()),
				a.getAplicadoEn(), a.getMotivoRevocacion(), a.getRevocadoEn());
	}
}
