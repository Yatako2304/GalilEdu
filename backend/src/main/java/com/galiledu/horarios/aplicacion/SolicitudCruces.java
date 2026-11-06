package com.galiledu.horarios.aplicacion;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.galiledu.horarios.aplicacion.DtosHorarios.SolicitudCelda;

/**
 * Cuerpo del POST /horarios/secciones/{seccionId}/cruces: las celdas que el Coordinador está armando, aunque aún no
 * estén guardadas. Usa la misma celda que el PUT. El {@code @Valid} va sobre el tipo de la celda y no sobre la lista.
 */
public record SolicitudCruces(
		@NotNull(message = "Envía las celdas del horario.") @Size(max = 72, message = "El horario admite hasta 72 bloques.") List<@NotNull(message = "Hay una celda vacía.") @Valid SolicitudCelda> celdas) {
}
