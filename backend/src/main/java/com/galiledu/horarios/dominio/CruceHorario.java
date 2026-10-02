package com.galiledu.horarios.dominio;

import java.util.Objects;

/** Datos necesarios para informar el conflicto de RF-117 o RF-118. */
public record CruceHorario(TipoCruceHorario tipo, String seccion, String curso) {
	public CruceHorario {
		Objects.requireNonNull(tipo, "El tipo de cruce es obligatorio");
		Objects.requireNonNull(seccion, "La sección es obligatoria");
		Objects.requireNonNull(curso, "El curso es obligatorio");
	}
}
