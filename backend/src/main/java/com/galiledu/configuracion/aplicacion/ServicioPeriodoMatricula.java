package com.galiledu.configuracion.aplicacion;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galiledu.compartido.web.ExcepcionNegocio;
import com.galiledu.compartido.web.Pagina;
import com.galiledu.configuracion.aplicacion.DtosPeriodoMatricula.AnioDisponible;
import com.galiledu.configuracion.aplicacion.DtosPeriodoMatricula.DocumentoSolicitud;
import com.galiledu.configuracion.aplicacion.DtosPeriodoMatricula.DocumentoVista;
import com.galiledu.configuracion.aplicacion.DtosPeriodoMatricula.FilaPeriodoMatricula;
import com.galiledu.configuracion.aplicacion.DtosPeriodoMatricula.FormularioPeriodoMatricula;
import com.galiledu.configuracion.aplicacion.DtosPeriodoMatricula.PeriodoVigente;
import com.galiledu.configuracion.aplicacion.DtosPeriodoMatricula.SeccionVacante;
import com.galiledu.configuracion.aplicacion.DtosPeriodoMatricula.SolicitudPeriodoMatricula;
import com.galiledu.configuracion.aplicacion.DtosPeriodoMatricula.VacanteSeccion;
import com.galiledu.configuracion.aplicacion.DtosSecciones.SeccionConVacantes;
import com.galiledu.configuracion.aplicacion.DtosSecciones.SolicitudCapacidad;
import com.galiledu.configuracion.dominio.AnioEscolar;
import com.galiledu.configuracion.dominio.DatosDocumento;
import com.galiledu.configuracion.dominio.DocumentoRequerido;
import com.galiledu.configuracion.dominio.EstadoAnioEscolar;
import com.galiledu.configuracion.dominio.EstadoPeriodoMatricula;
import com.galiledu.configuracion.dominio.EstadoVisiblePeriodoMatricula;
import com.galiledu.configuracion.dominio.PeriodoMatricula;
import com.galiledu.configuracion.dominio.PlantillaDocumento;
import com.galiledu.configuracion.dominio.PlantillaDocumento.Validada;
import com.galiledu.configuracion.dominio.Seccion;
import com.galiledu.configuracion.infraestructura.AnioEscolarRepositorio;
import com.galiledu.configuracion.infraestructura.ConsultasPeriodoMatricula;
import com.galiledu.configuracion.infraestructura.ConsultasPeriodoMatricula.PlantillaGuardada;
import com.galiledu.configuracion.infraestructura.PeriodoMatriculaRepositorio;

/**
 * HU-23 (y HU-21/22): periodo de matrícula regular de cada año escolar, con sus vacantes por sección y sus documentos
 * requeridos (pantalla 13). "Ahora" sale del {@link Clock}. El dominio lanza IllegalStateException (409) e
 * IllegalArgumentException (400) con el mensaje que se muestra.
 *
 * <p>
 * Crear y editar validan todo antes de escribir (fechas, documentos, tarifario, secciones y vacantes) y escriben en una
 * sola transacción: el periodo, los documentos y {@link ServicioSecciones#cambiarCapacidad} por cada sección. Un fallo
 * inesperado a mitad de camino lo deshace {@code @Transactional}.
 *
 * <p>
 * Para Matrícula (sin endpoint): {@link #vigente()} y {@link #estadoEn(UUID, Instant)}.
 */
@Service
public class ServicioPeriodoMatricula {
	private final PeriodoMatriculaRepositorio periodos;
	private final AnioEscolarRepositorio anios;
	private final ConsultasPeriodoMatricula consultas;
	private final ServicioSecciones secciones;
	private final ServicioTarifario tarifarios;
	private final EntityManager entityManager;
	private final Clock reloj;

	public ServicioPeriodoMatricula(PeriodoMatriculaRepositorio periodos, AnioEscolarRepositorio anios,
			ConsultasPeriodoMatricula consultas, ServicioSecciones secciones, ServicioTarifario tarifarios,
			EntityManager entityManager, Clock reloj) {
		this.periodos = periodos;
		this.anios = anios;
		this.consultas = consultas;
		this.secciones = secciones;
		this.tarifarios = tarifarios;
		this.entityManager = entityManager;
		this.reloj = reloj;
	}

	// ---------- Consultas ----------

	/**
	 * Listado de la pantalla 13, del periodo más reciente al más antiguo, con el estado calculado ahora. Los filtros
	 * (año y estado visible) son opcionales; {@code pagina} empieza en 1.
	 */
	@Transactional(readOnly = true)
	public Pagina<FilaPeriodoMatricula> listar(Integer anio, EstadoVisiblePeriodoMatricula estado, int pagina,
			int tamano) {
		Instant ahora = reloj.instant();
		List<PeriodoMatricula> todos = periodos.findAllByOrderByInicioDesc();
		Map<UUID, AnioEscolar> aniosPorId = aniosDe(todos);
		Map<UUID, Integer> matriculas = consultas.matriculasPorAnio();
		UUID vigente = elVigente(todos, ahora).map(PeriodoMatricula::getId).orElse(null);

		List<FilaPeriodoMatricula> filas = todos.stream()
				.filter(p -> anio == null || aniosPorId.get(p.getAnioEscolarId()).getAnio() == anio)
				.filter(p -> estado == null || p.estadoEn(ahora) == estado)
				.map(p -> {
					int numeroAnio = aniosPorId.get(p.getAnioEscolarId()).getAnio();
					return new FilaPeriodoMatricula(p.getId(), titulo(numeroAnio), p.getAnioEscolarId(), numeroAnio,
							p.getInicio(), p.getCierre(), matriculas.getOrDefault(p.getAnioEscolarId(), 0),
							p.estadoEn(ahora), p.getId().equals(vigente));
				}).toList();

		int tam = Math.clamp(tamano, 1, 100);
		int desde = Math.min(Math.max(0, pagina - 1) * tam, filas.size());
		List<FilaPeriodoMatricula> delaPagina = filas.subList(desde, Math.min(desde + tam, filas.size()));
		return Pagina.de(new PageImpl<>(delaPagina, PageRequest.of(Math.max(0, pagina - 1), tam), filas.size()),
				f -> f);
	}

	/** El periodo ABIERTO ahora o, si no hay, el próximo PROGRAMADO. Vacío si no hay ninguno. */
	@Transactional(readOnly = true)
	public Optional<PeriodoVigente> vigente() {
		Instant ahora = reloj.instant();
		return elVigente(periodos.findAll(), ahora).map(p -> {
			AnioEscolar anio = buscarAnio(p.getAnioEscolarId());
			List<SeccionConVacantes> activas = secciones.listarSeccionesActivasConVacantes(anio.getId());
			return new PeriodoVigente(p.getId(), anio.getId(), anio.getAnio(), titulo(anio.getAnio()), p.getInicio(),
					p.getCierre(), p.estadoEn(ahora), p.isBloquearPorMorosidad(), p.isPermitirExtemporanea(),
					vacantesTotales(activas), consultas.matriculasDelAnio(anio.getId()));
		});
	}

	/** Para Matrícula: el estado visible del periodo en el instante indicado. 404 si el periodo no existe. */
	@Transactional(readOnly = true)
	public EstadoVisiblePeriodoMatricula estadoEn(UUID periodoId, Instant ahora) {
		return buscar(periodoId).estadoEn(ahora);
	}

	/** Años PLANIFICADO o ACTIVO que todavía no tienen periodo: las opciones de "Año académico". */
	@Transactional(readOnly = true)
	public List<AnioDisponible> aniosDisponibles() {
		return consultas.aniosSinPeriodo().stream().map(a -> new AnioDisponible(a.id(), a.anio(), a.estado()))
				.toList();
	}

	/**
	 * El formulario vacío de "Nuevo periodo de matrícula" para un año: sus secciones con su capacidad, el monto y si el
	 * tarifario está confirmado. 409 si el año no admite periodo o ya tiene uno.
	 */
	@Transactional(readOnly = true)
	public FormularioPeriodoMatricula formularioNuevo(UUID anioEscolarId) {
		AnioEscolar anio = buscarAnio(anioEscolarId);
		exigirAnioHabilitable(anio);
		return formulario(anio, null, reloj.instant());
	}

	/** El formulario para editar (PROGRAMADO) o en solo lectura (ABIERTO y CERRADO), con el resumen del modal. */
	@Transactional(readOnly = true)
	public FormularioPeriodoMatricula ver(UUID periodoId) {
		PeriodoMatricula periodo = buscar(periodoId);
		return formulario(buscarAnio(periodo.getAnioEscolarId()), periodo, reloj.instant());
	}

	// ---------- Cambios ----------

	/** "Guardar configuración" de un periodo nuevo: lo deja HABILITADO, con sus documentos y las vacantes pedidas. */
	@Transactional
	public FormularioPeriodoMatricula crear(SolicitudPeriodoMatricula solicitud) {
		Instant ahora = reloj.instant();
		if (solicitud.anioEscolarId() == null) {
			throw ExcepcionNegocio.invalido("Elige el año académico.");
		}
		AnioEscolar anio = buscarAnio(solicitud.anioEscolarId());
		exigirAnioHabilitable(anio);
		LocalDateTime inicio = enHoraLocal(solicitud.fechaInicio(), solicitud.horaInicio());
		LocalDateTime cierre = enHoraLocal(solicitud.fechaCierre(), solicitud.horaCierre());
		List<DatosDocumento> documentos = documentosDe(solicitud);
		traducir(() -> PeriodoMatricula.validar(inicio, cierre, documentos, ahora));
		exigirSinOtroPeriodoAbierto(null, ahora);
		List<SeccionConVacantes> activas = exigirTarifarioYSecciones(anio);
		validarVacantes(anio, activas, solicitud.vacantes());

		// Todo es válido: recién ahora se escribe.
		cerrarHabilitadosVencidos(null, ahora);
		PeriodoMatricula nuevo = periodos
				.saveAndFlush(PeriodoMatricula.habilitar(anio.getId(), inicio, cierre, documentos, ahora));
		aplicarVacantes(solicitud.vacantes());
		return formulario(anio, nuevo, ahora);
	}

	/** Edita un periodo PROGRAMADO: fechas, documentos y vacantes. En ABIERTO y CERRADO responde 409. */
	@Transactional
	public FormularioPeriodoMatricula editar(UUID periodoId, SolicitudPeriodoMatricula solicitud) {
		Instant ahora = reloj.instant();
		PeriodoMatricula periodo = buscar(periodoId);
		AnioEscolar anio = buscarAnio(periodo.getAnioEscolarId());
		traducir(() -> periodo.exigirEditable(ahora));
		LocalDateTime inicio = enHoraLocal(solicitud.fechaInicio(), solicitud.horaInicio());
		LocalDateTime cierre = enHoraLocal(solicitud.fechaCierre(), solicitud.horaCierre());
		List<DatosDocumento> documentos = documentosDe(solicitud);
		traducir(() -> PeriodoMatricula.validar(inicio, cierre, documentos, ahora));
		exigirDocumentosDelPeriodo(periodo, documentos);
		exigirSinOtroPeriodoAbierto(periodo.getId(), ahora);
		List<SeccionConVacantes> activas = exigirTarifarioYSecciones(anio);
		validarVacantes(anio, activas, solicitud.vacantes());
		exigirQuitarSoloDocumentosSinEntregas(periodo, documentos);

		// Todo es válido: recién ahora se escribe. Quitar, flush y recién entonces agregar (uq_documento_requerido_nombre).
		cerrarHabilitadosVencidos(periodo.getId(), ahora);
		periodo.retirarDocumentosAusentes(documentos);
		periodos.flush();
		traducir(() -> periodo.editar(inicio, cierre, documentos, ahora));
		periodos.saveAndFlush(periodo);
		aplicarVacantes(solicitud.vacantes());
		return formulario(anio, periodo, ahora);
	}

	/** Elimina un periodo PROGRAMADO. 409 si el año tiene cualquier matrícula o si ya hay documentos entregados. */
	@Transactional
	public void eliminar(UUID periodoId) {
		PeriodoMatricula periodo = buscar(periodoId);
		AnioEscolar anio = buscarAnio(periodo.getAnioEscolarId());
		traducir(() -> periodo.exigirEliminable(reloj.instant()));
		if (consultas.tieneMatriculas(anio.getId())) {
			throw ExcepcionNegocio
					.conflicto("No se puede eliminar el periodo de matrícula: el año " + anio.getAnio()
							+ " ya tiene matrículas.");
		}
		if (consultas.tieneEntregas(periodo.getId())) {
			throw ExcepcionNegocio
					.conflicto("No se puede eliminar el periodo de matrícula: ya hay documentos entregados.");
		}
		periodos.delete(periodo);
		periodos.flush();
	}

	// ---------- Plantillas ----------

	/**
	 * Sube o reemplaza la plantilla de un documento. Solo con el periodo PROGRAMADO; XLSX, XLS o CSV de hasta 10 MB.
	 */
	@Transactional
	public DocumentoVista subirPlantilla(UUID periodoId, UUID documentoId, String nombreArchivo, byte[] contenido) {
		PeriodoMatricula periodo = buscar(periodoId);
		DocumentoRequerido documento = buscarDocumento(periodo, documentoId);
		exigirPlantillaModificable(periodo);
		Validada plantilla = traducir(() -> PlantillaDocumento.validar(nombreArchivo, contenido));
		consultas.guardarPlantilla(documentoId, plantilla.nombreArchivo(), plantilla.tipoMime(),
				plantilla.contenido());
		entityManager.refresh(documento);
		return vistaDe(documento, consultas.documentosConEntregas(periodoId));
	}

	/** La plantilla de un documento, en cualquier estado del periodo (la vista de solo lectura la muestra). */
	@Transactional(readOnly = true)
	public PlantillaGuardada descargarPlantilla(UUID periodoId, UUID documentoId) {
		buscarDocumento(buscar(periodoId), documentoId);
		return consultas.plantilla(documentoId)
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("El documento no tiene plantilla."));
	}

	/** Quita la plantilla de un documento. Solo con el periodo PROGRAMADO. */
	@Transactional
	public DocumentoVista quitarPlantilla(UUID periodoId, UUID documentoId) {
		PeriodoMatricula periodo = buscar(periodoId);
		DocumentoRequerido documento = buscarDocumento(periodo, documentoId);
		exigirPlantillaModificable(periodo);
		if (consultas.plantilla(documentoId).isEmpty()) {
			throw ExcepcionNegocio.noEncontrado("El documento no tiene plantilla.");
		}
		consultas.quitarPlantilla(documentoId);
		entityManager.refresh(documento);
		return vistaDe(documento, consultas.documentosConEntregas(periodoId));
	}

	private void exigirPlantillaModificable(PeriodoMatricula periodo) {
		if (periodo.estadoEn(reloj.instant()) != EstadoVisiblePeriodoMatricula.PROGRAMADO) {
			throw ExcepcionNegocio.conflicto("Solo se puede cambiar la plantilla de un periodo programado.");
		}
	}

	// ---------- Validaciones previas a escribir ----------

	/** El año debe estar PLANIFICADO o ACTIVO y todavía no tener periodo (hay uno por año). */
	private void exigirAnioHabilitable(AnioEscolar anio) {
		if (anio.getEstado() == EstadoAnioEscolar.CERRADO) {
			throw ExcepcionNegocio.conflicto("Solo se puede habilitar la matrícula de un año planificado o activo.");
		}
		if (periodos.findByAnioEscolarId(anio.getId()).isPresent()) {
			throw ExcepcionNegocio
					.conflicto("Ya existe un periodo de matrícula para el año " + anio.getAnio() + ".");
		}
	}

	/** Un periodo por año, y ninguno más sin cerrar (PROGRAMADO o ABIERTO): así Matrícula nunca ve dos a la vez. */
	private void exigirSinOtroPeriodoAbierto(UUID excepto, Instant ahora) {
		for (PeriodoMatricula otro : periodos.findAll()) {
			if (!otro.getId().equals(excepto) && otro.estadoEn(ahora) != EstadoVisiblePeriodoMatricula.CERRADO) {
				throw ExcepcionNegocio.conflicto("Ya existe un periodo de matrícula "
						+ buscarAnio(otro.getAnioEscolarId()).getAnio() + " sin cerrar.");
			}
		}
	}

	/** El tarifario del año debe estar CONFIRMADO y el año debe tener secciones activas. Devuelve las secciones. */
	private List<SeccionConVacantes> exigirTarifarioYSecciones(AnioEscolar anio) {
		if (tarifarios.vigente(anio.getId()).isEmpty()) {
			throw ExcepcionNegocio.conflicto(
					"Confirma el tarifario " + anio.getAnio() + " antes de habilitar la matrícula.");
		}
		List<SeccionConVacantes> activas = secciones.listarSeccionesActivasConVacantes(anio.getId());
		if (activas.isEmpty()) {
			throw ExcepcionNegocio
					.invalido("Crea las secciones de " + anio.getAnio() + " antes de habilitar la matrícula.");
		}
		return activas;
	}

	/**
	 * Cada vacante debe ser de una sección activa del año, sin repetirse, de 1 a 45 y no menor que los alumnos que
	 * ocupan vacante (RF-50). Las secciones que no vienen conservan su capacidad.
	 */
	private static void validarVacantes(AnioEscolar anio, List<SeccionConVacantes> activas,
			List<VacanteSeccion> vacantes) {
		if (vacantes == null) {
			return;
		}
		Map<UUID, SeccionConVacantes> porId = activas.stream()
				.collect(Collectors.toMap(SeccionConVacantes::id, Function.identity()));
		Set<UUID> vistas = new HashSet<>();
		for (VacanteSeccion v : vacantes) {
			if (v.seccionId() == null) {
				throw ExcepcionNegocio.invalido("Indica la sección de cada vacante.");
			}
			SeccionConVacantes seccion = porId.get(v.seccionId());
			if (seccion == null) {
				throw ExcepcionNegocio
						.invalido("La sección no pertenece a las secciones activas de " + anio.getAnio() + ".");
			}
			String etiqueta = EtiquetasSecciones.etiqueta(seccion.gradoCodigo(), seccion.nombre());
			if (!vistas.add(v.seccionId())) {
				throw ExcepcionNegocio.invalido("La sección " + etiqueta + " está repetida.");
			}
			if (v.capacidad() == null) {
				throw ExcepcionNegocio.invalido("Ingresa las vacantes de la sección " + etiqueta + ".");
			}
			if (v.capacidad() < Seccion.CAPACIDAD_MINIMA || v.capacidad() > Seccion.CAPACIDAD_MAXIMA) {
				throw ExcepcionNegocio.invalido("Las vacantes de la sección " + etiqueta + " deben estar entre "
						+ Seccion.CAPACIDAD_MINIMA + " y " + Seccion.CAPACIDAD_MAXIMA + ".");
			}
			if (v.capacidad() < seccion.alumnosOcupanVacante()) {
				throw ExcepcionNegocio.invalido("Las vacantes de la sección " + etiqueta
						+ " no pueden ser menos que los alumnos que ocupan vacante (" + seccion.alumnosOcupanVacante()
						+ ").");
			}
		}
	}

	private static void exigirDocumentosDelPeriodo(PeriodoMatricula periodo, List<DatosDocumento> documentos) {
		Set<UUID> propios = periodo.getDocumentos().stream().map(DocumentoRequerido::getId)
				.collect(Collectors.toSet());
		for (DatosDocumento d : documentos) {
			if (d.id() != null && !propios.contains(d.id())) {
				throw ExcepcionNegocio.invalido("El documento no pertenece a este periodo.");
			}
		}
	}

	/** Un documento que ya tiene entregas no se puede quitar de la lista. */
	private void exigirQuitarSoloDocumentosSinEntregas(PeriodoMatricula periodo, List<DatosDocumento> documentos) {
		Set<UUID> conservados = documentos.stream().map(DatosDocumento::id).filter(java.util.Objects::nonNull)
				.collect(Collectors.toSet());
		Set<UUID> conEntregas = consultas.documentosConEntregas(periodo.getId());
		for (DocumentoRequerido d : periodo.getDocumentos()) {
			if (!conservados.contains(d.getId()) && conEntregas.contains(d.getId())) {
				throw ExcepcionNegocio
						.conflicto("No se puede quitar el documento \"" + d.getNombre() + "\": ya tiene entregas.");
			}
		}
	}

	// ---------- Escrituras ----------

	/** Para que nunca haya dos HABILITADO vigentes: marca CERRADO los que ya pasaron su cierre. */
	private void cerrarHabilitadosVencidos(UUID excepto, Instant ahora) {
		periodos.findByEstado(EstadoPeriodoMatricula.HABILITADO).stream()
				.filter(p -> !p.getId().equals(excepto) && p.estaHabilitadoYVencido(ahora))
				.forEach(PeriodoMatricula::cerrar);
	}

	/** Una llamada a HU-75 por sección, dentro de la misma transacción. */
	private void aplicarVacantes(List<VacanteSeccion> vacantes) {
		if (vacantes == null) {
			return;
		}
		for (VacanteSeccion v : vacantes) {
			secciones.cambiarCapacidad(v.seccionId(), new SolicitudCapacidad(v.capacidad()));
		}
	}

	// ---------- Armado de respuestas ----------

	private FormularioPeriodoMatricula formulario(AnioEscolar anio, PeriodoMatricula periodo, Instant ahora) {
		List<SeccionConVacantes> activas = secciones.listarSeccionesActivasConVacantes(anio.getId());
		Optional<java.math.BigDecimal> monto = tarifarios.vigente(anio.getId())
				.map(t -> t.montoMatricula());
		int matriculas = consultas.matriculasDelAnio(anio.getId());
		EstadoVisiblePeriodoMatricula estado = periodo == null ? null : periodo.estadoEn(ahora);
		boolean editable = periodo == null || estado == EstadoVisiblePeriodoMatricula.PROGRAMADO;
		boolean eliminable = periodo != null && estado == EstadoVisiblePeriodoMatricula.PROGRAMADO
				&& !consultas.tieneMatriculas(anio.getId()) && !consultas.tieneEntregas(periodo.getId());

		List<SeccionVacante> filas = activas.stream()
				.map(s -> new SeccionVacante(s.id(), EtiquetasSecciones.etiqueta(s.gradoCodigo(), s.nombre()),
						s.gradoCodigo(), s.gradoNombre(), s.tutor(), s.capacidadMaxima(), s.alumnosOcupanVacante()))
				.toList();
		List<DocumentoVista> documentos = new ArrayList<>();
		if (periodo != null) {
			Set<UUID> conEntregas = consultas.documentosConEntregas(periodo.getId());
			periodo.getDocumentos().forEach(d -> documentos.add(vistaDe(d, conEntregas)));
		}
		LocalDateTime inicio = periodo == null ? null : PeriodoMatricula.aHoraLocal(periodo.getInicio());
		LocalDateTime cierre = periodo == null ? null : PeriodoMatricula.aHoraLocal(periodo.getCierre());
		return new FormularioPeriodoMatricula(periodo == null ? null : periodo.getId(), anio.getId(), anio.getAnio(),
				titulo(anio.getAnio()), estado, editable, eliminable, inicio == null ? null : inicio.toLocalDate(),
				inicio == null ? null : inicio.toLocalTime(), cierre == null ? null : cierre.toLocalDate(),
				cierre == null ? null : cierre.toLocalTime(),
				inicio == null ? null : ChronoUnit.DAYS.between(inicio.toLocalDate(), cierre.toLocalDate()),
				monto.orElse(null), monto.isPresent(), vacantesTotales(activas), matriculas, activas.size(),
				EtiquetasSecciones.resumen(activas), periodo == null || periodo.isBloquearPorMorosidad(),
				periodo == null || periodo.isPermitirExtemporanea(), filas, documentos);
	}

	private static DocumentoVista vistaDe(DocumentoRequerido d, Set<UUID> conEntregas) {
		return new DocumentoVista(d.getId(), d.getNombre(), d.getDescripcion(), d.getOrden(), d.tienePlantilla(),
				d.getPlantillaNombreArchivo(), conEntregas.contains(d.getId()));
	}

	private static int vacantesTotales(List<SeccionConVacantes> activas) {
		return activas.stream().mapToInt(SeccionConVacantes::capacidadMaxima).sum();
	}

	private static String titulo(int anio) {
		return "Matrícula " + anio;
	}

	/** El ABIERTO ahora o, si no hay, el PROGRAMADO que empieza primero. */
	private static Optional<PeriodoMatricula> elVigente(List<PeriodoMatricula> todos, Instant ahora) {
		Optional<PeriodoMatricula> abierto = todos.stream()
				.filter(p -> p.estadoEn(ahora) == EstadoVisiblePeriodoMatricula.ABIERTO).findFirst();
		if (abierto.isPresent()) {
			return abierto;
		}
		return todos.stream().filter(p -> p.estadoEn(ahora) == EstadoVisiblePeriodoMatricula.PROGRAMADO)
				.min(java.util.Comparator.comparing(PeriodoMatricula::getInicio));
	}

	private static LocalDateTime enHoraLocal(java.time.LocalDate fecha, java.time.LocalTime hora) {
		return fecha == null || hora == null ? null : LocalDateTime.of(fecha, hora);
	}

	private static List<DatosDocumento> documentosDe(SolicitudPeriodoMatricula solicitud) {
		List<DocumentoSolicitud> lista = solicitud.documentos() == null ? List.of() : solicitud.documentos();
		return lista.stream().map(d -> new DatosDocumento(d.id(), d.nombre(), d.descripcion())).toList();
	}

	private Map<UUID, AnioEscolar> aniosDe(List<PeriodoMatricula> lista) {
		Set<UUID> ids = lista.stream().map(PeriodoMatricula::getAnioEscolarId).collect(Collectors.toSet());
		return anios.findAllById(ids).stream().collect(Collectors.toMap(AnioEscolar::getId, Function.identity()));
	}

	private PeriodoMatricula buscar(UUID periodoId) {
		return periodos.findById(periodoId)
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("El periodo de matrícula no existe."));
	}

	private AnioEscolar buscarAnio(UUID anioId) {
		return anios.findById(anioId).orElseThrow(() -> ExcepcionNegocio.noEncontrado("El año escolar no existe."));
	}

	private static DocumentoRequerido buscarDocumento(PeriodoMatricula periodo, UUID documentoId) {
		return periodo.getDocumentos().stream().filter(d -> d.getId().equals(documentoId)).findFirst()
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("El documento no existe."));
	}

	/** Traduce las excepciones del dominio conservando el mensaje: estado no permitido es 409; dato inválido, 400. */
	private static void traducir(Runnable operacion) {
		traducir(() -> {
			operacion.run();
			return null;
		});
	}

	private static <T> T traducir(java.util.function.Supplier<T> operacion) {
		try {
			return operacion.get();
		} catch (IllegalStateException e) {
			throw ExcepcionNegocio.conflicto(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw ExcepcionNegocio.invalido(e.getMessage());
		}
	}
}
