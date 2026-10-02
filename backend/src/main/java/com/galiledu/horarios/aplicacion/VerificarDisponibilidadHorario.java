package com.galiledu.horarios.aplicacion;

import java.util.List;
import java.util.Objects;

import com.galiledu.horarios.aplicacion.puertos.ConsultaAsignacionesHorario;
import com.galiledu.horarios.dominio.AsignacionHorario;
import com.galiledu.horarios.dominio.CruceHorario;
import com.galiledu.horarios.dominio.ValidadorCrucesHorario;

/** Verifica un bloque antes de registrarlo o modificarlo; no guarda la asignación. */
public final class VerificarDisponibilidadHorario {
	private final ConsultaAsignacionesHorario asignaciones;
	private final ValidadorCrucesHorario validador;

	public VerificarDisponibilidadHorario(ConsultaAsignacionesHorario asignaciones,
		ValidadorCrucesHorario validador) {
		this.asignaciones = Objects.requireNonNull(asignaciones);
		this.validador = Objects.requireNonNull(validador);
	}

	public Resultado ejecutar(AsignacionHorario propuesta) {
		Objects.requireNonNull(propuesta, "La propuesta es obligatoria");
		List<AsignacionHorario> vigentes = Objects.requireNonNull(asignaciones.buscarEnBloque(
			propuesta.anioEscolar(), propuesta.dia(), propuesta.bloque()),
			"La consulta no puede devolver null");
		boolean seccionOcupada = vigentes.stream().anyMatch(vigente ->
			!vigente.identificador().equals(propuesta.identificador())
				&& vigente.anioEscolar() == propuesta.anioEscolar()
				&& vigente.dia() == propuesta.dia()
				&& vigente.bloque() == propuesta.bloque()
				&& vigente.seccion().equals(propuesta.seccion()));
		return new Resultado(seccionOcupada, validador.detectar(propuesta, vigentes));
	}

	public record Resultado(boolean seccionOcupada, List<CruceHorario> cruces) {
		public Resultado {
			cruces = List.copyOf(cruces);
		}

		public boolean disponible() {
			return !seccionOcupada && cruces.isEmpty();
		}
	}
}
