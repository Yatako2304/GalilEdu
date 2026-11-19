package com.galiledu.asistencia.aplicacion;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galiledu.asistencia.aplicacion.CalendarioEscolar.CalendarioDelNivel;
import com.galiledu.asistencia.aplicacion.DtosAsistencia.AsistenciaDelDia;
import com.galiledu.asistencia.aplicacion.DtosAsistencia.FilaAsistencia;
import com.galiledu.asistencia.aplicacion.DtosAsistencia.MarcaSolicitud;
import com.galiledu.asistencia.aplicacion.DtosAsistencia.RegistroDelDia;
import com.galiledu.asistencia.aplicacion.DtosAsistencia.Resumen;
import com.galiledu.asistencia.aplicacion.DtosAsistencia.SeccionAsistencia;
import com.galiledu.asistencia.aplicacion.DtosAsistencia.SolicitudAsistencia;
import com.galiledu.asistencia.aplicacion.SeccionesTutoradas.EstudianteDeSeccion;
import com.galiledu.asistencia.aplicacion.SeccionesTutoradas.SeccionTutorada;
import com.galiledu.asistencia.dominio.AsistenciaEstudiante;
import com.galiledu.asistencia.dominio.CorreccionAsistencia;
import com.galiledu.asistencia.dominio.EstadoAsistencia;
import com.galiledu.asistencia.dominio.MarcaAsistencia;
import com.galiledu.asistencia.dominio.RegistroAsistencia;
import com.galiledu.asistencia.infraestructura.CorreccionAsistenciaRepositorio;
import com.galiledu.asistencia.infraestructura.NombresDeUsuario;
import com.galiledu.asistencia.infraestructura.RegistroAsistenciaRepositorio;
import com.galiledu.compartido.web.ExcepcionNegocio;

/**
 * HU-65 · RF-143: el tutor o cotutor registra la asistencia diaria de su sección (Presente, Tardanza con hora de
 * llegada o Falta) y la corrige solo durante el mismo día. Figma › Tutor › AS-01 a AS-06.
 * <p>
 * Reglas: solo el día de hoy (reloj de Lima) y si es día escolar ({@link ReglaDiaEscolar}); hay que marcar a todos los
 * estudiantes MATRICULADO de la sección; cada corrección deja historial. Si el tutor y el cotutor guardan a la vez, el
 * segundo recibe 409 y vuelve a cargar.
 */
@Service
public class ServicioAsistencia {
	static final String MENSAJE_SOLO_HOY = "Solo puedes registrar o corregir la asistencia del día de hoy.";
	static final String MENSAJE_CAMBIO = "La asistencia cambió mientras la editabas. Vuelve a cargarla.";

	private final SeccionesTutoradas secciones;
	private final CalendarioEscolar calendario;
	private final RegistroAsistenciaRepositorio registros;
	private final CorreccionAsistenciaRepositorio correcciones;
	private final NombresDeUsuario nombres;
	private final Clock reloj;

	public ServicioAsistencia(SeccionesTutoradas secciones, CalendarioEscolar calendario,
			RegistroAsistenciaRepositorio registros, CorreccionAsistenciaRepositorio correcciones,
			NombresDeUsuario nombres, Clock reloj) {
		this.secciones = secciones;
		this.calendario = calendario;
		this.registros = registros;
		this.correcciones = correcciones;
		this.nombres = nombres;
		this.reloj = reloj;
	}

	/** Secciones de las que el usuario es tutor o cotutor en el año activo. */
	@Transactional(readOnly = true)
	public List<SeccionAsistencia> seccionesDelTutor(UUID usuarioId) {
		return secciones.delUsuario(usuarioId).stream().map(ServicioAsistencia::aDto).toList();
	}

	/** AS-01, AS-05 y AS-06: la asistencia de un día de la sección (registrada o no). */
	@Transactional(readOnly = true)
	public AsistenciaDelDia consultar(UUID usuarioId, UUID seccionId, LocalDate fecha) {
		SeccionTutorada seccion = seccionDelTutor(usuarioId, seccionId);
		return vista(seccion, fecha);
	}

	/** AS-04 (registrar) y AS-06 (corregir): guarda la asistencia de hoy con todos los estudiantes marcados. */
	@Transactional
	public AsistenciaDelDia guardar(UUID usuarioId, UUID seccionId, LocalDate fecha, SolicitudAsistencia solicitud) {
		SeccionTutorada seccion = seccionDelTutor(usuarioId, seccionId);
		LocalDate hoy = LocalDate.now(reloj);
		if (!fecha.equals(hoy)) {
			throw ExcepcionNegocio.conflicto(MENSAJE_SOLO_HOY);
		}
		String motivo = motivoNoEscolar(seccion, fecha);
		if (motivo != null) {
			throw ExcepcionNegocio.conflicto(motivo);
		}
		List<EstudianteDeSeccion> estudiantes = secciones.estudiantes(seccion.id());
		List<MarcaAsistencia> marcas = marcas(solicitud, estudiantes);
		OffsetDateTime ahora = OffsetDateTime.now(reloj);
		var existente = registros.findBySeccionIdAndFecha(seccion.id(), fecha);
		try {
			if (existente.isEmpty()) {
				if (solicitud.version() != null) {
					throw ExcepcionNegocio.conflicto(MENSAJE_CAMBIO);
				}
				registros.saveAndFlush(RegistroAsistencia.registrar(seccion.id(), fecha, marcas, usuarioId, ahora));
			} else {
				corregir(existente.get(), solicitud.version(), marcas, usuarioId, ahora);
			}
		} catch (DataIntegrityViolationException | ObjectOptimisticLockingFailureException excepcion) {
			throw ExcepcionNegocio.conflicto(MENSAJE_CAMBIO);
		}
		return vista(seccion, fecha);
	}

	private void corregir(RegistroAsistencia registro, Integer version, List<MarcaAsistencia> marcas,
			UUID usuarioId, OffsetDateTime ahora) {
		if (version == null || version != registro.getVersion()) {
			throw ExcepcionNegocio.conflicto(MENSAJE_CAMBIO);
		}
		Set<UUID> registrados = registro.getAsistencias().stream().map(AsistenciaEstudiante::getMatriculaId)
				.collect(Collectors.toSet());
		List<MarcaAsistencia> nuevos = marcas.stream().filter(m -> !registrados.contains(m.matriculaId())).toList();
		List<CorreccionAsistencia> cambios = registro.corregir(marcas, usuarioId, ahora);
		nuevos.forEach(registro::agregar);
		if (cambios.isEmpty() && nuevos.isEmpty()) {
			throw ExcepcionNegocio.invalido("No hay cambios que guardar.");
		}
		registros.saveAndFlush(registro);
		correcciones.saveAll(cambios);
	}

	/** Valida la lista completa: cada estudiante de la sección una sola vez, y ninguno de otra sección. */
	private static List<MarcaAsistencia> marcas(SolicitudAsistencia solicitud, List<EstudianteDeSeccion> estudiantes) {
		ExcepcionNegocio.exigir(solicitud, "Envía la asistencia de los estudiantes.");
		List<MarcaSolicitud> enviadas = solicitud.asistencias() == null ? List.of() : solicitud.asistencias();
		Set<UUID> deLaSeccion = estudiantes.stream().map(EstudianteDeSeccion::matriculaId).collect(Collectors.toSet());
		Set<UUID> vistos = new HashSet<>();
		List<MarcaAsistencia> marcas = new ArrayList<>();
		for (MarcaSolicitud marca : enviadas) {
			if (marca == null || marca.matriculaId() == null || !deLaSeccion.contains(marca.matriculaId())) {
				throw ExcepcionNegocio.invalido("Uno de los estudiantes no pertenece a la sección.");
			}
			if (!vistos.add(marca.matriculaId())) {
				throw ExcepcionNegocio.invalido("Un estudiante está marcado más de una vez.");
			}
			if (marca.estado() == null) {
				continue;
			}
			try {
				marcas.add(new MarcaAsistencia(marca.matriculaId(), marca.estado(), marca.horaLlegada()));
			} catch (IllegalArgumentException excepcion) {
				throw ExcepcionNegocio.invalido(excepcion.getMessage());
			}
		}
		int faltan = estudiantes.size() - marcas.size();
		if (faltan > 0) {
			throw ExcepcionNegocio.invalido(faltan == 1
					? "Falta 1 estudiante por marcar."
					: "Faltan " + faltan + " estudiantes por marcar.");
		}
		return marcas;
	}

	private AsistenciaDelDia vista(SeccionTutorada seccion, LocalDate fecha) {
		LocalDate hoy = LocalDate.now(reloj);
		String motivo = motivoNoEscolar(seccion, fecha);
		var registro = registros.findBySeccionIdAndFecha(seccion.id(), fecha);
		Map<UUID, AsistenciaEstudiante> marcadas = registro.map(r -> r.getAsistencias().stream()
				.collect(Collectors.toMap(AsistenciaEstudiante::getMatriculaId, Function.identity())))
				.orElse(Map.of());
		Set<UUID> modificadas = marcadas.isEmpty()
				? Set.of()
				: new HashSet<>(correcciones.conCorrecciones(
						marcadas.values().stream().map(AsistenciaEstudiante::getId).toList()));
		List<FilaAsistencia> filas = secciones.estudiantes(seccion.id()).stream().map(e -> {
			AsistenciaEstudiante marca = marcadas.get(e.matriculaId());
			return new FilaAsistencia(e.matriculaId(), e.estudianteId(), e.nombres(), e.apellidos(), e.documento(),
					marca == null ? null : marca.getEstado(), marca == null ? null : marca.getHoraLlegada(),
					marca != null && modificadas.contains(marca.getId()));
		}).toList();
		RegistroDelDia delDia = registro.map(this::aDto).orElse(null);
		return new AsistenciaDelDia(aDto(seccion), fecha, hoy, motivo == null && fecha.equals(hoy), motivo, delDia,
				resumen(filas), filas);
	}

	private static Resumen resumen(List<FilaAsistencia> filas) {
		int presentes = contar(filas, EstadoAsistencia.PRESENTE);
		int tardanzas = contar(filas, EstadoAsistencia.TARDANZA);
		int faltas = contar(filas, EstadoAsistencia.FALTA);
		return new Resumen(presentes, tardanzas, faltas, filas.size() - presentes - tardanzas - faltas, filas.size());
	}

	private static int contar(List<FilaAsistencia> filas, EstadoAsistencia estado) {
		return (int) filas.stream().filter(f -> f.estado() == estado).count();
	}

	private String motivoNoEscolar(SeccionTutorada seccion, LocalDate fecha) {
		CalendarioDelNivel delNivel = calendario.de(seccion.anioEscolarId(), seccion.nivel())
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("No existe el año escolar de la sección."));
		return ReglaDiaEscolar.motivoNoEscolar(delNivel, fecha).orElse(null);
	}

	/** Un tutor solo ve y edita sus secciones; una ajena o inexistente responde 404. */
	private SeccionTutorada seccionDelTutor(UUID usuarioId, UUID seccionId) {
		return secciones.delUsuario(usuarioId).stream().filter(s -> s.id().equals(seccionId)).findFirst()
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("No eres tutor de esta sección."));
	}

	private RegistroDelDia aDto(RegistroAsistencia registro) {
		return new RegistroDelDia(registro.getId(), registro.getVersion(), registro.getRegistradoEn(),
				nombres.de(registro.getRegistradoPor()), registro.getCorregidoEn(),
				nombres.de(registro.getCorregidoPor()));
	}

	private static SeccionAsistencia aDto(SeccionTutorada seccion) {
		return new SeccionAsistencia(seccion.id(), seccion.anio(), seccion.grado(), seccion.nombre(), seccion.cargo());
	}
}
