package com.galiledu.horarios.aplicacion;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galiledu.compartido.web.ExcepcionNegocio;
import com.galiledu.horarios.aplicacion.DtosHorarioApoderado.ClaseDelHorario;
import com.galiledu.horarios.aplicacion.DtosHorarioApoderado.HijoConHorario;
import com.galiledu.horarios.aplicacion.DtosHorarioApoderado.HorarioDelEstudiante;
import com.galiledu.horarios.aplicacion.DtosHorarioApoderado.MatriculaDelHijo;
import com.galiledu.horarios.aplicacion.DtosHorarioApoderado.TutorDelHorario;
import com.galiledu.horarios.aplicacion.DtosHorarios.HorarioDeSeccion;
import com.galiledu.horarios.aplicacion.HijosDelApoderado.MatriculaDeHijo;
import com.galiledu.horarios.infraestructura.HorarioSeccionRepositorio;

/**
 * HU-51: el apoderado consulta el horario de clases de la sección de cada hijo matriculado. Solo lectura. La matriz
 * se arma con la misma lógica que la del Coordinador ({@link ServicioHorarios#horarioDeSeccion}), sin modificarla.
 */
@Service
public class ServicioHorarioApoderado {
	static final String SIN_HORARIO = "No encontramos el horario de este estudiante.";

	private final HijosDelApoderado hijos;
	private final HorarioSeccionRepositorio horarios;
	private final ServicioHorarios servicioHorarios;

	public ServicioHorarioApoderado(HijosDelApoderado hijos, HorarioSeccionRepositorio horarios,
			ServicioHorarios servicioHorarios) {
		this.hijos = hijos;
		this.horarios = horarios;
		this.servicioHorarios = servicioHorarios;
	}

	/**
	 * Los hijos matriculados del usuario (selector de Figma), con sus matrículas del año más reciente al más antiguo.
	 */
	@Transactional(readOnly = true)
	public List<HijoConHorario> hijos(UUID usuarioId) {
		Map<UUID, List<MatriculaDeHijo>> porHijo = new LinkedHashMap<>();
		for (MatriculaDeHijo m : hijos.matriculasDeHijos(usuarioId)) {
			porHijo.computeIfAbsent(m.estudianteId(), k -> new ArrayList<>()).add(m);
		}
		return porHijo.values().stream()
				.map(ms -> new HijoConHorario(ms.get(0).estudianteId(), ms.get(0).nombreCompleto(),
						ms.stream().map(m -> new MatriculaDelHijo(m.anio(), m.gradoNombre(), m.nivel(), m.seccion()))
								.toList()))
				.toList();
	}

	/**
	 * El horario de la sección en la que el hijo está matriculado en {@code anio} (si es null, el año más reciente).
	 * 404 si el estudiante no es hijo del usuario o no está matriculado ese año. Sin horario guardado responde
	 * {@code disponible = false}.
	 */
	@Transactional(readOnly = true)
	public HorarioDelEstudiante horario(UUID usuarioId, UUID estudianteId, Integer anio) {
		MatriculaDeHijo m = hijos.matriculasDeHijos(usuarioId).stream()
				.filter(x -> x.estudianteId().equals(estudianteId) && (anio == null || x.anio() == anio))
				.findFirst()
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado(SIN_HORARIO));
		TutorDelHorario tutor = m.tutorNombre() == null
				? null
				: new TutorDelHorario(m.tutorNombre(), m.tutorCelular(), m.tutorCorreo());

		if (horarios.findBySeccionId(m.seccionId()).isEmpty()) {
			return new HorarioDelEstudiante(m.estudianteId(), m.nombreCompleto(), m.anio(), m.gradoNombre(),
					m.nivel(), m.seccion(), tutor, false, null, List.of());
		}
		HorarioDeSeccion h = servicioHorarios.horarioDeSeccion(m.seccionId());
		List<ClaseDelHorario> clases = h.celdas().stream()
				.map(c -> new ClaseDelHorario(c.dia(), c.numeroBloque(), c.curso(),
						c.docente() == null ? null : c.docente().nombre(),
						c.ambiente() == null ? null : c.ambiente().nombre()))
				.toList();
		return new HorarioDelEstudiante(m.estudianteId(), m.nombreCompleto(), m.anio(), m.gradoNombre(), m.nivel(),
				m.seccion(), tutor, true, h.jornada(), clases);
	}
}
