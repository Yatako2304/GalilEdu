package com.galiledu.horarios.aplicacion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;

/**
 * Detecta los cruces de docente y de ambiente entre secciones del mismo año (HU-46).
 * <p>
 * Dos bloques se cruzan cuando son del mismo día y sus horas se superponen. Se compara por hora y no por número de
 * bloque porque cada sección tiene su propia estructura horaria: el bloque 2 de una puede caer a mitad del bloque 1 de
 * otra. Que un bloque termine justo cuando empieza el otro no es un cruce.
 */
@Service
public class ValidadorCrucesHorario implements ValidadorCruces {
	private static final Comparator<CruceDetectado> ORDEN = Comparator.comparing(CruceDetectado::dia)
			.thenComparingInt(CruceDetectado::numeroBloque).thenComparing(CruceDetectado::tipo);

	private final OcupacionHorarios ocupacionHorarios;

	public ValidadorCrucesHorario(OcupacionHorarios ocupacionHorarios) {
		this.ocupacionHorarios = ocupacionHorarios;
	}

	@Override
	public List<CruceDetectado> buscarCruces(UUID anioId, UUID seccionId, List<BloqueSolicitado> solicitados) {
		if (solicitados.isEmpty()) {
			return List.of();
		}
		return compararCruces(solicitados, ocupacionHorarios.bloquesOcupados(anioId, seccionId));
	}

	@Override
	public List<CruceDetectado> compararCruces(List<BloqueSolicitado> solicitados, List<BloqueOcupado> ocupados) {
		List<CruceDetectado> cruces = new ArrayList<>();
		for (BloqueSolicitado solicitado : solicitados) {
			for (BloqueOcupado ocupado : ocupados) {
				if (!seSuperponen(solicitado, ocupado)) {
					continue;
				}
				// Sin docente o sin ambiente asignado no hay con quién chocar.
				if (solicitado.docenteId() != null && Objects.equals(solicitado.docenteId(), ocupado.docenteId())) {
					cruces.add(cruce(TipoCruce.DOCENTE, solicitado, ocupado, MensajesCruce.docente(ocupado)));
				}
				if (solicitado.ambienteId() != null && Objects.equals(solicitado.ambienteId(), ocupado.ambienteId())) {
					cruces.add(cruce(TipoCruce.AMBIENTE, solicitado, ocupado, MensajesCruce.ambiente(ocupado)));
				}
			}
		}
		// El orden es estable: los empates conservan el orden de los ocupados.
		cruces.sort(ORDEN);
		return cruces;
	}

	private static boolean seSuperponen(BloqueSolicitado solicitado, BloqueOcupado ocupado) {
		return solicitado.dia() == ocupado.dia() && solicitado.inicio().isBefore(ocupado.fin())
				&& ocupado.inicio().isBefore(solicitado.fin());
	}

	private static CruceDetectado cruce(TipoCruce tipo, BloqueSolicitado solicitado, BloqueOcupado ocupado,
			String mensaje) {
		return new CruceDetectado(tipo, solicitado.dia(), solicitado.numeroBloque(), mensaje, ocupado.seccion(),
				ocupado.curso());
	}
}
