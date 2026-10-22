package com.galiledu.configuracion.aplicacion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galiledu.compartido.web.ExcepcionNegocio;
import com.galiledu.configuracion.aplicacion.DtosSecciones.DocenteElegible;
import com.galiledu.configuracion.aplicacion.DtosSecciones.GradoConSecciones;
import com.galiledu.configuracion.aplicacion.DtosSecciones.SeccionConVacantes;
import com.galiledu.configuracion.aplicacion.DtosSecciones.SeccionResumen;
import com.galiledu.configuracion.aplicacion.DtosSecciones.SolicitudCapacidad;
import com.galiledu.configuracion.aplicacion.DtosSecciones.SolicitudSeccion;
import com.galiledu.configuracion.aplicacion.DtosSecciones.SolicitudTutores;
import com.galiledu.configuracion.dominio.AnioEscolar;
import com.galiledu.configuracion.dominio.EstadoSeccion;
import com.galiledu.configuracion.dominio.Grado;
import com.galiledu.configuracion.dominio.Seccion;
import com.galiledu.configuracion.infraestructura.AnioEscolarRepositorio;
import com.galiledu.configuracion.infraestructura.ConsultasSecciones;
import com.galiledu.configuracion.infraestructura.SeccionRepositorio;

/**
 * HU-75: secciones de cada grado del año escolar, con su tutor y cotutor. Las reglas de una sección viven en
 * {@link Seccion}; aquí se decide lo que depende de otros datos (año, grados, docentes, matrículas, otras secciones) y
 * se traducen los errores de dominio: argumento inválido → 400, estado que no lo permite → 409.
 * <p>
 * Para la configuración de matrícula (HU-23) ofrece, sin endpoint, {@link #listarSeccionesActivasConVacantes}. Esa
 * pantalla cambia una capacidad con {@code PUT /configuracion/secciones/{id}}.
 */
@Service
public class ServicioSecciones {
	private static final Set<String> NIVELES_VALIDOS = Set.of("PRIMARIA", "SECUNDARIA");

	/**
	 * Una sección solo se elimina si nunca tuvo una matrícula ni un cambio de sección (RF-49). Contar solo las que
	 * ocupan vacante no alcanza: una matrícula RETIRADO o un cambio de sección que la tiene de origen siguen apuntando
	 * a ella y la base impediría el borrado. Para una sección con historial, lo correcto es inactivarla.
	 */
	private static final String SECCION_CON_HISTORIAL = "La sección tiene matrículas o cambios de sección registrados. Inactívala para que no reciba más alumnos.";

	private final SeccionRepositorio seccionRepositorio;
	private final AnioEscolarRepositorio anioEscolarRepositorio;
	private final ConsultasSecciones consultasSecciones;
	private final DirectorioDocentes directorioDocentes;
	private final MatriculasDeSecciones matriculasDeSecciones;

	public ServicioSecciones(SeccionRepositorio seccionRepositorio, AnioEscolarRepositorio anioEscolarRepositorio,
			ConsultasSecciones consultasSecciones, DirectorioDocentes directorioDocentes,
			MatriculasDeSecciones matriculasDeSecciones) {
		this.seccionRepositorio = seccionRepositorio;
		this.anioEscolarRepositorio = anioEscolarRepositorio;
		this.consultasSecciones = consultasSecciones;
		this.directorioDocentes = directorioDocentes;
		this.matriculasDeSecciones = matriculasDeSecciones;
	}

	// ---------- Consultas ----------

	/** Los grados del nivel pedido (o los 11), también los que no tienen secciones. */
	@Transactional(readOnly = true)
	public List<GradoConSecciones> listarGradosConSecciones(UUID anioId, String nivel) {
		obtenerAnio(anioId);
		String nivelSolicitado = normalizarNivel(nivel);
		List<Seccion> seccionesDelAnio = seccionRepositorio.findByAnioEscolarIdOrderByNombre(anioId);
		Map<UUID, String> nombresPorUsuarioId = nombresDeTutoresYCotutores(seccionesDelAnio);
		Map<String, List<Seccion>> seccionesPorCodigoDeGrado = seccionesDelAnio.stream()
				.collect(Collectors.groupingBy(Seccion::getGradoCodigo));

		List<GradoConSecciones> gradosConSecciones = new ArrayList<>();
		for (Grado grado : consultasSecciones.grados()) {
			boolean gradoPedido = nivelSolicitado == null || grado.nivel().equals(nivelSolicitado);
			if (gradoPedido) {
				List<Seccion> seccionesDelGrado = seccionesPorCodigoDeGrado.getOrDefault(grado.codigo(), List.of());
				List<SeccionResumen> resumenesDeSecciones = new ArrayList<>();
				int cantidadSeccionesActivas = 0;
				for (Seccion seccion : seccionesDelGrado) {
					resumenesDeSecciones.add(SeccionResumen.de(seccion, nombresPorUsuarioId));
					if (seccion.estaActiva()) {
						cantidadSeccionesActivas++;
					}
				}
				gradosConSecciones.add(new GradoConSecciones(grado.codigo(), grado.nombre(), grado.nivel(),
						grado.numero(), cantidadSeccionesActivas, resumenesDeSecciones));
			}
		}
		return gradosConSecciones;
	}

	/** Docentes para el "Modal - Asignar tutor", marcando a quienes ya son tutores de una sección activa. */
	@Transactional(readOnly = true)
	public List<DocenteElegible> listarDocentesElegibles(UUID anioId) {
		obtenerAnio(anioId);
		Set<UUID> tutorIdsDelAnio = new HashSet<>();
		for (Seccion seccion : seccionRepositorio.findByAnioEscolarIdAndEstado(anioId, EstadoSeccion.ACTIVA)) {
			if (seccion.getTutorId() != null) {
				tutorIdsDelAnio.add(seccion.getTutorId());
			}
		}

		List<DocenteElegible> docentesElegibles = new ArrayList<>();
		for (DirectorioDocentes.Docente docente : directorioDocentes.elegiblesParaTutoria()) {
			boolean yaEsTutor = tutorIdsDelAnio.contains(docente.id());
			docentesElegibles.add(new DocenteElegible(docente.id(), docente.nombre(), yaEsTutor));
		}
		return docentesElegibles;
	}

	/**
	 * Secciones activas del año, por grado y nombre, con su capacidad y los alumnos que ocupan vacante; las vacantes
	 * libres son la diferencia. Sin endpoint: Matrícula la usa para reservar (HU-23).
	 */
	@Transactional(readOnly = true)
	public List<SeccionConVacantes> listarSeccionesActivasConVacantes(UUID anioId) {
		obtenerAnio(anioId);
		Map<String, Grado> gradosPorCodigo = obtenerGradosPorCodigo();
		List<Seccion> seccionesActivas = new ArrayList<>(
				seccionRepositorio.findByAnioEscolarIdAndEstado(anioId, EstadoSeccion.ACTIVA));
		Comparator<Seccion> porOrdenDeGradoYLuegoNombre = Comparator
				.comparingInt((Seccion seccion) -> gradosPorCodigo.get(seccion.getGradoCodigo()).orden())
				.thenComparing(Seccion::getNombre);
		seccionesActivas.sort(porOrdenDeGradoYLuegoNombre);
		Map<UUID, String> nombresPorUsuarioId = nombresDeTutoresYCotutores(seccionesActivas);

		List<SeccionConVacantes> seccionesParaMatricula = new ArrayList<>();
		for (Seccion seccion : seccionesActivas) {
			Grado grado = gradosPorCodigo.get(seccion.getGradoCodigo());
			String nombreDelTutor = seccion.getTutorId() == null
					? null
					: nombresPorUsuarioId.get(seccion.getTutorId());
			seccionesParaMatricula.add(new SeccionConVacantes(seccion.getId(), grado.codigo(), grado.nombre(),
					seccion.getNombre(), nombreDelTutor, seccion.getCapacidadMaxima(),
					seccion.getAlumnosOcupanVacante()));
		}
		return seccionesParaMatricula;
	}

	// ---------- Cambios ----------

	@Transactional
	public SeccionResumen crear(UUID anioId, SolicitudSeccion solicitud) {
		validarAnioModificable(obtenerAnio(anioId));
		Grado grado = obtenerGradosPorCodigo().get(solicitud.gradoCodigo());
		if (grado == null) {
			throw ExcepcionNegocio.invalido("El grado no existe.");
		}
		Seccion seccionNueva = construir(
				() -> Seccion.crear(anioId, grado.codigo(), solicitud.nombre(), solicitud.capacidadMaxima()));
		boolean nombreYaExiste = seccionRepositorio.existsByAnioEscolarIdAndGradoCodigoAndNombre(anioId,
				grado.codigo(), seccionNueva.getNombre());
		if (nombreYaExiste) {
			throw nombreRepetido(seccionNueva, grado);
		}
		try {
			seccionRepositorio.saveAndFlush(seccionNueva);
		} catch (DataIntegrityViolationException violacionDeRestriccion) {
			// Otra petición creó el mismo nombre entre la consulta y el guardado: la restricción de la BD lo impide.
			throw nombreRepetido(seccionNueva, grado);
		}
		return SeccionResumen.de(seccionNueva, Map.of());
	}

	/** RF-50: solo cambia la capacidad, y no puede quedar por debajo de los alumnos que ocupan vacante. */
	@Transactional
	public SeccionResumen cambiarCapacidad(UUID seccionId, SolicitudCapacidad solicitud) {
		Seccion seccion = obtenerSeccionModificable(seccionId);
		validar(() -> seccion.cambiarCapacidad(solicitud.capacidadMaxima(), seccion.getAlumnosOcupanVacante()));
		return armarResumenSeccion(seccion);
	}

	/**
	 * Asigna el tutor y el cotutor. Un docente es tutor de una sola sección activa por año; la regla no aplica al
	 * cotutor.
	 */
	@Transactional
	public SeccionResumen asignarTutores(UUID seccionId, SolicitudTutores solicitud) {
		Seccion seccion = obtenerSeccionModificable(seccionId);
		seccionRepositorio.bloquearAsignacionesDelAnio(seccion.getAnioEscolarId());

		Map<UUID, String> nombresDeDocentesElegiblesPorId = new HashMap<>();
		for (DirectorioDocentes.Docente docente : directorioDocentes.elegiblesParaTutoria()) {
			nombresDeDocentesElegiblesPorId.put(docente.id(), docente.nombre());
		}
		validarElegible(nombresDeDocentesElegiblesPorId, solicitud.tutorId(), "tutor");
		validarElegible(nombresDeDocentesElegiblesPorId, solicitud.cotutorId(), "cotutor");

		// Los errores de la propia solicitud (sin tutor, tutor igual al cotutor) se informan antes que los de conflicto.
		validar(() -> Seccion.validarTutores(solicitud.tutorId(), solicitud.cotutorId()));
		// Una sección inactiva no ocupa al tutor: la regla se valida cuando se reactiva. Se valida antes de modificar.
		if (seccion.estaActiva()) {
			validarTutorSinOtraSeccion(seccion, solicitud.tutorId(), "Elige otro tutor.");
		}
		validar(() -> seccion.asignarTutores(solicitud.tutorId(), solicitud.cotutorId()));
		return armarResumenSeccion(seccion);
	}

	@Transactional
	public SeccionResumen inactivar(UUID seccionId) {
		Seccion seccion = obtenerSeccionModificable(seccionId);
		validar(seccion::inactivar);
		return armarResumenSeccion(seccion);
	}

	/**
	 * Si, mientras estuvo inactiva, su tutor pasó a otra sección activa, no se reactiva ni se quita el tutor por cuenta
	 * propia: el usuario decide.
	 */
	@Transactional
	public SeccionResumen reactivar(UUID seccionId) {
		Seccion seccion = obtenerSeccionModificable(seccionId);
		seccionRepositorio.bloquearAsignacionesDelAnio(seccion.getAnioEscolarId());
		boolean estabaInactiva = !seccion.estaActiva();
		if (estabaInactiva) {
			validarTutorSinOtraSeccion(seccion, seccion.getTutorId(),
					"Asígnale otro tutor a esta sección o inactiva la otra, y vuelve a reactivar.");
		}
		validar(seccion::reactivar); // si ya estaba activa, el dominio lo rechaza
		return armarResumenSeccion(seccion);
	}

	/** Se permite en un año PLANIFICADO o ACTIVO. Los docentes asignados a la sección se borran en cascada. */
	@Transactional
	public void eliminar(UUID seccionId) {
		Seccion seccion = obtenerSeccionModificable(seccionId);
		if (matriculasDeSecciones.tieneMatriculasOCambios(seccionId)) {
			throw ExcepcionNegocio.conflicto(SECCION_CON_HISTORIAL);
		}
		try {
			seccionRepositorio.delete(seccion);
			seccionRepositorio.flush();
		} catch (DataIntegrityViolationException violacionDeRestriccion) {
			// La BD encontró una referencia que la consulta no vio (p. ej. una matrícula creada justo antes): mismo 409.
			throw ExcepcionNegocio.conflicto(SECCION_CON_HISTORIAL);
		}
	}

	// ---------- Apoyo ----------

	private AnioEscolar obtenerAnio(UUID anioId) {
		return anioEscolarRepositorio.findById(anioId)
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("El año escolar no existe."));
	}

	/** La sección, si su año admite cambios: en un año CERRADO no se modifica nada. */
	private Seccion obtenerSeccionModificable(UUID seccionId) {
		Seccion seccion = seccionRepositorio.findById(seccionId)
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("La sección no existe."));
		validarAnioModificable(obtenerAnio(seccion.getAnioEscolarId()));
		return seccion;
	}

	private static void validarAnioModificable(AnioEscolar anio) {
		validar(() -> Seccion.validarAnioModificable(anio.getEstado()));
	}

	/** Sin nivel devuelve null (todos los niveles); con un nivel que no existe, responde 400. */
	private static String normalizarNivel(String nivel) {
		if (nivel == null || nivel.isBlank()) {
			return null;
		}
		String nivelNormalizado = nivel.strip().toUpperCase(Locale.ROOT);
		if (!NIVELES_VALIDOS.contains(nivelNormalizado)) {
			throw ExcepcionNegocio.invalido("El nivel debe ser PRIMARIA o SECUNDARIA.");
		}
		return nivelNormalizado;
	}

	private Map<String, Grado> obtenerGradosPorCodigo() {
		Map<String, Grado> gradosPorCodigo = new HashMap<>();
		for (Grado grado : consultasSecciones.grados()) {
			gradosPorCodigo.put(grado.codigo(), grado);
		}
		return gradosPorCodigo;
	}

	private SeccionResumen armarResumenSeccion(Seccion seccion) {
		return SeccionResumen.de(seccion, nombresDeTutoresYCotutores(List.of(seccion)));
	}

	/** Nombre de cada tutor y cotutor de las secciones indicadas, por id de usuario. Vacío si ninguna tiene. */
	private Map<UUID, String> nombresDeTutoresYCotutores(Collection<Seccion> seccionesAMostrar) {
		Set<UUID> usuarioIds = new HashSet<>();
		for (Seccion seccion : seccionesAMostrar) {
			if (seccion.getTutorId() != null) {
				usuarioIds.add(seccion.getTutorId());
			}
			if (seccion.getCotutorId() != null) {
				usuarioIds.add(seccion.getCotutorId());
			}
		}
		return usuarioIds.isEmpty() ? Map.of() : directorioDocentes.nombresDe(usuarioIds);
	}

	private static void validarElegible(Map<UUID, String> nombresDeDocentesElegiblesPorId, UUID usuarioId,
			String rolEnLaSeccion) {
		boolean noEsElegible = usuarioId != null && !nombresDeDocentesElegiblesPorId.containsKey(usuarioId);
		if (noEsElegible) {
			throw ExcepcionNegocio.invalido(
					"El %s debe ser un usuario activo con rol Docente o Tutor.".formatted(rolEnLaSeccion));
		}
	}

	/**
	 * Falla si el docente ya es tutor de otra sección activa del mismo año. Se llama antes de modificar la sección,
	 * para
	 * que un rechazo no deje cambios escritos.
	 */
	private void validarTutorSinOtraSeccion(Seccion seccion, UUID tutorId, String sugerencia) {
		if (tutorId == null) {
			return;
		}
		Optional<Seccion> otraSeccionDelTutor = seccionRepositorio
				.findFirstByAnioEscolarIdAndTutorIdAndEstadoAndIdNot(seccion.getAnioEscolarId(), tutorId,
						EstadoSeccion.ACTIVA, seccion.getId());
		if (otraSeccionDelTutor.isPresent()) {
			String nombreDelTutor = directorioDocentes.nombresDe(List.of(tutorId)).get(tutorId);
			Grado gradoDeLaOtraSeccion = obtenerGradosPorCodigo().get(otraSeccionDelTutor.get().getGradoCodigo());
			String etiquetaDeLaOtraSeccion = gradoDeLaOtraSeccion.etiquetaDe(otraSeccionDelTutor.get().getNombre());
			throw ExcepcionNegocio.conflicto("%s ya tiene la tutoría de %s este año. %s"
					.formatted(nombreDelTutor, etiquetaDeLaOtraSeccion, sugerencia));
		}
	}

	private static ExcepcionNegocio nombreRepetido(Seccion seccion, Grado grado) {
		return ExcepcionNegocio
				.conflicto("Ya existe la sección %s en %s.".formatted(seccion.getNombre(), grado.nombre()));
	}

	/** Traduce los errores del dominio: argumento inválido → 400; estado que no lo permite → 409. */
	private static void validar(Runnable reglaDeDominio) {
		try {
			reglaDeDominio.run();
		} catch (IllegalArgumentException argumentoInvalido) {
			throw ExcepcionNegocio.invalido(argumentoInvalido.getMessage());
		} catch (IllegalStateException estadoNoPermitido) {
			throw ExcepcionNegocio.conflicto(estadoNoPermitido.getMessage());
		}
	}

	private static <T> T construir(Supplier<T> reglaDeDominio) {
		try {
			return reglaDeDominio.get();
		} catch (IllegalArgumentException argumentoInvalido) {
			throw ExcepcionNegocio.invalido(argumentoInvalido.getMessage());
		}
	}
}
