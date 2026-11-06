package com.galiledu.horarios.aplicacion;

import java.time.LocalTime;

import com.galiledu.configuracion.dominio.DiaSemana;

/**
 * Textos de los cruces de horario (HU-46). Cada mensaje se muestra tal cual al usuario, así que debe leerse bien solo:
 * nombra con quién choca y cuándo (las horas son las del bloque ya guardado). No llevan título: el frontend lo arma con
 * el tipo ("Docente no disponible", "Ambiente no disponible"); solo el 409 del PUT lo antepone con {@link #conTitulo}.
 */
final class MensajesCruce {
	private MensajesCruce() {
	}

	static String docente(BloqueOcupado ocupado) {
		return "%s ya dicta %s con %s el %s de %s a %s.".formatted(ocupado.docente(),
				ocupado.curso(), ocupado.seccion(), nombreDelDia(ocupado.dia()), hora(ocupado.inicio()),
				hora(ocupado.fin()));
	}

	static String ambiente(BloqueOcupado ocupado) {
		return "%s ya usa %s para %s el %s de %s a %s.".formatted(ocupado.seccion(),
				ocupado.ambiente(), ocupado.curso(), nombreDelDia(ocupado.dia()), hora(ocupado.inicio()),
				hora(ocupado.fin()));
	}

	/** El título de la alerta. El GET y el endpoint de cruces no lo llevan: el frontend lo arma con el tipo. */
	static String titulo(TipoCruce tipo) {
		return switch (tipo) {
			case DOCENTE -> "Docente no disponible";
			case AMBIENTE -> "Ambiente no disponible";
		};
	}

	/** «Título: texto», como lo muestra el 409 del PUT en una sola notificación. */
	static String conTitulo(CruceDetectado cruce) {
		return titulo(cruce.tipo()) + ": " + cruce.mensaje();
	}

	static String nombreDelDia(DiaSemana dia) {
		return switch (dia) {
			case LUN -> "lunes";
			case MAR -> "martes";
			case MIE -> "miércoles";
			case JUE -> "jueves";
			case VIE -> "viernes";
			case SAB -> "sábado";
		};
	}

	/** H:mm, sin cero a la izquierda en la hora: "8:00", "14:20". */
	static String hora(LocalTime hora) {
		return "%d:%02d".formatted(hora.getHour(), hora.getMinute());
	}
}
