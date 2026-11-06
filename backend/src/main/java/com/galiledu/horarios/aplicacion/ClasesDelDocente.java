package com.galiledu.horarios.aplicacion;

import java.util.List;
import java.util.UUID;

import com.galiledu.configuracion.dominio.DiaSemana;

/**
 * Las clases que un docente dicta en un año: los bloques guardados donde es el titular de
 * {@code academico.docente_seccion} para ese curso y esa sección. Es de solo lectura (HU-49).
 */
public interface ClasesDelDocente {
	/**
	 * Un bloque que el docente dicta. {@code seccion} es la etiqueta legible ("1.° A"); {@code estructuraId} es la
	 * estructura horaria de ESA sección y {@code nivel} el de su grado, para pedir después la jornada de cada una.
	 * {@code ambiente} es el nombre del ambiente.
	 */
	record ClaseDelDocente(UUID seccionId, String seccion, String nivel, UUID estructuraId, DiaSemana dia,
			int numeroBloque, UUID planCursoId, String curso, UUID ambienteId, String ambiente) {
	}

	/** Un año escolar en el que el docente tiene clases. {@code estado} es PLANIFICADO, ACTIVO o CERRADO. */
	record AnioConClases(UUID id, int anio, String estado) {
	}

	/** Los años en los que el docente tiene al menos un bloque como titular, del más reciente al más antiguo. */
	List<AnioConClases> aniosDe(UUID docenteId);

	/**
	 * Las clases del docente en el año, ordenadas por día, número de bloque y sección. Vacía si no dicta ninguna
	 * (también si solo es tutor).
	 */
	List<ClaseDelDocente> clasesDe(UUID docenteId, UUID anioEscolarId);
}
