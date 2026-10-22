package com.galiledu.configuracion.aplicacion;

import java.util.Optional;
import java.util.UUID;

/**
 * Lo que la estructura horaria necesita saber del plan de evaluación de Académico: cuántas horas semanales tienen
 * planificadas los grados de un nivel. Es de solo lectura. Hoy lo resuelve {@code PlanesDeEvaluacionSql}; cuando
 * Académico ofrezca un servicio equivalente, solo se reemplaza esa clase.
 */
public interface PlanesDeEvaluacion {
	/** Un grado cuyas horas planificadas superan los bloques de la estructura: su nombre legible y esas horas. */
	record GradoExcedido(String gradoNombre, int horasPlanificadas) {
	}

	/**
	 * El grado del nivel, en el año, con más horas planificadas que {@code bloquesSemanales}; el de más horas si hay
	 * varios (a igualdad, el de menor orden). Vacío si ninguno se pasa o si el nivel no tiene planes.
	 */
	Optional<GradoExcedido> gradoQueExcede(UUID anioEscolarId, String nivel, int bloquesSemanales);

	/** Si algún grado del nivel tiene cursos planificados en el año. */
	boolean hayPlanes(UUID anioEscolarId, String nivel);
}
