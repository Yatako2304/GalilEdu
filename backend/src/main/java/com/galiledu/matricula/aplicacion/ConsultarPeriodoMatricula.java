package com.galiledu.matricula.aplicacion;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.galiledu.matricula.aplicacion.puertos.ConsultaPeriodoMatricula;
import com.galiledu.matricula.dominio.PeriodoMatricula;

/** Consulta el periodo y si su opción debe habilitarse (HU-27, RF-59). */
public final class ConsultarPeriodoMatricula {
	private final ConsultaPeriodoMatricula periodos;

	public ConsultarPeriodoMatricula(ConsultaPeriodoMatricula periodos) {
		this.periodos = Objects.requireNonNull(periodos);
	}

	public Optional<Resultado> ejecutar(UUID anioEscolarId, LocalDate fecha) {
		Objects.requireNonNull(anioEscolarId, "El año escolar es obligatorio");
		Objects.requireNonNull(fecha, "La fecha de consulta es obligatoria");
		return periodos.buscarPorAnio(anioEscolarId)
			.map(periodo -> new Resultado(periodo, periodo.habilitadoEn(fecha)));
	}

	public record Resultado(PeriodoMatricula periodo, boolean habilitado) {}
}
