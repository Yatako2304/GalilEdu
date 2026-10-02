package com.galiledu.horarios.dominio;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/** RF-117 y RF-118: cruces de docente y aula entre secciones del mismo bloque. */
public final class ValidadorCrucesHorario {
	public List<CruceHorario> detectar(AsignacionHorario propuesta,
		Collection<AsignacionHorario> asignacionesVigentes) {
		Objects.requireNonNull(propuesta, "La propuesta es obligatoria");
		Objects.requireNonNull(asignacionesVigentes, "Las asignaciones vigentes son obligatorias");
		List<CruceHorario> cruces = new ArrayList<>();
		for (AsignacionHorario vigente : asignacionesVigentes) {
			Objects.requireNonNull(vigente, "Una asignación vigente no puede ser nula");
			if (propuesta.identificador().equals(vigente.identificador())
				|| propuesta.anioEscolar() != vigente.anioEscolar()
				|| propuesta.dia() != vigente.dia()
				|| propuesta.bloque() != vigente.bloque()
				|| propuesta.seccion().equals(vigente.seccion())) {
				continue;
			}
			if (propuesta.docente().equals(vigente.docente())) {
				cruces.add(new CruceHorario(TipoCruceHorario.DOCENTE,
					vigente.seccion(), vigente.curso()));
			}
			if (propuesta.aula().equals(vigente.aula())) {
				cruces.add(new CruceHorario(TipoCruceHorario.AULA,
					vigente.seccion(), vigente.curso()));
			}
		}
		return List.copyOf(cruces);
	}
}
