package com.galiledu.horarios.aplicacion;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.galiledu.horarios.aplicacion.DtosHorarios.AmbienteOpcion;
import com.galiledu.horarios.aplicacion.DtosHorarios.Referencia;

/**
 * Contratos JSON de la reserva de espacios especiales (HU-48). Las horas de entrada son «HH:mm» (también se acepta
 * «HH:mm:ss») y las de salida siempre «HH:mm»; la fecha es «AAAA-MM-DD».
 */
public final class DtosReservas {
	private DtosReservas() {
	}

	/** El ambiente de una reserva: el {@code codigo} («LAB-01») y el nombre se ven en la tabla. */
	public record AmbienteDeReserva(UUID id, String codigo, String nombre) {
	}

	/** La sección de una reserva; {@code etiqueta} es «4.° B», como en el resto del módulo. */
	public record SeccionDeReserva(UUID id, String etiqueta) {
	}

	/** Lo que se calcula al leer: no se guarda. */
	public enum EstadoDeReserva {
		PENDIENTE, FINALIZADO
	}

	/** Cuerpo del POST /horarios/reservas. */
	public record SolicitudReserva(@NotNull(message = "Elige el ambiente.") UUID ambienteId,
			@NotNull(message = "Elige la sección.") UUID seccionId,
			@NotNull(message = "Elige el responsable.") UUID responsableId,
			@NotBlank(message = "Escribe la actividad.") @Size(max = 120, message = "La actividad admite hasta 120 caracteres.") String actividad,
			@NotNull(message = "Elige la fecha.") LocalDate fecha,
			@NotNull(message = "Elige la hora de inicio.") LocalTime horaInicio,
			@NotNull(message = "Elige la hora de fin.") LocalTime horaFin) {
	}

	/** Cuerpo del PUT /horarios/reservas/{id}: lo mismo, más la {@code version} que devolvió la lectura. */
	public record SolicitudEdicionReserva(@NotNull(message = "Falta la versión de la reserva.") Long version,
			@NotNull(message = "Elige el ambiente.") UUID ambienteId,
			@NotNull(message = "Elige la sección.") UUID seccionId,
			@NotNull(message = "Elige el responsable.") UUID responsableId,
			@NotBlank(message = "Escribe la actividad.") @Size(max = 120, message = "La actividad admite hasta 120 caracteres.") String actividad,
			@NotNull(message = "Elige la fecha.") LocalDate fecha,
			@NotNull(message = "Elige la hora de inicio.") LocalTime horaInicio,
			@NotNull(message = "Elige la hora de fin.") LocalTime horaFin) {
	}

	/**
	 * Una reserva. {@code estado} se calcula con la hora de fin; {@code editable} es true si está activa y no ha
	 * finalizado. {@code version} es la que debe volver en el PUT.
	 */
	public record ReservaDeAmbiente(UUID id, AmbienteDeReserva ambiente, String actividad, UUID anioId,
			String gradoCodigo, SeccionDeReserva seccion, Referencia responsable, LocalDate fecha, String horaInicio,
			String horaFin, EstadoDeReserva estado, boolean editable, long version) {
	}

	/** GET /horarios/reservas/opciones: los espacios especiales activos y quienes pueden ser responsables. */
	public record OpcionesDeReserva(List<AmbienteOpcion> ambientes, List<Referencia> responsables) {
	}
}
