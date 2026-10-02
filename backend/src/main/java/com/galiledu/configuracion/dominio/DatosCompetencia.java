package com.galiledu.configuracion.dominio;

import java.util.UUID;

public record DatosCompetencia(UUID areaCurricularId, String nombre, String descripcion) {
	public DatosCompetencia {
		if (areaCurricularId == null) throw new IllegalArgumentException("El área curricular es obligatoria");
		nombre = DatosAreaCurricular.obligatorio(nombre, 150, "El nombre de la competencia es obligatorio");
		descripcion = DatosAreaCurricular.obligatorio(descripcion, 255, "La descripción de la competencia es obligatoria");
	}
}
