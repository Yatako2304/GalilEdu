package com.galiledu.configuracion.dominio;

import java.util.UUID;

/**
 * Un documento requerido como lo envía la pantalla 13. {@code id} es null si el documento es nuevo; si viene, debe ser
 * uno del periodo. La plantilla se sube aparte, por documento.
 */
public record DatosDocumento(UUID id, String nombre, String descripcion) {
}
