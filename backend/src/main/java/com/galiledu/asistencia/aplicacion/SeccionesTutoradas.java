package com.galiledu.asistencia.aplicacion;

import java.util.List;
import java.util.UUID;

/**
 * Lectura de Configuración y Matrícula que necesita Asistencia: las secciones del tutor y sus estudiantes. Es de solo
 * lectura; hoy lo resuelve {@code SeccionesTutoradasSql}. Si esos módulos ofrecen un servicio, solo se reemplaza esa
 * clase.
 */
public interface SeccionesTutoradas {
	/**
	 * Sección ACTIVA del año ACTIVO de la que el usuario es tutor o cotutor (RF-143: los dos con los mismos permisos).
	 * {@code cargo} es TUTOR o COTUTOR.
	 */
	record SeccionTutorada(UUID id, UUID anioEscolarId, int anio, String grado, String nombre, String nivel,
			String cargo) {
	}

	/** Estudiante con matrícula MATRICULADO en la sección. {@code documento} es el número de documento. */
	record EstudianteDeSeccion(UUID matriculaId, UUID estudianteId, String nombres, String apellidos,
			String documento) {
	}

	/** Las secciones del usuario, por grado y nombre. Vacío si no es tutor ni cotutor de ninguna. */
	List<SeccionTutorada> delUsuario(UUID usuarioId);

	/** Estudiantes MATRICULADO de la sección, por apellido paterno, materno y nombres. */
	List<EstudianteDeSeccion> estudiantes(UUID seccionId);
}
