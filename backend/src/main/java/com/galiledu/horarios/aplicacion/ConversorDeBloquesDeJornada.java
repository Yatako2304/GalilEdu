package com.galiledu.horarios.aplicacion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galiledu.compartido.web.ExcepcionNegocio;
import com.galiledu.horarios.aplicacion.JornadaDeSeccion.BloqueDeJornada;
import com.galiledu.horarios.aplicacion.JornadaDeSeccion.JornadaEstructura;
import com.galiledu.horarios.aplicacion.PlanDeSeccion.CursoDelPlan;
import com.galiledu.horarios.infraestructura.HorarioSeccionRepositorio;

/**
 * Pasa las celdas a bloques con las horas reales (inicio y fin del bloque en la jornada de la estructura de la
 * sección) y el docente de academico.docente_seccion. Una celda sin docente no se incluye: no puede cruzarse por
 * docente y el servicio que guarda ya valida que cada curso tenga uno. Una celda cuyo bloque no existe en la jornada
 * tampoco: eso lo rechaza el servicio de guardado antes de llegar aquí.
 */
@Service
public class ConversorDeBloquesDeJornada implements ConversorDeBloques {
	private final DatosDeSeccion secciones;
	private final HorarioSeccionRepositorio horarios;
	private final ResolutorDeJornada resolutor;
	private final PlanDeSeccion plan;

	public ConversorDeBloquesDeJornada(DatosDeSeccion secciones, HorarioSeccionRepositorio horarios,
			ResolutorDeJornada resolutor, PlanDeSeccion plan) {
		this.secciones = secciones;
		this.horarios = horarios;
		this.resolutor = resolutor;
		this.plan = plan;
	}

	@Override
	@Transactional(readOnly = true)
	public List<BloqueSolicitado> aBloquesSolicitados(UUID seccionId, List<CeldaHorario> celdas) {
		if (celdas.isEmpty()) {
			return List.of();
		}
		var datos = secciones.buscar(seccionId)
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("La sección no existe."));
		JornadaEstructura jornada = resolutor.resolver(datos, horarios.findBySeccionId(seccionId));
		Map<Integer, BloqueDeJornada> bloques = new HashMap<>();
		jornada.bloques().forEach(b -> bloques.put(b.numero(), b));
		Map<UUID, CursoDelPlan> cursos = new HashMap<>();
		plan.cursosPorId(seccionId, celdas.stream().map(CeldaHorario::planCursoId).distinct().toList())
				.forEach(c -> cursos.put(c.planCursoId(), c));

		List<BloqueSolicitado> solicitados = new ArrayList<>();
		for (CeldaHorario celda : celdas) {
			BloqueDeJornada bloque = bloques.get(celda.numeroBloque());
			CursoDelPlan curso = cursos.get(celda.planCursoId());
			if (bloque == null || curso == null || curso.docenteId() == null) {
				continue;
			}
			solicitados.add(new BloqueSolicitado(celda.dia(), celda.numeroBloque(), bloque.inicio(), bloque.fin(),
					celda.planCursoId(), curso.docenteId(), celda.ambienteId()));
		}
		return solicitados;
	}
}
