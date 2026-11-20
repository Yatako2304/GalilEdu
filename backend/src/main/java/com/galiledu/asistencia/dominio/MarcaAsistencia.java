package com.galiledu.asistencia.dominio;

import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Lo que el tutor marca para un estudiante: el estado y, solo en la tardanza, la hora de llegada (obligatoria, como
 * acordó el equipo el 19/11). Se valida al crearla; los mensajes se muestran tal cual al usuario.
 */
public record MarcaAsistencia(UUID matriculaId, EstadoAsistencia estado, LocalTime horaLlegada) {
	public MarcaAsistencia {
		if (matriculaId == null) {
			throw new IllegalArgumentException("Falta indicar el estudiante.");
		}
		if (estado == null) {
			throw new IllegalArgumentException("Marca a cada estudiante como presente, tardanza o falta.");
		}
		if (estado == EstadoAsistencia.TARDANZA && horaLlegada == null) {
			throw new IllegalArgumentException("Indica la hora de llegada de cada tardanza.");
		}
		horaLlegada = estado == EstadoAsistencia.TARDANZA ? horaLlegada.withSecond(0).withNano(0) : null;
	}

	public boolean igualA(EstadoAsistencia otroEstado, LocalTime otraHora) {
		return estado == otroEstado && Objects.equals(horaLlegada, otraHora);
	}
}
