package com.galiledu.configuracion.dominio;

/** Un receso pedido por el usuario: cuánto dura y después de qué bloque (1 es el primero) va. */
public record DatosReceso(int despuesDeBloque, int duracionMin) {
}
