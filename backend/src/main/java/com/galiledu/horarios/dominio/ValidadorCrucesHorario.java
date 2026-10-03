package com.galiledu.horarios.dominio;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/** RF-117 y RF-118: cruces de sección, docente y aula en el mismo bloque, día y periodo. */
public final class ValidadorCrucesHorario {
	public List<CruceHorario> detectar(OcupacionHoraria propuesta,
		Collection<OcupacionHoraria> vigentes) {
		Objects.requireNonNull(propuesta, "La propuesta es obligatoria");
		Objects.requireNonNull(vigentes, "Las ocupaciones vigentes son obligatorias");
		List<CruceHorario> cruces = new ArrayList<>();
		for (OcupacionHoraria vigente : vigentes) {
			Objects.requireNonNull(vigente, "Una ocupación vigente no puede ser nula");
			if (propuesta.asignacionId().equals(vigente.asignacionId())
				|| !propuesta.coincideEnTiempo(vigente)) {
				continue;
			}
			if (propuesta.ocupaLaMismaSeccionQue(vigente)) {
				cruces.add(cruce(TipoCruceHorario.SECCION, vigente));
				continue;
			}
			if (propuesta.docenteId().equals(vigente.docenteId())) {
				cruces.add(cruce(TipoCruceHorario.DOCENTE, vigente));
			}
			if (propuesta.espacioFisicoId() != null
				&& propuesta.espacioFisicoId().equals(vigente.espacioFisicoId())) {
				cruces.add(cruce(TipoCruceHorario.AULA, vigente));
			}
		}
		return List.copyOf(cruces);
	}

	private static CruceHorario cruce(TipoCruceHorario tipo, OcupacionHoraria vigente) {
		return new CruceHorario(tipo, vigente.asignacionId(), vigente.seccion(), vigente.curso());
	}
}
