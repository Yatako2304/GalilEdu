package com.galiledu.horarios.aplicacion;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.galiledu.horarios.aplicacion.puertos.ConsultaHorarioSemanal;
import com.galiledu.horarios.aplicacion.puertos.ConsultaHorarioSemanal.EntradaHorario;

/**
 * Vistas semanales del horario vigente. Decidir quién puede ver qué (por ejemplo, que un
 * apoderado solo consulte a su hijo) corresponde a la autorización del llamador.
 */
public class ConsultarHorarios {
	private final ConsultaHorarioSemanal consulta;

	public ConsultarHorarios(ConsultaHorarioSemanal consulta) {
		this.consulta = Objects.requireNonNull(consulta);
	}

	public List<EntradaHorario> porDocente(UUID docenteId, UUID periodoId) {
		return consulta.porDocente(obligatorio(docenteId, "docente"), obligatorio(periodoId, "periodo"));
	}

	public List<EntradaHorario> porSeccion(UUID seccionId, UUID periodoId) {
		return consulta.porSeccion(obligatorio(seccionId, "sección"), obligatorio(periodoId, "periodo"));
	}

	public List<EntradaHorario> porMatricula(UUID matriculaId, UUID periodoId) {
		return consulta.porMatricula(obligatorio(matriculaId, "matrícula"), obligatorio(periodoId, "periodo"));
	}

	private static UUID obligatorio(UUID valor, String nombre) {
		return Objects.requireNonNull(valor, "El campo " + nombre + " es obligatorio");
	}
}
