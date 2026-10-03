package com.galiledu.configuracion.aplicacion.puertos;

import java.util.List;
import java.util.UUID;

/** Configuration supplies active sections of the chosen section's year and grade. */
public interface SeccionesParaMatricula {
	List<Seccion> bloquearOpciones(UUID anioEscolarId, UUID seccionSolicitadaId);

	record Seccion(UUID id, UUID gradoId, String nombre, int capacidadMaxima) {}
}
