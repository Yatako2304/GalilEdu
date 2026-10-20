package com.galiledu.pagos.dominio;

/** Estados guardados de pagos.cuota (CHECK ck_cuota_estado). "Vencida" no es un estado: se calcula (RF-91). */
public enum EstadoCuota {
	PENDIENTE, EN_PROCESO, PAGADA_PARCIAL, PAGADA, ANULADA
}
