package com.galiledu.configuracion.aplicacion;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lo que las secciones necesitan saber de los usuarios (módulo usuarios). Hoy se lee con SQL de solo lectura; cuando
 * usuarios ofrezca un servicio equivalente, solo cambia la clase que implementa esta interfaz.
 */
public interface DirectorioDocentes {
	record Docente(UUID id, String nombre) {
	}

	/** Usuarios activos con rol DOCENTE o TUTOR, sin repetir a quien tiene ambos, por nombre. */
	List<Docente> elegiblesParaTutoria();

	/** Nombre completo de cada usuario pedido, esté activo o no (una sección conserva a un tutor ya desactivado). */
	Map<UUID, String> nombresDe(Collection<UUID> usuarioIds);
}
