package com.galiledu.horarios.aplicacion;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galiledu.compartido.web.ExcepcionNegocio;

/**
 * Avisa de los cruces de docente y de ambiente de unas celdas antes de guardar (HU-46). Solo lee: no bloquea el año
 * ni guarda nada. Las celdas que el conversor no puede resolver (curso sin docente, bloque fuera de la jornada) se
 * ignoran; el motivo exacto lo da el PUT.
 */
@Service
public class ServicioCrucesHorario {
	private final DatosDeSeccion secciones;
	private final ConversorDeBloques conversor;
	private final ValidadorCruces validadorCruces;

	public ServicioCrucesHorario(DatosDeSeccion secciones, ConversorDeBloques conversor,
			ValidadorCruces validadorCruces) {
		this.secciones = secciones;
		this.conversor = conversor;
		this.validadorCruces = validadorCruces;
	}

	/** Lista vacía si no hay cruces. 404 si la sección no existe. */
	@Transactional(readOnly = true)
	public List<CruceDetectado> buscarCruces(UUID seccionId, SolicitudCruces solicitud) {
		var datos = secciones.buscar(seccionId)
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("La sección no existe."));
		List<CeldaHorario> celdas = solicitud.celdas().stream()
				.map(c -> new CeldaHorario(c.dia(), c.numeroBloque(), c.planCursoId(), c.ambienteId())).toList();
		List<BloqueSolicitado> solicitados = conversor.aBloquesSolicitados(seccionId, celdas);
		return validadorCruces.buscarCruces(datos.anioEscolarId(), seccionId, solicitados);
	}
}
