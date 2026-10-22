package com.galiledu.configuracion.aplicacion;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galiledu.compartido.web.ExcepcionNegocio;
import com.galiledu.configuracion.aplicacion.DtosEstructurasHorarias.DetalleEstructuraHoraria;
import com.galiledu.configuracion.aplicacion.DtosEstructurasHorarias.EstructuraGuardada;
import com.galiledu.configuracion.aplicacion.DtosEstructurasHorarias.EstructuraHorariaResumen;
import com.galiledu.configuracion.aplicacion.DtosEstructurasHorarias.EstructurasHorariasDelAnio;
import com.galiledu.configuracion.aplicacion.DtosEstructurasHorarias.JornadaCalculada;
import com.galiledu.configuracion.aplicacion.DtosEstructurasHorarias.SolicitudEdicionEstructuraHoraria;
import com.galiledu.configuracion.aplicacion.DtosEstructurasHorarias.SolicitudEstructuraHoraria;
import com.galiledu.configuracion.aplicacion.DtosEstructurasHorarias.VistaPreviaJornada;
import com.galiledu.configuracion.aplicacion.PlanesDeEvaluacion.GradoExcedido;
import com.galiledu.configuracion.dominio.AnioEscolar;
import com.galiledu.configuracion.dominio.CalculadoraJornada.Jornada;
import com.galiledu.configuracion.dominio.DefinicionEstructura;
import com.galiledu.configuracion.dominio.EstadoAnioEscolar;
import com.galiledu.configuracion.dominio.EstructuraHoraria;
import com.galiledu.configuracion.infraestructura.AnioEscolarRepositorio;
import com.galiledu.configuracion.infraestructura.EstructuraHorariaRepositorio;

/**
 * HU-44: estructuras horarias del año escolar. Las reglas de forma de una estructura (largos, rangos, la hora límite
 * de las 18:00) viven en {@link EstructuraHoraria}; aquí se decide lo que depende de otros datos (el año, el código
 * repetido, la principal de cada nivel y los planes de evaluación) y se traducen los errores del dominio: argumento
 * inválido → 400, estado que no lo permite → 409.
 * <p>
 * Un año puede tener varias estructuras, pero solo la principal de cada nivel se usa. Los cambios del año se
 * serializan con un bloqueo y todo se valida antes de modificar, porque las consultas de JPA envían los cambios
 * pendientes antes de leer y un rechazo no debe dejar nada escrito.
 */
@Service
public class ServicioEstructurasHorarias {
	static final String MENSAJE_OTRA_PERSONA = "Otra persona acaba de cambiar las estructuras de este año. Actualiza la pantalla e inténtalo de nuevo.";
	static final String MENSAJE_NO_SE_PUDO_GUARDAR = "No se pudo guardar la estructura horaria. Revisa los datos e inténtalo de nuevo.";

	private final EstructuraHorariaRepositorio estructuraRepositorio;
	private final AnioEscolarRepositorio anioEscolarRepositorio;
	private final PlanesDeEvaluacion planesDeEvaluacion;

	public ServicioEstructurasHorarias(EstructuraHorariaRepositorio estructuraRepositorio,
			AnioEscolarRepositorio anioEscolarRepositorio, PlanesDeEvaluacion planesDeEvaluacion) {
		this.estructuraRepositorio = estructuraRepositorio;
		this.anioEscolarRepositorio = anioEscolarRepositorio;
		this.planesDeEvaluacion = planesDeEvaluacion;
	}

	// ---------- Consultas ----------

	@Transactional(readOnly = true)
	public EstructurasHorariasDelAnio listarPorAnioEscolar(UUID anioId) {
		AnioEscolar anio = obtenerAnio(anioId);
		List<EstructuraHorariaResumen> resumenes = new ArrayList<>();
		for (EstructuraHoraria estructura : estructuraRepositorio.findByAnioEscolarIdOrderByCodigo(anioId)) {
			resumenes.add(EstructuraHorariaResumen.de(estructura));
		}
		return new EstructurasHorariasDelAnio(esAnioModificable(anio), resumenes);
	}

	@Transactional(readOnly = true)
	public DetalleEstructuraHoraria detalleEstructuraHoraria(UUID estructuraId) {
		EstructuraHoraria estructura = estructuraRepositorio.findById(estructuraId)
				.orElseThrow(() -> estructuraNoExiste());
		AnioEscolar anio = obtenerAnio(estructura.getAnioEscolarId());
		return DetalleEstructuraHoraria.de(estructura, esAnioModificable(anio));
	}

	/**
	 * Calcula la jornada y reúne todos los errores del formulario sin guardar nada. Solo exige que el año exista (para
	 * revisar el código repetido): no mira si el año admite cambios ni los planes de evaluación.
	 */
	@Transactional(readOnly = true)
	public VistaPreviaJornada vistaPreviaEstructuraHoraria(UUID anioId, SolicitudEstructuraHoraria solicitud) {
		obtenerAnio(anioId);
		DefinicionEstructura definicion = solicitud.definicion();
		List<String> errores = new ArrayList<>(EstructuraHoraria.detectarErrores(definicion));
		try {
			EstructuraHoraria.normalizarNivel(solicitud.nivel());
		} catch (IllegalArgumentException nivelInvalido) {
			errores.add(nivelInvalido.getMessage());
		}
		String codigo = definicion.normalizada().codigo();
		// Un código inválido (vacío, con NUL, fuera de la regla) ya tiene su error y no se busca en la base.
		if (EstructuraHoraria.esCodigoValido(codigo)
				&& estructuraRepositorio.existsByAnioEscolarIdAndCodigo(anioId, codigo)) {
			errores.add(codigoRepetido(codigo).getMessage());
		}

		JornadaCalculada jornadaCalculada = null;
		Optional<Jornada> jornada = EstructuraHoraria.calcularJornada(definicion);
		if (jornada.isPresent()) {
			DefinicionEstructura normalizada = definicion.normalizada();
			jornadaCalculada = new JornadaCalculada(DtosEstructurasHorarias.filasDe(jornada.get()),
					DtosEstructurasHorarias.horaSalidaDe(jornada.get()), bloquesSemanalesDe(normalizada));
		}
		return new VistaPreviaJornada(errores.isEmpty(), errores, jornadaCalculada);
	}

	// ---------- Cambios ----------

	@Transactional
	public EstructuraGuardada crearEstructuraHoraria(UUID anioId, SolicitudEstructuraHoraria solicitud) {
		AnioEscolar anio = obtenerAnio(anioId);
		validarAnioModificable(anio);
		estructuraRepositorio.bloquearEstructurasDelAnio(anioId);

		DefinicionEstructura definicion = solicitud.definicion();
		validarDefinicion(definicion);
		String nivel = construir(() -> EstructuraHoraria.normalizarNivel(solicitud.nivel()));
		String codigo = definicion.normalizada().codigo();
		if (estructuraRepositorio.existsByAnioEscolarIdAndCodigo(anioId, codigo)) {
			throw codigoRepetido(codigo);
		}

		Optional<EstructuraHoraria> principalActual = estructuraRepositorio
				.findByAnioEscolarIdAndNivelAndEsPrincipalTrue(anioId, nivel);
		// La primera estructura de un nivel nace principal aunque la casilla venga sin marcar.
		boolean seraPrincipal = solicitud.pideSerPrincipal() || principalActual.isEmpty();
		int bloquesSemanalesNuevos = bloquesSemanalesDe(definicion.normalizada());
		List<String> advertencias = new ArrayList<>();
		if (seraPrincipal) {
			Integer bloquesSemanalesAnteriores = principalActual.map(EstructuraHoraria::bloquesPorSemana)
					.orElse(null);
			validarQueLosPlanesCaben(anioId, nivel, bloquesSemanalesNuevos, bloquesSemanalesAnteriores);
		}

		EstructuraHoraria estructuraNueva = construir(
				() -> EstructuraHoraria.crear(anioId, nivel, seraPrincipal, definicion));
		if (seraPrincipal && principalActual.isPresent()) {
			EstructuraHoraria principalAnterior = principalActual.get();
			advertencias.add(advertenciaDeNuevaPrincipal(codigo, principalAnterior.getCodigo(), nivel));
			agregarAdvertenciaDePlanificacion(advertencias, anioId, nivel, principalAnterior.bloquesPorSemana(),
					bloquesSemanalesNuevos);
			// El índice único admite una sola principal por nivel: la anterior baja antes de guardar la nueva.
			principalAnterior.dejarDeSerPrincipal();
			estructuraRepositorio.flush();
		}
		guardar(estructuraNueva, codigo);
		return new EstructuraGuardada(DetalleEstructuraHoraria.de(estructuraNueva, true), advertencias);
	}

	@Transactional
	public EstructuraGuardada editarEstructuraHoraria(UUID estructuraId,
			SolicitudEdicionEstructuraHoraria solicitud) {
		EstructuraHoraria estructura = obtenerEstructuraBloqueandoSuAnio(estructuraId);
		UUID anioId = estructura.getAnioEscolarId();
		validarAnioModificable(obtenerAnio(anioId));

		DefinicionEstructura definicion = solicitud.definicion();
		validarDefinicion(definicion);
		String codigo = definicion.normalizada().codigo();
		if (estructuraRepositorio.existsByAnioEscolarIdAndCodigoAndIdNot(anioId, codigo, estructuraId)) {
			throw codigoRepetido(codigo);
		}
		boolean eraPrincipal = estructura.esPrincipal();
		// Ausente significa "no cambiar": solo un false explícito sobre la principal es un desmarcado.
		Boolean esPrincipalPedido = solicitud.esPrincipal();
		boolean pidePrincipal = esPrincipalPedido == null ? eraPrincipal : esPrincipalPedido;
		if (eraPrincipal && !pidePrincipal) {
			throw ExcepcionNegocio.conflicto("No se puede quitar la marca de estructura principal. "
					+ "Marca otra estructura de %s como principal.".formatted(nombreDelNivel(estructura)));
		}

		String nivel = estructura.getNivel();
		int bloquesSemanalesAntes = estructura.bloquesPorSemana();
		int bloquesSemanalesDespues = bloquesSemanalesDe(definicion.normalizada());
		Optional<EstructuraHoraria> principalAnterior = Optional.empty();
		boolean pasaAPrincipal = !eraPrincipal && pidePrincipal;
		if (pasaAPrincipal) {
			principalAnterior = estructuraRepositorio.findByAnioEscolarIdAndNivelAndEsPrincipalTrue(anioId, nivel);
		}
		// Una estructura que no es (ni pasa a ser) principal no se valida contra los planes.
		Integer bloquesSemanalesDeReferencia = eraPrincipal
				? Integer.valueOf(bloquesSemanalesAntes)
				: principalAnterior.map(EstructuraHoraria::bloquesPorSemana).orElse(null);
		if (eraPrincipal || pasaAPrincipal) {
			validarQueLosPlanesCaben(anioId, nivel, bloquesSemanalesDespues, bloquesSemanalesDeReferencia);
		}

		List<String> advertencias = new ArrayList<>();
		if (pasaAPrincipal) {
			String codigoDeLaAnterior = principalAnterior.map(EstructuraHoraria::getCodigo).orElse(null);
			advertencias.add(advertenciaDeNuevaPrincipal(codigo, codigoDeLaAnterior, nivel));
		}
		if (bloquesSemanalesDeReferencia != null && (eraPrincipal || pasaAPrincipal)) {
			agregarAdvertenciaDePlanificacion(advertencias, anioId, nivel, bloquesSemanalesDeReferencia,
					bloquesSemanalesDespues);
		}

		// Todo está validado: desde aquí se modifica.
		if (pasaAPrincipal) {
			principalAnterior.ifPresent(EstructuraHoraria::dejarDeSerPrincipal);
			estructuraRepositorio.flush();
			estructura.marcarComoPrincipal();
		}
		construir(() -> {
			estructura.editar(definicion);
			return estructura;
		});
		guardar(estructura, codigo);
		return new EstructuraGuardada(DetalleEstructuraHoraria.de(estructura, true), advertencias);
	}

	@Transactional
	public void eliminarEstructuraHoraria(UUID estructuraId) {
		EstructuraHoraria estructura = obtenerEstructuraBloqueandoSuAnio(estructuraId);
		UUID anioId = estructura.getAnioEscolarId();
		validarAnioModificable(obtenerAnio(anioId));

		if (estructura.esPrincipal()) {
			String nivel = estructura.getNivel();
			boolean quedanOtrasDelNivel = estructuraRepositorio.countByAnioEscolarIdAndNivel(anioId, nivel) > 1;
			if (quedanOtrasDelNivel) {
				throw ExcepcionNegocio.conflicto("No se puede eliminar la estructura principal de %s: quedan otras "
						.formatted(nombreDelNivel(estructura))
						+ "estructuras del nivel. Marca otra como principal primero.");
			}
			if (planesDeEvaluacion.hayPlanes(anioId, nivel)) {
				throw ExcepcionNegocio.conflicto("No se puede eliminar la estructura principal de %s: hay cursos "
						.formatted(nombreDelNivel(estructura))
						+ "planificados que dependen de ella. Marca otra como principal primero.");
			}
		}
		estructuraRepositorio.delete(estructura);
		estructuraRepositorio.flush();
	}

	// ---------- Apoyo ----------

	private AnioEscolar obtenerAnio(UUID anioId) {
		return anioEscolarRepositorio.findById(anioId)
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("El año escolar no existe."));
	}

	private static ExcepcionNegocio estructuraNoExiste() {
		return ExcepcionNegocio.noEncontrado("La estructura horaria no existe.");
	}

	/**
	 * Bloquea el año de la estructura y recién entonces la carga: así no se trabaja con una copia que otra petición ya
	 * cambió mientras esta esperaba el bloqueo.
	 */
	private EstructuraHoraria obtenerEstructuraBloqueandoSuAnio(UUID estructuraId) {
		UUID anioId = estructuraRepositorio.findAnioEscolarIdById(estructuraId)
				.orElseThrow(() -> estructuraNoExiste());
		estructuraRepositorio.bloquearEstructurasDelAnio(anioId);
		return estructuraRepositorio.findById(estructuraId).orElseThrow(() -> estructuraNoExiste());
	}

	private static boolean esAnioModificable(AnioEscolar anio) {
		return anio.getEstado() == EstadoAnioEscolar.PLANIFICADO;
	}

	private static void validarAnioModificable(AnioEscolar anio) {
		try {
			EstructuraHoraria.validarAnioModificable(anio.getEstado());
		} catch (IllegalStateException anioNoPlanificado) {
			throw ExcepcionNegocio.conflicto(anioNoPlanificado.getMessage());
		}
	}

	/** Lanza el primer error de forma de la definición (400); la vista previa, en cambio, los muestra todos. */
	private static void validarDefinicion(DefinicionEstructura definicion) {
		List<String> errores = EstructuraHoraria.detectarErrores(definicion);
		if (!errores.isEmpty()) {
			throw ExcepcionNegocio.invalido(errores.get(0));
		}
	}

	private static int bloquesSemanalesDe(DefinicionEstructura definicionNormalizada) {
		return definicionNormalizada.bloquesPorDia() * definicionNormalizada.diasLectivos().size();
	}

	/**
	 * La principal de un nivel no puede quedar con menos bloques por semana que las horas que un grado ya planificó.
	 * Solo se comprueba cuando algo puede romperlo: una principal nueva (sin anterior contra qué comparar) o una que
	 * baja de bloques. Aumentar nunca se rechaza.
	 */
	private void validarQueLosPlanesCaben(UUID anioId, String nivel, int bloquesSemanalesNuevos,
			Integer bloquesSemanalesAnteriores) {
		boolean podriaRomperLosPlanes = bloquesSemanalesAnteriores == null
				|| bloquesSemanalesNuevos < bloquesSemanalesAnteriores;
		if (!podriaRomperLosPlanes) {
			return;
		}
		Optional<GradoExcedido> gradoQueSePasa = planesDeEvaluacion.gradoQueExcede(anioId, nivel,
				bloquesSemanalesNuevos);
		if (gradoQueSePasa.isPresent()) {
			GradoExcedido grado = gradoQueSePasa.get();
			throw ExcepcionNegocio.conflicto(("No se puede reducir la jornada a %d bloques por semana: %s ya tiene "
					+ "%d horas de cursos planificadas. Reduce las horas del plan de evaluación o conserva al menos "
					+ "%d bloques.").formatted(bloquesSemanalesNuevos, grado.gradoNombre(),
							grado.horasPlanificadas(), grado.horasPlanificadas()));
		}
	}

	/**
	 * Avisa que cambiar los bloques por semana de la principal implica revisar lo planificado: no rechaza, solo
	 * informa, y solo si el nivel tiene planes y los bloques realmente cambian.
	 */
	private void agregarAdvertenciaDePlanificacion(List<String> advertencias, UUID anioId, String nivel,
			int bloquesSemanalesAntes, int bloquesSemanalesDespues) {
		if (bloquesSemanalesAntes == bloquesSemanalesDespues || !planesDeEvaluacion.hayPlanes(anioId, nivel)) {
			return;
		}
		// Con 0 bloques se pasan todos los grados con plan: el primero es el de más horas planificadas.
		GradoExcedido gradoConMasHoras = planesDeEvaluacion.gradoQueExcede(anioId, nivel, 0).orElseThrow();
		advertencias.add(("La jornada de %s pasa de %d a %d bloques por semana. Revisa el plan de evaluación: %s "
				+ "tiene %d horas planificadas.").formatted(nombreDelNivel(nivel), bloquesSemanalesAntes,
						bloquesSemanalesDespues, gradoConMasHoras.gradoNombre(),
						gradoConMasHoras.horasPlanificadas()));
	}

	private static String advertenciaDeNuevaPrincipal(String codigoNuevo, String codigoAnterior, String nivel) {
		String queDejaDeSer = codigoAnterior == null ? "" : "; %s deja de serlo".formatted(codigoAnterior);
		return "%s pasa a ser la estructura principal de %s%s.".formatted(codigoNuevo, nombreDelNivel(nivel),
				queDejaDeSer);
	}

	private static String nombreDelNivel(EstructuraHoraria estructura) {
		return nombreDelNivel(estructura.getNivel());
	}

	private static String nombreDelNivel(String nivel) {
		return nivel.toLowerCase(Locale.ROOT);
	}

	private static ExcepcionNegocio codigoRepetido(String codigo) {
		return ExcepcionNegocio.conflicto("Ya existe una estructura con el código %s en este año.".formatted(codigo));
	}

	/**
	 * Guarda y envía los cambios ya. Con el año bloqueado las restricciones de la BD no deberían saltar. Si saltan, el
	 * mensaje depende de cuál fue: el índice de la principal y el código repetido son carreras con otra petición;
	 * cualquier otra restricción da un mensaje genérico. Nunca se muestra el nombre de una restricción.
	 */
	private void guardar(EstructuraHoraria estructura, String codigo) {
		try {
			estructuraRepositorio.saveAndFlush(estructura);
		} catch (DataIntegrityViolationException violacionDeRestriccion) {
			String detalle = String.valueOf(violacionDeRestriccion.getMessage());
			if (detalle.contains("uq_estructura_horaria_codigo")) {
				throw codigoRepetido(codigo);
			}
			if (detalle.contains("ux_estructura_horaria_principal")) {
				throw ExcepcionNegocio.conflicto(MENSAJE_OTRA_PERSONA);
			}
			throw ExcepcionNegocio.conflicto(MENSAJE_NO_SE_PUDO_GUARDAR);
		}
	}

	/** Traduce los errores del dominio: argumento inválido → 400; estado que no lo permite → 409. */
	private static <T> T construir(Supplier<T> reglaDeDominio) {
		try {
			return reglaDeDominio.get();
		} catch (IllegalArgumentException argumentoInvalido) {
			throw ExcepcionNegocio.invalido(argumentoInvalido.getMessage());
		} catch (IllegalStateException estadoNoPermitido) {
			throw ExcepcionNegocio.conflicto(estadoNoPermitido.getMessage());
		}
	}
}
