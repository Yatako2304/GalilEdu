package com.galiledu.usuarios.aplicacion;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Lo que "Ver/Editar usuario" muestra de otros módulos: los estudiantes a cargo de un apoderado (estudiantes) y las
 * secciones de un tutor (configuracion). Se lee con SQL de solo lectura porque estudiantes ya depende de usuarios; si
 * esos módulos ofrecen un servicio equivalente, solo cambia la clase que implementa esta interfaz.
 */
public interface VinculosUsuario {
	/** Estudiante del que la persona es padre, madre o apoderado, con los datos del vínculo de su inscripción. */
	record EstudianteACargo(UUID estudianteId, String nombreCompleto, String documento, String grado,
			int anioAcademico, String estado, String parentesco, String relacion, boolean viveConEstudiante,
			boolean tienePatriaPotestad, boolean esResponsable, Acreditacion acreditacion) {
	}

	/** Documento que acredita al apoderado (carta poder…), sin su contenido. */
	record Acreditacion(String tipo, String nombreArchivo, Instant subidoEn) {
	}

	/** Sección en la que el usuario es tutor o cotutor. {@code seccion}: "1.° de secundaria A". */
	record SeccionTutoria(UUID seccionId, String seccion, String cargo) {
	}

	/** Estudiantes no eliminados vinculados a la persona, del año académico más reciente al más antiguo. */
	List<EstudianteACargo> estudiantesACargo(UUID personaId);

	/** Secciones activas del año escolar en curso (estado ACTIVO) en las que el usuario es tutor o cotutor. */
	List<SeccionTutoria> tutorDe(UUID usuarioId);
}
