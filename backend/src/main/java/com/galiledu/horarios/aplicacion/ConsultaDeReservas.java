package com.galiledu.horarios.aplicacion;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Lectura de reservas con los nombres que necesitan la pantalla y los mensajes de cruce (ambiente, sección, grado y
 * responsable). Es de solo lectura: las escrituras pasan por la entidad. Hoy lo resuelve {@code ConsultaDeReservasSql}.
 */
public interface ConsultaDeReservas {
	/** Una reserva con sus nombres. {@code estado} es ACTIVA o CANCELADA; {@code seccion} es «4.° B». */
	record ReservaLeida(UUID id, UUID ambienteId, String ambienteCodigo, String ambienteNombre, String actividad,
			UUID anioId, String gradoCodigo, UUID seccionId, String seccion, UUID responsableId, String responsable,
			LocalDate fecha, LocalTime horaInicio, LocalTime horaFin, String estado, long version) {
	}

	Optional<ReservaLeida> porId(UUID id);

	List<ReservaLeida> porIds(Collection<UUID> ids);

	/**
	 * Las ACTIVAS con fecha entre {@code desde} y {@code hasta} (inclusive), de la más reciente a la más antigua
	 * (fecha y hora de inicio descendentes).
	 */
	List<ReservaLeida> activasEntre(LocalDate desde, LocalDate hasta, int limite, long desplazamiento);

	long contarActivasEntre(LocalDate desde, LocalDate hasta);
}
