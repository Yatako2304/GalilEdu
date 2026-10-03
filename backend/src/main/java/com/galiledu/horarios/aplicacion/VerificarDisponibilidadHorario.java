package com.galiledu.horarios.aplicacion;

import java.util.List;
import java.util.Objects;

import com.galiledu.horarios.aplicacion.puertos.ConsultaOcupacionesHorario;
import com.galiledu.horarios.dominio.CruceHorario;
import com.galiledu.horarios.dominio.OcupacionHoraria;
import com.galiledu.horarios.dominio.TipoCruceHorario;
import com.galiledu.horarios.dominio.ValidadorCrucesHorario;

/** Verifica una ocupación antes de registrarla o modificarla; no guarda nada. */
public final class VerificarDisponibilidadHorario {
	private final ConsultaOcupacionesHorario ocupaciones;
	private final ValidadorCrucesHorario validador;

	public VerificarDisponibilidadHorario(ConsultaOcupacionesHorario ocupaciones,
		ValidadorCrucesHorario validador) {
		this.ocupaciones = Objects.requireNonNull(ocupaciones);
		this.validador = Objects.requireNonNull(validador);
	}

	public Resultado ejecutar(OcupacionHoraria propuesta) {
		Objects.requireNonNull(propuesta, "La propuesta es obligatoria");
		List<OcupacionHoraria> vigentes = Objects.requireNonNull(ocupaciones.buscarOcupaciones(
			propuesta.horarioSeccionId(), propuesta.bloqueHorarioId(), propuesta.dia(),
			propuesta.periodos()), "La consulta no puede devolver null");
		return new Resultado(validador.detectar(propuesta, vigentes));
	}

	public record Resultado(List<CruceHorario> cruces) {
		public Resultado {
			cruces = List.copyOf(cruces);
		}

		public boolean disponible() {
			return cruces.isEmpty();
		}

		/** La propia sección ya tiene algo en ese bloque; distinto de un cruce externo. */
		public boolean seccionOcupada() {
			return cruces.stream().anyMatch(cruce -> cruce.tipo() == TipoCruceHorario.SECCION);
		}
	}
}
