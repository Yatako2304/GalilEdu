package com.galiledu.usuarios.aplicacion;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Lo que "Ver/Editar usuario" y «Datos de la Familia» muestran de otros módulos: los estudiantes a cargo de un
 * apoderado y su familia (estudiantes) y las secciones de un tutor (configuracion). Se lee con SQL de solo lectura
 * porque estudiantes ya depende de usuarios; si esos módulos ofrecen un servicio equivalente, solo cambia la clase que
 * implementa esta interfaz.
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

	/** «Datos de la Familia»: estudiante del que la persona es padre, madre o apoderado. */
	record EstudianteDeFamilia(UUID estudianteId, String nombres, String apellidoPaterno, String apellidoMaterno,
			String grado, int anioAcademico, String estado) {
	}

	/**
	 * Padre, madre o apoderado registrado en la inscripción de un estudiante. {@code personaId} es null si el padre o
	 * la madre «no registra» (entonces trae el motivo).
	 */
	record IntegranteFamilia(UUID estudianteId, String parentesco, UUID personaId, String relacion,
			String motivoNoRegistra) {
	}

	/** Contacto de emergencia de un estudiante (Paso 4 de la inscripción), en el orden en que se registró. */
	record ContactoDeEmergencia(UUID estudianteId, String nombreCompleto, String parentesco, String celular,
			String telefonoFijo) {
	}

	/** Estudiantes no eliminados vinculados a la persona, del año académico más reciente al más antiguo. */
	List<EstudianteACargo> estudiantesACargo(UUID personaId);

	/** Secciones activas del año escolar en curso (estado ACTIVO) en las que el usuario es tutor o cotutor. */
	List<SeccionTutoria> tutorDe(UUID usuarioId);

	/** Estudiantes no eliminados de los que la persona es padre, madre o apoderado, ordenados por nombre. */
	List<EstudianteDeFamilia> estudiantesDeFamilia(UUID personaId);

	/** Padre, madre y apoderado de cada uno de esos estudiantes. */
	List<IntegranteFamilia> integrantesDe(Collection<UUID> estudianteIds);

	/** Contactos de emergencia de esos estudiantes. */
	List<ContactoDeEmergencia> contactosDeEmergencia(Collection<UUID> estudianteIds);
}
