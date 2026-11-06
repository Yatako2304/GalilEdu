package com.galiledu.horarios.aplicacion;

import java.util.Optional;
import java.util.UUID;

/**
 * Lo que Horarios necesita saber de una sección de Configuración: su año, grado, nivel, aula, tutor y el estado del
 * año. Es de solo lectura. Hoy lo resuelve {@code DatosDeSeccionSql}; cuando Configuración ofrezca un servicio
 * equivalente, solo se reemplaza esa clase.
 */
public interface DatosDeSeccion {
	/**
	 * Una sección. {@code nivel} es PRIMARIA o SECUNDARIA y {@code estadoAnio} es el del año escolar (PLANIFICADO,
	 * ACTIVO o CERRADO). El aula y el tutor pueden faltar.
	 */
	record DatosSeccion(UUID id, String nombre, String gradoCodigo, String gradoNombre, String nivel,
			UUID anioEscolarId, String estadoAnio, UUID ambienteId, UUID tutorId, String tutorNombre) {
	}

	Optional<DatosSeccion> buscar(UUID seccionId);
}
