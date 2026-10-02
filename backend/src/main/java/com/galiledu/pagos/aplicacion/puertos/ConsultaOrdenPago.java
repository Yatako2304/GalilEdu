package com.galiledu.pagos.aplicacion.puertos;

import java.util.Optional;

import com.galiledu.pagos.dominio.OrdenPago;

/** Obtiene la orden emitida por Pagos para contrastarla con una notificación verificada. */
public interface ConsultaOrdenPago {
	Optional<OrdenPago> buscarPorIdentificador(String identificador);
}
