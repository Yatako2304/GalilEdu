package com.galiledu.horarios.dominio;

/** Estado guardado de una reserva. "Pendiente" y "Finalizado" no se guardan: se calculan con la fecha y la hora. */
public enum EstadoReserva {
	ACTIVA, CANCELADA
}
