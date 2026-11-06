package com.galiledu.matricula.aplicacion;

import static com.galiledu.matricula.aplicacion.PresentacionMatricula.etiqueta;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galiledu.compartido.web.ExcepcionNegocio;
import com.galiledu.compartido.web.Pagina;
import com.galiledu.configuracion.aplicacion.DtosSecciones.SeccionConVacantes;
import com.galiledu.configuracion.aplicacion.ServicioPeriodoMatricula;
import com.galiledu.configuracion.aplicacion.ServicioSecciones;
import com.galiledu.configuracion.dominio.EstadoVisiblePeriodoMatricula;
import com.galiledu.matricula.aplicacion.DtosMatricula.ArchivoMatricula;
import com.galiledu.matricula.aplicacion.DtosMatricula.ArchivoParaVer;
import com.galiledu.matricula.aplicacion.DtosMatricula.DocumentoPedido;
import com.galiledu.matricula.aplicacion.DtosMatricula.EstudianteSinMatricula;
import com.galiledu.matricula.aplicacion.DtosMatricula.FilaMatriculaPeriodo;
import com.galiledu.matricula.aplicacion.DtosMatricula.IndicadoresPeriodo;
import com.galiledu.matricula.aplicacion.DtosMatricula.MatriculaCreada;
import com.galiledu.matricula.aplicacion.DtosMatricula.PeriodoMatriculas;
import com.galiledu.matricula.aplicacion.DtosMatricula.PreparacionMatricula;
import com.galiledu.matricula.aplicacion.DtosMatricula.SeccionAsignada;
import com.galiledu.matricula.aplicacion.DtosMatricula.SeccionesDelGrado;
import com.galiledu.matricula.infraestructura.ConsultasMatricula;
import com.galiledu.matricula.infraestructura.ConsultasMatricula.EstudianteParaMatricula;
import com.galiledu.matricula.infraestructura.ConsultasMatricula.PaginaFilas;
import com.galiledu.matricula.infraestructura.ConsultasMatricula.PeriodoPorId;
import com.galiledu.matricula.infraestructura.EscriturasMatricula;

/**
 * HU-24: pantalla "Matrículas del periodo" del Personal administrativo y su "Nueva matrícula". El estado del periodo
 * (PROGRAMADO, ABIERTO o CERRADO) lo calcula Configuración y las secciones con sus vacantes también
 * ({@link ServicioSecciones}); aquí solo se leen y se crean matrículas.
 */
@Service
public class ServicioMatriculasPeriodo {
	private final ConsultasMatricula consultas;
	private final EscriturasMatricula escrituras;
	private final ServicioPeriodoMatricula periodos;
	private final ServicioSecciones secciones;
	private final Clock reloj;

	public ServicioMatriculasPeriodo(ConsultasMatricula consultas, EscriturasMatricula escrituras,
			ServicioPeriodoMatricula periodos, ServicioSecciones secciones, Clock reloj) {
		this.consultas = consultas;
		this.escrituras = escrituras;
		this.periodos = periodos;
		this.secciones = secciones;
		this.reloj = reloj;
	}

	/** Aviso del periodo e indicadores. 404 si el periodo no existe. */
	@Transactional(readOnly = true)
	public PeriodoMatriculas periodo(UUID periodoId) {
		PeriodoPorId periodo = buscar(periodoId);
		Map<String, Integer> porEstado = consultas.contarPorEstado(periodo.anioEscolarId());
		int total = porEstado.values().stream().mapToInt(Integer::intValue).sum();
		int matriculados = porEstado.getOrDefault("MATRICULADO", 0);
		int requierenAccion = porEstado.getOrDefault("SECCION_RESERVADA", 0)
				+ porEstado.getOrDefault("EN_VALIDACION", 0);
		return new PeriodoMatriculas(periodo.id(), periodo.anio(), periodo.inicio(), periodo.cierre(),
				periodos.estadoEn(periodo.id(), reloj.instant()).name(), periodo.permiteExtemporanea(),
				new IndicadoresPeriodo(total, matriculados, total - matriculados, requierenAccion));
	}

	/** Tabla de A3: solo los estudiantes que ya tienen matrícula en el año del periodo. */
	@Transactional(readOnly = true)
	public Pagina<FilaMatriculaPeriodo> listar(UUID periodoId, String texto, String gradoCodigo, String estado,
			String orden, boolean descendente, int pagina, int tamano) {
		PeriodoPorId periodo = buscar(periodoId);
		int tam = Math.clamp(tamano, 1, 100);
		int numero = Math.max(1, pagina);
		PaginaFilas<ConsultasMatricula.FilaListado> filas = consultas.listar(periodo.anioEscolarId(), texto,
				vacioANulo(gradoCodigo), vacioANulo(estado), orden, descendente, numero, tam);
		return Pagina.de(new PageImpl<>(filas.filas(), PageRequest.of(numero - 1, tam), filas.total()),
				f -> new FilaMatriculaPeriodo(f.id(), f.estudianteId(), f.nombre(), f.documento(), f.gradoCodigo(),
						f.grado(), f.seccion(), f.estado(), f.codigo(), f.tipo()));
	}

	/** Buscador de "Nueva matrícula": inscritos para el año del periodo que todavía no tienen matrícula. */
	@Transactional(readOnly = true)
	public List<EstudianteSinMatricula> buscarEstudiantes(UUID periodoId, String texto) {
		PeriodoPorId periodo = buscar(periodoId);
		return consultas.estudiantesSinMatricula(periodo.anio(), periodo.anioEscolarId(), texto, 8).stream()
				.map(e -> new EstudianteSinMatricula(e.id(), e.nombre(), e.documento(), e.gradoCodigo(), e.grado()))
				.toList();
	}

	/** Lo que muestra el modal al elegir al alumno: sus datos, la sección que se le asignaría y los documentos. */
	@Transactional(readOnly = true)
	public PreparacionMatricula preparar(UUID periodoId, UUID estudianteId) {
		PeriodoPorId periodo = buscar(periodoId);
		EstudianteParaMatricula e = estudiante(periodo, estudianteId);
		var apoderado = consultas.responsableDe(estudianteId);
		var emergencia = consultas.contactoDeEmergencia(estudianteId);
		return new PreparacionMatricula(e.id(), e.nombre(), e.documento(), e.gradoCodigo(), e.grado(),
				tipo(e.id(), periodo.anio()),
				primeraConCupo(periodo, e.gradoCodigo())
						.map(s -> new SeccionAsignada(s.id(), etiqueta(s), s.tutor(), s.capacidadMaxima(),
								s.alumnosOcupanVacante()))
						.orElse(null),
				apoderado.map(a -> a.nombre()).orElse(null),
				apoderado.map(a -> PresentacionMatricula.unir(a.celular(), a.correo())).orElse(null),
				emergencia.map(c -> c.nombre() + " (" + c.parentesco() + ")"
						+ (c.celular() == null ? "" : " · " + c.celular())).orElse(null),
				PresentacionMatricula.resumenMedico(consultas.fichaMedica(estudianteId)),
				consultas.documentosRequeridosDe(periodoId).stream()
						.map(d -> new DocumentoPedido(d.id(), d.nombre(), d.descripcion(), d.tienePlantilla()))
						.toList());
	}

	/** Secciones del grado del alumno para el modal "Cambiar sección" de Nueva matrícula (la propuesta va marcada). */
	@Transactional(readOnly = true)
	public SeccionesDelGrado seccionesDelAlumno(UUID periodoId, UUID estudianteId) {
		PeriodoPorId periodo = buscar(periodoId);
		EstudianteParaMatricula e = estudiante(periodo, estudianteId);
		return new SeccionesDelGrado(e.grado(),
				primeraConCupo(periodo, e.gradoCodigo()).map(SeccionConVacantes::id).orElse(null),
				delGrado(periodo.anioEscolarId(), e.gradoCodigo()).stream().map(PresentacionMatricula::opcion)
						.toList());
	}

	/**
	 * Crea la matrícula en SECCION_RESERVADA y guarda los documentos que ya se subieron (cada uno queda EN_REVISION).
	 * Si se eligió una sección y tiene cupo, va a esa; si está llena (o no se eligió), a la primera del grado con
	 * cupo en orden alfabético, sin impedir la matrícula (HU-24). Solo con el periodo abierto.
	 */
	@Transactional
	public MatriculaCreada crear(UUID periodoId, UUID estudianteId, UUID seccionElegidaId,
			Map<UUID, ArchivoMatricula> archivos, UUID usuarioId) {
		PeriodoPorId periodo = buscar(periodoId);
		if (periodos.estadoEn(periodoId, reloj.instant()) != EstadoVisiblePeriodoMatricula.ABIERTO) {
			throw ExcepcionNegocio.conflicto("El periodo de matrícula " + periodo.anio()
					+ " no está abierto: no se pueden asignar secciones de matrícula regular.");
		}
		EstudianteParaMatricula e = estudiante(periodo, estudianteId);
		validarArchivos(periodoId, archivos);
		escrituras.bloquearSeccionesDelGrado(periodo.anioEscolarId(), e.gradoCodigo());
		List<SeccionConVacantes> delGrado = delGrado(periodo.anioEscolarId(), e.gradoCodigo());
		if (seccionElegidaId != null && delGrado.stream().noneMatch(s -> s.id().equals(seccionElegidaId))) {
			throw ExcepcionNegocio.invalido("La sección elegida no es de " + e.grado() + ".");
		}
		Optional<SeccionConVacantes> elegida = delGrado.stream().filter(s -> s.id().equals(seccionElegidaId))
				.filter(this::hayCupo).findFirst();
		SeccionConVacantes seccion = elegida.or(() -> primeraConCupo(delGrado)).orElseThrow(
				() -> ExcepcionNegocio.conflicto("No hay vacantes en ninguna sección de " + e.grado() + "."));
		UUID matriculaId = escrituras.insertarMatricula(e.id(), periodo.anioEscolarId(), e.gradoCodigo(),
				seccion.id(), tipo(e.id(), periodo.anio()), usuarioId);
		archivos.forEach(
				(documentoId, archivo) -> escrituras.insertarDocumento(matriculaId, documentoId, archivo, usuarioId));
		boolean reasignada = seccionElegidaId != null && !seccion.id().equals(seccionElegidaId);
		// Con todos los documentos cargados por el colegio, la matrícula nace lista para validar.
		int requeridos = consultas.documentosRequeridos(periodoId);
		boolean completa = requeridos > 0 && consultas.documentosEntregados(matriculaId) >= requeridos
				&& escrituras.marcarEnValidacion(matriculaId);
		return new MatriculaCreada(matriculaId, completa ? "EN_VALIDACION" : "SECCION_RESERVADA", etiqueta(seccion),
				archivos.size(), reasignada);
	}

	private EstudianteParaMatricula estudiante(PeriodoPorId periodo, UUID estudianteId) {
		EstudianteParaMatricula e = consultas.estudianteParaMatricula(estudianteId)
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("No existe el estudiante o no está activo."));
		if (e.anioAcademico() != periodo.anio()) {
			throw ExcepcionNegocio.invalido("El estudiante está inscrito para " + e.anioAcademico()
					+ ", no para el periodo " + periodo.anio() + ".");
		}
		if (consultas.tieneMatricula(estudianteId, periodo.anioEscolarId())) {
			throw ExcepcionNegocio.conflicto("El estudiante ya tiene una matrícula en " + periodo.anio() + ".");
		}
		return e;
	}

	/** Secciones activas del grado, en orden alfabético. */
	private List<SeccionConVacantes> delGrado(UUID anioEscolarId, String gradoCodigo) {
		return secciones.listarSeccionesActivasConVacantes(anioEscolarId).stream()
				.filter(s -> s.gradoCodigo().equals(gradoCodigo))
				.sorted(Comparator.comparing(SeccionConVacantes::nombre))
				.toList();
	}

	private boolean hayCupo(SeccionConVacantes s) {
		return s.alumnosOcupanVacante() < s.capacidadMaxima();
	}

	private Optional<SeccionConVacantes> primeraConCupo(List<SeccionConVacantes> delGrado) {
		return delGrado.stream().filter(this::hayCupo).findFirst();
	}

	private Optional<SeccionConVacantes> primeraConCupo(PeriodoPorId periodo, String gradoCodigo) {
		return primeraConCupo(delGrado(periodo.anioEscolarId(), gradoCodigo));
	}

	private String tipo(UUID estudianteId, int anio) {
		return consultas.fueMatriculadoAntes(estudianteId, anio) ? "CONTINUADOR" : "NUEVO";
	}

	/** Cada archivo debe ser de un documento que pide el periodo y ser PDF, JPG o PNG de hasta 10 MB (RF-62). */
	private void validarArchivos(UUID periodoId, Map<UUID, ArchivoMatricula> archivos) {
		Set<UUID> pedidos = consultas.documentosRequeridosDe(periodoId).stream().map(d -> d.id())
				.collect(Collectors.toSet());
		archivos.forEach((documentoId, archivo) -> {
			if (!pedidos.contains(documentoId)) {
				throw ExcepcionNegocio.invalido("Ese documento no lo pide este periodo de matrícula.");
			}
			PresentacionMatricula.validarArchivo(archivo);
		});
	}

	/** La plantilla de un documento del periodo, para descargarla (HU-28). */
	@Transactional(readOnly = true)
	public ArchivoParaVer plantilla(UUID periodoId, UUID documentoId) {
		buscar(periodoId);
		var plantilla = periodos.descargarPlantilla(periodoId, documentoId);
		return new ArchivoParaVer(plantilla.nombreArchivo(), plantilla.tipoMime(), plantilla.contenido());
	}

	private PeriodoPorId buscar(UUID periodoId) {
		return consultas.periodoPorId(periodoId)
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("No existe el periodo de matrícula."));
	}

	private static String vacioANulo(String valor) {
		return valor == null || valor.isBlank() ? null : valor;
	}
}
