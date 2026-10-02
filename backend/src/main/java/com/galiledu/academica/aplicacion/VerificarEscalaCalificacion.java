package com.galiledu.academica.aplicacion;

import java.util.Objects;

import com.galiledu.academica.aplicacion.puertos.ConsultaEscalaCompetencia;
import com.galiledu.academica.dominio.Calificacion;

/** Comprueba que la nota use la escala configurada antes de registrarla (RF-136). */
public final class VerificarEscalaCalificacion {
	private final ConsultaEscalaCompetencia escalas;

	public VerificarEscalaCalificacion(ConsultaEscalaCompetencia escalas) {
		this.escalas = Objects.requireNonNull(escalas);
	}

	public Resultado ejecutar(String curso, String grado, String competencia,
		Calificacion calificacion) {
		if (curso == null || curso.isBlank() || grado == null || grado.isBlank()
			|| competencia == null || competencia.isBlank()) {
			throw new IllegalArgumentException("Curso, grado y competencia son obligatorios");
		}
		Objects.requireNonNull(calificacion, "La calificación es obligatoria");
		return escalas.buscar(curso, grado, competencia)
			.map(escala -> escala == calificacion.escala()
				? Resultado.COMPATIBLE : Resultado.ESCALA_INCOMPATIBLE)
			.orElse(Resultado.ESCALA_NO_CONFIGURADA);
	}

	public enum Resultado {
		COMPATIBLE,
		ESCALA_INCOMPATIBLE,
		ESCALA_NO_CONFIGURADA
	}
}
