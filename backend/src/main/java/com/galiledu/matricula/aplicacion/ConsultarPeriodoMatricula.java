package com.galiledu.matricula.aplicacion;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import com.galiledu.matricula.aplicacion.puertos.ConsultaPeriodoMatricula;
import com.galiledu.matricula.dominio.PeriodoMatricula;

/** Consulta el periodo y si su opción debe habilitarse (HU-27, RF-59). */
public final class ConsultarPeriodoMatricula {
	private final ConsultaPeriodoMatricula periodos;

	public ConsultarPeriodoMatricula(ConsultaPeriodoMatricula periodos) {
		this.periodos = Objects.requireNonNull(periodos);
	}

	public Optional<Resultado> ejecutar(int anioEscolar, LocalDate fecha) {
		if (anioEscolar < 1) {
			throw new IllegalArgumentException("El año escolar debe ser positivo");
		}
		Objects.requireNonNull(fecha, "La fecha de consulta es obligatoria");
		return periodos.buscarPorAnio(anioEscolar)
			.map(periodo -> new Resultado(periodo, periodo.habilitadoEn(fecha)));
	}

	public record Resultado(PeriodoMatricula periodo, boolean habilitado) {}
}
