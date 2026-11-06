package com.galiledu.horarios.aplicacion;

import java.util.List;
import java.util.UUID;

/**
 * Lo que el horario del apoderado (HU-51) necesita saber de sus hijos: en qué sección está matriculado cada uno y
 * quién es su tutor. Es de solo lectura. Hoy lo resuelve {@code HijosDelApoderadoSql}; cuando Estudiantes y Matrícula
 * ofrezcan un servicio equivalente, solo se reemplaza esa clase.
 */
public interface HijosDelApoderado {
	/**
	 * Una matrícula en estado MATRICULADO de un hijo del usuario (padre, madre o apoderado vinculado y no fallecido).
	 * {@code nivel} es PRIMARIA o SECUNDARIA. El tutor (y su celular o correo) puede faltar.
	 */
	record MatriculaDeHijo(UUID estudianteId, String nombreCompleto, int anio, UUID seccionId, String seccion,
			String gradoNombre, String nivel, String tutorNombre, String tutorCelular, String tutorCorreo) {
	}

	/** Las matrículas de los hijos del usuario, por nombre del hijo y del año más reciente al más antiguo. */
	List<MatriculaDeHijo> matriculasDeHijos(UUID usuarioId);
}
