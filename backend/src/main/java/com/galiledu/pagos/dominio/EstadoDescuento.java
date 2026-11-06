package com.galiledu.pagos.dominio;

/** RF-76: un descuento INACTIVO no se puede aplicar, pero no afecta a los compromisos que ya lo tienen. */
public enum EstadoDescuento {
	ACTIVO, INACTIVO
}
