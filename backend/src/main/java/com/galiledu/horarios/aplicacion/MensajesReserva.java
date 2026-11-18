package com.galiledu.horarios.aplicacion;

import java.time.LocalDate;
import java.time.LocalTime;

import com.galiledu.configuracion.dominio.DiaSemana;
import com.galiledu.horarios.aplicacion.ReglasDeReserva.ReservaVigente;

/**
 * Textos de los cruces de una reserva (HU-48), en el estilo de {@link MensajesCruce}: cada mensaje se muestra tal cual
 * al usuario, nombra con quién choca y cuándo, y las horas van sin cero a la izquierda. Los cruces con un bloque del
 * horario semanal reutilizan los textos de {@link MensajesCruce}; aquí solo están los propios de las reservas.
 */
final class MensajesReserva {
	private MensajesReserva() {
	}

	/** «Título: texto», como lo muestra el 409 en una sola notificación. Sin título, el texto solo. */
	static String conTitulo(String titulo, String mensaje) {
		return titulo == null ? mensaje : titulo + ": " + mensaje;
	}

	static String ambienteReservado(ReservaVigente r) {
		return "%s ya está reservado por %s para %s %s.".formatted(r.ambiente(), r.seccion(), r.actividad(), cuando(r));
	}

	static String responsableConReserva(ReservaVigente r) {
		return "%s ya es responsable de %s con %s %s.".formatted(r.responsable(), r.actividad(), r.seccion(),
				cuando(r));
	}

	static String seccionConReserva(ReservaVigente r) {
		return "%s ya tiene reservado %s para %s %s.".formatted(r.seccion(), r.ambiente(), r.actividad(), cuando(r));
	}

	/** La sección ya tiene ese mismo ambiente en su horario: no necesita reservarlo. */
	static String ambienteEnSuHorario(BloqueOcupado b) {
		return "%s ya tiene %s en su horario el %s de %s a %s; no necesita reserva.".formatted(b.seccion(),
				b.ambiente(), MensajesCruce.nombreDelDia(b.dia()), MensajesCruce.hora(b.inicio()),
				MensajesCruce.hora(b.fin()));
	}

	/** La sección tiene clase en otro espacio especial a esa hora. */
	static String otroEspacioEnSuHorario(BloqueOcupado b) {
		return "%s tiene %s en %s el %s de %s a %s.".formatted(b.seccion(), b.curso(), b.ambiente(),
				MensajesCruce.nombreDelDia(b.dia()), MensajesCruce.hora(b.inicio()), MensajesCruce.hora(b.fin()));
	}

	/** «el lunes 23/11/2026 de 8:00 a 9:30». */
	private static String cuando(ReservaVigente r) {
		return "el %s %s de %s a %s".formatted(nombreDelDia(r.fecha()), fecha(r.fecha()), hora(r.horaInicio()),
				hora(r.horaFin()));
	}

	static String nombreDelDia(LocalDate fecha) {
		DiaSemana dia = ReglasDeReserva.diaDe(fecha);
		return dia == null ? "domingo" : MensajesCruce.nombreDelDia(dia);
	}

	static String fecha(LocalDate fecha) {
		return "%02d/%02d/%d".formatted(fecha.getDayOfMonth(), fecha.getMonthValue(), fecha.getYear());
	}

	static String hora(LocalTime hora) {
		return MensajesCruce.hora(hora);
	}
}
