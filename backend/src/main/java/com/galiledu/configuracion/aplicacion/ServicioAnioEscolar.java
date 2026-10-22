package com.galiledu.configuracion.aplicacion;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galiledu.compartido.web.ExcepcionNegocio;
import com.galiledu.configuracion.aplicacion.DtosConfiguracion.AnioEscolarActual;
import com.galiledu.configuracion.aplicacion.DtosConfiguracion.AnioEscolarDetalle;
import com.galiledu.configuracion.aplicacion.DtosConfiguracion.AnioEscolarResumen;
import com.galiledu.configuracion.aplicacion.DtosConfiguracion.PeriodoDetalle;
import com.galiledu.configuracion.aplicacion.DtosConfiguracion.Requisito;
import com.galiledu.configuracion.aplicacion.DtosConfiguracion.RequisitosAnio;
import com.galiledu.configuracion.aplicacion.DtosConfiguracion.SolicitudAnioEscolar;
import com.galiledu.configuracion.aplicacion.DtosConfiguracion.SolicitudEdicionAnio;
import com.galiledu.configuracion.aplicacion.DtosConfiguracion.SolicitudPeriodo;
import com.galiledu.configuracion.aplicacion.DtosConfiguracion.SolicitudRegimen;
import com.galiledu.configuracion.dominio.AnioEscolar;
import com.galiledu.configuracion.dominio.EstadoAnioEscolar;
import com.galiledu.configuracion.dominio.PeriodoAcademico;
import com.galiledu.configuracion.dominio.Regimen;
import com.galiledu.configuracion.infraestructura.AnioEscolarRepositorio;
import com.galiledu.configuracion.infraestructura.ConsultasConfiguracion;
import com.galiledu.configuracion.infraestructura.ConsultasConfiguracion.ConteosAnio;

/**
 * HU-74: años escolares y sus periodos. "Hoy" sale del {@link Clock} (America/Lima). El dominio lanza
 * IllegalStateException cuando el estado no permite la operación (se traduce a 409) e IllegalArgumentException cuando
 * los datos no son válidos (400), siempre con el mismo mensaje.
 */
@Service
public class ServicioAnioEscolar {
	private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	private final AnioEscolarRepositorio anios;
	private final ConsultasConfiguracion consultas;
	private final Clock reloj;

	public ServicioAnioEscolar(AnioEscolarRepositorio anios, ConsultasConfiguracion consultas, Clock reloj) {
		this.anios = anios;
		this.consultas = consultas;
		this.reloj = reloj;
	}

	private LocalDate hoy() {
		return LocalDate.now(reloj);
	}

	@Transactional(readOnly = true)
	public List<AnioEscolarResumen> listar() {
		return consultas.anios().stream().map(AnioEscolarResumen::de).toList();
	}

	@Transactional(readOnly = true)
	public AnioEscolarDetalle detalle(UUID id) {
		return detalle(buscar(id));
	}

	/** Card del año en curso (CI-01): el año ACTIVO con su periodo en curso, conteos y avance. */
	@Transactional(readOnly = true)
	public AnioEscolarActual actual() {
		AnioEscolar anio = anios.findByEstado(EstadoAnioEscolar.ACTIVO).stream().findFirst()
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("No hay un año escolar activo."));
		return AnioEscolarActual.de(anio, consultas.conteos(anio.getId()), consultas.alumnosMatriculados(anio.getId()),
				consultas.codigosEstructuras(anio.getId()), hoy());
	}

	/** Crea el año PLANIFICADO y genera sus periodos (4 bimestres o 3 trimestres) con fechas sugeridas. */
	@Transactional
	public AnioEscolarDetalle crear(SolicitudAnioEscolar solicitud) {
		validarFechas(solicitud.fechaInicio(), solicitud.fechaFin(), solicitud.regimen());
		if (anios.existsByAnio(solicitud.anio().shortValue())) {
			throw ExcepcionNegocio.conflicto("Ya existe un año escolar " + solicitud.anio() + ".");
		}
		AnioEscolar nuevo = AnioEscolar.planificar(solicitud.anio(), solicitud.fechaInicio(), solicitud.fechaFin(),
				solicitud.regimen());
		return detalle(anios.save(nuevo));
	}

	/** Cambia las fechas de un año PLANIFICADO y vuelve a generar sus periodos con fechas sugeridas. */
	@Transactional
	public AnioEscolarDetalle editar(UUID id, SolicitudEdicionAnio solicitud) {
		AnioEscolar anio = buscar(id);
		validarFechas(solicitud.fechaInicio(), solicitud.fechaFin(), anio.getRegimen());
		regenerarPeriodos(anio, () -> anio.editarFechas(solicitud.fechaInicio(), solicitud.fechaFin()));
		return detalle(anio);
	}

	/** Cambia el régimen de un año PLANIFICADO y vuelve a generar sus periodos con fechas sugeridas. */
	@Transactional
	public AnioEscolarDetalle cambiarRegimen(UUID id, SolicitudRegimen solicitud) {
		AnioEscolar anio = buscar(id);
		validarFechas(anio.getFechaInicio(), anio.getFechaFin(), solicitud.regimen());
		regenerarPeriodos(anio, () -> anio.cambiarRegimen(solicitud.regimen()));
		return detalle(anio);
	}

	/** Los 3 requisitos obligatorios y los 2 recomendados de "Antes de activar el año" (CI-06). */
	@Transactional(readOnly = true)
	public RequisitosAnio requisitos(UUID id) {
		return requisitos(buscar(id));
	}

	/** Figma "Modal - Editar periodo". */
	@Transactional
	public PeriodoDetalle editarPeriodo(UUID periodoId, SolicitudPeriodo solicitud) {
		AnioEscolar anio = buscarPorPeriodo(periodoId);
		traducir(() -> anio.editarPeriodo(periodoId, solicitud.nombre(), solicitud.fechaInicio(), solicitud.fechaFin(),
				solicitud.fechaLimiteNotas(), hoy()));
		return PeriodoDetalle.de(periodoDe(anio, periodoId), anio.getEstado(), hoy());
	}

	/**
	 * Para otros módulos (Calificaciones): el periodo en curso del año; vacío si el año no está ACTIVO o hoy no cae en
	 * ninguno.
	 */
	@Transactional(readOnly = true)
	public Optional<PeriodoDetalle> periodoEnCurso(UUID anioId) {
		AnioEscolar anio = buscar(anioId);
		return anio.periodoEnCurso(hoy()).map(p -> PeriodoDetalle.de(p, anio.getEstado(), hoy()));
	}

	/** Para otros módulos (Calificaciones): si los docentes pueden registrar o cambiar notas del periodo hoy. */
	@Transactional(readOnly = true)
	public boolean registroNotasAbierto(UUID periodoId) {
		AnioEscolar anio = buscarPorPeriodo(periodoId);
		return periodoDe(anio, periodoId).registroNotasAbierto(hoy(), anio.getEstado());
	}

	// PENDIENTE(grupo): activar y cerrar aún no tienen endpoint; Figma no tiene botón "Activar".
	/**
	 * Activa un año PLANIFICADO. Rechaza si otro año sigue activo o si falta un requisito obligatorio. Esas
	 * validaciones
	 * dependen de otros datos, por eso están aquí y no en el dominio.
	 */
	@Transactional
	public AnioEscolarDetalle activar(UUID id) {
		AnioEscolar anio = buscar(id);
		if (anio.getEstado() == EstadoAnioEscolar.PLANIFICADO) {
			anios.findByEstado(EstadoAnioEscolar.ACTIVO).stream().findFirst().ifPresent(activo -> {
				throw ExcepcionNegocio.conflicto(
						"El año " + activo.getAnio() + " sigue activo. Ciérralo antes de activar este.");
			});
			List<String> faltantes = requisitos(anio).obligatorios().stream().filter(r -> !r.cumplido())
					.map(Requisito::texto).toList();
			if (!faltantes.isEmpty()) {
				throw ExcepcionNegocio
						.conflicto("Faltan requisitos obligatorios: " + String.join(", ", faltantes) + ".");
			}
		}
		traducir(anio::activar);
		return detalle(anio);
	}

	// PENDIENTE(grupo): sin endpoint todavía (ver activar).
	/** Cierra un año ACTIVO cuando ya venció el plazo de notas de su último periodo. */
	@Transactional
	public AnioEscolarDetalle cerrar(UUID id) {
		AnioEscolar anio = buscar(id);
		traducir(() -> anio.cerrar(hoy()));
		return detalle(anio);
	}

	private AnioEscolar buscar(UUID id) {
		return anios.findById(id).orElseThrow(() -> ExcepcionNegocio.noEncontrado("El año escolar no existe."));
	}

	private AnioEscolar buscarPorPeriodo(UUID periodoId) {
		return anios.findByPeriodosId(periodoId)
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("El periodo académico no existe."));
	}

	private static PeriodoAcademico periodoDe(AnioEscolar anio, UUID periodoId) {
		return anio.getPeriodos().stream().filter(p -> periodoId.equals(p.getId())).findFirst().orElseThrow();
	}

	private AnioEscolarDetalle detalle(AnioEscolar anio) {
		return AnioEscolarDetalle.de(anio, consultas.conteos(anio.getId()), hoy());
	}

	private static void validarFechas(LocalDate fechaInicio, LocalDate fechaFin, Regimen regimen) {
		if (!fechaFin.isAfter(fechaInicio)) {
			throw ExcepcionNegocio.invalido("La fecha de fin debe ser posterior a la fecha de inicio.");
		}
		long dias = ChronoUnit.DAYS.between(fechaInicio, fechaFin) + 1;
		if (dias < AnioEscolar.diasMinimos(regimen)) {
			throw ExcepcionNegocio.invalido(
					"El año escolar es muy corto para dividirlo en " + regimen.cantidadPeriodos() + " periodos.");
		}
	}

	/**
	 * Quita los periodos, hace flush y recién entonces genera los nuevos. Sin el flush, Hibernate inserta antes de
	 * borrar
	 * y choca con uq_periodo_academico_numero. El flush final asigna los ids de los periodos nuevos para la respuesta.
	 */
	private void regenerarPeriodos(AnioEscolar anio, Runnable regenerar) {
		traducir(anio::quitarPeriodos);
		anios.flush();
		traducir(regenerar);
		anios.flush();
	}

	/** Traduce las excepciones del dominio conservando el mensaje: estado no permitido es 409; dato inválido, 400. */
	private static void traducir(Runnable operacion) {
		try {
			operacion.run();
		} catch (IllegalStateException e) {
			throw ExcepcionNegocio.conflicto(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw ExcepcionNegocio.invalido(e.getMessage());
		}
	}

	private RequisitosAnio requisitos(AnioEscolar anio) {
		ConteosAnio c = consultas.conteos(anio.getId());
		List<String> codigos = consultas.codigosEstructuras(anio.getId());
		boolean haySecciones = c.seccionesActivas() > 0;

		Requisito periodos = new Requisito("PERIODOS", "Periodos académicos definidos", c.periodos() > 0,
				c.periodos() > 0
						? c.periodos() + " " + anio.getRegimen().unidadPlural() + " del "
								+ FORMATO_FECHA.format(anio.getFechaInicio()) + " al "
								+ FORMATO_FECHA.format(anio.getFechaFin())
						: "Aún no hay periodos definidos.");
		Requisito secciones = new Requisito("SECCIONES", "Secciones creadas", haySecciones,
				haySecciones
						? cantidad(c.seccionesActivas(), "sección activa", "secciones activas") + " en "
								+ cantidad(c.gradosConSecciones(), "grado", "grados")
						: "Aún no hay secciones activas.");
		Requisito estructura = new Requisito("ESTRUCTURA_HORARIA", "Estructura horaria registrada", !codigos.isEmpty(),
				codigos.isEmpty() ? "Aún no hay estructuras horarias." : String.join(" · ", codigos));
		Requisito tutores = new Requisito("TUTORES", "Tutores asignados (recomendado)",
				haySecciones && c.seccionesSinTutor() == 0, detalleTutores(c));
		boolean planCompleto = c.gradosConSecciones() > 0 && c.gradosConSeccionesConPlan() == c.gradosConSecciones();
		Requisito plan = new Requisito("PLAN_EVALUACION", "Plan de evaluación por grado (recomendado)", planCompleto,
				c.gradosConSecciones() == 0
						? "Aún no hay secciones activas."
						: c.gradosConSeccionesConPlan() + " de " + cantidad(c.gradosConSecciones(), "grado", "grados")
								+ " con cursos asignados");

		List<Requisito> obligatorios = List.of(periodos, secciones, estructura);
		return new RequisitosAnio(obligatorios.stream().allMatch(Requisito::cumplido), obligatorios,
				List.of(tutores, plan));
	}

	private static String detalleTutores(ConteosAnio c) {
		if (c.seccionesActivas() == 0) {
			return "Aún no hay secciones activas.";
		}
		if (c.seccionesSinTutor() == 0) {
			return "Todas las secciones tienen tutor.";
		}
		return c.seccionesSinTutor() + " de " + cantidad(c.seccionesActivas(), "sección", "secciones")
				+ (c.seccionesSinTutor() == 1 ? " aún no tiene tutor" : " aún no tienen tutor");
	}

	private static String cantidad(int n, String singular, String plural) {
		return n + " " + (n == 1 ? singular : plural);
	}
}
